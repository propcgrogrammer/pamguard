package cpod;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.io.FilenameUtils;

import PamController.PamController;
import PamUtils.PamCalendar;
import PamView.dialog.warn.WarnOnce;
import binaryFileStorage.BinaryDataSource;
import binaryFileStorage.BinaryFooter;
import binaryFileStorage.BinaryObjectData;
import binaryFileStorage.BinaryOutputStream;
import binaryFileStorage.BinaryStore;
import javafx.concurrent.Task;

/**
 * Imports data CPOD data and converts into binary files. 
 * 
 * @author Jamie Macaulay
 *
 */
public class CPODImporter {

	public static final int MAX_SAVE = 2000000; 


	private File cpFile;
	private long fileStart;

	private long fileEnd;
	private short podId;
	private byte waterDepth;

	private CPODControl2 cpodControl;
	float[] tempDataGramData;

	//	/**
	//	 * Flag for a CP1 file. 
	//	 */
	//	public static final int FILE_CP1 = 1;
	//	
	//	/**
	//	 * Flag for a CP3 file. 
	//	 */
	//	public static final int FILE_CP3 = 3; 


	CPODFileType cpFileType = CPODFileType.CP1; 

	/**
	 * Hnadles the queue for importing files tasks 
	 */
	private ExecutorService exec = Executors.newSingleThreadExecutor(r -> {
		Thread t = new Thread(r);
		t.setDaemon(true); // allows app to exit if tasks are running
		return t ;
	});

	/**
	 * CPOD file types
	 * @author Jamie Macaulay
	 *
	 */
	public enum CPODFileType {
		CP1("CP1"),
		CP3("CP3");
		//	    FP1("fp1"),
		//	    FP3("fp3");

		private String text;

		CPODFileType(String text) {
			this.text = text;
		}

		public String getText() {
			return this.text;
		}

		public static CPODFileType fromString(String text) {
			for (CPODFileType b : CPODFileType.values()) {
				if (b.text.equalsIgnoreCase(text)) {
					return b;
				}
			}
			return null;
		}
	}


	public CPODImporter(CPODControl2 cpodControl) {
		this.cpodControl = cpodControl;
		//		if (fileType != cpFileType) {
		//			System.err.println("CPOD Mismatched file type " + cpFile.getAbsolutePath());
		//		}
	}

	public static CPODFileType getFileType(File cpFile) {
		for (int i=0; i<CPODFileType.values().length; i++) {
			if (cpFile.getAbsolutePath().toLowerCase().endsWith(CPODFileType.values()[i].getText())) {
				return CPODFileType.values()[i];
			}
		}

		return null; 
	}

	public static int getHeadSize(CPODFileType fileType) {
		switch (fileType) {
		case CP1:
			return 360;
		case CP3:
			return 720;
		}
		return 0;
	}

	public static int getDataSize(CPODFileType fileType) {
		switch (fileType) {
		case CP1:
			return 10;
		case CP3:
			return 40;
		}
		return 0;
	}
	//	
	//	if (cpFileType == FILE_CP1) {
	//		dataBlock = cpodControl.getCP1DataBlock(); 
	//	}
	//	else {
	//		dataBlock = cpodControl.getCP3DataBlock(); 
	//
	//	}

	/**
	 * Import a file. 
	 * @param cpFile - the CP1 file. 
	 * @return the number of clicks saved to the datablock
	 */
	protected int importFile(File cpFile, CPODClickDataBlock dataBlock) {
		return	importFile( cpFile, dataBlock, -1, Integer.MAX_VALUE); 
	}

	/**
	 * Import a file. 
	 * @param cpFile - the CP1 file. 
	 * @param from - the click index to save from. e.g. 100 means that only click 100 + in the file is saved
	 * @param maxNum
	 * @return the total number of clicks int he file. 
	 */
	protected int importFile(File cpFile, 	CPODClickDataBlock dataBlock, int from, int maxNum) {
		BufferedInputStream bis = null;
		int bytesRead;
		FileInputStream fileInputStream = null;
		long totalBytes = 0;
		try {
			bis = new BufferedInputStream(fileInputStream = new FileInputStream(cpFile));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return -1;
		}
		if (readHeader(bis) == false) {
			return -2;
		};

		totalBytes = getHeadSize(cpFileType);
		int dataSize = getDataSize(cpFileType);
		byte[] byteData = new byte[dataSize];
		short[] shortData = new short[dataSize];
		int fileEnds = 0;
		boolean isClick;
		// first record is always a minute mark, so start
		// at -1 to avoid being skipped forward one minute. 
		int nClicks = 0, nMinutes = -1;
		try {
			while (true) {
				bytesRead = bis.read(byteData);
				for (int i = 0; i < bytesRead; i++) {
					shortData[i] = toUnsigned(byteData[i]);
				}
				if (isFileEnd(byteData)) {
					fileEnds++;
				}
				else {
					fileEnds = 0;
				}
				if (fileEnds == 2) {
					break;
				}

				isClick = byteData[dataSize-1] != -2;
				if (isClick) {
					nClicks++;

					if (from<0 || (nClicks>from && nClicks<(from+maxNum))) {

						//System.out.println("Create a new CPOD click: ");
						CPODClick cpodClick = processClick(nMinutes, shortData);

						dataBlock.addPamData(cpodClick);

					}

					//					// now remove the data unit from the data block in order to clear up memory.  Note that the remove method
					//					// saves the data unit to the Deleted-Items list, so clear that as well (otherwise we'll just be using
					//					// up all the memory with that one)
					//					dataBlock.remove(cpodClick);
					//					dataBlock.clearDeletedList();
				}
				else {
					nMinutes ++;
					processMinute(byteData);
				}
				totalBytes += dataSize;
			}
			bis.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println(String.format("File read: Clicks %d, minutes %d", nClicks, nMinutes));

		return nClicks;
	}

	/**
	 * A new minute. Don;t think we need to do anything here.?
	 * @param byteData
	 */
	private void processMinute(byte[] byteData) {
		// TODO Auto-generated method stub

	}

	private CPODClick processClick(int nMinutes, short[] shortData) {
		/*
		 * 
		 */
		return CPODClick.makeClick(cpodControl, fileStart + nMinutes * 60000L, shortData);
	}

	/**
	 * Java will only have read signed bytes. Nick clearly
	 * uses a lot of unsigned data, so convert and inflate to int16. 
	 * @param signedByte
	 * @return unsigned version as int16. 
	 */
	static short toUnsigned(byte signedByte) {
		short ans = signedByte;
		if (ans < 0) {
			ans += 256;
		}
		return ans;
	}

	/**
	 * Is it the end of the file ? 
	 * @param byteData
	 * @return true if all bytes == 255
	 */
	static boolean isFileEnd(byte[] byteData) {
		for (int i = 0; i < byteData.length; i++) {
			//			if ((byteData[i] ^ 0xFF) != 0)  {
			//				return false;
			//			}
			if (byteData[i] != -1)  {
				return false;
			}
		}
		return true;
	}

	boolean readHeader(BufferedInputStream bis) {
		int bytesRead;
		byte[] headData = new byte[getHeadSize(cpFileType)];
		try {
			bytesRead = bis.read(headData);
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		if (bytesRead != headData.length) {
			return false;
		}
		// read as a load of 4 byte integers and see what we get !
		DataInputStream dis = new DataInputStream(new ByteArrayInputStream(headData));
		int nShort = headData.length / 2;
		short[] shortData = new short[nShort];
		for (int i = 0; i < shortData.length; i++) {
			try {
				shortData[i] = dis.readShort();
				if (shortData[i] == 414) {
//					System.out.println("Found id at %d" + i);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		dis = new DataInputStream(new ByteArrayInputStream(headData));
		int nFloat = headData.length / 4;
		float[] floatData = new float[nFloat];
		for (int i = 0; i < floatData.length; i++) {
			try {
				floatData[i] = dis.readFloat();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		dis = new DataInputStream(new ByteArrayInputStream(headData));
		int nInt = headData.length / 4;
		int[] intData = new int[nInt];
		for (int i = 0; i < nInt; i++) {
			try {
				intData[i] = dis.readInt();
				int bOff = i*4;
				int sOff = i*2;
//				if (intData[i] > 0)
//					System.out.println(String.format("%d, Int = %d, Float = %3.5f, Short = %d,%d, bytes = %d,%d,%d,%d", i, intData[i],
//							floatData[i],
//							shortData[sOff], shortData[sOff+1],
//							headData[bOff], headData[bOff+1], headData[bOff+2], headData[bOff+3]));
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		fileStart = CPODControl.podTimeToMillis(intData[64]);
		fileEnd = CPODControl.podTimeToMillis(intData[65]);
		// other times seem to be packed in ints 66 - 69. 
		podId = shortData[50];
		waterDepth = headData[8];

		return true;
	}

	/**
	 * @return the fileStart
	 */
	public long getFileStart() {
		return fileStart;
	}

	/**
	 * Get the data 
	 * @param type
	 * @return
	 */
	public CPODClickDataBlock getDataBlock(CPODFileType type) {
		switch (type) {
		case CP1:
			return this.cpodControl.getCP1DataBlock();
		case CP3:
			return this.cpodControl.getCP3DataBlock();
		}
		return null;
	}

	/**
	 * Run the import task. 
	 */
	public void runImportTask(ArrayList<File> files, CPODClickDataBlock clickDataBlock) {
		Task<Integer>  cpodTask = new CPODImportTask(files, clickDataBlock); 

		Thread th = new Thread(cpodTask);

		th.setDaemon(true);

		th.start();
	}

	/**
	 * Import the CPOD data from a certain file type. 
	 * @param  files to import - a list of CPOD compatible files (can be a mix but only the files corresponding to type will be processed)
	 * @param type - the type flag of the file e.g. CPODFileType.CP1
	 * @return the CPOD import task. 
	 */
	public Task<Integer> importCPODDataTask(List<File> files, CPODFileType type) {

		List<File> cpXFIles = new ArrayList<File>(); 

		for (int i=0; i<files.size(); i++) {
			String ext = FilenameUtils.getExtension(files.get(i).getAbsolutePath());
			if (ext.equals(type.getText())) {
				cpXFIles.add(files.get(i));
			}
		}

		CPODImportTask cpodTask = new CPODImportTask(cpXFIles, getDataBlock(type)); 

		return cpodTask; 
	}



	/**
	 * Import the CPOD data from a list of CPOD files. 
	 * @param  a list of CPOD compatible files (can be a mix)
	 * @return a list of tasks whihc imports each file type. 
	 */
	public List<Task<Integer>> importCPODData(List<File> files) {

		List<Task<Integer>>  tasks  = new ArrayList<Task<Integer>>(); 

		for (int i=0; i<CPODFileType.values().length; i++) {
			Task<Integer> cp1Task = importCPODDataTask(files, CPODFileType.values()[i]); 
			tasks.add(cp1Task); 
		}

		tasks.get(tasks.size()-1).setOnSucceeded((workerState)->{
			PamController.getInstance().updateDataMap();
		});

		//TODO what if a task is cancelled...
		return tasks; 
	}

	/**
	 * Run the tasks
	 * @param tasks - the tasks. 
	 */
	public void runTasks(List<Task<Integer>> tasks) {

		for (int i=0; i<CPODFileType.values().length; i++) {
			this.exec.execute(tasks.get(i));
		}
	}


	/**
	 * Task for importing CPOD data. 
	 * @author Jamie Macaulay
	 *
	 */
	class CPODImportTask extends Task<Integer> {



		/**
		 * List of files, either CP1 or CP3
		 */
		private List<File> cpxFile;

		/**
		 * Reference to the binary store. 
		 */
		private BinaryStore binaryStore;

		/**
		 * The click data block. 
		 */
		private CPODClickDataBlock cpodDataBlock;

		/**
		 * The binary stream
		 */
		private BinaryOutputStream binaryStream; 

		/**
		 * 
		 * @param cpxfiles - a list of CP1 or CP3 files. 
		 * @param cpodDataBlock - the CPOD data block. 
		 */
		public CPODImportTask(List<File> cpxfiles, CPODClickDataBlock cpodDataBlock) {
			this.cpxFile = cpxfiles; 
			this.cpodDataBlock=cpodDataBlock; 
		}

		@Override
		protected Integer call() throws Exception {
			try {
				BinaryDataSource binarySource = cpodDataBlock.getBinaryDataSource();
				binaryStore = (BinaryStore) PamController.getInstance().findControlledUnit(BinaryStore.defUnitType);
				if (binaryStore == null) {
					String msg = "<html>Error: Can't convert CPOD files unless you have a Binary Storage module.<br>" + 
							"Please close this dialog and add/configure a binary store first.</html>";
					int ans = WarnOnce.showWarning(null, "CPOD Import",	msg, WarnOnce.OK_OPTION);
					System.out.println("Can't convert CPOD files unless you have a binary storage module");
					return null;
				}

				BinaryOutputStream outputStream = new BinaryOutputStream(binaryStore, cpodDataBlock);
				binarySource.setBinaryStorageStream(outputStream);
				binaryStream = cpodDataBlock.getBinaryDataSource().getBinaryStorageStream();


				for (int i=0; i<cpxFile.size(); i++) {
					int count=0; 

					if (this.isCancelled()) return -1; 


					this.updateMessage("Importing CPOD file: " + (i+1));

					int nClicks = 0; 
					int totalClicks = Integer.MAX_VALUE;

					while (nClicks<=totalClicks) {

						totalClicks = importFile(cpxFile.get(i), cpodDataBlock, nClicks, MAX_SAVE); 
						
						System.out.println("Number of CPOD data units in the data block: " + nClicks + " progress: " +  (i+1) + " " + cpxFile.size() );

						
						ListIterator<CPODClick> iterator = cpodDataBlock.getListIterator(0);
						CPODClick  click; 
						double day = -1; 

						Calendar cal = Calendar.getInstance();
						BinaryObjectData data ; 
						while (iterator.hasNext()) {
							if (this.isCancelled()) return -1; 
							click = iterator.next(); 
							count++;

							//System.out.println("Saving click: " + 	click.getUID());

							//new binary file every daya; 
							cal.setTimeInMillis(click.getTimeMilliseconds());
							int dayYear = cal.get(Calendar.DAY_OF_YEAR);
							if (day!=dayYear) {
								this.updateProgress(i+(count/(double) totalClicks), cpxFile.size());

								if (day>-1) {
									//close current file
									binaryStream.writeModuleFooter();
									binaryStream.writeFooter(click.getTimeMilliseconds(), System.currentTimeMillis(), BinaryFooter.END_UNKNOWN);
									binaryStream.closeFile();
									binaryStream.createIndexFile();
								}

								System.out.println("Open new binary file: " + 	PamCalendar.formatDBDateTime(click.getTimeMilliseconds()));
								this.updateMessage("Saving file: " + 	PamCalendar.formatDBDateTime(click.getTimeMilliseconds()));

								//write the module head
								binaryStream.openOutputFiles(click.getTimeMilliseconds());
								binaryStream.writeHeader(click.getTimeMilliseconds(), System.currentTimeMillis());
								binaryStream.writeModuleHeader();

								day=dayYear; 
							}

							data =  cpodDataBlock.getBinaryDataSource().getPackedData(click);
							this.binaryStream.storeData(data.getObjectType(), click.getBasicData(), data);
						}
						cpodDataBlock.clearAll(); 
						
						//update number of clicks. 
						nClicks=nClicks+MAX_SAVE; 
					}
	
				}
			} 
			catch (Exception e) {
				e.printStackTrace();
			}

			System.out.println("CPOD import thread finished: ");

			return 1;
		}

	}

}

