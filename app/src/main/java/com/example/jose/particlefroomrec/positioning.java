package com.example.jose.particlefroomrec;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Environment;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.ObjectInputStream;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import weka.classifiers.Classifier;
import weka.classifiers.lazy.KStar;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;

import static android.R.attr.data;

public class positioning extends AppCompatActivity implements SensorEventListener {
    particleFilter objParticleFilter;
    private static final int numberAN=5;
    Sensor magnetometer;
    Sensor accelerometer;
    Sensor mgravity;
    Sensor linearAcc;
    private float[] gravityValues = null;
    private float[] magneticValues = null;
    private float[] linearAccValues=null;
    private float[] accValues=null;
    private SensorManager mSensorManager;

    TextView txtRSS,txtRoomP,txtRoomPF,txtScanNumber;
    Button btnRoomPF,btnRegister;
    EditText editPosition;
    ImageView imageView;

    //************WiFi Anchor Nodes**************//
   public wifiANs[] wifiAN;
   public CustomDrawableView mCustomDrawableView;
   public int scanNumber=0;
    wifiReceiver broadcasReceiver;
    WifiManager myWifiManager;
    double[] rssVector;
    private double[] pedState;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mSensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
        magnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        accelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        linearAcc = mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        mgravity=mSensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);

        mCustomDrawableView = new CustomDrawableView(this,"plan_thirdroom",getWindowManager().getDefaultDisplay().getWidth());
        setContentView(R.layout.activity_positioning);
        imageView=(ImageView) findViewById(R.id.imageView);
        txtRSS =(TextView)findViewById(R.id.txtRSS);
        txtRoomP =(TextView)findViewById(R.id.txtRoomP);
        txtRoomPF =(TextView)findViewById(R.id.txtRoomPF);
        txtScanNumber =(TextView)findViewById(R.id.txtScanNumber);
        btnRoomPF=(Button) findViewById(R.id.btnRoomPF);
        btnRegister=(Button) findViewById(R.id.btnRegister);
        editPosition= (EditText) findViewById(R.id.editPosition);
        //**Initialize WiFi anchor nodes***//
        wifiAN=new wifiANs[numberAN];  //Number of anchor node must be defined per environment
        wifiAN[0]=new wifiANs();
        wifiAN[0].MAC="c0:a0:bb:18:83:8a";
        wifiAN[0].positionX=0.6f;
        wifiAN[0].positionY=1.22f;
        wifiAN[0].alpha=1.264;
        wifiAN[0].betha=-0.03614;

        wifiAN[1]=new wifiANs();
        wifiAN[1].MAC="c0:a0:bb:18:83:a2";
        wifiAN[1].positionX=3.7f;
        wifiAN[1].positionY=15.88f;
        wifiAN[1].alpha=1.278;
        wifiAN[1].betha=-0.03711;

        wifiAN[2]=new wifiANs();
        wifiAN[2].MAC="c0:a0:bb:18:83:b0";
        wifiAN[2].positionX=14.4f;
        wifiAN[2].positionY=13.5f;
        wifiAN[2].alpha=0.3701;
        wifiAN[2].betha=-0.05153;

        wifiAN[3]=new wifiANs();
        wifiAN[3].MAC="c0:a0:bb:18:83:90";
        wifiAN[3].positionX=13.6f;
        wifiAN[3].positionY=1.6f;
        wifiAN[3].alpha=0.5663;
        wifiAN[3].betha=-0.04431;

        wifiAN[4]=new wifiANs();
        wifiAN[4].MAC="c0:a0:bb:18:88:66";
        wifiAN[4].positionX=10.8f;
        wifiAN[4].positionY=14.2f;
        wifiAN[4].alpha=0.3496;
        wifiAN[4].betha=-0.05328;

        //RSS vector reading
        rssVector=new double[wifiAN.length];

        //WiFi scan process
        myWifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        broadcasReceiver = new wifiReceiver();

        pedState=new double[11]; //2 coordinates, 9 rooms



        objParticleFilter=new particleFilter(this,wifiAN);
        imageView.setImageBitmap(mCustomDrawableView.draw(wifiAN,pedState,objParticleFilter));

    }






    //************** WiFi Receiver Class ***********//
    public class wifiReceiver extends BroadcastReceiver
    {

        String Rss="",histogram="";
        public void onReceive(Context c, Intent intent)
        {
            scanNumber++;
            Rss="";
            histogram="";
            //scanNumber++;  //number of scan
            txtScanNumber.setText(Integer.toString(scanNumber));
            ScanResult resWifi;
            List<ScanResult> apsList =myWifiManager.getScanResults();
            int nWiFi=apsList.size();

            //COLLECT DATA FROM RSS
            boolean found;
            boolean ceros=false;
            for(int j=0;j<wifiAN.length;j++){
                found=false;
                for(int i=0;i<nWiFi;i++){
                    if(wifiAN[j].MAC.equals(apsList.get(i).BSSID)){
                        rssVector[j]= apsList.get(i).level;
                        found=true;
                        i=nWiFi;
                       // Rss=Rss+"AN"+Integer.toString(j+1)+": "+wifiAN[j].MAC+": "+Double.toString(rssVector[j])+"\n";
                        Rss=Rss+"AN"+Integer.toString(j+1)+": "+Double.toString(rssVector[j])+"\n";
                    }
                }
                if(!found){
                    rssVector[j]=0;
                    ceros=true;
                }


            }
          if(!ceros) {

              pedState=objParticleFilter.stateUpdate(rssVector,earthMag);
              histogram="X: "+String.format("%.2f", pedState[0])+" Y: "+String.format("%.2f", pedState[1])+"\n";
              int big=2;
              for(int i=2;i<pedState.length;i++){
                  if(pedState[big]<pedState[i]){
                     big=i;
                  }
                  if(pedState[i]!=0)
                    histogram+="Room "+Integer.toString(i-1)+": "+String.format("%.2f",(pedState[i]*100))+"%\n";
              }
              imageView.setImageBitmap(mCustomDrawableView.draw(wifiAN,pedState,objParticleFilter));
              //room number data begins in pedState[2]=data room 1, pedState[3]==data room 2 ...
              txtRoomPF.setText("PF: "+(big-1)+"\n"+histogram);

          }

            /**Just to see in screen***/
            //add magnetic field to see in screen
            //Round values
            earthMag[0]=Math.round(earthMag[0]*1000);
            earthMag[0]=earthMag[0]/1000;
            earthMag[1]=Math.round(earthMag[1]*1000);
            earthMag[1]=earthMag[1]/1000;
            earthMag[2]=Math.round(earthMag[2]*1000);
            earthMag[2]=earthMag[2]/1000;
            txtRSS.setText("Nada: "+Rss);
        }

    }
    protected void onResume()
    {
        try{
            super.onResume();
            mSensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_UI);
            mSensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
            mSensorManager.registerListener(this, mgravity, SensorManager.SENSOR_DELAY_UI);
            mSensorManager.registerListener(this, linearAcc, SensorManager.SENSOR_DELAY_UI);
            registerReceiver(broadcasReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));

        }catch (Exception ex){
            Log.d("Error onResume", "Error on Resume: "+ex.getMessage());
        }

    }
    protected void onPause()
    {
        super.onPause();
        mSensorManager.unregisterListener(this);
        unregisterReceiver(broadcasReceiver);
    }

    //***************** LOW PASS FILTER ***********************//
    static final float ALPHA=0.25f;
    public float[] lowPass(float[] input, float[] output){
        if(output==null) return input;
        for(int i=0;i<input.length;i++){
            output[i]=output[i]+ALPHA*(input[i]-output[i]);
        }
        return output;
    }
    //*****************************************************//
    //************* Take magnetic sensor values **********//
    //****************************************************//
    float[] deviceRelativeMagnetic = new float[4];
    float[] deviceRelativeAcceleration = new float[4];
    float[] deviceRelativeLinearAcc = new float[4];
    float[] earthAcc = new float[16],  earthMag = new float[16], earthLinearAcc = new float[16];
    public void onSensorChanged(SensorEvent event) {
        myWifiManager.startScan();
        if ((gravityValues != null) && (magneticValues != null)&& (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)) {
            accValues=lowPass( event.values.clone(),accValues);
            deviceRelativeAcceleration[0] = accValues[0];
            deviceRelativeAcceleration[1] = accValues[1];
            deviceRelativeAcceleration[2] = accValues[2];
            deviceRelativeAcceleration[3] = 0;
            // Change the device relative acceleration values to earth relative values
            // X axis -> East
            // Y axis -> North Pole
            // Z axis -> Sky
            float[] R = new float[16], I = new float[16];
            SensorManager.getRotationMatrix(R, I, gravityValues, magneticValues);
            float[] inv = new float[16];
            android.opengl.Matrix.invertM(inv, 0, R, 0);
            //Calculate acceleration in earth coordinate system
            android.opengl.Matrix.multiplyMV(earthAcc, 0, inv, 0, deviceRelativeAcceleration, 0);
            //Calculate magnetic field in earth coordinate system
            android.opengl.Matrix.multiplyMV(earthMag, 0, inv, 0, deviceRelativeMagnetic, 0);
            //Calculate linear acceleration in earth coordinate system
            android.opengl.Matrix.multiplyMV(earthLinearAcc, 0, inv, 0, deviceRelativeLinearAcc, 0);
            //   Log.d("Linear ACC:", "Values:(" + earthLinearAcc[0] + ", " + earthLinearAcc[1] + ", " + earthLinearAcc[2] + ")");
            //Log.d("Magnetic", "Values: (" + earthMag[0] + ", " + earthMag[1] + ", " + earthMag[2] + ")");
            //Log.d("Acceleration", "Values: (" + earthAcc[0] + ", " + earthAcc[1] + ", " + earthAcc[2] + ")");

        } else if (event.sensor.getType() == Sensor.TYPE_GRAVITY) {
            gravityValues = lowPass(event.values.clone(),gravityValues);

        } else if (event.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION) {
            linearAccValues=lowPass(event.values.clone(),linearAccValues);
            deviceRelativeLinearAcc[0] = linearAccValues[0];
            deviceRelativeLinearAcc[1] = linearAccValues[1];
            deviceRelativeLinearAcc[2] = linearAccValues[2];
            deviceRelativeLinearAcc[3] = 0;

        } else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            magneticValues = lowPass(event.values.clone(),magneticValues);
            deviceRelativeMagnetic[0] = magneticValues[0];
            deviceRelativeMagnetic[1] = magneticValues[1];
            deviceRelativeMagnetic[2] = magneticValues[2];
            deviceRelativeMagnetic[3] = 0;

        }

    }

    public void onAccuracyChanged(Sensor sensor, int accuracy)
    {

    }
}
