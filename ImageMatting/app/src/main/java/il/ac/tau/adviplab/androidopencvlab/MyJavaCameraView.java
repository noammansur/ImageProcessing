package il.ac.tau.adviplab.androidopencvlab;

import android.content.Context;
import android.hardware.Camera;
import android.util.AttributeSet;
import android.util.Log;

import org.opencv.android.JavaCameraView;

import java.io.FileOutputStream;
import java.util.List;

@SuppressWarnings("deprecation")

public class MyJavaCameraView extends JavaCameraView implements Camera.PictureCallback {

    private static final String TAG = "MyJavaCameraView";
    private String mPictureFileName;

    public MyJavaCameraView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mMaxHeight = 480;
        mMaxWidth = 640;
    }

    public boolean isCameraOpen(){
        return (mCamera != null); // returns null if camera is unavailable
    }

    public int getNumberOfCameras() {
        return Camera.getNumberOfCameras();
    }

    public void changeCameraIndex(int newIndex) {
        disconnectCamera();
        setCameraIndex(newIndex);
        connectCamera(getWidth(), getHeight());
    }


    public List<Camera.Size> getResolutionList() {
        return mCamera.getParameters().getSupportedPreviewSizes();
    }

    public void setResolution(Camera.Size resolution) {
        disconnectCamera();
        mMaxHeight = resolution.height;
        mMaxWidth = resolution.width;
        connectCamera(getWidth(), getHeight());
    }

    public Camera.Size getResolution() {
        return mCamera.getParameters().getPreviewSize();
    }

    public void takePicture(final String fileName) {
        Log.i(TAG, "Taking picture");
        this.mPictureFileName = fileName;
        // Postview and jpeg are sent in the same buffers if the queue is not empty when performing a capture.
        // Clear up buffers to avoid mCamera.takePicture to be stuck because of a memory issue
        mCamera.setPreviewCallback(null);

        // PictureCallback is implemented by the current class
        mCamera.takePicture(null, null, this);
    }

    @Override
    public void onPictureTaken(byte[] data, Camera camera) {
        Log.i(TAG, "Saving a bitmap to file");
        // The camera preview was automatically stopped. Start it again.
        mCamera.startPreview();
        mCamera.setPreviewCallback(this);

        // Write the image in a file (in jpeg format)
        try {
            FileOutputStream fos = new FileOutputStream(mPictureFileName);

            fos.write(data);
            fos.close();

        } catch (java.io.IOException e) {
            Log.e("PictureDemo", "Exception in photoCallback", e);
        }
    }
}


