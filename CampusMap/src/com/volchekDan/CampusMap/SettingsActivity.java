package com.volchekDan.CampusMap;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.text.InputType;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.*;
import com.google.android.gms.maps.model.LatLng;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

interface DownloadListener {
	void downloadFinished(String day, String which);
}

public class SettingsActivity extends Activity implements DownloadListener {

	private ListView day;

	private String selectedDay;

	private List<String[]> toDownload;
	private int numDownloaded;
	private int numToBeDownloaded;

	private Handler handler;

	private ProgressDialog downloadDialog;


	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.settings);

		day = (ListView) findViewById(R.id.listView);

		Intent intent = getIntent();

		selectedDay = intent.getStringExtra(MainActivity.DAY_MESSAGE);
		toDownload = new ArrayList<>();
		numDownloaded = 0;
		numToBeDownloaded = 0;

		downloadDialog = new ProgressDialog(this);
		downloadDialog.setTitle("");

		handler = new Handler();

		Spinner addSpinner = (Spinner) findViewById(R.id.spinner);
		ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
		                                     R.array.weekDays, android.R.layout.simple_spinner_item);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

		addSpinner.setAdapter(adapter);


		addSpinner.setSelection(Arrays.asList(getResources().getStringArray(R.array.weekDays)).indexOf(selectedDay));

		addSpinner.setOnItemSelectedListener(new SpinnerListener());

		(findViewById(R.id.addButton)).setOnClickListener(new ButtonListener());
		(findViewById(R.id.removeButton)).setOnClickListener(new ButtonListener());
		(findViewById(R.id.clearButton)).setOnClickListener(new ButtonListener());
		(findViewById(R.id.downloadButton)).setOnClickListener(new ButtonListener());

		getActionBar().setDisplayHomeAsUpEnabled(true);

	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			super.onBackPressed();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void downloadFinished(String day, String which) {
		//append to file, route will be cleared before any of these are called
		try {
			String s = getStringFromFile(Environment.getExternalStorageDirectory().getAbsolutePath() + "/CampusMap/Downloads/" + day + "/" + which + ".txt");
			JSONObject resp = new JSONObject(s);
			JSONArray routeObject = resp.getJSONArray("routes");
			JSONObject routes = routeObject.getJSONObject(0);
			JSONObject overviewPolylines = routes
			                               .getJSONObject("overview_polyline");
			String encodedString = overviewPolylines.getString("points");

			JSONObject legsOb = routes.getJSONArray("legs").getJSONObject(0);
			String start = legsOb.getString("start_address");
			String end = legsOb.getString("end_address");
			start = start.substring(0, start.indexOf(","));
			end = end.substring(0, end.indexOf(","));

			File path = Environment.getExternalStorageDirectory();
			File dir2 = new File(path.getAbsolutePath() + "/CampusMap/Places");
			dir2.mkdirs();
			File file2 = new File(dir2, day.toLowerCase() + ".txt");
			ArrayList<String> names = new ArrayList<>();
			if (file2.exists()) {
				FileInputStream is;
				BufferedReader reader;
				try {

					is = new FileInputStream(file2);
					reader = new BufferedReader(new InputStreamReader(is));
					String line = reader.readLine();
					while (line != null) {
						names.add(line.split(";")[5]);
						line = reader.readLine();
					}
					reader.close();
					is.close();


				} catch (Exception e) {
					Log.e("Exception", e.getMessage());
				} finally {
					start = names.get(Integer.parseInt(which));
					end = names.get(Integer.parseInt(which) + 1);
				}
			}


			ArrayList<LatLng> points = decodePoly(encodedString);

			Log.v("ROUTE INFO", "Writing from /Downloads/" + day + "/" + which + ".txt TO " + "/Routes/" + day + ".txt");

			//write to ROUTES DAY file
			File dir = new File(path.getAbsolutePath() + "/CampusMap/Routes");
			dir.mkdirs();
			File file = new File(dir, day.toLowerCase() + ".txt");

			FileOutputStream os;
			BufferedWriter writer;
			try {
				if (!file.exists())
					file.createNewFile();
				os = new FileOutputStream(file, true);
				writer = new BufferedWriter(new OutputStreamWriter(os));
				for (LatLng pt : points)
					writer.write(pt.latitude + ":" + pt.longitude + ";");
				writer.newLine();
				writer.close();
				os.close();
			} catch (Exception e) {
				Log.e("Exception", e.getMessage());
			}

			File dir1 = new File(path.getAbsolutePath() + "/CampusMap/Markers");
			dir1.mkdirs();
			File file1 = new File(dir1, day.toLowerCase() + ".txt");


			try {
				if (!file1.exists())
					file1.createNewFile();
				os = new FileOutputStream(file1, true);
				writer = new BufferedWriter(new OutputStreamWriter(os));
				writer.write(start + ":" + end);
				writer.newLine();
				writer.close();
				os.close();
			} catch (Exception e) {
				Log.e("Exception", e.getMessage());
			}

		} catch (Exception e) {

		}
		numDownloaded++;
		Log.v("DOWNLOAD INFO", "Download #" + numDownloaded + "/" + numToBeDownloaded + " finished!");
		//APPEND RELVENT INFO FROM Downloads/day/which.txt to routes/day.txt
	}

	private ArrayList<LatLng> decodePoly(String encoded) {

		ArrayList<LatLng> poly = new ArrayList<LatLng>();
		int index = 0, len = encoded.length();
		int lat = 0, lng = 0;

		while (index < len) {
			int b, shift = 0, result = 0;
			do {
				b = encoded.charAt(index++) - 63;
				result |= (b & 0x1f) << shift;
				shift += 5;
			} while (b >= 0x20);
			int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
			lat += dlat;

			shift = 0;
			result = 0;
			do {
				b = encoded.charAt(index++) - 63;
				result |= (b & 0x1f) << shift;
				shift += 5;
			} while (b >= 0x20);
			int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
			lng += dlng;

			LatLng p = new LatLng((((double) lat / 1E5)), (((double) lng / 1E5)));
			poly.add(p);
		}

		return poly;
	}

	private static String convertStreamToString(InputStream is) throws Exception {
		BufferedReader reader = new BufferedReader(new InputStreamReader(is));
		StringBuilder sb = new StringBuilder();
		String line = null;
		while ((line = reader.readLine()) != null) {
			sb.append(line).append("\n");
		}
		reader.close();
		return sb.toString();
	}

	private static String getStringFromFile(String filePath) throws Exception {
		File fl = new File(filePath);
		FileInputStream fin = new FileInputStream(fl);
		String ret = convertStreamToString(fin);
		//Make sure you close all streams.
		fin.close();
		return ret;
	}

	private class ButtonListener implements View.OnClickListener {
		private ArrayList<String> info;

		public void onClick(View view) {
			if (view.equals(findViewById(R.id.addButton))) {
				info = new ArrayList<>();

				AlertDialog.Builder builder = new AlertDialog.Builder(SettingsActivity.this);
				builder.setTitle("Location");

				LinearLayout l = new LinearLayout(getApplicationContext());
				l.setOrientation(LinearLayout.VERTICAL);

				final EditText bnam = new EditText(getApplicationContext());
				bnam.setInputType(InputType.TYPE_CLASS_TEXT);
				bnam.setHint("Building Name");
				l.addView(bnam);

				final EditText lat = new EditText(getApplicationContext());
				lat.setInputType(InputType.TYPE_CLASS_TEXT);
				lat.setHint("Address");
				l.addView(lat);

				final EditText name = new EditText(getApplicationContext());
				name.setInputType(InputType.TYPE_CLASS_TEXT);
				name.setHint("Class Name");
				l.addView(name);

				final EditText num = new EditText(getApplicationContext());
				num.setInputType(InputType.TYPE_CLASS_TEXT);
				num.setHint("Room Number");
				l.addView(num);

				final EditText stime = new EditText(getApplicationContext());
				stime.setInputType(InputType.TYPE_CLASS_TEXT);
				stime.setHint("Start Time (00:00)");
				l.addView(stime);

				final EditText etime = new EditText(getApplicationContext());
				etime.setInputType(InputType.TYPE_CLASS_TEXT);
				etime.setHint("End Time (00:00)");
				l.addView(etime);


				builder.setView(l);

				builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						info.add(lat.getText().toString().trim());
						info.add(name.getText().toString().trim());
						info.add(num.getText().toString().trim());
						info.add(stime.getText().toString().trim());
						info.add(etime.getText().toString().trim());
						info.add(bnam.getText().toString().trim());

						for (String s : info)
							if (s.equals("")) {
								Toast.makeText(getApplicationContext(), "All fields must be entered", Toast.LENGTH_SHORT).show();
								return;
							}


						//write to PLACES DAY file
						File path = Environment.getExternalStorageDirectory();
						File dir = new File(path.getAbsolutePath() + "/CampusMap/Places");
						dir.mkdirs();
						File file = new File(dir, selectedDay.toLowerCase() + ".txt");

						FileOutputStream os;
						BufferedWriter writer;
						try {
							if (!file.exists())
								file.createNewFile();
							os = new FileOutputStream(file, true);
							writer = new BufferedWriter(new OutputStreamWriter(os));
							writer.write(info.get(0) + ";" + info.get(1) + ";" + info.get(2) + ";" + info.get(3) + ";" + info.get(4) + ";" + info.get(5));
							writer.newLine();
							writer.close();
							os.close();
						} catch (Exception e) {
							Log.e("Exception", e.getMessage());
						}
						update();


					}
				});
				builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.cancel();
					}
				});

				builder.show();


			} else if (view.equals(findViewById(R.id.downloadButton))) {

				ConnectivityManager connectivityManager
				    = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
				NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
				if (activeNetworkInfo != null && activeNetworkInfo.isConnected())
					downloadRoutes();
				else
					Toast.makeText(getApplicationContext(), "WIFI/Mobile Data disabled so routes could not be downloaded", Toast.LENGTH_SHORT).show();
			} else if (view.equals(findViewById(R.id.clearButton))) {
				clearSchedule();
				update();
			} else if (view.equals(findViewById(R.id.removeButton))) {
				Toast.makeText(getApplicationContext(), "This doesn't work yet", Toast.LENGTH_SHORT).show();

			}
		}
	}

	private static boolean isInteger(String s) {
		return isInteger(s, 10);
	}

	private static boolean isInteger(String s, int radix) {
		if (s.isEmpty()) return false;
		for (int i = 0; i < s.length(); i++) {
			if (i == 0 && s.charAt(i) == '-') {
				if (s.length() == 1) return false;
				else continue;
			}
			if (Character.digit(s.charAt(i), radix) < 0) return false;
		}
		return true;
	}

	private void clearSchedule() {
		File path = Environment.getExternalStorageDirectory();
		File dir = new File(path.getAbsolutePath() + "/CampusMap/Places");
		dir.mkdirs();
		File file = new File(dir, selectedDay.toLowerCase() + ".txt");

		FileWriter fw;
		try {
			fw = new FileWriter(file, false);
			fw.write("");
			fw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		dir = new File(path.getAbsolutePath() + "/CampusMap/Routes");
		file = new File(dir, selectedDay.toLowerCase() + ".txt");
		try {
			fw = new FileWriter(file, false);
			fw.write("");
			fw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void update() {
		day.setAdapter(null);

		((Button) findViewById(R.id.addButton)).setText("Add location to " + selectedDay);
		((Button) findViewById(R.id.removeButton)).setText("Remove location from " + selectedDay);
		((Button) findViewById(R.id.clearButton)).setText("Clear all locations from " + selectedDay);
		((TextView) findViewById(R.id.scheduleText)).setText(selectedDay + " Schedule");

		ArrayList<String> a = new ArrayList<>();
		File path = Environment.getExternalStorageDirectory();
		File dir = new File(path.getAbsolutePath() + "/CampusMap/Places");
		dir.mkdirs();
		File file = new File(dir, selectedDay.toLowerCase() + ".txt");

		FileInputStream is;
		BufferedReader reader;
		try {
			if (!file.exists())
				file.createNewFile();
			is = new FileInputStream(file);
			reader = new BufferedReader(new InputStreamReader(is));
			String line = reader.readLine();
			while (line != null) {
				a.add(line);
				line = reader.readLine();
			}
			reader.close();
			is.close();


		} catch (Exception e) {
			Log.e("Exception", e.getMessage());
		}
		if (a.isEmpty())
			a.add(selectedDay + " schedule is empty");


		ArrayAdapter<String> adapter = new ArrayAdapter<String>(getApplicationContext(), android.R.layout.simple_list_item_1, a);
		day.setAdapter(adapter);
	}

	private void downloadRoutes() {
		String[] arr = getResources().getStringArray(R.array.weekDays);
		toDownload = new ArrayList<>();
		File path = Environment.getExternalStorageDirectory();

		File downs = new File(path.getAbsolutePath() + "/CampusMap/Downloads");
		if (downs.exists()) {
			String[] children = downs.list();
			for (int i = 0; i < children.length; i++) {
				new File(downs, children[i]).delete();
			}
		}

		for (String day : arr) {
			ArrayList<String> a = new ArrayList<>();


			File dir = new File(path.getAbsolutePath() + "/CampusMap/Places");
			dir.mkdirs();
			File file = new File(dir, day.toLowerCase() + ".txt");

			File dir2 = new File(path.getAbsolutePath() + "/CampusMap/Routes");
			dir2.mkdirs();
			File file2 = new File(dir2, day.toLowerCase() + ".txt");

			File dir3 = new File(path.getAbsolutePath() + "/CampusMap/Markers");
			dir3.mkdirs();
			File file3 = new File(dir3, day.toLowerCase() + ".txt");

			FileWriter fw;
			try {
				fw = new FileWriter(file2, false);
				fw.write("");
				fw.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			try {
				fw = new FileWriter(file3, false);
				fw.write("");
				fw.close();
			} catch (IOException e) {
				e.printStackTrace();
			}


			FileInputStream is;
			BufferedReader reader;
			try {
				if (!file.exists())
					file.createNewFile();
				is = new FileInputStream(file);
				reader = new BufferedReader(new InputStreamReader(is));
				String line = reader.readLine();
				while (line != null) {
					a.add(line);
					line = reader.readLine();
				}
				reader.close();
				is.close();

			} catch (Exception e) {
				Log.e("Exception", e.getMessage());
			}
			int x = 0;
			if (a.size() > 1) {
				for (int q = 0; q < a.size() - 1; q++) {
					ArrayList<String> info = new ArrayList<>();
					//a is reading places

					info.add(a.get(q).split(";")[0].replace(" ", "_"));
					info.add(a.get(q + 1).split(";")[0].replace(" ", "_"));

					toDownload.add(new String[] {"https://maps.googleapis.com/maps/api/directions/json?origin=" + info.get(0) + "&destination=" + info.get(1) + "&mode=walking", day.toLowerCase(), x + ""});


					x++;
				}
			}
		}


		handler.postDelayed(runnable, 1100);


	}

	private Runnable runnable = new Runnable() {
		@Override
		public void run() {
			try {
				startDownloading();
			} catch (Exception e) {
			}
			if (toDownload.size() != 0)
				handler.postDelayed(this, 1100);
			else
				downloadDialog.dismiss();
		}
	};


	private void startDownloading() {
		//Log.v("DOWNLOAD INFO", "Array size: " + toDownload.size() + "!");
		int batchNum = 2;
		numToBeDownloaded = toDownload.size() >= batchNum ? batchNum : toDownload.size();
		//Log.v("DOWNLOAD INFO", "Starting download of " + numToBeDownloaded + " files!");

		numDownloaded = 0;
		List<String[]> t = new ArrayList<>(toDownload.subList(0, numToBeDownloaded));
		toDownload.subList(0, numToBeDownloaded).clear();
		for (String[] info : t)
			try {
				runOnUiThread(new Runnable() {
					public void run() {
						Download d = new Download(SettingsActivity.this, info[0], info[1], info[2], downloadDialog);
						d.addListener(SettingsActivity.this);
						d.execute();
					}
				});

			} catch (Exception e) {
				Log.e("Error in downloading", e.getMessage());
			}
	}

	private class SpinnerListener implements AdapterView.OnItemSelectedListener {

		public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
			selectedDay = (String) parent.getItemAtPosition(pos);
			update();

		}

		public void onNothingSelected(AdapterView<?> parent) {

		}
	}
}
