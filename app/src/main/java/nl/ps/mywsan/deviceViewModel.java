package nl.ps.mywsan;


import android.bluetooth.BluetoothDevice;

import java.util.ArrayList;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class deviceViewModel extends ViewModel {
    /**
     * Live Data Instance
     */
    private final MutableLiveData<BluetoothDevice> selectedDevice = new MutableLiveData<>();


    public void selectDevice(BluetoothDevice device) {
        selectedDevice.setValue(device);
    }

    public MutableLiveData<BluetoothDevice> getSelectedDevice() {
        return selectedDevice;
    }

    // function to communicate between aggregation and analytics

    private final MutableLiveData<ArrayList<Node>> selectedDevices = new MutableLiveData<>();

    public void selectDevices(ArrayList<Node> deviceList) {
        selectedDevices.setValue(deviceList);
    }

    public MutableLiveData<ArrayList<Node>> getSelectedDevices() {
        return selectedDevices;
    }


}
