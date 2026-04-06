package hcmute.edu.vn.mytasklist.repository;

import android.app.Application;

import androidx.lifecycle.LiveData;

import java.util.List;

import hcmute.edu.vn.mytasklist.data.AppDatabase;
import hcmute.edu.vn.mytasklist.data.dao.TaskDao;
import hcmute.edu.vn.mytasklist.data.entity.TaskEntity;

public class TaskRepository {

    private final TaskDao taskDao;
    private final hcmute.edu.vn.mytasklist.data.dao.CategoryDao categoryDao;
    private final LiveData<List<TaskEntity>> allTasks;
    private final LiveData<List<hcmute.edu.vn.mytasklist.data.entity.CategoryEntity>> allCategories;
    private final Application application;

    public TaskRepository(Application application) {
        this.application = application;
        AppDatabase db = AppDatabase.getDatabase(application);
        taskDao = db.taskDao();
        categoryDao = db.categoryDao();
        allTasks = taskDao.getAllTasksLiveData();
        allCategories = categoryDao.getAllCategoriesLiveData();
    }

    public LiveData<List<TaskEntity>> getAllTasks() {
        return allTasks;
    }

    public LiveData<List<hcmute.edu.vn.mytasklist.data.entity.CategoryEntity>> getAllCategories() {
        return allCategories;
    }

    public void insert(TaskEntity task) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            long id = taskDao.insert(task);
            task.setId(id);
            hcmute.edu.vn.mytasklist.service.reminder.TaskReminderHelper.scheduleReminder(application, task);
            hcmute.edu.vn.mytasklist.service.deadline.DeadlineWarningHelper.scheduleDeadlineWarnings(application, task);
        });
    }

    public void update(TaskEntity task) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            taskDao.update(task);
            // Re-schedule alarms
            hcmute.edu.vn.mytasklist.service.reminder.TaskReminderHelper.scheduleReminder(application, task);
            hcmute.edu.vn.mytasklist.service.deadline.DeadlineWarningHelper.cancelDeadlineWarnings(application, task);
            hcmute.edu.vn.mytasklist.service.deadline.DeadlineWarningHelper.scheduleDeadlineWarnings(application, task);
        });
    }

    public void delete(TaskEntity task) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            taskDao.delete(task);
            hcmute.edu.vn.mytasklist.service.reminder.TaskReminderHelper.cancelReminder(application, task.getId());
            hcmute.edu.vn.mytasklist.service.deadline.DeadlineWarningHelper.cancelDeadlineWarnings(application, task);
        });
    }

    public void updateCompleted(long id, boolean completed) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            taskDao.updateCompleted(id, completed);
            if (completed) {
                hcmute.edu.vn.mytasklist.service.reminder.TaskReminderHelper.cancelReminder(application, id);
                TaskEntity task = taskDao.getTaskById(id);
                if(task != null) {
                    hcmute.edu.vn.mytasklist.service.deadline.DeadlineWarningHelper.cancelDeadlineWarnings(application, task);
                }
            }
        });
    }

    public void insertCategory(hcmute.edu.vn.mytasklist.data.entity.CategoryEntity category) {
        AppDatabase.databaseWriteExecutor.execute(() -> categoryDao.insert(category));
    }

    public void updateCategory(hcmute.edu.vn.mytasklist.data.entity.CategoryEntity category) {
        AppDatabase.databaseWriteExecutor.execute(() -> categoryDao.update(category));
    }

    public void deleteCategory(hcmute.edu.vn.mytasklist.data.entity.CategoryEntity category) {
        AppDatabase.databaseWriteExecutor.execute(() -> categoryDao.delete(category));
    }

    public void updateCategoryToInbox(String oldCategory) {
        AppDatabase.databaseWriteExecutor.execute(() -> taskDao.updateCategoryToInbox(oldCategory));
    }
}
