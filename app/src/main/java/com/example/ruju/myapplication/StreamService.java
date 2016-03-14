package com.example.ruju.myapplication;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import android.os.Handler;
import android.app.IntentService;

public class StreamService extends IntentService {
    int uploadInterval=0;
    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    static int i = 0;
    int serverResponseCode = 0;
    String upLoadServerUri = "https://impact.asu.edu/Appenstance/UploadToServerGPS.php";
    String downloadUri = "https://impact.asu.edu/Appenstance/" + Constants.UPLOAD_FILE_NAME;
    private boolean mInitialized;
    private final float NOISE = (float) 2.0;
    private float mLastX, mLastY, mLastZ;
    /**********  File Path *************/
    final String uploadFilePath = Environment.getExternalStorageDirectory()+"/downloads/";
    final String uploadFileName = "contextFile.txt";
    int maxCounter = 20;
    float[] bufferX = new float[maxCounter];
    float[] bufferY = new float[maxCounter];
    float[] bufferZ = new float[maxCounter];
    int bufferCounter = 0;

    public StreamService() {
        super("StreamService");
    }

    private int download(String dest) {
        HttpsURLConnection conn = null;
        int bytesRead, bytesAvailable, bufferSize;
        byte[] buffer;
        int maxBufferSize = 1 * 1024 * 1024;
        File sourceFile = new File(dest);
        InputStream caInput = null;
        int ret = 0;
        try {
            trustEveryone();
            URL url = new URL(downloadUri);
            // Open a HTTP  connection to  the URL
            conn = (HttpsURLConnection) url.openConnection();
            FileOutputStream fo = new FileOutputStream(sourceFile);
            InputStream is = conn.getInputStream();
            int i = is.read();
            while (i != -1) {
                fo.write(i);
                i = is.read();
            }
            fo.flush();
            fo.close();
            is.close();
            ret = conn.getResponseCode();
            conn.disconnect();
        } catch (MalformedURLException ex) {
            ex.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ret;
    }

    @Override
    public void onHandleIntent(Intent i) {
        String action = i.getAction();
        Bundle extras=i.getExtras();
        switch (action) {
            case  Constants.MSG_UPLOAD:
                String url = i.getData().toString();
                int ret = uploadFile(url);
                if (extras!=null) {
                    Messenger messenger=(Messenger)extras.get(Constants.MSG_EXTRA);
                    Message msg = Message.obtain();
                    msg.what = Constants.MSG_UPLOAD_COMPLETE;
                    msg.arg1 = ret;
                    try {
                        messenger.send(msg);
                    }
                    catch (android.os.RemoteException e1) {
                        Log.w(getClass().getName(), "Exception sending message", e1);
                    }
                }
                break;
            case Constants.MSG_DOWNLOAD:
                String destFile = i.getData().toString();
                ret = download(destFile);
                if (extras!=null) {
                    Messenger messenger=(Messenger)extras.get(Constants.MSG_EXTRA);
                    Message msg = Message.obtain();
                    msg.what = Constants.MSG_DOWNLOAD_COMPLETE;
                    msg.arg1 = ret;
                    try {
                        messenger.send(msg);
                    }
                    catch (android.os.RemoteException e1) {
                        Log.w(getClass().getName(), "Exception sending message", e1);
                    }
                }
                break;
            default:
        }
        stopSelf();
    }
    private int uploadFile(String sourceFileUri) {
        String fileName = uploadFileName;
        HttpsURLConnection conn = null;
        DataOutputStream dos = null;
        String lineEnd = "\r\n";
        String twoHyphens = "--";
        String boundary = "*****";
        int bytesRead, bytesAvailable, bufferSize;
        byte[] buffer;
        int maxBufferSize = 1 * 1024 * 1024;
        File sourceFile = new File(sourceFileUri);
        Log.d("Demo", bufferCounter + "");
        InputStream caInput = null;

        HttpsURLConnection caConn = null;
        try {
            Log.d("Demo", "my file3");
            // open a URL connection to the Servlet
            trustEveryone();
            FileInputStream fileInputStream = new FileInputStream(sourceFileUri);
            URL url = new URL(upLoadServerUri);
            Log.d("Demo", upLoadServerUri);
            // Open a HTTP  connection to  the URL
            conn = (HttpsURLConnection) url.openConnection();
            conn.setDoInput(true); // Allow Inputs
            conn.setDoOutput(true); // Allow Outputs
            conn.setUseCaches(false); // Don't use a Cached Copy
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Connection", "Keep-Alive");
            conn.setRequestProperty("ENCTYPE", "multipart/form-data");
            conn.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);
            conn.setRequestProperty("uploaded_file", Constants.UPLOAD_FILE_NAME);
           // conn.setSSLSocketFactory(context.getSocketFactory());
            dos = new DataOutputStream(conn.getOutputStream());

            dos.writeBytes(twoHyphens + boundary + lineEnd);
            dos.writeBytes("Content-Disposition: form-data; name=uploaded_file;"+"filename="
                    + Constants.UPLOAD_FILE_NAME + " " + lineEnd);

            dos.writeBytes(lineEnd);

            // create a buffer of  maximum size
            bytesAvailable = fileInputStream.available();

            bufferSize = Math.min(bytesAvailable, maxBufferSize);
            buffer = new byte[bufferSize];

            // read file and write it into form...
            bytesRead = fileInputStream.read(buffer, 0, bufferSize);

            while (bytesRead > 0) {
                dos.write(buffer, 0, bufferSize);
                bytesAvailable = fileInputStream.available();
                bufferSize = Math.min(bytesAvailable, maxBufferSize);
                bytesRead = fileInputStream.read(buffer, 0, bufferSize);
            }

            // send multipart form data necesssary after file data...
            dos.writeBytes(lineEnd);
            dos.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);

            // Responses from the server (code and message)
            serverResponseCode = conn.getResponseCode();
            String serverResponseMessage = conn.getResponseMessage();
            //Log.i("uploadFile", "HTTP Response is : "
            //   + serverResponseMessage + ": " + serverResponseCode);
            Log.d("Demo status", serverResponseCode+"");
            if(serverResponseCode == 200){
                Log.d("Demo", "write file successful");
            }
            fileInputStream.close();
            dos.flush();
            dos.close();
            bufferCounter = 0;

        } catch (MalformedURLException ex) {
            ex.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return serverResponseCode;
    }

    private void trustEveryone() {
        try {
            HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier(){
                public boolean verify(String hostname, SSLSession session) {
                    return true;
                }});
            SSLContext context = SSLContext.getInstance("TLS");
            context.init(null, new X509TrustManager[]{new X509TrustManager(){
                public void checkClientTrusted(X509Certificate[] chain,
                                               String authType) throws CertificateException {}
                public void checkServerTrusted(X509Certificate[] chain,
                                               String authType) throws CertificateException {}
                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[0];
                }}}, new SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(
                    context.getSocketFactory());
        } catch (Exception e) { // should never happen
            e.printStackTrace();
        }
    }
}



