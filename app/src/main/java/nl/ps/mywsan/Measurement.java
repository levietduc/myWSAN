package nl.ps.mywsan;


public class Measurement {

    private int id; // id for database
    private int latitude;
    private int longitude;
    //    private int linkstate; // byte[0] -> 1
    private int connHandle; // byte[1,2] -> 3
    private int type; // byte[3] -> 4
    private int hopcount; // byte[5] -> 5
    private int temperature; // byte[6,7] -> 7
    private int pressure; // byte[8, 9] -> 11
    private int humidity; // byte[10,11] -> 13
    private int btnState; // byte[12] -> 14
    private String name; // byte[name.length] -> < Max. 64
    private int clusterID;
    private int nodeID;
    private String timestamp;

    public Measurement() {
        name = "Noname";
        latitude = 0;
        longitude = 0;
        hopcount = 1;
        temperature = 25;
        pressure = 1000;
        humidity = 70;
        btnState = 0;
        timestamp = "null";
//        id = -1;
    }

    public Measurement(int connHandle, int type) {
        this.connHandle = connHandle;
        this.type = type;
//        this.timestamp = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
//        this.timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date()); // the moment of data

        this.clusterID = connHandle >> 8 & 0x00FF;
        this.nodeID = connHandle & 0x00FF;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
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

    public int getClusterID() {
        return clusterID;
    }

    public void setClusterID(int clusterID) {
        this.clusterID = clusterID;
    }

    public int getNodeID() {
        return nodeID;
    }

    public void setNodeID(int nodeID) {
        this.nodeID = nodeID;
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

    public int getHopcount() {
        return hopcount;
    }

    public void setHopcount(int hopcount) {
        this.hopcount = hopcount;
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

    public int getBtnState() {
        return btnState;
    }

    public void setBtnState(int btnState) {
        this.btnState = btnState;
    }

    public int getLatitude() {
        return latitude;
    }

    public void setLatitude(int latitude) {
        this.latitude = latitude;
    }

    public int getLongitude() {
        return longitude;
    }

    public void setLongitude(int longitude) {
        this.longitude = longitude;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return id + " - " + connHandle + " - " + name + " - " + type + " - " + temperature + "oC " + pressure + "mPa " + humidity + "pH " + btnState + " - " + timestamp;
    }

}