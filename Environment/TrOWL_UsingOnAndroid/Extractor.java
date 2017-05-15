package tr.tech.adaptui;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.OWLFunctionalSyntaxOntologyFormat;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;

public class Extractor {

	private boolean dAddTimeStampToFileName = false;
	
	private Runtime runtime = Runtime.getRuntime();
	
	long startTime;
	long loadingTime;
	long processTime;
	long ontoWritingTime;

	/******************************************* Tests *******************************************/
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String dOntoFolder = "assets";
		String dOntoFileName = "snomed_jan17.owl";
		String dDesiredFileName = "Desired_snomed_jan17.txt";
		Extractor dCls = new Extractor();
		try {
			String dOntoOriginalFullPath = dOntoFolder + File.separator + dOntoFileName;
			System.out.println("dOntoOriginalFullPath : " + dOntoOriginalFullPath);

			String dDesiredFullPath = dOntoFolder + File.separator + dDesiredFileName;
			System.out.println("dDesiredFullPath : " + dDesiredFullPath);

			BufferedReader br = new BufferedReader(new FileReader(dOntoFolder + File.separator + "AxiomCounts.txt"));
			int dLine = 0;
			String line;
			while ((line = br.readLine()) != null) {
				dLine = Integer.parseInt(line);
				System.out.println("*** parseInt(line) : " + dLine);
				dCls.extractKnowledge(dOntoOriginalFullPath, dDesiredFullPath,dLine);
				Thread.sleep(1000L);
			}
			br.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	/******************************************* Tests *******************************************/
	
	public String extractKnowledge(String pOntologyFilePath,
			String pDesiredFilePath, int maxAxiomCount) throws Exception {
		
		startTime = System.nanoTime();
		long usedMemoryBefore = (runtime.totalMemory() - runtime.freeMemory()) / 1024L;
		
		String dExtactedFileName = "";
		String dNow = StringOfNow();
		
		File ontFile = new File(pOntologyFilePath);
		String dCanonicalPath = folderCanonicalOfFile(ontFile);
		// System.out.println("dCanonicalPath : " + dCanonicalPath);
		
		String dFileName = "";
		String dFileExtention = "";

		String dOutputFolder = dCanonicalPath + File.separator + "output";
		File directoryOutput = new File(dOutputFolder);
		if (!directoryOutput.exists()) {
			directoryOutput.mkdir();
		}
		
		String dLogFolder = dCanonicalPath + File.separator + "log";
		File directoryLog = new File(dLogFolder);
		if (!directoryLog.exists()) {
			directoryLog.mkdir();
		}
		
		try {
			
			OWLOntologyManager dOm = OWLManager.createOWLOntologyManager();
			dFileName = nameOfFile(ontFile);
			dFileExtention = extensionOfFile(ontFile);
			OWLOntology dOnt = dOm.loadOntologyFromOntologyDocument(ontFile);
			
			// Log File Name: DateTime_File.log
			String dLogFileName = directoryLog + File.separator + dNow + "_" + dFileName + "_" + maxAxiomCount + ".log";
			PrintStream out = new PrintStream(new FileOutputStream(dLogFileName));
			System.setOut(out);
			System.out.println("--- Ontology Traversor ---");
			System.out.println("Used Memory before: " + usedMemoryBefore + " KB.");	

			HashSet<OWLAxiom> dAxioms = new HashSet<OWLAxiom>();
			HashSet<String> dSignatures = new HashSet<String>();
			HashSet<String> dDesiredSignatures = getDesired(pDesiredFilePath);
			
			long t1_RAM = (runtime.totalMemory() - runtime.freeMemory()) / 1024L;
			System.out.println("(Ontology Loaded to OWLOntology - RAM Beginning:" + usedMemoryBefore + ": KB. RAM End:" + t1_RAM
							+ ": KB. RAM Diff:" + (t1_RAM - usedMemoryBefore) + ": KB.)");
			long t1_Time = System.nanoTime();

			HashSet<OWLAxiom> dAxiomsOfOntology = new HashSet<OWLAxiom>();
			for (OWLAxiom dAxiom : dOnt.getAxioms()) {
				if ((!dAxiom.getAxiomType().toString()
						.equalsIgnoreCase("AnnotationAssertion"))
						&& (!dAxiom.getAxiomType().toString()
								.equalsIgnoreCase("SubAnnotationPropertyOf"))) {
					dAxiomsOfOntology.add(dAxiom);
				}
			}
			dOnt = null;
			dOm = null;

			long t2_RAM = (runtime.totalMemory() - runtime.freeMemory()) / 1024L;
			System.out.println("(Copying Axioms to Hashset - RAM Beginning:" + usedMemoryBefore + ": KB. RAM End:" + t2_RAM
							+ ": KB. RAM Diff:" + (t2_RAM - usedMemoryBefore) + ": KB.)");
			long t2_Time = System.nanoTime();
			System.out.println("(Copying Axioms to Hashset took : " + (t2_Time - t1_Time) / 1000000L + " msec.)");
			loadingTime = ((System.nanoTime() - startTime) / 1000000L);
			System.out.println("(So far Time passed : " + loadingTime + " msec.)");

			System.out.println("AxiomCount in Ontology : " + dAxiomsOfOntology.size());
			System.out.println("* Desired Axioms: " + dDesiredSignatures.size());

			boolean dContinueIteration = true;
			int dPreviousAxiomCount = 0;
			int iterationCount = 0;
			while (dContinueIteration) {
				iterationCount++;
				dPreviousAxiomCount = dAxioms.size();
				dContinueIteration = findRelatedAxioms(dAxiomsOfOntology, dAxioms, dSignatures, dDesiredSignatures, maxAxiomCount);

				System.out.println("* iterationCount: " + iterationCount);
				System.out.println("Axioms.size(): " + dAxioms.size());
				System.out.println("DesiredSignatures.size(): " + dDesiredSignatures.size());
				System.out.println("ContinueIteration: " + dContinueIteration);
				System.out.println("dAxiomsOfOntology Size : " + dAxiomsOfOntology.size());
				if ((!dContinueIteration) || (dPreviousAxiomCount == dAxioms.size())) {
					break;
				}
			}
			dAxiomsOfOntology = null;
			dSignatures = null;
			dDesiredSignatures = null;

			// System.gc();

			processTime = System.nanoTime();
			System.out.println("(Extracting New Ontology : " + (processTime - t2_Time) / 1000000L + " msec.)");

			System.out.println("--- Stats ---");
			
			dExtactedFileName = dOutputFolder + File.separator + dNow + "_" + dFileName + "_" + maxAxiomCount + "."
					+ dFileExtention;
			if (!dAddTimeStampToFileName) {
				dExtactedFileName = dOutputFolder + File.separator + dFileName + "_" + maxAxiomCount + "." + dFileExtention;
			}
			writeOntologyFromAxioms(dAxioms, dExtactedFileName, maxAxiomCount);

			ontoWritingTime = System.nanoTime();
			System.out.println("(Writing New Ontology : " + (ontoWritingTime - processTime) / 1000000L + " msec.)");
			
			long usedMemoryAfter = (runtime.totalMemory() - runtime.freeMemory()) / 1024L;
			System.out.println("RAM Beginning:" + usedMemoryBefore + ": KB. RAM End:" + usedMemoryAfter + ": KB. RAM Diff:"
					+ (usedMemoryAfter - usedMemoryBefore) + ": KB.");

			
			System.out.println("TOTAL PROCESSING TIME : " + (ontoWritingTime - t1_Time) / 1000000L + " msec.");
			
			
			System.out.println("--- The End ---");
			out.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return dExtactedFileName;
	}

	public HashSet<String> getDesired(String pDesiredFilePath) {
		HashSet<String> dDesiredSignatures = null;
		try {
			BufferedReader br = new BufferedReader(new FileReader(pDesiredFilePath));
			String dLine;
			while ((dLine = br.readLine()) != null) {

				if (!dLine.trim().equalsIgnoreCase("")) {
					System.out.println("Signature : " + dLine);
					if (dDesiredSignatures == null) {
						dDesiredSignatures = new HashSet<String>();
					}
					dDesiredSignatures.add(IRI.create(dLine).toString());
				}
			}
			br.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return dDesiredSignatures;
	}

	public boolean findRelatedAxioms(HashSet<OWLAxiom> pAxiomsOfOntology,
			HashSet<OWLAxiom> pAxioms, HashSet<String> pSignatures,
			HashSet<String> pDesiredSignatures, int maxAxiomCount) {
		for (OWLAxiom dAxiom : pAxiomsOfOntology) {
			if (isThereRelatedSignatures(dAxiom, pDesiredSignatures)) {
				pAxioms.add(dAxiom);
				for (OWLClass dClass : dAxiom.getClassesInSignature()) {
					pSignatures.add(dClass.toString());
				}
			}
			if (pAxioms.size() >= maxAxiomCount) {
				System.out.println("* MAX AXIOM REACHED:" + pAxioms.size());
				return false;
			}
		}
		pDesiredSignatures.addAll(pSignatures);
		pSignatures.clear();

		return true;
	}

	public static boolean isThereRelatedSignatures(OWLAxiom pAxiom,
			HashSet<String> pDesiredSignatures) {
		Set<OWLClass> dClasses = pAxiom.getClassesInSignature();
		for (OWLClass dClass : dClasses) {
			String dIri = dClass.toString();
			if (pDesiredSignatures.contains(dIri)) {
				return true;
			}
		}
		return false;
	}

	private void writeOntologyFromAxioms(HashSet<OWLAxiom> pAxioms,
			String pFullFileName, int maxAxiomCount) {
		try {
			System.out.println("* Axioms written to File: " + pFullFileName);
			OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
			OWLOntology newonto = manager.createOntology();
			manager.addAxioms(newonto, pAxioms);
			manager.saveOntology(newonto, new OWLFunctionalSyntaxOntologyFormat(),
					IRI.create("file:////" + pFullFileName));
			manager = null;
			newonto = null;
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
		return pFile.getName().substring(pFile.getName().lastIndexOf(".") + 1,pFile.getName().length());
	}

	public String nameOfFile(File pFile) {
		return pFile.getName().substring(0, pFile.getName().lastIndexOf("."));
	}
	
	public String folderCanonicalOfFile(File pFile) {
		String dResult = "";
		try{
			int p = Math.max(pFile.getCanonicalPath().lastIndexOf("/"), pFile.getCanonicalPath().lastIndexOf("\\"));
			dResult = pFile.getCanonicalPath().substring(0, p);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return dResult;
	}
}
