package il.ac.tau.adviplab.androidopencvlab;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import java.util.Locale;

import il.ac.tau.adviplab.myimageproc.Util;

public class StillsActivity extends SpatialFilteringActivity {

    private static final int SELECT_PICTURE = 1;

    private Uri mURI;
    private Bitmap mBitmap;
    private ImageView mImageView;
    private Mat mImToProcess = new Mat();
    private Mat mImGray = new Mat();
    private Mat mFilteredImage = new Mat();
    private SeekBar mSeekBarSpatial;
    private SeekBar mSeekBarIntensity;
    private SeekBar mSeekBarAlpha;
    private SeekBar mSeekBarBeta;
    private TextView mTextViewSpatial;
    private TextView mTextViewIntensity;
    private TextView mTextViewAlpha;
    private TextView mTextViewBeta;

    private MenuItem mSelectedItem;

    private void setSeekBar(final SeekBar seekbar, final TextView textview,
                            final String string, final float sigmaMax) {
        float sigma = ((float) seekbar.getProgress() / (float) seekbar.getMax()) * sigmaMax;
        textview.setText(String.format(Locale.ENGLISH, "%s%.2f", string, sigma));
        seekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progressValue, boolean fromUser) {
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                float sigma = ((float) seekbar.getProgress() / (float)
                        seekbar.getMax()) * sigmaMax;
                textview.setText(String.format(Locale.ENGLISH, "%s%.2f", string, sigma));
                //Call the filter again
                if (mSelectedItem != null) {
                    int groupId = mSelectedItem.getGroupId();
                    if (groupId == FILTER_GROUP_ID) {
                        launchRingDialog(mSelectedItem.getItemId());
                    }
                }
            }
                                                   });
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stills);
        mImageView = findViewById(R.id.imageView1);
        Button loadButton = (Button) findViewById(R.id.loadButton);
        loadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(Intent.createChooser(intent,
                        "Select image to load"), SELECT_PICTURE);
            }
        });
        mSeekBarSpatial = findViewById(R.id.seekBarSpatial);
        mTextViewSpatial = findViewById(R.id.sigmaSpatialTextView);
        setSeekBar(mSeekBarSpatial, mTextViewSpatial,
                getResources().getString(R.string.stringSpatial),
                MyImageProc.SIGMA_SPATIAL_MAX);
        mSeekBarIntensity = findViewById(R.id.seekBarIntensity);
        mTextViewIntensity = findViewById(R.id.sigmaIntensityTextView);
        setSeekBar(mSeekBarIntensity,mTextViewIntensity, getResources()
                        .getString(R.string.stringIntensity),
                MyImageProc.SIGMA_INTENSITY_MAX);

        mSeekBarAlpha = findViewById(R.id.seekBarAlpha);
        mTextViewAlpha = findViewById(R.id.alphaTextView);
        setSeekBar(mSeekBarAlpha, mTextViewAlpha,
                getResources().getString(R.string.stringAlpha),
                MyImageProc.ALPHA_MAX);

        mSeekBarBeta = findViewById(R.id.seekBarBeta);
        mTextViewBeta = findViewById(R.id.betaTextView);
        setSeekBar(mSeekBarBeta, mTextViewBeta,
                getResources().getString(R.string.stringBeta),
                MyImageProc.BETA_MAX);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent
            data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SELECT_PICTURE) {
            if (resultCode == RESULT_OK) {
                mURI = data.getData();
                if (mURI != null) {
                    try {
                        mBitmap = Util.getBitmap(this, mURI);
                        if (mBitmap != null) {
                            mImageView.setImageBitmap(Util.getResizedBitmap(mBitmap,
                                    1000));
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        mSelectedItem = item;
        int groupId = item.getGroupId();
        int id = item.getItemId();
        switch (groupId) {
            case SETTINGS_GROUP_ID:
                Toast.makeText(this, getString(R.string.notAvailable),
                        Toast.LENGTH_SHORT).show();
                return true;
            case DEFAULT_GROUP_ID:
                mSeekBarIntensity.setProgress(0);
                mSeekBarSpatial.setProgress(0);
                mSeekBarAlpha.setProgress(0);
                mSeekBarBeta.setProgress(0);
                mTextViewIntensity = (TextView) findViewById(R.id.sigmaIntensityTextView);
                mTextViewSpatial = (TextView) findViewById(R.id.sigmaSpatialTextView);
                mTextViewAlpha = (TextView) findViewById(R.id.alphaTextView);
                mTextViewBeta = (TextView) findViewById(R.id.betaTextView);

                mTextViewIntensity.setText(String.format(Locale.ENGLISH, "%s%.2f", getResources().getString(R.string.stringSpatial), 0.00));
                mTextViewSpatial.setText(String.format(Locale.ENGLISH, "%s%.2f", getResources().getString(R.string.stringIntensity), 0.00));
                mTextViewAlpha.setText(String.format(Locale.ENGLISH, "%s%.2f", getResources().getString(R.string.stringAlpha), 0.00));
                mTextViewBeta.setText(String.format(Locale.ENGLISH, "%s%.2f", getResources().getString(R.string.stringBeta), 0.00));

                if (mURI != null) {
                    mBitmap = Util.getBitmap(this, mURI);
                    if (mBitmap != null) {
                        mImageView.setImageBitmap(Util.getResizedBitmap(mBitmap,
                                1000));
                        return true;
                    }

                }

            case COLOR_GROUP_ID:
                if (mURI != null) {
                    mBitmap = Util.getBitmap(this, mURI);
                    if (mBitmap != null) {
                        if (id == CameraListener.VIEW_MODE_GRAYSCALE) {
                            Utils.bitmapToMat(mBitmap, mImToProcess);
                            Imgproc.cvtColor(mImToProcess, mImToProcess,
                                    Imgproc.COLOR_RGBA2GRAY);
                            mBitmap = Bitmap.createBitmap(mImToProcess.cols(),
                                    mImToProcess.rows(), Bitmap.Config.RGB_565);
                            Utils.matToBitmap(mImToProcess, mBitmap);
                        }
                        mImageView.setImageBitmap(Util.getResizedBitmap(mBitmap,
                                1000));
                        return true;
                    }
                }
            case FILTER_GROUP_ID:
                launchRingDialog(id);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public static void filterImage(int id, Mat imToDisplay, Mat imToProcess, Mat filteredImage, float sigmaSpatial,
                                   float sigmaIntensity, float alpha, float beta) {
        switch (id) {
            case CameraListener.VIEW_MODE_SOBEL:
                if (sigmaSpatial > 0) {
                    MyImageProc.sobelCalcDisplay(imToDisplay, imToProcess,
                            filteredImage, sigmaSpatial);
                } else {
                    MyImageProc.sobelCalcDisplay(imToDisplay, imToProcess,
                            filteredImage);
                }

                break;
            case CameraListener.VIEW_MODE_GAUSSIAN:
                if (sigmaSpatial > 0) {
                    MyImageProc.gaussianCalcDisplay(imToDisplay, imToProcess,
                            filteredImage, sigmaSpatial);
                }
                break;
            case CameraListener.VIEW_MODE_BILATERAL:
                if (sigmaSpatial > 0) {
                    MyImageProc.bilateralCalcDisplay(imToDisplay,
                            imToProcess, filteredImage, sigmaSpatial,
                            sigmaIntensity);
                }
                break;
            case CameraListener.VIEW_MODE_UNSHARP:
                if (sigmaSpatial > 0 && alpha>0 && beta >0) {
                    MyImageProc.unSharpCalcDisplay(imToDisplay,
                            imToProcess, filteredImage, sigmaSpatial,
                            alpha, beta);
                }else{
                    MyImageProc.unSharpCalcDisplay(imToDisplay,
                            imToProcess, filteredImage);
                }
                break;
        }
    }

    public void launchRingDialog(final int id) {
        final ProgressDialog ringProgressDialog = ProgressDialog.show(this,
                "Please wait ...", "Processing Image ...");
        ringProgressDialog.setCancelable(false);
        Thread filterThread = new Thread(new Runnable() {
            @Override
            public void run() {
                String TAG = "launcherDialogTag";
                try {
                    // Here you should write your time consuming task...
                    float sigmaSpatial = mSeekBarSpatial.getProgress();
                    float sigmaIntensity = mSeekBarIntensity.getProgress();
                    float alpha = mSeekBarAlpha.getProgress();
                    float beta = mSeekBarBeta.getProgress();
                    mBitmap = Util.getBitmap(StillsActivity.this, mURI);
                    Utils.bitmapToMat(mBitmap, mImToProcess);
                    Imgproc.cvtColor(mImToProcess, mImGray,
                            Imgproc.COLOR_RGBA2GRAY);
                    filterImage(id, mImToProcess, mImGray, mFilteredImage,
                            sigmaSpatial, sigmaIntensity, alpha, beta);
                    mBitmap = Bitmap.createBitmap(mImToProcess.cols(),
                            mImToProcess.rows(), Bitmap.Config.RGB_565);
                    Utils.matToBitmap(mImToProcess, mBitmap);
                    //Since a View can only be updated by the thread that created it, we use the "post" method, to tell the UI
                    //thread to update the ImageView after the other
                    //thread ended
                    mImageView.post(new Runnable() {
                        public void run() {
                            mImageView.setImageBitmap(Util.getResizedBitmap(mBitmap,
                                    1000));
                        }
                    });
                    Log.i(TAG, "filter finished");
                } catch (Exception e) {
                    Log.e(TAG, e.getMessage(), e);
                }
                ringProgressDialog.dismiss();
            }
        });
        filterThread.start();
    }


}
