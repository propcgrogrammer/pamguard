package PosBridge;

import Acquisition.AcquisitionControl;
import Acquisition.AcquisitionDialog;
import Acquisition.AudioDataQueue;
import Acquisition.DaqSystem;
import PamController.PamControlledUnitSettings;
import PamController.PamSettingManager;
import PamController.PamSettings;
import PamDetection.RawDataUnit;
import PamUtils.PamCalendar;
import PamView.TopToolBar;
import PamguardMVC.PamDataUnit;
import PamguardMVC.PamObservable;
import PamguardMVC.PamObserver;
import simulatedAcquisition.SimObject;
import simulatedAcquisition.SimObjectDataUnit;
import simulatedAcquisition.SimObjectsDataBlock;
import simulatedAcquisition.SimProcess;

import java.awt.BorderLayout;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Serializable;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.BlockingQueue;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import org.apache.commons.collections.map.HashedMap;
import org.json.JSONArray;
import org.json.JSONObject;

public class POSMsgDaq extends DaqSystem implements PamSettings, PamObserver {
	public static String plugin_name = "Poseidoon plugin";

	static AcquisitionControl acquisition_control;
	
	int sampleRate = 0;
	
	private SimObjectsDataBlock simObjectsDataBlock;
	
	private SimProcess sp = null;
	
	/*
	 * ServeræŠ“åˆ°çš„è³‡æ–™ç•¶æˆ� AudioFile ç”¢ç”Ÿçš„éŸ³è¨Šï¼Œä¸¦æ”¾åˆ° AudioDataQueue è£¡é�¢
	 */
	protected AudioDataQueue newDataUnits;
	
	private GenerationThread genThread;

	private Thread theThread;
	
	private volatile boolean dontStop;

	private volatile boolean stillRunning;
	
	private volatile long startTimeMillis;
	
	private volatile long totalSamples;
	
	private int dataUnitSamples;

	private POSMsgDaqPanel ros_msg_daq_panel;

	private POSMsgParams params = new POSMsgParams();

	private volatile boolean pam_stop;

	private volatile boolean pam_running;
	
	private boolean isFirst = true;
	
	private volatile String timestamp = "";
	
	private int record = 0;
	private int countData = 0;
	
	private List<double[]> dataNoiseLst = new ArrayList<double[]>();
	
	private HttpURLConnection conn = null;
	
	private URL url = null;
	
	private BufferedReader reader = null;
	
	private HttpURLConnection urlConnection = null;  
	
	private OutputStream os = null;
	
	private InputStream is = null;
	

	@Override
	public String getUnitName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public long getRequiredDataHistory(PamObservable observable, Object arg) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void addData(PamObservable observable, PamDataUnit pamDataUnit) {
		// TODO Auto-generated method stub

	}

	@Override
	public void updateData(PamObservable observable, PamDataUnit pamDataUnit) {
		// TODO Auto-generated method stub

	}

	@Override
	public void removeObservable(PamObservable observable) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setSampleRate(float sampleRate, boolean notify) {
		
		acquisition_control.acquisitionParameters.sampleRate = sampleRate;
		this.sampleRate = (int) sampleRate;
		//		System.out.println("Acquisition set sample rate to " + sampleRate);
		
	}

	@Override
	public void noteNewSettings() {
		// TODO Auto-generated method stub

	}

	@Override
	public String getObserverName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void masterClockUpdate(long milliSeconds, long sampleNumber) {
		// TODO Auto-generated method stub

	}

	@Override
	public PamObserver getObserverObject() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void receiveSourceNotification(int type, Object object) {
		// TODO Auto-generated method stub

	}

	@Override
	public String getUnitType() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Serializable getSettingsReference() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public long getSettingsVersion() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public boolean restoreSettings(PamControlledUnitSettings pamControlledUnitSettings) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String getSystemType() {
		return plugin_name;
	}

	@Override
	public String getSystemName() {
		return plugin_name;
	}

	@Override
	public JComponent getDaqSpecificDialogComponent(AcquisitionDialog acquisitionDialog) {
		if (this.ros_msg_daq_panel == null)
			this.ros_msg_daq_panel = new POSMsgDaqPanel(acquisitionDialog, this.params);
		return this.ros_msg_daq_panel;
	}

	@Override
	public void dialogSetParams() {
	}

	@Override
	public boolean dialogGetParams() {
		if (this.ros_msg_daq_panel == null)
			return false;
		return true;
	}

	@Override
	public int getMaxSampleRate() {
		return 192000;
	}

	@Override
	public int getMaxChannels() {
		return 2;
	}

	@Override
	public double getPeak2PeakVoltage(int swChannel) {
		return 2.0D;
	}

	@Override
	public boolean prepareSystem(AcquisitionControl daqControl) {
		System.out.println("DaqSystem:Prepare system.");
		
		/*
		 * æ­¤éƒ¨åˆ†ç¨‹å¼�ç¢¼ä¾†è‡ª SimProcess Class
		 * ç›®çš„ç‚ºæº–å‚™ä¸€å€‹Threadå°‡æŠ“åˆ°çš„è³‡æ–™æ”¾åˆ°è©²Threadè£¡é�¢å�šæ¨¡æ“¬
		 */
		this.newDataUnits = daqControl.getDaqProcess().getNewDataQueue();
		if (this.newDataUnits == null) return false;
//		genThread = new GenerationThread();
//		theThread = new Thread(genThread);
		startTimeMillis = System.currentTimeMillis();
		totalSamples = 0;
		dataUnitSamples = (int) (daqControl.acquisitionParameters.sampleRate/10);
		
		
		if (!this.params.m_status) {
			acquisition_control.getDaqProcess().pamStop();
			System.out.println("preparing system failed: connection status is false");
			return false;
		}
		acquisition_control = daqControl;
		this.params.m_audioDataQueue = acquisition_control.getDaqProcess().getNewDataQueue();
		this.sp = new SimProcess(acquisition_control);
		return true;
	}

	@Override
	public boolean startSystem(AcquisitionControl daqControl) {
		
		/*
		 * æ­¤éƒ¨åˆ†ç¨‹å¼�ç¢¼ä¾†è‡ª SimProcess Class
		 * åŸ·è¡Œè©²Threadè£¡é�¢çš„è³‡æ–™
		 */
		dontStop = true;
		genThread = new GenerationThread();
		theThread = new Thread(genThread);
		theThread.start();
		
		System.out.println("DaqSystem:Start system.");
		System.out.println("m_status:" + this.params.m_status);
		System.out.println("pam_stop:" + this.pam_stop);
		System.out.println("pam_running:" + this.pam_running);
		if (!this.params.m_status) {
			System.out.println("Starting system failed: connection status is false");
			return false;
		}
//		Thread thread = new Thread(new DataStreamThread());
//		thread.start();
		setStreamStatus(STREAM_RUNNING);
		TopToolBar.enableStartButton(false);
		TopToolBar.enableStopButton(true);
		return true;
	}

	@Override
	public void stopSystem(AcquisitionControl daqControl) {
		dontStop = false;
		setStreamStatus(STREAM_CLOSED);
		
	}

	@Override
	public boolean isRealTime() {
		// TODO Auto-generated method stub
		return true;
	}

	@Override
	public boolean canPlayBack(float sampleRate) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public int getDataUnitSamples() {
		return 1000;
	}

	@Override
	public void daqHasEnded() {
		System.out.println("Daq has ended.");
	}

	@Override
	public String getDeviceName() {
		return plugin_name;
	}
	
	public Map estConnection() {
		
		String urlStr = this.params.uri;
		String getUrlStr = urlStr;
		
//		HttpURLConnection urlConnection = null;  
//		OutputStream os = null;
//		InputStream is = null;
		
		Map<String, Object> rtnMap = new HashMap<String, Object>();
		
		try {
			
			URL url = new URL(getUrlStr);
			urlConnection = (HttpURLConnection) url.openConnection();
			urlConnection.setRequestMethod("POST");
			urlConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
			
			urlConnection.setDoOutput(true);
			os = urlConnection.getOutputStream();
			is = urlConnection.getInputStream();
			
			
			if(!"".equals(this.timestamp.toString())) {
				String param = "time_stamp="+this.timestamp.toString();
				os.write(param.getBytes());
			}
			os.flush();
			
			
			int responseCode = urlConnection.getResponseCode();
			System.out.println("POST Response Code :: " + responseCode);
			
			if (responseCode == HttpURLConnection.HTTP_OK) { //success
				is = urlConnection.getInputStream();
				BufferedReader in = new BufferedReader(new InputStreamReader(is));
				String inputLine;
				StringBuffer response = new StringBuffer();

				while ((inputLine = in.readLine()) != null) {
					response.append(inputLine);
				}

				// print result
				System.out.println(response.toString());
				
				if(!"".equals(response.toString())) {
					
					System.out.println(response.toString());
					JSONArray array = new JSONArray(response.toString());
//					System.out.println(array);
					
					String sampleRate = "51200";
					
					StringBuilder sb = new StringBuilder();
					
					String recordStr = "0";
					
//					int record = 0;
//					int countData = 0;
					
					this.dataNoiseLst = new ArrayList<double[]>();
					
					countData = 0;
					
					for(Object obj : array) {
						
						JSONObject json = new JSONObject(obj.toString());  
						
						if(json.has("record")) {
							
							recordStr = json.get("record").toString();
							record = Integer.parseInt(recordStr);
						}
						
					}
					
				}
				
				
			} else {
				System.out.println("POST request not worked");
			}
			
		} catch (MalformedURLException e) {
			return null;
		} catch (IOException ex) {
			return null;
        }
		
		rtnMap.put("HttpURLConnection", urlConnection);
		rtnMap.put("OutputStream", os);
		rtnMap.put("InputStream", is);
		
		return rtnMap;	
	}
	
	public boolean closeConn() {
		
		try {
			if(os != null) {
				os.close();
				os = null;
			}
			if(is != null) {
				is.close();
				is = null;
			}
			if(urlConnection != null) {
				urlConnection.disconnect();
				urlConnection = null;
			}
		} catch (IOException e) {
			return false;
		}
		return true;
		
	}
	
	
	/**n
	 * æ­¤æ®µç¨‹å¼�ç¢¼ä¾†è‡ª SimProcess Class
	 *
	 */
	class GenerationThread implements Runnable {

		@Override
		public void run() {
			stillRunning = true;
			
			  while (dontStop) {
				generateData();
				/*
				 * this is the point we wait at for the other thread to
				 * get it's act together on a timer and use this data
				 * unit, then set it's reference to zero.
				 */
//				while (newDataUnits.getQueueSize() > acquisition_control.acquisitionParameters.nChannels*2) {
//					if (dontStop == false) break;
//				}
				try {
					Thread.sleep(2000);
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			  }
			
//			closeConnection();
			
			stillRunning = false;
		}

	}
	
	/*
	 * ç”¢ç”Ÿéš¨æ©Ÿè³‡æ–™çš„å¯¦ä½œæ–¹æ³•
	 */
	private void generateData() {
		
		RawDataUnit rdu;
		int nChan = 1;
		int phone = 0;
		int nSource = 0;
		int nSamples = params.sampleRate;
		
//		double nse = 6.924;
		double nse = 2.589;
		
		double[] channelData;
		long currentTimeMillis = startTimeMillis + totalSamples / 1000;
		channelData = new double[nSamples];
		
		
		
		if(!generateNoise(channelData, nse)) {
			JOptionPane.showMessageDialog(null, "Invalid Poseidoon Server Data !!");
		}
		
//		generateNoise1(channelData, nse);
		
		for(int i = 0; i< nChan; i++) {
			channelData = new double[nSamples];
			
//			double dbNse = 96.812;
			double dbNse = 20.25;
			
			System.out.println("Comparing data ..... ");
			System.out.println("Total data size => "+this.dataNoiseLst.size());
			for(int j = 0 ; j<this.countData && this.countData == this.dataNoiseLst.size() ; j++) {
				for(int k = j+1 ; k<this.countData && this.countData == this.dataNoiseLst.size() ; k++) {
					int same = 0;
					double[] one = this.dataNoiseLst.get(j);
					double[] two = this.dataNoiseLst.get(k);
					
					for(int m = 0;m<one.length;m++) {
						if(one[m] == two[m]) {
							same++;
						}
					}
					if(same == one.length) {
						System.out.println("find the same array : ( "+ j +" ," + k + " )");
					}
				}	
			}
			
			
			for(int j = 0 ; j<this.countData && this.countData == this.dataNoiseLst.size() ; j++) {
				rdu = new RawDataUnit(currentTimeMillis/2, 1<<i, totalSamples, nSamples/2);
				rdu.setRawData(this.dataNoiseLst.get(j), true);
		
				System.out.println("drawing "+(j+1)+" record data");
				
				
//				try {
//					this.fos.write("********(DRAWING DATA )*************\r\n".getBytes());
//					this.fos.write(Arrays.toString(this.dataNoiseLst.get(j)).subSequence(0, 600).toString().getBytes());
//					this.fos.write("\r\n*********************\r\n\r\n".getBytes());
//					this.fos.flush();
//				} catch (IOException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				}
				
				
				newDataUnits.addNewData(rdu, i);
				if(isFirst) {
					newDataUnits.addNewData(rdu, i);
					isFirst = false;
				}
					
				totalSamples += nSamples;
			}
			
			
		}
		
//		PamCalendar.setSoundFileTimeInMillis(totalSamples * 1000L / (long)nSamples);
//		currentTimeMillis += nSamples;
//		this.sp.updateObjectPositions(currentTimeMillis);
//		updateObjectPositions(currentTimeMillis);
//		totalSamples += nSamples;
	}
	
	
	private boolean openConnection() {
		
		String urlStr = this.params.uri;
		String getUrlStr = urlStr;
		
		if(!"".equals(this.timestamp.toString())) {
			getUrlStr += "?time_stamp=" + this.timestamp.toString();
		}
		else {
			getUrlStr = urlStr;
		}
		
		try {
			
			this.url = new URL(getUrlStr);
			this.conn = (HttpURLConnection) url.openConnection();
			
			
			// Request setup
			this.conn.setRequestMethod("GET");
			this.conn.setConnectTimeout(2000);// 5000 milliseconds = 5 seconds
			this.conn.setReadTimeout(2000);
			
			
			int status = this.conn.getResponseCode();
			
			if (status >= 300) {
				this.reader = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
			}
			else {
				this.reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
			}
			
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			JOptionPane.showMessageDialog(null, "Malformed URL !!");
			return false;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			JOptionPane.showMessageDialog(null, "fail to connect server !!");
			return false;
		}
		return true;
		
	}
	
	private boolean closeConnection() {
		
		try {
			this.reader.close();
			this.conn.disconnect();
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			JOptionPane.showMessageDialog(null, "fail to close connection !!");
			return false;
		}
		return true;
		
	}
	
	private void generateNoise1(double[] data, double noise) {
		
		Random rand = new Random();
		
		for(int i=0;i<51200;i++) {
			data[i] = rand.nextDouble();
//			data[i] = 0.125;
		}
		
		this.dataNoiseLst.add(data);
	}
	
	
	/**
	 * Generate noise on a data channel
	 * @param data data array to fill
	 * @param noise noise level 
	 */
	private boolean generateNoise(double[] data, double noise) {
		
		StringBuffer responseContent = new StringBuffer();
		String urlStr = this.params.uri;
		String getUrlStr = urlStr;
		
		try{
			
			
			if(!"".equals(this.timestamp.toString())) {
				getUrlStr = getUrlStr + "?time_stamp="+this.timestamp.toString();
			}
			URL url = new URL(getUrlStr);
			
			
			HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
			urlConnection.setRequestMethod("GET");
			urlConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
			urlConnection.setDoOutput(true);
			
			
			// Test if the response from the server is successful
			int responseCode = urlConnection.getResponseCode();
			
			if(responseCode == HttpURLConnection.HTTP_OK) {
				
				InputStream is = urlConnection.getInputStream();
				
				BufferedReader in = new BufferedReader(
						new InputStreamReader(is));
				String inputLine;
				
				while((inputLine = in.readLine()) != null) {
					responseContent.append(inputLine);
				}
				in.close();
			}
		
			
			if(!"".equals(responseContent.toString())) {
				
//				System.out.println(responseContent.toString());
				
				JSONArray array = new JSONArray(responseContent.toString());
				
				String sampleRate = "51200";
				
				StringBuilder sb = new StringBuilder();
				
				String recordStr = "0";
				
				
				this.dataNoiseLst = new ArrayList<double[]>();
				
				countData = 0;
				
				for(Object obj : array) {
					
					JSONObject json = new JSONObject(obj.toString());  
					
					if(json.has("record")) {
						
						recordStr = json.get("record").toString();
						record = Integer.parseInt(recordStr);
					}
					if(json.has("data")) {
						String dataContent = json.get("data").toString();
						
						
						JSONArray array1 = new JSONArray(dataContent);
						
						int i=array1.length()-1;
						for(Object obj1 : array1) {
							data[i] = Double.parseDouble(obj1.toString());
//							this.fos.write("--------------------------\r\n".getBytes());
//							this.fos.write(("data["+i+"] :" + data[i]).getBytes());
//							this.fos.write(" >> ".getBytes());
//							this.fos.flush();
							i--;
						}
						
						double[] copyData = new double[data.length];
						System.arraycopy(data, 0, copyData, 0, data.length);
						
						this.dataNoiseLst.add(copyData);
						
						
						sampleRate = json.get("fs").toString();
						System.out.println(sampleRate); 
						String timestamp_tmp = json.get("time_stamp").toString();  
						this.timestamp = timestamp_tmp.substring(0,23);
						
						
						countData++;
						
						System.out.println("The newest timestamp => " + timestamp);
						System.out.println("get "+ recordStr + " records data ");
						System.out.println("This phase data is index => " + countData );
						
					}
				    
				}
				
			}
			
		}
		catch (MalformedURLException e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(null, "Malformed URL !!");
			
		} catch (IOException e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(null, "fail to connect server !!");
		} 
		
		return record == countData;
		
	}
	

	class DataStreamThread implements Runnable {

		public void run() {
			System.out.println("THREAD START");
			POSMsgDaq.this.params.m_msgList_ch1.clear();
			POSMsgDaq.this.params.m_msgList_ch2.clear();
			POSMsgDaq.this.pam_stop = false;
			POSMsgDaq.this.pam_running = true;
			RawDataUnit[] arrayOfRawDataUnit = new RawDataUnit[2];
			double[][] arrayOfDouble = new double[2][9600];
			long l = 0L;
			while (!POSMsgDaq.this.pam_stop && POSMsgDaq.this.params.m_status) {
				try {
					arrayOfDouble[0] = POSMsgDaq.this.params.m_msgList_ch1.take();
					arrayOfDouble[1] = POSMsgDaq.this.params.m_msgList_ch2.take();
				} catch (InterruptedException interruptedException) {
					System.out.println("Exception Happen!!");
					break;
				}
				long l1 = POSMsgDaq.acquisition_control.getAcquisitionProcess().absSamplesToMilliseconds(l);
				for (byte b = 0; b < 2; b++) {
					arrayOfRawDataUnit[b] = new RawDataUnit(l1, b + 1, l, 9600L);
					arrayOfRawDataUnit[b].setRawData(arrayOfDouble[b]);
					POSMsgDaq.this.params.m_audioDataQueue.addNewData(arrayOfRawDataUnit[b]);
				}
				l += 9600L;
				System.out.println("TOTAL TIME:" + (float) l / 96000.0F);
			}
			POSMsgDaq.this.pam_running = false;
			System.out.println("THREAD END");
		}

	}

	public void update(PamObservable paramPamObservable, PamDataUnit paramPamDataUnit) {
	}

	public POSMsgDaq(AcquisitionControl paramAcquisitionControl) {
		System.out.println("POSMsgDaq init!!!");
		acquisition_control = paramAcquisitionControl;
		this.params.m_audioDataQueue = new AudioDataQueue();
		PamSettingManager.getInstance().registerSettings(this);
	}
}
