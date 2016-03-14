package com.example.ruju.myapplication;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Messenger;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import java.io.File;

public class MainActivity extends AppCompatActivity implements SensorEventListener {
    GraphView graph;
    LinearLayout l;
    private SensorManager senSensorManager;
    private Sensor senAccelerometer;
    private long lastUpdate = 0;
    private float last_x, last_y, last_z;
    private SQLiteDatabase mydatabase;
    private String dbname = "HealthDB1";
    private String tablename = null;
    private String sex = new String();
    private EditText name;
    private EditText age;
    private EditText id;
    private RadioButton femaleBtn = null;
    private volatile boolean running = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mydatabase = openOrCreateDatabase(dbname,MODE_PRIVATE,null);
        setContentView(R.layout.activity_main);
        senSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        senAccelerometer = senSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        senSensorManager.registerListener(this, senAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        l = (LinearLayout)findViewById(R.id.lin);
        name = (EditText)findViewById(R.id.nametext);
        age = (EditText)findViewById(R.id.agetext);
        id = (EditText)findViewById(R.id.idtext);
        femaleBtn = (RadioButton)findViewById(R.id.radioButton2);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });
        running = false;
    }
    protected void onPause() {
        super.onPause();
        senSensorManager.unregisterListener(this);
    }

    protected void onResume() {
        super.onResume();
        senSensorManager.registerListener(this, senAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
    }
    //BASICALLY THIS IS GOING ON FROM WHEN THE APP STARTS AND COLLECTS THE ACCELEROMETER DATA.
    // IT IS ALSO WHERE THE TABLE IS CREATED. SO HERE WHATS HAPPENING IS THAT THE USER HAS NOT
    //ENTERED ANY PATIENT DATA YET, SO IM USING AN ARBITRARY TABLE TO STORE DATA. BUT WE HAVE TO
    //STORE DATA IN THE TABLENAME WITH FORMAT THAT HE HAS GIVEN
    //SO WE NEED TO FIGURE OUT A WAY TO ACTIVATE THIS SENSOR ON CHANGED FUNCTION
    //ONLY AFTER THE USER HITS RUN FOR THE FIRST TIME. THIS IS THE HELP I REQUIRE FROM YOU TWO.
    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        Sensor mySensor = sensorEvent.sensor;
        if (mySensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            if (running) {
                float x = sensorEvent.values[0];
                float y = sensorEvent.values[1];
                float z = sensorEvent.values[2];
                long curTime = System.currentTimeMillis();
                if ((curTime - lastUpdate) > 1000) {
                    long diffTime = (curTime - lastUpdate);
                    lastUpdate = curTime;
                    last_x = x;
                    last_y = y;
                    last_z = z;
                    //insertionquery = new String("INSERT INTO "+name.getText().toString()+"_"+age.getText().toString()+"_"+id.getText().toString()+"_"+sex+"(Time TIMESTAMP,X FLOAT,Y FLOAT,Z FLOAT);");
                    String query1 = new String("CREATE TABLE IF NOT EXISTS " + tablename + "(Time TIMESTAMP,X FLOAT,Y FLOAT,Z FLOAT);");
                    mydatabase.execSQL(query1);
                    ContentValues contentValues = new ContentValues();
                    contentValues.put("Time", 1);
                    contentValues.put("X", last_x);
                    contentValues.put("Y", last_y);
                    contentValues.put("Z", last_z);
                    long res = mydatabase.insert(tablename, null, contentValues);
                }

            }
        }
    }
    public Handler handler = new Handler(){
        @Override
        public void handleMessage(android.os.Message msg){
            switch(msg.what){
                case Constants.MSG_UPDATE_GRAPH:
                    setGraph();
                    break;
                case Constants.MSG_UPLOAD_COMPLETE:
                    int duration = Toast.LENGTH_SHORT;
                    Toast toast = Toast.makeText(MainActivity.this, "Upload Database Completed", duration);
                    toast.show();
                    break;
                case Constants.MSG_DOWNLOAD_COMPLETE:
                    duration = Toast.LENGTH_SHORT;
                    String text = "";
                    if (msg.arg1 == 404) {
                        text = "Opps, database doesn't exist in web server.";
                    } else if (msg.arg1 == 200){
                        text = "Download database sucessfully.";
                        mydatabase = openOrCreateDatabase(dbname,MODE_PRIVATE,null);
                        name.setText("ruju");
                        id.setText("001");
                        age.setText("21");
                        femaleBtn.setChecked(true);
                        tablename = Constants.UPLOAD_FILE_NAME;
                        setGraph();
                    } else {
                        text = "web serer error:" + msg.arg1;
                    }
                    toast = Toast.makeText(MainActivity.this, text, duration);
                    toast.show();
                    break;
            }
        }
    };

    public Runnable startDraw = new Runnable(){
        @Override
        public void run(){
            while(running){
                handler.sendEmptyMessage(Constants.MSG_UPDATE_GRAPH);
                try{
                    Thread.sleep(1000);
                } catch (Exception e){
                    e.printStackTrace();
                }
            }
        }
    };
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
    //startgraph and stopgraph functions written by us.
    //THIS IS WHERE THE LAST TEN POINTS ARE RETRIEVED AND PLOTTED ON THE GRAPH.
    public void startgraph(View v) {
        Cursor cursor = null;
        RadioGroup rg = (RadioGroup) findViewById(R.id.radioGroup);
        if (rg.getCheckedRadioButtonId() == R.id.radioButton) {
            sex = "Male";
        } else {
            sex = "Female";
        }
        if (name.getText().length() == 0 || id.getText().length() == 0 || age.getText().length() == 0) {
            AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).create();
            alertDialog.setTitle("Missing Required Input");
            alertDialog.setMessage("Please enter all required information before runing the graph.");
            alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });
            alertDialog.show();
            return;
        }
        tablename = String.format("%s_%s_%s_%s",name.getText(),id.getText(), age.getText(), sex);
        Log.d("demo", "table is " + tablename);
        String query1 = new String("CREATE TABLE IF NOT EXISTS " + tablename + "(Time TIMESTAMP,X FLOAT,Y FLOAT,Z FLOAT);");
        mydatabase.execSQL(query1);
        running = true;
        Thread t = new Thread(startDraw);
        t.start();
    }

    public void stopgraph(View v){
        running = false;
        float[] values = new float[] { 0.0f,0.0f, 0.0f, 0.0f , 0.0f};
        float[] val1 = new float[] { 0.0f,0.0f, 0.0f, 0.0f , 0.0f};
        float[] val2= new float[] { 0.0f,0.0f, 0.0f, 0.0f , 0.0f};
        l.removeAllViews();
        if (graph != null) {
            graph.setValues(values, val1, val2);
            l.addView(graph);
        }
    }

    public void setGraph(){
        try {
            float[] x = new float[10];
            float[] y = new float[10];
            float[] z = new float[10];
            getDataFromDB(x, y, z);
            l.removeAllViews();
            String[] verlabels = new String[]{"max", "min"};
            String[] horlabels = new String[]{"1", "2", "3", "4", "5", "6", "7", "8", "9", "10"};
            graph = new GraphView(this, x, y, z, "ECG", horlabels, verlabels, GraphView.LINE);
            l.addView(graph);
        } catch (Exception e) {
            Log.e("Demo", e.getMessage(), e);
        }

    }

    private void getDataFromDB(float[] xvalues, float[] yvalues, float[] zvalues) {
        Cursor cursor = null;
        float[] timevalues = new float[10];
        String query = new String("Select * from "+ tablename +";");
        Cursor resultSet = mydatabase.rawQuery(query,null);
        int count = resultSet.getCount();
        int start=0;
        if(count>10)
            start=count-9;
        resultSet.moveToPosition(start);

        for(int i=0;i<Math.min(count, 10);i++)
        {
            timevalues[i] = Float.parseFloat(resultSet.getString(0));
            xvalues[i]=Float.parseFloat(resultSet.getString(1)) ;
            yvalues[i]=Float.parseFloat(resultSet.getString(2)) ;
            zvalues[i]=Float.parseFloat(resultSet.getString(3));
            resultSet.moveToPosition(start+i);
        }
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    private String getDatabasePath() {
        File dbFile = getDatabasePath(dbname);
        return dbFile.getAbsolutePath();
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void doUpload(View v) {
        if (tablename != null) {
            Intent i = new Intent(this, StreamService.class);
            i.setData(Uri.parse(getDatabasePath()));
            i.setAction(Constants.MSG_UPLOAD);
            i.putExtra(Constants.MSG_EXTRA, new Messenger(handler));
            startService(i);
        }
    }

    public void doDownload(View v) {
            stopgraph(v);
            if (mydatabase != null)
                mydatabase.close();
            Intent i = new Intent(this, StreamService.class);
            i.setData(Uri.parse(getDatabasePath()));
            i.setAction(Constants.MSG_DOWNLOAD);
            i.putExtra(Constants.MSG_EXTRA, new Messenger(handler));
            startService(i);
    }
}
