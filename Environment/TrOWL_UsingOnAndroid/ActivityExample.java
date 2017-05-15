package tr.tech.adaptui;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.DialogInterface.OnCancelListener;
import android.os.AsyncTask;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Environment;
/*import android.support.multidex.MultiDex;
import android.support.multidex.MultiDexApplication;*/
import android.view.View;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import tr.tech.adaptui.R;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.reasoner.InferenceType;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.util.OWLOntologyMerger;

import org.semanticweb.sparql.OWLReasonerSPARQLEngine;
import org.semanticweb.sparql.arq.OWLOntologyDataSet;
import org.semanticweb.sparql.arq.TrOWLOntologyGraph;
import org.semanticweb.sparql.bgpevaluation.monitor.MinimalPrintingMonitor;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.ResultSetFormatter;
import com.hp.hpl.jena.sparql.function.library.date;

import eu.trowl.owl.api3.ReasonerFactory;
import eu.trowl.owlapi3.rel.reasoner.el.*;

/**
 * Class MainActivity extends the Activity class and its purpose is to create
 * the TrOWL reasoner which is capable to perform the tasks it has been
 * assigned. It also is capable of Measuring the power it drained as well as
 * record the results into different files.
 * 
 * @author Isa Guclu Code of Edgaras Valincius is utilized)
 * @version 1.0
 * @since 2016-02-16
 */
@SuppressLint("SdCardPath")
public class ActivityExample extends Activity {

	private GridLayout layout;
	private ProgressDialog progressDialog;


	Runtime runtime = Runtime.getRuntime();
	String glbFolder;
	String glbOntoFileName = "";
	String glbStringOfNow = "";
	long glbTimeStarting = 0;
	long glbUsedMemoryInTheBeginning = 0;
	
	/**
	 * onCreate is used to initialize activity. This method launches the
	 * AsyncTask class that allows to use background operations.Method also gets
	 * the extras from the intent.
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main_activity);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		Intent myIntent = getIntent(); // gets the previously created intent
		String datasetFileName = myIntent.getStringExtra("ontologyFile");
		String queryName = myIntent.getStringExtra("queryName");
		String ontologyName = myIntent.getStringExtra("ontologyName");
		if (datasetFileName == null) {
			System.out.println("CLOSED. Dataset Empty");
			// Thread is used to hold the activity, before closing it, so
			// the Toast have enough time to show its message.
			Thread thread = new Thread() {
				@Override
				public void run() {
					try {
						Thread.sleep(1000); // As I am using LENGTH_LONG in
											// Toast
						finish();
						System.exit(0);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			};

			Toast.makeText(getApplicationContext(), "Launch From The PowerBenchMark app", Toast.LENGTH_LONG).show();
			thread.start();

		} else {

			progressDialog = new ProgressDialog(this);
			progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
			progressDialog.setMessage("Please Wait");
			progressDialog.setCanceledOnTouchOutside(false);
			progressDialog.setCancelable(true);
			progressDialog.setOnCancelListener(new OnCancelListener() {
				@Override
				public void onCancel(DialogInterface dialog) {
					onBackPressed();
				}
			});
			// display dialog
			progressDialog.show();
			// start async task
			new MyAsyncTaskClass().execute();
		}
	}

	/**
	 * Creates Asynchronous tasks on one same UI thread. In this case progress
	 * wheel and the calculations are performed asynchronously.
	 */
	private class MyAsyncTaskClass extends AsyncTask<Void, Void, Void> {
		/**
		 * Method performs a computation on a background thread.
		 */
		@Override
		protected Void doInBackground(Void... params) {
			layout = (GridLayout) findViewById(R.id.layout);
			Collection<View> views = new ArrayList<View>();
			views.add(layout);
			try {
				/*
				File folder = new File(
						Environment
								.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
								+ File.separator
								+ "ontologies"
								+ File.separator + "extracted");
				glbFolder = folder.toString();
				
				File[] listOfFiles = folder.listFiles();

				ArrayList<String> sListFiles = new ArrayList<String>();

				
				
				for (int i = 0; i < listOfFiles.length; i++) {
					if (listOfFiles[i].isFile()) {
						try {
							executeClassification(listOfFiles[i].getName().toString());
							Thread.sleep(1000);
						} catch (Exception e) {
							WriteException("NOT WORKED: doInBackground: "
									+ e.toString());
						}
					}
				}
				*/
				
				String dOntoFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + File.separator + "ontologies";
				String dOntoFileName = "snomed_jan17.owl";
				String dDesiredFileName = "Desired_4thScenario_snomed_jan17.txt";
				String dAxiomCountFileName = "AxiomCounts4thScenario.txt";
				System.out.println("dOntoOriginalFullPath : " + dOntoFolder + File.separator + dOntoFileName);
				System.out.println("dDesiredFullPath : " + dOntoFolder + File.separator + dDesiredFileName);

				Extractor dCls = new Extractor();

				BufferedReader br = new BufferedReader(new FileReader(dOntoFolder + File.separator + dAxiomCountFileName));
				int dLine = 0;
				String line;
				while ((line = br.readLine()) != null) {
					dLine = Integer.parseInt(line);
					System.out.println("*** parseInt(line) : " + dLine);
					dCls.extractKnowledge(dOntoFolder + File.separator + dOntoFileName, 
							dOntoFolder + File.separator + dDesiredFileName, dLine);
					Thread.sleep(1000L);
				}
				br.close();
			} catch (Exception e) {
				System.out.println("Error in main: " + e.toString());
				e.printStackTrace();
			}

			return null;
		}

		/**
		 * Runs on the UI thread after doInBackground.
		 */
		@Override
		protected void onPostExecute(Void result) {
			progressDialog.dismiss();
			finishWithResult(1);
			finish();
			System.exit(0);
		}
	}

	public void executeClassification(String pOntologyName) {

		glbStringOfNow = StringOfNow();
		glbOntoFileName = pOntologyName;
		
		long timeAfterLoading = 0;
		long timeAfterClassification = 0;
		long usedMemoryAfterLoading = 0;
		long usedMemoryAfterClassification = 0;
		OWLOntology ont = null;
		OWLOntologyManager om = null;
		RELReasoner reasoner = null;
		
		try {
			pOntologyName = "file:///" + glbFolder + "/" + pOntologyName;
			System.out.println("Ontology File :" + pOntologyName);
			// Isa: Here starts LOADING of an Ontology
			glbTimeStarting = System.nanoTime();
			glbUsedMemoryInTheBeginning = (runtime.totalMemory() - runtime.freeMemory())  / 1024;
			System.out.println("Used RAM In The Beginning: " + glbUsedMemoryInTheBeginning + " KB.");
			writeText("ontologies" + File.separator + "LogsRuningExtracted", glbStringOfNow + "_" + glbOntoFileName, "--- Ontology Processing by TROWL ---\n");
			writeText("ontologies" + File.separator + "LogsRuningExtracted", glbStringOfNow + "_" + glbOntoFileName, "Ontology File :" + pOntologyName + "\n");
			writeText("ontologies" + File.separator + "LogsRuningExtracted", glbStringOfNow + "_" + glbOntoFileName, "Used RAM In The Beginning: " + glbUsedMemoryInTheBeginning + " KB.\n");
			
			
			
			om = OWLManager.createOWLOntologyManager();
			ont = om.loadOntology(IRI.create(pOntologyName));
	
			
			// Isa: Here finishes LOADING of an Ontology
			timeAfterLoading = System.nanoTime();
			usedMemoryAfterLoading = (runtime.totalMemory() - runtime.freeMemory()) / 1024;
			System.out.println("Loading Ontology took : " + ((timeAfterLoading - glbTimeStarting) / 1000000) + " msec.");
			writeText("ontologies" + File.separator + "LogsRuningExtracted", glbStringOfNow + "_" + glbOntoFileName, "Loading Ontology took : " + ((timeAfterLoading - glbTimeStarting) / 1000000) + " msec.\n");
			System.out.println("Used RAM In The Beginning:" + glbUsedMemoryInTheBeginning + ": KB. RAM After Loading:" + usedMemoryAfterLoading + ": KB. RAM Diff:" + (usedMemoryAfterLoading - glbUsedMemoryInTheBeginning) + ": KB.");
			writeText("ontologies" + File.separator + "LogsRuningExtracted", glbStringOfNow + "_" + glbOntoFileName, "Used RAM In The Beginning:" + glbUsedMemoryInTheBeginning + ": KB. RAM After Loading:" + usedMemoryAfterLoading + ": KB. RAM Diff:" + (usedMemoryAfterLoading - glbUsedMemoryInTheBeginning) + ": KB.\n");
			
			
			RELReasonerFactory relfactory = new RELReasonerFactory();
			reasoner = relfactory.createReasoner(ont);
			reasoner.classify(true, false);
			
			
			// Isa: Here starts CLASSIFICATION of an Ontology
			timeAfterClassification = System.nanoTime();
			usedMemoryAfterClassification = (runtime.totalMemory() - runtime.freeMemory()) / 1024;
			System.out.println("Classifying Ontology took : " + ((timeAfterClassification - timeAfterLoading) / 1000000) + " msec.");
			writeText("ontologies" + File.separator + "LogsRuningExtracted", glbStringOfNow + "_" + glbOntoFileName, "Classifying Ontology took : " + ((timeAfterClassification - timeAfterLoading) / 1000000) + " msec.\n");
			System.out.println("Used RAM In The Beginning:" + glbUsedMemoryInTheBeginning + ": KB. RAM After Classifying:" + usedMemoryAfterClassification + ": KB. RAM Diff:" + (usedMemoryAfterClassification - glbUsedMemoryInTheBeginning) + ": KB.");
			writeText("ontologies" + File.separator + "LogsRuningExtracted", glbStringOfNow + "_" + glbOntoFileName, "Used RAM In The Beginning:" + glbUsedMemoryInTheBeginning + ": KB. RAM After Classifying:" + usedMemoryAfterClassification + ": KB. RAM Diff:" + (usedMemoryAfterClassification - glbUsedMemoryInTheBeginning) + ": KB.\n");
			
	
			// Isa: The End
			System.out.println("--- Ontology " + glbOntoFileName + " processed successfuly ---");
			writeText("ontologies" + File.separator + "LogsRuningExtracted", glbStringOfNow + "_" + glbOntoFileName, "--- Ontology " + glbOntoFileName + " processed successfuly ---\n");
		} catch (Exception E) {
			WriteException(E.toString());
		}
		
	}
	
	/**
	 * Sends results to intent activity (PowerBenchMark app that was called
	 * from) to send the information that it finished its task.
	 */
	private void finishWithResult(int a) {
		Bundle conData = new Bundle();
		conData.putInt("results", a);
		Intent intent = new Intent();
		intent.putExtras(conData);
		setResult(RESULT_OK, intent);
	}

	/**
	 * Closes the app. Is called when reasoner encounters an error and is
	 * manually called for closing. Records the power consumption it drained.
	 */
	public void quiteAnApp(int a) {
		
		float dNow = System.nanoTime();
		System.out.println("Classifying aborted after " + ((dNow - glbTimeStarting) / 1000000) + " msec.");

		writeText("ontologies/TrOWLAbortLog", "TrOwl." + StringOfNow() + "." + glbOntoFileName + "_Abort", 
				"________ABORTED____________\n" + 
				"Classifying aborted after " + ((dNow - glbTimeStarting) / 1000000) + " msec.");
		
		progressDialog.dismiss();
		finishWithResult(a);
		finish();
		System.exit(0);

	}

	/**
	 * Method created the pop up dialog asking if user wants really quite and
	 * application.
	 */
	@Override
	public void onBackPressed() {
		final Dialog dialog = new Dialog(this);
		dialog.setContentView(R.layout.customexit);
		dialog.setTitle("TrOWL");
		TextView text = (TextView) dialog.findViewById(R.id.text);
		text.setText("Are you sure you want");
		TextView text2 = (TextView) dialog.findViewById(R.id.text2);
		text2.setText("to CANCEL reasoning?");
		ImageView image = (ImageView) dialog.findViewById(R.id.image);
		image.setImageResource(R.drawable.cancel);
		Button dialogButton = (Button) dialog.findViewById(R.id.btnok);
		Button dialogButton2 = (Button) dialog.findViewById(R.id.btncancel);
		// if button is clicked, close the custom dialog
		dialogButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				quiteAnApp(-1);
				dialog.dismiss();
			}
		});
		dialogButton2.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				progressDialog.show();
				dialog.dismiss();
			}
		});

		dialog.setOnCancelListener(new OnCancelListener() {

			@Override
			public void onCancel(DialogInterface dialog) {
				progressDialog.show();
			}
		});
		dialog.show();
	}
	
	public void writeText(String pFolderName, String pFileName, String pText) {
		String dFilename = "";
		String temp = "";
		BufferedWriter writer = null;
		try {
			if (pFolderName.equalsIgnoreCase("")) {
				dFilename = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
						+ File.separator + pFileName + ".txt";
				temp = read(dFilename);
			} else {
				File directory = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
						+ File.separator + pFolderName);
				if (!directory.exists()) {
					directory.mkdir();
				}
				dFilename = directory.toString() + File.separator + pFileName + ".txt";
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


	public void WriteException(String ErrContent) {
		writeText("ontologies/TrOWLErrorLog", "TrOWL." + StringOfNow() + "." + glbOntoFileName, glbOntoFileName + "\n" + ErrContent);
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