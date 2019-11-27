package nl.ps.mywsan;


import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Node {

    public boolean Checked;
    private int[] location;
    //    private int linkstate; // byte[0] -> 1
    private int connHandle; // byte[1,2] -> 3
    private int type; // byte[3] -> 4
    private int cluster; // byte[4] -> 5
    private int hopcount; // byte[5] -> 6
    private int temperature; // byte[6,7] -> 8
    private int pressure; // byte[8, 9] -> 10
    private int humidity; // byte[10,11] -> 12
    private String name; // byte[name.length] -> < Max. 64
    private String timestamp;

    public Node() {
        Checked = false;
        name = "Noname";
        location = new int[]{0, 0};
        hopcount = 1;
        temperature = 25;
        pressure = 1000;
        humidity = 70;
    }

    public Node(int connHandle, int type) {
        this.connHandle = connHandle;
        this.name = name;
        this.type = type;
        this.timestamp = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
//        this.timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date()); // the moment of data
    }

    public int getConnHandle() {
        return connHandle;
    }

    public void setConnHandle(int connHandle) {
        this.connHandle = connHandle;
    }

    public boolean connHandleMatch(int connHandle) {
        return this.connHandle == connHandle;
    }


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public int getCluster() {
        return cluster;
    }

    public void setCluster(int cluster) {
        this.cluster = cluster;
    }

    public int getHopcount() {
        return hopcount;
    }

    public void setHopcount(int hopcount) {
        this.hopcount = hopcount;
    }

    public int[] getLocation() {
        return location;
    }

    public void setLocation(int[] location) {
        this.location = location;
    }

    public int getTemperature() {
        return temperature;
    }

    public void setTemperature(int temperature) {
        this.temperature = temperature;
    }

    public int getPressure() {
        return pressure;
    }

    public void setPressure(int pressure) {
        this.pressure = pressure;
    }

    public int getHumidity() {
        return humidity;
    }

    public void setHumidity(int humidity) {
        this.humidity = humidity;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }


    @Override
    public String toString() {
        return connHandle + " - " + name + " - " + type + " - " + temperature + "oC " + pressure + "mPa " + humidity + "pH " + timestamp;
    }

}