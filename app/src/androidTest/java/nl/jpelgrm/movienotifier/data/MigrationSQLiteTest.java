package nl.jpelgrm.movienotifier.data;

import android.content.ContentValues;

import androidx.room.Room;
import androidx.room.testing.MigrationTestHelper;
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import nl.jpelgrm.movienotifier.models.Cinema;
import nl.jpelgrm.movienotifier.models.User;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(AndroidJUnit4.class)
public class MigrationSQLiteTest {
    private TestSQLiteOpenHelper sqlHelper;

    @Rule
    public MigrationTestHelper roomHelper =
            new MigrationTestHelper(InstrumentationRegistry.getInstrumentation(),
                    AppDatabase.class.getCanonicalName(),
                    new FrameworkSQLiteOpenHelperFactory());

    @Before
    public void prepareDatabase() {
        sqlHelper = new TestSQLiteOpenHelper(ApplicationProvider.getApplicationContext());

        // Ensure tables are in correct state for test
        TestSQLiteHelper.createTables(sqlHelper);
    }

    @After
    public void doneWithDatabase() throws IOException {
        TestSQLiteHelper.clearDatabase(sqlHelper);
    }

    @Test
    public void migrationToRoomContainsCorrectData() throws IOException {
        ContentValues testUser = TestSQLiteHelper.getTestUser();
        ContentValues testCinema = TestSQLiteHelper.getTestCinema(true);

        // Create initial database and insert test data
        sqlHelper.addUser(testUser);
        sqlHelper.addCinema();

        // Migrate
        roomHelper.runMigrationsAndValidate("notifierteststore", 5, true,
                AppDatabase.Companion.getMIGRATION_3_4(), AppDatabase.Companion.getMIGRATION_4_5());
        AppDatabase latestDb = Room.databaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase.class, "notifierteststore")
                .addMigrations(AppDatabase.Companion.getMIGRATION_3_4(), AppDatabase.Companion.getMIGRATION_4_5())
                .build();
        roomHelper.closeWhenFinished(latestDb); // Close the database and release any stream resources when the test finishes

        // Verify data
        User dbUser = latestDb.users().getUserByIdSynchronous(testUser.getAsString("ID"));
        assertNotNull(dbUser);
        assertEquals(dbUser.getId(), testUser.getAsString("ID"));
        assertEquals(dbUser.getName(), testUser.getAsString("Name"));
        assertEquals(dbUser.getEmail(), testUser.getAsString("Email"));
        assertEquals(dbUser.getApikey(), testUser.getAsString("APIKey"));

        Cinema dbCinema = latestDb.cinemas().getCinemaById(testCinema.getAsInteger("NewID"));
        assertNotNull(dbCinema);
        assertEquals(dbCinema.getId(), testCinema.getAsInteger("NewID").intValue());
        assertEquals(dbCinema.getName(), testCinema.getAsString("Name"));
        assertEquals(dbCinema.getLat(), testCinema.getAsDouble("Lat"));
        assertEquals(dbCinema.getLon(), testCinema.getAsDouble("Lon"));

    }
}
