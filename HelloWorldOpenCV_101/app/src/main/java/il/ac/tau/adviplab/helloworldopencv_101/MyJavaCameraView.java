package il.ac.tau.adviplab.helloworldopencv_101;

import org.opencv.android.JavaCameraView;

import android.content.Context;
import android.hardware.Camera;
import android.util.AttributeSet;
import android.util.Log;

import java.io.FileOutputStream;
import java.util.List;

/**
 * Created by advlab_10_1 on 3/4/2018.
 */

public class MyJavaCameraView extends JavaCameraView implements Camera.PictureCallback {

    private static final String TAG = "MyJavaCameraView";
    private String mPictureFileName;

    public MyJavaCameraView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public List<Camera.Size> getResolutionList() {
        return mCamera.getParameters().getSupportedPreviewSizes();
    }
    public Camera.Size getResolution() {
        return mCamera.getParameters().getPreviewSize();
    }
    public void setResolution(Camera.Size resolution) {
        disconnectCamera();
        mMaxHeight = resolution.height;
        mMaxWidth = resolution.width;
        connectCamera(getWidth(), getHeight());
    }


    public void takePicture(final String fileName) {
        Log.i(TAG, "Taking picture");
        mPictureFileName = fileName;
        // Postview and jpeg are sent in the same buffers if the queue is not empty when performing a capture.
        // Clear up buffers to avoid mCamera.takePicture to be stuck because of a memory issue
        mCamera.setPreviewCallback(null);
        mCamera.takePicture(null, null, this);
        // PictureCallback is implemented by the current class
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
            Log.e(TAG, "Exception in onPictureTaken", e);
        }
    }
}
