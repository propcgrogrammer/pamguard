package PosBridge;

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
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;

import org.json.JSONArray;
import org.json.JSONObject;

import Acquisition.AcquisitionDialog;
import PamView.dialog.PamGridBagContraints;

public class POSMsgDaqPanel extends JPanel {
  private AcquisitionDialog acquisition_dialog;
  
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
    ((GridBagConstraints)pamGridBagContraints).gridx++;
    this.p_ros.add(this.b_connect = new JButton("connect"), pamGridBagContraints);
    this.b_connect.setEnabled(true);
    this.b_connect.addActionListener(new ActionListener() {
    	
    	public void actionPerformed(ActionEvent param1ActionEvent) {
    		
    		String str = "http://"+POSMsgDaqPanel.this.tf_server.getText()+":"
    				+ POSMsgDaqPanel.this.tf_topic.getText();
    		
                
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
        						
        						POSMsgDaqPanel.this.params.isConnected = true;
        						POSMsgDaqPanel.this.b_connect.setEnabled(false);
        			      POSMsgDaqPanel.this.b_disconnect.setEnabled(true);
        					}else {
        						JOptionPane.showMessageDialog(null, "Fail to connect Poseidoon Server !!");
        					}
        				}
        				
        				params.sampleRate = Integer.parseInt(sampleRate);
        				acquisition_dialog.setSampleRate(Float.parseFloat(sampleRate));
        				acquisition_dialog.getSampleRateComponent().setEditable(false);
        				
        			}
        			
        		}catch (MalformedURLException e) {
        			e.printStackTrace();
        			JOptionPane.showMessageDialog(null, "Malformed URL !!");
        			
        		} catch (IOException e) {
        			e.printStackTrace();
        			JOptionPane.showMessageDialog(null, "fail to connect server !!");
        			
        		}finally {
        			conn.disconnect();
        		}
        
    	}
    });
    
    ((GridBagConstraints)pamGridBagContraints).gridx++;
    this.p_ros.add(this.b_disconnect = new JButton("disconnect"), pamGridBagContraints);
    this.b_disconnect.setEnabled(true);
    this.b_disconnect.addActionListener(new ActionListener() {
    	public void actionPerformed(ActionEvent param1ActionEvent) {
    		POSMsgDaqPanel.this.params.isConnected = false;
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
