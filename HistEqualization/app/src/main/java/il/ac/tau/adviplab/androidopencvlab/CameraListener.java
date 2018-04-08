package il.ac.tau.adviplab.androidopencvlab;

import android.graphics.Bitmap;

import org.opencv.android.CameraBridgeViewBase;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

class CameraListener implements CameraBridgeViewBase.CvCameraViewListener2 {
    // Constants:
    static final int VIEW_MODE_DEFAULT              = 0;
    static final int VIEW_MODE_RGBA                 = 1;
    static final int VIEW_MODE_GRAYSCALE            = 2;
    static final int VIEW_MODE_SHOW_HIST            = 3;
    static final int VIEW_MODE_HIST_EQUALIZE        = 4;
    static final int VIEW_MODE_SHOW_CUMULATIVE_HIST = 5;

    static final int VIEW_MODE_HIST_MATCH           = 6;



    //Mode selectors:
    private int mViewMode  = VIEW_MODE_DEFAULT;
    private int mColorMode = VIEW_MODE_RGBA;

    public boolean isShowHistogram() {
        return mShowHistogram;
    }

    public void setShowHistogram(boolean showHistogram) {
        mShowHistogram = showHistogram;
    }

    private boolean mShowHistogram = false;

    public boolean isShowCumulativeHistogram() {
        return mShowCumulativeHistogram;
    }

    public void setShowCumulativeHistogram(boolean showCumulativeHistogram) {
        mShowCumulativeHistogram = showCumulativeHistogram;
    }

    private boolean mShowCumulativeHistogram = false;



    //members
    private Mat mImToProcess;
    private Mat mImageToMatch;
    private Mat[] mHistArray;
    private Mat[] mHistDstArray;
    private Mat[] mCumuHistArray;



    //Getters and setters

    int getColorMode() {
        return mColorMode;
    }


    void setColorMode(int colorMode) {
        mColorMode = colorMode;
    }


    int getViewMode() {
        return mViewMode;
    }


    void setViewMode(int viewMode) {
        mViewMode = viewMode;
    }



    @Override
    public void onCameraViewStarted(int width, int height) {
        mImToProcess = new Mat();
        mHistArray = new Mat[] {new Mat(), new Mat(), new Mat()};
        mCumuHistArray = new Mat[] {new Mat(), new Mat(), new Mat()};
    }

    @Override
    public void onCameraViewStopped() {
        mImToProcess.release();
        for (Mat histMat : mHistArray) {
            histMat.release();
        }
        if (mHistDstArray != null) {
            for (Mat mat : mHistDstArray) {
                mat.release();
            }
        }
        if (mImageToMatch != null) {
            mImageToMatch.release();
        }
        if (mCumuHistArray != null) {
            for (Mat CumhistMat : mCumuHistArray) {
                CumhistMat.release();
            }
        }
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
            case VIEW_MODE_HIST_EQUALIZE:
                MyImageProc.equalizeHist(mImToProcess);
                break;
            case VIEW_MODE_HIST_MATCH:
                if (null == mHistDstArray) break;
                if (mHistDstArray[0].total() > 0) {
                    //This handles the case that an image hasn’t been chosen
                    MyImageProc.matchHist(mImToProcess, mImageToMatch,
                            mHistArray, mHistDstArray, true);
                }
                break;
        }

        if (mShowHistogram) {
            int histSizeNum = 100;
            MyImageProc.calcHist(mImToProcess, mHistArray, histSizeNum);
            MyImageProc.showHist(mImToProcess, mHistArray, histSizeNum);
        }

        if (mShowCumulativeHistogram) {
            int numOfChannels = 3;
            int histSizeNum = 100;
            MyImageProc.calcHist(mImToProcess, mHistArray, histSizeNum);
            MyImageProc.calcCumulativeHist(mHistArray, mCumuHistArray , Math.min(numOfChannels, mImToProcess.channels()));
            MyImageProc.showHist(mImToProcess, mCumuHistArray, histSizeNum);
        }


        return mImToProcess;
    }

    void computeHistOfImageToMatch(Bitmap image) {
        //converts a bitmap to Mat
        mImageToMatch = new Mat();
        Utils.bitmapToMat(image, mImageToMatch);
        //convert to grayscale
        Imgproc.cvtColor(mImageToMatch, mImageToMatch,
                Imgproc.COLOR_RGBA2GRAY);
        if (mHistDstArray == null) {
            mHistDstArray = new Mat[3];
            for (int i = 0; i < mHistDstArray.length; i++) {
                mHistDstArray[i] = new Mat();
            }
        }
        if (mCumuHistArray == null) {
            mCumuHistArray = new Mat[3];
            for (int i = 0; i < mCumuHistArray.length; i++) {
                mCumuHistArray[i] = new Mat();
            }
        }

        MyImageProc.calcHist(mImageToMatch, mHistDstArray, 256,
                MyImageProc.HIST_NORMALIZATION_CONST, Core.NORM_L1);

    }

}
