package nl.jpelgrm.movienotifier.data;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;
import nl.jpelgrm.movienotifier.models.Cinema;
import nl.jpelgrm.movienotifier.models.Notification;
import nl.jpelgrm.movienotifier.models.User;
import nl.jpelgrm.movienotifier.util.RoomUtil;

@Database(entities = {User.class, Cinema.class, Notification.class}, version = 5)
@TypeConverters(RoomUtil.class)
public abstract class AppDatabase extends RoomDatabase {
    private static AppDatabase instance = null;

    private static void createInstance(Context context) {
        instance = Room.databaseBuilder(context.getApplicationContext(), AppDatabase.class, "notifierlocalstore")
                .addMigrations(MIGRATION_3_4, MIGRATION_4_5)
                .build();
    }

    public static AppDatabase getInstance(Context context) {
        if(instance == null) {
            synchronized(AppDatabase.class) {
                createInstance(context);
            }
        }
        return instance;
    }

    static final Migration MIGRATION_3_4 = new Migration(3, 4) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            // Room will throw an exception because the existing DB columns for ID are nullable, so fix that
            // SQLite doesn't allow altering + dropping columns, so create new + copy old data
            // New table SQL from generated schema file

            // Users
            database.execSQL("CREATE TABLE IF NOT EXISTS `UsersNew` (`ID` TEXT NOT NULL, `Name` TEXT, `Email` TEXT, `Phone` TEXT, `Notifications` TEXT, `APIKey` TEXT, PRIMARY KEY(`ID`))");
            database.execSQL("INSERT INTO UsersNew(ID, Name, Email, Phone, Notifications, APIKey) SELECT * FROM Users");
            database.execSQL("DROP TABLE Users");
            database.execSQL("ALTER TABLE UsersNew RENAME TO Users");

            // Cinemas
            database.execSQL("CREATE TABLE IF NOT EXISTS `CinemasNew` (`ID` TEXT NOT NULL, `Name` TEXT, `Lat` REAL, `Lon` REAL, PRIMARY KEY(`ID`))");
            database.execSQL("INSERT INTO CinemasNew(ID, Name, Lat, Lon) SELECT * FROM Cinemas");
            database.execSQL("DROP TABLE Cinemas");
            database.execSQL("ALTER TABLE CinemasNew RENAME TO Cinemas");
        }
    };

    static final Migration MIGRATION_4_5 = new Migration(4, 5) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            // New table SQL from generated schema file
            // Users: drop phone number and notifications and add FCM tokens and paused notifications from/to table
            database.execSQL("CREATE TABLE IF NOT EXISTS `UsersNew` (`ID` TEXT NOT NULL, `Name` TEXT, `Email` TEXT, `FCMTokens` TEXT, `APIKey` TEXT, PRIMARY KEY(`ID`))");
            database.execSQL("INSERT INTO UsersNew(ID, Name, Email, APIKey) SELECT ID, Name, Email, APIKey FROM Users");
            database.execSQL("DROP TABLE Users");
            database.execSQL("ALTER TABLE UsersNew RENAME TO Users");

            // Cinemas: change IDs for cinemas to integers and alter existing data
            database.execSQL("UPDATE Cinemas SET ID = REPLACE(ID, 'PATHE', '')");
            database.execSQL("CREATE TABLE IF NOT EXISTS `CinemasNew` (`ID` INTEGER NOT NULL, `Name` TEXT, `Lat` REAL, `Lon` REAL, PRIMARY KEY(`ID`))");
            database.execSQL("INSERT INTO CinemasNew(ID, Name, Lat, Lon) SELECT CAST(ID AS INTEGER), Name, Lat, Lon FROM Cinemas");
            database.execSQL("DROP TABLE Cinemas");
            database.execSQL("ALTER TABLE CinemasNew RENAME TO Cinemas");

            // Notifications: create new table
            database.execSQL("CREATE TABLE IF NOT EXISTS `Notifications` (`ID` TEXT NOT NULL, `Time` INTEGER NOT NULL, `UserID` TEXT NOT NULL, `WatcherID` TEXT, `WatcherName` TEXT, `WatcherMovieID` INTEGER NOT NULL, `Matches` INTEGER NOT NULL, `Body` TEXT, PRIMARY KEY(`ID`), FOREIGN KEY(`UserID`) REFERENCES `Users`(`ID`) ON UPDATE CASCADE ON DELETE CASCADE )");
            database.execSQL("CREATE  INDEX `index_Notifications_UserID` ON `Notifications` (`UserID`)");
        }
    };

    public abstract DaoUsers users();
    public abstract DaoCinemas cinemas();
    public abstract DaoNotifications notifications();
}
