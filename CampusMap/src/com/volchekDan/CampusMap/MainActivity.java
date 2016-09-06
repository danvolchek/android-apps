package com.volchekDan.CampusMap;

import android.animation.AnimatorInflater;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.*;
import android.widget.*;

import com.google.android.gms.maps.*;
import com.google.android.gms.maps.model.*;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class MainActivity extends Activity implements OnMapReadyCallback, DownloadListener {
	private DrawerLayout mDrawerLayout;
	private ListView mDrawerList;
	private ActionBarDrawerToggle mDrawerToggle;

	private CharSequence mDrawerTitle;
	private CharSequence mTitle;
	private String[] mDays;

	private boolean mapReady;
	private GoogleMap map;

	private int[] cray;
	private int selectedDay;

	private boolean isShowAllClosed;
	private ObjectAnimator close;
	private ObjectAnimator open;

	private boolean showNext;

	private ArrayList<ArrayList<Polyline>> polylines;
	private ArrayList<ArrayList<Marker>> markers;
	private ArrayList<ArrayList<String>> places;

	private ArrayList<String> floorplans;
	private ProgressDialog downloadDialog;

	private boolean readingFiles;

	private ListView scheduleList;

	public final static String DAY_MESSAGE = "com.volchekDan.CampusMap.DAY";

	/**
	 * Called when the activity is first created.
	 */
	@Override
	public void onResume() {
		super.onResume();
		readMapFiles();
		selectItem(selectedDay);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		mapReady = false;
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		cray = new int[] {Color.RED, Color.rgb(255, 128, 0), Color.YELLOW, Color.GREEN, Color.BLUE, Color.DKGRAY, Color.BLACK};

		showNext = false;

		readingFiles = false;

		markers = new ArrayList<>();
		polylines = new ArrayList<>();
		places = new ArrayList<>();

		scheduleList = (ListView) findViewById(R.id.scheduleItems);

		floorplans = new ArrayList<>();

		MapFragment mapFragment = (MapFragment) getFragmentManager()
		                          .findFragmentById(R.id.map);
		mapFragment.getMapAsync(this);


		mTitle = mDrawerTitle = getTitle();
		mDays = getResources().getStringArray(R.array.weekDays);
		mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
		mDrawerList = (ListView) findViewById(R.id.left_drawer);

		selectedDay = Arrays.asList(mDays).indexOf(new SimpleDateFormat("EEEE", Locale.ENGLISH).format(Calendar.getInstance().getTime().getTime()));
		if (selectedDay == -1)
			selectedDay = 0;

		// Set the adapter for the list view
		mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);
		// set up the drawer's list view with items and click listener
		mDrawerList.setAdapter(new ArrayAdapter<>(this,
		                       R.layout.drawer_list_item, mDays));
		mDrawerList.setOnItemClickListener(new DrawerItemClickListener());

		getActionBar().setDisplayHomeAsUpEnabled(true);
		getActionBar().setHomeButtonEnabled(true);
		mDrawerToggle = new ActionBarDrawerToggle(
		    this,                  /* host Activity */
		    mDrawerLayout,         /* DrawerLayout object */
		    R.drawable.ic_drawer,  /* nav drawer image to replace 'Up' caret */
		    R.string.drawer_open,  /* "open drawer" description for accessibility */
		    R.string.drawer_close  /* "close drawer" description for accessibility */
		) {
			public void onDrawerClosed(View view) {
				getActionBar().setTitle(mTitle);
				invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
			}

			public void onDrawerOpened(View drawerView) {
				getActionBar().setTitle(mDrawerTitle);


				invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
			}

			public void onDrawerSlide(View drawerView, float slideOffset) {
				((SlidingLayer) findViewById(R.id.scheduleContainer)).closeLayer(true);
			}
		};
		mDrawerLayout.setDrawerListener(mDrawerToggle);


		if (savedInstanceState == null) {
			selectItem(selectedDay);
		}

		downloadDialog = new ProgressDialog(this);
		downloadDialog.setTitle("");

		readMapFiles();
		readFloorplans();


	}

	private void readFloorplans() {
		floorplans.clear();
		try {
			InputStream is = getResources().openRawResource(R.raw.floorplans);
			BufferedReader reader = new BufferedReader(new InputStreamReader(is));
			String line = reader.readLine();
			while (line != null) {
				floorplans.add(line);
				line = reader.readLine();
			}
			reader.close();
			is.close();

		} catch (Exception e) {
			Log.e("Exception", e.getMessage());
		}

	}

	@Override
	public void downloadFinished(String day, String which) {
		downloadDialog.dismiss();
		openPDF(new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/CampusMap/Floorplans/" + day + ".pdf"));

	}

	private void openPDF(File file) {
		Intent target = new Intent(Intent.ACTION_VIEW);
		target.setDataAndType(Uri.fromFile(file), "application/pdf");
		target.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);

		try {
			startActivity(target);
		} catch (ActivityNotFoundException e) {
		}
	}

	@Override
	public void onMapReady(GoogleMap mappy) {
		LatLng uni = new LatLng(40.107651, -88.225831);
		map = mappy;
		mapReady = true;
		mappy.setMyLocationEnabled(true);
		mappy.moveCamera(CameraUpdateFactory.newLatLngZoom(uni, 14));

		map.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {

			@Override
			public void onMapLongClick(LatLng latLng) {
				for (Marker marker : markers.get(selectedDay)) {
					if (Math.abs(marker.getPosition().latitude - latLng.latitude) < 0.0005 && Math.abs(marker.getPosition().longitude - latLng.longitude) < 0.0005) {

						String code = "none";
						for (String f : floorplans)
							if (f.contains(marker.getTitle()))
								code = f.substring(f.indexOf(";") + 1);

						if (!code.equals("none")) {
							File path = Environment.getExternalStorageDirectory();
							File dir = new File(path.getAbsolutePath() + "/CampusMap/Floorplans");
							File file = new File(dir, code + ".pdf");
							if (file.exists())
								openPDF(file);
							else {
								Download d = new Download(MainActivity.this, "http://police.illinois.edu/emergency/floorplans/pdf/" + code + ".pdf", code, "-1", downloadDialog);
								d.addListener(MainActivity.this);
								d.execute();
							}
						} else
							Toast.makeText(MainActivity.this, marker.getTitle() + " floorplan not found", Toast.LENGTH_LONG).show(); //do some stuff

						break;
					}
				}

			}
		});


		readMapFiles();

		selectItem(selectedDay);

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		// If the nav drawer is open, hide action items related to the content view
		boolean drawerOpen = mDrawerLayout.isDrawerOpen(mDrawerList);
		menu.findItem(R.id.settings).setVisible(!drawerOpen);
		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// The action bar home/up action should open or close the drawer.
		// ActionBarDrawerToggle will take care of this.
		if (mDrawerToggle.onOptionsItemSelected(item)) {
			return true;
		}
		// Handle action buttons
		switch (item.getItemId()) {
		case R.id.settings:

			//open settings
			Intent intent = new Intent(this, SettingsActivity.class);
			String message = mDays[selectedDay];
			intent.putExtra(DAY_MESSAGE, message);
			startActivity(intent);
			return true;

		default:
			return super.onOptionsItemSelected(item);
		}
	}

	/* The click listner for ListView in the navigation drawer */
	private class DrawerItemClickListener implements ListView.OnItemClickListener {
		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
			selectItem(position);
		}
	}


	private void readMapFiles() {
		readingFiles = true;
		if (mapReady) {
			// update the main content by replacing fragments

			//UPDATE GOOGLIES

			//READ ROUTES FILE

			//lat longs
			map.clear();
			polylines.clear();
			markers.clear();
			places.clear();
			for (String mDay : mDays) {
				ArrayList<Marker> markerHolder = new ArrayList<Marker>();
				ArrayList<Polyline> polylineHolder = new ArrayList<Polyline>();
				ArrayList<String> placesHolder = new ArrayList<>();

				ArrayList<String> a = new ArrayList<>();
				//marker NAMES
				ArrayList<String> j = new ArrayList<>();
				//marker SNIPPETS
				ArrayList<String> k = new ArrayList<>();
				File path = Environment.getExternalStorageDirectory();
				File dir = new File(path.getAbsolutePath() + "/CampusMap/Routes");
				dir.mkdirs();
				File file = new File(dir, mDay.toLowerCase() + ".txt");
				if (file.exists()) {
					FileInputStream is;
					BufferedReader reader;
					try {

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
					File dir1 = new File(path.getAbsolutePath() + "/CampusMap/Markers");
					dir1.mkdirs();
					File file1 = new File(dir1, mDay.toLowerCase() + ".txt");
					if (file1.exists()) {
						try {

							is = new FileInputStream(file1);
							reader = new BufferedReader(new InputStreamReader(is));
							String line = reader.readLine();
							while (line != null) {
								j.add(line);
								line = reader.readLine();
							}
							reader.close();
							is.close();


						} catch (Exception e) {
							Log.e("Exception", e.getMessage());
						}
					}
					File dir2 = new File(path.getAbsolutePath() + "/CampusMap/Places");
					dir2.mkdirs();
					File file2 = new File(dir2, mDay.toLowerCase() + ".txt");
					if (file2.exists()) {
						try {

							is = new FileInputStream(file2);
							reader = new BufferedReader(new InputStreamReader(is));
							String line = reader.readLine();
							while (line != null) {
								k.add(line);
								line = reader.readLine();
							}
							reader.close();
							is.close();


						} catch (Exception e) {
							Log.e("Exception", e.getMessage());
						}
					}

					ArrayList<LatLng> added = new ArrayList<>();
					if (!a.isEmpty()) {
						int x = 0;

						for (int z = 0; z < a.size(); z++) {
							String b = a.get(z);
							String[] c = b.split(";");
							//UPDATE TEH GOOOGLIES

							PolylineOptions po = new PolylineOptions();


							for (int q = 0; q < c.length; q++) {
								String d = c[q];
								LatLng poos = new LatLng(Double.valueOf(d.substring(0, d.indexOf(":"))), Double.valueOf(d.substring(d.indexOf(":") + 1)));
								po.add(poos);
								if (q == 0 && !added.contains(poos)) {
									String[] t = k.get(z).split(";");
									markerHolder.add(map.addMarker(new MarkerOptions()
									                               .title(j.get(z).substring(0, j.get(z).indexOf(":")))
									                               .snippet(t[1] + " | Room " + t[2] + " | " + t[3] + "-" + t[4])
									                               .position(poos)));
									markerHolder.get(markerHolder.size() - 1).setVisible(false);
									placesHolder.add(k.get(z));

									added.add(poos);
								}
								if (z == a.size() - 1 && q == c.length - 1 && !added.contains(poos)) { //last one
									String[] t = k.get(z + 1).split(";");
									markerHolder.add(map.addMarker(new MarkerOptions()
									                               .title(j.get(z).substring(j.get(z).indexOf(":") + 1))
									                               .snippet(t[1] + " | Room " + t[2] + " | " + t[3] + "-" + t[4])
									                               .position(poos)));

									markerHolder.get(markerHolder.size() - 1).setVisible(false);
									placesHolder.add(k.get(z + 1));

									added.add(poos);
								}

							}

							po.color(cray[x]);
							x++;
							polylineHolder.add(map.addPolyline(po));
							polylineHolder.get(polylineHolder.size() - 1).setVisible(false);

						}
					}
				}

				polylines.add(polylineHolder);
				markers.add(markerHolder);
				places.add(placesHolder);

			}
		}
		readingFiles = false;
	}

	private void selectItem(int position) {
		selectedDay = position;
		String cur = new SimpleDateFormat("kk:mm", Locale.US).format(new Date());

		//mDrawerList.setAdapter(new ArrayAdapter<>(this,
		//R.layout.drawer_list_item, mDays));
		if (places.size() != 0) {
			ArrayList<String> a = new ArrayList<>(places.get(position));
			for (int x = 0; x < a.size(); x++) {
				String[] c = a.get(x).split(";");
				String d = c[5] + "\n" + c[0] + "\n" + c[1] + " | Room " + c[2] + " | " + c[3] + "-" + c[4];
				a.set(x, d);
			}

			scheduleList.setAdapter(new ArrayAdapter<>(this, R.layout.schedule_list_item, a));


		}

		if (markers.size() > position)
			Log.d("soy lola", markers.get(position).toString());
		for (int i = 0; i < markers.size(); i++) {
			ArrayList<Marker> mm = markers.get(i);
			for (Marker m : mm) {
				m.setVisible(i == position);
			}
		}
		for (int i = 0; i < polylines.size(); i++) {
			ArrayList<Polyline> pp = polylines.get(i);
			for (Polyline p : pp)
				p.setVisible(i == position);
		}

		// update selected item and title, then close the drawer
		mDrawerList.setItemChecked(position, true);
		setTitle(mDays[position]);
		mDrawerLayout.closeDrawer(mDrawerList);
	}


	@Override
	public void setTitle(CharSequence title) {
		mTitle = title;
		getActionBar().setTitle(mTitle);
	}

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
		// Sync the toggle state after onRestoreInstanceState has occurred.
		mDrawerToggle.syncState();
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		// Pass any configuration change to the drawer toggls
		mDrawerToggle.onConfigurationChanged(newConfig);
	}

}
