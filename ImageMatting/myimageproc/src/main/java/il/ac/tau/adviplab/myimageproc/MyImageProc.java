package il.ac.tau.adviplab.androidopencvlab;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.utils.Converters;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by advlab_10_1 on 3/25/2018.
 */

public class MyImageProc {

  static final int HIST_NORMALIZATION_CONST = 1;
  static final int COMP_MATCH_DISTANCE = 99;
  private static final int SIGMA_SPATIAL_DEFAULT = 10;
  private static final int SIGMA_INTENSITY_DEFAULT = 50;
  public static final int SIGMA_SPATIAL_MAX = 50;
  public static final int SIGMA_INTENSITY_MAX = 50;
  public static final float ALPHA_DEFAULT = 0.5f;
  public static final float BETA_DEFAULT = 5;
  public static final int ALPHA_MAX = 1;
  public static final int BETA_MAX = 10;
  public static final int CV_FILL = -1;

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

  private static void sobelFilter(Mat inputImage, Mat outputImage, int[]
          window) {
//Applies the Sobel filter to image
    Mat grayInnerWindow = inputImage.submat(window[0], window[1],
            window[2], window[3]);
    Mat grad_x = new Mat();
    Mat grad_y = new Mat();
    int ddepth = CvType.CV_16SC1;
    Imgproc.Sobel(grayInnerWindow, grad_x, ddepth, 1, 0);
    Core.convertScaleAbs(grad_x, grad_x, 10, 0);
    Imgproc.Sobel(grayInnerWindow, grad_y, ddepth, 0, 1);
    Core.convertScaleAbs(grad_y, grad_y, 10, 0);
    Core.addWeighted(grad_x, 0.5, grad_y, 0.5, 0, outputImage);
    grayInnerWindow.release();
    grad_x.release();
    grad_y.release();
  }

  private static void displayFilter(Mat displayImage, Mat filteredImage,
                                    int[] window) {
    Mat rgbaInnerWindow = displayImage.submat(window[0], window[1],
            window[2], window[3]);
    if (displayImage.channels() > 1) {
      Imgproc.cvtColor(filteredImage, rgbaInnerWindow,
              Imgproc.COLOR_GRAY2BGRA, 4);
    } else {
      filteredImage.copyTo(rgbaInnerWindow);
    }
    rgbaInnerWindow.release();
  }

  private static int[] setWindow(Mat image) {
    //Add your implementation here.
    int width=image.cols();
    int height=image.rows();
    int delta_x = Math.max(width/20,10);
    int delta_y = Math.max(height/20,10);

    int top = delta_y;
    int bottom= height-delta_y;
    int left = delta_x;
    int right = width-delta_x;

    return new int[] {top, bottom, left, right};
  }

  public static void sobelCalcDisplay(Mat displayImage, Mat inputImage,
                                      Mat filteredImage) {
    //The function applies the Sobel filter and returns the result in a format suitable for display.
    int[] window = setWindow(displayImage);
    sobelFilter(inputImage, filteredImage, window);
    displayFilter(displayImage, filteredImage, window);
  }

  public static void sobelCalcDisplay(Mat displayImage, Mat inputImage,
                                      Mat filteredImage, float sigma) {
    //The function applies the Sobel filter and returns the result in a format suitable for display.
    Mat tmp = new Mat();
    int[] window = setWindow(displayImage);
    gaussianFilter(inputImage, tmp, window, sigma);
    window = setWindow(tmp);
    sobelFilter(tmp, filteredImage, window);
    displayFilter(displayImage, filteredImage, window);
    tmp.release();
  }

  private static void gaussianFilter(Mat inputImage, Mat outputImage, int[] window, float sigma) {
    //Applies Gaussian filter to image
    Mat grayInnerWindow = inputImage.submat(window[0], window[1],
            window[2], window[3]);
    Size ksize = new Size(4 * (int) sigma + 1, 4 * (int) sigma + 1);
    try {
      // sigmaX = sigmaY = sigma
      Imgproc.GaussianBlur(grayInnerWindow, outputImage, ksize, sigma,
              sigma);
      grayInnerWindow.release();
    } catch (Exception e) {
      System.out.println("Error: " + e.getMessage());
    }
  }

  private static void gaussianFilter(Mat inputImage, Mat outputImage, int[] window) {
    gaussianFilter(inputImage, outputImage, window, SIGMA_SPATIAL_DEFAULT);
  }

  public static void gaussianCalcDisplay(Mat displayImage, Mat inputImage, Mat filteredImage, float sigma) {
    int[] window = setWindow(displayImage);
    gaussianFilter(inputImage, filteredImage, window,sigma);
    displayFilter(displayImage, filteredImage, window);
  }

  public static void gaussianCalcDisplay(Mat displayImage, Mat inputImage, Mat filteredImage) {
    gaussianCalcDisplay(displayImage, inputImage, filteredImage, SIGMA_SPATIAL_DEFAULT);
  }

  private static void bilateralFilter(Mat inputImage, Mat outputImage, int[] window, float sigmaSpatial, float sigmaIntensity) {
    //Applies bilateralFilter to image
    Mat grayInnerWindow = inputImage.submat(window[0], window[1],
            window[2], window[3]);
    int d = 4 * (int) sigmaSpatial + 1;
    try {
      Imgproc.bilateralFilter(grayInnerWindow, outputImage, d,
              sigmaIntensity, sigmaSpatial);
      grayInnerWindow.release();
    } catch (Exception e) {
      System.out.println("Error: " + e.getMessage());
    }
  }

  private static void bilateralFilter(Mat inputImage, Mat outputImage, int[] window) {
    bilateralFilter(inputImage, outputImage, window, SIGMA_SPATIAL_DEFAULT, SIGMA_INTENSITY_DEFAULT);
  }


  public static void bilateralCalcDisplay(Mat displayImage, Mat inputImage, Mat filteredImage, float sigmaSpatial, float sigmaIntensity) {
    int[] window = setWindow(displayImage);
    bilateralFilter(inputImage, filteredImage, window, sigmaSpatial, sigmaIntensity);
    displayFilter(displayImage, filteredImage, window);
  }

  public static void bilateralCalcDisplay(Mat displayImage, Mat inputImage, Mat filteredImage) {
    bilateralCalcDisplay(displayImage, inputImage, filteredImage, SIGMA_SPATIAL_DEFAULT, SIGMA_INTENSITY_DEFAULT);
  }

  public static void unSharpFilter(Mat inputImage, Mat outputImage,
                                   int[] window, float sigmaSpatial, float alpha, float beta) {

    inputImage.convertTo(inputImage,CvType.CV_32FC1);
    outputImage.convertTo(outputImage, CvType.CV_32FC1);
    float c = 1/(1+beta-alpha*beta);
    try {

      gaussianFilter(inputImage, outputImage, window, sigmaSpatial);
      Mat inputImageWindow = inputImage.submat(window[0],window[1],window[2],window[3]);

      Core.addWeighted(inputImageWindow,1,outputImage,-alpha,0,outputImage); // y = x-a*(G*x)
      Core.addWeighted(inputImageWindow, 1, outputImage,beta,0,outputImage); // output = x+b*y
      Core.multiply(outputImage, Scalar.all(c), outputImage); // normalize
      outputImage.convertTo(outputImage, CvType.CV_8UC1);
      }
      catch (Exception e) {
      System.out.println("Error: " + e.getMessage());
    }
  }

  public static void unSharpCalcDisplay(Mat displayImage ,Mat inputImage,Mat filteredImage,
                                        float sigmaSpatial, float alpha, float beta){
    int[] window = setWindow(displayImage);
    unSharpFilter(inputImage,filteredImage, window, sigmaSpatial, alpha, beta);
    displayFilter(displayImage, filteredImage, window);
  }

  public static void unSharpCalcDisplay(Mat displayImage ,Mat inputImage,Mat filteredImage){
    unSharpCalcDisplay(displayImage ,inputImage, filteredImage,
            SIGMA_SPATIAL_DEFAULT, ALPHA_DEFAULT ,BETA_DEFAULT);
  }

  public static void detectAndReplaceChessboard(Mat sourceImage, Mat replacementImage)
  {
    // sourceImage - a source image with a chessboard pattern
    // replacementImage - an image to replace the chessboard
    int bwThreshold = 100;
    Mat bwImage = new Mat();
    Mat dilatedImage = new Mat();
    Mat erodedImage = new Mat();
    Size size = new Size(5, 5);

    im2BW(sourceImage, bwImage, bwThreshold, Imgproc.THRESH_BINARY_INV);
    imDilate(bwImage, dilatedImage, size);
    imErode(bwImage, erodedImage, size);

    bwImage.release();

    List<MatOfPoint> hull = findClutterOfConnectedComponents(sourceImage, dilatedImage, erodedImage, 30, 35, false);
    List<Point> listOfPointsOnApproxCurve = approxCurve(sourceImage, hull, false);
    List<Point> oldListOfPoints = null;
    List<Point> boundaryPoints = null;
    List<MatOfPoint> harrisContours;
    List<MatOfPoint> harrisContoursOnChess;
    Mat labelImage = new Mat();
    Mat corners = new Mat();
    try {
      if (listOfPointsOnApproxCurve.size() == 4) {
        Imgproc.cvtColor(sourceImage, bwImage, Imgproc.COLOR_RGBA2GRAY);
        corners = bwImage.clone();
        corners.convertTo(corners, CvType.CV_32F);
        Imgproc.cornerHarris(bwImage, corners, 3, 5, 0.04);
        Core.normalize(corners, corners, 0, 255, Core.NORM_MINMAX);
        corners.convertTo(corners, CvType.CV_8U);
        Imgproc.threshold(corners, corners, 55, 255, Imgproc.THRESH_BINARY_INV);
        harrisContours = getContours(corners, labelImage, false);
        harrisContoursOnChess = getContours(corners.submat(Imgproc.boundingRect(hull.get(0))), labelImage, false);
        if ((float)harrisContoursOnChess.size() / (float)harrisContours.size() > 0.8 & (float)harrisContoursOnChess.size()>30) {
          boundaryPoints = sortPoints(listOfPointsOnApproxCurve, sourceImage, false);
          oldListOfPoints = boundaryPoints;
        }
      }
      else{
        boundaryPoints = oldListOfPoints;
      }
      if (boundaryPoints!= null)
      {
        replaceROIwithAnotherImage(sourceImage, replacementImage, boundaryPoints);
      }
    }
    catch (Exception e){
      System.out.println("Error: " + e.getMessage());
    }

    labelImage.release();
  }

  private static void im2BW(Mat image, Mat bwImage, double threshold, int type)
  {
    // type can be either Imgproc.THRESH_BINARY_INV or Imgproc.THRESH_BINARY
    // first convert to gray scale
    Imgproc.cvtColor(image, bwImage, Imgproc.COLOR_RGB2GRAY);
    Imgproc.threshold(bwImage, bwImage, threshold, 255, type);
  }

  private static void imDilate(Mat image, Mat dilatedImage, Size strelSize) {
    Mat SE = new Mat();
    SE = Imgproc.getStructuringElement(Imgproc.CV_SHAPE_RECT,strelSize);
    Imgproc.dilate(image, dilatedImage, SE);
  }

  private static void imErode(Mat image, Mat erodedImage, Size strelSize) {
    Mat SE = new Mat();
    SE = Imgproc.getStructuringElement(Imgproc.CV_SHAPE_RECT,strelSize);
    Imgproc.erode(image, erodedImage, SE);
  }

  private static List<MatOfPoint> findClutterOfConnectedComponents(
          Mat sourceImage, Mat dilatedImage, Mat erodedImage, int tmin, int tmax, boolean showFlag)
  {
    List<MatOfPoint> hullList = new ArrayList<>();
    Mat bwImage = new Mat();
    int kernel_size  = 5;
    Size seSize=new Size(kernel_size,kernel_size);
    int bwThreshold = 100;
    Mat labelImage = new Mat();
    Mat stats=new Mat();
    Mat centroids=new Mat();
    int numOfBlobs;
    Mat boundedLabels = new Mat();
    int labelIdx=-1;

    im2BW(sourceImage, bwImage, bwThreshold, Imgproc.THRESH_BINARY_INV);
    imDilate(bwImage, dilatedImage, seSize);
    int numOfObjects = Imgproc.connectedComponentsWithStats(dilatedImage, labelImage, stats, centroids);

    MyImageProc.imErode(bwImage, erodedImage, seSize);
    int maxNumOfConnectedComp = 0;
    for (int i=0; i < numOfObjects; i++)
    {
      Mat statsRow=stats.row(i);
      Mat centerVector=centroids.row(i);
      int [] rectDim= new int [4];
      double [] currentCentroid= new double [2];
      statsRow.get(0,0,rectDim);
      centerVector.get(0,0,currentCentroid);
      statsRow.release();
      centerVector.release();
      Point center =new Point(currentCentroid[0], currentCentroid[1]);

      Mat boundedObject = erodedImage.submat(rectDim[1], rectDim[1] + rectDim[3], rectDim[0], rectDim[0] + rectDim[2]);
      numOfBlobs = Imgproc.connectedComponents(boundedObject, boundedLabels);
      boundedObject.release();

      if (showFlag) {
        Imgproc.rectangle(sourceImage, new Point(rectDim[0], rectDim[1]), new Point(rectDim[0] + rectDim[2], rectDim[1]   + rectDim[3]), new Scalar(255, 0, 0));
        Imgproc.putText(sourceImage, Integer.toString(numOfBlobs), center, Core.FONT_HERSHEY_COMPLEX, 1, new Scalar(0, 0, 255));
      }

      if (numOfBlobs <= tmax && numOfBlobs >= tmin) {
        labelIdx = i;
        break;
      }
      if (maxNumOfConnectedComp < numOfBlobs)
      {
        maxNumOfConnectedComp = numOfBlobs;
        labelIdx = i;
      }
    }

    Mat singleBlob = new Mat();
    Mat nonZero = new Mat();
    MatOfPoint hull = new MatOfPoint();
    Core.compare(labelImage, new Scalar(labelIdx), singleBlob, Core.CMP_EQ );
    Core.findNonZero(singleBlob, nonZero);
    MatOfPoint points = new MatOfPoint(nonZero);
    convexHull(points, hull);
    hullList.add(hull);


    singleBlob.release();
    nonZero.release();
    labelImage.release();
    stats.release();
    centroids.release();
    boundedLabels.release();
    points.release();
    return hullList;
  }

  private static void convexHull(MatOfPoint points, MatOfPoint hull) {
    MatOfInt hullMatOfInt = new MatOfInt();
    List<Point> pointsList = new LinkedList<>();
    Imgproc.convexHull(points, hullMatOfInt);
    for (int i = 0; i < hullMatOfInt.height(); i++) {
      pointsList.add(points.toList().get(hullMatOfInt.toList().get(i)));
    }
    hull.fromList(pointsList);
    hullMatOfInt.release();
  }

  private static List<Point> approxCurve(Mat sourceImage,
                                         List<MatOfPoint> pointsOnCurveList, boolean drawFlag) {
    MatOfPoint2f hullOfChessBoard2f = new
            MatOfPoint2f(pointsOnCurveList.get(0).toArray());
    MatOfPoint2f approxCurve = new MatOfPoint2f();
    List<MatOfPoint> approxCurveList = new ArrayList<>();
    List<Point> listOfPointsOnApproxCurve = new ArrayList<>();
    Imgproc.approxPolyDP(hullOfChessBoard2f, approxCurve,
            Imgproc.arcLength(hullOfChessBoard2f, true) * 0.02, true);
    approxCurveList.add(new MatOfPoint(approxCurve.toArray()));
    if (drawFlag) {
      Imgproc.drawContours(sourceImage, approxCurveList, 0, new
              Scalar(0, 255, 0), 2);
    }
    for (MatOfPoint matOfPoint : approxCurveList) {
      listOfPointsOnApproxCurve = matOfPoint.toList();
    }
    return listOfPointsOnApproxCurve;
  }

  private static List<Point> sortPoints(List<Point> listOfPoints, Mat image, boolean drawFlag)
  {
    List<Point> tempList = new ArrayList<>();
    List<Point> listOfSortedPoints = new ArrayList<>();
    Point centerMass = new Point(0,0);
    // calculate the center mass of all points
    for (int i=0;i<listOfPoints.size();++i) {
      tempList.add(i,listOfPoints.get(i));
      centerMass.x += listOfPoints.get(i).x;
      centerMass.y += listOfPoints.get(i).y;
    }
    centerMass.x = centerMass.x/listOfPoints.size();
    centerMass.y = centerMass.y/listOfPoints.size();

    double minDistance;
    double currDistance;
    int index=0;

    Point firstPoint = tempList.get(0);
    // calculate the closest point to (0,0)
    minDistance = Math.pow(Math.pow(firstPoint.x, 2) + Math.pow(firstPoint.y, 2), 0.5);
    for (int i=1;i<tempList.size();++i) {
      firstPoint = tempList.get(i);
      currDistance = Math.pow(Math.pow(firstPoint.x, 2) + Math.pow(firstPoint.y, 2), 0.5);
      if (currDistance < minDistance) {
        minDistance = currDistance;
        index = i;
      }
    }
    listOfSortedPoints.add(tempList.get(index)); //insert first point
    tempList.remove(index);
    while (tempList.size()>0){
      int size = listOfSortedPoints.size();
      double lastAngle = Math.atan2(listOfSortedPoints.get(size-1).y-centerMass.y , listOfSortedPoints.get(size-1).x-centerMass.x);
      double currAngle = 0;
      double minDistAngle = Double.MAX_VALUE;
      index = 0;
      double currAngleDiff;
      for (int i=0;i<tempList.size();++i) {
        currAngle = Math.atan2(tempList.get(i).y - centerMass.y , tempList.get(i).x - centerMass.x);
        currAngleDiff = lastAngle - currAngle;
        if (currAngleDiff < 0) {
          currAngleDiff += 2 * Math.PI;
        }
        if (currAngleDiff < minDistAngle) {
          minDistAngle = currAngleDiff;
          index = i;
        }
      }
      listOfSortedPoints.add(tempList.get(index)); //insert next point
      tempList.remove(index);
    }
    if (drawFlag){
      int j = 0;
      while ( j < listOfSortedPoints.size()) {
        if (j % 4 == 0)
          Imgproc.drawMarker(image, listOfSortedPoints.get(j), new Scalar(255, 0, 0));
        if (j % 4 == 1)
          Imgproc.drawMarker(image, listOfSortedPoints.get(j), new Scalar(0, 255, 0));
        if (j % 4 == 2)
          Imgproc.drawMarker(image, listOfSortedPoints.get(j), new Scalar(0, 0, 255));
        if (j % 4 == 3)
          Imgproc.drawMarker(image, listOfSortedPoints.get(j), new Scalar(255, 255, 0));
        j++;
      }

    }
    return listOfSortedPoints;
  }

  private static void replaceROIwithAnotherImage(Mat image, Mat replacementImage, List<Point> boundaryPoints)
  {
    Mat intermediateMat = new Mat(image.size(), image.type());
    List<Point> quadPoints;
    Mat transMtx;
    quadPoints = setQuadCorners(replacementImage);
    //converting a list of points to Mat
    Mat quadPointsMat = Converters.vector_Point2d_to_Mat(quadPoints);
    Mat boundaryPointsMat = Converters.vector_Point2d_to_Mat(boundaryPoints);
    quadPointsMat.convertTo(quadPointsMat, CvType.CV_32FC2);
    boundaryPointsMat.convertTo(boundaryPointsMat, CvType.CV_32FC2);
    //getting the transformation matrix
    transMtx = Imgproc.getPerspectiveTransform(quadPointsMat, boundaryPointsMat);
    //Applying the warp and putting the result in intermediateMat
    intermediateMat.setTo(new Scalar(0));
    Imgproc.warpPerspective(replacementImage, intermediateMat, transMtx, intermediateMat.size());
    //Replacing the chessboard with the warped image
    fillPoly(image, boundaryPoints, new Scalar(0));
    Core.add(image, intermediateMat, image);
    intermediateMat.release();
    transMtx.release();
  }

  private static List<Point> setQuadCorners(Mat image)
  {
    List<Point> quadPoints = new ArrayList<>();

    quadPoints.add(new Point(0,0));
    quadPoints.add(new Point(0,image.height()-1));
    quadPoints.add(new Point(image.width()-1,image.height()-1));
    quadPoints.add(new Point(image.width()-1,0));

    return quadPoints;
  }

  private static void fillPoly(Mat image, List<Point> listOfPointsOnPolygon, Scalar scalar)
  {
    List<MatOfPoint> listOfPointsMatOfPoints = new ArrayList<>();
    MatOfPoint matOfPointsOnPolygon = new MatOfPoint();
    matOfPointsOnPolygon.fromList(listOfPointsOnPolygon);
    listOfPointsMatOfPoints.add(matOfPointsOnPolygon);
    Imgproc.fillPoly(image, listOfPointsMatOfPoints, scalar);
  }

  private static List<MatOfPoint> getContours(Mat bwImage, Mat labelImage, boolean showFlag) {
    List<MatOfPoint> contours = new ArrayList<>();
    Mat hierarchy = new Mat(bwImage.rows(), bwImage.cols(), CvType.CV_8UC1, new Scalar(0));
    Mat bwCopy = bwImage.clone();
    Imgproc.findContours(bwCopy, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
    int numOfContours = contours.size();
    if (showFlag) {
      for (int idx = 0; idx < numOfContours; idx++) {
        Scalar color = new Scalar(((double) idx / numOfContours) * 255,
                ((double) idx / numOfContours) * 255,
                ((double) idx / numOfContours) * 255);
        Imgproc.drawContours(labelImage, contours, idx, color,
                MyImageProc.CV_FILL, 8, hierarchy, 0, new Point());
      }
    }
    hierarchy.release();
    bwCopy.release();
    return contours;
  }

}