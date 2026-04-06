package hcmute.edu.vn.mytasklist.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

import hcmute.edu.vn.mytasklist.data.entity.TaskEntity;

@Dao
public interface TaskDao {

    @Insert
    long insert(TaskEntity task);

    @Update
    int update(TaskEntity task);

    @Delete
    void delete(TaskEntity task);

    @Query("SELECT * FROM tasks ORDER BY due_date ASC")
    LiveData<List<TaskEntity>> getAllTasksLiveData();

    @Query("SELECT * FROM tasks ORDER BY due_date ASC")
    List<TaskEntity> getAllTasks();

    @Query("SELECT * FROM tasks WHERE id = :id LIMIT 1")
    TaskEntity getTaskById(long id);

    @Query("SELECT * FROM tasks WHERE category = :category ORDER BY due_date ASC")
    LiveData<List<TaskEntity>> getTasksByCategory(String category);

    @Query("UPDATE tasks SET completed = :completed WHERE id = :id")
    void updateCompleted(long id, boolean completed);

    @Query("UPDATE tasks SET category = 'Inbox' WHERE category = :oldCategory")
    void updateCategoryToInbox(String oldCategory);

    @Query("SELECT * FROM tasks WHERE due_date >= :from AND due_date <= :to ORDER BY due_date ASC")
    List<TaskEntity> getTasksByDateRange(long from, long to);

    @Query("SELECT * FROM tasks WHERE completed = 1")
    List<TaskEntity> getCompletedTasks();

    @Query("DELETE FROM tasks WHERE completed = 1 AND due_date < :thresholdTime")
    void deleteOldCompletedTasks(long thresholdTime);

    @Query("SELECT * FROM tasks WHERE due_date >= :startOfDay AND due_date < :endOfDay AND completed = 0")
    List<TaskEntity> getIncompleteTasksDueToday(long startOfDay, long endOfDay);

    @Query("SELECT COUNT(*) FROM tasks WHERE due_date >= :from AND due_date <= :to AND completed = 1")
    int getCompletedTaskCountByDateRange(long from, long to);

    @Query("SELECT COUNT(*) FROM tasks WHERE due_date >= :from AND due_date <= :to AND completed = 0")
    int getIncompleteTaskCountByDateRange(long from, long to);

    @Query("SELECT COUNT(*) FROM tasks WHERE completed = 0")
    int getPendingTaskCount();
}
