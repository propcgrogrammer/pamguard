package dataModelFX.connectionNodes;

import dataModelFX.DataModelStyle;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import pamViewFX.fxGlyphs.PamGlyphDude;
import pamViewFX.fxGlyphs.PamSVGIcon;

/**
 * Handles module icons in PAMGuard.
 * <br>
 * This has two functions
 * 1) It keeps in JavaFX stuff out of the PamModel and in the GUI
 * 2) It means nodes can be created easily and copied for several GUI components. 
 * <br>
 * @author Jamie Macaulay
 *
 */
public class ModuleIconFactory {

	private static ModuleIconFactory instance; 

	private static int fontSize=20;

	/**
	 * Enum for module icons. 
	 * @author Jamie
	 *
	 */
	public enum ModuleIcon {
		DATAMAP, NMEA, GPS, MAP, SOUND_AQ, SOUND_OUTPUT, FFT, FILTER, CLICK, CLICK_TRAIN, RECORDER, WHISTLE_MOAN,
		NOISE_BAND, NOISE_FILT, DATABASE, BINARY, TIME_DISPLAY, DETECTION_DISPLAY, ARRAY, DEEP_LEARNING
	}

	/**
	 * Get the icon for a pamcontrolled unit. 
	 * @param icon - the enum of the controlled unit
	 * @return the icon for the controlled unit
	 */
	public  Node getModuleNode(ModuleIcon icon){
		switch (icon){
		case ARRAY:
			return new ImageView(new Image(getClass().getResourceAsStream("/Resources/modules/array_manager.png")));
		case BINARY:
//			return PamGlyphDude.createModuleGlyph(OctIcon.FILE_BINARY); 
			return PamGlyphDude.createModuleIcon("mdi2f-file-star-outline"); 
		case CLICK:
			return new ImageView(new Image(getClass().getResourceAsStream("/Resources/modules/click.png")));
		case CLICK_TRAIN:
			return new ImageView(new Image(getClass().getResourceAsStream("/Resources/modules/clicktrain.png")));
		case DATABASE:
//			return PamGlyphDude.createModuleGlyph(FontAwesomeIcon.DATABASE);
			return PamGlyphDude.createModuleIcon("mdi2d-database");
		case DATAMAP:
			return new ImageView(new Image(getClass().getResourceAsStream("/Resources/modules/dataMap.png")));
		case FFT:
			return new ImageView(new Image(getClass().getResourceAsStream("/Resources/modules/fft.png")));
		case FILTER:
			return new ImageView(new Image(getClass().getResourceAsStream("/Resources/modules/filters.png")));
		case GPS:
//			return PamGlyphDude.createModuleGlyph(MaterialIcon.GPS_FIXED); 
			return PamGlyphDude.createModuleIcon("mdi2c-crosshairs-gps"); 
		case MAP:
//			return PamGlyphDude.createModuleGlyph(MaterialIcon.MAP);
			return PamGlyphDude.createModuleIcon("mdi2m-map");
		case NMEA:
			return createNMEASymbol();
		case NOISE_BAND:
			return new ImageView(new Image(getClass().getResourceAsStream("/Resources/modules/filterdNoiseMeasurementBank.png")));
		case NOISE_FILT:
			return new ImageView(new Image(getClass().getResourceAsStream("/Resources/modules/filterdNoiseMeasurement.png")));
		case RECORDER:
			return new ImageView(new Image(getClass().getResourceAsStream("/Resources/modules/recorder.png")));
		case SOUND_AQ:
			return getSVGIcon("/Resources/modules/noun_Soundwave_1786340.svg");
			//return new ImageView(new Image(getClass().getResourceAsStream("/Resources/modules/aquisition.png")));
		case SOUND_OUTPUT:
//			return PamGlyphDude.createModuleGlyph(MaterialDesignIcon.HEADPHONES); 
			return PamGlyphDude.createModuleIcon("mdi2h-headphones"); 
			//return new ImageView(new Image(getClass().getResourceAsStream("/Resources/modules/playback.png")));
		case WHISTLE_MOAN:
			return new ImageView(new Image(getClass().getResourceAsStream("/Resources/modules/whistles.png")));
		case TIME_DISPLAY:
			return new ImageView(new Image(getClass().getResourceAsStream("/Resources/modules/timeDisplay.png"))); 
		case DETECTION_DISPLAY: 
			return new ImageView(new Image(getClass().getResourceAsStream("/Resources/modules/detectionDisplay.png")));
		case DEEP_LEARNING:
			
			//System.out.println("------GET THE SVG ICON FOR DEEP LEARNING--------");
			return getSVGIcon("/Resources/modules/noun_Deep Learning_2486374.svg"); 
		default:
			break;

		}
		return null;
	}; 

	/**
	 * Get an SVG icon.
	 * @param resourcePath - the path from the src folder
	 * @return a node for the SVG icon. 
	 */
	private Node getSVGIcon(String resourcePath) {
		try {
			
			PamSVGIcon iconMaker= new PamSVGIcon(); 
			PamSVGIcon svgsprite = iconMaker.create(getClass().getResource(resourcePath).toURI().toURL(), Color.BLACK);
			svgsprite.getSpriteNode().setStyle("-fx-text-color: black");				
			svgsprite.getSpriteNode().setStyle("-fx-fill: black");
			svgsprite.setFitHeight(DataModelStyle.iconSize-10);
			svgsprite.setFitWidth(DataModelStyle.iconSize-10);
			return svgsprite.getSpriteNode(); 
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		return null; 
	}


	private Node createNMEASymbol(){
		Font font = new Font(fontSize);
		Text label = new Text("NMEA");
		label.setFill(Color.BLACK);
		label.setFont(font);
		StackPane stackPane=new StackPane();
//		stackPane.getChildren().addAll(PamGlyphDude.createModuleGlyph(FontAwesomeIcon.FILE_ALT),label);
		stackPane.getChildren().addAll(PamGlyphDude.createModuleIcon("mdi2f-file-document-outline"),label);
		return stackPane;
	}

	/**
	 * Get instance of the ModuleFactory.
	 * @return
	 */
	public static ModuleIconFactory getInstance(){
		if (instance==null){
			instance=new ModuleIconFactory(); 
		}
		return instance; 
	}

	/**
	 * Get the icon for a pam controlled unit. 
	 * @param icon - the class name string of the controlled unit. 
	 * @return the icon for the controlled unit
	 */
	public Node getModuleNode(String className) {
		ModuleIcon icon = getModuleIcon(className); 
		if (icon==null) return null; 
		else return getModuleNode(icon); 
	}

	/**
	 * Get the module icon for a module class. 
	 * @param className - the class name. 
	 * @return the module icon enum
	 */
	public ModuleIcon getModuleIcon(String className) {
		//System.out.println("CLASS NAME: " + className);
		ModuleIcon icon = null; 
		switch (className) {
		case "Acquisition.AcquisitionControl":
			icon=ModuleIcon.SOUND_AQ; 
			break; 
		case "ArrayManager":
			icon=ModuleIcon.ARRAY; 
			break; 
		case "fftManager.PamFFTControl":
			icon=ModuleIcon.FFT; 
			break; 
		case "Filters.FilterControl":
			icon=ModuleIcon.FILTER; 
			break; 
		case "binaryFileStorage.BinaryStore":
			icon=ModuleIcon.BINARY; 
			break; 
		case "generalDatabase.DBControlUnit":
			icon=ModuleIcon.DATABASE; 
			break; 
		case "whistlesAndMoans.WhistleMoanControl":
			icon=ModuleIcon.WHISTLE_MOAN; 
			break; 
		case "clickDetector.ClickControl":
			icon=ModuleIcon.CLICK; 
			break; 
		case "clickTrainDetector.ClickTrainControl":
			icon=ModuleIcon.CLICK_TRAIN; 
			break; 
		case "dataPlotsFX.TDDisplayController":
			icon=ModuleIcon.TIME_DISPLAY; 
			break; 
		case "detectionPlotFX.DetectionDisplayControl":
			icon=ModuleIcon.DETECTION_DISPLAY; 
			break; 
		case "dataMap.DataMapControl":
			icon=ModuleIcon.DATAMAP; 
			break; 
		case "soundPlayback.PlaybackControl":
			icon=ModuleIcon.SOUND_OUTPUT; 
			break; 
		case "rawDeepLearningClassifier.DLControl":
			icon=ModuleIcon.DEEP_LEARNING; 
			break; 
		}
		return icon;
	}

}
