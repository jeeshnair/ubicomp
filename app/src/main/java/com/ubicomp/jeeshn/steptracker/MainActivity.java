package com.ubicomp.jeeshn.steptracker;

import android.content.Context;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    SensorManager mSensorManager;
    Sensor mAccelerometer;
    Sensor mStepCounter;

    GraphView rawGraph = null;
    GraphView smoothGraph = null;
    LineGraphSeries<DataPoint> rawXSeries;
    LineGraphSeries<DataPoint> rawYSeries;
    LineGraphSeries<DataPoint> rawZSeries;
    LineGraphSeries<DataPoint> rawSeries;
    LineGraphSeries<DataPoint> smoothXSeries;
    LineGraphSeries<DataPoint> smoothYSeries;
    LineGraphSeries<DataPoint> smoothZSeries;
    LineGraphSeries<DataPoint> smoothSeries;

    // Increasing the size of the smoothing window will increasingly smooth the accel signal; however,
    // at a cost of responsiveness. Play around with different window sizes: 20, 50, 100...
    // Note that I've implemented a simple Mean Filter smoothing algorithm
    static int SMOOTHING_WINDOW_SIZE = 20;

    float[] gravity = {0, 0, 0};

    int readingCount = 0;
    int stepCount = 0;
    int calculatedStepCount = 0;
    int initialCounterValue = 0;

    TextView txtStepCount;
    TextView txtCalculatedStepCount;
    int StepDetected = 1;
    int Idle = 0;
    int state;
    int previousState;

    long streakStartTime;
    long streakPrevTime;

    private float rawAccelValues[] = new float[3];

    // smoothing accelerometer signal stuff
    private float accelValueHistory[][] = new float[3][SMOOTHING_WINDOW_SIZE];
    private float runningAccelTotal[] = new float[3];
    private float curAccelAvg[] = new float[3];
    private int curReadIndex = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        txtStepCount = findViewById((R.id.txtStepCount));
        txtCalculatedStepCount = findViewById((R.id.txtCalculatedStep));

        rawGraph = findViewById(R.id.graphRaw);
        smoothGraph = findViewById(R.id.graphSmooth);

        rawXSeries = new LineGraphSeries<DataPoint>();
        rawXSeries.setColor(Color.RED);
        this.InitializeGraphControl(rawGraph,rawXSeries,20,80);
        rawYSeries = new LineGraphSeries<DataPoint>();
        rawYSeries.setColor(Color.GREEN);
        this.InitializeGraphControl(rawGraph,rawYSeries,20,80);
        rawZSeries = new LineGraphSeries<DataPoint>();
        rawZSeries.setColor(Color.YELLOW);
        this.InitializeGraphControl(rawGraph,rawZSeries,20,80);
        rawSeries = new LineGraphSeries<DataPoint>();
        rawSeries.setColor(Color.MAGENTA);
        this.InitializeGraphControl(rawGraph,rawSeries,20,80);

        smoothXSeries = new LineGraphSeries<DataPoint>();
        smoothXSeries.setColor(Color.RED);
        this.InitializeGraphControl(smoothGraph,smoothXSeries,5,80);
        smoothYSeries = new LineGraphSeries<DataPoint>();
        smoothYSeries.setColor(Color.GREEN);
        this.InitializeGraphControl(smoothGraph,smoothYSeries,5,80);
        smoothZSeries = new LineGraphSeries<DataPoint>();
        smoothZSeries.setColor(Color.YELLOW);
        this.InitializeGraphControl(smoothGraph,smoothZSeries,5,80);
        smoothSeries = new LineGraphSeries<DataPoint>();
        smoothSeries.setColor(Color.MAGENTA);
        this.InitializeGraphControl(smoothGraph,smoothSeries,5,80);

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mStepCounter  = mSensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        StartSensor();
    }

    void InitializeGraphControl(
            GraphView graph, LineGraphSeries<DataPoint> series, double maxY, double maxX)
    {
        graph.getViewport().setYAxisBoundsManual(true);
        graph.getViewport().setMinY(0);
        graph.getViewport().setMaxY(maxY);

        graph.getViewport().setXAxisBoundsManual(true);
        graph.getViewport().setMinX(0);
        graph.getViewport().setMaxX(maxX);

        graph.getViewport().setScalable(true);
        graph.getViewport().setScalableY(true);

        graph.addSeries(series);
    }

    private void StartSensor() {
        mSensorManager.registerListener(
                this,
                mAccelerometer,
                SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(
                this,
                mStepCounter,
                SensorManager.SENSOR_DELAY_FASTEST);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        switch (event.sensor.getType()) {
            case Sensor.TYPE_ACCELEROMETER:
                this.InferStep(event);
                break;
            case Sensor.TYPE_STEP_COUNTER:
                this.IncrementStepCount(event);
                break;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    protected void onResume() {
        super.onResume();
        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(
                this,
                mStepCounter,
                SensorManager.SENSOR_DELAY_NORMAL);
    }

    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);
    }

    // https://developer.android.com/guide/topics/sensors/sensors_motion.html
    float[] IsolateGravity(float[] sensorValues)
    {
        final float alpha = 0.8f;
        float[] acceleration = {0,0,0};
        gravity[0] = alpha * gravity[0] + (1 - alpha) * sensorValues[0];
        gravity[1] = alpha * gravity[1] + (1 - alpha) *sensorValues[1];
        gravity[2] = alpha * gravity[2] + (1 - alpha) * sensorValues[2];

        // Remove the gravity contribution with the high-pass filter.
        acceleration[0] = sensorValues[0] - gravity[0];
        acceleration[1] = sensorValues[1] - gravity[1];
        acceleration[2] = sensorValues[2] - gravity[2];

        return acceleration;
    }

    void InferStep(SensorEvent event) {

        try {
            double rawd = Math.sqrt(
                    event.values[0] * event.values[0] +
                            event.values[1] * event.values[1] +
                            event.values[2] * event.values[2]);

            readingCount = readingCount + 1;

            this.PlotGraph(rawd, readingCount, rawSeries);
            this.PlotGraph(event.values[0], readingCount, rawXSeries);
            this.PlotGraph(event.values[1], readingCount, rawYSeries);
            this.PlotGraph(event.values[2], readingCount, rawZSeries);

            rawAccelValues[0] = event.values[0];
            rawAccelValues[1] = event.values[1];
            rawAccelValues[2] = event.values[2];

            // Smoothing algorithm copied from
            // https://github.com/jonfroehlich/CSE590Sp2018/blob/master/L01-ReadAndVisAccel/app/src/main/java/makeabilitylab/l01_readandvisaccel/AccelView.java
            for (int i = 0; i < 3; i++) {
                runningAccelTotal[i] = runningAccelTotal[i] - accelValueHistory[i][curReadIndex];
                accelValueHistory[i][curReadIndex] = rawAccelValues[i];
                runningAccelTotal[i] = runningAccelTotal[i] + accelValueHistory[i][curReadIndex];
                curAccelAvg[i] = runningAccelTotal[i] / SMOOTHING_WINDOW_SIZE;
            }

            curReadIndex++;
            if (curReadIndex >= SMOOTHING_WINDOW_SIZE) {
                curReadIndex = 0;
            }

            curAccelAvg = IsolateGravity(curAccelAvg);
            double smoothd = Math.sqrt(
                    curAccelAvg[0] * curAccelAvg[0] +
                            curAccelAvg[1] * curAccelAvg[1] +
                            curAccelAvg[2] * curAccelAvg[2]);

            this.PlotGraph(smoothd, readingCount, smoothSeries);
            this.PlotGraph(curAccelAvg[0], readingCount, smoothXSeries);
            this.PlotGraph(curAccelAvg[1], readingCount, smoothYSeries);
            this.PlotGraph(curAccelAvg[2], readingCount, smoothZSeries);

            if (smoothd >= 1.5) {
                this.state = this.StepDetected;
                if (this.previousState != this.state) {
                    streakStartTime = System.currentTimeMillis();
                    if ((streakStartTime - streakPrevTime) <= 500f) {
                        streakPrevTime = System.currentTimeMillis();
                        return;
                    }
                    streakPrevTime = streakStartTime;
                    calculatedStepCount = calculatedStepCount + 1;
                    txtCalculatedStepCount.setText((String.valueOf(calculatedStepCount)));
                }
            } else {
                this.state = this.Idle;
                this.previousState = this.state;
            }
        }
        catch(Exception ex)
        {
            Log.e("Ex",ex.getMessage());
        }
    }

    void PlotGraph(double reading, int readingCount,  LineGraphSeries<DataPoint> series)
    {
        series.appendData(new DataPoint(readingCount,reading), true,20000);
    }

    void IncrementStepCount(SensorEvent event)
    {
        if(initialCounterValue==0)
        {
            initialCounterValue = (int) event.values[0];
        }
        if(event.values[0]>0) {
            stepCount = (int)event.values[0] - initialCounterValue;
            txtStepCount.setText(String.valueOf(stepCount));
        }
    }
}
