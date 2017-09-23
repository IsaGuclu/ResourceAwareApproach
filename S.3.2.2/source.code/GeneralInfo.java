package uk.abdn.cs.semanticweb.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import uk.ac.manchester.cs.owl.owlapi.OWLClassImpl;

public class GeneralInfo {

	/**
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {		

		
		PrintStream out = new PrintStream(new FileOutputStream("C:/Users/Isa/Desktop/6modules.of.SLM.txt"));
		System.setOut(out);
		
		GeneralInfo dCls = new GeneralInfo();
		dCls.infoFolder("6modules.of.SLM", "General");
	
	}
	
	public String infoFolder(String pFolderPath, String pFormat) {

		File folder = new File(pFolderPath);
		File[] listOfFiles = folder.listFiles();
		
		for (File file : listOfFiles) {
			if (file.isFile()) {
				
				System.out.println("--- General Info of an Ontology ---");
				long processTime = 0;
				long start = System.nanoTime(); 
				
				if (pFormat.equalsIgnoreCase("General")){
					consoleAxiomTypesGeneral(pFolderPath + "/" + file.getName());
				}else if (pFormat.equalsIgnoreCase("Logical")){
					consoleAxiomTypesLogical(pFolderPath + "/" + file.getName());
				}
 				
				processTime = System.nanoTime() - start;
				System.out.println("--- End of Task. (" + (processTime/1000000) + " msec.) ---");
			}
		}

		return "";
	}

	
	public static void consoleTBoxAxioms(String pFileName){		
		try {

			File ontFile = new File(pFileName);
			OWLOntologyManager om = OWLManager.createOWLOntologyManager();
			OWLOntology ont = om.loadOntologyFromOntologyDocument(ontFile);
			System.out.println(ont.getTBoxAxioms(true).toString().replace(",", ",\n"));
			
		} catch (Exception e) {
			e.printStackTrace();
		} 
	}

	public static void consoleAxiomTypesGeneral(String pFileName){		
		try {

			File ontFile = new File(pFileName);
			OWLOntologyManager om = OWLManager.createOWLOntologyManager();
			OWLOntology ont = om.loadOntologyFromOntologyDocument(ontFile);
			System.out.println("Ontology: " + ontFile.toString() + " - " + ont.getAxioms().size() + " axioms.");
			System.out.println("(Axiom Types)");
			System.out.println("-------------");
			
			Map<String, Integer> dMap = new HashMap<String, Integer>();
			
			for (OWLAxiom axiom: ont.getAxioms()) {			
				Integer dCount = dMap.get(axiom.getAxiomType().toString());
				if (dCount == null){
					dMap.put(axiom.getAxiomType().toString(), 1);
				}else{
					dMap.put(axiom.getAxiomType().toString(), dCount + 1);
				}
				
			}
			
			Iterator it = dMap.entrySet().iterator();
			while(it.hasNext()){
				Map.Entry pair = (Map.Entry) it.next();
				System.out.println(pair.getKey() + " = " + pair.getValue());
				it.remove();
			}
			System.out.println("TOTAL = " + ont.getAxioms().size());
			System.out.println("***");
						
		} catch (Exception e) {
			e.printStackTrace();
		} 
	}
	
	public static void consoleAxiomTypesLogical(String pFileName){		
		try {

			File ontFile = new File(pFileName);
			OWLOntologyManager om = OWLManager.createOWLOntologyManager();
			OWLOntology ont = om.loadOntologyFromOntologyDocument(ontFile);
			System.out.println("Ontology: " + ontFile.toString() + " - " + ont.getAxioms().size() + " axioms.");
			System.out.println("(Logical Axiom Types)");
			System.out.println("-------------");
			Map<String, Integer> dMap = new HashMap<String, Integer>();
			
			for (OWLAxiom axiom: ont.getLogicalAxioms()) {
				Integer dCount = dMap.get(axiom.getAxiomType().toString());
				if (dCount == null){
					dMap.put(axiom.getAxiomType().toString(), 1);
				}else{
					dMap.put(axiom.getAxiomType().toString(), dCount + 1);
				}
				
			}
			
			Iterator it = dMap.entrySet().iterator();
			while(it.hasNext()){
				Map.Entry pair = (Map.Entry) it.next();
				System.out.println(pair.getKey() + " = " + pair.getValue());
				it.remove();
			}
			System.out.println("TOTAL = " + ont.getAxioms().size());
			
		} catch (Exception e) {
			e.printStackTrace();
		} 
	}

}
