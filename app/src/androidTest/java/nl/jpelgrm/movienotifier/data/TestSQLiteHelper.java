package nl.jpelgrm.movienotifier.data;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;

import com.google.gson.Gson;

import java.io.IOException;
import java.util.Collections;
import java.util.UUID;

import nl.jpelgrm.movienotifier.models.Cinema;
import nl.jpelgrm.movienotifier.models.User;
import nl.jpelgrm.movienotifier.util.ApiKeyHelper;

// Helper class for database operations as in version 1.0 of the app
// (based on commit c186fb9521b1188511db14d1d5f28d135de78471)
class TestSQLiteHelper {
    static void createTables(TestSQLiteOpenHelper helper) {
        SQLiteDatabase db = helper.getWritableDatabase();

        db.execSQL(TestSQLiteOpenHelper.CREATE_USERS_TABLE.replace("CREATE TABLE",  "CREATE TABLE IF NOT EXISTS"));
        db.execSQL(TestSQLiteOpenHelper.CREATE_CINEMAS_TABLE.replace("CREATE TABLE",  "CREATE TABLE IF NOT EXISTS"));

        db.close();
    }

    static void clearDatabase(TestSQLiteOpenHelper helper) throws IOException {
        SQLiteDatabase db = helper.getWritableDatabase();

        db.execSQL("DROP TABLE IF EXISTS Users");
        db.execSQL("DROP TABLE IF EXISTS Cinemas");
        db.execSQL("DROP TABLE IF EXISTS Notifications");

        db.close();
    }

    static ContentValues getTestUser() {
        ContentValues cv = new ContentValues();
        cv.put("ID", UUID.randomUUID().toString());
        cv.put("Name", "cinemaenthousia");
        cv.put("Email", "enthousiast@example.com");
        cv.put("Phone", "+31698765432");
        cv.put("Notifications", new Gson().toJson(Collections.singletonList("FBM")));
        cv.put("APIKey", new ApiKeyHelper().randomAPIKey());

        return cv;
    }

    static ContentValues getTestCinema(boolean includeNewID) {
        ContentValues cv = new ContentValues();
        cv.put("ID", "PATHE27");
        if(includeNewID) {
            cv.put("NewID", 27);
        }
        cv.put("Name", "Pathé Arnhem");
        cv.put("Lat", 51.98422);
        cv.put("Lon", 5.90339);

        return cv;
    }
}
