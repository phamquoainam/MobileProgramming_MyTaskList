package hcmute.edu.vn.mytasklist.data.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

import hcmute.edu.vn.mytasklist.data.entity.PomodoroSession;

@Dao
public interface PomodoroDao {

    @Insert
    long insert(PomodoroSession session);

    @Query("SELECT * FROM pomodoro_sessions WHERE task_id = :taskId ORDER BY start_time DESC")
    List<PomodoroSession> getSessionsForTask(long taskId);

    @Query("SELECT SUM(duration) FROM pomodoro_sessions WHERE status = 'COMPLETED' AND start_time >= :from AND start_time <= :to")
    Long getCompletedPomodoroDuration(long from, long to);
}
