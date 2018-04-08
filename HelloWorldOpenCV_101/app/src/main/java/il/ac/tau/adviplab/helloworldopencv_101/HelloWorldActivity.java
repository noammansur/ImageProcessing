package il.ac.tau.adviplab.helloworldopencv_101;

import android.annotation.SuppressLint;
import android.hardware.Camera;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;

import java.text.SimpleDateFormat;
import java.util.Date;

public class HelloWorldActivity extends AppCompatActivity {

    private static final String TAG = "HelloWorldOpenCV";
    private MyJavaCameraView mOpenCvCameraView;
    private Menu mSettingsMenu;
    private CameraListener mCameraListener = new CameraListener();
    // menu members
    private static final int RESOLUTION_GROUP_ID = 1;
    private static final int PICK_CAMERA_ID = 99;
    private static final int REAR_CAMERA_ID = 100;
    private static final int FRONT_CAMERA_ID = 101;

    private BaseLoaderCallback mLoaderCallback = new
            BaseLoaderCallback(this) {
                @Override
                public void onManagerConnected(int status) {
                    switch (status) {
                        case LoaderCallbackInterface.SUCCESS:
                            Log.i(TAG, "OpenCV loaded successfully");
                            mOpenCvCameraView.enableView();
                            break;
                        default:
                            super.onManagerConnected(status);
                            break;
                    }
                }
            };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        mOpenCvCameraView = findViewById(R.id.Java_Camera_View);
        mOpenCvCameraView.setCameraIndex(CameraBridgeViewBase.CAMERA_ID_ANY);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(mCameraListener);

        Button saveButton = findViewById(R.id.saveButton);
        saveButton.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("SimpleDateFormat")
            @Override
            public void onClick(View v) {
                Log.i(TAG,"onClick event");
                SimpleDateFormat sdf = new
                        SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
                String currentDateandTime = sdf.format(new Date());
                String fileName =
                        Environment.getExternalStorageDirectory().getPath() +
                                "/sample_picture_" + currentDateandTime + ".jpg";
                mOpenCvCameraView.takePicture(fileName);
                Toast.makeText(HelloWorldActivity.this, fileName + " saved",
                        Toast.LENGTH_SHORT).show();
            }
        });

    }

    @Override
    protected void onResume() {
        super.onResume();
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_4_0, this,
                mLoaderCallback);
        Log.i(TAG,"OpenCVLoader success");
    }
    @Override
    protected void onPause() {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu){
        if (mOpenCvCameraView != null && mSettingsMenu == null){
            getMenuInflater().inflate(R.menu.menu_hello_world, menu);
            super.onCreateOptionsMenu(menu);
            int i = 0;
            mSettingsMenu = menu.addSubMenu("Settings");
            SubMenu resolutionMenu = mSettingsMenu.addSubMenu("Resolution");
            for (Camera.Size res : mOpenCvCameraView.getResolutionList()) {
                resolutionMenu.add(RESOLUTION_GROUP_ID, i++, Menu.NONE,
                        res.width + "x" + res.height);
            }
            SubMenu cameraMenu = mSettingsMenu.addSubMenu("Camera");
            cameraMenu.add(PICK_CAMERA_ID, FRONT_CAMERA_ID, Menu.NONE, "Front");
            cameraMenu.add(PICK_CAMERA_ID, REAR_CAMERA_ID, Menu.NONE, "Rear");
            return true;
        }
        return false;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.i(TAG, "called onOptionsItemSelected; selected item: " + item);
        int groupId = item.getGroupId();
        int id = item.getItemId();
        Camera.Size res = mOpenCvCameraView.getResolution();;
        switch (groupId) {
            case RESOLUTION_GROUP_ID:
                // we chose a new resolution
                res =
                        mOpenCvCameraView.getResolutionList().get(id);
                mOpenCvCameraView.setResolution(res);
                res = mOpenCvCameraView.getResolution();
                Toast.makeText(this, res.width + "x" + res.height,
                        Toast.LENGTH_SHORT).show();
                return true;
            case PICK_CAMERA_ID:
                // we chose a new camera
                switch (id) {
                    case FRONT_CAMERA_ID:
                        mOpenCvCameraView.setCameraIndex(CameraBridgeViewBase.CAMERA_ID_FRONT);
                        mOpenCvCameraView.setResolution(res);
                        return true;
                    case REAR_CAMERA_ID:
                        mOpenCvCameraView.setCameraIndex(CameraBridgeViewBase.CAMERA_ID_BACK);
                        mOpenCvCameraView.setResolution(res);
                        return true;
                }
            default:
                return super.onOptionsItemSelected(item);
        }
    }



}
