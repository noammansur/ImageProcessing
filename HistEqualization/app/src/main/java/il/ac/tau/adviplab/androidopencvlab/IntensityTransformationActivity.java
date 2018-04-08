package il.ac.tau.adviplab.androidopencvlab;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
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

@SuppressWarnings("deprecation")
public class IntensityTransformationActivity extends AppCompatActivity {
    private static final String TAG = "IntensityTransforms";

    //Intent tags
    private static final int SELECT_PICTURE = 1;

    //menu members
    private SubMenu mResolutionMenu;
    private SubMenu mCameraMenu;
    private SubMenu mDisplayMenu;

    //flags
    private Boolean mSettingsMenuAvailable = false;

    private static final int RESOLUTION_GROUP_ID = 1;
    private static final int CAMERA_GROUP_ID     = 2;
    private static final int DEFAULT_GROUP_ID    = 3;
    private static final int COLOR_GROUP_ID      = 4;
    private static final int HISTOGRAM_GROUP_ID  = 5;

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
    private int[] mCameraIDarray = {CameraBridgeViewBase.CAMERA_ID_FRONT,
            CameraBridgeViewBase.CAMERA_ID_BACK};


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_intensity_transformation);

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
                addImageToGallery(fileName,
                        IntensityTransformationActivity.this);

                Toast.makeText(IntensityTransformationActivity.this, fileName + " saved",
                        Toast.LENGTH_SHORT).show();
            }
        });

    }

    @Override
    public void onResume() {
        super.onResume();
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_4_0, this, mLoaderCallback);
        Log.i(TAG, "OpenCVLoader success");
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
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
        getMenuInflater().inflate(R.menu.menu_intensity_transforms, menu);
        super.onCreateOptionsMenu(menu);

        Menu settingsMenu = menu.addSubMenu("Settings");
        mResolutionMenu= settingsMenu.addSubMenu("Resolution");
        mCameraMenu = settingsMenu.addSubMenu("Camera");
        //we set up the settings menu in onPrepareOptionsMenu

        menu.add(DEFAULT_GROUP_ID, CameraListener.VIEW_MODE_DEFAULT, Menu.NONE, "Default");

        Menu colorMenu = menu.addSubMenu("Color");
        colorMenu.add(COLOR_GROUP_ID, CameraListener.VIEW_MODE_RGBA, Menu.NONE, "RGBA");
        colorMenu.add(COLOR_GROUP_ID, CameraListener.VIEW_MODE_GRAYSCALE, Menu.NONE, "Grayscale");
        Menu histogramMenu = menu.addSubMenu("Histogram");
        histogramMenu.add(HISTOGRAM_GROUP_ID, CameraListener.VIEW_MODE_HIST_EQUALIZE, Menu.NONE, "Equalize");
        histogramMenu.add(HISTOGRAM_GROUP_ID, CameraListener.VIEW_MODE_HIST_MATCH, Menu.NONE, "Match");
        // Creates toggle button to show and hide histogram on a new "Display" sub-menu
        mDisplayMenu = histogramMenu.addSubMenu("Display");
        mDisplayMenu.add(HISTOGRAM_GROUP_ID,
                CameraListener.VIEW_MODE_SHOW_HIST, Menu.NONE, "Show histogram")
                .setCheckable(true)
                .setChecked(mCameraListener.isShowHistogram());

        mDisplayMenu.add(HISTOGRAM_GROUP_ID,
                CameraListener.VIEW_MODE_SHOW_CUMULATIVE_HIST, Menu.NONE, "Show cumulative histogram")
                .setCheckable(true)
                .setChecked(mCameraListener.isShowCumulativeHistogram());
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {

        if (mOpenCvCameraView.isCameraOpen() && !mSettingsMenuAvailable) {
            setResolutionMenu(mResolutionMenu);
            setCameraMenu(mCameraMenu);
            mSettingsMenuAvailable = true;
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean
    onOptionsItemSelected(MenuItem item) {
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
                setResolutionMenu(mResolutionMenu);
                return true;

            case HISTOGRAM_GROUP_ID:
                switch (id) {
                    case CameraListener.VIEW_MODE_SHOW_HIST:
                        //Toggle button to show/hide histogram
                        item.setChecked(!item.isChecked());
                        mDisplayMenu.getItem(1).setChecked(false);
                        mCameraListener.setShowHistogram(item.isChecked());
                        mCameraListener.setShowCumulativeHistogram(false);
                        break;
                    case CameraListener.VIEW_MODE_HIST_EQUALIZE:
                        mCameraListener.setViewMode(id);
                        break;
                    case CameraListener.VIEW_MODE_SHOW_CUMULATIVE_HIST:
                        item.setChecked(!item.isChecked());
                        mDisplayMenu.getItem(0).setChecked(false);
                        mCameraListener.setShowCumulativeHistogram(item.isChecked());
                        mCameraListener.setShowHistogram(false);
                        break;
                    case CameraListener.VIEW_MODE_HIST_MATCH:
                        if (mCameraListener.getColorMode() == CameraListener.VIEW_MODE_GRAYSCALE )
                        {
                            //Open gallery to select image for matching
                            Intent intent = new Intent();
                            intent.setType("image/*");
                            intent.setAction(Intent.ACTION_GET_CONTENT);
                            startActivityForResult(Intent.createChooser(intent,
                                    "Select image for histogram matching"),
                                    SELECT_PICTURE);
                            mCameraListener.setViewMode(id);
                            break;
                        }
                        else
                        {
                            Toast.makeText(this, "This feature currently works only in grayscale mode", Toast.LENGTH_SHORT).show();
                            break;
                        }

                }
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

    private static void addImageToGallery(final String filePath, final Context
            context) {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.DATE_TAKEN,
                System.currentTimeMillis());
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
        values.put(MediaStore.MediaColumns.DATA, filePath);
        context.getContentResolver().
                insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent
            data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SELECT_PICTURE) {
            if (resultCode == RESULT_OK) {
                Uri imageUri = data.getData();
                if (imageUri != null) {
                    try {
                        Bitmap imageToMatch =
                                MediaStore.Images.Media.getBitmap
                                        (this.getContentResolver(), imageUri);
                        mCameraListener.computeHistOfImageToMatch(imageToMatch);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }


}
