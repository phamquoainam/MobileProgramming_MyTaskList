package hcmute.edu.vn.mytasklist.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import java.util.List;

import hcmute.edu.vn.mytasklist.data.entity.TaskEntity;
import hcmute.edu.vn.mytasklist.repository.TaskRepository;

public class TaskViewModel extends AndroidViewModel {

    private final TaskRepository repository;
    private final LiveData<List<TaskEntity>> allTasks;
    private final LiveData<List<hcmute.edu.vn.mytasklist.data.entity.CategoryEntity>> allCategories;

    public TaskViewModel(@NonNull Application application) {
        super(application);
        repository = new TaskRepository(application);
        allTasks = repository.getAllTasks();
        allCategories = repository.getAllCategories();
    }

    public LiveData<List<TaskEntity>> getAllTasks() {
        return allTasks;
    }

    public void insert(TaskEntity task) {
        repository.insert(task);
    }

    public void update(TaskEntity task) {
        repository.update(task);
    }

    public void delete(TaskEntity task) {
        repository.delete(task);
    }

    public void updateCompleted(long id, boolean completed) {
        repository.updateCompleted(id, completed);
    }

    public LiveData<List<hcmute.edu.vn.mytasklist.data.entity.CategoryEntity>> getAllCategories() {
        return allCategories;
    }

    public void insertCategory(hcmute.edu.vn.mytasklist.data.entity.CategoryEntity category) {
        repository.insertCategory(category);
    }

    public void updateCategory(hcmute.edu.vn.mytasklist.data.entity.CategoryEntity category) {
        repository.updateCategory(category);
    }

    public void deleteCategory(hcmute.edu.vn.mytasklist.data.entity.CategoryEntity category) {
        repository.deleteCategory(category);
    }

    public void updateCategoryToInbox(String oldCategory) {
        repository.updateCategoryToInbox(oldCategory);
    }
}
