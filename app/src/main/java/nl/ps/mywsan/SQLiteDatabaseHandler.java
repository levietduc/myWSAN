package nl.ps.mywsan;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;

public class SQLiteDatabaseHandler extends SQLiteOpenHelper {

    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "nodesDB_WSAN";
    private static final String TABLE_NAME = "nodesWSAN";
    private static final String KEY_ID = "id";
    private static final String KEY_NAME = "name";
    private static final String KEY_TYPE = "type";
    private static final String KEY_CONN_HANDLE = "connHandle";
    private static final String KEY_CLUSTER_ID = "clusterID";
    private static final String KEY_NODE_ID = "nodeID";
    private static final String KEY_HOPCOUNT = "hopCount";
    private static final String KEY_TEMPERATURE = "temperature";
    private static final String KEY_PRESSURE = "pressure";
    private static final String KEY_HUMIDITY = "humidity";
    private static final String KEY_BUTTON_STATE = "btnState";
    private static final String KEY_lATITUDE = "latitude";
    private static final String KEY_lONGITUDE = "longitude";
    private static final String KEY_TIMESTAMP = "timestamp";
    private static final String[] COLUMNS = {KEY_ID, KEY_NAME, KEY_TYPE, KEY_CONN_HANDLE, KEY_CLUSTER_ID, KEY_NODE_ID,
            KEY_HOPCOUNT, KEY_TEMPERATURE, KEY_PRESSURE, KEY_HUMIDITY, KEY_BUTTON_STATE, KEY_lATITUDE, KEY_lONGITUDE, KEY_TIMESTAMP};

    public SQLiteDatabaseHandler(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // use AUTOINCREMENT to force inserting a new row with id
        String CREATION_TABLE = "CREATE TABLE nodesWSAN ( "
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, " + "name TEXT, " + "type INTEGER, " + "connHandle INTEGER, " + "clusterID INTEGER, " + "nodeID INTEGER, "
                + "hopCount INTEGER, " + "temperature INTEGER, " + "pressure INTEGER, " + "humidity INTEGER, " + "btnState INTEGER, " + "latitude INTEGER, " + "longitude INTEGER, " + "timestamp TEXT )";

        Log.d("SQLiteDatabaseOnCreate", CREATION_TABLE);
        db.execSQL(CREATION_TABLE);

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // you can implement here migration process
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }

    // delete one measurement
    public void deleteMeasurement(Measurement measurement) {
        // Get reference to writable DB
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_NAME, "id = ?", new String[]{String.valueOf(measurement.getId())});
        db.close();
    }

    // delete measurements of a node
    public void deleteNodeMeasurements(Node node) {
        // Get reference to writable DB
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_NAME, "connHandle = ?", new String[]{String.valueOf(node.getConnHandle())});
        db.close();
    }

    // retrieve one measurement from the database given id
    public Measurement getMeasurement(int id) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_NAME, // a. table
                COLUMNS, // b. column names
                " id = ?", // c. selections
                new String[]{String.valueOf(id)}, // d. selections args
                null, // e. group by
                null, // f. having
                null, // g. order by
                null); // h. limit

        if (cursor != null)
            cursor.moveToFirst();

        Measurement measurement = new Measurement();

        measurement.setId(Integer.parseInt(cursor.getColumnName(0)));
        measurement.setName(cursor.getString(1));
        measurement.setType(Integer.parseInt(cursor.getString(2)));
        measurement.setConnHandle(Integer.parseInt(cursor.getString(3)));
        measurement.setClusterID(Integer.parseInt(cursor.getString(4)));
        measurement.setNodeID(Integer.parseInt(cursor.getString(5)));
        measurement.setHopcount(Integer.parseInt(cursor.getString(6)));
        measurement.setTemperature(Integer.parseInt(cursor.getString(7)));
        measurement.setPressure(Integer.parseInt(cursor.getString(8)));
        measurement.setHumidity(Integer.parseInt(cursor.getString(9)));
        measurement.setBtnState(Integer.parseInt(cursor.getString(10)));
        measurement.setLatitude(Integer.parseInt(cursor.getString(11)));
        measurement.setLongitude(Integer.parseInt(cursor.getString(12)));
        measurement.setTimestamp(cursor.getString(13));

        return measurement;
    }

    // retrieve measurement from one node in the database
    public LinkedHashMap<String, Measurement> getNodeMeasurements(int connHandle, int limit) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_NAME, // a. table
                COLUMNS, // b. column names
                " connHandle = ?", // c. selections
                new String[]{String.valueOf(connHandle)}, // d. selections args
                null, // e. group by
                null, // f. having
                null, // g. order by
                String.valueOf(limit)); // h. limit

        LinkedHashMap<String, Measurement> nodeMeasurements = new LinkedHashMap<>();
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                do {
                    final String mTimestamp = cursor.getString(cursor.getColumnCount() - 1); // timestamp is stored at the last column
                    final Measurement measurement = new Measurement();

                    measurement.setId(Integer.parseInt(cursor.getColumnName(0)));
                    measurement.setName(cursor.getString(1));
                    measurement.setType(Integer.parseInt(cursor.getString(2)));
                    measurement.setConnHandle(Integer.parseInt(cursor.getString(3)));
                    measurement.setClusterID(Integer.parseInt(cursor.getString(4)));
                    measurement.setNodeID(Integer.parseInt(cursor.getString(5)));
                    measurement.setHopcount(Integer.parseInt(cursor.getString(6)));
                    measurement.setTemperature(Integer.parseInt(cursor.getString(7)));
                    measurement.setPressure(Integer.parseInt(cursor.getString(8)));
                    measurement.setHumidity(Integer.parseInt(cursor.getString(9)));
                    measurement.setBtnState(Integer.parseInt(cursor.getString(10)));
                    measurement.setLatitude(Integer.parseInt(cursor.getString(11)));
                    measurement.setLongitude(Integer.parseInt(cursor.getString(12)));
                    measurement.setTimestamp(cursor.getString(13));

                    nodeMeasurements.put(mTimestamp, measurement);
                } while (cursor.moveToNext());

            }
            cursor.close();
        }

        return nodeMeasurements;
    }

    // get all measurements from database
    public List<Measurement> getAllMeasurements() {

        List<Measurement> measurements = new LinkedList<Measurement>();
        String query = "SELECT  * FROM " + TABLE_NAME;
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(query, null);
        Measurement measurement = null;

        if (cursor.moveToFirst()) {
            do {
                measurement = new Measurement();
                measurement.setId(Integer.parseInt(cursor.getColumnName(0)));
                measurement.setName(cursor.getString(1));
                measurement.setType(Integer.parseInt(cursor.getString(2)));
                measurement.setConnHandle(Integer.parseInt(cursor.getString(3)));
                measurement.setClusterID(Integer.parseInt(cursor.getString(4)));
                measurement.setNodeID(Integer.parseInt(cursor.getString(5)));
                measurement.setHopcount(Integer.parseInt(cursor.getString(6)));
                measurement.setTemperature(Integer.parseInt(cursor.getString(7)));
                measurement.setPressure(Integer.parseInt(cursor.getString(8)));
                measurement.setHumidity(Integer.parseInt(cursor.getString(9)));
                measurement.setBtnState(Integer.parseInt(cursor.getString(10)));
                measurement.setLatitude(Integer.parseInt(cursor.getString(11)));
                measurement.setLongitude(Integer.parseInt(cursor.getString(12)));
                measurement.setTimestamp(cursor.getString(13));
                measurements.add(measurement);
            } while (cursor.moveToNext());
        }

        return measurements;
    }

    // get the last timestamp from database
    public String getLasTimestamp() {

        String lastTimestamp = null;
        String query = "SELECT  * FROM " + TABLE_NAME;
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(query, null);

        if (cursor.moveToLast()) {
            lastTimestamp = cursor.getString(13);
        }
        return lastTimestamp;
    }

    //COLUMNS = {KEY_ID, KEY_NAME, KEY_TYPE, KEY_CONN_HANDLE, KEY_CLUSTER_ID, KEY_NODE_ID,
    //            KEY_HOPCOUNT, KEY_TEMPERATURE, KEY_PRESSURE, KEY_HUMIDITY, KEY_BUTTON_STATE, KEY_TIMESTAMP};

    // add a measurement
    public void addMeasurement(Measurement measurement) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
//        if (measurement.getId()!= -1){
//            values.put(KEY_ID, measurement.getId());
//        }
        values.put(KEY_NAME, measurement.getName());
        values.put(KEY_TYPE, measurement.getType());
        values.put(KEY_CONN_HANDLE, measurement.getConnHandle());
        values.put(KEY_CLUSTER_ID, measurement.getClusterID());
        values.put(KEY_NODE_ID, measurement.getNodeID());
        values.put(KEY_HOPCOUNT, measurement.getHopcount());
        values.put(KEY_TEMPERATURE, measurement.getTemperature());
        values.put(KEY_PRESSURE, measurement.getPressure());
        values.put(KEY_HUMIDITY, measurement.getHumidity());
        values.put(KEY_BUTTON_STATE, measurement.getBtnState());
        values.put(KEY_lATITUDE, measurement.getLatitude());
        values.put(KEY_lONGITUDE, measurement.getLongitude());
        values.put(KEY_TIMESTAMP, measurement.getTimestamp());
        // insert
        // id will be automatically created, incrementally
        db.insert(TABLE_NAME, null, values);
        db.close();
    }

    // update a measurement
    public int updateMeasurement(Measurement measurement) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
//        values.put(KEY_ID, measurement.getId());
        values.put(KEY_NAME, measurement.getName());
        values.put(KEY_TYPE, measurement.getType());
        values.put(KEY_CONN_HANDLE, measurement.getConnHandle());
        values.put(KEY_CLUSTER_ID, measurement.getClusterID());
        values.put(KEY_NODE_ID, measurement.getNodeID());
        values.put(KEY_HOPCOUNT, measurement.getHopcount());
        values.put(KEY_TEMPERATURE, measurement.getTemperature());
        values.put(KEY_PRESSURE, measurement.getPressure());
        values.put(KEY_HUMIDITY, measurement.getHumidity());
        values.put(KEY_BUTTON_STATE, measurement.getBtnState());
        values.put(KEY_lATITUDE, measurement.getLatitude());
        values.put(KEY_lONGITUDE, measurement.getLongitude());
        values.put(KEY_TIMESTAMP, measurement.getTimestamp());

        int i = db.update(TABLE_NAME, // table
                values, // column/value
                "id = ?", // selections
                new String[]{String.valueOf(measurement.getId())});

        db.close();

        return i;
    }

    // retrieve only temperature from one node in the database
    public LinkedHashMap<String, String> getNodeTemperatures(int connHandle, int limit) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_NAME, // a. table
                COLUMNS, // b. column names
                " connHandle = ?", // c. selections
                new String[]{String.valueOf(connHandle)}, // d. selections args
                null, // e. group by
                null, // f. having
                "id DESC", // g. order by
                String.valueOf(limit)); // h. limit

        LinkedHashMap<String, String> nodeTemperature = new LinkedHashMap<>();
        if (cursor != null) {
            if (cursor.moveToLast()) {
                do {
                    final String mTimestamp = cursor.getString(cursor.getColumnCount() - 1); // timestamp is stored at the last column
                    nodeTemperature.put(mTimestamp, cursor.getString(7));
                } while (cursor.moveToPrevious());

            }
            cursor.close();
        }

        return nodeTemperature;
    }

    // retrieve only pressures from one node in the database
    public LinkedHashMap<String, String> getNodePressures(int connHandle, int limit) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_NAME, // a. table
                COLUMNS, // b. column names
                " connHandle = ?", // c. selections
                new String[]{String.valueOf(connHandle)}, // d. selections args
                null, // e. group by
                null, // f. having
                "id DESC", // g. order by
                String.valueOf(limit)); // h. limit

        LinkedHashMap<String, String> nodePressure = new LinkedHashMap<>();
        if (cursor != null) {
            if (cursor.moveToLast()) {
                do {
                    final String mTimestamp = cursor.getString(cursor.getColumnCount() - 1); // timestamp is stored at the last column
                    nodePressure.put(mTimestamp, cursor.getString(8));
                } while (cursor.moveToPrevious());

            }
            cursor.close();
        }

        return nodePressure;
    }

    // retrieve only humidity from one node in the database
    public LinkedHashMap<String, String> getNodeHumidity(int connHandle, int limit) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_NAME, // a. table
                COLUMNS, // b. column names
                " connHandle = ?", // c. selections
                new String[]{String.valueOf(connHandle)}, // d. selections args
                null, // e. group by
                null, // f. having
                "id DESC", // g. order by
                String.valueOf(limit)); // h. limit

        LinkedHashMap<String, String> nodeHumidity = new LinkedHashMap<>();
        if (cursor != null) {
            if (cursor.moveToLast()) {
                do {
                    final String mTimestamp = cursor.getString(cursor.getColumnCount() - 1); // timestamp is stored at the last column
                    nodeHumidity.put(mTimestamp, cursor.getString(9));
                } while (cursor.moveToPrevious());

            }
            cursor.close();
        }

        return nodeHumidity;
    }

}
