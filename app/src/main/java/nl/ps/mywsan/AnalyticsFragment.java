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
import java.util.Random;

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
//            prepareTemperatureMultiLines();
            prepareTemperatureMultiLines_V2();
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

//        prepareTemperatureMultiLines();
        prepareTemperatureMultiLines_V2();

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

                                    updateTemperatureValuesMultiLines();
//                                    updateSavedTemperatureValuesMultiLines();

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

    // prepare mulitple lines
    // plot multiple lines
    private void prepareTemperatureMultiLines() {
        String timestamp = new SimpleDateFormat("yyyyMMdd_HH:mm:ss:SSS", Locale.getDefault()).format(new Date());

        // use the interface ILineDataSet
        LineData data = null;

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

    }

    // add color and axes' titles
    private void prepareTemperatureMultiLines_V2() {

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

        String timestamp = new SimpleDateFormat("yyyyMMdd_HH:mm:ss:SSS", Locale.getDefault()).format(new Date());

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
            LineDataSet setCompi = new LineDataSet(valsCompi, String.valueOf(checkedNodeList.get(i).getConnHandle()));
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
            setCompi.setValueFormatter(new TemperatureChartValueFormatter());
            setCompi.setDrawValues(true);
            setCompi.setDrawCircles(true);
            setCompi.setDrawCircleHole(false);
            setCompi.setValueTextSize(Utils.CHART_VALUE_TEXT_SIZE);
            setCompi.setLineWidth(Utils.CHART_LINE_WIDTH);


            dataSets.add(setCompi);

        }
        temperatureData.clear();

        List<String> xVals = new ArrayList<>();
//        xVals.add(new SimpleDateFormat("yyyyMMdd_HH:mm:ss:SSS", Locale.getDefault()).format(new Date()));
        xVals.add(new SimpleDateFormat("yyyyMMdd_HH:mm:ss:SSS", Locale.getDefault()).format(new Date()));

        data = new LineData(xVals, dataSets);
        data.setValueFormatter(new TemperatureChartValueFormatter());
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
            for (int i = 0; i < data.getDataSets().size(); i++) {
                for (int j = 0; j < data.getDataSetByIndex(i).getEntryCount(); j++) {
                    Log.d(TAG, "data.getDataSetByIndex(i).getEntriesForXIndex(j)= " + data.getDataSetByIndex(i).getEntriesForXIndex(j));
                }
            }
        }

    }

    // plot udpate values for multiple lines
    private void updateTemperatureValuesMultiLines() {
        String timestamp = new SimpleDateFormat("yyyyMMdd_HH:mm:ss:SSS", Locale.getDefault()).format(new Date());

        // use the interface ILineDataSet
        LineData data = mLineChartTemperature.getData();

        if (data != null) {
            List<ILineDataSet> updateDataSets = new ArrayList<ILineDataSet>();
            updateDataSets = data.getDataSets();
            LinkedHashMap<String, String> temperatureData = new LinkedHashMap<String, String>();
            // debug dataSets
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
            for (int i = 0; i < data.getDataSetCount(); i++) {
                Node plottingNode = checkedNodeList.get(i);
                temperatureData = db.getNodeTemperatures(plottingNode.getConnHandle(), 2);
                String timestamp_i = temperatureData.keySet().toArray()[temperatureData.size() - 1].toString();
                final float temperature = Float.parseFloat(temperatureData.get(timestamp_i)) / 100f;
                // debug with random temperature values
//                final float temperature = (new Random().nextInt(((4000 - 1000) + 1)) / 100f;
                final Entry cie1 = new Entry(temperature, data.getXValCount()); // 0 == quarter 1
                updateDataSets.get(i).addEntry(cie1);

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
                Log.d(TAG, "After first the new entries added:");
                Log.d(TAG, "data.getXValCount() = " + data.getXValCount());
                Log.d(TAG, "data.getYValCount() = " + data.getYValCount());
                Log.d(TAG, "data.getDataSets().size()= " + data.getDataSets().size());
                for (int i = 0; i < data.getDataSets().size(); i++) {
                    for (int j = 0; j < data.getDataSetByIndex(i).getEntryCount(); j++) {
                        Log.d(TAG, "data.getDataSetByIndex(i).getEntriesForXIndex(j)= " + data.getDataSetByIndex(i).getEntriesForXIndex(j));
                    }
                }
            }
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