package nl.ps.mywsan;

import android.content.Context;
import android.content.res.Resources;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;

/**
 * Created by Le Viet Duc on 26-November-2019
 */
// id for a node in database should not be the connHandler as the connHandler can be duplicated when adding new sensor data of the same node
public class NodeLinkManager {
    // database
    // save measurement when new node is added to view list and when data of a node is updated
    private SQLiteDatabaseHandler db;

    // Connection
    private NodeListViewAdapter mNodeListViewAdapter;
    private LedButtonService mService;
    private NodeLinkListener mListener = null;

    private deviceViewModel viewModel;

    public NodeLinkManager(Context appContext) {

        ArrayList<Node> nodeList = new ArrayList<Node>();
        mNodeListViewAdapter = new NodeListViewAdapter(appContext, nodeList);

        // database
        db = new SQLiteDatabaseHandler(appContext);
        Log.d("NodeLinkManager", "new db");

//        // add dummy nodes to debug
//        addNode(4353, "Dummy", 6,  11,  1,  3,  4,
//                5,  6,  1,  1,  1, new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date()));
//        addNode(4609, "Dummy", 6,  12,  2,  3,  4,
//                5,  6,  1,  1,  1, new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date()));
//        // end of adding dummy nodes
    }

    public void setNodeLinkListener(NodeLinkListener listener) {
        mListener = listener;
    }

    public int getNumberOfLinks() {
        return mNodeListViewAdapter.getCount();
    }

    public void setOutgoingService(LedButtonService service) {
        mService = service;
    }

    public void processNodePacket(byte[] packet) {
        String hexString = "";
        for (int i = 0; i < packet.length; i++) {
            hexString += (int) packet[i] + " ";
        }
        Log.d("NodeLinkManager", "New packet: " + hexString);

        int connHandle = 0xFFFF; // combine of cluster id + node local id
        if (packet.length >= 3) {
            connHandle = packet[1] << 8 | packet[2];
        }
        switch (NODE_TX_COMMANDS.values()[packet[0]]) {
            // Device connected
//            case LINK_CONNECTED:
//                addNode(connHandle, Arrays.copyOfRange(packet, 3, packet.length));
//                if (mListener != null) {
//                    mListener.onListChanged();
//                }
//                break;
//            // Device disconnected
//            case LINK_DISCONNECTED:
//                removeNodeDevice(connHandle);
//                if (mListener != null) {
//                    mListener.onListChanged();
//                }
//                break;
//            // Data update received
//            case LINK_DATA_UPDATE:
//                Log.d("NodeLinkManager", "Data updated");
//                nodeDeviceDataUpdate(connHandle, Arrays.copyOfRange(packet, 4, 4 + packet[3]));
//                break;
//
//            case LED_BUTTON_PRESSED:
//                nodeDevicesLedUpdate(packet[1]);
//                break;

            // Network Nodes
            case NODE_LINK_CONNECTED:
                addNode(connHandle, Arrays.copyOfRange(packet, 3, packet.length));
                if (mListener != null) {
                    mListener.onListChanged();
                }
//                // trigger listener if there are checked node
//                if (mListener != null && getCheckedNodeList().size()>0){
//                    mListener.onClickedNodeChanged();
//                }
                break;
            // Device disconnected
            case NODE_LINK_DISCONNECTED:
                removeNode(connHandle);
                if (mListener != null) {
                    mListener.onListChanged();
                }
//                // trigger listener if there are checked node
//                if (mListener != null && getCheckedNodeList().size()>0){
//                    mListener.onClickedNodeChanged();
//                }
                break;
            // Data update received
            case NODE_LINK_DATA_UPDATE:
                Log.d("NodeLinkManager", "Data updated");
                // check again if the node is existed in the list view
                if (mNodeListViewAdapter.findByConnHandle(connHandle) != null) {
                    nodeDataUpdate(connHandle, Arrays.copyOfRange(packet, 5, 5 + packet[4]));
//                    // trigger listener if there are checked node
//                    if (mListener != null && getCheckedNodeList().size()>0){
//                        mListener.onClickedNodeChanged();
//                    }
                } else {
                    //TODO: the node hasn't added to the list view, now add it
                    addNode(connHandle, Arrays.copyOfRange(packet, 3, packet.length));
//                    // trigger listener if there are checked node
//                    if (mListener != null && getCheckedNodeList().size()>0){
//                        mListener.onClickedNodeChanged();
//                    }

                }
                if (mListener != null) {
                    mListener.onListChanged();
                }

                break;
        }
    }

    public ArrayAdapter getListAdapter() {
        return mNodeListViewAdapter;
    }

    public ArrayList<Node> getNodeList() {
        ArrayList<Node> nodeList = new ArrayList<>();
        for (int i = 0; i < mNodeListViewAdapter.getCount(); i++) {
            nodeList.add(i, (Node) mNodeListViewAdapter.getItem(i));
        }
        return nodeList;
    }

    // get list of checked nodes
    public ArrayList<Node> getCheckedNodeList() {
        ArrayList<Node> checkedNodeList = new ArrayList<>();

        for (int i = 0; i < mNodeListViewAdapter.getCount(); i++) {
            Node node = (Node) mNodeListViewAdapter.getItem(i);
            if (node.Checked == true) {
                checkedNodeList.add(node);
            }
        }
        return checkedNodeList;
    }

    public void addDebugItem() {
        mNodeListViewAdapter.add(new Node());
    }

    public void addNode(int connHandle, String name, int type, int clusterID, int nodeID, int hopCount, int temperature,
                        int pressure, int humidity, int btnState, int latitude, int longitude, String timestamp) {

        Node newNode = new Node(connHandle, type);
        newNode.setName(name);
        newNode.setHopcount(hopCount);
        newNode.setTemperature(temperature);
        newNode.setPressure(pressure);
        newNode.setHumidity(humidity);
        newNode.setBtnState(btnState);
        newNode.setTimestamp(timestamp);
        mNodeListViewAdapter.add(newNode);

        // Add measurement to database
        Measurement measurement = new Measurement();
        measurement.setName(name);
        measurement.setType(type);
        measurement.setConnHandle(connHandle);
        measurement.setClusterID(connHandle >> 8 & 0x00FF);
        measurement.setNodeID(connHandle & 0x00FF);
        measurement.setHopcount(hopCount);
        measurement.setTemperature(temperature);
        measurement.setPressure(pressure);
        measurement.setHumidity(humidity);
        measurement.setBtnState(btnState);
        measurement.setLatitude(latitude);
        measurement.setLongitude(longitude);
        measurement.setTimestamp(timestamp);
        db.addMeasurement(measurement);
    }

    public void addNode(int connHandle, byte[] nodeData) {
        String deviceName;
        if (nodeData.length > nodeData[1] + 2) {
            deviceName = new String(Arrays.copyOfRange(nodeData, nodeData[1] + 2, nodeData.length));
        } else deviceName = "No name";

        Node newNode = null;
        Measurement newMeasurement = null;
        for (int i = 0; i < mNodeListViewAdapter.getCount(); i++) {
            if (((Node) mNodeListViewAdapter.getItem(i)).getConnHandle() == connHandle) {
                newNode = (Node) mNodeListViewAdapter.getItem(i);
                break;
            }
        }
//
//        tx_command_payload[1] = data->p_data[4];
//        tx_command_payload[2] = data->p_data[5];
//        tx_command_payload[3] = 2 ;//type; -> nodeData[0]
//        tx_command_payload[4] = data->p_data[3]; //hopcounts -> nodeData[1]
//        tx_command_payload[5] = data->p_data[7]; //temp1 -> nodeData[2]
//        tx_command_payload[6] = data->p_data[8];; //temp2 -> nodeData[3]
//        tx_command_payload[7] =  data->p_data[9]; //pressure 1 -> nodeData[4]
//        tx_command_payload[8] =  data->p_data[10];; //pressure 2 -> nodeData[5]
//        tx_command_payload[9] = data->p_data[11];; //hum 1 -> nodeData[6]
//        tx_command_payload[10] =  data->p_data[12];; //hum 2 -> nodeData[7]
//        tx_command_payload[11] =  data->p_data[13];; //btnState -> nodeData[8]

//        String timestamp = new SimpleDateFormat("HH:mm:ss:SSS", Locale.getDefault()).format(new Date());
        String timestamp = new SimpleDateFormat("yyyyMMdd_HH:mm:ss:SSS", Locale.getDefault()).format(new Date());
        boolean addNewNode = false;
        if (newNode == null) {
            newNode = new Node(connHandle, nodeData[0]);
            addNewNode = true;
            // database
            newMeasurement = new Measurement(connHandle, nodeData[0]);
            // Add measurement to database
            newMeasurement.setName(deviceName);
            newMeasurement.setConnHandle(connHandle);
            newMeasurement.setClusterID(connHandle >> 8 & 0x00FF);
            newMeasurement.setNodeID(connHandle & 0x00FF);
            newMeasurement.setHopcount((int) nodeData[2]);
            newMeasurement.setTemperature((nodeData[3] << 8) | (nodeData[4] & 0x00FF));
            newMeasurement.setPressure(nodeData[5] << 24 | nodeData[6] << 16 & 0x00FF0000 | nodeData[7] << 8 & 0x0000FF00 | nodeData[8] & 0x00FF);
            newMeasurement.setHumidity(nodeData[9] << 8 | nodeData[10]);
            newMeasurement.setBtnState((int) nodeData[11]);
            newMeasurement.setLatitude(0);
            newMeasurement.setLongitude(0);
            newMeasurement.setTimestamp(timestamp);
        }
        newNode.setName(deviceName);
        newNode.setHopcount((int) nodeData[2]);
        newNode.setTemperature(nodeData[3] << 8 | nodeData[4]);
        newNode.setPressure(nodeData[5] << 8 | nodeData[6]);
        newNode.setHumidity(nodeData[7] << 8 | nodeData[8]);
        newNode.setBtnState((int) nodeData[9]);
        newNode.setTimestamp(timestamp);
        newNode.setAlive(60);


        if (addNewNode) {
            mNodeListViewAdapter.add(newNode);
            // add new measurement to database
            db.addMeasurement(newMeasurement);
        }
        mNodeListViewAdapter.notifyDataChanged();
    }

    public void removeNode(int connHandle) {
        mNodeListViewAdapter.removeByConnHandle(connHandle);
        // when a node is removed from list view, we don't have to remove it from the database
    }


    //        tx_command_payload[1] = data->p_data[4];
//        tx_command_payload[2] = data->p_data[5];
//        tx_command_payload[3] = 2 ;//type;
//        tx_command_payload[4] = data->p_data[3]; //hopcounts -> nodeData[0]
//        tx_command_payload[5] = data->p_data[7]; //temp1 -> nodeData[1]
//        tx_command_payload[6] = data->p_data[8];; //temp2 -> nodeData[2]
//        tx_command_payload[7] =  data->p_data[9]; //pressure 1 -> nodeData[3]
//        tx_command_payload[8] =  data->p_data[10];; //pressure 2 -> nodeData[4]
//        tx_command_payload[9] = data->p_data[11];; //hum 1 -> nodeData[5]
//        tx_command_payload[10] =  data->p_data[12];; //hum 2 -> nodeData[6]

    public void nodeDataUpdate(int connHandle, byte[] data) {
        // update the node view list, which describes node characteristics
        Node updatedNode = mNodeListViewAdapter.findByConnHandle(connHandle);
        // create an new measurement database
        Measurement newMeasurement = null;
        if (updatedNode != null && data.length >= 1) {
            updatedNode.setHopcount((int) data[0]);
            updatedNode.setTemperature((data[1] << 8) | (data[2] & 0x00FF));
            updatedNode.setPressure(data[3] << 24 | data[4] << 16 & 0x00FF0000 | data[5] << 8 & 0x0000FF00 | data[6] & 0x00FF);
            // when pressing the button on a thingy, data length is only 7 (data[0..6]) -> get a bug from here.
            updatedNode.setHumidity(data[7] << 8 | data[8]);
            updatedNode.setBtnState((int) data[9]);
//            String timestamp = new SimpleDateFormat("HH:mm:ss:SSS", Locale.getDefault()).format(new Date());
            String timestamp = new SimpleDateFormat("yyyyMMdd_HH:mm:ss:SSS", Locale.getDefault()).format(new Date());
            updatedNode.setTimestamp(timestamp);
            updatedNode.setAlive(60); // timeout is 60 seconds
            mNodeListViewAdapter.notifyDataChanged();

            // update database = add the update measurement
            newMeasurement = new Measurement(connHandle, updatedNode.getType()); // fix this
            newMeasurement.setName(updatedNode.getName());
            newMeasurement.setConnHandle(connHandle);
            newMeasurement.setClusterID(connHandle >> 8 & 0x00FF);
            newMeasurement.setNodeID(connHandle & 0x00FF);
            newMeasurement.setHopcount((int) data[0]);

            // add a random temperature value to debug chart plots
//            newMeasurement.setTemperature(new Random().nextInt((4000 - 1000) + 1) + 1000);
            newMeasurement.setTemperature((data[1] << 8) | (data[2] & 0x00FF));
            // add a random pressure value to debug chart plots
//            newMeasurement.setPressure(new Random().nextInt((1100 - 720) + 1) + 720);
            newMeasurement.setPressure(data[3] << 24 | data[4] << 16 & 0x00FF0000 | data[5] << 8 & 0x0000FF00 | data[6] & 0x00FF);
//             add a random humidity value to debug chart plots
//            newMeasurement.setHumidity(new Random().nextInt((90 - 20) + 1) + 20);
            newMeasurement.setHumidity(data[7] << 8 | data[8]);
            newMeasurement.setBtnState((int) data[9]);
            newMeasurement.setLatitude(updatedNode.getLatitude());
            newMeasurement.setLongitude(updatedNode.getLongitude());
            newMeasurement.setTimestamp(timestamp);
            db.addMeasurement(newMeasurement);
        }
    }

    private int getConnIdSelectedMask() {
        int mask = 0;
        Node node;
        for (int i = 0; i < mNodeListViewAdapter.getCount(); i++) {
            node = (Node) mNodeListViewAdapter.getItem(i);
            if (node.Checked) mask |= (1 << node.getConnHandle());
        }

        // If no devices are selected, select all
        if (mask == 0) mask = 0xFFFFFFFF;

        // Return the mask
        return mask;
    }

    private void sendOutgoingCommand(byte[] command) {
        if (mService != null && mService.isConnected()) {
            mService.writeRXCharacteristic(command);
        }
    }

    public void listSelectAll() {
        for (int i = 0; i < mNodeListViewAdapter.getCount(); i++) {
            ((Node) mNodeListViewAdapter.getItem(i)).Checked = true;
        }
        mNodeListViewAdapter.notifyDataChanged();
    }

    public void listDeselectAll() {
        for (int i = 0; i < mNodeListViewAdapter.getCount(); i++) {
            ((Node) mNodeListViewAdapter.getItem(i)).Checked = false;
        }
        mNodeListViewAdapter.notifyDataChanged();
    }

    public void disconnectCentral() {
        byte[] newCmd = new byte[1];
        newCmd[0] = (byte) NodeLinkManager.OutgoingCommand.DisconnectCentral.ordinal();
        sendOutgoingCommand(newCmd);
    }

    public void itemClicked(int index) {
        if (index < mNodeListViewAdapter.getCount()) {
            Node node = (Node) mNodeListViewAdapter.getItem(index);
            node.Checked = !node.Checked;
            mNodeListViewAdapter.notifyDataChanged();
        }
    }

    public void clearNodeDevices() {
        mNodeListViewAdapter.clear();
        mNodeListViewAdapter.notifyDataChanged();
    }

    private enum NODE_TX_COMMANDS {INVALID, LINK_CONNECTED, LINK_DISCONNECTED, LINK_DATA_UPDATE, LED_BUTTON_PRESSED, NODE_LINK_CONNECTED, NODE_LINK_DISCONNECTED, NODE_LINK_DATA_UPDATE, NODE_LED_BUTTON_PRESSED}

    private enum OutgoingCommand {
        ERROR, SetLedColorAll, SetLedStateAll, PostConnectMessage, DisconnectAllPeripherals, DisconnectCentral, SetLedPulse
    }

    public interface NodeLinkListener {
        void onListChanged();
    }


    private class NodeListViewAdapter extends ArrayAdapter {
        public NodeListViewAdapter(Context context, ArrayList<Node> nodeDeviceList) {
            super(context, 0, nodeDeviceList);
        }

        public boolean removeByConnHandle(int connHandle) {
            Node foundNode = null;
            for (int i = 0; i < getCount(); i++) {
                if (((Node) getItem(i)).connHandleMatch(connHandle)) {
                    foundNode = (Node) getItem(i);
                    break;
                }
            }
            if (foundNode != null) {
                mNodeListViewAdapter.remove(foundNode);
                return true;
            } else return false;
        }

        public Node findByConnHandle(int connHandle) {
            for (int i = 0; i < getCount(); i++) {
                if (((Node) getItem(i)).connHandleMatch(connHandle)) {
                    return (Node) getItem(i);
                }
            }
            return null;
        }


        public void notifyDataChanged() {
            notifyDataSetChanged();
        }


        @Override
        public View getView(int position, View convertView, ViewGroup viewGroup) {

            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.node_link_view, viewGroup, false);
            }
            Node currentDevice = (Node) getItem(position);
            // remove the last four letters of device name
            Resources res = getContext().getResources();
            String parsedDeviceName = currentDevice.getName();
//            parsedDeviceName = parsedDeviceName.substring(0, parsedDeviceName.length() - 4);
            ((TextView) convertView.findViewById(R.id.txtName)).setText(parsedDeviceName);
            String[] nameList = {"Unknown!", "LocalBlinky", "LocalThingy", "RemoteBlinky", "RemoteThingy", "ClusterHeard"};
            if (currentDevice.getType() < nameList.length) {
                //((TextView)convertView.findViewById(R.id.type)).setText(nameList[currentDevice.getType()]);
                switch (currentDevice.getType()) {
                    case 1:
                    case 3:
                    case 5:
                        convertView.findViewById(R.id.img_type).setBackground(res.getDrawable(R.drawable.dev_dk));
                        break;
                    case 2:
                    case 4:
                        convertView.findViewById(R.id.img_type).setBackground(res.getDrawable(R.drawable.dev_thingy));
                        break;
                }

            }
            int mConnHandle = currentDevice.getConnHandle();
            int mClusterID = mConnHandle >> 8 & 0x00FF;
            int mNodeID = mConnHandle & 0x00FF;
            Log.d("NodeLinkManager", String.valueOf(mClusterID));
            ((TextView) convertView.findViewById(R.id.txtClusterID)).setText(String.valueOf(mClusterID));
            ((TextView) convertView.findViewById(R.id.txtNodeID)).setText(String.valueOf(mNodeID));
            ((TextView) convertView.findViewById(R.id.txtHopCount)).setText(String.valueOf(currentDevice.getHopcount()));

            String btnStateStr = "Off";
            TextView buttonStateText = convertView.findViewById(R.id.txtBtnState);
            if (currentDevice.getBtnState() == 1) {
                btnStateStr = "On";
                buttonStateText.setTextAppearance(getContext(), R.style.FontBleDeviceFieldOn);
            } else {
                btnStateStr = "Off";
                buttonStateText.setTextAppearance(getContext(), R.style.FontBleDeviceField);
            }
            buttonStateText.setText(btnStateStr);

            convertView.findViewById(R.id.nodeListMainLayout).setBackground(res.getDrawable(currentDevice.Checked ?
                    R.drawable.ble_device_list_background_checked :
                    R.drawable.ble_device_list_background));

            ((TextView) convertView.findViewById(R.id.txtTimeStamp)).setText(String.valueOf(currentDevice.getTimestamp()));
            return convertView;
        }
    }
}
