package com.volchekD.SVN_Explorer;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.*;
import android.widget.*;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;
import java.util.Stack;

public class MainActivity extends Activity implements DownloadListener {
	/**
	 * Called when the activity is first created.
	 */

	private ListView v;
	private URLStack<String> currURL;
	private LinearLayout pathScroll;
	private HorizontalScrollView hScroll;
	private ViewFlipper flipper;
	private TextView errorText;
	private TextView info;
	private ImageView image;
	private ViewSwitcher imageSwitcher;

	private String[] repos;
	private Stack<String> actions;
	private int count;
	private SimpleAdapter simpleAdaper;

	private int dStart;
	private int dEnd;
	private String baseRepo;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		ActionBar actionBar = getActionBar();

		// add the custom view to the action bar
		actionBar.setCustomView(getLayoutInflater().inflate(R.layout.url, null));
		actionBar.setDisplayShowTitleEnabled(false);
		actionBar.setDisplayShowCustomEnabled(true);


		repos = getResources().getStringArray(R.array.repositories);
		v = (ListView) findViewById(R.id.listView);
		pathScroll = (LinearLayout) actionBar.getCustomView().findViewById(R.id.pathScrollLayout);
		flipper = (ViewFlipper) findViewById(R.id.viewFlipper);
		errorText = (TextView) findViewById(R.id.errorText);
		hScroll = (HorizontalScrollView) actionBar.getCustomView().findViewById(R.id.pathScroll);
		info = (TextView) findViewById(R.id.display);
		image = (ImageView) findViewById(R.id.imageView);
		imageSwitcher = (ViewSwitcher) findViewById(R.id.viewSwitcher);

		count = 0;
		currURL = new URLStack<>();
		actions = new Stack<>();
		dStart = 0;
		dEnd = 0;
		baseRepo = "";
		v.setOnItemClickListener(new android.widget.AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				String item = (String) ((HashMap) v.getItemAtPosition(position)).get("left");
				goTo(item);
			}
		});

		errorText.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				goTo("", false, true);
			}
		});
		setBaseRepo(repos[0]);

	}

	private void setBaseRepo(String which) {
		actions.clear();
		currURL.clear();
		pathScroll.removeAllViews();
		pathScroll.addView(new PathView(this, ""));
		count = 0;
		dStart = 1;
		dEnd = 0;
		baseRepo = which;
		File f = new File(Environment.getExternalStorageDirectory() + "/SVN Explorer/" + baseRepo);
		if (!f.exists()) {
			errorText.setText("Downloading Repo: " + baseRepo + "...");
			updateList(null, false, false, true);
			new Download(this, false).execute("https://subversion.ews.illinois.edu/svn/" + baseRepo + "username/");
		} else {
			pathScroll.removeAllViews();
			goTo(baseRepo, false, false);
			goTo("username/");
		}
	}

	private void goTo(String place) {
		goTo(place, true, true);
	}

	private void goTo(String place, boolean save, boolean update) {
		if ((currURL.toString() + place).equals(""))
			setBaseRepo(baseRepo);
		else {
			if (save)
				actions.push(place.equals("..") ? currURL.peek() : "..");

			if (place.equals("..")) {
				count--;
				currURL.pop();
				if (count > 0)
					((PathView) pathScroll.getChildAt(count - 1)).hideImage();
				pathScroll.removeViewAt(count);

			} else if (!place.equals("")) {
				currURL.push(place);
				if (count > 0)
					((PathView) pathScroll.getChildAt(count - 1)).showImage();
				PathView p = new PathView(this, currURL.peek());
				p.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View view) {
						int click = pathScroll.indexOfChild(view);
						if (count != click + 1) {
							int tot = pathScroll.getChildCount();
							for (int i = 0; i < tot - click - 2; i++)
								goTo("..", true, false);
							goTo("..");
						}
					}
				});
				pathScroll.addView(p);
				ViewTreeObserver vto = hScroll.getViewTreeObserver();
				vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
					@Override
					public void onGlobalLayout() {
						hScroll.getViewTreeObserver().removeGlobalOnLayoutListener(this);
						hScroll.smoothScrollTo(pathScroll.getWidth(), 0);
					}
				});
				count++;
			}
			if (currURL.toString().endsWith("/")) {
				String filePath = Environment.getExternalStorageDirectory() + "/SVN Explorer/" + currURL.toString();
				File f = new File(filePath);
				File file[] = f.listFiles();
				ArrayList<HashMap<String, Object>> n = new ArrayList<>();
				if (!contains(currURL.toString())) {
					HashMap<String, Object> map = new HashMap<String, Object>();
					map.put("left", "..");
					map.put("right", "");
					n.add(map);
				}
				for (File ff : file) {
					HashMap<String, Object> map = new HashMap<String, Object>();
					map.put("left", ff.getName() + (ff.isDirectory() ? "/" : ""));

					if (ff.isFile() && isGradeFile(ff.getName())) {
						map.put("right", getScoreFromFile(ff));
					} else if (ff.isDirectory())
						map.put("right", searchInNextDir(ff));

					n.add(map);
				}
				flipTo(0);
				updateList(n, update, false, false);
			} else {
				errorText.setText("Downloading " + currURL.lastElement() + "...");
				updateList(null, false, false, true);
				new Download(this).execute("https://subversion.ews.illinois.edu/svn/" + currURL.toString()); //dont preload files
			}
		}
	}

	public boolean isGradeFile(String ff) {
		return (ff.startsWith("grade_report") || ff.startsWith("web_homework")) && ff.endsWith(".txt");
	}

	private String searchInNextDir(File nextDir) {
		//either
		//grade_report_part_1, grade_report_part_2
		//or
		//grade_report_final, grade_report, grade_report_extra_credit
		File[] files = nextDir.listFiles();
		HashMap<String, String> reports = new HashMap<>();
		for (File ff : files)
			if (ff.isFile() && isGradeFile(ff.getName()))
				reports.put(ff.getName(), getScoreFromFile(ff));
		String s = "";
		if (reports.containsKey("grade_report_part_1.txt")) {
			s = reports.get("grade_report_part_1.txt");
			if (reports.containsKey("grade_report_part_2.txt"))
				s += " " + reports.get("grade_report_part_2.txt");
			else
				s += " ?%";
		} else {
			if (reports.containsKey("grade_report_rerun.txt"))
				s = reports.get("grade_report_rerun.txt");
			else if (reports.containsKey("grade_report_final.txt"))
				s = reports.get("grade_report_final.txt");
			else if (reports.containsKey("grade_report.txt"))
				s = reports.get("grade_report.txt");

			if (reports.containsKey("grade_report_extra_credit.txt"))
				s += " " + reports.get("grade_report_extra_credit.txt");
		}
		return s;
	}

	private String getScoreFromFile(File f) {
		try {
			BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(f)));
			String line;
			while ((line = br.readLine()) != null) {
				if (line.startsWith("Final Score: ")) {
					line = line.substring(line.indexOf("Final Score: ") + 13);
					double a = Double.valueOf(line.substring(0, line.indexOf("/")).trim());
					double b = Double.valueOf(line.substring(line.indexOf("/") + 1).trim());
					return (Math.round(((a * 100.0) / b) * 100.0) / 100.0) + "%";
				} else if (line.startsWith("TOTAL SCORE: "))
					return line.substring(line.indexOf("TOTAL SCORE: ") + 13) + "%";
				else if (line.endsWith("Total")) {
					line = line.substring(0, line.indexOf("Total")).trim();
					double a = Double.valueOf(line.substring(0, line.indexOf("/")).trim());
					double b = Double.valueOf(line.substring(line.indexOf("/") + 1).trim());
					return (Math.round(((a * 100.0) / b) * 100.0) / 100.0) + "%";
				}
			}
			br.close();
		} catch (Exception e) {

		}
		return "";
	}

	private boolean contains(String b) {
		for (String c : repos)
			if (c.equals(b))
				return true;
		return false;
	}

	private void flipTo(int x) {
		if (flipper.getDisplayedChild() != x)
			flipper.setDisplayedChild(x);
	}

	private void updateList(ArrayList<HashMap<String, Object>> result, boolean update, boolean file, boolean error) {
		if (error) {
			flipTo(1);
		} else if (!file) {
			flipTo(0);
		} else {
			flipTo(2);
		}
		String[] from = new String[] {"left", "right"};
		int[] to = new int[] {R.id.leftText, R.id.rightText};

		if (update && !file) {
			simpleAdaper = new SimpleAdapter(this, result, R.layout.listitem, from, to);
			v.setAdapter(simpleAdaper);
		}
	}

	@Override
	public void onBackPressed() {
		if (!actions.empty())
			goTo(actions.pop(), false, true);
	}

	@Override
	public void downloadFinished(String result, String path, String error, Bitmap b, boolean user) {
		dEnd++;
		if (error.equals("")) {
			if (!user) {
				//preloading everything
			} else if (path.endsWith("png") || path.endsWith("jpg")) { //user clicked image file
				imageSwitcher.setDisplayedChild(1);
				image.setImageBitmap(b);
				flipTo(2);
			} else { // user click text file
				imageSwitcher.setDisplayedChild(0); //wow apk changes
				info.setText(result);
				flipTo(2);
			}
		} else {
			errorText.setText(getResources().getText(R.string.wifierr));
			updateList(null, false, false, true);

			if (path.endsWith("/"))
				deleteRecursive(new File(Environment.getExternalStorageDirectory() + "/SVN Explorer/" + baseRepo));
		}

		if (!user && error.equals("") && dStart == dEnd) {
			pathScroll.removeAllViews();
			goTo(baseRepo, false, false);
			goTo("username/");
		}
	}

	@Override
	public void newStarted() {
		dStart++;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.action_settings:
			// User chose the "Settings" item, show the app settings UI...
			return true;
		case R.id.action_refresh:
			File f = new File(Environment.getExternalStorageDirectory() + "/SVN Explorer/" + baseRepo);
			deleteRecursive(f);
			setBaseRepo(baseRepo);
			return true;
		case R.id.action_favorite:
			AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(MainActivity.this, android.R.style.Theme_Material_Dialog_Alert));

			builder.setTitle("Choose Respository")
			.setItems(R.array.names, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					setBaseRepo(repos[which]);
				}
			});
			builder.create().show();
			return true;

		default:
			// If we got here, the user's action was not recognized.
			// Invoke the superclass to handle it.
			return super.onOptionsItemSelected(item);

		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu, menu);
		return true;
	}

	private void deleteRecursive(File fileOrDirectory) {
		if (fileOrDirectory.isDirectory())
			for (File child : fileOrDirectory.listFiles())
				deleteRecursive(child);

		fileOrDirectory.delete();
	}
}
