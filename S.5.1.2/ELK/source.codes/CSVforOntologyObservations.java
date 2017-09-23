package uk.abdn.cs.semanticweb.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import uk.abdn.cs.semanticweb.knowledge.SampleWithGraphTraversals;

public class CSVforOntologyObservations {

	public static void main(String[] args) {

		CSVforOntologyObservations dCls = new CSVforOntologyObservations();
		
		// dCls.process10SamplesFromFile("C:/Users/Isa/Desktop/files1165.txt", "C:/Users/Isa/Desktop/logsRuning");
		dCls.combinePredictionsFrom10Samples("C:/ProgramExt/WS-R/ELK_SNOMEDCT_1165/prediction4original");
		
		
	}

	public void process10SamplesFromFile(String pListOfFiles, String pFolderOfFiles) {

		int dCounter = 0; // Isa: used for counting/checking while developing
							// code.

		CSVforOntologyObservations dCls = new CSVforOntologyObservations();
		File dFile = new File(pListOfFiles);

		String dFolderForCSV = this.folderCanonicalOfFile(dFile) + "/" + "CSV";
		File dirFolderForCSV = new File(dFolderForCSV);
		if (!dirFolderForCSV.exists()) {
			dirFolderForCSV.mkdir();
		}

		BufferedReader br;
		String line;
		try {
			br = new BufferedReader(new FileReader(dFile));

			// 11,770 samples from 1,177 original ontologies
			File dSampleLogsFolder = new File(pFolderOfFiles);
			File[] dListOfSampleLogs = dSampleLogsFolder.listFiles();
			int i = 0;

			while ((line = br.readLine()) != null) {
				i++;
				File dOriginalFile = new File(line);
				// String dOriginalOntologyName = nameOfFile(dOriginalFile);

				File dCSVFile = new File(dFolderForCSV + "/" + dOriginalFile + ".csv");
				dCSVFile.createNewFile();
				Writer dCSVWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(dCSVFile), "utf-8"));

				// *** ISA: This part is used for taking [RATIO in PERCENTAGE] as the X-AXIS. 
				dCSVWriter.write("sizePC,msec\n");
				// *** ISA: This part is used for taking [AXIOM COUNT] as the X-AXIS.
				// dCSVWriter.write("AxiomCount,msec\n");

				for (File file : dListOfSampleLogs) {
					String dLogFileWOTimeStamp = file.getName().substring(file.getName().toString().indexOf("_") + 1,
							file.getName().length());
					// System.out.println("*FILES IN LOGS* " +
					// dLogFileWOTimeStamp);
					if (dLogFileWOTimeStamp.startsWith(line)) {
						// System.out.println("" + i + "." + line + " - " +
						// file.getName().toString());

						BufferedReader brLogFile = null;
						String lineLog;

						String dOntoFile = "";

						try {
							brLogFile = new BufferedReader(
									new FileReader(pFolderOfFiles + "/" + file.getName().toString()));
							while ((lineLog = brLogFile.readLine()) != null) {

								if (lineLog.contains("Ontology File")) {
									String[] dLinePieces = lineLog.split(":");
									
									// *** ISA: This part takes [RATIO in PERCENTAGE] as the X-AXIS. 
									String strSampleNumber = dLinePieces[1].substring(line.length(), line.length() + 3);
									int dPieceNumber = Integer.parseInt(strSampleNumber.replace("_", ""));
									
									// *** ISA: This part takes [AXIOM COUNT] as the X-AXIS.
									// String strSampleNumberAxiomCount = dLinePieces[1].substring(line.length() + 1, dLinePieces[1].length());
									// String[] dSamplePieces = strSampleNumberAxiomCount.split("_");
									// System.out.println(strSampleNumberAxiomCount + " - " + dSamplePieces[0] + " - " + dSamplePieces[1] + " - " + dSamplePieces[2]);
									// int dPieceNumber = Integer.parseInt(dSamplePieces[1]);
									
									dCSVWriter.write("" + dPieceNumber);
									dCSVWriter.write(",");
								}
								if (lineLog.contains("Classifying Ontology took")) {
									String[] dLinePieces = lineLog.split(":");
									String strSampleTime = dLinePieces[1].replace("msec.", "").trim();
									int dSampleTime = Integer.parseInt(strSampleTime);
									// System.out.println(dSampleTime);
									dCSVWriter.write("" + dSampleTime);
									dCSVWriter.write("\n");
								}

							}
							// System.out.println(dCounter);

						} catch (Exception e) {
							e.printStackTrace();
						} finally {
							brLogFile.close();
						}

					}
				}

				dCSVWriter.close();

			}
			br.close();

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public void combinePredictionsFrom10Samples(String pPredictionsFolder) {
		try {
			File folder = new File(pPredictionsFolder);
			File[] listOfFiles = folder.listFiles();

			File dPredictionTSVFile = new File(pPredictionsFolder + "/" + "Predictions" + ".tsv");
			dPredictionTSVFile.createNewFile();
			Writer dCSVWriter = new BufferedWriter(
					new OutputStreamWriter(new FileOutputStream(dPredictionTSVFile), "utf-8"));

			for (File file : listOfFiles) {
				if (file.isFile()) {
					// System.out.println(pFolderPath + "/" + file.getName());
					if (!file.getName().startsWith(".")) {
						readFromFile(pPredictionsFolder + "/" + file.getName(), dCSVWriter);
					}
				}
			}
			dCSVWriter.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void readFromFile(String pFileName, Writer pWriterTSV) {

		File dFile = new File(pFileName);
		BufferedReader br;
		String line;

		try {
			br = new BufferedReader(new FileReader(dFile));
			while ((line = br.readLine()) != null) {

				if (line.contains("CSVFile:")) {
					String[] dLinePieces = line.split(":");
					System.out.print("Ontology" + "\t" + dLinePieces[1] + "\t");
					pWriterTSV.write("Ontology" + "\t" + dLinePieces[1] + "\t");
				}
				if (line.contains("linear:")) {
					String[] dLinePieces = line.split(":");
					System.out.print("Linear" + "\t" + dLinePieces[1] + "\t");
					pWriterTSV.write("Linear" + "\t" + dLinePieces[1] + "\t");
				}
				if (line.contains("polynomial2:")) {
					String[] dLinePieces = line.split(":");
					System.out.print("polynomial2" + "\t" + dLinePieces[1] + "\t");
					pWriterTSV.write("polynomial2" + "\t" + dLinePieces[1] + "\t");
				}
				if (line.contains("polynomial3:")) {
					String[] dLinePieces = line.split(":");
					System.out.print("polynomial3" + "\t" + dLinePieces[1] + "\t");
					pWriterTSV.write("polynomial3" + "\t" + dLinePieces[1] + "\t");
				}

			}
			System.out.println("");
			pWriterTSV.write("\n");

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	/******************************************* Utils *******************************************/

	public String StringOfNow() {
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd.HHmmss.SSS");
		Date date = new Date();
		String dNow = dateFormat.format(date);
		return dNow;
	}

	public String extensionOfFile(File pFile) {
		return pFile.getName().substring(pFile.getName().lastIndexOf(".") + 1, pFile.getName().length());
	}

	public String nameOfFile(File pFile) {
		return pFile.getName().substring(0, pFile.getName().lastIndexOf("."));
	}

	public String folderCanonicalOfFile(File pFile) {
		String dResult = "";
		try {
			int p = Math.max(pFile.getCanonicalPath().lastIndexOf("/"), pFile.getCanonicalPath().lastIndexOf("\\"));
			dResult = pFile.getCanonicalPath().substring(0, p);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return dResult;
	}

}
