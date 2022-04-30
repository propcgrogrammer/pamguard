package RosBridge;

import Acquisition.AudioDataQueue;
import java.util.concurrent.BlockingQueue;
import org.java_websocket.client.WebSocketClient;

public class ROSMsgParams {
	
  public WebSocketClient m_ws;
  
  public volatile boolean m_status = false;
  
  public AudioDataQueue m_audioDataQueue;
  
  public BlockingQueue<double[]> m_msgList_ch1;
  
  public BlockingQueue<double[]> m_msgList_ch2;
}
