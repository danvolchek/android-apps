package com.volchekD.LaundryApp;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Environment;
import android.widget.Toast;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

public class Download extends AsyncTask<Void, Void, String> {

	private Context context;
	private String urly;
	private String num;


	private List<DownloadListener> listeners = new ArrayList<DownloadListener>();

	public void addListener(DownloadListener toAdd) {
		listeners.add(toAdd);
	}


	public Download(Context context, String url, int num) {
		this.context = context;
		urly = url;
		this.num = "" + num;



	}


	protected String doInBackground(Void... voids) {
		try {
			List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(1);
			nameValuePairs.add(new BasicNameValuePair("Halls", num));
			HttpClient httpClient = new DefaultHttpClient();
			String paramsString = URLEncodedUtils.format(nameValuePairs, "UTF-8");
			HttpGet httpGet = new HttpGet(urly + "?" + paramsString);
			HttpResponse response = httpClient.execute(httpGet);


			InputStream is = response.getEntity().getContent();


			String pathy = "/LaundryApp/";

			String PATH = Environment.getExternalStorageDirectory() + pathy;
			File file = new File(PATH);
			file.mkdirs();


			String fileName = "data.txt";

			File outputFile = new File(file, fileName);

			FileOutputStream fos = new FileOutputStream(outputFile);

			byte[] buffer = new byte[1024];
			int len1;
			while ((len1 = is.read(buffer)) != -1) {
				fos.write(buffer, 0, len1);
			}
			fos.close();
			is.close();

		} catch (Exception e) {
			e.printStackTrace();
		}
		return "done";
	}


	protected void onPreExecute() {

	}


	protected void onPostExecute(String result) {
		if (result.equals("done")) {

			for (DownloadListener hl : listeners)
				hl.downloadFinished();
		} else if (result.equals("failed")) {
			Toast.makeText(context, "Download failed", Toast.LENGTH_SHORT).show();

		}
	}

}