package com.ubicomp.jeeshn.steptracker;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;

import java.util.ArrayList;

public class UserView extends Fragment {
    private OnFragmentInteractionListener mListener;

    private static String TAG = "MainActivity";

    private float[] yData = {25.3f, 10.6f, 66.76f, 44.32f, 46.01f, 16.89f, 23.9f};

    private String[] xData = {"Mitch", "Jessica" , "Mohammad" , "Kelsey", "Sam", "Robert", "Ashley"};

    PieChart pieChart;

    public UserView() {
        // Required empty public constructor
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.d(TAG, "onCreate: starting to create chart");


        pieChart =view.findViewById(R.id.progressChart);

        Description description = new Description();
        description.setText("Sales by employee (In Thousands $) ");

        pieChart.setRotationEnabled(true);

        //pieChart.setUsePercentValues(true);

        //pieChart.setHoleColor(Color.BLUE);

        //pieChart.setCenterTextColor(Color.BLACK);

        pieChart.setHoleRadius(25f);

        pieChart.setTransparentCircleAlpha(0);

        pieChart.setCenterText("Super Cool Chart");

        pieChart.setCenterTextSize(10);

        //pieChart.setDrawEntryLabels(true);

        //pieChart.setEntryLabelTextSize(20);

        //More options just check out the documentation!
        addDataSet();

        pieChart.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {
            @Override
            public void onValueSelected(Entry e, Highlight h) {
                Log.d(TAG, "onValueSelected: Value select from chart.");
                Log.d(TAG, "onValueSelected: " + e.toString());
                Log.d(TAG, "onValueSelected: " + h.toString());

                int pos1 = e.toString().indexOf("(sum): ");

                String sales = e.toString().substring(pos1 + 7);
                for(int i = 0; i < yData.length; i++){

                    if(yData[i] == Float.parseFloat(sales)){
                        pos1 = i;
                        break;
                    }
                }
                String employee = xData[pos1 + 1];
                Toast.makeText(getActivity(), "Employee " + employee + "\n" + "Sales: $" + sales + "K", Toast.LENGTH_LONG).show();
            }
            @Override
            public void onNothingSelected() {
            }
        });
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_user_view, container, false);
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
        void onFragmentInteraction(String tag);
    }

    private void addDataSet() {

        Log.d(TAG, "addDataSet started");

        ArrayList<PieEntry> yEntrys = new ArrayList<>();

        for(int i = 0; i < yData.length; i++){

            yEntrys.add(new PieEntry(yData[i] , xData[i]));

        }

        //create the data set

        PieDataSet pieDataSet = new PieDataSet(yEntrys, "Employee Sales");

        pieDataSet.setSliceSpace(2);

        pieDataSet.setValueTextSize(12);

        //add colors to dataset

        ArrayList<Integer> colors = new ArrayList<>();

        colors.add(Color.GRAY);

        colors.add(Color.BLUE);

        colors.add(Color.RED);

        colors.add(Color.GREEN);

        colors.add(Color.CYAN);

        colors.add(Color.YELLOW);

        colors.add(Color.MAGENTA);

        pieDataSet.setColors(colors);

        //add legend to chart
        Legend legend = pieChart.getLegend();

        legend.setForm(Legend.LegendForm.CIRCLE);
        legend.setVerticalAlignment(Legend.LegendVerticalAlignment.BOTTOM);
        legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);
        legend.setOrientation(Legend.LegendOrientation.HORIZONTAL);
        legend.setDrawInside(false);

        //create pie data object

        PieData pieData = new PieData(pieDataSet);

        pieChart.setData(pieData);

        pieChart.invalidate();

    }
}
