package PosBridge;

import Acquisition.AcquisitionControl;
import Acquisition.DaqSystem;
import Acquisition.DaqSystemInterface;

public class POSMsgPlugin implements DaqSystemInterface {

	public String jarfile;
	  
	  public String getDefaultName() {
	    return "Poseidoon plugin";
	  }
	  
	  public String getHelpSetName() {
	    return null;
	  }
	  
	  public void setJarFile(String paramString) {
	    this.jarfile = this.jarfile;
	  }
	  
	  public String getJarFile() {
	    return this.jarfile;
	  }
	  
	  public String getDeveloperName() {
	    return "Shane";
	  }
	  
	  public String getContactEmail() {
	    return "XXXX@gmail.com";
	  }
	  
	  public String getVersion() {
	    return "1.00 Beta";
	  }
	  
	  public String getPamVerDevelopedOn() {
	    return "1.00 Beta";
	  }
	  
	  public String getPamVerTestedOn() {
	    return "1.00 Beta";
	  }
	  
	  public String getAboutText() {
	    return "This device can display spectrogram from the PAM buoy device using PAMGuard MVC";
	  }
	  
	  public DaqSystem createDAQControl(AcquisitionControl paramAcquisitionControl) {
	    return new POSMsgDaq(paramAcquisitionControl);
	  }
}
