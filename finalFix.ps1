$content = Get-Content -Path 'app\src\main\java\hcmute\edu\vn\mytasklist\MainActivity.java' -Raw

# Remove method implementations
$content = $content -replace '(?s)private void sendAddTaskIntent.*?\}', ''
$content = $content -replace '(?s)private void sendRemoveTaskIntent.*?\}', ''

# Remove calls to those methods
$content = $content -replace 'sendRemoveTaskIntent\(.*?\);', ''
$content = $content -replace 'sendAddTaskIntent\(.*?\);', ''

# Add Pomodoro click listener setup
$setupRvPattern = 'taskAdapter\.setOnTaskLongClickListener\(this::showEditTaskDialog\);'
$setupRvReplacement = "taskAdapter.setOnTaskLongClickListener(this::showEditTaskDialog);
        taskAdapter.setOnPomodoroClickListener(task -> {
            Intent intent = new Intent(this, hcmute.edu.vn.mytasklist.service.pomodoro.PomodoroForegroundService.class);
            intent.setAction(hcmute.edu.vn.mytasklist.service.pomodoro.PomodoroForegroundService.ACTION_START);
            intent.putExtra(hcmute.edu.vn.mytasklist.service.pomodoro.PomodoroForegroundService.EXTRA_TASK_ID, task.getId());
            intent.putExtra(hcmute.edu.vn.mytasklist.service.pomodoro.PomodoroForegroundService.EXTRA_TASK_TITLE, task.getTitle());
            intent.putExtra(hcmute.edu.vn.mytasklist.service.pomodoro.PomodoroForegroundService.EXTRA_DURATION, 25L * 60 * 1000);
            androidx.core.content.ContextCompat.startForegroundService(this, intent);
        });"
$content = $content -replace [regex]::Escape($setupRvPattern), $setupRvReplacement

Set-Content -Path 'app\src\main\java\hcmute\edu\vn\mytasklist\MainActivity.java' -Value $content
