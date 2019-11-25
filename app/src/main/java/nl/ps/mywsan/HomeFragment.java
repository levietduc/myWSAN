package nl.ps.mywsan;


import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;
import androidx.viewpager.widget.ViewPager;

public class HomeFragment extends Fragment {


    public static final String TAG = "HomeFragment";
    private static final long SCAN_PERIOD = 10000; //10 seconds
    List<BluetoothDevice> deviceList;
    Map<String, Integer> devRssiValues;

    private Button cancelButton;

    ListView mBleDeviceListView;
    private deviceViewModel viewModel;
    private ListView lv;
    private BluetoothAdapter mBluetoothAdapter;
    private boolean mScanning;
    private Handler mHandler;
    // private BluetoothAdapter mBtAdapter;
    private TextView mEmptyList;
    private DeviceAdapter deviceAdapter;
    private ServiceConnection onService = null;
    private View view;
    private BleLinkManager mBleLinkManager;
    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {

                @Override
                public void onLeScan(final BluetoothDevice device, final int rssi, final byte[] scanRecord) {
                    if (isAdded()) {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {

                                getActivity().runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        addDevice(device, rssi, scanRecord);
                                    }
                                });

                            }
                        });
                    }
                }
            };
//    private AdapterView.OnItemClickListener mDeviceClickListener = new AdapterView.OnItemClickListener() {
//
//        @Override
//        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
//            BluetoothDevice device = deviceList.get(position);
//            mBluetoothAdapter.stopLeScan(mLeScanCallback);
//            ViewPager mviewPager = getActivity().findViewById(R.id.view_pager);
//            mviewPager.setCurrentItem(1);
//        }
//    };


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Use this check to determine whether BLE is supported on the device.  Then you can
        // selectively disable BLE-related features.
        if (!getActivity().getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(getActivity(), R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            getActivity().finish();
        }

        // Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
        // BluetoothAdapter through BluetoothManager.
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getActivity().getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        // Checks if Bluetooth is supported on the device.
        if (mBluetoothAdapter == null) {
            Toast.makeText(getActivity(), R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            getActivity().finish();
            return;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup viewGroup, Bundle savedInstanceState) {
        mHandler = new Handler();
        view = inflater.inflate(R.layout.homelayout, viewGroup, false);

        cancelButton = view.findViewById(R.id.btn_cancel);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mScanning == false) {
                    scanLeDevice(true);
                    deviceList.clear();
                    deviceAdapter.notifyDataSetChanged();
                }
                else {
                    scanLeDevice(false);
                }
            }
        });

        mEmptyList = view.findViewById(R.id.empty);

        populateList();


//        mBleLinkManager = new BleLinkManager(getContext());
//        mBleDeviceListView = (ListView)view.findViewById(R.id.new_devices);
//        mBleDeviceListView.setAdapter(mBleLinkManager.getListAdapter());
//        mBleDeviceListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
//        mBleDeviceListView.setItemsCanFocus(false);
//        // Change tab when device is selected
//        mBleDeviceListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
//            @Override
//            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
//                mBleLinkManager.itemClicked(i);
//            }
//        });

        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        viewModel = ViewModelProviders.of(this.getActivity()).get(deviceViewModel.class);


        Log.d(TAG, "onActivityCreated");
        deviceList = new ArrayList<BluetoothDevice>();
        deviceAdapter = new DeviceAdapter(getContext(), deviceList);
        devRssiValues = new HashMap<String, Integer>();

        AdapterView.OnItemClickListener selectedDevice = new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                BluetoothDevice device = deviceList.get(position);

                // Send device to other fragment
                viewModel.selectDevice(device);

                mBluetoothAdapter.stopLeScan(mLeScanCallback);

                ViewPager mviewPager = getActivity().findViewById(R.id.view_pager);
                mviewPager.setCurrentItem(1);

            }
        };

        lv = view.findViewById(R.id.new_devices);
        lv.setAdapter(deviceAdapter);
        lv.setOnItemClickListener(selectedDevice);
    }

    private void scanLeDevice(final boolean enable) {
        final Button cancelButton = view.findViewById(R.id.btn_cancel);
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);

                    cancelButton.setText(R.string.scan);
                    mEmptyList.setVisibility(View.GONE);

                }
            }, SCAN_PERIOD);

            mScanning = true;
            mBluetoothAdapter.startLeScan(mLeScanCallback);
            cancelButton.setText(R.string.cancel);
            mEmptyList.setVisibility(View.VISIBLE);
        } else {
            mScanning = false;
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
            cancelButton.setText(R.string.scan);
            mEmptyList.setVisibility(View.GONE);
        }

    }

    private UUID findServiceUuidInScanRecord(byte[] scanRecord) {
        final char[] hexArray = "0123456789abcdef".toCharArray();
        for (int index = 0; (index >= 0 && index < scanRecord.length - 1); ) {
            int length = scanRecord[index];
            int header = scanRecord[index + 1];
            if (length == 0 || header == 0) {
                return null;
            }
            if (length == 17 && header == 7) {
                String stringUUID = "";
                for (int uIndex = 0; uIndex < 16; uIndex++) {
                    int hexVal = scanRecord[index + 2 + (15 - uIndex)] & 0xFF;
                    stringUUID += hexArray[hexVal / 16];
                    stringUUID += hexArray[hexVal % 16];
                    if (uIndex == 3 || uIndex == 5 || uIndex == 7 || uIndex == 9) {
                        stringUUID += "-";
                    }
                }
                UUID returnUUID = UUID.fromString(stringUUID);
                return returnUUID;
            }
            index += (length + 1);
        }
        return null;
    }

    private void addDevice(BluetoothDevice device, int rssi, byte[] scanRecord) {
        boolean deviceFound = false;

//        devRssiValues.put(device.getAddress(), rssi);
//        if (!deviceFound) {
//            deviceList.add(device);
//            mEmptyList.setVisibility(View.GONE);
//
//            deviceAdapter.notifyDataSetChanged();
//        }

        UUID serviceUUID = findServiceUuidInScanRecord(scanRecord);
        if (serviceUUID != null && serviceUUID.equals(LedButtonService.RX_SERVICE_UUID)) {
            for (BluetoothDevice listDev : deviceList) {
                if (listDev.getAddress().equals(device.getAddress())) {
                    deviceFound = true;
                    break;
                }
            }

            devRssiValues.put(device.getAddress(), rssi);
            if (!deviceFound) {
                deviceList.add(device);
                mEmptyList.setVisibility(View.GONE);

                deviceAdapter.notifyDataSetChanged();
            }
        }
    }

    private void populateList() {
        /* Initialize device list container */
        Log.d(TAG, "populateList");
        deviceList = new ArrayList<BluetoothDevice>();
        deviceAdapter = new DeviceAdapter(getContext(), deviceList);
        devRssiValues = new HashMap<String, Integer>();

        ListView newDevicesListView = view.findViewById(R.id.new_devices);
        newDevicesListView.setAdapter(deviceAdapter);
//        newDevicesListView.setOnItemClickListener(mDeviceClickListener);

        scanLeDevice(true);

    }

    class DeviceAdapter extends BaseAdapter {
        Context context;
        List<BluetoothDevice> devices;
        LayoutInflater inflater;

        public DeviceAdapter(Context context, List<BluetoothDevice> devices) {
            this.context = context;
            inflater = LayoutInflater.from(context);
            this.devices = devices;
        }

        @Override
        public int getCount() {
            return devices.size();
        }

        @Override
        public Object getItem(int position) {
            return devices.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewGroup vg;

            if (convertView != null) {
                vg = (ViewGroup) convertView;
            } else {
                vg = (ViewGroup) inflater.inflate(R.layout.device_element, null);
            }

            BluetoothDevice device = devices.get(position);
            final TextView tvadd = vg.findViewById(R.id.address);
            final TextView tvname = vg.findViewById(R.id.name);
            final TextView tvpaired = vg.findViewById(R.id.paired);
            final TextView tvrssi = vg.findViewById(R.id.rssi);

            tvrssi.setVisibility(View.VISIBLE);
            byte rssival = (byte) devRssiValues.get(device.getAddress()).intValue();
            if (rssival != 0) {
                tvrssi.setText("RSSI = " + rssival + "dB");
            }

            tvname.setText(device.getName());
            tvadd.setText(device.getAddress());
            if (device.getBondState() == BluetoothDevice.BOND_BONDED) {
                Log.i(TAG, "device::" + device.getName());
                tvname.setTextColor(Color.BLUE);
                tvadd.setTextColor(Color.BLUE);
                tvpaired.setTextColor(Color.BLUE);
                tvpaired.setVisibility(View.VISIBLE);
                tvpaired.setText(R.string.paired);
                tvrssi.setVisibility(View.VISIBLE);
                tvrssi.setTextColor(Color.BLUE);

            } else {
                tvname.setTextColor(Color.BLUE);
                tvadd.setTextColor(Color.BLUE);
                tvpaired.setVisibility(View.GONE);
                tvrssi.setVisibility(View.VISIBLE);
                tvrssi.setTextColor(Color.BLUE);
            }
            return vg;
        }
    }

}