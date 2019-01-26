package nl.jpelgrm.movienotifier.data;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;

import androidx.room.Room;
import androidx.room.testing.MigrationTestHelper;
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import nl.jpelgrm.movienotifier.models.Cinema;
import nl.jpelgrm.movienotifier.models.User;

import static org.junit.Assert.assertEquals;

@RunWith(AndroidJUnit4.class)
public class MigrationSQLiteTest {
    private TestSQLiteOpenHelper sqlHelper;

    @Rule
    public MigrationTestHelper roomHelper =
            new MigrationTestHelper(InstrumentationRegistry.getInstrumentation(),
                    AppDatabase.class.getCanonicalName(),
                    new FrameworkSQLiteOpenHelperFactory());

    @Before
    public void prepareDatabase() throws Exception {
        sqlHelper = new TestSQLiteOpenHelper(ApplicationProvider.getApplicationContext());

        // Ensure tables are in correct state for test
        TestSQLiteHelper.createTables(sqlHelper);
    }

    @After
    public void doneWithDatabase() throws Exception {
        TestSQLiteHelper.clearDatabase(sqlHelper);
    }

    @Test
    public void migrationToRoomContainsCorrectData() throws IOException {
        User testUser = TestSQLiteHelper.getTestUser();
        Cinema testCinema = TestSQLiteHelper.getTestCinema();

        // Create initial database and insert test data
        sqlHelper.addUser(testUser);
        sqlHelper.addCinema(testCinema);

        // Migrate
        roomHelper.runMigrationsAndValidate("notifierteststore", 4, true, AppDatabase.MIGRATION_3_4);
        AppDatabase latestDb = Room.databaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase.class, "notifierteststore")
                .addMigrations(AppDatabase.MIGRATION_3_4)
                .build();
        roomHelper.closeWhenFinished(latestDb); // Close the database and release any stream resources when the test finishes

        // Verify data
        User dbUser = latestDb.users().getUserByIdSynchronous(testUser.getId());
        checkUser(dbUser, testUser);
        Cinema dbCinema = latestDb.cinemas().getCinemaById(testCinema.getId());
        checkCinema(dbCinema, testCinema);
    }

    private void checkUser(User fromDB, User expected) {
        assertEquals(fromDB.getId(), expected.getId());
        assertEquals(fromDB.getName(), expected.getName());
        assertEquals(fromDB.getEmail(), expected.getEmail());
        assertEquals(fromDB.getPhonenumber(), expected.getPhonenumber());
        assertEquals(fromDB.getNotifications(), expected.getNotifications());
        assertEquals(fromDB.getApikey(), expected.getApikey());
    }

    private void checkCinema(Cinema fromDB, Cinema expected) {
        assertEquals(fromDB.getId(), expected.getId());
        assertEquals(fromDB.getName(), expected.getName());
        assertEquals(fromDB.getLat(), expected.getLat());
        assertEquals(fromDB.getLon(), expected.getLon());
    }
}
