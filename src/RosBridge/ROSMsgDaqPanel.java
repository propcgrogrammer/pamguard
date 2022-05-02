package RosBridge;

import Acquisition.AcquisitionDialog;
import Map.MapUtils;
import PamView.dialog.PamGridBagContraints;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft;
import org.java_websocket.drafts.Draft_6455;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.*;

public class ROSMsgDaqPanel extends JPanel {
  private AcquisitionDialog acquisition_dialog;
  
  private ROSMsgParams params;
  
  private JPanel p_ros;
  
  private JLabel label_server;
  
  private JLabel label_topic;
  
  private JLabel label_msg;
  
  private JTextField tf_server;
  
  private JTextField tf_topic;
  
  private JTextField tf_msg;
  
  private JTextField tf_status;
  
  private JCheckBox cb_lock;
  
  private JButton b_connect;
  
  private JButton b_disconnect;
  
  private JButton button_ok;
  
  private JButton button_cancel;
  
  public ROSMsgDaqPanel(AcquisitionDialog paramAcquisitionDialog, final ROSMsgParams params) {
    this.acquisition_dialog = paramAcquisitionDialog;
    this.params = params;
    this.button_ok = paramAcquisitionDialog.getOkButton();
    this.button_ok.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent param1ActionEvent) {
            System.out.println("Ok button pressed");
          }
        });
    this.button_cancel = paramAcquisitionDialog.getCancelButton();
    this.button_cancel.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent param1ActionEvent) {
            System.out.println("Cancel button pressed");
          }
        });
    setLayout(new BoxLayout(this, 1));
    this.p_ros = new JPanel(new GridBagLayout());
    PamGridBagContraints pamGridBagContraints = new PamGridBagContraints();
    this.p_ros.setBorder(new TitledBorder("ROS Bridge Server Settings"));
    this.p_ros.add(this.label_server = new JLabel("IP", 4), pamGridBagContraints);
    ((GridBagConstraints)pamGridBagContraints).gridx++;
    this.p_ros.add(this.tf_server = new JTextField("http://127.0.0.1", 25), pamGridBagContraints);
    this.tf_server.setEditable(true);
    ((GridBagConstraints)pamGridBagContraints).gridx = 0;
    ((GridBagConstraints)pamGridBagContraints).gridy++;
    this.p_ros.add(this.label_topic = new JLabel("PORT", 4), pamGridBagContraints);
    ((GridBagConstraints)pamGridBagContraints).gridx++;
    this.p_ros.add(this.tf_topic = new JTextField("8000"), pamGridBagContraints);
    this.tf_topic.setEditable(true);
    ((GridBagConstraints)pamGridBagContraints).gridx = 0;
    ((GridBagConstraints)pamGridBagContraints).gridy++;
    this.p_ros.add(this.label_msg = new JLabel("Server Data Path", 4), pamGridBagContraints);
    ((GridBagConstraints)pamGridBagContraints).gridx++;
    this.p_ros.add(this.tf_msg = new JTextField("/raw_data/"), pamGridBagContraints);
    this.tf_msg.setEditable(true);
    ((GridBagConstraints)pamGridBagContraints).gridx = 0;
    ((GridBagConstraints)pamGridBagContraints).gridy++;
    this.p_ros.add(this.tf_status = new JTextField("WebSocket Client Disconnected", 20), pamGridBagContraints);
    this.tf_status.setEditable(true);
    ((GridBagConstraints)pamGridBagContraints).gridx++;
    this.p_ros.add(this.b_connect = new JButton("connect"), pamGridBagContraints);
    this.b_connect.setEnabled(true);
    this.b_connect.addActionListener(new ActionListener() {
    	
    	public void actionPerformed(ActionEvent param1ActionEvent) {
    		
    		String str = ROSMsgDaqPanel.this.tf_server.getText()+":"
    				+ ROSMsgDaqPanel.this.tf_topic.getText();
    		
    		try {
    			
    			params.m_msgList_ch1 = (BlockingQueue)new LinkedBlockingQueue<>();
                params.m_msgList_ch2 = (BlockingQueue)new LinkedBlockingQueue<>();
                
                params.m_ws = new WebSocketClient(new URI(str), (Draft)new Draft_6455()) {
                	public void onMessage(String param2String) {
                		
                		JSONObject jSONObject1 = new JSONObject(param2String);
                        JSONObject jSONObject2 = (JSONObject)jSONObject1.get("msg");
                        JSONArray jSONArray1 = jSONObject2.getJSONArray("data_ch1");
                        JSONArray jSONArray2 = jSONObject2.getJSONArray("data_ch2");
                        double[] arrayOfDouble1 = new double[jSONArray1.length()];
                        double[] arrayOfDouble2 = new double[jSONArray1.length()];
                        
                        for (byte b = 0; b < jSONArray1.length(); b++) {
                            arrayOfDouble1[b] = jSONArray1.getDouble(b);
                            arrayOfDouble2[b] = jSONArray2.getDouble(b);
                        } 
                        
                        try {
                            params.m_msgList_ch1.put(arrayOfDouble1);
                            params.m_msgList_ch2.put(arrayOfDouble2);
                        } catch (Exception exception) {
                            System.out.println("exception happened in onMessage method");
                        } 
                        
                	}
                	
                	public void onOpen(ServerHandshake param2ServerHandshake) {
                        System.out.println("connection opened");
                      }
                      
                      public void onClose(int param2Int, String param2String, boolean param2Boolean) {
                        System.out.println("connection closed");
                      }
                      
                      public void onError(Exception param2Exception) {
                        param2Exception.printStackTrace();
                      }
                	
                };
                
                params.m_status = true;
 
    		}catch (URISyntaxException uRISyntaxException) {
                System.out.println(uRISyntaxException);
            } 
    		
    		if (params.m_status) {
                params.m_ws.connect();
                try {
                  Thread.sleep(1000L);
                } catch (Exception exception) {
                  System.out.println(exception);
                } 
                
                HttpURLConnection conn = null;
                
                BufferedReader reader;
        		String line;
        		StringBuilder responseContent = new StringBuilder();
        		
        		String urlStr = ROSMsgDaqPanel.this.tf_server.getText()+":"
        				+ ROSMsgDaqPanel.this.tf_topic.getText()
        				+ ROSMsgDaqPanel.this.tf_msg.getText();
        		
        		params.uri = urlStr;
        		
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
        				String data = json.get("data").toString();
        				JSONArray array = new JSONArray(data);
//        				String data = json.getString("data");  
//        				System.out.println(data);  
        				StringBuilder sb = new StringBuilder();

        				for(Object obj : array) {
        					sb.append(obj.toString()).append("\n");
        				}
        				
        				JFrame frame = new JFrame("Server Data");
        				frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        				
        				JTextArea contentTxtArea = new JTextArea(sb.toString()); 
        				contentTxtArea.setEditable(false);
        				JScrollPane jsp = new JScrollPane(contentTxtArea);
        				frame.getContentPane().add(jsp, BorderLayout.CENTER);
        				frame.setSize(800, 600);
        				frame.setLocationRelativeTo(null);
        				frame.setResizable(true);
        				frame.setVisible(true);
        				
        			}
        			
        		}
        		catch (MalformedURLException e) {
        			e.printStackTrace();
        		} catch (IOException e) {
        			e.printStackTrace();
        		}finally {
        			conn.disconnect();
        		}
        		
                
                ROSMsgDaqPanel.this.b_connect.setEnabled(false);
                ROSMsgDaqPanel.this.tf_status.setText("WebSocket Connection Successful");
                ROSMsgDaqPanel.this.b_disconnect.setEnabled(true);
             } 
    		
    	}
    });
    
    
    
    
    ((GridBagConstraints)pamGridBagContraints).gridx++;
    this.p_ros.add(this.b_disconnect = new JButton("disconnect"), pamGridBagContraints);
    this.b_disconnect.setEnabled(false);
    
    ((GridBagConstraints)pamGridBagContraints).gridx = 0;
    ((GridBagConstraints)pamGridBagContraints).gridy++;
    ((GridBagConstraints)pamGridBagContraints).insets = new Insets(10, 2, 2, 2);
    this.p_ros.add(this.cb_lock = new JCheckBox("Locked", true), pamGridBagContraints);
    this.cb_lock.addItemListener(new ItemListener() {
          public void itemStateChanged(ItemEvent param1ItemEvent) {
            if (param1ItemEvent.getStateChange() == 1) {
              System.out.println("lock the layout");
              ROSMsgDaqPanel.this.tf_msg.setEditable(false);
              ROSMsgDaqPanel.this.tf_topic.setEditable(false);
              ROSMsgDaqPanel.this.tf_server.setEditable(false);
            } 
            if (param1ItemEvent.getStateChange() == 2) {
              System.out.println("free the layout");
              ROSMsgDaqPanel.this.tf_msg.setEditable(true);
              ROSMsgDaqPanel.this.tf_topic.setEditable(true);
              ROSMsgDaqPanel.this.tf_server.setEditable(true);
            } 
          }
        });
    add(this.p_ros);
  }
}
