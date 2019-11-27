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

public class NodeLinkManager {
    private NodeListViewAdapter mNodeListViewAdapter;
    private LedButtonService mService;
    private NodeLinkListener mListener = null;

    public NodeLinkManager(Context appContext) {
        ArrayList<Node> nodeDeviceList = new ArrayList<Node>();
        mNodeListViewAdapter = new NodeListViewAdapter(appContext, nodeDeviceList);
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
                break;
            // Device disconnected
            case NODE_LINK_DISCONNECTED:
                removeNode(connHandle);
                if (mListener != null) {
                    mListener.onListChanged();
                }
                break;
            // Data update received
            case NODE_LINK_DATA_UPDATE:
                Log.d("NodeLinkManager", "Data updated");
                nodeDataUpdate(connHandle, Arrays.copyOfRange(packet, 4, 4 + packet[3]));
                break;
        }
    }

    public ArrayAdapter getListAdapter() {
        return mNodeListViewAdapter;
    }

    public void addDebugItem() {
        mNodeListViewAdapter.add(new Node());
    }

    public void addNode(int connHandle, int type, int[] location, int hopcount, int temperature,
                        int pressure, int humidity, String name, String timestamp) {
        Node newNode = new Node(connHandle, type);
        newNode.setName(name);
        newNode.setLocation(location);
        newNode.setHopcount(hopcount);
        newNode.setTemperature(temperature);
        newNode.setPressure(pressure);
        newNode.setHumidity(humidity);
        newNode.setTimestamp(timestamp);
        mNodeListViewAdapter.add(newNode);

        // Send welcome message to the new device
        /*byte []newCmd = new byte[2];
        newCmd[0] = (byte)OutgoingCommand.PostConnectMessage.ordinal();
        newCmd[1] = (byte)connHandle;
        sendOutgoingCommand(newCmd);*/
    }

    public void addNode(int connHandle, byte[] nodeData) {
        String deviceName;
        if (nodeData.length > 8) {
            deviceName = new String(Arrays.copyOfRange(nodeData, 8, nodeData.length));
        } else deviceName = "No name";

        Node newNode = null;
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

        boolean addNewNode = false;
        if (newNode == null) {
            newNode = new Node(connHandle, nodeData[0]);
            addNewNode = true;
        }
        newNode.setName(deviceName);
        newNode.setHopcount((int) nodeData[1]);
        newNode.setTemperature(nodeData[2] << 8 | nodeData[3]);
        newNode.setPressure(nodeData[4] << 8 | nodeData[5]);
        newNode.setHumidity(nodeData[6] << 8 | nodeData[7]);
//        newNode.setTimestamp(new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date()));
        newNode.setTimestamp(new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date()));
        if (addNewNode) mNodeListViewAdapter.add(newNode);

        // Send welcome message to the new device
        /*byte []newCmd = new byte[2];
        newCmd[0] = (byte)OutgoingCommand.PostConnectMessage.ordinal();
        newCmd[1] = (byte)connHandle;
        sendOutgoingCommand(newCmd);*/
        mNodeListViewAdapter.notifyDataChanged();
    }

    public void removeNode(int connHandle) {
        mNodeListViewAdapter.removeByConnHandle(connHandle);
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
        Node updatedNode = mNodeListViewAdapter.findByConnHandle(connHandle);
        if (updatedNode != null && data.length >= 1) {
            updatedNode.setHopcount((int) data[0]);
            updatedNode.setTemperature(data[2] << 8 | data[3]);
            updatedNode.setPressure(data[4] << 8 | data[5]);
            updatedNode.setHumidity(data[6] << 8 | data[7]);
//            updatedNode.setTimestamp(new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date()));
            updatedNode.setTimestamp(new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date()));
            mNodeListViewAdapter.notifyDataChanged();
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
            Resources res = getContext().getResources();
            ((TextView) convertView.findViewById(R.id.txtName)).setText(currentDevice.getName());
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

            ((TextView) convertView.findViewById(R.id.txtClusterID)).setText(String.valueOf(mClusterID));
            ((TextView) convertView.findViewById(R.id.txtNodeID)).setText(String.valueOf(mNodeID));
            ((TextView) convertView.findViewById(R.id.txtHopCount)).setText(String.valueOf(currentDevice.getHopcount()));
            ((TextView) convertView.findViewById(R.id.txtLocation)).setText(String.valueOf(currentDevice.getLocation()));
            ((TextView) convertView.findViewById(R.id.txtTimeStamp)).setText(String.valueOf(currentDevice.getTimestamp()));
            return convertView;
        }
    }
}
