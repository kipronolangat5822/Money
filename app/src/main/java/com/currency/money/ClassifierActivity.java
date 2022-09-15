
package com.currency.money;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Typeface;
import android.media.ImageReader.OnImageAvailableListener;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Size;
import android.util.TypedValue;
import android.view.Display;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import com.currency.money.OverlayView.DrawCallback;
import com.currency.money.env.BorderedText;
import  com.currency.money.env.Logger;

public class ClassifierActivity extends CameraActivity implements OnImageAvailableListener {
    private static final Logger LOGGER = new Logger();
    MediaPlayer mp,mp1,mp2;
    private static final Size DESIRED_PREVIEW_SIZE = new Size(640, 480);
    private static final float TEXT_SIZE_DIP = 10;

    private Integer sensorOrientation;
    private ICognitiveServicesClassifier classifier;
    private BorderedText borderedText;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        classifier = new MSCognitiveServicesCustomVisionClassifier(this);
    }

    @Override
    public synchronized void onStop() {
        super.onStop();

        if (classifier != null) {
            classifier.close();
        }
    }

    @Override
    protected int getLayoutId() {
        return R.layout.camera_connection_fragment;
    }

    @Override
    protected Size getDesiredPreviewFrameSize() {
        return DESIRED_PREVIEW_SIZE;
    }

    @Override
    public void onPreviewSizeChosen(final Size size, final int rotation) {
        final float textSizePx = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, TEXT_SIZE_DIP, getResources().getDisplayMetrics());
        borderedText = new BorderedText(textSizePx);
        borderedText.setTypeface(Typeface.MONOSPACE);

        previewWidth = size.getWidth();
        previewHeight = size.getHeight();

        final Display display = getWindowManager().getDefaultDisplay();
        final int screenOrientation = display.getRotation();

        LOGGER.i("Sensor orientation: %d, Screen orientation: %d", rotation, screenOrientation);

        sensorOrientation = rotation + screenOrientation;

        LOGGER.i("Initializing at size %dx%d", previewWidth, previewHeight);
        rgbFrameBitmap = Bitmap.createBitmap(previewWidth, previewHeight, Config.ARGB_8888);

        yuvBytes = new byte[3][];

        addCallback(
                new DrawCallback() {
                    @Override
                    public void drawCallback(final Canvas canvas) {
                        renderDebug(canvas);
                    }
                });
        mp = MediaPlayer.create(this, R.raw.hun);
        mp1 = MediaPlayer.create(this, R.raw.ten);
        mp2 = MediaPlayer.create(this, R.raw.five);
    }

    boolean hun = false;
    boolean five = false;
    boolean ten = false;

    protected void processImageRGBbytes(int[] rgbBytes) {

        rgbFrameBitmap.setPixels(rgbBytes, 0, previewWidth, 0, 0, previewWidth, previewHeight);

        runInBackground(
                new Runnable() {
                    @Override
                    public void run() {
                        final long startTime = SystemClock.uptimeMillis();
                        com.currency.money.Classifier.Recognition r = classifier.classifyImage(rgbFrameBitmap, sensorOrientation);
                        lastProcessingTimeMs = SystemClock.uptimeMillis() - startTime;

                        final List<com.currency.money.Classifier.Recognition> results = new ArrayList<>();

                        if (r.getConfidence() > 0.99) {
                            results.add(r);
                        }

                        LOGGER.i("Detect: %s", results);
                        if (resultsView == null) {
                            resultsView=findViewById(R.id.results);
                        }
                        resultsView.setResults(results);
                    if ( !five && results.toString().contains("200")){
                        mp2.start();
                        five =true;
                        ten = false;
                        hun = false;
                        }

                        if ( !hun && results.toString().contains("100")){
                            mp.start();
                            hun = true;
                            five =false;
                            ten = false;
                        }
                        if ( !ten && results.toString().contains("50")){
                            mp1.start();
                            ten  =true;
                            five =false;
                            hun = false;
                        }
                       // if (resultsView.getText().toString().equalsIgnoreCase("[[0] 50 (97.1%)]")){
                           // Toast.makeText(ClassifierActivity.this, "hwduhejhdjn", Toast.LENGTH_LONG).show();
                       // }
                        requestRender();
                        computing = false;
                        if (postInferenceCallback != null) {
                            postInferenceCallback.run();
                        }
                    }
                });

    }

    @Override
    public void onSetDebug(boolean debug) {
    }

    private void renderDebug(final Canvas canvas) {
        if (!isDebug()) {
            return;
        }

        final Vector<String> lines = new Vector<String>();
        lines.add("Inference time: " + lastProcessingTimeMs + "ms");
        borderedText.drawLines(canvas, 10, canvas.getHeight() - 10, lines);
    }
}
