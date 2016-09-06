package com.volchekDan.CampusMap;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

public class Download extends AsyncTask<Void, Void, String> {
	private ProgressDialog mProgressDialog;

	private Context context;
	private String urly;
	private String day;
	private String which;

	private List<DownloadListener> listeners = new ArrayList<DownloadListener>();

	public void addListener(DownloadListener toAdd) {
		listeners.add(toAdd);
	}

	public Download(Context context, String url, String day, String which, ProgressDialog d) {
		this.context = context;
		urly = url;
		this.day = day;
		this.which = which;

		mProgressDialog = d;

	}


	protected String doInBackground(Void... voids) {
		try {
			URL url = new URL(urly);
			HttpURLConnection c = (HttpURLConnection) url.openConnection();

			if (!which.equals("-1")) {
				c.setRequestMethod("GET");
				c.setDoOutput(true);
			}

			try {
				c.connect();
			} catch (UnknownHostException e) {
				return "failed";
			}

			String pathy = which.equals("-1") ? "/CampusMap/Floorplans/" : ("/CampusMap/Downloads/" + day.toLowerCase());

			String PATH = Environment.getExternalStorageDirectory() + pathy;
			File file = new File(PATH);
			file.mkdirs();


			String fileName = which.equals("-1") ? (day + ".pdf") : (which + ".txt");

			File outputFile = new File(file, fileName);

			FileOutputStream fos = new FileOutputStream(outputFile);

			InputStream is = c.getInputStream();


			byte[] buffer = new byte[1024];
			int len1;
			while ((len1 = is.read(buffer)) != -1) {
				fos.write(buffer, 0, len1);
			}
			fos.close();
			is.close();

		} catch (IOException e) {
			e.printStackTrace();
		}
		return "done";
	}


	protected void onPreExecute() {
		String s = which.equals("-1") ? (day + ".pdf") : (day + "_" + which + ".txt");
		mProgressDialog.setMessage("Downloading: " + s);
		mProgressDialog.show();
	}


	protected void onPostExecute(String result) {
		String s = which.equals("-1") ? (day + ".pdf") : (day + "_" + which + ".txt");
		if (result.equals("done")) {
			mProgressDialog.setMessage("Downloaded: " + s);
			for (DownloadListener hl : listeners)
				hl.downloadFinished(day, which);
		} else if (result.equals("failed")) {
			Toast.makeText(context, "Download " + s + " failed", Toast.LENGTH_SHORT).show();
			mProgressDialog.dismiss();
		}
	}

}