package nl.ps.mywsan;

import android.graphics.Color;
import android.os.Bundle;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

public class AnalyticsFragment extends Fragment {

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


        prepareTemperatureGraph();
        preparePressureGraph();
        prepareHumidityGraph();

        plotSavedTemperatureData();

        return rootView;
    }

    // Prepare graphs
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

    // Create datasets
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

    // Add data entry
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

    // Plot data

    private void plotSavedTemperatureData() {
//        LinkedHashMap<String, String> temperatureData = mListener.getSavedTemperatureData(mDevice);
        LinkedHashMap<String, String> temperatureData = new LinkedHashMap<String, String>() {{
            put("13:39:59:419", "23.59");
            put("13:40:01:427", "23.59");
            put("13:40:03:438", "23.75");
            put("13:40:05:418", "23.66");
            put("13:40:07:428", "23.54");
            put("13:40:09:438", "23.57");
            put("13:40:11:418", "23.70");
            put("13:40:13:429", "23.64");
            put("13:40:15:439", "23.61");
            put("13:40:17:419", "23.70");
        }};

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