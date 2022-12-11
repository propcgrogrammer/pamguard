package PosBridge;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JOptionPane;

import org.json.JSONArray;
import org.json.JSONObject;

import Acquisition.AcquisitionControl;
import Acquisition.AcquisitionDialog;
import Acquisition.AudioDataQueue;
import Acquisition.DaqSystem;
import PamController.PamControlledUnitSettings;
import PamController.PamSettingManager;
import PamController.PamSettings;
import PamDetection.RawDataUnit;
import PamView.TopToolBar;
import PamguardMVC.PamDataUnit;
import PamguardMVC.PamObservable;
import PamguardMVC.PamObserver;

public class POSMsgDaq extends DaqSystem implements PamSettings, PamObserver {
	public static String plugin_name = "Poseidoon plugin";

	static AcquisitionControl acquisition_control;
	
	int sampleRate = 0;
	
	protected AudioDataQueue newDataUnits;
	
	private GenerationThread genThread;

	private Thread theThread;
	
	private volatile boolean dontStop;
	
	private volatile long startTimeMillis;
	
	private volatile long totalSamples;

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
	
	
	
	@Override
	public String getUnitName() {
		return acquisition_control.getUnitName();
	}

	@Override
	public long getRequiredDataHistory(PamObservable observable, Object arg) {
		return 0L;
	}

	@Override
	public void addData(PamObservable observable, PamDataUnit pamDataUnit) {
		// TODO Auto-generated method stub
	}

	@Override
	public void updateData(PamObservable observable, PamDataUnit pamDataUnit) {
	}

	@Override
	public void removeObservable(PamObservable observable) {
		System.out.println("QQQQQQQQQQQQQQQQQQQQQQQ");
	}

	@Override
	public void setSampleRate(float sampleRate, boolean notify) {
		acquisition_control.acquisitionParameters.sampleRate = sampleRate;
		this.sampleRate = (int) sampleRate;
	}

	@Override
	public void noteNewSettings() {
		System.out.println("ccccccc");
	}

	@Override
	public String getObserverName() {
		return null;
	}

	@Override
	public void masterClockUpdate(long milliSeconds, long sampleNumber) {
	}

	@Override
	public PamObserver getObserverObject() {
		return null;
	}

	@Override
	public void receiveSourceNotification(int type, Object object) {
		// TODO Auto-generated method stub
	}

	@Override
	public String getUnitType() {
		return "Poseidoon plugin_Params";
	}

	@Override
	public Serializable getSettingsReference() {
		return null;
	}

	@Override
	public long getSettingsVersion() {
		return 0L;
	}

	@Override
	public boolean restoreSettings(PamControlledUnitSettings pamControlledUnitSettings) {
		try {
		      System.out.println("restore settings");
		    } catch (Exception exception) {
		      System.out.println("exception happen");
		    } 
		    return false;
	}

	public String getSystemType() {
		return plugin_name;
	}

	public String getSystemName() {
		return plugin_name;
	}

	public JComponent getDaqSpecificDialogComponent(AcquisitionDialog acquisitionDialog) {
		if (this.ros_msg_daq_panel == null)
			this.ros_msg_daq_panel = new POSMsgDaqPanel(acquisitionDialog, this.params);
		return this.ros_msg_daq_panel;
	}
	
	public void setSelected(boolean paramBoolean) {}
	  
	  public void notifyModelChanged(int paramInt) {}

	public void dialogSetParams() {
	}

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
		 * prepare for the daq system
		 */
		this.newDataUnits = daqControl.getDaqProcess().getNewDataQueue();
		if (this.newDataUnits == null) return false;
		startTimeMillis = System.currentTimeMillis();
		totalSamples = 0;
		
		if (!this.params.m_status) {
			acquisition_control.getDaqProcess().pamStop();
			System.out.println("preparing system failed: connection status is false");
			return false;
		}
		acquisition_control = daqControl;
		this.params.m_audioDataQueue = acquisition_control.getDaqProcess().getNewDataQueue();
		return true;
	}

	@Override
	public boolean startSystem(AcquisitionControl daqControl) {
		
		/*
		 * startSystem Starts this daq system.
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
		setStreamStatus(STREAM_RUNNING);
		TopToolBar.enableStartButton(false);
		TopToolBar.enableStopButton(true);
		return true;
	}

	@Override
	public void stopSystem(AcquisitionControl daqControl) {
		dontStop = false;
		this.pam_stop = true;
	    TopToolBar.enableStartButton(true);
	    TopToolBar.enableStopButton(false);
		setStreamStatus(STREAM_CLOSED);
	}

	@Override
	public boolean isRealTime() {
		return true;
	}

	@Override
	public boolean canPlayBack(float sampleRate) {
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
	
	public boolean areSampleSettingsOk(int paramInt, float paramFloat) {
	    return true;
	}
	
	public void showSampleSettingsDialog(AcquisitionDialog paramAcquisitionDialog) {}
	
	public boolean supportsChannelLists() {
	    return false;
	}
	
	public double getChannelGain(int paramInt) {
	    return 0.0D;
	  }
	  
	  public int getSampleBits() {
	    return 24;
	  }
	  
	  public long getStallCheckSeconds() {
	    return 2L;
	  }
	  
	  public void dialogFXSetParams() {}
	  
	

	@Override
	public String getDeviceName() {
		return plugin_name;
	}
	
	/**
	 * the thread that can hold the process
	 */
	class GenerationThread implements Runnable {

		@Override
		public void run() {
			if(!params.isConnected){
				JOptionPane.showMessageDialog(null, "please reconnect to Poseidoon Server Data !!");
			}
			while (dontStop && params.isConnected) {
				generateData();
				/*
				 * this is the point we wait at for the other thread to
				 * get it's act together on a timer and use this data
				 * unit, then set it's reference to zero.
				 */
				try {
					Thread.sleep(500); // loop frequency is 0.5s
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
		}

	}
	
	/*
	 * generate the data
	 */
	private void generateData() {
		
		RawDataUnit rdu;
		int nChan = 1;
		int phone = 0;
		int nSource = 0;
		int nSamples = params.sampleRate;
		
		double nse = 2.589;
		
		double[] channelData;
		long currentTimeMillis = startTimeMillis + totalSamples / 1000;
		channelData = new double[nSamples];
		
		if(!generateNoise(channelData, nse)) {
			JOptionPane.showMessageDialog(null, "Invalid Poseidoon Server Data !!");
		}
		
		for(int i = 0; i< nChan; i++) {
			channelData = new double[nSamples];
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
				newDataUnits.addNewData(rdu, i);
				if(isFirst) {
					newDataUnits.addNewData(rdu, i);
					isFirst = false;
				}
				totalSamples += nSamples;
			}
			
			
		}
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
