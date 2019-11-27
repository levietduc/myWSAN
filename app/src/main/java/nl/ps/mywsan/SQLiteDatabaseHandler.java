package nl.ps.mywsan;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.LinkedList;
import java.util.List;

public class SQLiteDatabaseHandler extends SQLiteOpenHelper {

    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "nodesDB_WSAN";
    private static final String TABLE_NAME = "nodesWSAN";
    private static final String KEY_ID = "id"; // connHandler
    private static final String KEY_NAME = "name";
    private static final String KEY_TYPE = "type";
    private static final String KEY_HOPCOUNT = "hopcount";
    private static final String KEY_TEMPERATURE = "temperature";
    private static final String KEY_PRESSURE = "pressure";
    private static final String KEY_HUMIDITY = "humidity";
    private static final String KEY_TIMESTAMP = "timestamp";
    private static final String[] COLUMNS = {KEY_ID, KEY_NAME, KEY_TYPE, KEY_HOPCOUNT, KEY_TEMPERATURE, KEY_PRESSURE, KEY_HUMIDITY, KEY_TIMESTAMP};

    public SQLiteDatabaseHandler(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // use AUTOINCREMENT to force inserting a new row with id
        String CREATION_TABLE = "CREATE TABLE nodes ( "
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, " + "name TEXT, " + "type INTEGER, "
                + "hopcount INTEGER, " + "temperature INTEGER, " + "pressure INTEGER, " + "humidity INTEGER, " + "timestamp TEXT )";

        db.execSQL(CREATION_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // you can implement here migration process
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        this.onCreate(db);
    }

    public void deleteOne(Node node) {
        // Get reference to writable DB
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_NAME, "id = ?", new String[]{String.valueOf(node.getConnHandle())});
        db.close();
    }

    // retrieve node data from the database
    public Node getnode(int id) {
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

        Node node = new Node();
        node.setConnHandle(Integer.parseInt(cursor.getString(0)));
        node.setName(cursor.getString(1));
        node.setType(Integer.parseInt(cursor.getString(2)));
        node.setHopcount(Integer.parseInt(cursor.getString(3)));
        node.setTemperature(Integer.parseInt(cursor.getString(4)));
        node.setPressure(Integer.parseInt(cursor.getString(5)));
        node.setHumidity(Integer.parseInt(cursor.getString(6)));
        node.setTimestamp(cursor.getString(7));

        return node;
    }

    // get all nodes from db
    public List<Node> allnodes() {

        List<Node> nodes = new LinkedList<Node>();
        String query = "SELECT  * FROM " + TABLE_NAME;
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(query, null);
        Node node = null;

        if (cursor.moveToFirst()) {
            do {
                node = new Node();
                node.setConnHandle(Integer.parseInt(cursor.getString(0)));
                node.setName(cursor.getString(1));
                node.setType(Integer.parseInt(cursor.getString(2)));
                node.setHopcount(Integer.parseInt(cursor.getString(3)));
                node.setTemperature(Integer.parseInt(cursor.getString(4)));
                node.setPressure(Integer.parseInt(cursor.getString(5)));
                node.setHumidity(Integer.parseInt(cursor.getString(6)));
                node.setTimestamp(cursor.getString(7));
                nodes.add(node);
            } while (cursor.moveToNext());
        }

        return nodes;
    }

    //COLUMNS = {KEY_ID, KEY_NAME, KEY_TYPE, KEY_HOPCOUNT, KEY_TEMPERATURE, KEY_PRESSURE, KEY_HUMIDITY, KEY_TIMESTAMP};

    public void addnode(Node node) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_NAME, node.getName());
        values.put(KEY_TYPE, node.getType());
        values.put(KEY_HOPCOUNT, node.getHopcount());
        values.put(KEY_TEMPERATURE, node.getTemperature());
        values.put(KEY_PRESSURE, node.getPressure());
        values.put(KEY_HUMIDITY, node.getHumidity());
        values.put(KEY_TIMESTAMP, node.getTimestamp());
        // insert
        db.insert(TABLE_NAME, null, values);
        db.close();
    }

    public int updatenode(Node node) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_NAME, node.getName());
        values.put(KEY_TYPE, node.getType());
        values.put(KEY_HOPCOUNT, node.getHopcount());
        values.put(KEY_TEMPERATURE, node.getTemperature());
        values.put(KEY_PRESSURE, node.getPressure());
        values.put(KEY_HUMIDITY, node.getHumidity());
        values.put(KEY_TIMESTAMP, node.getTimestamp());

        int i = db.update(TABLE_NAME, // table
                values, // column/value
                "id = ?", // selections
                new String[]{String.valueOf(node.getConnHandle())});

        db.close();

        return i;
    }

}
