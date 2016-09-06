package com.volchekD.LaundryApp;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.*;

import java.io.*;
import java.util.ArrayList;

interface DownloadListener {
    void downloadFinished();
}

interface AlarmListener{
    void alarmFinished();
}

public class MainActivity extends Activity implements DownloadListener,AlarmListener {
    private int[] settings;
    //building,washer,dryer

    private Spinner[] spinners;
    //building, washer, dryer

    private MaxScroll[] scrollers;
    //washers, dryers

    private String[] buildings;

    private int unset;

    Alarm m;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        settings = new int[]{0, 0, 0, 0};
        spinners = new Spinner[3];
        scrollers = new MaxScroll[2];

        m = new Alarm();
        m.addListener(MainActivity.this);

        unset = 0;

        buildings = getResources().getStringArray(R.array.buildings);

        getPrefs();


        spinners[0] = (Spinner) findViewById(R.id.building);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.buildings, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinners[0].setAdapter(adapter);

        spinners[0].setSelection(settings[0]);

        spinners[1] = (Spinner) findViewById(R.id.washer);
        spinners[2] = (Spinner) findViewById(R.id.dryer);

        for (int x = 0; x < spinners.length; x++)
            spinners[x].setOnItemSelectedListener(new SpinnerListener(x));

        scrollers[0] = (MaxScroll) findViewById(R.id.washers);
        scrollers[1] = (MaxScroll) findViewById(R.id.dryers);

        findViewById(R.id.loc).setOnClickListener(new ClickListener());

        ((CheckBox) findViewById(R.id.wdCheck)).setOnCheckedChangeListener(new CheckListener());
        update(0);

        doAlarm();
    }

    private void getPrefs() {
        File path = Environment.getExternalStorageDirectory();
        File dir = new File(path.getAbsolutePath() + "/LaundryApp/");
        dir.mkdirs();
        File file = new File(dir, "prefs.txt");

        FileInputStream is;
        BufferedReader reader;
        try {
            if (!file.exists())
                file.createNewFile();
            is = new FileInputStream(file);
            reader = new BufferedReader(new InputStreamReader(is));

            String line;
            int x = 0;
            while ((line = reader.readLine()) != null) {
                settings[x] = Integer.parseInt(line);
                x++;
            }

            reader.close();
            is.close();
        } catch (Exception e) {
            Log.e("Exception", e.getMessage());
        }
    }


    private void setPrefs() {
        File path = Environment.getExternalStorageDirectory();
        File dir = new File(path.getAbsolutePath() + "/LaundryApp/");
        dir.mkdirs();
        File file = new File(dir, "prefs.txt");

        FileOutputStream os;
        BufferedWriter writer;
        try {
            if (!file.exists())
                file.createNewFile();
            os = new FileOutputStream(file, false);
            writer = new BufferedWriter(new OutputStreamWriter(os));
            for (int x : settings) {
                writer.write(x + "\n");
            }
            writer.close();
            os.close();
        } catch (Exception e) {
            Log.e("Exception", e.getMessage());
        }
    }

    private void doAlarm() {
        String val1 = (String) ((TextView) findViewById(R.id.wStatusV)).getText();
        String val2 = (String) ((TextView) findViewById(R.id.dStatusV)).getText();

        int a = 100;
        int b = 100;
        if (val1.contains("min"))
            val1 = val1.substring(val1.indexOf(":") + 2, val1.indexOf("min")).trim();
        if (val2.contains("min"))
            val2 = val2.substring(val2.indexOf(":") + 2, val2.indexOf("min")).trim();

        try {
            a = Integer.parseInt(val1);
        } catch (Exception e) {

        }
        try {
            b = Integer.parseInt(val2);
        } catch (Exception e) {

        }

        long time = (a < b ? a : b) - 5;
        time = (time < 0 ? 0 : time) * 60000;

        if (settings[3] == 1 && time != 5700000)
            m.SetAlarm(time, this);
        else
           m.CancelAlarm(this);

    }

    private void update(int which) {
        setPrefs();
        ((CheckBox)findViewById(R.id.wdCheck)).setChecked(false);
        if (which == 0) {

            Download d = new Download(MainActivity.this, "https://www.laundryalert.com/cgi-bin/urba7723/LMRoom", settings[0]);
            d.addListener(MainActivity.this);
            d.execute();
        } else if (which == 1) {
            try {
                File sdcard = Environment.getExternalStorageDirectory();
                File file = new File(sdcard, "/LaundryApp/data.txt");
                BufferedReader reader = new BufferedReader(new FileReader(file));
                String line;
                String s = "";
                boolean tt = false;
                ArrayList<String> list = new ArrayList<>();
                while ((line = reader.readLine()) != null) {
                    if (line.contains("410"))
                        tt = true;
                    if (tt) {
                        list.add(line);
                        s += line;
                    }
                }
                String[] ss = list.toArray(new String[list.size()]);


                int ii = s.indexOf(" washers available");
                int iii = s.indexOf(" washer available");
                int i = ii > iii ? ii : iii;
                String washersAvailable = s.substring(findCarrot(s, i) + 1, i);


                ii = s.indexOf(" dryers available");
                iii = s.indexOf(" dryer available");
                i = ii > iii ? ii : iii;
                String dryersAvailable = s.substring(findCarrot(s, i) + 1, i);

                int totalWashers = getCount(s, "Load Washer");

                int totalDryers = getCount(s, "Dryer");

                String[] a = new String[totalWashers];
                for (int x = 0; x < a.length; x++)
                    a[x] = (x + 1) + "";

                String[] b = new String[totalDryers];
                for (int x = 0; x < b.length; x++)
                    b[x] = (x + 1 + a.length) + "";

                ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, a);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinners[1].setAdapter(adapter);
                adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, b);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinners[2].setAdapter(adapter);

                if (unset < 2) {
                    spinners[1].setSelection(settings[1] - 1);
                    spinners[2].setSelection(settings[2] - totalWashers - 1);
                    ((CheckBox) findViewById(R.id.wdCheck)).setChecked(settings[3] == 1);
                    unset++;
                } else {
                    spinners[1].setSelection(0);
                    spinners[2].setSelection(0);
                }

                scrollers[0].removeAllViews();
                scrollers[1].removeAllViews();
                LinearLayout l = new LinearLayout(this);
                l.setOrientation(LinearLayout.VERTICAL);
                for (int x = 0; x < a.length; x++) {
                    TextView t = (TextView) getLayoutInflater().inflate(R.layout.text_view, null);
                    t.setPadding(0, 0, 0, 10);
                    String es = x < 9 && a.length > 9 ? "  " : "";
                    t.setText((x + 1) + ": " + es + getState(ss, x + 1));
                    l.addView(t);
                }
                scrollers[0].addView(l);
                LinearLayout p = new LinearLayout(this);
                p.setOrientation(LinearLayout.VERTICAL);
                for (int x = 0; x < b.length; x++) {
                    TextView t = (TextView) getLayoutInflater().inflate(R.layout.text_view, null);
                    t.setText((x + 1 + a.length) + ": " + getState(ss, x + 1 + a.length));
                    t.setPadding(0, 0, 0, 10);
                    p.addView(t);
                }
                scrollers[1].addView(p);

                ((TextView) findViewById(R.id.washersNum)).setText(washersAvailable + "/" + totalWashers);
                ((TextView) findViewById(R.id.dryersNum)).setText(dryersAvailable + "/" + totalDryers);

                update(2);
            } catch (Exception e) {
                Log.e("Exception/: " + which, e.getMessage());
            }
        } else if (which == 2) {
            try {

                File sdcard = Environment.getExternalStorageDirectory();
                File file = new File(sdcard, "/LaundryApp/data.txt");
                BufferedReader reader = new BufferedReader(new FileReader(file));
                String line;

                boolean tt = false;
                ArrayList<String> list = new ArrayList<>();
                while ((line = reader.readLine()) != null) {
                    if (line.contains("410"))
                        tt = true;
                    if (tt)
                        list.add(line);

                }
                String[] ss = list.toArray(new String[list.size()]);


                ((TextView) findViewById(R.id.wStatusV)).setText(getState(ss, settings[1]));
                ((TextView) findViewById(R.id.dStatusV)).setText(getState(ss, settings[2]));


            } catch (Exception e) {
                Log.e("Exception: " + which, e.getMessage());
            }
        }


    }

    private String getState(String[] s, int which) {

        int i = find(s, which + "", 0);
        int j = find(s, "Available", i);

        int k = find(s, "In Use", i);

        String q = j < k ? "Available" : "In Use";

        if (k < j) {
            int d = findy(s, " min", k);
            if (d != s.length) {
                String work = s[d];
                q += ": " + work.substring(findCarrot(work, work.indexOf("min")) + 1, work.indexOf("min") + 3) + " left";
            } else
                q += ": error";
        }

        return q;
    }

    public void downloadFinished() {
        update(1);
    }

    public void alarmFinished(){
        ((CheckBox)findViewById(R.id.wdCheck)).setChecked(false);
    }

    private int getCount(String s, String b) {
        int i = s.indexOf(b);
        if (i == -1)
            return 0;
        else if (s.charAt(i + b.length()) == 'O')
            return getCount(s.substring(i + b.length() - 1), b);
        else return 1 + getCount(s.substring(i + b.length() - 1), b);
    }

    private int findCarrot(String s, int start) {
        int x = start;
        while (s.charAt(x) != '>' || x == -1)
            x--;
        return x;
    }

    private int find(String[] a, String b, int from) {
        if (from >= a.length)
            return a.length;
        for (int x = from; x < a.length; x++)
            if (a[x].trim().length() > b.length() - 1 && a[x].trim().substring(0, b.length()).equals(b))
                return x;
        return a.length;
    }

    private int findy(String[] a, String b, int from) {
        if (from >= a.length)
            return a.length;
        for (int x = from; x < a.length; x++)
            if (a[x].trim().contains(b))
                return x;
        return a.length;
    }

    private void dumb(){
        m.CancelAlarm(this);
    }

    private class CheckListener implements CompoundButton.OnCheckedChangeListener {

        @Override
        public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
            settings[3] = b ? 1 : 0;
            setPrefs();
            dumb();
            doAlarm();
        }
    }

    private class ClickListener implements View.OnClickListener {

        @Override
        public void onClick(View view) {
            update(0);
        }
    }

    private class SpinnerListener implements AdapterView.OnItemSelectedListener {
        private int which;

        public SpinnerListener(int which) {
            this.which = which;
        }

        public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
            switch (which) {
                case 0:
                    settings[0] = find(buildings, (String) parent.getItemAtPosition(pos), 0);
                    update(0);//everything
                    break;
                case 1:
                    settings[1] = Integer.parseInt((String) parent.getItemAtPosition(pos));
                    update(2);//washer only
                    break;
                case 2:
                    settings[2] = Integer.parseInt((String) parent.getItemAtPosition(pos));
                    update(2);//drery only
                    break;

            }


        }

        public void onNothingSelected(AdapterView<?> parent) {

        }
    }
}
