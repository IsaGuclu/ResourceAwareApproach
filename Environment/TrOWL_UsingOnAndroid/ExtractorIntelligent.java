/////////////////////////////////////////////////////////////////////////////
// File: ExtractorIntelligent.java 
// Author: Carlos Bobed 
// Date: April 2017
// Version: 0.01
// Comments: Class that implements the expansion/fitting algorithm based 
// 	on the algorithm by Cuenca et al. 
// Modifications: 
///////////////////////////////////////////////////////////////////////////////

package tr.tech.adaptui;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.AbstractMap.SimpleEntry;
import java.util.Set;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.OWLFunctionalSyntaxOntologyFormat;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLDeclarationAxiom;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import uk.ac.manchester.syntactic_locality.ModuleExtractorManager;

public class ExtractorIntelligent {

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
//		String dOntoFileName = "snomed_jan17.owl";
		String dOntoFileName = "00540.owl_functional.owl"; 
		
		String dDesiredFileName = "Desired_test_entities.txt";
		ExtractorIntelligent dCls = new ExtractorIntelligent();
		
		try {
			// the ontology to be modularized
			String dOntoOriginalFullPath = dOntoFolder + File.separator + dOntoFileName;
			System.out.println("dOntoOriginalFullPath : " + dOntoOriginalFullPath);

			// the file which contains the desired signature
			// one IRI per line
			String dDesiredFullPath = dOntoFolder + File.separator + dDesiredFileName;
			System.out.println("dDesiredFullPath : " + dDesiredFullPath);
			
			// the file which contains the required sizes
			BufferedReader br = new BufferedReader(new FileReader(dOntoFolder + File.separator + "AxiomCounts1.txt"));
			
			int dLine = 0;
			String line;
			while ((line = br.readLine()) != null) {
				dLine = Integer.parseInt(line);
				System.out.println("*** parseInt(line) : " + dLine);
				dCls.extractKnowledge(dOntoOriginalFullPath, dDesiredFullPath,dLine, "LUM");
				Thread.sleep(1000L);
			}
			br.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	/******************************************* Tests *******************************************/
	
	public String extractKnowledge(String pOntologyFilePath,
			String pDesiredFilePath, int maxAxiomCount, String moduleType) throws Exception {
		
		String moduleFilenameHeader = moduleType+"-INT-"; 
		
		startTime = System.nanoTime();
		long usedMemoryBefore = (runtime.totalMemory() - runtime.freeMemory()) / 1024L;
		
		String dExtactedFileName = "";
		String dNow = StringOfNow();
		
		File ontFile = new File(pOntologyFilePath);
		String dCanonicalPath = folderCanonicalOfFile(ontFile);
		
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
			// First of all, we load the ontology as usual
			OWLOntologyManager dOm = OWLManager.createOWLOntologyManager();
			dFileName = nameOfFile(ontFile);
			dFileExtention = extensionOfFile(ontFile);
			OWLOntology dOriginalOnt = dOm.loadOntologyFromOntologyDocument(ontFile);
			
			// Log File Name: DateTime_File.log
			String dLogFileName = directoryLog + File.separator + moduleFilenameHeader + dNow + "_" + dFileName + "_" + maxAxiomCount + ".log";
			PrintStream out = new PrintStream(new FileOutputStream(dLogFileName));
			System.setOut(out);
			System.out.println("--- Ontology Traversor ---");
			System.out.println("--- Type of modules: "+moduleType); 
			System.out.println("Used Memory before: " + usedMemoryBefore + " KB.");	
			
			// we get the desired signature and order the elements according to their module 
			// size
			// we get the desiredSignature
			HashSet<OWLEntity> dOriginalDesiredSignature = getDesired(pDesiredFilePath, dOriginalOnt);
			
			// we store the extended and base signatures
			HashSet<OWLEntity> dExtendedDesiredSignature = new HashSet<OWLEntity>(); 
			dExtendedDesiredSignature.addAll(dOriginalDesiredSignature);
			HashSet<OWLEntity> dBaseDesiredSignature = new HashSet<OWLEntity>(); 
			dBaseDesiredSignature.addAll(dExtendedDesiredSignature); 
			
			System.out.println("Matched "+dOriginalDesiredSignature.size()+" entities in the ontology"); 
			
			ModuleExtractorManager moduleManager = new ModuleExtractorManager(dOriginalOnt, moduleType, true, false, false);
		
			HashSet<OWLAxiom> dAxioms = new HashSet<OWLAxiom>();
			
			// check the memory again
			long t1_RAM = (runtime.totalMemory() - runtime.freeMemory()) / 1024L;
			System.out.println("(Ontology Loaded to OWLOntology - RAM Beginning:" + usedMemoryBefore + ": KB. RAM End:" + t1_RAM
							+ ": KB. RAM Diff:" + (t1_RAM - usedMemoryBefore) + ": KB.)");
			long t1_Time = System.nanoTime(); 
			
			// first, we enlarge the signature as much as we can to 
			// get the biggest module that fits in memory
			System.out.println("--> Enlarging the LUM Module ... initial signature size: "+dExtendedDesiredSignature.size()) ;
			// we keep track of both modules, the biggest that fit, and the firt that does not
			OWLOntology baseModule = moduleManager.extractModule(dOriginalDesiredSignature);
			System.out.println("initial module signature size: "+baseModule.getSignature().size()); 
			OWLOntology extendedModule = null;   
			
			if (baseModule.getAxiomCount() <maxAxiomCount) {			
				// first extension
				System.out.println("trying to enlarge..."); 
				dExtendedDesiredSignature.addAll(baseModule.getSignature()); 
				if (dBaseDesiredSignature.containsAll(dExtendedDesiredSignature)) {
					// recall that before the addAll base and extended contained exactly the same
					// we haven't added anything, so we do not do anything but update the variables 
					extendedModule = baseModule; 
				}
				else {
					System.out.println("trying to enlarge..."); 
					extendedModule = moduleManager.extractModule(dExtendedDesiredSignature);  
					System.out.println("1st baseModule size: "+baseModule.getAxiomCount()); 
					System.out.println("1st extendedModule size: "+extendedModule.getAxiomCount());
					while (extendedModule.getAxiomCount()< maxAxiomCount && !dBaseDesiredSignature.containsAll(dExtendedDesiredSignature)) {
						dBaseDesiredSignature.addAll(dExtendedDesiredSignature); 
						dExtendedDesiredSignature.addAll(extendedModule.getSignature());			
						if (dBaseDesiredSignature.size() != dExtendedDesiredSignature.size()) {
							System.out.println("Signature enlarged to "+dExtendedDesiredSignature.size());
						}
						else {
							System.out.println("Nothing added to the signature"); 
						}  
						baseModule = extendedModule; 
						extendedModule = moduleManager.extractModule(dExtendedDesiredSignature);
	
						System.out.println("baseModule size: "+baseModule.getAxiomCount()); 
						System.out.println("extendedModule size: "+extendedModule.getAxiomCount());
					}
				}
			}
			else{
				extendedModule = baseModule; 
				assert dBaseDesiredSignature.size() == dExtendedDesiredSignature.size() && dBaseDesiredSignature.containsAll(dExtendedDesiredSignature); 
			}
			
			assert (baseModule.getAxiomCount() == extendedModule.getAxiomCount() && dBaseDesiredSignature.size() == dExtendedDesiredSignature.size()) ||
				(dBaseDesiredSignature.size()<dExtendedDesiredSignature.size() && baseModule.getAxiomCount()<extendedModule.getAxiomCount()); 
			
			// if baseModule != extendedModule, fit module should be in the middle
			// if baseModule == extendedModule then, fit should be either between 0 and baseModule (if baseModule is > maxAxiomCount) 
			// or between baseModule and Ontology if extendedModule < maxAxiomCount
			
			OWLOntology fittedModule = null; 
			HashSet<OWLEntity> dSignatureFittedModule = new HashSet<OWLEntity>(); 
			if (extendedModule.getAxiomCount() > maxAxiomCount) {
				System.out.println("--> Enlarging done, starting fitting proceess ...");				
				if (dExtendedDesiredSignature.size() > 1) { 
					// assessImportance gives priority to the elements that were 
					// in the original signature
					ArrayList<OWLEntity> assessedEntities = assessImportance(dExtendedDesiredSignature, dOriginalDesiredSignature, moduleManager);
					int elemsSkipped = 1; 
					dSignatureFittedModule.addAll(assessedEntities.subList(0, assessedEntities.size()-elemsSkipped));
					fittedModule = moduleManager.extractModule(dSignatureFittedModule); 
					while (fittedModule.getAxiomCount()>maxAxiomCount && elemsSkipped < assessedEntities.size()) {
						elemsSkipped++; 
						dSignatureFittedModule.clear(); 
						dSignatureFittedModule.addAll(assessedEntities.subList(0, assessedEntities.size()-elemsSkipped)); 
						fittedModule = moduleManager.extractModule(dSignatureFittedModule); 
					}
				}
				else {
					// we haven't been able to do anything over the signature
					fittedModule = extendedModule; 
					dSignatureFittedModule.addAll(dExtendedDesiredSignature); 
				}	
			}
			else {
				// the extendedModule is still smaller than the maxAxiomCount
				fittedModule = extendedModule; 
				dSignatureFittedModule.addAll(dExtendedDesiredSignature); 
			}
			// if fitted module has still a size bigger than the maxAxiomCount
			// we do not have other option rather than perform agnostic extraction
		
			// currently, we have: 
			// <baseModule, dbaseDesiredSignature>: the LUM module biggest that fit in the maxAxiom count (or the smallest than does not fit)
			// <initialModule, dExtendedSignature>: the module with the extended signature after the possible expansion
			// <fittedModule, dSignatureFittedModule>: the module fitted and its signature 
			
			// depending on their values, we launch the agnostic extraction 
			// on one set of axioms and signatures or another 
			
			HashSet<OWLAxiom> candidateAxioms = new HashSet<OWLAxiom>();
			HashSet<OWLEntity> dCurrentSignature = new HashSet<OWLEntity>(); 
			HashSet<OWLEntity> dDesiredSignature = new HashSet<OWLEntity>(); 
			// the initial conditions for the agnostic extraction 
			// are quite complicated after the expansion and the fitting
			
			// let S be the original Signature 
			// bS the signature of the baseModule, fS the fittedModule, eS the extended one
		
			if (baseModule.getAxiomCount() > maxAxiomCount) {
				// mod(bS) does not fit in our requirements
				// we check if the reduced module fits  
				if (fittedModule.getAxiomCount() <= maxAxiomCount ) {
					// mod(fS) fit in our requirements
					// fS is bS with less elements, might have got rid of elements 
					// in the original signature 
					// we check whether the original signature is still in it 
					
					// in both cases, the candidate axioms are the difference between 
					// base and fitted 
					candidateAxioms.addAll(baseModule.getAxioms()); 
					candidateAxioms.removeAll(fittedModule.getAxioms());
					if (dSignatureFittedModule.containsAll(dOriginalDesiredSignature)) {
						// we should axioms in base-fitted 
						// and the signature should be fitted one 
						dDesiredSignature.addAll(dSignatureFittedModule); 
					}
					else {
						dDesiredSignature.addAll(dOriginalDesiredSignature);
					}
				}
				else {
					// neither the base nor the fitted module fit 
					// |fS| must be 1 
					assert dSignatureFittedModule.size()==1; 
					// the candidate axioms => the baseModule 
					candidateAxioms.addAll(baseModule.getAxioms()); 
					// the signature => the original one 
					dDesiredSignature.addAll(dOriginalDesiredSignature); 
				}
			}
			else {
				// the base module fitted in our requirements 
				// we might have extended it 
				if (extendedModule.getAxiomCount() > maxAxiomCount) {
					// baseModule fits, Ext doesn't => fitted must					
					if (fittedModule.getAxiomCount()<= maxAxiomCount) {
						// base <= fitted < extended
						// bS inEq fS in eS
						// fS always includes bS in this situation
						
						// the candidate => the extended - fitted 
						candidateAxioms.addAll(extendedModule.getAxioms()); 
						candidateAxioms.removeAll(fittedModule.getAxioms());
						// the signature => the fittedOne
						dDesiredSignature.addAll(dSignatureFittedModule);						
					}
					else {
						throw new Exception("The fitted module should fit in this situation");
					}
					
				}
				else {
					// extended fits
					// we haven't been able to extend it further
					candidateAxioms.addAll(dOriginalOnt.getLogicalAxioms()); 
					for (OWLEntity ent: dOriginalOnt.getSignature()) {
						candidateAxioms.addAll(dOriginalOnt.getDeclarationAxioms(ent)); 
					}
					candidateAxioms.removeAll(extendedModule.getAxioms()); 
					dDesiredSignature.addAll(dExtendedDesiredSignature);	
					assert dExtendedDesiredSignature.size() == extendedModule.getSignature().size(); 
				}
			}
			
			// output of the axioms copy
			// this copy can be done faster => without the checkings
			long t2_RAM = (runtime.totalMemory() - runtime.freeMemory()) / 1024L;
			System.out.println("(Extending and fitting modules - RAM Beginning:" + usedMemoryBefore + ": KB. RAM End:" + t2_RAM
							+ ": KB. RAM Diff:" + (t2_RAM - usedMemoryBefore) + ": KB.)");
			long t2_Time = System.nanoTime();
			System.out.println("(Extending and fitting modules took : " + (t2_Time - t1_Time) / 1000000L + " msec.)");
			loadingTime = ((System.nanoTime() - startTime) / 1000000L);
			System.out.println("(So far Time passed : " + loadingTime + " msec.)");

			System.out.println("AxiomCount in Ontology : " + candidateAxioms.size());
			System.out.println("* Desired Signature: " + dDesiredSignature.size());

			boolean dContinueIteration = true;
			int dPreviousAxiomCount = 0;
			int iterationCount = 0;
			
			while (dContinueIteration) {
				iterationCount++;
				dPreviousAxiomCount = dAxioms.size();
				// now the candidates are sieved
				dContinueIteration = findRelatedAxioms(candidateAxioms, dAxioms, dCurrentSignature, dDesiredSignature, maxAxiomCount);

				System.out.println("* iterationCount: " + iterationCount);
				System.out.println("Axioms.size(): " + dAxioms.size());
				System.out.println("DesiredSignatures.size(): " + dDesiredSignature.size());
				System.out.println("ContinueIteration: " + dContinueIteration);
				System.out.println("dAxiomsOfOntology Size : " + candidateAxioms.size());
				if ((!dContinueIteration) || (dPreviousAxiomCount == dAxioms.size())) {
					break;
				}
			}
			// we clear everything
			candidateAxioms = null; 
			extendedModule = null; 
			baseModule = null; 
			fittedModule = null; 
			dSignatureFittedModule = null;
			dDesiredSignature = null;
			dCurrentSignature = null; 
			dBaseDesiredSignature = null; 
			dExtendedDesiredSignature = null; 

			System.gc();

			processTime = System.nanoTime();
			System.out.println("(Extracting New Ontology : " + (processTime - t2_Time) / 1000000L + " msec.)");

			System.out.println("--- Stats ---");
			
			dExtactedFileName = dOutputFolder + File.separator + dNow + "_" + moduleFilenameHeader + dFileName + "_" + maxAxiomCount + "."
					+ dFileExtention;
			if (!dAddTimeStampToFileName) {
				dExtactedFileName = dOutputFolder + File.separator + moduleFilenameHeader + dFileName + "_" + maxAxiomCount + "." + dFileExtention;
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

	/** 
	 * Obtains the desired signature read from the file which is passed as an argument
	 *  
	 * @param pDesiredFilePath File which contains the different elements conforming the desired
	 * 	signature (one IRI per line).
	 * @param originalOntology OWLOntology which the entities have to be matched to  
	 * @return HashSet with the OWLEntities of the ontology
	 */
	public HashSet<OWLEntity> getDesired(String pDesiredFilePath, OWLOntology originalOntology) {
		HashSet<OWLEntity> dDesiredSignatures = null;
		Hashtable<String, OWLEntity> entityTable = new Hashtable<String, OWLEntity>(); 		
		for (OWLEntity ent: originalOntology.getSignature()) {
			entityTable.put(ent.getIRI().toString(), ent); 
		}		
		try {
			BufferedReader br = new BufferedReader(new FileReader(pDesiredFilePath));
			String dLine;
			while ((dLine = br.readLine()) != null) {
				if (!dLine.trim().equalsIgnoreCase("")) {
					System.out.println("Signature : " + dLine);
					if (dDesiredSignatures == null) {
						dDesiredSignatures = new HashSet<OWLEntity>();
					}
					if (entityTable.containsKey(dLine.trim())) {
						dDesiredSignatures.add(entityTable.get(dLine.trim())); 
					}
				}
			}
			br.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return dDesiredSignatures;
	}
	

	/** 
	 * Select the axioms that have something to do with the given signature. It is done at distance 1, i.e., it selects the axioms 
	 * where the signature is participating directly.  
	 * @param candidateAxioms Set of candidate axioms to be selected
	 * @param pAxioms Set where the results are added to 
	 * @param pSignatures New elements found in the expanded axioms 
	 * @param pDesiredSignatures Original set of elements to be expanded
	 * @param maxAxiomCount
	 * @return
	 */
	public boolean findRelatedAxioms(HashSet<OWLAxiom> candidateAxioms,
			HashSet<OWLAxiom> pAxioms, HashSet<OWLEntity> pSignatures,
			HashSet<OWLEntity> pDesiredSignatures, int maxAxiomCount) {
		HashSet<OWLAxiom> addedAxioms = new HashSet<OWLAxiom>(); 
		for (OWLAxiom dAxiom : candidateAxioms) {
			if (isThereRelatedSignatures(dAxiom, pDesiredSignatures)) {
				pAxioms.add(dAxiom);
				for (OWLEntity dEntity: dAxiom.getSignature()) {
					pSignatures.add(dEntity); 
				}
				// we get rid of it 
				addedAxioms.add(dAxiom); 
			}
			if (pAxioms.size() >= maxAxiomCount) {
				System.out.println("* MAX AXIOM REACHED:" + pAxioms.size());
				return false;
			}
		}
		pDesiredSignatures.addAll(pSignatures);
		candidateAxioms.removeAll(addedAxioms); 
		pSignatures.clear();

		return true;
	}

	public static boolean isThereRelatedSignatures(OWLAxiom pAxiom,
			HashSet<OWLEntity> pDesiredSignatures) {
		Set<OWLEntity> dEntities = pAxiom.getSignature(); 
		for (OWLEntity dEntity : dEntities) {
			if (pDesiredSignatures.contains(dEntity)) {
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
			
			// added the replace for windows environments
			manager.saveOntology(newonto, new OWLFunctionalSyntaxOntologyFormat(),
					IRI.create("file:///" + pFullFileName.replace("\\", "/")));
			manager = null;
			newonto = null;
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/** 
	 * Estimates the importance of each element in the given signature according to the size of its LUM module 
	 * 
	 * @param entities Entities to be assessed
	 * @param extractor ModuleExtractor already created over the ontology
	 * @return The list of entities sorted by descending order of importance 
	 */
	private ArrayList<OWLEntity> assessImportance (HashSet<OWLEntity> entities, HashSet<OWLEntity> originalEntities, ModuleExtractorManager extractor) {
		ArrayList<OWLEntity> result = new ArrayList<OWLEntity>(); 
		
		ArrayList<SimpleEntry<OWLEntity, Integer>> taggedEntities = new ArrayList<SimpleEntry<OWLEntity, Integer>>(); 
		
		HashSet<OWLEntity> auxHashSet = new HashSet<OWLEntity> (); 
		for (OWLEntity ent: entities) {
			auxHashSet.add(ent); 
			taggedEntities.add(new SimpleEntry<OWLEntity, Integer>(ent, extractor.extractModule(auxHashSet).getAxiomCount())); 
			auxHashSet.clear(); 
		}
		// beware, it sorts in ascending order
		Collections.sort(taggedEntities, new TaggedEntityComparator(originalEntities));
		
		for (SimpleEntry<OWLEntity, Integer> se: taggedEntities) {
			// I have to add it at the initial position
			// to get them in descending order
			result.add(0,se.getKey()); 
		}
		
		return result; 
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
	
	private class TaggedEntityComparator implements Comparator<SimpleEntry<OWLEntity, Integer>> {
		
		HashSet<OWLEntity> originalEntities = null; 
		
		public TaggedEntityComparator (HashSet<OWLEntity> originalEntities) {
			this.originalEntities = originalEntities; 
		}
		
		public int compare(SimpleEntry<OWLEntity, Integer> o1,
				SimpleEntry<OWLEntity, Integer> o2) {
			
			// we modify a little the comparison
			// if any of the entities is one of the original ones, 
			// we give it priority
			
			if (originalEntities.contains(o1) && originalEntities.contains(o2)) {
				// both are original ones, 
				// we order them appropriately
				if (o1.getValue() < o2.getValue()) {
					return -1; 
				}
				else if (o1.getValue() > o2.getValue()) {
					return 1; 
				}
				else {
					return 0;
				}
			}
			else if (originalEntities.contains(o1)) {
				// only o1 is contained => o1 is always more important
				return 1; 
			}
			else if (originalEntities.contains(o2)) {
				// only o2 is contained => o2 is always more important
				return -1; 
			}
			else {
				// none of them are originals
				// we compare them as usual
				if (o1.getValue() < o2.getValue()) {
					return -1; 
				}
				else if (o1.getValue() > o2.getValue()) {
					return 1; 
				}
				else {
					return 0;
				}
			}
			
			
			
		}
	}
}
