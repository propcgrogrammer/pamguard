package RosBridge;

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

import java.awt.BorderLayout;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.BlockingQueue;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import org.json.JSONArray;
import org.json.JSONObject;

public class ROSMsgDaq extends DaqSystem implements PamSettings, PamObserver {
	public static String plugin_name = "ROS Msg DAQ plugin";

	static AcquisitionControl acquisition_control;
	
	/*
	 * Server抓到的資料當成 AudioFile 產生的音訊，並放到 AudioDataQueue 裡面
	 */
	protected AudioDataQueue newDataUnits;
	
	private GenerationThread genThread;

	private Thread theThread;
	
	private volatile boolean dontStop;

	private volatile boolean stillRunning;
	
	private volatile long startTimeMillis;
	
	private volatile long totalSamples;
	
	private int dataUnitSamples;

	private ROSMsgDaqPanel ros_msg_daq_panel;

	private ROSMsgParams params = new ROSMsgParams();

	private volatile boolean pam_stop;

	private volatile boolean pam_running;

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
		// TODO Auto-generated method stub

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
			this.ros_msg_daq_panel = new ROSMsgDaqPanel(acquisitionDialog, this.params);
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
		 * 此部分程式碼來自 SimProcess Class
		 * 目的為準備一個Thread將抓到的資料放到該Thread裡面做模擬
		 */
		newDataUnits = daqControl.getDaqProcess().getNewDataQueue();
		genThread = new GenerationThread();
		theThread = new Thread(genThread);
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
		return true;
	}

	@Override
	public boolean startSystem(AcquisitionControl daqControl) {
		
		/*
		 * 此部分程式碼來自 SimProcess Class
		 * 執行該Thread裡面的資料
		 */
		dontStop = true;

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
		setStreamStatus(2);
		TopToolBar.enableStartButton(false);
		TopToolBar.enableStopButton(true);
		return true;
	}

	@Override
	public void stopSystem(AcquisitionControl daqControl) {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean isRealTime() {
		// TODO Auto-generated method stub
		return false;
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
	
	/**
	 * 此段程式碼來自 SimProcess Class
	 *
	 */
	class GenerationThread implements Runnable {

		@Override
		public void run() {
			stillRunning = true;
//			generateData();
		
			
			while (dontStop) {
				generateData();
				/*
				 * this is the point we wait at for the other thread to
				 * get it's act together on a timer and use this data
				 * unit, then set it's reference to zero.
				 */
				while (newDataUnits.getQueueSize() > acquisition_control.acquisitionParameters.nChannels*2) {
					if (dontStop == false) break;
					try {
						Thread.sleep(2);
					} catch (Exception ex) {
						ex.printStackTrace();
					}
				}
			}
			
			stillRunning = false;
		}

	}
	
	
	/*
	 * 產生隨機資料的實作方法
	 */
	private void generateData() {
		
		RawDataUnit rdu;
		int nChan = 1;
		int phone = 0;
		int nSource = 0;
		int nSamples = 51200;
		
//		double nse = 6.924;
		double nse = 2.589;
		
		double[] channelData;
		long currentTimeMillis = startTimeMillis + totalSamples / 1000;
		
		int totalSamples = 0;
		
		for(int i = 0; i< nChan; i++) {
			channelData = new double[nSamples];
			
//			double dbNse = 96.812;
			double dbNse = 20.25;
			generateNoise(channelData, nse);
			
			rdu = new RawDataUnit(currentTimeMillis, 1<<i, totalSamples, nSamples);
			rdu.setRawData(channelData, true);
			newDataUnits.addNewData(rdu, i);
			
		}
		
	}
	
	/**
	 * Generate noise on a data channel
	 * @param data data array to fill
	 * @param noise noise level 
	 */
	private void generateNoise(double[] data, double noise) {
		
		HttpURLConnection conn = null;
        
        BufferedReader reader;
		String line;
		StringBuilder responseContent = new StringBuilder();
		
		String urlStr = this.params.uri;
		
		try{
			
			URL url = new URL(urlStr);
			conn = (HttpURLConnection) url.openConnection();
			
			// Request setup
			conn.setRequestMethod("GET");
			conn.setConnectTimeout(5000);// 5000 milliseconds = 5 seconds
			conn.setReadTimeout(5000);
			
			// Test if the response from the server is successful
			int status = conn.getResponseCode();
			
			if (status >= 300) {
				reader = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
				while ((line = reader.readLine()) != null) {
					responseContent.append(line);
				}
				reader.close();
			}
			else {
				reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
				while ((line = reader.readLine()) != null) {
					responseContent.append(line).append("\n");
				}
				reader.close();
			}
			
			System.out.println(responseContent.toString());
			
			
			if(!"".equals(responseContent.toString())) {
				
				
				JSONObject json = new JSONObject(responseContent.toString());  
				String dataContent = json.get("data").toString();
				JSONArray array = new JSONArray(dataContent);
 
				StringBuilder sb = new StringBuilder();

				int i=0;
				for(Object obj : array) {
					data[i] = Double.parseDouble(obj.toString());
					i++;
				}
				
				
			}
			
		}
		catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}finally {
			conn.disconnect();
		}
		
	}
	

	class DataStreamThread implements Runnable {

		public void run() {
			System.out.println("THREAD START");
			ROSMsgDaq.this.params.m_msgList_ch1.clear();
			ROSMsgDaq.this.params.m_msgList_ch2.clear();
			ROSMsgDaq.this.pam_stop = false;
			ROSMsgDaq.this.pam_running = true;
			RawDataUnit[] arrayOfRawDataUnit = new RawDataUnit[2];
			double[][] arrayOfDouble = new double[2][9600];
			long l = 0L;
			while (!ROSMsgDaq.this.pam_stop && ROSMsgDaq.this.params.m_status) {
				try {
					arrayOfDouble[0] = ROSMsgDaq.this.params.m_msgList_ch1.take();
					arrayOfDouble[1] = ROSMsgDaq.this.params.m_msgList_ch2.take();
				} catch (InterruptedException interruptedException) {
					System.out.println("Exception Happen!!");
					break;
				}
				long l1 = ROSMsgDaq.acquisition_control.getAcquisitionProcess().absSamplesToMilliseconds(l);
				for (byte b = 0; b < 2; b++) {
					arrayOfRawDataUnit[b] = new RawDataUnit(l1, b + 1, l, 9600L);
					arrayOfRawDataUnit[b].setRawData(arrayOfDouble[b]);
					ROSMsgDaq.this.params.m_audioDataQueue.addNewData(arrayOfRawDataUnit[b]);
				}
				l += 9600L;
				System.out.println("TOTAL TIME:" + (float) l / 96000.0F);
			}
			ROSMsgDaq.this.pam_running = false;
			System.out.println("THREAD END");
		}

	}

	public void update(PamObservable paramPamObservable, PamDataUnit paramPamDataUnit) {
	}

	public ROSMsgDaq(AcquisitionControl paramAcquisitionControl) {
		System.out.println("ROSMsgDaq init!!!");
		acquisition_control = paramAcquisitionControl;
		this.params.m_audioDataQueue = new AudioDataQueue();
		PamSettingManager.getInstance().registerSettings(this);
	}
}
