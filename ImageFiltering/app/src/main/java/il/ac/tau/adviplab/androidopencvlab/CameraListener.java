package il.ac.tau.adviplab.androidopencvlab;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.core.Mat;


class CameraListener implements CameraBridgeViewBase.CvCameraViewListener2 {

    // Constants:
    static final int VIEW_MODE_DEFAULT = 0;
    static final int VIEW_MODE_RGBA = 1;
    static final int VIEW_MODE_GRAYSCALE = 2;
    static final int VIEW_MODE_SOBEL = 3;
    static final int VIEW_MODE_GAUSSIAN = 4;
    static final int VIEW_MODE_BILATERAL = 5;
    static final int VIEW_MODE_UNSHARP = 6;


    //Mode selectors:
    private int mViewMode = VIEW_MODE_DEFAULT;
    private int mColorMode = VIEW_MODE_RGBA;


    //members
    private Mat mImToProcess;
    private Mat mFilteredImage;


    //Getters and setters

    // not used
    /*
    int getColorMode() {
        return mColorMode;
    }
    */

    void setColorMode(int colorMode) {
        mColorMode = colorMode;
    }

    // not used
    /*
    int getViewMode() {
        return mViewMode;
    }
    */

    void setViewMode(int viewMode) {
        mViewMode = viewMode;
    }

    @Override
    public void onCameraViewStarted(int width, int height) {

        mImToProcess = new Mat();
        mFilteredImage = new Mat();

    }

    @Override
    public void onCameraViewStopped() {
        mImToProcess.release();
        mFilteredImage.release();

    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        switch (mColorMode) {
            case VIEW_MODE_RGBA:
                mImToProcess = inputFrame.rgba();
                break;

            case VIEW_MODE_GRAYSCALE:
                mImToProcess = inputFrame.gray();
                break;
        }

        switch (mViewMode) {
            case VIEW_MODE_DEFAULT:
                break;
            case VIEW_MODE_SOBEL:
                MyImageProc.sobelCalcDisplay(mImToProcess,
                        inputFrame.gray(), mFilteredImage);
                break;
            case VIEW_MODE_GAUSSIAN:
                MyImageProc.gaussianCalcDisplay(mImToProcess,
                        inputFrame.gray(), mFilteredImage);
                break;
            case VIEW_MODE_BILATERAL:
                MyImageProc.bilateralCalcDisplay(mImToProcess,
                        inputFrame.gray(), mFilteredImage);
                break;
            case VIEW_MODE_UNSHARP:
                MyImageProc.unSharpCalcDisplay(mImToProcess,
                        inputFrame.gray(), mFilteredImage);
                break;
        }
        return mImToProcess;
    }
}



