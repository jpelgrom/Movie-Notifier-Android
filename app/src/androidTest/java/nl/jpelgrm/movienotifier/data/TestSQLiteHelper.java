package nl.jpelgrm.movienotifier.data;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;

import com.google.gson.Gson;

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

    static void clearDatabase(TestSQLiteOpenHelper helper) {
        SQLiteDatabase db = helper.getWritableDatabase();

        db.execSQL("DROP TABLE IF EXISTS Users");
        db.execSQL("DROP TABLE IF EXISTS Cinemas");

        db.close();
    }

    static User getTestUser() {
        return new User(UUID.randomUUID().toString(),
                "cinemaenthousia",
                "enthousiast@example.com",
                "+31698765432",
                Collections.singletonList("FBM"),
                new ApiKeyHelper().randomAPIKey());
    }

    static Cinema getTestCinema() {
        return new Cinema("PATHE27",
                "Pathé Arnhem",
                51.98422,
                5.90339);
    }

    static ContentValues toContentValuesUser(User user) {
        ContentValues cv = new ContentValues();
        cv.put("ID", user.getId());
        cv.put("Name", user.getName());
        cv.put("Email", user.getEmail());
        cv.put("Phone", user.getPhonenumber());
        cv.put("Notifications", new Gson().toJson(user.getNotifications()));
        cv.put("APIKey", user.getApikey());

        return cv;
    }

    static ContentValues toContentValuesCinema(Cinema cinema) {
        ContentValues cv = new ContentValues();
        cv.put("ID", cinema.getId());
        cv.put("Name", cinema.getName());
        cv.put("Lat", cinema.getLat());
        cv.put("Lon", cinema.getLon());

        return cv;
    }
}
