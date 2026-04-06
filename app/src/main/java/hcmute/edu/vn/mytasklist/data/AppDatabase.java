package hcmute.edu.vn.mytasklist.data;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteDatabase;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import hcmute.edu.vn.mytasklist.data.dao.CategoryDao;
import hcmute.edu.vn.mytasklist.data.dao.PomodoroDao;
import hcmute.edu.vn.mytasklist.data.dao.TaskDao;
import hcmute.edu.vn.mytasklist.data.entity.CategoryEntity;
import hcmute.edu.vn.mytasklist.data.entity.PomodoroSession;
import hcmute.edu.vn.mytasklist.data.entity.TaskEntity;

@Database(entities = {TaskEntity.class, CategoryEntity.class, PomodoroSession.class}, version = 1, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    public abstract TaskDao taskDao();
    public abstract CategoryDao categoryDao();
    public abstract PomodoroDao pomodoroDao();

    private static volatile AppDatabase INSTANCE;
    private static final int NUMBER_OF_THREADS = 4;
    public static final ExecutorService databaseWriteExecutor =
            Executors.newFixedThreadPool(NUMBER_OF_THREADS);

    public static AppDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    AppDatabase.class, "mytasklist_room.db")
                            .addCallback(sRoomDatabaseCallback)
                            // Allow main thread queries for simplicity in some scenarios, but best to avoid
                            // .allowMainThreadQueries() 
                            .build();
                }
            }
        }
        return INSTANCE;
    }

    private static final RoomDatabase.Callback sRoomDatabaseCallback = new RoomDatabase.Callback() {
        @Override
        public void onCreate(@NonNull SupportSQLiteDatabase db) {
            super.onCreate(db);

            databaseWriteExecutor.execute(() -> {
                CategoryDao dao = INSTANCE.categoryDao();
                String[][] defaults = {
                        {"Work Tasks",   "#5C6BC0"},
                        {"Study Goals",  "#26A69A"},
                        {"Travel Plans", "#FFA726"},
                        {"Daily To-Do",  "#EC407A"},
                        {"Life Events",  "#8D6E63"},
                        {"Inbox",        "#9E9E9E"},
                };
                for (String[] cat : defaults) {
                    dao.insert(new CategoryEntity(cat[0], cat[1]));
                }
            });
        }
    };
}
