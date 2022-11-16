package PosBridge;

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
import javax.swing.JOptionPane;
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

public class POSMsgDaqPanel extends JPanel {
  private AcquisitionDialog acquisition_dialog;
  
  private boolean isConnected = false;
  
  private POSMsgParams params;
  
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
  
  public POSMsgDaqPanel(AcquisitionDialog paramAcquisitionDialog, final POSMsgParams params) {
	  
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
    
    
    acquisition_dialog.getnChanComponent().setEditable(false);
    
    
    this.p_ros = new JPanel(new GridBagLayout());
    PamGridBagContraints pamGridBagContraints = new PamGridBagContraints();
    this.p_ros.setBorder(new TitledBorder("Poseidoon Server Settings"));
    this.p_ros.add(this.label_server = new JLabel("IP", 4), pamGridBagContraints);
    ((GridBagConstraints)pamGridBagContraints).gridx++;
    this.p_ros.add(this.tf_server = new JTextField("127.0.0.1", 25), pamGridBagContraints);
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
//    this.p_ros.add(this.tf_status = new JTextField("WebSocket Client Disconnected", 20), pamGridBagContraints);
//    this.tf_status.setEditable(true);
    ((GridBagConstraints)pamGridBagContraints).gridx++;
    this.p_ros.add(this.b_connect = new JButton("connect"), pamGridBagContraints);
    this.b_connect.setEnabled(true);
    this.b_connect.addActionListener(new ActionListener() {
    	
    	public void actionPerformed(ActionEvent param1ActionEvent) {
    		
    		String str = "http://"+POSMsgDaqPanel.this.tf_server.getText()+":"
    				+ POSMsgDaqPanel.this.tf_topic.getText();
    		
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
        		
        		String urlStr = "http://"+POSMsgDaqPanel.this.tf_server.getText()+":"
        				+ POSMsgDaqPanel.this.tf_topic.getText()
        				+ POSMsgDaqPanel.this.tf_msg.getText();
        		
        		String urlConn = "http://"+POSMsgDaqPanel.this.tf_server.getText()+":"
        				+ POSMsgDaqPanel.this.tf_topic.getText()
        				+ "/connect_pamguard/";
        		
        		params.uri = urlStr;
        		
        		try{
        			
        			URL url = new URL(urlConn);
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
        			
        			
        			
        			
        			if(!"".equals(responseContent.toString())) {
        				
        				System.out.println(responseContent.toString());
        				
//        				JSONArray array = new JSONArray(responseContent.toString());
//        				System.out.println(array);
        				
        				String sampleRate = "51200";
        				String connStat = "fail";
        				
        				StringBuilder sb = new StringBuilder();
        				
        				JSONObject json = new JSONObject(responseContent.toString());
        				
        				if(json.has("fs")) {
        					
        					sampleRate = json.get("fs").toString();
        					
        				}
        				
        				
        				if(json.has("status")) {
        					
        					connStat = json.get("status").toString();
        					if("success".equals(connStat)) {
        						JOptionPane.showMessageDialog(null, "Connected to Poseidoon Server Successfully !!");
        						isConnected = true;
        						POSMsgDaqPanel.this.b_connect.setEnabled(false);
        			        	POSMsgDaqPanel.this.b_disconnect.setEnabled(true);
        						
        					}else {
        						JOptionPane.showMessageDialog(null, "Fail to connect Poseidoon Server !!");
        					}
        				}
        				
        				
        				
//        				for(Object obj : array) {
//        					
//        					JSONObject json = new JSONObject(obj.toString());  
//        					
//        					if(json.has("record")) {
//        						
//        						String recordStr = json.get("record").toString();
//        						int record = Integer.parseInt(recordStr);
//        					}
//        					if(json.has("data")) {
//        						String data = json.get("data").toString();
//        						JSONArray array1 = new JSONArray(data);
//        					
//        						for(Object obj1 : array1) {
//        							sb.append(obj1.toString()).append("\n");
//        						}
//        					
//        						sampleRate = json.get("fs").toString();
//        						System.out.println(sampleRate); 
//        					}
//        					
//        				}
        				
//        				JSONObject json = new JSONObject(responseContent.toString());  
//        				String data = json.get("data").toString();
//        				JSONArray array = new JSONArray(data);
//        				String sampleRate = json.get("fs").toString();  
//        				System.out.println(sampleRate);  
        				
        				params.sampleRate = Integer.parseInt(sampleRate);
        				acquisition_dialog.setSampleRate(Float.parseFloat(sampleRate));
        				acquisition_dialog.getSampleRateComponent().setEditable(false);
        				
        				
//        				JFrame frame = new JFrame("Server Data");
//        				frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);      				
//        				JTextArea contentTxtArea = new JTextArea(sb.toString()); 
//        				contentTxtArea.setEditable(false);
//        				JScrollPane jsp = new JScrollPane(contentTxtArea);
//        				frame.getContentPane().add(jsp, BorderLayout.CENTER);
//        				frame.setSize(800, 600);
//        				frame.setLocationRelativeTo(null);
//        				frame.setResizable(true);
//        				frame.setVisible(true);
        				
//        				JFrame frame = new JFrame("Connect Successful");
//        				frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
//        				JTextArea contentTxtArea = new JTextArea("Connected to Poseidoon Server"); 
//        				contentTxtArea.setEditable(false);
//        				JScrollPane jsp = new JScrollPane(contentTxtArea);
//        				frame.getContentPane().add(jsp, BorderLayout.CENTER);
//        				frame.setSize(400, 200);
//        				frame.setLocationRelativeTo(null);
//        				frame.setResizable(true);
//        				frame.setVisible(true);
        				
        				
        			}
        			
        		}
        		catch (MalformedURLException e) {
        			e.printStackTrace();
        			JOptionPane.showMessageDialog(null, "Malformed URL !!");
        			
        		} catch (IOException e) {
        			e.printStackTrace();
        			JOptionPane.showMessageDialog(null, "fail to connect server !!");
        			
        		}finally {
        			conn.disconnect();
        		}
        		
				
             } 
    		
    	}
    });
    
    
    
    
    ((GridBagConstraints)pamGridBagContraints).gridx++;
    this.p_ros.add(this.b_disconnect = new JButton("disconnect"), pamGridBagContraints);
    this.b_disconnect.setEnabled(true);
    this.b_disconnect.addActionListener(new ActionListener() {
    	public void actionPerformed(ActionEvent param1ActionEvent) {
    		
    		isConnected = false;
			POSMsgDaqPanel.this.b_connect.setEnabled(true);
        	POSMsgDaqPanel.this.b_disconnect.setEnabled(false);
        	JOptionPane.showMessageDialog(null, "Disconnected from Poseidoon Server Successfully !!");
			
    	}
    });
    
    ((GridBagConstraints)pamGridBagContraints).gridx = 0;
    ((GridBagConstraints)pamGridBagContraints).gridy++;
    ((GridBagConstraints)pamGridBagContraints).insets = new Insets(10, 2, 2, 2);
    this.p_ros.add(this.cb_lock = new JCheckBox("Locked", true), pamGridBagContraints);
    this.cb_lock.addItemListener(new ItemListener() {
          public void itemStateChanged(ItemEvent param1ItemEvent) {
            if (param1ItemEvent.getStateChange() == 1) {
              System.out.println("lock the layout");
              POSMsgDaqPanel.this.tf_msg.setEditable(false);
              POSMsgDaqPanel.this.tf_topic.setEditable(false);
              POSMsgDaqPanel.this.tf_server.setEditable(false);
            } 
            if (param1ItemEvent.getStateChange() == 2) {
              System.out.println("free the layout");
              POSMsgDaqPanel.this.tf_msg.setEditable(true);
              POSMsgDaqPanel.this.tf_topic.setEditable(true);
              POSMsgDaqPanel.this.tf_server.setEditable(true);
            } 
          }
        });
    add(this.p_ros);
  }
}
