package rawDeepLearningClassifier.logging;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import PamUtils.PamArrayUtils;
import rawDeepLearningClassifier.dlClassification.PredictionResult;
import rawDeepLearningClassifier.dlClassification.animalSpot.SoundSpotResult;
import rawDeepLearningClassifier.dlClassification.dummyClassifier.DummyModelResult;
import rawDeepLearningClassifier.dlClassification.genericModel.GenericPrediction;
import rawDeepLearningClassifier.dlClassification.ketos.KetosResult;

/**
 * Handles the saving and loading of Model results from binary files. 
 * <p>
 * ModelResults are generated by a classifier and may have classifier-specific fields that need saved. 
 * The model results factory allows unique subclasses of ModelResult to save and load data different
 * data fields to binary fields. 
 * 
 * @author Jamie Macaulay 
 *
 */
public class ModelResultBinaryFactory {


	/**
	 * Flag for model res
	 */
	public static final int GENERIC = 0; 

	/**
	 * Flag for model res
	 */
	public static final int SOUND_SPOT = 1; 

	/**
	 * Flag for model res
	 */
	public static final int DUMMY_RESULT = 2; 
	
	/**
	 * Flag for model res
	 */
	public static final int KETOS = 3; 


	/**
	 * Write data to a binary output stream 
	 * @param modelResult - the model result to write. 
	 * @param dos
	 */
	public static void getPackedData(PredictionResult modelResult, DataOutputStream dos, int type) {

		float[] probabilities = modelResult.getPrediction(); 
		double maxVal = PamArrayUtils.max(probabilities); 
		double scale;
		if (maxVal > 0) {
			scale = (float) (32767./maxVal);			
		}
		else {
			scale = 1.;
		}
		/*
		 * Pretty minimilst write since channel map will already be stored in the
		 * standard header and data.length must match the channel map. 
		 */
		try {
			dos.writeByte(type);
			dos.writeBoolean(modelResult.isBinaryClassification());
			dos.writeFloat((float) scale);
			dos.writeShort(probabilities.length);
			for (int i = 0; i < probabilities.length; i++) {
				dos.writeShort((short) (scale*probabilities[i]));
			}

			if (modelResult.getClassNames()==null) {
				dos.writeShort(0);
			}
			else {
				dos.writeShort(modelResult.getClassNames().length);
				for (int i = 0; i < modelResult.getClassNames().length; i++) {
					dos.writeShort((short) modelResult.getClassNames()[i]);
				}
			}


			//specific settings for different modules 
			switch (type) {
			case SOUND_SPOT:
				//no extra info to write 
				break; 
			case KETOS:
				//no extra info to write beyond defaults
				break; 
			default:
				//no extra information. 
				break; 
			}

		} catch (IOException e) {
			e.printStackTrace();
		}
	}


	/**
	 * Read binary data and make a model result
	 * @param binaryObjectData
	 * @param bh
	 * @param moduleVersion
	 * @return
	 */
	public static PredictionResult sinkData(DataInputStream dis) {
		try {

			//System.out.println("Make model result: "); 

			int type = dis.readByte(); 
			boolean isBinary = dis.readBoolean(); 
			double scale = dis.readFloat();
			short nSpecies = dis.readShort(); 
			float[] data = new float[nSpecies];
			for (int i = 0; i < nSpecies; i++) {
				data[i] = (float) (dis.readShort() / scale);
			}
			
			//the class names. 
			int nClass =  dis.readShort(); 
			short[] classID = new short[nClass];
			for (int i = 0; i < nClass; i++) {
				classID[i] =  dis.readShort(); 
			}			
			//System.out.println("ModelResultBinaryFactory Type: " + type); 

			PredictionResult result; 
			//specific settings for different modules 
			switch (type) {
			case SOUND_SPOT:
				result = new SoundSpotResult(data, classID, isBinary);  
				break; 
			case DUMMY_RESULT:
				result = new DummyModelResult(data);  
				break; 
			case KETOS:
				result = new KetosResult(data);  
				break; 
			default:
				//ideally should never be used. 
				result = new GenericPrediction(data, isBinary); 
				break; 
			}

			//System.out.println("New model result: "+ type); 

			return result; 

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null; 
		}
	}

	/**
	 * Get the type flag for a model result. this is based on the class type. 
	 * @param modelResult - the model result 
	 * @return the type flag for the subclass of the result. 
	 */
	public static int getType(PredictionResult modelResult) {
		int type=0; 
		if (modelResult instanceof SoundSpotResult) {
			return SOUND_SPOT; 
		}
		if (modelResult instanceof KetosResult) {
			return KETOS; 
		}
		//must be last because this is often sub classed
		if (modelResult instanceof GenericPrediction) {
			return GENERIC; 
		}
		if (modelResult instanceof DummyModelResult) {
			return DUMMY_RESULT; 
		}

		return type;
	}

}