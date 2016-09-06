package com.volchekD.SVN_Explorer;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;

import javax.net.ssl.HttpsURLConnection;
import java.io.*;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.util.ArrayList;

public class Download extends AsyncTask<String, String, String> {

	private DownloadListener listener;
	private String error;
	private Bitmap b;
	private String path;
	private boolean user;


	public Download(DownloadListener d) {
		listener = d;
		error = "";
		user = true;
	}

	public Download(DownloadListener d, boolean bool) {
		listener = d;
		error = "";
		user = bool;
	}

	/**
	 * Before starting background thread Show Progress Bar Dialog
	 */
	@Override
	protected void onPreExecute() {
		super.onPreExecute();
	}

	/**
	 * Downloading file in background thread
	 */
	@Override
	protected String doInBackground(String... f_url) {
		String s = "";
		path = f_url[0];

		try {
			URL url = new URL(f_url[0]);

			HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();

			Authenticator.setDefault(new Authenticator() {
				@Override
				protected PasswordAuthentication getPasswordAuthentication() {
					return new PasswordAuthentication("username", "password".toCharArray());
				}
			});

			connection.connect();

			// download the file
			if (f_url[0].endsWith("png") || f_url[0].endsWith("jpg")) {
				b = BitmapFactory.decodeStream(connection.getInputStream());
				s = "image";
			} else {
				BufferedReader r = new BufferedReader(new InputStreamReader(connection.getInputStream()));

				StringBuilder result = new StringBuilder();
				String line;
				while ((line = r.readLine()) != null) {
					result.append(line).append('\n');
				}
				s = result.toString();
			}

		} catch (Exception e) {
			error = e.getMessage();
			Log.e("Error ", e.getMessage());
		}

		if (error.equals(""))
			writeToFile(s);

		return s;
	}

	private void writeToFile(String s) {
		if (path.endsWith("/")) {
			String PATH = Environment.getExternalStorageDirectory() + "/SVN Explorer/" + path.replace("https://subversion.ews.illinois.edu/svn/", "");
			File file = new File(PATH);
			file.mkdirs();
			ArrayList<String> results = parseResult(s);
			for (String result : results) {
				File newF = new File(file, result);
				if (result.endsWith("/")) {
					newF.mkdirs();
					listener.newStarted();
					new Download(listener, user).execute(path + result);
				} else {
					try {
						newF.createNewFile();
						if (listener.isGradeFile(result)) {
							listener.newStarted();
							new Download(listener, user).execute(path + result);
						}
					} catch (IOException e) {
					}
				}
			}

		}
		String PATH = Environment.getExternalStorageDirectory() + "/SVN Explorer/" + path.replace("https://subversion.ews.illinois.edu/svn/", "");
		String f = path.substring(path.lastIndexOf('/') + 1);
		if (listener.isGradeFile(f)) {
			try {
				BufferedWriter wr = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(PATH)));
				wr.write(s);
				wr.flush();
				wr.close();
			} catch (Exception e) {
			}
		}
	}

	/**
	 * Updating progress bar
	 */
	protected void onProgressUpdate(String... progress) {

	}

	/**
	 * After completing background task Dismiss the progress dialog
	 **/
	@Override
	protected void onPostExecute(String result) {
		listener.downloadFinished(result, path, error, b, user);
	}

	ArrayList<String> parseResult(String result) {
		ArrayList<String> results = new ArrayList<>();
		String[] lines = result.split("\n");
		int index = 0;
		while (index != lines.length - 1) {
			String part = lines[index].trim();
			if (part.startsWith("<li>")) {
				results.add(part.substring(part.indexOf(">", 4) + 1, part.indexOf("<", 5)));
			}
			index++;
		}
		return results;
	}

}