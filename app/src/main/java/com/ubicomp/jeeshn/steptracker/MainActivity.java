package com.ubicomp.jeeshn.steptracker;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private boolean mInitialized; // used for initializing sensor only once
    private SensorManager mSensorManager;
    private Sensor mAccelerometer;

    private final float NOISE = (float) 0.00001;
    double mLastX = 0;
    double mLastY = 0;
    double mLastZ = 0;
    TextView txtSpace = null;
    TextView txtHorizontal = null;
    TextView txtVertical = null;
    GraphView horizontalGraph = null;
    LineGraphSeries<DataPoint> horizontalSeries;

    int incrementCountHorizontal = 0;
    int incrementCountVertical = 0;
    int incrementCountSpace = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        txtSpace = findViewById(R.id.txtSpace);
        txtHorizontal = findViewById(R.id.txtHorizontal);
        txtVertical = findViewById(R.id.txtVertical);

        horizontalGraph = findViewById((R.id.graphHorizontal));
        horizontalSeries = new LineGraphSeries<DataPoint>();
        horizontalGraph.addSeries(horizontalSeries);

        horizontalGraph.getViewport().setScalable(true);
        horizontalGraph.getViewport().setScalableY(true);


        mInitialized = false;
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mAccelerometer = mSensorManager
                .getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        StartSensor();
    }

    private void StartSensor() {
        mSensorManager.registerListener(
                this,
                mAccelerometer,
                SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        this.CalculateMovement(event);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    protected void onResume() {
        super.onResume();
        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
    }

    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);
    }

    // Calculates the horizontal , vertical and in space movement based on Accelerometer data.
    void CalculateMovement(SensorEvent event)
    {
        // event object contains values of acceleration, read those
        double x = event.values[0];
        double y = event.values[1];
        double z = event.values[2];

        final double alpha = 0.8; // constant for our filter below

        double[] gravity = {0, 0, 0};

        // Isolate the force of gravity with the low-pass filter.
        gravity[0] = alpha * gravity[0] + (1 - alpha) * event.values[0];
        gravity[1] = alpha * gravity[1] + (1 - alpha) * event.values[1];
        gravity[2] = alpha * gravity[2] + (1 - alpha) * event.values[2];

        // Remove the gravity contribution with the high-pass filter.
        x = event.values[0] - gravity[0];
        y = event.values[1] - gravity[1];
        z = event.values[2] - gravity[2];

        if (!mInitialized) {
            // sensor is used for the first time, initialize the last read values
            mLastX = x;
            mLastY = y;
            mLastZ = z;
            mInitialized = true;
        } else {
            // sensor is already initialized, and we have previously read values.
            // take difference of past and current values and decide which
            // axis acceleration was detected by comparing values

            double deltaX = Math.abs(mLastX - x);
            double deltaY = Math.abs(mLastY - y);
            double deltaZ = Math.abs(mLastZ - z);
            if (deltaX < NOISE)
                deltaX = (float) 0.0;
            if (deltaY < NOISE)
                deltaY = (float) 0.0;
            if (deltaZ < NOISE)
                deltaZ = (float) 0.0;
            mLastX = x;
            mLastY = y;
            mLastZ = z;

            if (deltaX > deltaY) {
                incrementCountHorizontal++;
                if (incrementCountHorizontal > 0) {
                    txtHorizontal.setText(String.valueOf(incrementCountHorizontal));
                    this.PlotGraph(deltaX,incrementCountHorizontal);
                }
            } else if (deltaY > deltaX) {
                incrementCountVertical++;
                if (incrementCountVertical > 0) {
                    txtVertical.setText(String.valueOf(incrementCountVertical));
                }
            } else if ((deltaZ > deltaX) && (deltaZ > deltaY)) {
                incrementCountSpace++;
                if (incrementCountSpace > 0) {
                    txtSpace.setText(String.valueOf(incrementCountSpace));
                }
            } else {
                // no shake detected
            }
        }
    }
    void PlotGraph(double deltaX, int readingCount)
    {
       horizontalSeries.appendData(new DataPoint(readingCount,deltaX), true,20000);
    }
}
