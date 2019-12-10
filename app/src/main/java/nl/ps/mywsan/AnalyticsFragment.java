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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;

import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;

public class AnalyticsFragment extends Fragment {

    public static final String TAG = "AnalyticsFragment";

    private static final int REQUEST_ENABLE_BT = 1021;
    private static final int MAX_VISISBLE_GRAPH_ENTRIES = 300;
    private TextView mTemperatureView;
    private TextView mPressureView;
    private TextView mHumidityView;
    private TextView mCarbon;
    private TextView mTvoc;
    private TextView mColorView;
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
//            prepareTemperatureMultiLines();
            Log.d(TAG, "current number of checked nodes: " + checkedNodeList.size());
            for (int i = 0; i < checkedNodeList.size(); i++) {
                // always remove the first one, index=0, as after removing one dataset, the other datasets will be shifted
                Boolean isDataSetRemoved = mLineChartTemperature.getData().removeDataSet(0);
                Log.d(TAG, "mLineChartTemperature.getData().removeDataSet(" + i + ") = " + isDataSetRemoved);
                // must notify the chart
                mLineChartTemperature.notifyDataSetChanged();
                mLineChartTemperature.invalidate(); // refresh the chart
            }
            Log.d(TAG, "getSelectedDevices().observe::mLineChartTemperature.getData().getDataSets()" + mLineChartTemperature.getData().getDataSets());
            // Reset the graph
            Log.d(TAG, "preparing for a new set checked nodes of " + checkedNodes.size());
            checkedNodeList = checkedNodes;
            Log.d(TAG, "... getSelectedDevices =" + checkedNodeList.size());
            prepareTemperatureMultiLines();
            Log.d(TAG, "getSelectedDevices().observe::mLineChartTemperature.getData().getDataSets()" + mLineChartTemperature.getData().getDataSets());


        });

        mNodeLinkManager.setNodeLinkListener(new NodeLinkManager.NodeLinkListener() {
            @Override
            public void onListChanged() {
                Log.d(TAG, "New event from nodes: " + mNodeLinkManager.getNumberOfLinks());
            }
        });

//        plotSavedTemperatureEntry();
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

        mTemperatureView = rootView.findViewById(R.id.temperature);
        mPressureView = rootView.findViewById(R.id.pressure);
        mHumidityView = rootView.findViewById(R.id.humidity);
        mCarbon = rootView.findViewById(R.id.carbon);
        mTvoc = rootView.findViewById(R.id.tvoc);
        mColorView = rootView.findViewById(R.id.color);
        btn_Settings = rootView.findViewById(R.id.weather_settings);

        mLineChartTemperature = rootView.findViewById(R.id.line_chart_temperature);
        mLineChartPressure = rootView.findViewById(R.id.line_chart_pressure);
        mLineChartHumidity = rootView.findViewById(R.id.line_chart_humidity);

//        prepareTemperatureGraph();
        preparePressureGraph();
        prepareHumidityGraph();

        prepareTemperatureMultiLines();

//        plotSavedTemperatureEntry();

        // use a timer to update graph
        Thread t = new Thread() {

            @Override
            public void run() {
                try {
                    while (!isInterrupted()) {
                        Thread.sleep(10000);
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
//                                    plotSavedTemperatureData();
//                                    plotSavedTemperatureEntry();
//                                    plotSavedTemperatureMultiLines();
//                                    plotSavedTemperatureMultiEntries();
//                                    prepareTemperatureMultiLines();
//                                    prepareTemperatureMultiLines_V2();

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

    // Prepare temperature graphs
    private void prepareTemperatureGraph() {
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
        /*final ChartMarker marker = new ChartMarker(getContext(), R.layout.marker_layout_temperature);
        mLineChartTemperature.setMarkerView(marker);*/

        LineData data = new LineData();
        data.setValueFormatter(new TemperatureChartValueFormatter());
        data.setValueTextColor(Color.WHITE);
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
    }

    // Create temperature datasets
    private LineDataSet createTemperatureDataSet() {
        LineDataSet lineDataSet = new LineDataSet(null, getString(R.string.temperature_graph));
        lineDataSet.setAxisDependency(YAxis.AxisDependency.LEFT);
        lineDataSet.setColor(ContextCompat.getColor(requireContext(), R.color.red));
        lineDataSet.setFillColor(ContextCompat.getColor(requireContext(), R.color.accent));
        lineDataSet.setHighLightColor(ContextCompat.getColor(requireContext(), R.color.accent));
        lineDataSet.setValueFormatter(new TemperatureChartValueFormatter());
        lineDataSet.setDrawValues(true);
        lineDataSet.setDrawCircles(true);
        lineDataSet.setDrawCircleHole(false);
        lineDataSet.setValueTextSize(Utils.CHART_VALUE_TEXT_SIZE);
        lineDataSet.setLineWidth(Utils.CHART_LINE_WIDTH);
        return lineDataSet;
    }

    // Create customed temperature datasets
    private LineDataSet createCustomedTemperatureDataSet(String nodeName, int lineColor) {
        LineDataSet lineDataSet = new LineDataSet(null, nodeName);
        lineDataSet.setAxisDependency(YAxis.AxisDependency.LEFT);
        switch (lineColor) {
            case 0:
                lineDataSet.setColor(ContextCompat.getColor(requireContext(), R.color.red));
                break;
            case 1:
                lineDataSet.setColor(ContextCompat.getColor(requireContext(), R.color.orange));
                break;
            case 2:
                lineDataSet.setColor(ContextCompat.getColor(requireContext(), R.color.yellow));
                break;
            case 3:
                lineDataSet.setColor(ContextCompat.getColor(requireContext(), R.color.blue));
                break;
            case 4:
                lineDataSet.setColor(ContextCompat.getColor(requireContext(), R.color.green));
                break;
            case 5:
                lineDataSet.setColor(ContextCompat.getColor(requireContext(), R.color.purple));
                break;
            case 6:
                lineDataSet.setColor(ContextCompat.getColor(requireContext(), R.color.grey));
                break;
            case 7:
                lineDataSet.setColor(ContextCompat.getColor(requireContext(), R.color.brown));
                break;
            case 8:
                lineDataSet.setColor(ContextCompat.getColor(requireContext(), R.color.black));
                break;
            default:
                lineDataSet.setColor(ContextCompat.getColor(requireContext(), R.color.black));
                break;
        }
//        lineDataSet.setColor(ContextCompat.getColor(requireContext(), R.color.red));
        lineDataSet.setFillColor(ContextCompat.getColor(requireContext(), R.color.accent));
        lineDataSet.setHighLightColor(ContextCompat.getColor(requireContext(), R.color.accent));
        lineDataSet.setValueFormatter(new TemperatureChartValueFormatter());
        lineDataSet.setDrawValues(true);
        lineDataSet.setDrawCircles(true);
        lineDataSet.setDrawCircleHole(false);
        lineDataSet.setValueTextSize(Utils.CHART_VALUE_TEXT_SIZE);
        lineDataSet.setLineWidth(Utils.CHART_LINE_WIDTH);
        return lineDataSet;
    }

    // Add temperature data entry
    private void addTemperatureEntry(final String timeStamp, final float temperatureValue) {
        final LineData data = mLineChartTemperature.getData();

        if (data != null) {
            ILineDataSet set = data.getDataSetByIndex(0);

            if (set == null) {
                set = createTemperatureDataSet();
                data.addDataSet(set);
            }
            data.addXValue(timeStamp);
            final Entry entry = new Entry(temperatureValue, set.getEntryCount());
            data.addEntry(entry, 0);
            final YAxis leftAxis = mLineChartTemperature.getAxisLeft();

            if (temperatureValue > leftAxis.getAxisMaximum()) {
                leftAxis.setAxisMaxValue(leftAxis.getAxisMaximum() + 20f);
            } else if (temperatureValue < leftAxis.getAxisMinimum()) {
                leftAxis.setAxisMinValue(leftAxis.getAxisMinimum() - 20f);
            }

            mLineChartTemperature.notifyDataSetChanged();
            mLineChartTemperature.setVisibleXRangeMaximum(10);

            if (data.getXValCount() >= 10) {
                final int highestVisibleIndex = mLineChartTemperature.getHighestVisibleXIndex();
                if ((data.getXValCount() - 10) < highestVisibleIndex) {
                    mLineChartTemperature.moveViewToX(data.getXValCount() - 11);
                } else {
                    mLineChartTemperature.invalidate();
                }
            } else {
                mLineChartTemperature.invalidate();
            }
        }
    }

    // Add temperature data with multi entries, each entry belongs to a checked node
    private void addTemperatureMultiEntries(final String timeStamp, final HashMap<String, Float> temperatureValues) {
        final LineData data = mLineChartTemperature.getData();
        data.getDataSetCount();
        if (data != null) {
//            List<ILineDataSet> sets = data.getDataSets();

//            if (sets == null) {
//                sets = new ArrayList<>();
//            }
            for (int i = 0; i < temperatureValues.size(); i++) {
                ILineDataSet set = data.getDataSetByIndex(i);
                String nodeName = temperatureValues.keySet().toArray()[i].toString();
                if (set == null) {
                    set = createCustomedTemperatureDataSet(nodeName, i);
                    data.addDataSet(set);
//                    sets.add(set);
                }

                data.addXValue(timeStamp);
                final float temperature = (float) temperatureValues.values().toArray()[i];
                final Entry entry = new Entry(temperature, set.getEntryCount());
                data.addEntry(entry, i);
                final YAxis leftAxis = mLineChartTemperature.getAxisLeft();

                if (temperature > leftAxis.getAxisMaximum()) {
                    leftAxis.setAxisMaxValue(leftAxis.getAxisMaximum() + 20f);
                } else if (temperature < leftAxis.getAxisMinimum()) {
                    leftAxis.setAxisMinValue(leftAxis.getAxisMinimum() - 20f);
                }


            }

            mLineChartTemperature.notifyDataSetChanged();
            mLineChartTemperature.setVisibleXRangeMaximum(10);

            if (data.getXValCount() >= 10) {
                final int highestVisibleIndex = mLineChartTemperature.getHighestVisibleXIndex();
                if ((data.getXValCount() - 10) < highestVisibleIndex) {
                    mLineChartTemperature.moveViewToX(data.getXValCount() - 11);
                } else {
                    mLineChartTemperature.invalidate();
                }
            } else {
                mLineChartTemperature.invalidate();
            }
        }
    }

    // Add temperature data with multi entries, each entry belongs to a checked node
    private void addTemperatureMultiEntries_V2(final String timeStamp, final HashMap<String, Float> temperatureValues) {
        LineData data = mLineChartTemperature.getData();

        if (data == null) {
            List<ILineDataSet> sets = new ArrayList<>();

            for (int i = 0; i < temperatureValues.size(); i++) {
                String nodeName = temperatureValues.keySet().toArray()[i].toString();
                ILineDataSet set = createCustomedTemperatureDataSet(nodeName, i);
                sets.add(set);
            }
            List<String> xVals = new ArrayList<>();
            xVals.add(timeStamp);
            data = new LineData(xVals, sets);
            mLineChartTemperature.setData(data);
        }

        for (int i = 0; i < temperatureValues.size(); i++) {
            data.addXValue(timeStamp);
            String nodeName = temperatureValues.keySet().toArray()[i].toString();
            ILineDataSet set = createCustomedTemperatureDataSet(nodeName, i);
            final float temperature = (float) temperatureValues.values().toArray()[i];
            final Entry entry = new Entry(temperature, set.getEntryCount());
            data.addEntry(entry, i);
            final YAxis leftAxis = mLineChartTemperature.getAxisLeft();

            if (temperature > leftAxis.getAxisMaximum()) {
                leftAxis.setAxisMaxValue(leftAxis.getAxisMaximum() + 20f);
            } else if (temperature < leftAxis.getAxisMinimum()) {
                leftAxis.setAxisMinValue(leftAxis.getAxisMinimum() - 20f);
            }


        }

        mLineChartTemperature.notifyDataSetChanged();
        mLineChartTemperature.setVisibleXRangeMaximum(10);

        if (data.getXValCount() >= 10) {
            final int highestVisibleIndex = mLineChartTemperature.getHighestVisibleXIndex();
            if ((data.getXValCount() - 10) < highestVisibleIndex) {
                mLineChartTemperature.moveViewToX(data.getXValCount() - 11);
            } else {
                mLineChartTemperature.invalidate();
            }
        } else {
            mLineChartTemperature.invalidate();
        }
//        }
    }

    private void plotSavedTemperatureMultiEntries() {
        LinkedHashMap<String, String> temperatureData = new LinkedHashMap<String, String>();
        String timestamp = new SimpleDateFormat("yyyyMMdd_HH:mm:ss:SSS", Locale.getDefault()).format(new Date());

//        ArrayList<Node> tempCheckedNodeList = checkedNodeList;
        HashMap<String, Float> temperatureValues = new HashMap<>();
        if (checkedNodeList.size() > 0) {
            for (int i = 0; i < checkedNodeList.size(); i++) {
                Node plottingNode = checkedNodeList.get(i);
                temperatureData = db.getNodeTemperatures(plottingNode.getConnHandle(), 2);
                String timestamp_i = temperatureData.keySet().toArray()[temperatureData.size() - 1].toString();
                float temperature = (new Random().nextInt((5000 + 1000) + 1) + 0) / 100f;
                Log.d(TAG, "plotting temperature graph of " + plottingNode.getConnHandle() + " temp = " + temperature);
//                float temperature = Float.parseFloat(temperatureData.get(timestamp_i))/100f;
                temperatureValues.put(plottingNode.getName() + plottingNode.getConnHandle(), temperature);
//            }
            }
//
//            mLineChartTemperature.setData(data);

            addTemperatureMultiEntries(timestamp, temperatureValues);
//            addTemperatureMultiEntries_V2(timestamp, temperatureValues);
            final LineData data = mLineChartTemperature.getData();
            mLineChartTemperature.moveViewToX(data.getXValCount() - 11);

        }
        temperatureData.clear();
//        temperatureValues.clear();
//        tempCheckedNodeList.clear();

    }
    // Plot data

    private void plotSavedTemperatureEntry() {
        LinkedHashMap<String, String> temperatureData = new LinkedHashMap<String, String>();
        String timestamp;
        String temperature;

        ArrayList<Node> tempCheckedNodeList = checkedNodeList;
        if (tempCheckedNodeList.size() > 0) {
            Node plottingNode = tempCheckedNodeList.get(0);

            temperatureData = db.getNodeTemperatures(plottingNode.getConnHandle(), 2);
            timestamp = temperatureData.keySet().toArray()[temperatureData.size() - 1].toString();
//            if (currentTimestamp.compareTo(timestamp) < 0) {
                // new data, need to update graph
                Log.d(TAG, "plotting temperature graph");
                temperature = temperatureData.get(timestamp);
                addTemperatureEntry(timestamp, Float.valueOf(temperature));
                final LineData data = mLineChartTemperature.getData();
                mLineChartTemperature.moveViewToX(data.getXValCount() - 11);
//                currentTimestamp = timestamp;
//            }

        }
        temperatureData.clear();

    }

    // prepare mulitple lines
    // plot multiple lines
    private void prepareTemperatureMultiLines() {
        String timestamp = new SimpleDateFormat("yyyyMMdd_HH:mm:ss:SSS", Locale.getDefault()).format(new Date());

        // use the interface ILineDataSet
        LineData data = null;

//        if (data==null) {
        List<ILineDataSet> dataSets = new ArrayList<ILineDataSet>();
        for (int i = 0; i < checkedNodeList.size(); i++) {
            List<Entry> valsCompi = new ArrayList<Entry>();
            final float temperature1 = (new Random().nextInt((4000 - 1000) + 1) + 0) / 100f;
            final Entry cie1 = new Entry(temperature1, 0); // 0 == quarter 1
            valsCompi.add(cie1);
            final float temperature2 = (new Random().nextInt((4000 - 1000) + 1) + 0) / 100f;
            final Entry cie2 = new Entry(temperature2, 1); // 1 == quarter 2 ...
            valsCompi.add(cie2);
            LineDataSet setCompi = new LineDataSet(valsCompi, String.valueOf(checkedNodeList.get(i).getConnHandle()));
            setCompi.setAxisDependency(YAxis.AxisDependency.LEFT);
            dataSets.add(setCompi);
        }

        List<String> xVals = new ArrayList<>();
        xVals.add(new SimpleDateFormat("yyyyMMdd_HH:mm:ss:SSS", Locale.getDefault()).format(new Date()));
        xVals.add(new SimpleDateFormat("yyyyMMdd_HH:mm:ss:SSS", Locale.getDefault()).format(new Date()));

        data = new LineData(xVals, dataSets);
        mLineChartTemperature.setData(data);
        mLineChartTemperature.invalidate(); // refresh
        mLineChartTemperature.moveViewToX(data.getXValCount() - 11);

        if (checkedNodeList.size() > 0) {
            data = mLineChartTemperature.getData();
            Log.d(TAG, "After first 2 entries added:");
            Log.d(TAG, "data.getXValCount() = " + data.getXValCount());
            Log.d(TAG, "data.getYValCount() = " + data.getYValCount());
            Log.d(TAG, "data.getDataSets().size()= " + data.getDataSets().size());
            for (int i = 0; i < data.getDataSets().size(); i++) {
                for (int j = 0; j < data.getDataSetByIndex(i).getEntryCount(); j++) {
                    Log.d(TAG, "data.getDataSetByIndex(i).getEntriesForXIndex(j)= " + data.getDataSetByIndex(i).getEntriesForXIndex(j));
                }
            }
        }


//        }
//        }else{
//            List<ILineDataSet> dataSets = new ArrayList<ILineDataSet>();
//            for (int i = 0; i < checkedNodeList.size(); i++) {
//                List<Entry> valsCompi = new ArrayList<Entry>();
//                float temperature1 = (new Random().nextInt((5000 + 1000) + 1) + 0) / 100f;
//                Entry cie1 = new Entry(temperature1, data.getXValCount()); // 0 == quarter 1
//                valsCompi.add(cie1);
//                LineDataSet setCompi = new LineDataSet(valsCompi, String.valueOf(checkedNodeList.get(i).getConnHandle()));
//                setCompi.setAxisDependency(YAxis.AxisDependency.LEFT);
//                data.addDataSet(setCompi);
//            }
//            data.addXValue(new SimpleDateFormat("yyyyMMdd_HH:mm:ss:SSS", Locale.getDefault()).format(new Date()));
//            mLineChartTemperature.invalidate(); // refresh
//            mLineChartTemperature.moveViewToX(data.getXValCount() - 11);
//        }

    }

    // prepare mulitple lines
    // plot multiple lines
    private void prepareTemperatureMultiLines_V2() {
        String timestamp = new SimpleDateFormat("yyyyMMdd_HH:mm:ss:SSS", Locale.getDefault()).format(new Date());

        // use the interface ILineDataSet
        LineData data = mLineChartTemperature.getData();

        if (data == null) {

            List<ILineDataSet> dataSets = new ArrayList<ILineDataSet>();
            for (int i = 0; i < checkedNodeList.size(); i++) {
                List<Entry> valsCompi = new ArrayList<Entry>();
                final float temperature1 = (new Random().nextInt((4000 - 1000) + 1) + 0) / 100f;
                final Entry cie1 = new Entry(temperature1, 0); // 0 == quarter 1
                valsCompi.add(cie1);
                final float temperature2 = (new Random().nextInt((4000 - 1000) + 1) + 0) / 100f;
                final Entry cie2 = new Entry(temperature2, 1); // 1 == quarter 2 ...
                valsCompi.add(cie2);
                LineDataSet setCompi = new LineDataSet(valsCompi, String.valueOf(checkedNodeList.get(i).getConnHandle()));
                setCompi.setAxisDependency(YAxis.AxisDependency.LEFT);
                dataSets.add(setCompi);
            }

            List<String> xVals = new ArrayList<>();
            xVals.add(new SimpleDateFormat("yyyyMMdd_HH:mm:ss:SSS", Locale.getDefault()).format(new Date()));
            xVals.add(new SimpleDateFormat("yyyyMMdd_HH:mm:ss:SSS", Locale.getDefault()).format(new Date()));

            data = new LineData(xVals, dataSets);
            mLineChartTemperature.setData(data);
            mLineChartTemperature.invalidate(); // refresh
            mLineChartTemperature.moveViewToX(data.getXValCount() - 11);

            if (checkedNodeList.size() > 0) {
                data = mLineChartTemperature.getData();
                Log.d(TAG, "After first 2 entries added:");
                Log.d(TAG, "data.getXValCount() = " + data.getXValCount());
                Log.d(TAG, "data.getYValCount() = " + data.getYValCount());
                Log.d(TAG, "data.getDataSets().size()= " + data.getDataSets().size());
                Log.d(TAG, "data.getDataSetByIndex(0).getEntryCount()= " + data.getDataSetByIndex(0).getEntryCount());
                Log.d(TAG, "data.getDataSetByIndex(0).getEntriesForXIndex(1)= " + data.getDataSetByIndex(0).getEntriesForXIndex(1));
                Log.d(TAG, "data.getDataSetByIndex(1).getEntriesForXIndex(1)= " + data.getDataSetByIndex(1).getEntriesForXIndex(1));
            }
        } else {
            List<ILineDataSet> updateDataSets = new ArrayList<ILineDataSet>();
            updateDataSets = data.getDataSets();
            // debug dataSets
            if (checkedNodeList.size() > 0) {
                data = mLineChartTemperature.getData();
                Log.d(TAG, "After first 2 entries added:");
                Log.d(TAG, "data.getXValCount() = " + data.getXValCount());
                Log.d(TAG, "data.getYValCount() = " + data.getYValCount()); // return all yVals from all datasets
                Log.d(TAG, "data.getDataSets().size()= " + data.getDataSets().size());
                Log.d(TAG, "data.getDataSetByIndex(0).getEntryCount()= " + data.getDataSetByIndex(0).getEntryCount());
                Log.d(TAG, "data.getDataSetByIndex(0).getEntriesForXIndex(1)= " + data.getDataSetByIndex(0).getEntriesForXIndex(1));
                Log.d(TAG, "data.getDataSetByIndex(1).getEntriesForXIndex(1)= " + data.getDataSetByIndex(1).getEntriesForXIndex(1));
            }
            for (int i = 0; i < data.getDataSetCount(); i++) {
                final float temperature1 = (new Random().nextInt((5000 + 1000) + 1) + 0) / 100f;
                final Entry cie1 = new Entry(temperature1, data.getXValCount()); // 0 == quarter 1
                updateDataSets.get(i).addEntry(cie1);

                final YAxis leftAxis = mLineChartTemperature.getAxisLeft();

                if (temperature1 > leftAxis.getAxisMaximum()) {
                    leftAxis.setAxisMaxValue(leftAxis.getAxisMaximum() + 20f);
                } else if (temperature1 < leftAxis.getAxisMinimum()) {
                    leftAxis.setAxisMinValue(leftAxis.getAxisMinimum() - 20f);
                }
            }
            data.addXValue(new SimpleDateFormat("yyyyMMdd_HH:mm:ss:SSS", Locale.getDefault()).format(new Date()));

            mLineChartTemperature.notifyDataSetChanged();
            mLineChartTemperature.setVisibleXRangeMaximum(10);

            if (data.getXValCount() >= 10) {
                final int highestVisibleIndex = mLineChartTemperature.getHighestVisibleXIndex();
                if ((data.getXValCount() - 10) < highestVisibleIndex) {
                    mLineChartTemperature.moveViewToX(data.getXValCount() - 11);
                } else {
                    mLineChartTemperature.invalidate();
                }
            } else {
                mLineChartTemperature.invalidate();
            }
            mLineChartTemperature.moveViewToX(data.getXValCount() - 11);

            // recheck data after updating new entry
            // debug, recheck data adding
            if (checkedNodeList.size() > 0) {
                data = mLineChartTemperature.getData();
                Log.d(TAG, "After first 2 entries added:");
                Log.d(TAG, "data.getXValCount() = " + data.getXValCount());
                Log.d(TAG, "data.getYValCount() = " + data.getYValCount()); // return all yVals from all datasets
                Log.d(TAG, "data.getDataSets().size()= " + data.getDataSets().size());
                Log.d(TAG, "data.getDataSetByIndex(0).getEntryCount()= " + data.getDataSetByIndex(0).getEntryCount());
                Log.d(TAG, "data.getDataSetByIndex(0).getEntriesForXIndex(1)= " + data.getDataSetByIndex(0).getEntriesForXIndex(1));
                Log.d(TAG, "data.getDataSetByIndex(1).getEntriesForXIndex(1)= " + data.getDataSetByIndex(1).getEntriesForXIndex(1));
            }

        }

    }

    // plot multiple lines
    private void plotSavedTemperatureMultiLines() {
//        LinkedHashMap<String, String> temperatureData = new LinkedHashMap<String, String>();
        String timestamp = new SimpleDateFormat("yyyyMMdd_HH:mm:ss:SSS", Locale.getDefault()).format(new Date());
//        ArrayList<Node> tempCheckedNodeList = checkedNodeList;
//        if (tempCheckedNodeList.size() > 0) {
//            Log.d(TAG, "plotting temperature graph");
//            // set a customed dataset (lineColor) for each node
//            List<List<Entry> > valsNode = new ArrayList<>();
//            List<ILineDataSet> dataSets = new ArrayList<>();
//            for (int i=0; i<tempCheckedNodeList.size(); i++){
//                Node plottingNode = tempCheckedNodeList.get(i);
//                temperatureData = db.getNodeTemperatures(plottingNode.getConnHandle(), 2);
//                valsNode.add(new ArrayList<Entry>());
//                float temperature = (float)Integer.parseInt(temperatureData.values().toArray()[temperatureData.size() - 1].toString())/100f;
//                Entry currentTemp = new Entry(18.5f,0);
//                valsNode.get(i).add(currentTemp);
//
//                dataSets.add(createCustomedTemperatureDataSet(String.valueOf(18.5f),i));
//            }
//            List<String> xVals = new ArrayList<>();
//            xVals.add(timestamp);
//            LineData data = new LineData(xVals, dataSets);
//            mLineChartTemperature.setData(data);
//            mLineChartTemperature.invalidate(); // refresh
////            final LineData data = mLineChartTemperature.getData();
//            mLineChartTemperature.moveViewToX(data.getXValCount() - 11);
//
//
//        }
//        temperatureData.clear();

//                    List<ILineDataSet> set = data.getDataSets();
////
////            if (set == null) {
////                // create temperature datasets
////                set = new ArrayList<>();
////                for (int i=0; i<temperatureValues.size(); i++ ){
////                    set.add(createCustomedTemperatureDataSet(temperatureValues.keySet().toArray()[i].toString(),i));
////                }
////                data.addDataSet(set);
//
//            Node plottingNode = tempCheckedNodeList.get(0);
//
//            temperatureData = db.getNodeTemperatures(plottingNode.getConnHandle(), 2);
//            timestamp = temperatureData.keySet().toArray()[temperatureData.size() - 1].toString();
////            if (currentTimestamp.compareTo(timestamp) < 0) {
//            // new data, need to update graph
//            Log.d(TAG, "plotting temperature graph");
//            temperature = temperatureData.get(timestamp);
//            // Set multilines


        List<Entry> valsComp1 = new ArrayList<Entry>();
        List<Entry> valsComp2 = new ArrayList<Entry>();

//            put("13:39:59:419", "23.59");
//            put("13:40:01:427", "23.59");
//            put("13:40:03:438", "23.75");
//            put("13:40:05:418", "23.66");
//            put("13:40:07:428", "23.54");
//            put("13:40:09:438", "23.57");
//            put("13:40:11:418", "23.70");
//            put("13:40:13:429", "23.64");
//            put("13:40:15:439", "23.61");
//            put("13:40:17:419", "23.70");

        Entry c1e1 = new Entry(23f, 0); // 0 == quarter 1
        valsComp1.add(c1e1);
        Entry c1e2 = new Entry(24f, 1); // 1 == quarter 2 ...
        valsComp1.add(c1e2);
        // and so on ...

        Entry c2e1 = new Entry(28f, 0); // 0 == quarter 1
        valsComp2.add(c2e1);
        Entry c2e2 = new Entry(30f, 1); // 1 == quarter 2 ...
        valsComp2.add(c2e2);
        //...

        LineDataSet setComp1 = new LineDataSet(valsComp1, "Node 1");
        setComp1.setAxisDependency(YAxis.AxisDependency.LEFT);
        setComp1.setColor(ContextCompat.getColor(requireContext(), R.color.red));
        LineDataSet setComp2 = new LineDataSet(valsComp2, "Node 2");
        setComp2.setAxisDependency(YAxis.AxisDependency.LEFT);
        setComp2.setColor(ContextCompat.getColor(requireContext(), R.color.blue));

        // use the interface ILineDataSet
        List<ILineDataSet> dataSets = new ArrayList<ILineDataSet>();
        dataSets.add(setComp1);
        dataSets.add(setComp2);
        List<String> xVals = new ArrayList<>();
        xVals.add("419");
        xVals.add("427");

        LineData data = new LineData(xVals, dataSets);
        mLineChartTemperature.setData(data);
        mLineChartTemperature.invalidate(); // refresh
//            final LineData data = mLineChartTemperature.getData();
        mLineChartTemperature.moveViewToX(data.getXValCount() - 11);

//
//            addTemperatureEntry(timestamp, Float.valueOf(temperature));
//            final LineData data = mLineChartTemperature.getData();
//            mLineChartTemperature.moveViewToX(data.getXValCount() - 11);
//                currentTimestamp = timestamp;
//            }

//        }
//        temperatureData.clear();

    }

    private void plotSavedTemperatureData() {
        LinkedHashMap<String, String> temperatureData = new LinkedHashMap<String, String>();
//        LinkedHashMap<String, String> temperatureData = new LinkedHashMap<String, String>() {{
//            put("13:39:59:419", "23.59");
//            put("13:40:01:427", "23.59");
//            put("13:40:03:438", "23.75");
//            put("13:40:05:418", "23.66");
//            put("13:40:07:428", "23.54");
//            put("13:40:09:438", "23.57");
//            put("13:40:11:418", "23.70");
//            put("13:40:13:429", "23.64");
//            put("13:40:15:439", "23.61");
//            put("13:40:17:419", "23.70");
//        }};
        ArrayList<Node> tempCheckedNodeList = new ArrayList<>();
        tempCheckedNodeList = checkedNodeList;
        if (tempCheckedNodeList.size() > 0) {
            Node plottingNode = tempCheckedNodeList.get(0);
            int plottingNodeConnHandle = plottingNode.getConnHandle();

            temperatureData = db.getNodeTemperatures(plottingNode.getConnHandle(), 20);
        }


        if (temperatureData.size() > 0) {
            final Set<String> timeStamps = temperatureData.keySet();
            String temperature;
            for (String timeStamp : timeStamps) {
                temperature = temperatureData.get(timeStamp);
                addTemperatureEntry(timeStamp, Float.valueOf(temperature));

                final LineData data = mLineChartTemperature.getData();
                if (data != null) {
                    mLineChartTemperature.moveViewToX(data.getXValCount() - 11);
                }
            }
            timeStamps.clear();
            temperatureData.clear();
//            mTemperatureData.clear();
        }
    }

    private synchronized void handleTemperatureGraphUpdates(LineChart lineChart) {
        final LineData lineData = lineChart.getData();

        if (lineData.getXVals().size() > MAX_VISISBLE_GRAPH_ENTRIES) {
            ILineDataSet set = lineData.getDataSetByIndex(0);
            if (set != null) {
                if (set.removeFirst()) {
                    lineData.removeXValue(0);
                    final List xValues = lineData.getXVals();
                    for (int i = 0; i < xValues.size(); i++) {
                        Entry entry = set.getEntryForIndex(i);
                        if (entry != null) {
                            entry.setXIndex(i);
                            entry.setVal(entry.getVal());
                        }
                    }
                    lineData.notifyDataChanged();
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

    class TemperatureChartValueFormatter implements ValueFormatter {
        private DecimalFormat mFormat;

        TemperatureChartValueFormatter() {
            mFormat = new DecimalFormat("##,##,#0.00");
        }

        @Override
        public String getFormattedValue(float value, Entry entry, int dataSetIndex, ViewPortHandler viewPortHandler) {
            return mFormat.format(value);
        }
    }
//// end of temperature plot

    private void preparePressureGraph() {
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
        /*final ChartMarker marker = new ChartMarker(getActivity(), R.layout.marker_layout_pressure);
        mLineChartPressure.setMarkerView(marker);*/

        LineData data = new LineData();
        data.setValueFormatter(new TemperatureChartValueFormatter());
        data.setValueTextColor(Color.WHITE);
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
        leftAxis.setTextColor(Color.BLACK);
        leftAxis.setValueFormatter(new PressureChartYValueFormatter());
        leftAxis.setDrawLabels(true);
        leftAxis.setAxisMinValue(700f);
        leftAxis.setAxisMaxValue(1100f);
        leftAxis.setLabelCount(10, false); //
        YAxis rightAxis = mLineChartPressure.getAxisRight();
        rightAxis.setEnabled(false);
    }

    private void prepareHumidityGraph() {
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
        /*final ChartMarker marker = new ChartMarker(getActivity(), R.layout.marker_layout_pressure);
        mLineChartHumidity.setMarkerView(marker);*/

        final LineData data = new LineData();
        data.setValueFormatter(new TemperatureChartValueFormatter());
        data.setValueTextColor(Color.WHITE);
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
        leftAxis.setTextColor(Color.BLACK);
        leftAxis.setValueFormatter(new PressureChartYValueFormatter());
        leftAxis.setDrawLabels(true);
        leftAxis.setAxisMinValue(0f);
        leftAxis.setAxisMaxValue(100f);
        leftAxis.setLabelCount(6, false); //
        YAxis rightAxis = mLineChartHumidity.getAxisRight();
        rightAxis.setEnabled(false);
    }


    private LineDataSet createPressureDataSet() {
        LineDataSet lineDataSet = new LineDataSet(null, getString(R.string.pressure_graph));
        lineDataSet.setAxisDependency(YAxis.AxisDependency.LEFT);
        lineDataSet.setColor(ContextCompat.getColor(requireContext(), R.color.red));
        lineDataSet.setFillColor(ContextCompat.getColor(requireContext(), R.color.accent));
        lineDataSet.setHighLightColor(ContextCompat.getColor(requireContext(), R.color.accent));
        lineDataSet.setValueFormatter(new TemperatureChartValueFormatter());
        lineDataSet.setDrawValues(true);
        lineDataSet.setDrawCircles(true);
        lineDataSet.setDrawCircleHole(false);
        lineDataSet.setValueTextSize(Utils.CHART_VALUE_TEXT_SIZE);
        lineDataSet.setLineWidth(Utils.CHART_LINE_WIDTH);
        return lineDataSet;
    }

    private LineDataSet createHumidityDataSet() {
        final LineDataSet lineDataSet = new LineDataSet(null, getString(R.string.humidity_graph));
        lineDataSet.setAxisDependency(YAxis.AxisDependency.LEFT);
        lineDataSet.setColor(ContextCompat.getColor(requireContext(), R.color.red));
        lineDataSet.setFillColor(ContextCompat.getColor(requireContext(), R.color.accent));
        lineDataSet.setHighLightColor(ContextCompat.getColor(requireContext(), R.color.accent));
        lineDataSet.setValueFormatter(new HumidityChartValueFormatter());
        lineDataSet.setDrawValues(true);
        lineDataSet.setDrawCircles(true);
        lineDataSet.setDrawCircleHole(false);
        lineDataSet.setValueTextSize(Utils.CHART_VALUE_TEXT_SIZE);
        lineDataSet.setLineWidth(Utils.CHART_LINE_WIDTH);
        return lineDataSet;
    }



    private void addPressureEntry(final String timestamp, float pressureValue) {
        final LineData data = mLineChartPressure.getData();

        if (data != null) {
            ILineDataSet set = data.getDataSetByIndex(0);
            if (set == null) {
                set = createPressureDataSet();
                data.addDataSet(set);
            }

            data.addXValue(timestamp);
            data.addEntry(new Entry(pressureValue, set.getEntryCount()), 0);

            if (pressureValue < 700 && pressureValue > 600 && mLineChartPressure.getAxisLeft().getAxisMinimum() > 600) {
                mLineChartPressure.getAxisLeft().setAxisMinValue(600);
                mLineChartPressure.getAxisLeft().setZeroLineColor(ContextCompat.getColor(requireContext(), R.color.nordicBlue));
            } else if (pressureValue < 600 && pressureValue > 500 && mLineChartPressure.getAxisLeft().getAxisMinimum() > 500) {
                mLineChartPressure.getAxisLeft().setAxisMinValue(500);
            }

            mLineChartPressure.notifyDataSetChanged();
            mLineChartPressure.setVisibleXRangeMaximum(10);

            if (data.getXValCount() >= 10) {
                final int highestVisibleIndex = mLineChartPressure.getHighestVisibleXIndex();
                if ((data.getXValCount() - 10) < highestVisibleIndex) {
                    mLineChartPressure.moveViewToX(data.getXValCount() - 11);
                } else {
                    mLineChartPressure.invalidate();
                }
            } else {
                mLineChartPressure.invalidate();
            }
        }
    }

    private void addHumidityEntry(final String timestamp, float humidityValue) {
        final LineData data = mLineChartHumidity.getData();

        if (data != null) {
            ILineDataSet set = data.getDataSetByIndex(0);
            if (set == null) {
                set = createHumidityDataSet();
                data.addDataSet(set);
            }

            data.addXValue(timestamp);
            data.addEntry(new Entry(humidityValue, set.getEntryCount()), 0);

            mLineChartHumidity.notifyDataSetChanged();
            mLineChartHumidity.setVisibleXRangeMaximum(10);

            if (data.getXValCount() >= 10) {
                final int highestVisibleIndex = mLineChartHumidity.getHighestVisibleXIndex();
                if ((data.getXValCount() - 10) < highestVisibleIndex) {
                    mLineChartHumidity.moveViewToX(data.getXValCount() - 11);
                } else {
                    mLineChartHumidity.invalidate();
                }
            } else {
                mLineChartHumidity.invalidate();
            }
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