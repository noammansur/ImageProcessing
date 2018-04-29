package il.ac.tau.adviplab.androidopencvlab;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
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

@SuppressWarnings("deprecation")

public class SpatialFilteringActivity extends AppCompatActivity {

    private static final String TAG = "SpatialFiltering";

    //menu members
    private SubMenu mResolutionSubMenu;
    private SubMenu mCameraSubMenu;

    //flags
    private Boolean mSettingsMenuAvailable =false;

    protected static final int SETTINGS_GROUP_ID = 1;
    protected static final int RESOLUTION_GROUP_ID = 2;
    protected static final int CAMERA_GROUP_ID = 3;
    protected static final int DEFAULT_GROUP_ID = 4;
    protected static final int COLOR_GROUP_ID = 5;
    protected static final int FILTER_GROUP_ID = 6;
    protected static final int STILL_GROUP_ID = 7;



    private MyJavaCameraView mOpenCvCameraView;

    private CameraListener mCameraListener = new CameraListener();

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
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

    private String[] mCameraNames = {"Front", "Rear"};
    private int[] mCameraIDarray = {CameraBridgeViewBase.CAMERA_ID_FRONT, CameraBridgeViewBase.CAMERA_ID_BACK};


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_spatial_filtering);

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

                Log.i(TAG, "onClick event");
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
                String currentDateandTime = sdf.format(new Date());
                String fileName = Environment.getExternalStorageDirectory().getPath() +
                        "/sample_picture_" + currentDateandTime + ".jpg";
                mOpenCvCameraView.takePicture(fileName);
                addImageToGallery(fileName, SpatialFilteringActivity.this);
                Toast.makeText(SpatialFilteringActivity.this, fileName + " saved",
                        Toast.LENGTH_SHORT).show();

            }
        });

    }

    @Override
    public void onPause() {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onResume() {
        super.onResume();
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_2_0, this, mLoaderCallback);
        Log.i(TAG, "OpenCVLoader success");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.i(TAG, "Options menu created");
        getMenuInflater().inflate(R.menu.menu_spatial_filtering, menu);
        super.onCreateOptionsMenu(menu);

        Menu settingsMenu = menu.addSubMenu("Settings");
        mResolutionSubMenu= settingsMenu.addSubMenu(SETTINGS_GROUP_ID, Menu.NONE, Menu.NONE,
                "Resolution");
        mCameraSubMenu = settingsMenu.addSubMenu(SETTINGS_GROUP_ID, Menu.NONE, Menu.NONE, "Camera");

        //we set up the settings menu in onPrepareOptionsMenu
        menu.add(STILL_GROUP_ID, CameraListener.VIEW_MODE_DEFAULT,
                Menu.NONE, "Still mode");
        menu.add(DEFAULT_GROUP_ID, CameraListener.VIEW_MODE_DEFAULT, Menu.NONE, "Default");

        Menu colorMenu = menu.addSubMenu("Color");
        colorMenu.add(COLOR_GROUP_ID, CameraListener.VIEW_MODE_RGBA, Menu.NONE, "RGBA");
        colorMenu.add(COLOR_GROUP_ID, CameraListener.VIEW_MODE_GRAYSCALE, Menu.NONE, "Grayscale");
        Menu filteringMenu = menu.addSubMenu("Filter");
        SubMenu linearMenu = filteringMenu.addSubMenu("Linear");
        linearMenu.add(FILTER_GROUP_ID, CameraListener.VIEW_MODE_SOBEL,
                Menu.NONE, "Sobel");
        linearMenu.add(FILTER_GROUP_ID, CameraListener.VIEW_MODE_GAUSSIAN,
                Menu.NONE, "Gaussian");
        linearMenu.add(FILTER_GROUP_ID, CameraListener.VIEW_MODE_UNSHARP,
                Menu.NONE, "Unsharp");
        SubMenu nonLinearMenu = filteringMenu.addSubMenu("Non-Linear");
        nonLinearMenu.add(FILTER_GROUP_ID, CameraListener.VIEW_MODE_BILATERAL,
                Menu.NONE, "Bilateral");
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {

        if (mOpenCvCameraView.isCameraOpen() && !mSettingsMenuAvailable){
            setResolutionMenu(mResolutionSubMenu);
            setCameraMenu(mCameraSubMenu);
            mSettingsMenuAvailable =true;
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.i(TAG, "called onOptionsItemSelected; selected item: " + item);
        int groupId = item.getGroupId();
        int id = item.getItemId();

        switch (groupId) {
            case DEFAULT_GROUP_ID:
                mCameraListener.setViewMode(id);
                return true;
            case COLOR_GROUP_ID:
                mCameraListener.setColorMode(id);
                return true;
            case RESOLUTION_GROUP_ID:
                // we chose a new resolution
                Camera.Size res = mOpenCvCameraView.getResolutionList().get(id);
                mOpenCvCameraView.setResolution(res);
                res = mOpenCvCameraView.getResolution();
                Toast.makeText(this, res.width + "x" + res.height, Toast.LENGTH_SHORT).show();
                return true;
            case CAMERA_GROUP_ID:
                mOpenCvCameraView.changeCameraIndex(mCameraIDarray[id]);
                String caption = mCameraNames[id] + " camera";
                Toast.makeText(this, caption, Toast.LENGTH_SHORT).show();
                setResolutionMenu(mResolutionSubMenu);
                return true;
            case FILTER_GROUP_ID:
                mCameraListener.setViewMode(id);
                return true;
            case STILL_GROUP_ID:
                Intent i= new Intent(this, StillsActivity.class);
                startActivity(i);
                return true;


            default:
                return super.onOptionsItemSelected(item);
        }

    }


    public void setResolutionMenu(SubMenu resMenu){
        int i=0;

        resMenu.clear();
        for (Camera.Size res : mOpenCvCameraView.getResolutionList()) {
            resMenu.add(RESOLUTION_GROUP_ID, i++, Menu.NONE, res.width + "x" + res.height);
        }
    }

    private void setCameraMenu(SubMenu camMenu){
        for (int i = 0; i < mOpenCvCameraView.getNumberOfCameras(); i++) {
            camMenu.add(CAMERA_GROUP_ID, i, Menu.NONE, mCameraNames[i]);
        }
    }

    private static void addImageToGallery(final String filePath, final Context context) {

        ContentValues values = new ContentValues();

        values.put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis());
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
        values.put(MediaStore.MediaColumns.DATA, filePath);

        context.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
    }
}
