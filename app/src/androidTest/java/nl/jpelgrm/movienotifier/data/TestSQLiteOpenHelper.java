package nl.jpelgrm.movienotifier.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import nl.jpelgrm.movienotifier.models.Cinema;
import nl.jpelgrm.movienotifier.models.User;

// Helper class for creating DB as in version 1.0 of the app
// (based on commit c186fb9521b1188511db14d1d5f28d135de78471)
public class TestSQLiteOpenHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "notifierteststore";
    private static final int DATABASE_VERSION = 3;

    static final String CREATE_USERS_TABLE = "CREATE TABLE Users (ID TEXT PRIMARY KEY, Name TEXT, Email TEXT, Phone TEXT, Notifications TEXT, APIKey TEXT)";
    static final String CREATE_CINEMAS_TABLE = "CREATE TABLE Cinemas (ID TEXT PRIMARY KEY, Name TEXT, Lat TEXT, Lon TEXT)"; // TEXT is used instead of REAL for precision

    public TestSQLiteOpenHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_USERS_TABLE);
        db.execSQL(CREATE_CINEMAS_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Not required as initial testing version
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Moving from Room to SQLiteOpenHelper-based setup
        db.execSQL("DROP TABLE IF EXISTS Users");
        db.execSQL("DROP TABLE IF EXISTS Cinemas");
        onCreate(db);
    }

    public void addUser(User user) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            db.insertOrThrow("Users", null, TestSQLiteHelper.toContentValuesUser(user));
            db.setTransactionSuccessful();
        } catch(Exception e) {
            e.printStackTrace();
        } finally {
            db.endTransaction();
        }
    }

    public void addCinema(Cinema cinema) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            db.insertOrThrow("Cinemas", null, TestSQLiteHelper.toContentValuesCinema(cinema));
            db.setTransactionSuccessful();
        } catch(Exception e) {
            e.printStackTrace();
        } finally {
            db.endTransaction();
        }
    }
}
