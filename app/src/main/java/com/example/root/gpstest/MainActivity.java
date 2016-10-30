package com.example.root.gpstest;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.SocketException;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import javax.net.ssl.HttpsURLConnection;

public class MainActivity extends AppCompatActivity{

    private LocationManager locationManager;
    public  DatagramSocket serversocket;

    //---------------TextViews----
    TextView NLat;
    TextView NLon;
    TextView NAlt;
    TextView GLat;
    TextView GLon;
    TextView GAlt;
    TextView PLat;
    TextView PLon;
    TextView PAlt;
    TextView Angle;
    //-------------------------------
    // -----
    double GLatLast=(double)0;
    double GLonLast=(double)0;
    double GAltLast=(double)0;
    float rad = (float)0;
    int rad_count = 0;
    //----------------------------
    Button send;
    String response = "";

    private SensorManager sensorManager;
    Sensor accelerometer;
    Sensor magnetometer;
    float[] mGravity;
    float[] mGeomagnetic;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        init();
        createUDPServer("0.0.0.0", 9998);

        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendRotation(getIMEI(MainActivity.this), "94.60.223.201", 9999, rad);

            }
        });

        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        LocationListener locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {

                if(location.getProvider().equalsIgnoreCase("gps")){
                    //Log.e("QWE","GPS");
                    GLatLast = location.getLatitude();
                    GLonLast = location.getLongitude();
                    GAltLast = location.getAltitude();
                    sendLocation(getIMEI(MainActivity.this),"94.60.223.201",9999,GLatLast,GLonLast,GAltLast);
                    GLat.setText(Double.toString(location.getLatitude()));
                    GLon.setText(Double.toString(location.getLongitude()));
                    GAlt.setText(Double.toString(location.getAltitude()));
                }else if(location.getProvider().equalsIgnoreCase("network")){
                    //sendLocation(getIMEI(MainActivity.this),"192.168.88.238",9999,location.getLatitude(),location.getLongitude(),location.getAltitude(),rad);
                    //Log.e("QWE","NET");
                    NLat.setText(Double.toString(location.getLatitude()));
                    NLon.setText(Double.toString(location.getLongitude()));
                    NAlt.setText(Double.toString(location.getAltitude()));
                } else if(location.getProvider().equalsIgnoreCase("fused")){

                    PLat.setText(Double.toString(location.getLatitude()));
                    PLon.setText(Double.toString(location.getLongitude()));
                    PAlt.setText(Double.toString(location.getAltitude()));
                }

            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {

            }

            @Override
            public void onProviderEnabled(String provider) {

            }

            @Override
            public void onProviderDisabled(String provider) {

            }
        };
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            return;
        }
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);
        locationManager.requestLocationUpdates(LocationManager.PASSIVE_PROVIDER, 0, 0, locationListener);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0,0, locationListener);


        sensorManager=(SensorManager) getSystemService(SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        SensorEventListener sensorEventListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                    mGravity = event.values;
                }
                if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
                    mGeomagnetic = event.values;
                }
                if (mGravity != null && mGeomagnetic != null) {
                    float R[] = new float[9];
                    float I[] = new float[9];
                    boolean success = SensorManager.getRotationMatrix(R, I, mGravity, mGeomagnetic);
                    if (success) {
                        float orientation[] = new float[3];
                        float angle[] = new float[3];
                        SensorManager.getOrientation(R, orientation);
                        rad = orientation[0];
                        //rad_count++;
                        Angle.setText(orientation[0] + "");
                        if(orientation[0]<0) Angle.setText(Math.toDegrees(2*Math.PI+orientation[0]) + "");
                        else Angle.setText(Math.toDegrees(orientation[0]) + "");

                    }
                }
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {

            }
        };

        sensorManager.registerListener(sensorEventListener, accelerometer, SensorManager.SENSOR_DELAY_GAME);
        sensorManager.registerListener(sensorEventListener, magnetometer, SensorManager.SENSOR_DELAY_GAME);

    }

    public void init(){
        NLat = (TextView)findViewById(R.id.NLAT);
        NLon = (TextView)findViewById(R.id.NLON);
        NAlt = (TextView)findViewById(R.id.NALT);

        GLat = (TextView)findViewById(R.id.GLAT);
        GLon = (TextView)findViewById(R.id.GLON);
        GAlt = (TextView)findViewById(R.id.GALT);

        PLat = (TextView)findViewById(R.id.PLAT);
        PLon = (TextView)findViewById(R.id.PLON);
        PAlt = (TextView)findViewById(R.id.PALT);

        Angle = (TextView)findViewById(R.id.Angle);

        send = (Button)findViewById(R.id.Send);
    }

    @Override
    protected void onStart() {
        super.onStart();
        boolean hasInternetPermission = (ContextCompat.checkSelfPermission(this, Manifest.permission.INTERNET) == PackageManager.PERMISSION_GRANTED);
        boolean hasGPSPermission = (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED);
        boolean hasPHONEPermission = (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED);
        if (hasInternetPermission && hasGPSPermission && hasPHONEPermission ) {
            Log.e("PERMISSIONS","YES");
        }else {
            Log.e("PERMISSIONS","NO");
        }
    }

    public void sendHTTPGET(){
        Thread  thrd = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    URL url = new URL("http://3bd40eae.ngrok.io/send");
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setReadTimeout(15000);
                    conn.setConnectTimeout(15000);
                    conn.setRequestMethod("GET");
                    conn.setRequestProperty("Content-Type", "application/json");
                    conn.setDoInput(true);
                    conn.setDoOutput(true);
                    OutputStream os = conn.getOutputStream();
                    BufferedWriter writer = new BufferedWriter(
                            new OutputStreamWriter(os, "UTF-8"));
                    writer.write("{\"FODA\":\"Fuck youh\"}");

                    writer.flush();
                    writer.close();
                    os.close();
                    int responseCode=conn.getResponseCode();
                    Log.e("ANSW", Integer.toString(responseCode));
                    if (responseCode == HttpsURLConnection.HTTP_OK) {
                        String line;
                        BufferedReader br=new BufferedReader(new InputStreamReader(conn.getInputStream()));
                        while ((line=br.readLine()) != null) {
                            response+=line;
                        }
                    }
                    Log.e("ANSW", response);
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                } catch (ProtocolException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        thrd.start();

    }

    public void createUDPServer(final String sourceIP,final Integer sourcePORT){
        InetAddress serv_address = null;
        try {
            serv_address = InetAddress.getByName(sourceIP);
            serversocket = new DatagramSocket(sourcePORT,serv_address);
        } catch (UnknownHostException e) {
            Log.e("UnknownHostException",e.toString());
            e.printStackTrace();
        } catch (SocketException e) {
            Log.e("SocketException",e.toString());
            e.printStackTrace();
        }

    }

    public void sendLocation(final String imei,final String destIP,final Integer destPORT,final double dlat, final double dlon, final double dalt){

        Thread thrd = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    ByteBuffer txbuff = ByteBuffer.allocate(40).order(ByteOrder.LITTLE_ENDIAN);
                    byte[] Bimei = imei.getBytes();
                    final DatagramPacket packet = new DatagramPacket(txbuff.array(),1);
                    packet.setAddress(InetAddress.getByName(destIP));
                    packet.setPort(destPORT);
                    txbuff.put((byte)0x01);
                    txbuff.put(Bimei);
                    txbuff.putDouble(dlat);
                    txbuff.putDouble(dlon);
                    txbuff.putDouble(dalt);
                    packet.setData(txbuff.array());
                    packet.setLength(40);
                    serversocket.send(packet);
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                }
                catch (SocketException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        thrd.start();


    }

    public void sendRotation(final String imei,final String destIP,final Integer destPORT, final float rad){
        Thread thrd = new Thread(new Runnable() {
            @Override
            public void run() {
                try {

                    ByteBuffer txbuff = ByteBuffer.allocate(24).order(ByteOrder.LITTLE_ENDIAN);
                    final DatagramPacket packet = new DatagramPacket(txbuff.array(),1);
                    packet.setAddress(InetAddress.getByName(destIP));
                    packet.setPort(destPORT);
                    txbuff.put((byte) 0x02);
                    txbuff.put(imei.getBytes());
                    txbuff.putFloat(rad);
                    packet.setData(txbuff.array());
                    packet.setLength(24);
                    serversocket.send(packet);
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                }
                catch (SocketException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        thrd.start();
    }

    public String getIMEI(Context context){
        TelephonyManager mngr = (TelephonyManager) context.getSystemService(context.TELEPHONY_SERVICE);
        String imei = mngr.getDeviceId();
        return imei;

    }



}
