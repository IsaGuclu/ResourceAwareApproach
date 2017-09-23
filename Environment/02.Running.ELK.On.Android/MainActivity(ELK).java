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
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.ref.WeakReference;
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
import android.content.ComponentName;
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

	int ontologyId = 0;
	OntologyWrap[] ontologies;
	Reasoner reasoner;

	String glbFolderOntologies = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
			+ File.separator + "ontologies";
	String glbFolderLogs = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
			+ File.separator + "logsRuning";
	String glbOntoFileName = "";

	String glbStringOfNow = "";
	long glbTimeStarting = 0;

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
		ArrayAdapter<OntologyWrap> adapter = new ArrayAdapter<OntologyWrap>(this, android.R.layout.simple_spinner_item,
				ontologies);

		// Specify the layout to use when the list of choices appears
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		// Apply the adapter to the spinner
		spinner.setAdapter(adapter);
		spinner.setOnItemSelectedListener(new SpinnerActivity());

		// creating log screen
		TextView tv = (TextView) findViewById(R.id.logText);
		tv.setMovementMethod(new ScrollingMovementMethod());
		// Logger.getRootLogger().addAppender(new TextViewAppender(tv));

		// set default log level from preferences
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		// Logger.getRootLogger().setLevel( SettingsActivity.getLogLevel(this,
		// prefs));
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		/*
		 * switch (item.getItemId()) {
		 * 
		 * 
		 * case R.id.action_settings: Intent i = new Intent(this,
		 * SettingsActivity.class); startActivityForResult(i, 0); break; }
		 */
		return true;
	}

	public static void myGC() {
		try {

			Runtime r = Runtime.getRuntime();
			r.gc();
			System.gc();
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
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

	public class SpinnerActivity extends Activity implements OnItemSelectedListener {
		@Override
		public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
			ontologyId = ontologies[pos].getResourceId();
			/*
			if (reasoner != null) {
				try {
					reasoner.shutdown();
				} catch (InterruptedException e) {
				}
				reasoner = null;
			}
			*/
		}

		@Override
		public void onNothingSelected(AdapterView<?> parent) {
			// nothing changes
		}
	}

	
	public class ProgressIndicator implements ProgressMonitor {
		private final ProgressBar mProgress_ = (ProgressBar) MainActivity.this.findViewById(R.id.reasonerProgress);
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


	// Isa - Run ELK

	public void runViaELK(View view) {

		File folder = new File(glbFolderOntologies);
		File[] listOfFiles = folder.listFiles();
		for (int i = 0; i < listOfFiles.length; i++) {
			if (listOfFiles[i].isFile()) {
				System.out.println("File sent to ELK4Android: " + listOfFiles[i].getName().toString());
				glbOntoFileName = listOfFiles[i].getName().toString();
				executeClassification(glbFolderOntologies + File.separator + glbOntoFileName);
			}
		}

	}

	public void executeClassification(String pOntology) {

		long timeAfterClassification = 0;

		try {

			String dOntology = pOntology;
			System.out.println("Ontology File :" + dOntology);

			glbStringOfNow = StringOfNow();
			glbTimeStarting = System.nanoTime();

			initReasoner_Isa(dOntology);
			reasoner.getTaxonomyQuietly();

			timeAfterClassification = System.nanoTime();

			reasoner.shutdown();
			reasoner = null;

			writeText("", glbStringOfNow + "_" + glbOntoFileName, "--- Ontology Processing by ELK4Android ---\n");
			writeText("", glbStringOfNow + "_" + glbOntoFileName, "Ontology File:" + glbOntoFileName + "\n");
			writeText("", glbStringOfNow + "_" + glbOntoFileName, "Execution Started:" + glbStringOfNow + "\n");

			System.out.println("Classifying Ontology took : " + ((timeAfterClassification - glbTimeStarting) / 1000000)
					+ " msec.");
			writeText("", glbStringOfNow + "_" + glbOntoFileName, "Classifying Ontology took : "
					+ ((timeAfterClassification - glbTimeStarting) / 1000000) + " msec.\n");
			System.out.println("--- [" + glbOntoFileName + "] finished successfuly ---");
			writeText("", glbStringOfNow + "_" + glbOntoFileName,
					"--- [" + glbOntoFileName + "] finished successfuly ---\n");

			myGC();

		} catch (Exception E) {
			WriteException(E.toString());
		}

	}

	private void initReasoner_Isa(String pOntologyFullPath) {
		try {
			// create the reasoner
			ReasonerFactory reasoningFactory = new ReasonerFactory();
			ReasonerConfiguration configuration = ReasonerConfiguration.getConfiguration();
			Owl2ParserFactory parserFactory = new Owl2FunctionalStyleParserFactory();
			AxiomLoader loader;

			loader = new Owl2StreamLoader(parserFactory, new File(pOntologyFullPath));

			reasoner = reasoningFactory.createReasoner(loader, new LoggingStageExecutor(), configuration);
			// set the parameters from preferences
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
			int workers = 1;
			reasoner.setNumberOfWorkers(workers);

		} catch (Exception e) {
			System.out.println("Error in INITIALIZING ELK REASONER.");
			e.printStackTrace();
		}

	}
	

	/**************************************
	 * Isa - Utils
	 **************************************/

	public void writeText(String pFolderName, String pFileName, String pText) {
		String dFilename = "";

		BufferedWriter bw = null;
		FileWriter fw = null;

		try {

			if (pFolderName.equalsIgnoreCase("")) {
				File directory1 = new File(glbFolderLogs);

				if (!directory1.exists()) {
					directory1.mkdir();
				}

				dFilename = glbFolderLogs + File.separator + pFileName + ".txt";
			} else {
				File directory2 = new File(glbFolderLogs + File.separator + pFolderName);
				if (!directory2.exists()) {
					directory2.mkdir();
				}
				dFilename = directory2.toString() + File.separator + pFileName + ".txt";
			}

			File file = new File(dFilename);
			// if file doesnt exists, then create it
			if (!file.exists()) {
				file.createNewFile();
			}
			// true = append file
			fw = new FileWriter(file.getAbsoluteFile(), true);
			bw = new BufferedWriter(fw);

			bw.write(pText);

		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (bw != null)
					bw.close();
				if (fw != null)
					fw.close();
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}

	}

	public void WriteException(String ErrContent) {
		writeText("ErrorTrOWL4Android", glbOntoFileName, glbOntoFileName + "\n" + ErrContent);
	}

	public String StringOfNow() {
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd.HHmmss.SSS");
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
