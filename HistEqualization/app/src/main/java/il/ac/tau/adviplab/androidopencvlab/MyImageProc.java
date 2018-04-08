package il.ac.tau.adviplab.androidopencvlab;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfInt;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by advlab_10_1 on 3/25/2018.
 */

class MyImageProc extends CameraListener {

    static final int HIST_NORMALIZATION_CONST = 1;
    static final int COMP_MATCH_DISTANCE = 99;

    static void calcHist(Mat image, Mat[] histList, int histSizeNum, int
            normalizationConst, int normalizationNorm) {
        Mat mat0 = new Mat();
        MatOfInt histSize = new MatOfInt(histSizeNum);
        MatOfFloat ranges = new MatOfFloat(0f, 256f);
        int numberOfChannels = Math.min(image.channels(), 3);
        MatOfInt[] channels = new MatOfInt[numberOfChannels];
        for (int i = 0; i < numberOfChannels; i++) {
            channels[i] = new MatOfInt(i);
        }
        int chIdx = 0;
        for (MatOfInt channel : channels) {
            Imgproc.calcHist(Collections.singletonList(image), channel,
                    mat0, histList[chIdx], histSize, ranges);
            Core.normalize(histList[chIdx], histList[chIdx],
                    normalizationConst, 0, normalizationNorm);
            chIdx++;
        }

        mat0.release();
        histSize.release();
        ranges.release();
        for (MatOfInt channel : channels) {
            channel.release();
        }
    }


    static void calcHist(Mat image, Mat[] histList, int histSizeNum) {
        calcHist(image, histList, histSizeNum, 1, Core.NORM_L1);
    }

    private static void showHist(Mat image, Mat[] histList, int
            histSizeNum, int offset, int thickness) {
        float[] buff = new float[histSizeNum];
        int numberOfChannels = Math.min(image.channels(), 3);
        // if image is RGBA, ignore the last channel
        Point mP1 = new Point();
        Point mP2 = new Point();
        Scalar[] mColorsRGB;
        mColorsRGB = new Scalar[] {new Scalar(255, 0, 0), new Scalar(0,
                255, 0), new Scalar(0, 0, 255)};
        for (int chIdx = 0; chIdx < numberOfChannels; chIdx++) {
            Core.normalize(histList[chIdx], histList[chIdx],
                    image.height() / 2, 0, Core.NORM_INF);
            histList[chIdx].get(0, 0, buff);
            for (int h = 0; h < histSizeNum; h++) {
                mP1.x = mP2.x = offset + (chIdx * (histSizeNum + 10) + h) *
                        thickness;
                mP1.y = image.height() - 50;
                mP2.y = mP1.y - 2 - (int) buff[h];
                Imgproc.line(image, mP1, mP2, mColorsRGB[chIdx], thickness);
            }
        }
    }

    static void showHist(Mat image, Mat[] histList, int histSizeNum) {
        int thickness = Math.min(image.width() / (histSizeNum + 10) / 5, 5);
        showHist(image, histList, histSizeNum,
                (image.width() - (5 * histSizeNum + 4 * 10) * thickness) / 2,
                thickness);
    }

    static void equalizeHist(Mat image) {
        int numberOfChannels = image.channels();
        if (numberOfChannels > 1) {
            List<Mat> RGBAChannels = new ArrayList<>(numberOfChannels);
            Core.split(image, RGBAChannels);
            for (Mat colorChannel : RGBAChannels) {
                Imgproc.equalizeHist(colorChannel, colorChannel);
            }
            Core.merge(RGBAChannels, image);
            for (Mat colorChannel : RGBAChannels) {
                colorChannel.release();
            }
        } else {
            Imgproc.equalizeHist(image, image);
        }
    }

    private static void calcCumulativeHist(Mat hist, Mat cumuHist) {
    // Mat hist - histogram
    // Mat cumuHist - cumulative histogram
        int histSizeNum = (int) hist.total();
        float[] buff = new float[histSizeNum];
        float[] CumulativeSum = new float[histSizeNum];
        hist.get(0, 0, buff);
        float sum = 0;

        for (int h = 0; h < histSizeNum; h++) {
            sum += buff[h];
            CumulativeSum[h] = sum;
        }
        cumuHist.put(0, 0, CumulativeSum);
    }

    static void calcCumulativeHist(Mat[] hist, Mat[] cumuHist, int numberOfChannels){
        for (int chIdx = 0; chIdx < numberOfChannels; chIdx++) {
            cumuHist[chIdx].create(hist[chIdx].size(), hist[chIdx].type());
            calcCumulativeHist(hist[chIdx], cumuHist[chIdx]);
        }
    }

    private static void matchHistogram(Mat histSrc, Mat histDst, Mat lookUpTable)
    {
        // Mat histSrc - source histogram
        // Mat histDst - destination histogram
        // Mat lookUpTable - look-up table
        Mat histSrcCum = new Mat(histSrc.size(), histSrc.type());
        Mat histDstCum = new Mat(histDst.size(), histDst.type());

        calcCumulativeHist(histSrc, histSrcCum);
        Core.normalize(histSrcCum, histSrcCum, 1, 0, Core.NORM_INF);
        calcCumulativeHist(histDst, histDstCum);
        Core.normalize(histDstCum, histDstCum, 1, 0, Core.NORM_INF);

        int numOfScales = (int) histSrcCum.total(); //256

        float[] buffSrc = new float[numOfScales];
        float[] buffDst = new float[numOfScales];

        histSrcCum.get(0, 0, buffSrc);
        histDstCum.get(0, 0, buffDst);

        //allocate a buff for the LUT
        int[] buffLUT = new int[numOfScales];
        int j = 0;

        for (int i=0; i< numOfScales; i++){
            while(j< numOfScales && buffSrc[i] > buffDst[j]){
                j++;
            }
            if (j ==256 ) {
                j = 255;
            }
            buffLUT[i] = j;
        }


        lookUpTable.put(0,0,buffLUT);

        //release dynamically allocated memory
        histSrcCum.release();
        histDstCum.release();

    }

    private static void applyIntensityMapping(Mat srcImage, Mat
            lookUpTable) {
        Mat tempMat = new Mat();
        Core.LUT(srcImage, lookUpTable, tempMat);
        tempMat.convertTo(srcImage, CvType.CV_8UC1);
        tempMat.release();
    }

    public static void matchHist(Mat srcImage, Mat dstImage, Mat[] srcHistArray,
                                 Mat[] dstHistArray, boolean histShow) {
        Mat lookupTable = new Mat(256, 1, CvType.CV_32SC1);
        calcHist(srcImage, srcHistArray, 256);
        compareHistograms(srcImage, srcHistArray[0], dstHistArray[0], new Point(50,50), COMP_MATCH_DISTANCE, "Old distance: ");
        matchHistogram(srcHistArray[0], dstHistArray[0], lookupTable);
        applyIntensityMapping(srcImage, lookupTable);
        calcHist(srcImage, srcHistArray, 256);
        compareHistograms(srcImage, srcHistArray[0], dstHistArray[0], new Point(400,50), COMP_MATCH_DISTANCE, "New distance: ");
        lookupTable.release();

        if (histShow) {
            Mat[] dstHistArrayForShow = new Mat[3];
            int thick = Math.min(srcImage.width() / (110) / 5, 5);
            int offset = 2*(srcImage.width()) / 3;
            for (int i = 0; i < dstHistArrayForShow.length; i++) {
                dstHistArrayForShow[i] = new Mat();
            }
            calcHist(dstImage, dstHistArrayForShow, 100);
            showHist(srcImage, dstHistArrayForShow, 100, offset, thick);

            for (int i = 0; i < dstHistArrayForShow.length; i++) {
                dstHistArrayForShow[i].release();
            }
        }
    }

    public static void compareHistograms(Mat image, Mat h1, Mat h2, Point point, int compType, String string) {
        double dist;
        if (compType == COMP_MATCH_DISTANCE) {
            dist = matchDistance(h1, h2);
        } else {
            dist = Imgproc.compareHist(h1, h2, compType);
        }
        Imgproc.putText(
                image, string + String.format("%.2f", dist), point, Core.FONT_HERSHEY_COMPLEX_SMALL, 0.8, new Scalar(200, 200, 250), 1);
    }

    public static double matchDistance(Mat h1, Mat h2) {
        double dist = 0;
        Mat cum1 = new Mat(h1.size(), h1.type());
        Mat cum2 = new Mat(h2.size(), h2.type());
        Mat diffMatrix = new Mat(h2.size(), h2.type());
        calcCumulativeHist(h1, cum1);
        calcCumulativeHist(h2, cum2);
        Core.normalize(cum1, cum1, 1, 0, Core.NORM_L1);
        Core.normalize(cum2, cum2, 1, 0, Core.NORM_L1);
        Core.absdiff(cum1, cum2, diffMatrix);
        dist = Core.norm(diffMatrix, Core.NORM_L1);

        cum1.release();
        cum2.release();
        diffMatrix.release();
        return dist;
    }

}

