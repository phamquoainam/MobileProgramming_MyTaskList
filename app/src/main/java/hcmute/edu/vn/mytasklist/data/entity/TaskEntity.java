package hcmute.edu.vn.mytasklist.data.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.util.Date;

@Entity(tableName = "tasks")
public class TaskEntity {

    @PrimaryKey(autoGenerate = true)
    private long id;

    @ColumnInfo(name = "title")
    private String title;

    @ColumnInfo(name = "due_date")
    private Long dueDate; // Stored as Unix timestamp (milliseconds)

    @ColumnInfo(name = "completed")
    private boolean completed;

    @ColumnInfo(name = "priority")
    private String priority; // HIGH, MEDIUM, LOW, NONE

    @ColumnInfo(name = "category")
    private String category;

    @ColumnInfo(name = "recurrence")
    private String recurrence;

    @ColumnInfo(name = "reminder_time")
    private Long reminderTime; // Exact time to remind

    @ColumnInfo(name = "reminder_offsets")
    private String reminderOffsets; // JSON string of offsets for deadline warnings (e.g. "[600000, 3600000]")

    public TaskEntity(String title, Long dueDate, String priority, String category) {
        this.title = title;
        this.dueDate = dueDate;
        this.completed = false;
        this.priority = priority;
        this.category = category;
        this.recurrence = "NONE";
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public Long getDueDate() { return dueDate; }
    public void setDueDate(Long dueDate) { this.dueDate = dueDate; }

    public boolean isCompleted() { return completed; }
    public void setCompleted(boolean completed) { this.completed = completed; }

    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getRecurrence() { return recurrence; }
    public void setRecurrence(String recurrence) { this.recurrence = recurrence; }

    public Long getReminderTime() { return reminderTime; }
    public void setReminderTime(Long reminderTime) { this.reminderTime = reminderTime; }

    public String getReminderOffsets() { return reminderOffsets; }
    public void setReminderOffsets(String reminderOffsets) { this.reminderOffsets = reminderOffsets; }
}
