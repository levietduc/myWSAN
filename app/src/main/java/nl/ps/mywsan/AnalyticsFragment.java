package nl.ps.mywsan;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.formatter.YAxisValueFormatter;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.utils.ViewPortHandler;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;

import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;

public class AnalyticsFragment extends Fragment {

    public static final String TAG = "AnalyticsFragment";

    private static final int REQUEST_ENABLE_BT = 1021;
    private static final int MAX_VISISBLE_GRAPH_ENTRIES = 3600;
    private TextView btn_Settings;
    private LineChart mLineChartTemperature;
    private LineChart mLineChartPressure;
    private LineChart mLineChartHumidity;

    // for multiple lines on a chart
    private ILineDataSet mMultiLinesChartTemperature;

    private deviceViewModel viewModel;
    private ArrayList<Node> checkedNodeList;

    // remote node
    private NodeLinkManager mNodeLinkManager;

    // read measurement when from checked node in Aggregation Fragment
    private SQLiteDatabaseHandler db;

    private String currentTimestamp;
    private Measurement lastMeasurementinExistingDB;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mNodeLinkManager = new NodeLinkManager(getActivity().getApplicationContext());
        Log.d(TAG, "creating mNodeLinkManager");

        // database
        db = new SQLiteDatabaseHandler(getContext());

        // set current timestamp is the last entry in current database
        currentTimestamp = db.getLasTimestamp();
        if (currentTimestamp == null) {
            currentTimestamp = "20191207_02:22:31:906"; // set to the time when this code is being written
        }
        checkedNodeList = new ArrayList<>();
        viewModel = ViewModelProviders.of(this.getActivity()).get(deviceViewModel.class);

        viewModel.getSelectedDevices().observe(this, checkedNodes -> {
            Log.d(TAG, "Before clear datasets and entries:");
            Log.d(TAG, "getSelectedDevices().observe::mLineChartTemperature.getData().getDataSets()" + mLineChartTemperature.getData().getDataSets());
            Log.d(TAG, "getSelectedDevices().observe::mLineChartPressure.getData().getDataSets()" + mLineChartPressure.getData().getDataSets());
            Log.d(TAG, "getSelectedDevices().observe::mLineChartHumidity.getData().getDataSets()" + mLineChartHumidity.getData().getDataSets());
            boolean isDataSetRemoved = false;
            // CLEAR TEMPERATURE DATASETS and ENTRIES///////////////////////////////
            for (int i = 0; i < mLineChartTemperature.getData().getDataSetCount(); i++) {
                // always remove the first one, index=0, as after removing one dataset, the other datasets will be shifted
                isDataSetRemoved = mLineChartTemperature.getData().removeDataSet(0);
                Log.d(TAG, "mLineChartTemperature.getData().removeDataSet(" + i + ") = " + isDataSetRemoved);
                // must notify the chart
                mLineChartTemperature.notifyDataSetChanged();
                mLineChartTemperature.invalidate(); // refresh the chart
            }
            // CLEAR PRESSURE DATASETS and ENTRIES///////////////////////////////
            for (int i = 0; i < mLineChartPressure.getData().getDataSetCount(); i++) {
                // always remove the first one, index=0, as after removing one dataset, the other datasets will be shifted
                isDataSetRemoved = mLineChartPressure.getData().removeDataSet(0);
                Log.d(TAG, "mLineChartPressure.getData().removeDataSet(" + i + ") = " + isDataSetRemoved);
                // must notify the chart
                mLineChartPressure.notifyDataSetChanged();
                mLineChartPressure.invalidate(); // refresh the chart
            }
            // CLEAR PRESSURE DATASETS and ENTRIES///////////////////////////////
            for (int i = 0; i < mLineChartHumidity.getData().getDataSetCount(); i++) {
                // always remove the first one, index=0, as after removing one dataset, the other datasets will be shifted
                isDataSetRemoved = mLineChartHumidity.getData().removeDataSet(0);
                Log.d(TAG, "mLineChartHumidity.getData().removeDataSet(" + i + ") = " + isDataSetRemoved);
                // must notify the chart
                mLineChartHumidity.notifyDataSetChanged();
                mLineChartHumidity.invalidate(); // refresh the chart
            }
            checkedNodeList = checkedNodes;
            Log.d(TAG, "After clean datasets and entries");
            // Reset the graph
            Log.d(TAG, "prepareTemperatureMultiLines for a new set checked nodes of " + checkedNodeList.size());
            prepareTemperatureMultiLines();

            // Reset the graph
            Log.d(TAG, "preparePressureMultiLines for a new set checked nodes of " + checkedNodeList.size());
            preparePressureMultiLines();

            // Reset the graph
            Log.d(TAG, "prepareHumidityMultiLines for a new set checked nodes of " + checkedNodeList.size());
            prepareHumidityMultiLines();

        });

        mNodeLinkManager.setNodeLinkListener(new NodeLinkManager.NodeLinkListener() {
            @Override
            public void onListChanged() {
                Log.d(TAG, "New event from nodes: " + mNodeLinkManager.getNumberOfLinks());
            }
        });

    }

    public void displayDetails(Node node) {
//        deviceInfo.setText(device.getName() + " MAC = " + device.getAddress());
//        deviceType.setText(device.getAddress());
//        age.setText(""+player.getAge());
//        country.setText(player.getCountry());
//        titles.setText(""+player.getTitles());
//        rank.setText(""+player.getRank());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup viewGroup, Bundle savedInstanceState) {
        final View rootView = inflater.inflate(R.layout.fragment_environment, viewGroup, false);
        final Toolbar toolbarEnvironment = rootView.findViewById(R.id.environment_toolbar);

        btn_Settings = rootView.findViewById(R.id.weather_settings);

        mLineChartTemperature = rootView.findViewById(R.id.line_chart_temperature);
        mLineChartPressure = rootView.findViewById(R.id.line_chart_pressure);
        mLineChartHumidity = rootView.findViewById(R.id.line_chart_humidity);

        prepareTemperatureMultiLines();
        preparePressureMultiLines();
        prepareHumidityMultiLines();

        // use a timer to update graph
        Thread t = new Thread() {

            @Override
            public void run() {
                try {
                    while (!isInterrupted()) {
                        Thread.sleep(1000);
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
//
                                    handleGraphUpdates(mLineChartTemperature);
                                    updateTemperatureValuesMultiLines();

                                    handleGraphUpdates(mLineChartPressure);
                                    updatePressureValuesMultiLines();

                                    handleGraphUpdates(mLineChartHumidity);
                                    updateHumidityValuesMultiLines();
                                }
                            });
                        }
                    }
                } catch (InterruptedException e) {
                }
            }
        };

        t.start();

        return rootView;
    }

    // add color and axes' titles
    private void prepareTemperatureMultiLines() {

        // set up the chart view
        if (!mLineChartTemperature.isEmpty()) {
            mLineChartTemperature.getData().getXVals().clear();
            mLineChartTemperature.clearValues();
        }
        mLineChartTemperature.setDescription(getString(R.string.time));
        mLineChartTemperature.setTouchEnabled(true);
        mLineChartTemperature.setVisibleXRangeMinimum(5);
        // enable scaling and dragging
        mLineChartTemperature.setDragEnabled(true);
        mLineChartTemperature.setPinchZoom(true);
        mLineChartTemperature.setScaleEnabled(true);
        mLineChartTemperature.setAutoScaleMinMaxEnabled(true);
        mLineChartTemperature.setDrawGridBackground(false);
        mLineChartTemperature.setBackgroundColor(Color.WHITE);

        // use the interface ILineDataSet
        LineData data = null;

        List<ILineDataSet> dataSets = new ArrayList<ILineDataSet>();
        LinkedHashMap<String, String> temperatureData = new LinkedHashMap<String, String>();
        for (int i = 0; i < checkedNodeList.size(); i++) {
            List<Entry> valsCompi = new ArrayList<Entry>();
            Node plottingNode = checkedNodeList.get(i);
            temperatureData = db.getNodeTemperatures(plottingNode.getConnHandle(), 2);
            String timestamp_i = temperatureData.keySet().toArray()[temperatureData.size() - 1].toString();
            final float temperature = Float.parseFloat(temperatureData.get(timestamp_i)) / 100f;
//            final float temperature = (new Random().nextInt((4000 - 1000) + 1) + 0) / 100f;
            final Entry cie = new Entry(temperature, 0); // 0 == quarter 1
            valsCompi.add(cie);
            LineDataSet setCompi = new LineDataSet(valsCompi, checkedNodeList.get(i).getName() + ":" + checkedNodeList.get(i).getClusterID() + "." + checkedNodeList.get(i).getLocalNodeID());
            setCompi.setAxisDependency(YAxis.AxisDependency.LEFT);
            switch (i) {
                case 0:
                    setCompi.setColor(ContextCompat.getColor(requireContext(), R.color.red));
                    break;
                case 1:
                    setCompi.setColor(ContextCompat.getColor(requireContext(), R.color.orange));
                    break;
                case 2:
                    setCompi.setColor(ContextCompat.getColor(requireContext(), R.color.yellow));
                    break;
                case 3:
                    setCompi.setColor(ContextCompat.getColor(requireContext(), R.color.blue));
                    break;
                case 4:
                    setCompi.setColor(ContextCompat.getColor(requireContext(), R.color.green));
                    break;
                case 5:
                    setCompi.setColor(ContextCompat.getColor(requireContext(), R.color.purple));
                    break;
                case 6:
                    setCompi.setColor(ContextCompat.getColor(requireContext(), R.color.grey));
                    break;
                case 7:
                    setCompi.setColor(ContextCompat.getColor(requireContext(), R.color.brown));
                    break;
                case 8:
                    setCompi.setColor(ContextCompat.getColor(requireContext(), R.color.black));
                    break;
                default:
                    setCompi.setColor(ContextCompat.getColor(requireContext(), R.color.black));
                    break;
            }
            setCompi.setFillColor(ContextCompat.getColor(requireContext(), R.color.accent));
            setCompi.setHighLightColor(ContextCompat.getColor(requireContext(), R.color.accent));
            setCompi.setValueFormatter(new ChartXValueFormatter());
            setCompi.setDrawValues(true);
            setCompi.setDrawCircles(true);
            setCompi.setDrawCircleHole(false);
            setCompi.setValueTextSize(Utils.CHART_VALUE_TEXT_SIZE);
            setCompi.setLineWidth(Utils.CHART_LINE_WIDTH);


            dataSets.add(setCompi);

        }

        List<String> xVals = new ArrayList<>();
//        xVals.add(new SimpleDateFormat("yyyyMMdd_HH:mm:ss:SSS", Locale.getDefault()).format(new Date()));
        xVals.add(new SimpleDateFormat("HH:mm:ss:SSS", Locale.getDefault()).format(new Date()));

        data = new LineData(xVals, dataSets);
        data.setValueFormatter(new ChartXValueFormatter());
        data.setValueTextColor(Color.BLUE);
        mLineChartTemperature.setData(data);

        Legend legend = mLineChartTemperature.getLegend();
        legend.setForm(Legend.LegendForm.LINE);
        legend.setTextColor(Color.BLACK);

        XAxis xAxis = mLineChartTemperature.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setTextColor(Color.BLACK);
        xAxis.setDrawGridLines(false);
        xAxis.setAvoidFirstLastClipping(true);

        YAxis leftAxis = mLineChartTemperature.getAxisLeft();
        leftAxis.setDrawZeroLine(true);
        leftAxis.setTextColor(Color.BLACK);
        leftAxis.setValueFormatter(new TemperatureYValueFormatter());
        leftAxis.setDrawLabels(true);
        leftAxis.setAxisMinValue(-10f);
        leftAxis.setAxisMaxValue(40f);
        leftAxis.setLabelCount(6, false); //

        YAxis rightAxis = mLineChartTemperature.getAxisRight();
        rightAxis.setEnabled(false);

        mLineChartTemperature.invalidate(); // refresh
        mLineChartTemperature.moveViewToX(data.getXValCount() - 11);

        if (checkedNodeList.size() > 0) {
            data = mLineChartTemperature.getData();
            Log.d(TAG, "After first 2 entries added:");
            Log.d(TAG, "data.getXValCount() = " + data.getXValCount());
            Log.d(TAG, "data.getYValCount() = " + data.getYValCount());
            Log.d(TAG, "data.getDataSets().size()= " + data.getDataSets().size());
//            for (int i = 0; i < data.getDataSets().size(); i++) {
//                for (int j = 0; j < data.getDataSetByIndex(i).getEntryCount(); j++) {
//                    Log.d(TAG, "data.getDataSetByIndex(i).getEntriesForXIndex(j)= " + data.getDataSetByIndex(i).getEntriesForXIndex(j));
//                }
//            }
        }

        temperatureData.clear();

    }

    // plot udpate values for multiple lines
    private void updateTemperatureValuesMultiLines() {
        String timestamp = new SimpleDateFormat("HH:mm:ss:SSS", Locale.getDefault()).format(new Date());

        // use the interface ILineDataSet
        LineData data = mLineChartTemperature.getData();


        if (data != null) {
            List<ILineDataSet> updateDataSets = new ArrayList<ILineDataSet>();
            updateDataSets = data.getDataSets();
            LinkedHashMap<String, String> temperatureData = new LinkedHashMap<String, String>();
            // debug dataSets
            if (checkedNodeList.size() > 0) {
                data = mLineChartTemperature.getData();
                Log.d(TAG, "Before adding a new entry:");
                Log.d(TAG, "data.getXValCount() = " + data.getXValCount());
                Log.d(TAG, "data.getYValCount() = " + data.getYValCount());
                Log.d(TAG, "data.getDataSets().size()= " + data.getDataSets().size());
//                for (int i = 0; i < data.getDataSets().size(); i++) {
//                    for (int j = 0; j < data.getDataSetByIndex(i).getEntryCount(); j++) {
//                        Log.d(TAG, "data.getDataSetByIndex(i).getEntriesForXIndex(j)= " + data.getDataSetByIndex(i).getEntriesForXIndex(j));
//                    }
//                }

            for (int i = 0; i < data.getDataSetCount(); i++) {
                Node plottingNode = checkedNodeList.get(i);
                temperatureData = db.getNodeTemperatures(plottingNode.getConnHandle(), 2);
                String timestamp_i = temperatureData.keySet().toArray()[temperatureData.size() - 1].toString();
                final float temperature = Float.parseFloat(temperatureData.get(timestamp_i)) / 100f;
                // debug with random temperature values
//                final float temperature = (new Random().nextInt(((4000 - 1000) + 1)) / 100f;
                final Entry cie = new Entry(temperature, data.getXValCount()); // 0 == quarter 1
                updateDataSets.get(i).addEntry(cie);

                final YAxis leftAxis = mLineChartTemperature.getAxisLeft();

                if (temperature > leftAxis.getAxisMaximum()) {
                    leftAxis.setAxisMaxValue(leftAxis.getAxisMaximum() + 20f);
                } else if (temperature < leftAxis.getAxisMinimum()) {
                    leftAxis.setAxisMinValue(leftAxis.getAxisMinimum() - 20f);
                }
            }
            data.addXValue(timestamp);

            mLineChartTemperature.notifyDataSetChanged();
            mLineChartTemperature.setVisibleXRangeMaximum(10);

                Log.d(TAG, "data.getXValCount() = " + data.getXValCount());
                Log.d(TAG, "mLineChartTemperature.getHighestVisibleXIndex() = " + mLineChartTemperature.getHighestVisibleXIndex());
            if (data.getXValCount() >= 10) {
                final int highestVisibleIndex = mLineChartTemperature.getHighestVisibleXIndex();
                if ((data.getXValCount() - 10) < highestVisibleIndex) {
                    Log.d(TAG, "mLineChartTemperature.moveViewToX = " + (data.getXValCount() - 11));
                    mLineChartTemperature.moveViewToX(data.getXValCount() - 11);
                } else {
                    mLineChartTemperature.invalidate();
                }
            } else {
                mLineChartTemperature.invalidate();
            }

            // recheck data after updating new entry
            // debug, recheck data adding
//            if (checkedNodeList.size() > 0) {
//                data = mLineChartTemperature.getData();
//                Log.d(TAG, "After first the new entries added:");
//                Log.d(TAG, "data.getXValCount() = " + data.getXValCount());
//                Log.d(TAG, "data.getYValCount() = " + data.getYValCount());
//                Log.d(TAG, "data.getDataSets().size()= " + data.getDataSets().size());
//                for (int i = 0; i < data.getDataSets().size(); i++) {
//                    for (int j = 0; j < data.getDataSetByIndex(i).getEntryCount(); j++) {
//                        Log.d(TAG, "data.getDataSetByIndex(i).getEntriesForXIndex(j)= " + data.getDataSetByIndex(i).getEntriesForXIndex(j));
//                    }
//                }
//            }
            }

            temperatureData.clear(); // to keep getHighestVisibleXIndex updated so that 11 xValues are displayed
        }

    }

    //////////////////  PRESSURE GRAPH
    // add color and axes' titles
    private void preparePressureMultiLines() {
        // set up the chart view
        if (!mLineChartPressure.isEmpty()) {
            mLineChartPressure.getData().getXVals().clear();
            mLineChartPressure.clearValues();
        }
        mLineChartPressure.setDescription(getString(R.string.time));
        mLineChartPressure.setTouchEnabled(true);
        mLineChartPressure.setVisibleXRangeMinimum(5);
        // enable scaling and dragging
        mLineChartPressure.setDragEnabled(true);
        mLineChartPressure.setPinchZoom(true);
        mLineChartPressure.setScaleEnabled(true);
        mLineChartPressure.setAutoScaleMinMaxEnabled(true);
        mLineChartPressure.setDrawGridBackground(false);
        mLineChartPressure.setBackgroundColor(Color.WHITE);

        // use the interface ILineDataSet
        LineData data = null;

        List<ILineDataSet> dataSets = new ArrayList<ILineDataSet>();
        LinkedHashMap<String, String> pressureData = new LinkedHashMap<String, String>();
        for (int i = 0; i < checkedNodeList.size(); i++) {
            List<Entry> valsCompi = new ArrayList<Entry>();
            Node plottingNode = checkedNodeList.get(i);
            pressureData = db.getNodePressures(plottingNode.getConnHandle(), 2);
            String timestamp_i = pressureData.keySet().toArray()[pressureData.size() - 1].toString();
            final int pressureValue = Integer.parseInt(pressureData.get(timestamp_i));
//            final float temperature = (new Random().nextInt((4000 - 1000) + 1) + 0) / 100f;
            final Entry cie = new Entry(pressureValue, 0); // 0 == quarter 1
            valsCompi.add(cie);
            LineDataSet setCompi = new LineDataSet(valsCompi, checkedNodeList.get(i).getName() + ":" + checkedNodeList.get(i).getClusterID() + "." + checkedNodeList.get(i).getLocalNodeID());
            setCompi.setAxisDependency(YAxis.AxisDependency.LEFT);
            switch (i) {
                case 0:
                    setCompi.setColor(ContextCompat.getColor(requireContext(), R.color.red));
                    break;
                case 1:
                    setCompi.setColor(ContextCompat.getColor(requireContext(), R.color.orange));
                    break;
                case 2:
                    setCompi.setColor(ContextCompat.getColor(requireContext(), R.color.yellow));
                    break;
                case 3:
                    setCompi.setColor(ContextCompat.getColor(requireContext(), R.color.blue));
                    break;
                case 4:
                    setCompi.setColor(ContextCompat.getColor(requireContext(), R.color.green));
                    break;
                case 5:
                    setCompi.setColor(ContextCompat.getColor(requireContext(), R.color.purple));
                    break;
                case 6:
                    setCompi.setColor(ContextCompat.getColor(requireContext(), R.color.grey));
                    break;
                case 7:
                    setCompi.setColor(ContextCompat.getColor(requireContext(), R.color.brown));
                    break;
                case 8:
                    setCompi.setColor(ContextCompat.getColor(requireContext(), R.color.black));
                    break;
                default:
                    setCompi.setColor(ContextCompat.getColor(requireContext(), R.color.black));
                    break;
            }
            setCompi.setFillColor(ContextCompat.getColor(requireContext(), R.color.accent));
            setCompi.setHighLightColor(ContextCompat.getColor(requireContext(), R.color.accent));
            setCompi.setValueFormatter(new ChartXValueFormatter());
            setCompi.setDrawValues(true);
            setCompi.setDrawCircles(true);
            setCompi.setDrawCircleHole(false);
            setCompi.setValueTextSize(Utils.CHART_VALUE_TEXT_SIZE);
            setCompi.setLineWidth(Utils.CHART_LINE_WIDTH);


            dataSets.add(setCompi);

        }


        List<String> xVals = new ArrayList<>();
//        xVals.add(new SimpleDateFormat("yyyyMMdd_HH:mm:ss:SSS", Locale.getDefault()).format(new Date()));
        xVals.add(new SimpleDateFormat("HH:mm:ss:SSS", Locale.getDefault()).format(new Date()));

        data = new LineData(xVals, dataSets);
        data.setValueFormatter(new ChartXValueFormatter());
        data.setValueTextColor(Color.BLUE);
        mLineChartPressure.setData(data);

        Legend legend = mLineChartPressure.getLegend();
        legend.setForm(Legend.LegendForm.LINE);
        legend.setTextColor(Color.BLACK);

        XAxis xAxis = mLineChartPressure.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setTextColor(Color.BLACK);
        xAxis.setDrawGridLines(false);
        xAxis.setAvoidFirstLastClipping(true);

        YAxis leftAxis = mLineChartPressure.getAxisLeft();
        leftAxis.setDrawZeroLine(true);
        leftAxis.setTextColor(Color.BLACK);
        leftAxis.setValueFormatter(new PressureChartYValueFormatter());
        leftAxis.setDrawLabels(true);
        leftAxis.setAxisMinValue(700f);
        leftAxis.setAxisMaxValue(1100f);
        leftAxis.setLabelCount(10, false); //

        YAxis rightAxis = mLineChartPressure.getAxisRight();
        rightAxis.setEnabled(false);

        mLineChartPressure.invalidate(); // refresh
        mLineChartPressure.moveViewToX(data.getXValCount() - 11);

        if (checkedNodeList.size() > 0) {
            data = mLineChartPressure.getData();
            Log.d(TAG, "After first 2 entries added:");
            Log.d(TAG, "data.getXValCount() = " + data.getXValCount());
            Log.d(TAG, "data.getYValCount() = " + data.getYValCount());
            Log.d(TAG, "data.getDataSets().size()= " + data.getDataSets().size());
//            for (int i = 0; i < data.getDataSets().size(); i++) {
//                for (int j = 0; j < data.getDataSetByIndex(i).getEntryCount(); j++) {
//                    Log.d(TAG, "data.getDataSetByIndex(i).getEntriesForXIndex(j)= " + data.getDataSetByIndex(i).getEntriesForXIndex(j));
//                }
//            }
        }

        pressureData.clear();

    }

    // plot udpate values for multiple lines
    private void updatePressureValuesMultiLines() {
        String timestamp = new SimpleDateFormat("HH:mm:ss:SSS", Locale.getDefault()).format(new Date());

        // use the interface ILineDataSet
        LineData data = mLineChartPressure.getData();


        if (data != null) {
            List<ILineDataSet> updateDataSets = new ArrayList<ILineDataSet>();
            updateDataSets = data.getDataSets();
            LinkedHashMap<String, String> pressureData = new LinkedHashMap<String, String>();
            // debug dataSets
            if (checkedNodeList.size() > 0) {
                data = mLineChartPressure.getData();
                Log.d(TAG, "Before adding a new entry:");
                Log.d(TAG, "data.getXValCount() = " + data.getXValCount());
                Log.d(TAG, "data.getYValCount() = " + data.getYValCount());
                Log.d(TAG, "data.getDataSets().size()= " + data.getDataSets().size());
//                for (int i = 0; i < data.getDataSets().size(); i++) {
//                    for (int j = 0; j < data.getDataSetByIndex(i).getEntryCount(); j++) {
//                        Log.d(TAG, "data.getDataSetByIndex(i).getEntriesForXIndex(j)= " + data.getDataSetByIndex(i).getEntriesForXIndex(j));
//                    }
//                }

                for (int i = 0; i < data.getDataSetCount(); i++) {
                    Node plottingNode = checkedNodeList.get(i);
                    pressureData = db.getNodePressures(plottingNode.getConnHandle(), 2);
                    String timestamp_i = pressureData.keySet().toArray()[pressureData.size() - 1].toString();
                    final int pressureValue = Integer.parseInt(pressureData.get(timestamp_i));
                    // debug with random temperature values
//                final float temperature = (new Random().nextInt(((4000 - 1000) + 1)) / 100f;
                    final Entry cie = new Entry(pressureValue, data.getXValCount()); // 0 == quarter 1
                    updateDataSets.get(i).addEntry(cie);

                    final YAxis leftAxis = mLineChartPressure.getAxisLeft();

                    if (pressureValue < 700 && pressureValue > 600 && mLineChartPressure.getAxisLeft().getAxisMinimum() > 600) {
                        mLineChartPressure.getAxisLeft().setAxisMinValue(600);
                        mLineChartPressure.getAxisLeft().setZeroLineColor(ContextCompat.getColor(requireContext(), R.color.nordicBlue));
                    } else if (pressureValue < 600 && pressureValue > 500 && mLineChartPressure.getAxisLeft().getAxisMinimum() > 500) {
                        mLineChartPressure.getAxisLeft().setAxisMinValue(500);
                    }
                }
                data.addXValue(timestamp);

                mLineChartPressure.notifyDataSetChanged();
                mLineChartPressure.setVisibleXRangeMaximum(10);

                Log.d(TAG, "data.getXValCount() = " + data.getXValCount());
                Log.d(TAG, "mLineChartPressure.getHighestVisibleXIndex() = " + mLineChartPressure.getHighestVisibleXIndex());
                if (data.getXValCount() >= 10) {
                    final int highestVisibleIndex = mLineChartPressure.getHighestVisibleXIndex();
                    if ((data.getXValCount() - 10) < highestVisibleIndex) {
                        Log.d(TAG, "mLineChartPressure.moveViewToX = " + (data.getXValCount() - 11));
                        mLineChartPressure.moveViewToX(data.getXValCount() - 11);
                    } else {
                        mLineChartPressure.invalidate();
                    }
                } else {
                    mLineChartPressure.invalidate();
                }

                // recheck data after updating new entry
                // debug, recheck data adding
//            if (checkedNodeList.size() > 0) {
//                data = mLineChartPressure.getData();
//                Log.d(TAG, "After first the new entries added:");
//                Log.d(TAG, "data.getXValCount() = " + data.getXValCount());
//                Log.d(TAG, "data.getYValCount() = " + data.getYValCount());
//                Log.d(TAG, "data.getDataSets().size()= " + data.getDataSets().size());
//                for (int i = 0; i < data.getDataSets().size(); i++) {
//                    for (int j = 0; j < data.getDataSetByIndex(i).getEntryCount(); j++) {
//                        Log.d(TAG, "data.getDataSetByIndex(i).getEntriesForXIndex(j)= " + data.getDataSetByIndex(i).getEntriesForXIndex(j));
//                    }
//                }
//            }
            }

            pressureData.clear(); // to keep getHighestVisibleXIndex updated so that 11 xValues are displayed
        }

    }
    //////////////////

    //////////////////  HUMIDITY GRAPH
    // add color and axes' titles
    private void prepareHumidityMultiLines() {
        // set up the chart view
        if (!mLineChartHumidity.isEmpty()) {
            mLineChartHumidity.getData().getXVals().clear();
            mLineChartHumidity.clearValues();
        }
        mLineChartHumidity.setDescription(getString(R.string.time));
        mLineChartHumidity.setTouchEnabled(true);
        mLineChartHumidity.setVisibleXRangeMinimum(5);
        // enable scaling and dragging
        mLineChartHumidity.setDragEnabled(true);
        mLineChartHumidity.setPinchZoom(true);
        mLineChartHumidity.setScaleEnabled(true);
        mLineChartHumidity.setAutoScaleMinMaxEnabled(true);
        mLineChartHumidity.setDrawGridBackground(false);
        mLineChartHumidity.setBackgroundColor(Color.WHITE);

        // use the interface ILineDataSet
        LineData data = null;

        List<ILineDataSet> dataSets = new ArrayList<ILineDataSet>();
        LinkedHashMap<String, String> humidityData = new LinkedHashMap<String, String>();
        for (int i = 0; i < checkedNodeList.size(); i++) {
            List<Entry> valsCompi = new ArrayList<Entry>();
            Node plottingNode = checkedNodeList.get(i);
            humidityData = db.getNodeHumidity(plottingNode.getConnHandle(), 2);
            String timestamp_i = humidityData.keySet().toArray()[humidityData.size() - 1].toString();
            final int humidityValue = Integer.parseInt(humidityData.get(timestamp_i));
//            final float temperature = (new Random().nextInt((4000 - 1000) + 1) + 0) / 100f;
            final Entry cie = new Entry(humidityValue, 0); // 0 == quarter 1
            valsCompi.add(cie);
            LineDataSet setCompi = new LineDataSet(valsCompi, checkedNodeList.get(i).getName() + ":" + checkedNodeList.get(i).getClusterID() + "." + checkedNodeList.get(i).getLocalNodeID());
            setCompi.setAxisDependency(YAxis.AxisDependency.LEFT);
            switch (i) {
                case 0:
                    setCompi.setColor(ContextCompat.getColor(requireContext(), R.color.red));
                    break;
                case 1:
                    setCompi.setColor(ContextCompat.getColor(requireContext(), R.color.orange));
                    break;
                case 2:
                    setCompi.setColor(ContextCompat.getColor(requireContext(), R.color.yellow));
                    break;
                case 3:
                    setCompi.setColor(ContextCompat.getColor(requireContext(), R.color.blue));
                    break;
                case 4:
                    setCompi.setColor(ContextCompat.getColor(requireContext(), R.color.green));
                    break;
                case 5:
                    setCompi.setColor(ContextCompat.getColor(requireContext(), R.color.purple));
                    break;
                case 6:
                    setCompi.setColor(ContextCompat.getColor(requireContext(), R.color.grey));
                    break;
                case 7:
                    setCompi.setColor(ContextCompat.getColor(requireContext(), R.color.brown));
                    break;
                case 8:
                    setCompi.setColor(ContextCompat.getColor(requireContext(), R.color.black));
                    break;
                default:
                    setCompi.setColor(ContextCompat.getColor(requireContext(), R.color.black));
                    break;
            }
            setCompi.setFillColor(ContextCompat.getColor(requireContext(), R.color.accent));
            setCompi.setHighLightColor(ContextCompat.getColor(requireContext(), R.color.accent));
            setCompi.setValueFormatter(new ChartXValueFormatter());
            setCompi.setDrawValues(true);
            setCompi.setDrawCircles(true);
            setCompi.setDrawCircleHole(false);
            setCompi.setValueTextSize(Utils.CHART_VALUE_TEXT_SIZE);
            setCompi.setLineWidth(Utils.CHART_LINE_WIDTH);


            dataSets.add(setCompi);

        }

        List<String> xVals = new ArrayList<>();
//        xVals.add(new SimpleDateFormat("yyyyMMdd_HH:mm:ss:SSS", Locale.getDefault()).format(new Date()));
        xVals.add(new SimpleDateFormat("HH:mm:ss:SSS", Locale.getDefault()).format(new Date()));

        data = new LineData(xVals, dataSets);
        data.setValueFormatter(new ChartXValueFormatter());
        data.setValueTextColor(Color.BLUE);
        mLineChartHumidity.setData(data);

        Legend legend = mLineChartHumidity.getLegend();
        legend.setForm(Legend.LegendForm.LINE);
        legend.setTextColor(Color.BLACK);

        XAxis xAxis = mLineChartHumidity.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setTextColor(Color.BLACK);
        xAxis.setDrawGridLines(false);
        xAxis.setAvoidFirstLastClipping(true);

        YAxis leftAxis = mLineChartHumidity.getAxisLeft();
        leftAxis.setDrawZeroLine(true);
        leftAxis.setTextColor(Color.BLACK);
        leftAxis.setValueFormatter(new PressureChartYValueFormatter());
        leftAxis.setDrawLabels(true);
        leftAxis.setAxisMinValue(0);
        leftAxis.setAxisMaxValue(100);
        leftAxis.setLabelCount(6, false); //

        YAxis rightAxis = mLineChartHumidity.getAxisRight();
        rightAxis.setEnabled(false);

        mLineChartHumidity.invalidate(); // refresh
        mLineChartHumidity.moveViewToX(data.getXValCount() - 11);

        if (checkedNodeList.size() > 0) {
            data = mLineChartHumidity.getData();
            Log.d(TAG, "After first entry added:");
            Log.d(TAG, "data.getXValCount() = " + data.getXValCount());
            Log.d(TAG, "data.getYValCount() = " + data.getYValCount());
            Log.d(TAG, "data.getDataSets().size()= " + data.getDataSets().size());
            for (int i = 0; i < data.getDataSets().size(); i++) {
                for (int j = 0; j < data.getDataSetByIndex(i).getEntryCount(); j++) {
                    Log.d(TAG, "data.getDataSetByIndex(i).getEntriesForXIndex(j)= " + data.getDataSetByIndex(i).getEntriesForXIndex(j));
                }
            }
        }

        humidityData.clear();

    }

    // plot udpate values for multiple lines
    private void updateHumidityValuesMultiLines() {
        String timestamp = new SimpleDateFormat("HH:mm:ss:SSS", Locale.getDefault()).format(new Date());

        // use the interface ILineDataSet
        LineData data = mLineChartHumidity.getData();


        if (data != null) {
            List<ILineDataSet> updateDataSets = new ArrayList<ILineDataSet>();
            updateDataSets = data.getDataSets();
            LinkedHashMap<String, String> humidityData = new LinkedHashMap<String, String>();
            // debug dataSets
            if (checkedNodeList.size() > 0) {
                data = mLineChartHumidity.getData();
                Log.d(TAG, "Before adding a new entry:");
                Log.d(TAG, "data.getXValCount() = " + data.getXValCount());
                Log.d(TAG, "data.getYValCount() = " + data.getYValCount());
                Log.d(TAG, "data.getDataSets().size()= " + data.getDataSets().size());
//                for (int i = 0; i < data.getDataSets().size(); i++) {
//                    for (int j = 0; j < data.getDataSetByIndex(i).getEntryCount(); j++) {
//                        Log.d(TAG, "data.getDataSetByIndex(i).getEntriesForXIndex(j)= " + data.getDataSetByIndex(i).getEntriesForXIndex(j));
//                    }
//                }

                for (int i = 0; i < data.getDataSetCount(); i++) {
                    Node plottingNode = checkedNodeList.get(i);
                    humidityData = db.getNodeHumidity(plottingNode.getConnHandle(), 2);
                    String timestamp_i = humidityData.keySet().toArray()[humidityData.size() - 1].toString();
                    final int humidityValues = Integer.parseInt(humidityData.get(timestamp_i));
                    // debug with random temperature values
//                final float temperature = (new Random().nextInt(((4000 - 1000) + 1)) / 100f;
                    final Entry cie1 = new Entry(humidityValues, data.getXValCount()); // 0 == quarter 1
                    updateDataSets.get(i).addEntry(cie1);

                }
                data.addXValue(timestamp);

                mLineChartHumidity.notifyDataSetChanged();
                mLineChartHumidity.setVisibleXRangeMaximum(10);

                Log.d(TAG, "data.getXValCount() = " + data.getXValCount());
                Log.d(TAG, "mLineChartHumidity.getHighestVisibleXIndex() = " + mLineChartHumidity.getHighestVisibleXIndex());
                if (data.getXValCount() >= 10) {
                    final int highestVisibleIndex = mLineChartHumidity.getHighestVisibleXIndex();
                    if ((data.getXValCount() - 10) < highestVisibleIndex) {
                        Log.d(TAG, "mLineChartHumidity.moveViewToX = " + (data.getXValCount() - 11));
                        mLineChartHumidity.moveViewToX(data.getXValCount() - 11);
                    } else {
                        mLineChartHumidity.invalidate();
                    }
                } else {
                    mLineChartHumidity.invalidate();
                }

                // recheck data after updating new entry
                // debug, recheck data adding
//            if (checkedNodeList.size() > 0) {
//                data = mLineChartHumidity.getData();
//                Log.d(TAG, "After first the new entries added:");
//                Log.d(TAG, "data.getXValCount() = " + data.getXValCount());
//                Log.d(TAG, "data.getYValCount() = " + data.getYValCount());
//                Log.d(TAG, "data.getDataSets().size()= " + data.getDataSets().size());
//                for (int i = 0; i < data.getDataSets().size(); i++) {
//                    for (int j = 0; j < data.getDataSetByIndex(i).getEntryCount(); j++) {
//                        Log.d(TAG, "data.getDataSetByIndex(i).getEntriesForXIndex(j)= " + data.getDataSetByIndex(i).getEntriesForXIndex(j));
//                    }
//                }
//            }
            }

            humidityData.clear(); // to keep getHighestVisibleXIndex updated so that 11 xValues are displayed
        }

    }
    //////////////////

    private synchronized void handleGraphUpdates(LineChart lineChart) {
        final LineData lineData = lineChart.getData();

        if ((lineData.getXVals().size() > MAX_VISISBLE_GRAPH_ENTRIES) & !checkedNodeList.isEmpty()) {
            for (int i = 0; i < lineData.getDataSetCount(); i++) {
                ILineDataSet set = lineData.getDataSetByIndex(0);
                if (set != null) {
                    if (set.removeFirst()) {
                        lineData.removeXValue(0);
                        final List xValues = lineData.getXVals();
                        for (int j = 0; j < xValues.size(); j++) {
                            Entry entry = set.getEntryForIndex(j);
                            if (entry != null) {
                                entry.setXIndex(j);
                                entry.setVal(entry.getVal());
                            }
                        }
                        lineData.notifyDataChanged();
                    }
                }
            }

        }
    }

    // Data Format for Line Charts
    class TemperatureYValueFormatter implements YAxisValueFormatter {
        private DecimalFormat mFormat;

        TemperatureYValueFormatter() {
            mFormat = new DecimalFormat("##,##,#0.00");
        }

        @Override
        public String getFormattedValue(float value, YAxis yAxis) {
            return mFormat.format(value); //
        }
    }

    class ChartXValueFormatter implements ValueFormatter {
        private DecimalFormat mFormat;

        ChartXValueFormatter() {
            mFormat = new DecimalFormat("##,##,#0.00");
        }

        @Override
        public String getFormattedValue(float value, Entry entry, int dataSetIndex, ViewPortHandler viewPortHandler) {
            return mFormat.format(value);
        }
    }

    class PressureChartYValueFormatter implements YAxisValueFormatter {
        private DecimalFormat mFormat;

        PressureChartYValueFormatter() {
            mFormat = new DecimalFormat("###,##0.00");
        }

        @Override
        public String getFormattedValue(float value, YAxis yAxis) {
            return mFormat.format(value);
        }
    }

    class HumidityChartValueFormatter implements ValueFormatter {
        private DecimalFormat mFormat;

        HumidityChartValueFormatter() {
            mFormat = new DecimalFormat("##,##,#0");
        }

        @Override
        public String getFormattedValue(float value, Entry entry, int dataSetIndex, ViewPortHandler viewPortHandler) {
            return mFormat.format(value);
        }
    }
}