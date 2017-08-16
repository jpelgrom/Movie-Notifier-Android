package nl.jpelgrm.movienotifier.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

import nl.jpelgrm.movienotifier.models.User;

public class DBHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "notifierlocalstore";
    private static final int DATABASE_VERSION = 1;

    private static final String CREATE_USERS_TABLE = "CREATE TABLE Users (UUID TEXT PRIMARY KEY, Name TEXT, Email TEXT, Phone TEXT, Notifications TEXT, APIKey TEXT)";

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
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if(oldVersion != newVersion) {
            db.execSQL("DROP TABLE IF EXISTS Users");
            onCreate(db);
        }
    }

    public void addUser(User user) {
        if(getUserByUUID(user.getUuid()) != null) {
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

    public User getUserByUUID(String UUID) {
        List<ContentValues> rows = performSelect("SELECT * FROM Users WHERE UUID=?", new String[] { UUID });
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
        return db.update("Users", user.toContentValues(), "UUID=?", new String[] { user.getUuid() });
    }

    public void deleteUser(String UUID) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            // Order of deletions is important when foreign key relationships exist.
            db.delete("Users", "UUID=?", new String[] { UUID });
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
