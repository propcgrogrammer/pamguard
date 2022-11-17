package PosBridge;

import Acquisition.AudioDataQueue;
import java.util.concurrent.BlockingQueue;
import org.java_websocket.client.WebSocketClient;

public class POSMsgParams {
	
  public WebSocketClient m_ws;
  
  public volatile boolean m_status = false;
  
  public AudioDataQueue m_audioDataQueue;
  
  public BlockingQueue<double[]> m_msgList_ch1;
  
  public BlockingQueue<double[]> m_msgList_ch2;
  
  public String uri = "";
  
  public int sampleRate = 0;

  public boolean isConnected = false;
  
}
