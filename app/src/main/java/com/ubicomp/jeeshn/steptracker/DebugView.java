package com.ubicomp.jeeshn.steptracker;

import android.content.Context;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import org.apache.commons.math3.stat.descriptive.SummaryStatistics;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DebugView extends Fragment implements SensorEventListener {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private OnFragmentInteractionListener mListener;

    SensorManager mSensorManager;
    Sensor mAccelerometer;
    Sensor mStepCounter;

    GraphView rawGraph = null;
    GraphView smoothGraph = null;
    GraphView peakGraph = null;
    LineGraphSeries<DataPoint> rawXSeries;
    LineGraphSeries<DataPoint> rawYSeries;
    LineGraphSeries<DataPoint> rawZSeries;
    LineGraphSeries<DataPoint> rawSeries;
    LineGraphSeries<DataPoint> smoothXSeries;
    LineGraphSeries<DataPoint> smoothYSeries;
    LineGraphSeries<DataPoint> smoothZSeries;
    LineGraphSeries<DataPoint> smoothSeries;
    LineGraphSeries<DataPoint>peakSeries;

    // Increasing the size of the smoothing window will increasingly smooth the accel signal; however,
    // at a cost of responsiveness. Play around with different window sizes: 20, 50, 100...
    // Note that I've implemented a simple Mean Filter smoothing algorithm
    static int SMOOTHING_WINDOW_SIZE = 20;

    float[] gravity = {0, 0, 0};

    int readingCount = 0;
    int peakCount = 0;
    int stepCount = 0;
    int calculatedStepCount = 0;
    int initialCounterValue = 0;

    int LAG_SIZE = 5;
    int DATA_SAMPLING_SIZE = 15;

    TextView txtStepCount;
    TextView txtCalculatedStepCount;

    float rawAccelValues[] = new float[3];

    // smoothing accelerometer signal stuff
    float accelValueHistory[][] = new float[3][SMOOTHING_WINDOW_SIZE];
    float runningAccelTotal[] = new float[3];
    float curAccelAvg[] = new float[3];
    int curReadIndex = 0;
    List<Double> zscoreCalculationValues = new ArrayList<>();

    public DebugView() {
        // Required empty public constructor
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
        series.appendData(new DataPoint(0,0), true,20000);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    // This event is triggered soon after onCreateView().
    // onViewCreated() is only called if the view returned from onCreateView() is non-null.
    // Any view setup should occur here.  E.g., view lookups and attaching view listeners.
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        txtStepCount = view.findViewById((R.id.txtStepCount));
        txtCalculatedStepCount = view.findViewById((R.id.txtCalculatedStep));

        rawGraph = view.findViewById(R.id.graphRaw);
        smoothGraph = view.findViewById(R.id.graphSmooth);
        peakGraph = view.findViewById(R.id.graphPeak);

        rawXSeries = new LineGraphSeries<DataPoint>();
        rawXSeries.setColor(Color.RED);
        this.InitializeGraphControl(rawGraph, rawXSeries, 20, 80);
        rawYSeries = new LineGraphSeries<DataPoint>();
        rawYSeries.setColor(Color.GREEN);
        this.InitializeGraphControl(rawGraph, rawYSeries, 20, 80);
        rawZSeries = new LineGraphSeries<DataPoint>();
        rawZSeries.setColor(Color.YELLOW);
        this.InitializeGraphControl(rawGraph, rawZSeries, 20, 80);
        rawSeries = new LineGraphSeries<DataPoint>();
        rawSeries.setColor(Color.MAGENTA);
        this.InitializeGraphControl(rawGraph, rawSeries, 5, 80);

        smoothXSeries = new LineGraphSeries<DataPoint>();
        smoothXSeries.setColor(Color.RED);
        this.InitializeGraphControl(smoothGraph, smoothXSeries, 5, 80);
        smoothYSeries = new LineGraphSeries<DataPoint>();
        smoothYSeries.setColor(Color.GREEN);
        this.InitializeGraphControl(smoothGraph, smoothYSeries, 5, 80);
        smoothZSeries = new LineGraphSeries<DataPoint>();
        smoothZSeries.setColor(Color.YELLOW);
        this.InitializeGraphControl(smoothGraph, smoothZSeries, 5, 80);
        smoothSeries = new LineGraphSeries<DataPoint>();
        smoothSeries.setColor(Color.MAGENTA);
        this.InitializeGraphControl(smoothGraph, smoothSeries, 5, 80);

        peakSeries = new LineGraphSeries<DataPoint>();
        this.InitializeGraphControl(peakGraph, peakSeries, 1, 80);

        mSensorManager = (SensorManager) this.getActivity().getSystemService(Context.SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mStepCounter  = mSensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);

        this.StartSensor();
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_debug_view, container, false);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
        mSensorManager.unregisterListener(this);
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

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(String tag);
    }

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

            readingCount = readingCount + 1;

            rawAccelValues[0] = event.values[0];
            rawAccelValues[1] = event.values[1];
            rawAccelValues[2] = event.values[2];

            rawAccelValues = IsolateGravity(rawAccelValues);

            double rawd = Math.sqrt(
                    rawAccelValues[0] * rawAccelValues[0] +
                            rawAccelValues[1] * rawAccelValues[1] +
                            rawAccelValues[2] * rawAccelValues[2]);

            this.PlotGraph(rawd, readingCount, rawSeries);
            this.PlotGraph(rawAccelValues[0], readingCount, rawXSeries);
            this.PlotGraph(rawAccelValues[1], readingCount, rawYSeries);
            this.PlotGraph(rawAccelValues[2], readingCount, rawZSeries);

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

            double smoothd = Math.sqrt(
                    curAccelAvg[0] * curAccelAvg[0] +
                            curAccelAvg[1] * curAccelAvg[1] +
                            curAccelAvg[2] * curAccelAvg[2]);

            this.PlotGraph(smoothd, readingCount, smoothSeries);
            this.PlotGraph(curAccelAvg[0], readingCount, smoothXSeries);
            this.PlotGraph(curAccelAvg[1], readingCount, smoothYSeries);
            this.PlotGraph(curAccelAvg[2], readingCount, smoothZSeries);

           /* if (smoothd >= 1.5) {
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
            } */

            if(zscoreCalculationValues.size()< DATA_SAMPLING_SIZE)
            {
                zscoreCalculationValues.add(rawd);
            }
            else if(zscoreCalculationValues.size()== DATA_SAMPLING_SIZE)
            {
                calculatedStepCount = calculatedStepCount + this.DetectPeak(zscoreCalculationValues,LAG_SIZE,0.30d,0d);
                txtCalculatedStepCount.setText((String.valueOf(calculatedStepCount)));
                zscoreCalculationValues.clear();
                zscoreCalculationValues.add(rawd);
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

    /**
     * "Smoothed zero-score alogrithm" shamelessly copied from https://stackoverflow.com/a/22640362/6029703
     *  Uses a rolling mean and a rolling deviation (separate) to identify peaks in a vector
     *
     * @param y - The input vector to analyze
     * @param lag - The lag of the moving window (i.e. how big the window is)
     * @param threshold - The z-score at which the algorithm signals (i.e. how many standard deviations away from the moving mean a peak (or signal) is)
     * @param influence - The influence (between 0 and 1) of new signals on the mean and standard deviation (how much a peak (or signal) should affect other values near it)
     * @return - The calculated averages (avgFilter) and deviations (stdFilter), and the signals (signals)
     */

    public int DetectPeak(List<Double> y, int lag, Double threshold, Double influence) {
        int peaksDetected = 0;
        //init stats instance
        SummaryStatistics stats = new SummaryStatistics();

        //the results (peaks, 1 or -1) of our algorithm
        ArrayList<Integer> signals = new ArrayList<Integer>(Collections.nCopies(y.size(), 0));
        //filter out the signals (peaks) from our original list (using influence arg)
        ArrayList<Double> filteredY = new ArrayList<Double>(Collections.nCopies(y.size(), 0d));
        //the current average of the rolling window
        ArrayList<Double> avgFilter = new ArrayList<Double>(Collections.nCopies(y.size(), 0.0d));
        //the current standard deviation of the rolling window
        ArrayList<Double> stdFilter = new ArrayList<Double>(Collections.nCopies(y.size(), 0.0d));
        //init avgFilter and stdFilter
        for(int i=0;i<lag;i++) {
            stats.addValue(y.get(i));
            filteredY.add(y.get(i));
        }
        avgFilter.set(lag-1,stats.getMean());
        stdFilter.set(lag-1 , stats.getStandardDeviation());
        stats.clear();

        for(int i = lag ; i<y.size();i++) {
            peakCount = peakCount+1;
            if (Math.abs(y.get(i) - avgFilter.get(i - 1)) > threshold * stdFilter.get(i - 1)) {
                //this is a signal (i.e. peak), determine if it is a positive or negative signal
                if(y.get(i) > avgFilter.get(i - 1)) {
                    signals.set(i,1);
                    if(y.get(i)>1.5) {
                        peaksDetected = peaksDetected + 1;
                        peakSeries.appendData(new DataPoint(peakCount, 1), true, 20000);
                    }
                }
                else {
                    signals.set(i,-1);
                    peakSeries.appendData(new DataPoint(peakCount,-1), true,20000);
                }
                //filter this signal out using influence
                filteredY.set(i,(influence * y.get(i)) + ((1-influence) * filteredY.get(i-1)));
            } else {
                //ensure this signal remains a zero
                signals.set(i, 0);
                peakSeries.appendData(new DataPoint(peakCount,0), true,20000);
                //ensure this value is not filtered
                filteredY.set(i, y.get(i));
            }
            //update rolling average and deviation
            for(int j=i-lag;j<i;j++)

            {
                stats.addValue(filteredY.get(j));
            }
            avgFilter.set(i,stats.getMean());
            stdFilter.set(i, stats.getStandardDeviation());

        }

        return peaksDetected;
    }
}
