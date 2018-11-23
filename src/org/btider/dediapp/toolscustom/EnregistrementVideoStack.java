package org.btider.dediapp.toolscustom;

import android.app.Activity;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.Environment;
import android.os.SystemClock;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.TextView;

import org.btider.dediapp.components.camera.CameraSurfaceView;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class EnregistrementVideoStack implements SurfaceHolder.Callback {

    private Activity activity;

    private SurfaceHolder surfaceHolder;
    public MediaRecorder mrec = new MediaRecorder();
    private Camera mCamera;
    private Camera.Size previewSize;
    private int rotation;
    private int cameraID;
    private TextView timeClock;

    public EnregistrementVideoStack(Activity activity, CameraSurfaceView surfaceView, Camera.Size previewSize, int rotation, int cameraID, TextView timeClock) {//, Camera.Size previewSize, int rotation, Rect croppingRect, Camera camera
        this.activity = activity;

        this.previewSize = previewSize;
        this.rotation = rotation;
        this.cameraID = cameraID;
        this.timeClock = timeClock;

        timeClock.setVisibility(View.VISIBLE);

        surfaceHolder = surfaceView.getHolder();
        surfaceHolder.addCallback(this);
        surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        if(cameraID == 1){
            int i;
            for (i=0; i< Camera.getNumberOfCameras(); i++) {
                Camera.CameraInfo newInfo = new Camera.CameraInfo();
                Camera.getCameraInfo(i, newInfo);
                if (newInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                    break;
                }
            }
            mCamera = Camera.open(i);
        }else{
            mCamera = Camera.open();
        }



        Camera.Parameters params = mCamera.getParameters();
        List<Camera.Size> sizes = params.getSupportedPreviewSizes();
        Camera.Size selected = previewSize;
        params.setPreviewSize(selected.width, selected.height);
        params.setRotation(rotation);
        mCamera.setParameters(params);
        setCameraDisplayOrientation(activity,cameraID,mCamera);

        //mCamera.setDisplayOrientation(rotation);
        mCamera.startPreview();
    }
    public static void setCameraDisplayOrientation(Activity activity, int cameraId, android.hardware.Camera camera) {
        android.hardware.Camera.CameraInfo info = new android.hardware.Camera.CameraInfo();
        android.hardware.Camera.getCameraInfo(cameraId, info);
        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0: degrees = 0; break;
            case Surface.ROTATION_90: degrees = 90; break;
            case Surface.ROTATION_180: degrees = 180; break;
            case Surface.ROTATION_270: degrees = 270; break;
        }

        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;  // compensate the mirror
        } else {  // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }
        camera.setDisplayOrientation(result);
    }

    public void startRecort() {
        try {
            startRecording();
        } catch (Exception e) {
            String message = e.getMessage();
            Log.i(null, "Problem " + message);
            mrec.release();
        }
    }
    public byte[] stopRecort() {
        Log.i(null , "Video stop");

        stopRecording();
        releaseCamera();
        timeClock.setVisibility(View.GONE);
        timeClock.setText("00:00");

        return getVideoByte();
    }

    private void videoDelete(){
        File file = new File(fileName);
        boolean deleted = file.delete();
    }

    private String fileName = "";

    private void startRecording() throws IOException
    {

        if (mCamera == null)
            mCamera = Camera.open();

        File folder = new File(Environment.getExternalStorageDirectory().getAbsolutePath()+ "/Dedi/videos");
        if (!folder.exists()) {
            folder.mkdir();
        }
        fileName = Environment.getExternalStorageDirectory().getAbsolutePath()+"/Dedi/videos/"+new Date().getTime()+"_dedi.mp4";

        // create empty file it must use
        //File file = new File(fileName);

        mrec = new MediaRecorder();

        mCamera.lock();
        mCamera.unlock();

        mrec.setCamera(mCamera);

        // Please maintain sequence of following code.
        //mrec.setVideoSize(  previewSize.width,previewSize.height);
        mrec.setMaxDuration(50000); // 50 seconds
        mrec.setMaxFileSize(20000000); // Approximately 20 megabytes
        // If you change sequence it will not work.
        mrec.setVideoSource(MediaRecorder.VideoSource.DEFAULT);
        mrec.setAudioSource(MediaRecorder.AudioSource.DEFAULT);
       // mrec.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        //mrec.setVideoEncoder(MediaRecorder.VideoEncoder.MPEG_4_SP);
        //mrec.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

        mrec.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH));
        mrec.setPreviewDisplay(surfaceHolder.getSurface());
        mrec.setOutputFile(fileName);


        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0: degrees = 0; break;
            case Surface.ROTATION_90: degrees = 90; break;
            case Surface.ROTATION_180: degrees = 180; break;
            case Surface.ROTATION_270: degrees = 270; break;
        }
        mrec.setOrientationHint(degrees+90);


        mrec.prepare();
        mrec.start();


        long tStart = SystemClock.elapsedRealtime();
        T.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                activity.runOnUiThread(new Runnable() {

                    @Override
                    public void run() {

                        long tEnd = SystemClock.elapsedRealtime();
                        long tRes = tEnd - tStart; // time in nanoseconds
                        //myTextView.setText("count="+count);
                        timeClock.setText(convertTime(tRes));
                        //System.out.println("+++++++++++++"+convertTime(tRes));

                    }
                });

            }
        }, 1000, 1000);

    }
    public String convertTime(long time){
        Date date = new Date(time);
        Format format = new SimpleDateFormat("mm:ss");
        return format.format(date);
    }
    Timer T=new Timer();
    protected void stopRecording() {
        if (mrec != null) {
            mrec.stop();
            mrec.release();
            mCamera.release();
            T.cancel();
        }
    }

    private void releaseMediaRecorder() {
        if (mrec != null) {
            mrec.reset(); // clear recorder configuration
            mrec.release(); // release the recorder object
        }
    }

    private void releaseCamera() {
        if (mCamera != null) {
            mCamera.release(); // release the camera for other applications
            mCamera = null;
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width,
                               int height) {

//        if (mCamera != null) {
//            Camera.Parameters params = mCamera.getParameters();
//            Camera.Size selected = previewSize;
//            params.setPreviewSize(selected.width, selected.height);
//            params.setRotation(rotation);
//            setCameraDisplayOrientation(activity,cameraID,mCamera);
//            mCamera.setParameters(params);
            Log.i("Surface", "surfaceChanged");
//        }

    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if (mCamera != null) {
            Camera.Parameters params = mCamera.getParameters();
            Camera.Size selected = previewSize;
            params.setPreviewSize(selected.width, selected.height);
            params.setRotation(rotation);
            setCameraDisplayOrientation(activity,cameraID,mCamera);
            mCamera.setParameters(params);
            Log.i("Surface", "Created");
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
//        mCamera.stopPreview();
 //       mCamera.release();
    }



    private byte[] getVideoByte(){
        try
        {
            File file = new File(fileName);

            if(file.exists())
            {
                int size = (int) file.length();
                byte[] bytes = new byte[size];
                try {
                    BufferedInputStream buf = new BufferedInputStream(new FileInputStream(file));
                    buf.read(bytes, 0, bytes.length);
                    buf.close();
                } catch (FileNotFoundException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                videoDelete();

                return bytes;
            }

        }
        catch (Exception e)
        {
            Log.e("error", e.getMessage());
            if(e.getMessage().contains("Permission denied")){
                //  initializePermissions(title,text);
            }
        }
        return null;
    }
}