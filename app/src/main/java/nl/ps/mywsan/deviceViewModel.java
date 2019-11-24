package nl.ps.mywsan;


import android.bluetooth.BluetoothDevice;

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

}
