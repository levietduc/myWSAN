package nl.ps.mywsan;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

public class AggregationFragment extends Fragment {
    public static final String TAG = "AboutFragment";
    private static final int UART_PROFILE_READY = 10;
    private static final int UART_PROFILE_CONNECTED = 20;
    private static final int UART_PROFILE_DISCONNECTED = 21;
    private deviceViewModel viewModel;
    private TextView deviceInfo;
    private TextView deviceType;
    private LedButtonService mService = null;
    private BluetoothDevice mDevice = null;
    private BleLinkManager mBleLinkManager;

    //UART service connected/disconnected
    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder rawBinder) {
            Log.d(TAG, rawBinder.toString());
            mService = ((LedButtonService.LocalBinder) rawBinder).getService();
            Log.d(TAG, "onServiceConnected mService= " + mService);
            mBleLinkManager.setOutgoingService(mService);
            if (!mService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
//                finish();
            }
        }

        public void onServiceDisconnected(ComponentName classname) {
            mBleLinkManager.setOutgoingService(null);
            mService = null;
        }
    };
    ListView mBleDeviceListView;
    private BluetoothAdapter mBluetoothAdapter;
    private Button btnConnectDisconnect;
    private TextView mStatusText;
    private int mState = UART_PROFILE_DISCONNECTED;
    private ProgressDialog mConProgDialog;
    private final BroadcastReceiver UARTStatusChangeReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            final Intent mIntent = intent;
            //*********************//
            if (action.equals(LedButtonService.ACTION_GATT_CONNECTED)) {
                getActivity().runOnUiThread(new Runnable() {
                    public void run() {
//                        String currentDateTimeString = DateFormat.getTimeInstance().format(new Date());
                        Log.d(TAG, "UART_CONNECT_MSG");
                        btnConnectDisconnect.setText("Disconnect");
//                        mBtnDisconnectAllPeripherals.setEnabled(true);
//                        mBtnSelNone.setEnabled(true);
                        mStatusText.setEnabled(true);
//                        mBtnLedOnOff.setEnabled(true);
//                        mSeekBarHue.setEnabled(true);
//                        mSeekBarIntensity.setEnabled(true);
                        mConProgDialog.hide();
//                        writeToLog("Connected", AppLogFontType.APP_NORMAL);
                    }
                });
            }

            //*********************//
            if (action.equals(LedButtonService.ACTION_GATT_DISCONNECTED)) {
                getActivity().runOnUiThread(new Runnable() {
                    public void run() {
//                        String currentDateTimeString = DateFormat.getTimeInstance().format(new Date());
                        Log.d(TAG, "UART_DISCONNECT_MSG");
                        btnConnectDisconnect.setText("Connect");
//                        mBtnDisconnectAllPeripherals.setEnabled(false);
//                        mBtnSelNone.setEnabled(false);
                        mStatusText.setEnabled(false);
//                        mBtnLedOnOff.setEnabled(false);
//                        mSeekBarHue.setEnabled(false);
//                        mSeekBarIntensity.setEnabled(false);
//                        mStatusText.setText("Connected Devices: 0");
//                        writeToLog("Disconnected", AppLogFontType.APP_NORMAL);
                        mState = UART_PROFILE_DISCONNECTED;
                        mService.close();
                        mConProgDialog.hide();
                        mBleLinkManager.clearBleDevices();
                    }
                });
            }

            //*********************//
            if (action.equals(LedButtonService.ACTION_GATT_SERVICES_DISCOVERED)) {
                mService.enableTXNotification();
                //mRgbLedButton.setEnabled(true);
            }
            //*********************//
            if (action.equals(LedButtonService.ACTION_DATA_AVAILABLE)) {

                final byte[] txValue = intent.getByteArrayExtra(LedButtonService.EXTRA_DATA);
                getActivity().runOnUiThread(new Runnable() {
                    public void run() {
                        try {
                            mBleLinkManager.processBlePacket(txValue);
                        } catch (Exception e) {
                            Log.e(TAG, e.toString());
                        }
                    }
                });
            }
            //*********************//
            if (action.equals(LedButtonService.DEVICE_DOES_NOT_SUPPORT_LEDBUTTON)) {
                //showMessage("Device doesn't support UART. Disconnecting");
//                writeToLog("APP: Invalid BLE service, disconnecting!",  AppLogFontType.APP_ERROR);
                mService.disconnect();
            }
        }
    };


    private void service_init() {
        Intent bindIntent = new Intent(getContext(), LedButtonService.class);
        getActivity().bindService(bindIntent, mServiceConnection, Context.BIND_AUTO_CREATE);
//        Log.d(TAG, mService.toString());
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(UARTStatusChangeReceiver, makeGattUpdateIntentFilter());
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(LedButtonService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(LedButtonService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(LedButtonService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(LedButtonService.ACTION_DATA_AVAILABLE);
        intentFilter.addAction(LedButtonService.DEVICE_DOES_NOT_SUPPORT_LEDBUTTON);
        return intentFilter;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mConProgDialog = new ProgressDialog(getContext());
        mConProgDialog.setTitle("Connecting...");
        mConProgDialog.setCancelable(false);

        service_init();


        viewModel = ViewModelProviders.of(this.getActivity()).get(deviceViewModel.class);

        viewModel.getSelectedDevice().observe(this, device -> {
            mDevice = device;
            displayDetails(device);
//            mDevice = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(device.getAddress());
            Log.d(TAG, "... getSelectedDevice.address==" + mDevice + ", mserviceValue=" + mService);
            //((TextView) findViewById(R.id.deviceName)).setText(mDevice.getName()+ " - connecting");
//                    mService.connect(mDevice.getAddress());

//                    mConProgDialog.show();
        });

        mBleLinkManager = new BleLinkManager(getActivity().getApplicationContext());
        Log.d(TAG, "onCreate");

        mBleLinkManager.setBleLinkListener(new BleLinkManager.BleLinkListener() {
            @Override
            public void onListChanged() {
                mStatusText.setText("Connected Devices: " + mBleLinkManager.getNumberOfLinks());
            }
        });
    }

    public void displayDetails(BluetoothDevice device) {
//        deviceInfo.setText(device.getName() + " MAC = " + device.getAddress());
//        deviceType.setText(device.getAddress());
//        age.setText(""+player.getAge());
//        country.setText(player.getCountry());
//        titles.setText(""+player.getTitles());
//        rank.setText(""+player.getRank());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup viewGroup, Bundle savedInstanceState) {


        View view = inflater.inflate(R.layout.aggregationlayout, viewGroup, false);

//        deviceInfo = view.findViewById(R.id.deviceAddress);
//        deviceType = view.findViewById(R.id.deviceType);
        mBleDeviceListView = view.findViewById(R.id.listViewBleDevice);
        mBleDeviceListView = view.findViewById(R.id.listViewBleDevice);
        mBleDeviceListView.setAdapter(mBleLinkManager.getListAdapter());
        mBleDeviceListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        mBleDeviceListView.setItemsCanFocus(false);
        mBleDeviceListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                mBleLinkManager.itemClicked(i);
            }
        });
        mStatusText = view.findViewById(R.id.textViewStatus);

        btnConnectDisconnect = view.findViewById(R.id.btn_connect);
        btnConnectDisconnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });

        // Handler Disconnect & Connect button
        btnConnectDisconnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                if (!mBluetoothAdapter.isEnabled()) {
                    Log.i(TAG, "onClick - BT not enabled yet");
                } else {
                    if (btnConnectDisconnect.getText().equals("Connect")) {
                        Log.d(TAG, "... btnConnectDisconnect.address==" + mDevice + "mserviceValue" + mService);
                        if (mService != null) {
                            mService.connect(mDevice.getAddress());

                            mConProgDialog.show();
                        }
                    } else {
                        //Disconnect button pressed
                        if (mDevice != null) {
                            mBleLinkManager.disconnectCentral();
                            //mService.disconnect();
                        }
                    }
                }
            }
        });

//        service_init();
        return view;
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy()");

        try {
            LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(UARTStatusChangeReceiver);
        } catch (Exception ignore) {
            Log.e(TAG, ignore.toString());
        }
        getActivity().unbindService(mServiceConnection);
        mService.stopSelf();
        mService = null;

    }


}
