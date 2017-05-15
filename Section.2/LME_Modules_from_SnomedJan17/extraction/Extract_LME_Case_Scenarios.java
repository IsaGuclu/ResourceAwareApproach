package uk.ac.manchester.syntactic_locality.test;

import java.io.File;
import java.io.FileOutputStream;
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
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import uk.ac.manchester.syntactic_locality.ModuleExtractor;

public class Extract_LME_Case_Scenarios {

	/**
	 * @param args
	 */
	public static void main(String[] args) {

		try{
			String dOntoPath = "/Users/isa/GDrive/UoA/ISWC2017/03Codes/LocalityModuleExtractor/assets/snomed_jan17.owl";
			String dSignPath = "/Users/isa/GDrive/UoA/ISWC2017/03Codes/LocalityModuleExtractor/assets/signaturesSnomedJan17_3sp.txt"; // All signature files are in the containing folder
			String[] dTypesOfModule = {"LM", "UM", "LUM", "ULM", "DCM", "DRM"};
			for (String  dTypeOfModule : dTypesOfModule) {
				String dOutputFile = "/Users/isa/Desktop/out/snomed_jan17_" + dTypeOfModule + "_3sp.owl";
				PrintStream out = new PrintStream(new FileOutputStream("/Users/isa/Desktop/out/snomed_jan17_" + dTypeOfModule + "_3sp.log"));
				System.setOut(out);
				System.out.println("Module Output : " + dOutputFile);
				CreatePhysicalOntologyModule dCls = new CreatePhysicalOntologyModule("file:////" + dOntoPath, dSignPath, dTypeOfModule, dOutputFile);
			}
			/*
			String dTypeOfModule = "DRM";		
			String dOutputFile = "/Users/isa/Desktop/out/snomed_jan17_" + dTypeOfModule + "_1sp.owl";
			System.out.println("Module Output : " + dOutputFile);
			CreatePhysicalOntologyModule dCls = new CreatePhysicalOntologyModule("file:////" + dOntoPath, dSignPath, dTypeOfModule, dOutputFile);
			*/
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
}
