package nl.jpelgrm.movienotifier.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

import nl.jpelgrm.movienotifier.models.Cinema;
import nl.jpelgrm.movienotifier.models.User;

public class DBHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "notifierlocalstore";
    private static final int DATABASE_VERSION = 3;

    private static final String CREATE_USERS_TABLE = "CREATE TABLE Users (ID TEXT PRIMARY KEY, Name TEXT, Email TEXT, Phone TEXT, Notifications TEXT, APIKey TEXT)";
    private static final String CREATE_CINEMAS_TABLE = "CREATE TABLE Cinemas (ID TEXT PRIMARY KEY, Name TEXT, Lat TEXT, Lon TEXT)"; // TEXT is used instead of REAL for precision

    private static DBHelper instance;

    private DBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public static synchronized DBHelper getInstance(Context context) {
        if(instance == null) {
            instance = new DBHelper(context.getApplicationContext());
        }
        return instance;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_USERS_TABLE);
        db.execSQL(CREATE_CINEMAS_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if(oldVersion != newVersion) {
            if(oldVersion == 2 && newVersion == 3) {
                db.execSQL(CREATE_CINEMAS_TABLE);
            } else { // Default
                db.execSQL("DROP TABLE IF EXISTS Users");
                db.execSQL("DROP TABLE IF EXISTS Cinemas");
                onCreate(db);
            }
        }
    }

    public void addUser(User user) {
        if(getUserByID(user.getID()) != null) {
            updateUser(user);
        } else {
            SQLiteDatabase db = getWritableDatabase();
            db.beginTransaction();
            try {
                db.insertOrThrow("Users", null, user.toContentValues());
                db.setTransactionSuccessful();
            } catch(Exception e) {
                e.printStackTrace();
            } finally {
                db.endTransaction();
            }
        }
    }

    public List<User> getUsers() {
        List<User> ret = new ArrayList<>();
        List<ContentValues> rows = performSelect("SELECT * FROM Users", null);
        for (ContentValues cv : rows) {
            User user = new User();
            user.setContent(cv);
            ret.add(user);
        }
        return ret;
    }

    public User getUserByID(String ID) {
        List<ContentValues> rows = performSelect("SELECT * FROM Users WHERE ID=?", new String[] { ID });
        if (rows.size() == 0) {
            return null;
        }

        User user = new User();
        user.setContent(rows.get(0));
        return user;
    }

    public User getUserByAPIKey(String APIKey) {
        List<ContentValues> rows = performSelect("SELECT * FROM Users WHERE APIKey=?", new String[] { APIKey });
        if (rows.size() == 0) {
            return null;
        }

        User user = new User();
        user.setContent(rows.get(0));
        return user;
    }

    public int updateUser(User user) {
        SQLiteDatabase db = this.getWritableDatabase();
        return db.update("Users", user.toContentValues(), "ID=?", new String[] { user.getID() });
    }

    public void deleteUser(String ID) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            // Order of deletions is important when foreign key relationships exist.
            db.delete("Users", "ID=?", new String[] { ID });
            db.setTransactionSuccessful();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            db.endTransaction();
        }
    }

    public void addCinema(Cinema cinema) {
        if(getCinemaByID(cinema.getID()) != null) {
            updateCinema(cinema);
        } else {
            SQLiteDatabase db = getWritableDatabase();
            db.beginTransaction();
            try {
                db.insertOrThrow("Cinemas", null, cinema.toContentValues());
                db.setTransactionSuccessful();
            } catch(Exception e) {
                e.printStackTrace();
            } finally {
                db.endTransaction();
            }
        }
    }

    public List<Cinema> getCinemas() {
        List<Cinema> ret = new ArrayList<>();
        List<ContentValues> rows = performSelect("SELECT * FROM Cinemas", null);
        for (ContentValues cv : rows) {
            Cinema cinema = new Cinema();
            cinema.setContent(cv);
            ret.add(cinema);
        }
        return ret;
    }

    public Cinema getCinemaByID(String ID) {
        List<ContentValues> rows = performSelect("SELECT * FROM Cinemas WHERE ID=?", new String[] { ID });
        if (rows.size() == 0) {
            return null;
        }

        Cinema cinema = new Cinema();
        cinema.setContent(rows.get(0));
        return cinema;
    }

    public int updateCinema(Cinema cinema) {
        SQLiteDatabase db = this.getWritableDatabase();
        return db.update("Cinemas", cinema.toContentValues(), "ID=?", new String[] { cinema.getID() });
    }

    public void deleteCinema(String ID) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            // Order of deletions is important when foreign key relationships exist.
            db.delete("Cinemas", "ID=?", new String[] { ID });
            db.setTransactionSuccessful();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            db.endTransaction();
        }
    }

    private List<ContentValues> performSelect(String query, String[] bindArgs) {
        List<ContentValues> ret = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();

        Cursor result = db.rawQuery(query, bindArgs);
        while(result.moveToNext()) {
            ContentValues cv = new ContentValues();
            DatabaseUtils.cursorRowToContentValues(result, cv);
            ret.add(cv);
        }
        result.close();
        return ret;
    }
}
