const fs = require('fs');
let content = fs.readFileSync('app/src/main/java/hcmute/edu/vn/mytasklist/MainActivity.java', 'utf8');

// Imports
content = content.replace(/import java\.util\.Date;/g, "import java.util.Date;\nimport hcmute.edu.vn.mytasklist.data.entity.TaskEntity;\nimport hcmute.edu.vn.mytasklist.data.entity.CategoryEntity;\nimport hcmute.edu.vn.mytasklist.viewmodel.TaskViewModel;\nimport androidx.lifecycle.ViewModelProvider;");

// ViewModel instance
content = content.replace(/private TaskDao taskDao;\n    private CategoryDao categoryDao;/g, "private TaskViewModel taskViewModel;\n    private List<TaskEntity> allTasks = new ArrayList<>();\n    private List<CategoryEntity> allCategories = new ArrayList<>();");

// Initialization
content = content.replace(/taskDao    = new TaskDao\(this\);\n        categoryDao = new CategoryDao\(this\);/g, "taskViewModel = new ViewModelProvider(this).get(TaskViewModel.class);\n        taskViewModel.getAllTasks().observe(this, tasks -> {\n            allTasks = tasks;\n            updateUI();\n        });\n        taskViewModel.getAllCategories().observe(this, cats -> {\n            allCategories = cats;\n            buildNav();\n        });");

// Types
content = content.replace(/List<Category> categories/g, "List<CategoryEntity> categories");
content = content.replace(/Category cat/g, "CategoryEntity cat");
content = content.replace(/Category\(/g, "CategoryEntity(");
content = content.replace(/List<Category>/g, "List<CategoryEntity>");
content = content.replace(/Task /g, "TaskEntity ");
content = content.replace(/List<Task>/g, "List<TaskEntity>");
content = content.replace(/Task\(/g, "TaskEntity(");
content = content.replace(/Task\.Priority/g, "String");
content = content.replace(/TaskEntity\.Priority\.HIGH/g, "\"HIGH\"");
content = content.replace(/TaskEntity\.Priority\.MEDIUM/g, "\"MEDIUM\"");
content = content.replace(/TaskEntity\.Priority\.LOW/g, "\"LOW\"");
content = content.replace(/TaskEntity\.Priority\.NONE/g, "\"NONE\"");
content = content.replace(/TaskEntity tv/g, "TextView tv"); // Fix regex overreach
content = content.replace(/List<TaskEntity> filtered = getFilteredTasks\(\);/g, "List<TaskEntity> filtered = getFilteredTasks();"); // This is fine

// DAOs to ViewModel
content = content.replace(/categoryDao\.insert\(/g, "taskViewModel.insertCategory(");
content = content.replace(/categoryDao\.update\(/g, "taskViewModel.updateCategory(");
content = content.replace(/categoryDao\.delete\(cat\.getId\(\)\)/g, "taskViewModel.deleteCategory(cat)");
content = content.replace(/categoryDao\.getAll\(\)/g, "allCategories");

content = content.replace(/taskDao\.insert\(/g, "taskViewModel.insert(");
content = content.replace(/taskDao\.update\(/g, "taskViewModel.update(");
content = content.replace(/taskDao\.delete\(task\.getId\(\)\)/g, "taskViewModel.delete(task)");
content = content.replace(/taskDao\.updateCompleted\(task\.getId\(\), task\.isCompleted\(\)\)/g, "taskViewModel.updateCompleted(task.getId(), task.isCompleted())");
content = content.replace(/taskDao\.updateCategoryToInbox\(/g, "taskViewModel.updateCategoryToInbox(");
content = content.replace(/taskDao\.getAll\(\)\.isEmpty\(\)/g, "allTasks.isEmpty()"); 
// Note: allTasks might not be populated in onCreate immediately, we should remove the insertSampleData check or handle it. Let's just remove that if block because Room already has pre-population!
content = content.replace(/if \(allTasks\.isEmpty\(\)\) \{\n            insertSampleData\(\);\n        \}/g, "// Sample data is managed via AppDatabase RoomCallback");
content = content.replace(/insertSampleData\(\);/g, "// Sample data managed by Room Callback");

// Remove TaskNotificationService intents
content = content.replace(/sendRemoveTaskIntent\(task\.getId\(\)\);/g, "");
content = content.replace(/sendAddTaskIntent\(task\.getId\(\), task\.getTitle\(\)\);/g, "");
content = content.replace(/sendAddTaskIntent\(newTask\.getId\(\), newTask\.getTitle\(\)\);/g, "");
content = content.replace(/private void sendAddTaskIntent.*?\}\n/gs, "");
content = content.replace(/private void sendRemoveTaskIntent.*?\}\n/gs, "");

// getFilteredTasks rewrite
let filteredTasksCode = private List<TaskEntity> getFilteredTasks() {
        List<TaskEntity> result = new ArrayList<>();
        Date todayStart = getDayStart(new Date());
        Calendar c = Calendar.getInstance(); c.add(Calendar.DAY_OF_YEAR, 1);
        Date tomorrowStart = getDayStart(c.getTime());
        Calendar c7 = Calendar.getInstance(); c7.add(Calendar.DAY_OF_YEAR, 7);
        Date nextWeekStart = getDayStart(c7.getTime());
        
        for (TaskEntity t : allTasks) {
            boolean include = false;
            if ("Today".equals(currentFilter)) {
                if (t.getDueDate() != null && getDayStart(t.getDueDate()).equals(todayStart)) include = true;
            } else if ("Next 7 Days".equals(currentFilter)) {
                if (t.getDueDate() != null && !getDayStart(t.getDueDate()).before(todayStart) && getDayStart(t.getDueDate()).before(nextWeekStart)) include = true;
            } else if ("Completed".equals(currentFilter)) {
                if (t.isCompleted()) include = true;
            } else {
                if (currentFilter.equals(t.getCategory())) include = true;
            }
            if (include) result.add(t);
        }
        return result;
    };
content = content.replace(/private List<TaskEntity> getFilteredTasks\(\) \{.*?\n    \}/gs, filteredTasksCode);

fs.writeFileSync('app/src/main/java/hcmute/edu/vn/mytasklist/MainActivity.java', content);
console.log('Modification complete.');
