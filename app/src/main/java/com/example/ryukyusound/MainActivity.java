package com.example.ryukyusound;

import android.content.Context;
import android.content.res.TypedArray;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaPlayer;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.TextView;

import org.w3c.dom.Text;

public class MainActivity extends AppCompatActivity implements SensorEventListener{

    private SensorManager mSensorManager;
    private Sensor mMagField;
    private Sensor mAccelerometer;
    private static final int AZIMUTH_THRESHOLD = 30;

    private static final int MATRIX_SIZE = 16;
    private float[] mgValues = new float[3];
    private float[] acValues = new float[3];

    private int nowScale = 0;
    private int oldScale = 9;
    private int nowAzimuth = 0;
    private int oldAzimuth = 0;

    private MediaPlayer[] mplayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        mSensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mMagField = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

    }

    @Override
    protected void onResume(){
        super.onResume();
        mSensorManager.registerListener(this, mAccelerometer, 100000);
        mSensorManager.registerListener(this, mMagField,100000);
        TypedArray notes = getResources().obtainTypedArray(R.array.notes);
        mplayer = new MediaPlayer[notes.length()];
        for(int i=0;i<notes.length();i++){
            mplayer[i] = MediaPlayer.create(this,notes.getResourceId(i,-1));
        }
    }

    @Override
    protected void onPause(){
        super.onPause();
        mSensorManager.unregisterListener(this, mAccelerometer);
        mSensorManager.unregisterListener(this, mMagField);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
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

    @Override
    public void onSensorChanged(SensorEvent event) {
        float[] inR = new float[MATRIX_SIZE];
        float[] outR = new float[MATRIX_SIZE];
        float[] I = new float[MATRIX_SIZE];
        float[] orValues = new float[3];
        TextView txt01 = (TextView)findViewById(R.id.txt01);
        switch(event.sensor.getType()){
            case Sensor.TYPE_ACCELEROMETER:
                acValues = event.values.clone();
                break;
            case Sensor.TYPE_MAGNETIC_FIELD:
                mgValues = event.values.clone();
                break;
        }

        if(mgValues == null || acValues == null){
            return;
        }

        SensorManager.getRotationMatrix(inR, I, acValues, mgValues);

        //　縦持ち、縦固定
        SensorManager.remapCoordinateSystem(inR, SensorManager.AXIS_X, SensorManager.AXIS_Z, outR);
        SensorManager.getOrientation(outR, orValues);

        StringBuilder sb = new StringBuilder();
        sb.append("方位角(アジマス):");
        sb.append(rad2Deg(orValues[0]));
        sb.append(System.getProperty("line.separator"));
        sb.append("傾斜角(ピッチ):");
        sb.append(rad2Deg(orValues[1]));
        sb.append(System.getProperty("line.separator"));
        nowScale = rad2Deg(orValues[1]) / 10;
        sb.append("index:" + nowScale);
        nowAzimuth = rad2Deg(orValues[0]);
        txt01.setText(sb.toString());

        if(nowScale != oldScale){
            playSound(nowScale);
            oldScale = nowScale;
            oldAzimuth = nowAzimuth;
        }else if (Math.abs(oldAzimuth - nowAzimuth) > AZIMUTH_THRESHOLD){
            playSound(nowScale);
            oldAzimuth = nowAzimuth;
        }

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    private int rad2Deg(float rad){
        return (int)Math.floor(Math.abs(Math.toDegrees(rad)));
    }

    private void playSound(int scale){
        mplayer[scale].seekTo(0);
        mplayer[scale].start();
    }
}
