package nl.jpelgrm.movienotifier.data

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import nl.jpelgrm.movienotifier.data.AppDatabase
import org.junit.*
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class MigrationSQLiteTest {
    private lateinit var sqlHelper: TestSQLiteOpenHelper

    @get:Rule
    var roomHelper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(),
            AppDatabase::class.java.canonicalName,
            FrameworkSQLiteOpenHelperFactory())

    @Before
    fun prepareDatabase() {
        sqlHelper = TestSQLiteOpenHelper(ApplicationProvider.getApplicationContext())

        // Ensure tables are in correct state for test
        TestSQLiteHelper.createTables(sqlHelper)
    }

    @After fun doneWithDatabase() = TestSQLiteHelper.clearDatabase(sqlHelper)

    @Test
    fun migrationToRoomContainsCorrectData() {
        val testUser = TestSQLiteHelper.getTestUser()
        val testCinema = TestSQLiteHelper.getTestCinema(true)

        // Create initial database and insert test data
        sqlHelper.addUser(testUser)
        sqlHelper.addCinema()

        // Migrate
        roomHelper.runMigrationsAndValidate("notifierteststore", 5, true,
                AppDatabase.MIGRATION_3_4, AppDatabase.MIGRATION_4_5)
        val latestDb = Room.databaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java, "notifierteststore")
                .addMigrations(AppDatabase.MIGRATION_3_4, AppDatabase.MIGRATION_4_5)
                .build()
        roomHelper.closeWhenFinished(latestDb) // Close the database and release any stream resources when the test finishes

        // Verify data
        val dbUser = latestDb.users().getUserByIdSynchronous(testUser.getAsString("ID"))
        Assert.assertNotNull(dbUser)
        Assert.assertEquals(dbUser!!.id, testUser.getAsString("ID"))
        Assert.assertEquals(dbUser.name, testUser.getAsString("Name"))
        Assert.assertEquals(dbUser.email, testUser.getAsString("Email"))
        Assert.assertEquals(dbUser.apikey, testUser.getAsString("APIKey"))
        val dbCinema = latestDb.cinemas().getCinemaById(testCinema.getAsInteger("NewID"))
        Assert.assertNotNull(dbCinema)
        Assert.assertEquals(dbCinema!!.id, testCinema.getAsInteger("NewID"))
        Assert.assertEquals(dbCinema.name, testCinema.getAsString("Name"))
        Assert.assertEquals(dbCinema.lat, testCinema.getAsDouble("Lat"))
        Assert.assertEquals(dbCinema.lon, testCinema.getAsDouble("Lon"))
    }
}