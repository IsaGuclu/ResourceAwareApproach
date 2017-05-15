package org.semanticweb.elk.android;

/*
 * #%L
 * ELK Android App
 * $Id:$
 * $HeadURL:$
 * %%
 * Copyright (C) 2011 - 2013 Department of Computer Science, University of Oxford
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.semanticweb.elk.loading.AxiomLoader;
import org.semanticweb.elk.loading.Owl2StreamLoader;
import org.semanticweb.elk.owl.exceptions.ElkException;
import org.semanticweb.elk.owl.parsing.Owl2ParserFactory;
import org.semanticweb.elk.owl.parsing.javacc.Owl2FunctionalStyleParserFactory;
import org.semanticweb.elk.reasoner.ProgressMonitor;
import org.semanticweb.elk.reasoner.Reasoner;
import org.semanticweb.elk.reasoner.ReasonerFactory;
import org.semanticweb.elk.reasoner.config.ReasonerConfiguration;
import org.semanticweb.elk.reasoner.stages.LoggingStageExecutor;
import org.semanticweb.elk.util.logging.CachedTimeThread;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.OWLFunctionalSyntaxOntologyFormat;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.text.method.ScrollingMovementMethod;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

public class MainActivity extends Activity {

	// private static final Logger logger_ =
	// Logger.getLogger(MainActivity.class);
	
	int ontologyId = 0;
	OntologyWrap[] ontologies;
	Reasoner reasoner;

	private Timer timer;
	private Runtime runtime = Runtime.getRuntime();
	String ontologyName = "";
	// String logRAMFileName = "";
	String glbStringOfNow = "";
	long usedMemoryBefore;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_main);

		Field[] rawOntologies = R.raw.class.getFields();
		ontologies = new OntologyWrap[rawOntologies.length];
		for (int i = 0; i < rawOntologies.length; i++) {
			Field rawOntology = rawOntologies[i];
			if (rawOntology.getName().startsWith("ont"))
				ontologies[i] = new OntologyWrap(rawOntology);
		}

		// load default preferences
		PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

		// creating ontology list
		Spinner spinner = (Spinner) findViewById(R.id.ontology_spinner);
		ArrayAdapter<OntologyWrap> adapter = new ArrayAdapter<OntologyWrap>(
				this, android.R.layout.simple_spinner_item, ontologies);

		// Specify the layout to use when the list of choices appears
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		// Apply the adapter to the spinner
		spinner.setAdapter(adapter);
		spinner.setOnItemSelectedListener(new SpinnerActivity());

		// creating log screen
		TextView tv = (TextView) findViewById(R.id.logText);
		tv.setMovementMethod(new ScrollingMovementMethod());
		Logger.getRootLogger().addAppender(new TextViewAppender(tv));

		// set default log level from preferences
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(this);
		Logger.getRootLogger().setLevel(
				SettingsActivity.getLogLevel(this, prefs));
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {

		case R.id.action_settings:
			Intent i = new Intent(this, SettingsActivity.class);
			startActivityForResult(i, 0);
			break;
		}

		return true;
	}

	/** Called when the user clicks the run button */
	public void run(View view) {
		deactivateOntologySelection();
		
		// Isa: Here starts iteration over an Ontology, and additional code 
		glbStringOfNow = StringOfNow();
		usedMemoryBefore = (runtime.totalMemory() - runtime.freeMemory())  / 1024;
		// start();
		System.out.println("Used Memory before: " + usedMemoryBefore + " KB.");
		// Isa: Here ends pre-additional code
		
		if (reasoner == null)
			initReasoner();

		final Thread reasonerThread = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					reasoner.getTaxonomyQuietly();
				} catch (ElkException e) {
					e.printStackTrace();
				} finally {
					
					// Isa: Here finishes iteration over an Ontology
					long usedMemoryAfter = (runtime.totalMemory() - runtime.freeMemory()) / 1024;
					System.out.println("RAM Beginning:" + usedMemoryBefore + ": KB. RAM End:" + usedMemoryAfter + ": KB. RAM Diff:" + (usedMemoryAfter - usedMemoryBefore) + ": KB.");
					// Isa: Here ends post-additional code
					
					try {
						reasoner.shutdown();
						reasoner = null;
						
						// stop();// Isa: Here stops the timer logging available RAM consumption.
						
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					Spinner spinner = (Spinner) findViewById(R.id.ontology_spinner);
					spinner.post(new Runnable() {
						@Override
						public void run() {
							activateOntologySelection();
						}
					});
				}
			}
		});
		reasonerThread.start();
	}

	/** Called when the user clicks the stop button */
	public void stop(View view) {
		if (reasoner != null)
			reasoner.interrupt();
	}

	private void initReasoner() {
		// create the reasoner
		ReasonerFactory reasoningFactory = new ReasonerFactory();
		ReasonerConfiguration configuration = ReasonerConfiguration
				.getConfiguration();
		Owl2ParserFactory parserFactory = new Owl2FunctionalStyleParserFactory();
		AxiomLoader loader = new Owl2StreamLoader(parserFactory, this
				.getResources().openRawResource(ontologyId));
		
		reasoner = reasoningFactory.createReasoner(loader,
				new LoggingStageExecutor(), configuration);
		reasoner.setProgressMonitor(new ProgressIndicator());
		// set the parameters from preferences
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(this);
		int workers = Integer.valueOf(prefs.getString(
				SettingsActivity.KEY_PREF_WORKERS, "1"));
		reasoner.setNumberOfWorkers(workers);		
		
	}
	
	private void deactivateOntologySelection() {
		Spinner spinner = (Spinner) findViewById(R.id.ontology_spinner);
		spinner.setEnabled(false);
		Button run = (Button) findViewById(R.id.run);
		Button stop = (Button) findViewById(R.id.stop);
		run.setVisibility(View.GONE);
		stop.setVisibility(View.VISIBLE);
	}

	private void activateOntologySelection() {
		Spinner spinner = (Spinner) findViewById(R.id.ontology_spinner);
		spinner.setEnabled(true);
		Button run = (Button) findViewById(R.id.run);
		Button stop = (Button) findViewById(R.id.stop);
		stop.setVisibility(View.GONE);
		run.setVisibility(View.VISIBLE);
	}

	public static class OntologyWrap {
		private final Field ontology_;

		public OntologyWrap(Field ontology) {
			this.ontology_ = ontology;
		}

		@Override
		public String toString() {
			String fullName = ontology_.getName();
			return fullName.substring(fullName.indexOf("_") + 1) + ".owl";
		}

		public int getResourceId() {
			try {
				return ontology_.getInt(ontology_);
			} catch (IllegalArgumentException e) {
				return 0;
			} catch (IllegalAccessException e) {
				return 0;
			}
		}
	}

	public class SpinnerActivity extends Activity implements
			OnItemSelectedListener {
		@Override
		public void onItemSelected(AdapterView<?> parent, View view, int pos,
				long id) {
			ontologyId = ontologies[pos].getResourceId();
			if (reasoner != null) {
				try {
					reasoner.shutdown();
				} catch (InterruptedException e) {
				}
				reasoner = null;
			}
		}

		@Override
		public void onNothingSelected(AdapterView<?> parent) {
			// nothing changes
		}
	}

	public class ProgressIndicator implements ProgressMonitor {
		private final ProgressBar mProgress_ = (ProgressBar) MainActivity.this
				.findViewById(R.id.reasonerProgress);
		private static final int UPDATE_INTERVAL = 1; // in ms
		private long lastUpdate_ = 0;
		private int currentMaxState_ = 0;

		@Override
		public void finish() {
			mProgress_.setProgress(currentMaxState_);
		}

		@Override
		public void report(final int state, final int maxState) {
			long time = CachedTimeThread.currentTimeMillis;
			if (time < lastUpdate_ + UPDATE_INTERVAL)
				return;
			lastUpdate_ = time;
			if (maxState != currentMaxState_) {
				currentMaxState_ = maxState;
				mProgress_.setMax(maxState);
			}
			mProgress_.setProgress(state);
		}

		@Override
		public void start(String message) {
			mProgress_.setProgress(0);

		}
	}
	
	/************************************** Isa - Run Extracted **************************************/
	

	public void runExtracted(View view) {
		
		File folder = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
				+ File.separator + "ontologies" + File.separator + "extracted");
		File[] listOfFiles = folder.listFiles();
		for (int i = 0; i < listOfFiles.length; i++) {
		// for (int i = 44; i < listOfFiles.length; i++) {	
			if (listOfFiles[i].isFile()) {
				try {
					String dFile = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
							+ File.separator + "ontologies" + File.separator + "extracted"
							+ File.separator + listOfFiles[i].getName().toString();
					glbStringOfNow = StringOfNow();
					glbOntoOriginalFileName = listOfFiles[i].getName().toString();
					System.out.println("Ontology File -> " + dFile);
					
					long dTempTime; long dTempMemory;
					startTime = System.nanoTime();
					usedMemoryBefore = (runtime.totalMemory() - runtime.freeMemory()) / 1024;
					
					System.out.println("Used Memory before: " + usedMemoryBefore + " KB.");
					writeText("ontologies" + File.separator + "LogsRuningExtracted", glbStringOfNow + "_" + glbOntoOriginalFileName, "--- Ontology Processing by ELK ---\n");
					writeText("ontologies" + File.separator + "LogsRuningExtracted", glbStringOfNow + "_" + glbOntoOriginalFileName, "Used Memory before: " + usedMemoryBefore + " KB.\n");
					
					initReasoner_Isa(dFile);
					
					/*
					dTempTime = System.nanoTime();
					dTempMemory = (runtime.totalMemory() - runtime.freeMemory()) / 1024;
					System.out.println("(Initiation) RAM Beginning:" + usedMemoryBefore+ ": KB. RAM End:" + dTempMemory + ": KB. RAM Diff:" + (dTempMemory - usedMemoryBefore) + ": KB.");
					writeText("ontologies" + File.separator + "LogsRuningExtracted", glbStringOfNow + "_" + glbOntoOriginalFileName, "(Initiation) RAM Beginning:" + usedMemoryBefore+ ": KB. RAM End:" + dTempMemory + ": KB. RAM Diff:" + (dTempMemory - usedMemoryBefore) + ": KB. \n");
					System.out.println("Initiation of ELK took " + ((dTempTime - startTime) / 1000000) + " msec.");
					writeText("ontologies" + File.separator + "LogsRuningExtracted", glbStringOfNow + "_" + glbOntoOriginalFileName, "Initiation of ELK took " + ((dTempTime - startTime) / 1000000) + " msec. \n");
					*/
					
					reasoner.getTaxonomyQuietly();
					reasoner.shutdown();
					reasoner = null;

					dTempTime = System.nanoTime();
					dTempMemory = (runtime.totalMemory() - runtime.freeMemory()) / 1024;
					System.out.println("(Classification) RAM Beginning:" + usedMemoryBefore+ ": KB. RAM End:" + dTempMemory + ": KB. RAM Diff:" + (dTempMemory - usedMemoryBefore) + ": KB.");
					writeText("ontologies" + File.separator + "LogsRuningExtracted", glbStringOfNow + "_" + glbOntoOriginalFileName, "(Classification) RAM Beginning:" + usedMemoryBefore+ ": KB. RAM End:" + dTempMemory + ": KB. RAM Diff:" + (dTempMemory - usedMemoryBefore) + ": KB. \n");
					System.out.println("Classification of ELK took " + ((dTempTime - startTime) / 1000000) + " msec.");
					writeText("ontologies" + File.separator + "LogsRuningExtracted", glbStringOfNow + "_" + glbOntoOriginalFileName, "Classification of ELK took " + ((dTempTime - startTime) / 1000000) + " msec. \n");
					
				} catch (Exception e) {
					e.printStackTrace();
				} finally {
					if (reasoner != null) {
						try {
							reasoner.shutdown();
							reasoner = null;
						} catch (Exception e2) {
							e2.printStackTrace();
						}
					}
				}
				System.out.println("*** GC STARTED ***");
				try {
					System.gc();
					Thread.sleep(3000);
					System.gc();
					Thread.sleep(3000);
				} catch (InterruptedException e) {
					System.out.println("--- Problem With Garbage Collection --");
					e.printStackTrace();
				}
				System.out.println("*** GC FINISHED ***");
			}
		}

		
	}
		
	private void initReasoner_Isa(String pOntologyFullPath) {
		try {
			// create the reasoner
			ReasonerFactory reasoningFactory = new ReasonerFactory();
			ReasonerConfiguration configuration = ReasonerConfiguration
					.getConfiguration();
			Owl2ParserFactory parserFactory = new Owl2FunctionalStyleParserFactory();
			AxiomLoader loader;

			loader = new Owl2StreamLoader(parserFactory, new File(
					pOntologyFullPath));

			// System.out.println("ontologyId : " + ontologyId);
			// System.out.println("this.getResources().openRawResource(ontologyId) : " + this.getResources().openRawResource(ontologyId).toString());
			// System.out.println("loader : " + loader.toString());

			reasoner = reasoningFactory.createReasoner(loader,
					new LoggingStageExecutor(), configuration);
			reasoner.setProgressMonitor(new ProgressIndicator());
			// set the parameters from preferences
			SharedPreferences prefs = PreferenceManager
					.getDefaultSharedPreferences(this);
			int workers = Integer.valueOf(prefs.getString(
					SettingsActivity.KEY_PREF_WORKERS, "1"));
			reasoner.setNumberOfWorkers(workers);

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

	}
	
	
	/************************************** Isa - Knowledge Extraction **************************************/
	
	boolean glbContinueIteration;
	// int glbMaxAxiomCount;
	
	long startTime;
	long loadingTime;
	long processTime;
	long ontoWritingTime;
	long iterationCount;
	String glbOntoFolder;
	String glbOntoOriginalFileName;
	
	/** Called when the user clicks the Extract button */
	public void extractKnowledge(View view) {

		final Thread extractorThread = new Thread(new Runnable() {
			@Override
			public void run() {
				int dLine = 0;
				try {

					glbOntoFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + File.separator + "ontologies";
					glbOntoOriginalFileName = "snomed_jan17";
					String dDesiredFileName = "Desired_4thScenario_snomed_jan17.txt";
					String dAxiomCountFileName = "AxiomCounts4thScenario.txt";
					
					System.out.println("dOntoOriginalFullPath : " + glbOntoFolder + File.separator + glbOntoOriginalFileName + ".owl");
					System.out.println("dDesiredFullPath : " + glbOntoFolder + File.separator + dDesiredFileName);
					
					/* 
					String[] dModuleTypes = {"LM", "UM", "LUM", "ULM", "DCM", "DRM"};
					for(int i=0; i< dModuleTypes.length ;i++){
						System.out.println("*** GC STARTED ***");
						try {
							System.gc();
							Thread.sleep(1000);
							System.gc();
							Thread.sleep(1000);
						} catch (InterruptedException e) {
							System.out.println("--- Problem With Garbage Collection --");
							e.printStackTrace();
						}
						System.out.println("*** GC FINISHED ***");
						
						ExtractorMod dCls = new ExtractorMod();
						dCls.extractKnowledge(glbOntoFolder + File.separator + glbOntoOriginalFileName + ".owl", glbOntoFolder + File.separator + dDesiredFileName, dModuleTypes[i]); //"LUM"
						
					}
					*/
					
					
					BufferedReader br = new BufferedReader(new FileReader(glbOntoFolder + File.separator + dAxiomCountFileName));
					String line;
					while ((line = br.readLine()) != null) {

						usedMemoryBefore = 0;						
						startTime = 0;
						loadingTime = 0;
						processTime = 0;
						ontoWritingTime = 0;
						iterationCount = 0;
						dLine = Integer.parseInt(line);
						System.out.println("parseInt(line) : " + dLine);
						System.out.println("dOntoOriginalFullPath : " + glbOntoFolder + File.separator + glbOntoOriginalFileName + ".owl");

						System.out.println("*** GC-1 STARTED ***");
						try {
							System.gc();
							Thread.sleep(5000);
							System.gc();
							Thread.sleep(5000);
						} catch (InterruptedException e) {
							System.out.println("--- Problem With Garbage Collection --");
							e.printStackTrace();
						}
						System.out.println("*** GC-1 FINISHED ***");
						
 						ExtractorExtended dCls = new ExtractorExtended();
						dCls.extractKnowledge(glbOntoFolder + File.separator + glbOntoOriginalFileName + ".owl", glbOntoFolder + File.separator + dDesiredFileName, dLine);
						
						
					
						Thread.sleep(1000);
					}
					br.close();
						
						
						
				} catch (Exception e) {

					writeText("ontologies" + File.separator + "LogsExtractionError", glbStringOfNow + "_" + glbOntoOriginalFileName + "_" + dLine + "_main", 
							"Exception.toString():\n" + e.toString() + "\n\n *** \n\nException.getMessage():\n" + e.getMessage() + "\n");
					
					StringWriter errors = new StringWriter();
					e.printStackTrace(new PrintWriter(errors));
					
					writeText("ontologies" + File.separator + "LogsExtractionError", glbStringOfNow + "_" + glbOntoOriginalFileName + "_" + dLine + "_main", 
							"\n *** \n\nException.printStackTrace():\n" + errors.toString());
					
					e.printStackTrace();
					
				} finally {

				}
			}
		});
		extractorThread.start();

	}

	/************************************** Isa - Utils **************************************/
	
	/**
	 * Starts timer that logs RAM consumption.
	 */
	public void start() {
		if (timer != null) {
			return;
		}
		timer = new Timer();
		timer.schedule(new TimerTask() {
			/**
			 * Timer calls method run every second. Within run, RAM CONSUMPTION
			 * is calculated. Method invokes
			 */

			public void run() {

				writeText("LogELK_RAM", glbStringOfNow + "_" + ontologyId, StringOfNow() + ":" + MemInfo() + "\n");

				/*
				 * runOnUiThread(new Runnable() {
				 * 
				 * @Override public void run() {
				 * 
				 * Date RightNow = new Date(); float timeElapsed = (float)
				 * ((float) DateDiffMsec(Begining, RightNow) / 1000.0); // This
				 * if ABORTS the reasoning task because it took too if
				 * (timeElapsed > 110) { moveFile(folder, ontologyName, "" +
				 * folder + File.separator + "TrOWLError"); quiteAnApp(1); } }
				 * });
				 */

			}
		}, 0, 1000);
	}
	
	/**
	 * Stops the previously launched Timer.
	 */
	public void stop() {
		if (timer != null) {
			timer.cancel();
		}
		timer = null;
	}

	public String MemInfo() {
		StringBuilder sb = new StringBuilder();

		long usedMemoryAfter = (runtime.totalMemory() - runtime.freeMemory()) / 1024;
		sb.append("RAM Beginning:");
		sb.append(usedMemoryBefore);
		sb.append(": KB. RAM End:");
		sb.append(usedMemoryAfter);
		sb.append(": KB. RAM Diff:");
		sb.append((usedMemoryAfter - usedMemoryBefore) + ": KB.");
		return sb.toString();

	}

	public void writeText(String pFolderName, String pFileName, String pText) {
		String dFilename = "";
		String temp = "";
		BufferedWriter writer = null;
		try {
			if (pFolderName.equalsIgnoreCase("")) {
				dFilename = Environment
						.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
						+ File.separator + pFileName + ".txt";
				temp = read(dFilename);
			} else {
				File directory = new File(
						Environment
								.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
								+ File.separator + pFolderName);
				if (!directory.exists()) {
					directory.mkdir();
				}
				dFilename = directory.toString() + File.separator + pFileName
						+ ".txt";
				temp = read(dFilename);
			}

			File logFile = new File(dFilename);
			// System.out.println(logFile.getCanonicalPath());
			writer = new BufferedWriter(new FileWriter(logFile));
			writer.write(temp + pText);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				writer.close(); // Close the writer regardless of what
								// happens...
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public String read(String fname) {
		BufferedReader br = null;
		String response = null;
		try {
			StringBuffer output = new StringBuffer();
			String fpath = fname;
			br = new BufferedReader(new FileReader(fpath));
			String line = "";
			while ((line = br.readLine()) != null) {
				output.append(line + "\n");
			}
			response = output.toString();
			br.close();
		} catch (Exception e) { // e.printStackTrace();
			return "";
		}
		return response;
	}

	public String StringOfNow() {
		SimpleDateFormat dateFormat = new SimpleDateFormat(
				"yyyyMMdd.HHmmss.SSS");
		Date date = new Date();
		String dNow = dateFormat.format(date);
		// System.out.println(dNow);
		return dNow;
	}

	public static long DateDiffMsec(Date date1, Date date2) {
		long diffInMillies = date2.getTime() - date1.getTime();
		TimeUnit timeUnit = TimeUnit.MILLISECONDS;
		return timeUnit.convert(diffInMillies, TimeUnit.MILLISECONDS);
	}
	
	
		 
}
