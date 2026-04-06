package hcmute.edu.vn.mytasklist;

import androidx.appcompat.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.text.SimpleDateFormat;

import android.Manifest;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.pm.PackageManager;
import android.os.Build;
import androidx.core.app.ActivityCompat;
import androidx.lifecycle.ViewModelProvider;

import hcmute.edu.vn.mytasklist.data.entity.TaskEntity;
import hcmute.edu.vn.mytasklist.data.entity.CategoryEntity;
import hcmute.edu.vn.mytasklist.data.AppDatabase;
import hcmute.edu.vn.mytasklist.viewmodel.TaskViewModel;

public class MainActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private Toolbar toolbar;
    private TextView tvToolbarTitle;
    private TextView tvTaskCount;
    private RecyclerView rvTasks;
    private FloatingActionButton fabAdd;
    private View layoutEmpty;
    private TaskAdapter taskAdapter;

    // ViewModel
    private TaskViewModel taskViewModel;
    private List<TaskEntity> allTasks = new ArrayList<>();
    private List<CategoryEntity> allCategories = new ArrayList<>();

    private String currentFilter = "Next 7 Days";
    private LinearLayout navContainer;

    // Color options for categories
    private static final String[] COLOR_OPTIONS = {
        "#E53935","#FB8C00","#F9A825","#43A047",
        "#26A69A","#1E88E5","#5C6BC0","#8E24AA",
        "#EC407A","#6D4C41","#546E7A","#9E9E9E"
    };
    private static final String[] COLOR_NAMES = {
        "Red","Orange","Yellow","Green",
        "Teal","Blue","Indigo","Purple",
        "Pink","Brown","Gray-Blue","Gray"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        taskViewModel = new ViewModelProvider(this).get(TaskViewModel.class);
        taskViewModel.getAllTasks().observe(this, tasks -> { allTasks = tasks; updateUI(); });
        taskViewModel.getAllCategories().observe(this, cats -> { allCategories = cats; buildNav(); });

        bindViews();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }

        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                } else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                    setEnabled(true);
                }
            }
        });

        setupToolbar();
        setupDrawer();
        setupRecyclerView();
        setupFab();
        updateUI();
    }

    private void bindViews() {
        drawerLayout   = findViewById(R.id.drawerLayout);
        toolbar        = findViewById(R.id.toolbar);
        tvToolbarTitle = findViewById(R.id.tvToolbarTitle);
        tvTaskCount    = findViewById(R.id.tvTaskCount);
        rvTasks        = findViewById(R.id.rvTasks);
        fabAdd         = findViewById(R.id.fabAdd);
        layoutEmpty    = findViewById(R.id.layoutEmpty);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null)
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        toolbar.setNavigationOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));
    }

    // ════════════════════════════════════════════════════════════
    //  SIDEBAR NAVIGATION
    // ════════════════════════════════════════════════════════════

    private void setupDrawer() {
        NavigationView navView = findViewById(R.id.navView);
        navView.removeAllViews();

        ScrollView scrollView = new ScrollView(this);
        navContainer = new LinearLayout(this);
        navContainer.setOrientation(LinearLayout.VERTICAL);
        navContainer.setPadding(0, dp(8), 0, dp(24));

        buildNav();

        scrollView.addView(navContainer);
        navView.addView(scrollView);
    }

    private void buildNav() {
        if (navContainer == null) return;
        navContainer.removeAllViews();

        // Smart lists
        addNavSection("SMART LISTS", false);
        addNavRow("Today",       R.drawable.ic_today,  "#E53935", "Today",       currentFilter.equals("Today"));
        addNavRow("Next 7 Days", R.drawable.ic_week,   "#4A90D9", "Next 7 Days", currentFilter.equals("Next 7 Days"));
        addNavRow("Inbox",       R.drawable.ic_inbox,  "#9C27B0", "Inbox",       currentFilter.equals("Inbox"));

        // My Lists
        addNavSection("MY LISTS", true);
        for (CategoryEntity cat : allCategories) {
            if ("Inbox".equalsIgnoreCase(cat.getName())) continue;
            addCategoryRow(cat);
        }

        // Completed
        addNavSection("", false);
        addNavRow("Completed", R.drawable.ic_check, "#9E9E9E", "Completed", currentFilter.equals("Completed"));
    }

    private void addNavSection(String label, boolean showAddBtn) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(20), dp(16), dp(16), dp(4));

        TextView tv = new TextView(this);
        tv.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        tv.setText(label);
        tv.setTextColor(ContextCompat.getColor(this, R.color.text_hint));
        tv.setTextSize(11f);
        tv.setLetterSpacing(0.1f);
        row.addView(tv);

        if (showAddBtn) {
            TextView btnAdd = new TextView(this);
            btnAdd.setText("＋");
            btnAdd.setTextColor(ContextCompat.getColor(this, R.color.ticktick_red));
            btnAdd.setTextSize(18f);
            btnAdd.setPadding(dp(16), dp(8), dp(16), dp(8));
            btnAdd.setClickable(true);
            btnAdd.setFocusable(true);
            btnAdd.setOnClickListener(v -> showAddCategoryDialog());
            row.addView(btnAdd);
        }

        navContainer.addView(row);
    }

    private void addNavRow(String label, int iconRes, String colorHex, String filter, boolean selected) {
        TextView row = makeNavRow(label, iconRes, colorHex, selected);
        row.setOnClickListener(v -> {
            currentFilter = filter;
            tvToolbarTitle.setText(label);
            drawerLayout.closeDrawer(GravityCompat.START);
            buildNav();
            updateUI();
        });
        navContainer.addView(row);
    }

    private void addCategoryRow(CategoryEntity cat) {
        LinearLayout rowLayout = new LinearLayout(this);
        rowLayout.setOrientation(LinearLayout.HORIZONTAL);
        rowLayout.setGravity(Gravity.CENTER_VERTICAL);
        rowLayout.setMinimumHeight(dp(48));

        TextView nameView = new TextView(this);
        nameView.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        nameView.setText(cat.getName());
        nameView.setTextSize(14f);
        nameView.setPadding(dp(20), dp(12), dp(8), dp(12));
        nameView.setGravity(Gravity.CENTER_VERTICAL);

        try {
            Drawable icon = ContextCompat.getDrawable(this, R.drawable.ic_list);
            if (icon != null) {
                int sz = dp(20);
                icon.setBounds(0, 0, sz, sz);
                icon.setTint(Color.parseColor(cat.getColor()));
                nameView.setCompoundDrawables(icon, null, null, null);
                nameView.setCompoundDrawablePadding(dp(16));
            }
        } catch (Exception ignored) {}

        if (currentFilter.equals(cat.getName())) {
            nameView.setTextColor(ContextCompat.getColor(this, R.color.nav_selected_text));
            rowLayout.setBackground(ContextCompat.getDrawable(this, R.drawable.nav_selected_bg));
        } else {
            nameView.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
            rowLayout.setBackgroundColor(Color.TRANSPARENT);
        }

        nameView.setOnClickListener(v -> {
            currentFilter = cat.getName();
            tvToolbarTitle.setText(cat.getName());
            drawerLayout.closeDrawer(GravityCompat.START);
            buildNav();
            updateUI();
        });

        TextView btnEdit = new TextView(this);
        btnEdit.setText("✏️");
        btnEdit.setTextSize(15f);
        btnEdit.setPadding(dp(12), dp(12), dp(16), dp(12));
        btnEdit.setClickable(true);
        btnEdit.setFocusable(true);
        btnEdit.setOnClickListener(v -> showEditCategoryDialog(cat));

        rowLayout.addView(nameView);
        rowLayout.addView(btnEdit);
        navContainer.addView(rowLayout);
    }

    private TextView makeNavRow(String label, int iconRes, String colorHex, boolean selected) {
        TextView row = new TextView(this);
        row.setText(label);
        row.setTextSize(14f);
        row.setPadding(dp(20), 0, dp(20), 0);
        row.setMinHeight(dp(48));
        row.setGravity(Gravity.CENTER_VERTICAL);

        try {
            Drawable icon = ContextCompat.getDrawable(this, iconRes);
            if (icon != null) {
                int sz = dp(20);
                icon.setBounds(0, 0, sz, sz);
                icon.setTint(Color.parseColor(colorHex));
                row.setCompoundDrawables(icon, null, null, null);
                row.setCompoundDrawablePadding(dp(16));
            }
        } catch (Exception ignored) {}

        if (selected) {
            row.setTextColor(ContextCompat.getColor(this, R.color.nav_selected_text));
            row.setBackground(ContextCompat.getDrawable(this, R.drawable.nav_selected_bg));
        } else {
            row.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
            row.setBackgroundColor(Color.TRANSPARENT);
        }
        return row;
    }

    // ════════════════════════════════════════════════════════════
    //  CategoryEntity CRUD DIALOGS
    // ════════════════════════════════════════════════════════════

    private void showAddCategoryDialog() {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_category, null);
        EditText etName  = view.findViewById(R.id.etCategoryName);
        Spinner  spinner = view.findViewById(R.id.spinnerCategoryColor);
        setupColorSpinner(spinner, "#5C6BC0");

        new AlertDialog.Builder(this)
            .setTitle("New List")
            .setView(view)
            .setPositiveButton("Add", (d, w) -> {
                String name = etName.getText().toString().trim();
                if (!name.isEmpty()) {
                    String color = COLOR_OPTIONS[spinner.getSelectedItemPosition()];
                    taskViewModel.insertCategory(new CategoryEntity(name, color));
                    Toast.makeText(this, "Đã tạo danh sách \"" + name + "\"", Toast.LENGTH_SHORT).show();
                }
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void showEditCategoryDialog(CategoryEntity cat) {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_category, null);
        EditText etName  = view.findViewById(R.id.etCategoryName);
        Spinner  spinner = view.findViewById(R.id.spinnerCategoryColor);
        etName.setText(cat.getName());

        int colorIdx = 0;
        for (int i = 0; i < COLOR_OPTIONS.length; i++) {
            if (COLOR_OPTIONS[i].equalsIgnoreCase(cat.getColor())) { colorIdx = i; break; }
        }
        setupColorSpinner(spinner, cat.getColor());
        spinner.setSelection(colorIdx);

        new AlertDialog.Builder(this)
            .setTitle("Edit List")
            .setView(view)
            .setPositiveButton("Save", (d, w) -> {
                String name = etName.getText().toString().trim();
                if (!name.isEmpty()) {
                    cat.setName(name);
                    cat.setColor(COLOR_OPTIONS[spinner.getSelectedItemPosition()]);
                    taskViewModel.updateCategory(cat);
                    if (currentFilter.equals(cat.getName())) tvToolbarTitle.setText(name);
                    Toast.makeText(this, "Đã cập nhật danh sách", Toast.LENGTH_SHORT).show();
                }
            })
            .setNeutralButton("Delete", (d, w) -> {
                taskViewModel.deleteCategory(cat);
                taskViewModel.updateCategoryToInbox(cat.getName());
                if (currentFilter.equals(cat.getName())) {
                    currentFilter = "Next 7 Days";
                    tvToolbarTitle.setText("Next 7 Days");
                }
                Toast.makeText(this, "Đã xóa danh sách \"" + cat.getName() + "\"", Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void setupColorSpinner(Spinner spinner, String selected) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, COLOR_NAMES);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
    }

    // ════════════════════════════════════════════════════════════
    //  RECYCLERVIEW + ADAPTER
    // ════════════════════════════════════════════════════════════

    private void setupRecyclerView() {
        taskAdapter = new TaskAdapter(task -> {
            taskViewModel.updateCompleted(task.getId(), task.isCompleted());
            if (task.isCompleted()) {
                showActionNotification("Hoàn thành Task", "Tuyệt vời! Bạn đã hoàn thành: " + task.getTitle(), android.R.drawable.ic_dialog_info);
                // Handle recurring tasks
                if (!"NONE".equals(task.getRecurrence())) {
                    cloneRecurringTask(task);
                }
            }
            updateUI();
        });
        taskAdapter.setOnTaskLongClickListener(this::showEditTaskDialog);

        // Wire up Pomodoro click listener
        taskAdapter.setOnPomodoroClickListener(task -> {
            showPomodoroSettingsDialog(task);
        });

        rvTasks.setLayoutManager(new LinearLayoutManager(this));
        rvTasks.setAdapter(taskAdapter);
        rvTasks.addItemDecoration(new DividerItemDecoration(this, LinearLayoutManager.VERTICAL));
    }

    private void setupFab() {
        fabAdd.setClickable(true);
        fabAdd.setFocusable(true);
        fabAdd.setOnClickListener(v -> showAddTaskDialog());
    }

    // ════════════════════════════════════════════════════════════
    //  RECURRING TASK LOGIC
    // ════════════════════════════════════════════════════════════

    /**
     * Khi một task lặp lại được hoàn thành, tạo bản sao với ngày mới.
     * Nếu là CUSTOM và đã qua ngày kết thúc thì không tạo nữa.
     */
    private void cloneRecurringTask(TaskEntity completedTask) {
        String recurrence = completedTask.getRecurrence();
        if (recurrence == null || "NONE".equals(recurrence)) return;

        Calendar cal = Calendar.getInstance();
        if (completedTask.getDueDate() != null) {
            cal.setTimeInMillis(completedTask.getDueDate());
        }

        if ("DAILY".equals(recurrence)) {
            cal.add(Calendar.DAY_OF_YEAR, 1);
        } else if ("WEEKLY".equals(recurrence)) {
            cal.add(Calendar.WEEK_OF_YEAR, 1);
        } else if ("MONTHLY".equals(recurrence)) {
            cal.add(Calendar.MONTH, 1);
        } else if (recurrence.startsWith("CUSTOM:")) {
            String[] parts = recurrence.split(":");
            if (parts.length >= 3) {
                int days = 1;
                try { days = Integer.parseInt(parts[1]); } catch (NumberFormatException ignored) {}
                long endMs = 0;
                try { endMs = Long.parseLong(parts[2]); } catch (NumberFormatException ignored) {}

                cal.add(Calendar.DAY_OF_YEAR, days);

                // Check if the new date exceeds the end date
                if (endMs > 0 && cal.getTimeInMillis() > endMs) {
                    Toast.makeText(this, "Chuỗi lặp đã kết thúc cho: " + completedTask.getTitle(), Toast.LENGTH_SHORT).show();
                    return; // Don't clone — recurrence period ended
                }
            }
        } else {
            return; // Unknown recurrence type
        }

        TaskEntity newTask = new TaskEntity(
                completedTask.getTitle(),
                cal.getTimeInMillis(),
                completedTask.getPriority(),
                completedTask.getCategory()
        );
        newTask.setRecurrence(recurrence);
        // Set reminder time relative to new due date (same offset as original)
        if (completedTask.getReminderTime() != null && completedTask.getDueDate() != null) {
            long offset = completedTask.getDueDate() - completedTask.getReminderTime();
            newTask.setReminderTime(cal.getTimeInMillis() - offset);
        }
        newTask.setReminderOffsets(completedTask.getReminderOffsets());

        taskViewModel.insert(newTask);
        Toast.makeText(this, "🔁 Đã tạo task lặp lại: " + newTask.getTitle(), Toast.LENGTH_SHORT).show();
    }

    // ════════════════════════════════════════════════════════════
    //  TaskEntity CRUD DIALOGS
    // ════════════════════════════════════════════════════════════

    private void showAddTaskDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_task, null);
        EditText etTitle   = dialogView.findViewById(R.id.etTaskTitle);
        etTitle.setRawInputType(android.text.InputType.TYPE_CLASS_TEXT);
        Spinner spinnerCat = dialogView.findViewById(R.id.spinnerAddCategory);

        // Category spinner
        List<String> catNames = new ArrayList<>();
        catNames.add("Inbox");
        for (CategoryEntity c : allCategories) {
            if (!"Inbox".equalsIgnoreCase(c.getName())) {
                catNames.add(c.getName());
            }
        }

        ArrayAdapter<String> catAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, catNames);
        catAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCat.setAdapter(catAdapter);

        int preSelect = 0;
        for (int i = 0; i < catNames.size(); i++) {
            if (catNames.get(i).equals(currentFilter)) { preSelect = i; break; }
        }
        spinnerCat.setSelection(preSelect);

        // Priority buttons
        com.google.android.material.button.MaterialButton btnHigh =
                dialogView.findViewById(R.id.btnPriorityHigh);
        com.google.android.material.button.MaterialButton btnMed =
                dialogView.findViewById(R.id.btnPriorityMed);
        com.google.android.material.button.MaterialButton btnLow =
                dialogView.findViewById(R.id.btnPriorityLow);
        com.google.android.material.button.MaterialButton btnNone =
                dialogView.findViewById(R.id.btnPriorityNone);

        final String[] selectedPriority = {"NONE"};
        updatePriorityUI(btnHigh, btnMed, btnLow, btnNone, selectedPriority[0]);

        btnHigh.setOnClickListener(v -> { selectedPriority[0] = "HIGH";   updatePriorityUI(btnHigh, btnMed, btnLow, btnNone, selectedPriority[0]); });
        btnMed .setOnClickListener(v -> { selectedPriority[0] = "MEDIUM"; updatePriorityUI(btnHigh, btnMed, btnLow, btnNone, selectedPriority[0]); });
        btnLow .setOnClickListener(v -> { selectedPriority[0] = "LOW";    updatePriorityUI(btnHigh, btnMed, btnLow, btnNone, selectedPriority[0]); });
        btnNone.setOnClickListener(v -> { selectedPriority[0] = "NONE";   updatePriorityUI(btnHigh, btnMed, btnLow, btnNone, selectedPriority[0]); });

        AlertDialog dialog = new AlertDialog.Builder(this).setView(dialogView).create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            dialog.getWindow().setSoftInputMode(
                android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE |
                android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        }

        // Date & Time setup
        final Calendar selectedDateTime = Calendar.getInstance();
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());

        TextView tvDate = dialogView.findViewById(R.id.tvDate);
        TextView tvTime = dialogView.findViewById(R.id.tvTime);
        tvDate.setText(dateFormat.format(selectedDateTime.getTime()));
        tvTime.setText(timeFormat.format(selectedDateTime.getTime()));

        dialogView.findViewById(R.id.layoutDate).setOnClickListener(v -> {
            new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
                selectedDateTime.set(Calendar.YEAR, year);
                selectedDateTime.set(Calendar.MONTH, month);
                selectedDateTime.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                tvDate.setText(dateFormat.format(selectedDateTime.getTime()));
            }, selectedDateTime.get(Calendar.YEAR), selectedDateTime.get(Calendar.MONTH), selectedDateTime.get(Calendar.DAY_OF_MONTH)).show();
        });

        dialogView.findViewById(R.id.layoutTime).setOnClickListener(v -> {
            new TimePickerDialog(this, (view, hourOfDay, minute) -> {
                selectedDateTime.set(Calendar.HOUR_OF_DAY, hourOfDay);
                selectedDateTime.set(Calendar.MINUTE, minute);
                tvTime.setText(timeFormat.format(selectedDateTime.getTime()));
            }, selectedDateTime.get(Calendar.HOUR_OF_DAY), selectedDateTime.get(Calendar.MINUTE), true).show();
        });

        // Recurrence spinner
        Spinner spinnerRecurrence = dialogView.findViewById(R.id.spinnerRecurrence);
        LinearLayout layoutCustomRecurrence = dialogView.findViewById(R.id.layoutCustomRecurrence);
        EditText etCustomDays = dialogView.findViewById(R.id.etCustomDays);
        TextView tvCustomEndDate = dialogView.findViewById(R.id.tvCustomEndDate);

        String[] recOptions = {"Không lặp", "Hàng ngày", "Hàng tuần", "Hàng tháng", "Tùy chỉnh"};
        ArrayAdapter<String> recAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, recOptions);
        recAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerRecurrence.setAdapter(recAdapter);

        spinnerRecurrence.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int pos, long id) {
                layoutCustomRecurrence.setVisibility(pos == 4 ? View.VISIBLE : View.GONE);
            }
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });

        final Date[] customEndDate = {null};
        tvCustomEndDate.setOnClickListener(v -> {
            Calendar c = Calendar.getInstance();
            if (customEndDate[0] != null) c.setTime(customEndDate[0]);
            new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
                Calendar c2 = Calendar.getInstance();
                c2.set(year, month, dayOfMonth, 23, 59, 59);
                customEndDate[0] = c2.getTime();
                tvCustomEndDate.setText(dateFormat.format(customEndDate[0]));
            }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
        });

        dialogView.findViewById(R.id.btnCancel).setOnClickListener(v -> dialog.dismiss());
        dialogView.findViewById(R.id.btnSave).setOnClickListener(v -> {
            String title = etTitle.getText().toString().trim();
            if (!title.isEmpty()) {
                String chosenCat = catNames.get(spinnerCat.getSelectedItemPosition());
                TaskEntity newTask = new TaskEntity(title, selectedDateTime.getTime().getTime(), selectedPriority[0], chosenCat);

                // Set reminderTime = dueDate so that the reminder fires at due time
                newTask.setReminderTime(selectedDateTime.getTime().getTime());
                
                // Khởi tạo nhiều mốc thời gian nhắc nhở trước deadline (Deadline Warning Service)
                // Theo mặc định: Nhắc trước 1 giờ (3600000 ms) và 10 phút (600000 ms)
                newTask.setReminderOffsets("[3600000, 600000]");

                int rp = spinnerRecurrence.getSelectedItemPosition();
                String rs = "NONE";
                if (rp == 1) rs = "DAILY";
                else if (rp == 2) rs = "WEEKLY";
                else if (rp == 3) rs = "MONTHLY";
                else if (rp == 4) {
                    String d = etCustomDays.getText().toString();
                    if (d.isEmpty()) d = "1";
                    long ems = customEndDate[0] != null ? customEndDate[0].getTime() : 0;
                    rs = "CUSTOM:" + d + ":" + ems;
                }
                newTask.setRecurrence(rs);

                taskViewModel.insert(newTask);
                
                String dateStr = dateFormat.format(selectedDateTime.getTime()) + " " + timeFormat.format(selectedDateTime.getTime());
                showActionNotification("Tạo mới Task", "Đã thêm: " + title + " (" + dateStr + ")", android.R.drawable.ic_menu_add);
                
                updateUI();
                dialog.dismiss();
            } else {
                etTitle.setError("Vui lòng nhập tên công việc");
            }
        });
        dialog.setOnShowListener(d -> etTitle.requestFocus());
        dialog.show();
    }

    private void showEditTaskDialog(TaskEntity task) {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_edit_task, null);
        EditText etTitle = view.findViewById(R.id.etEditTitle);
        etTitle.setRawInputType(
            android.text.InputType.TYPE_CLASS_TEXT |
            android.text.InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        Spinner spinner = view.findViewById(R.id.spinnerEditCategory);
        etTitle.setText(task.getTitle());

        // Category spinner
        List<String> catNames = new ArrayList<>();
        catNames.add("Inbox");
        for (int i = 0; i < allCategories.size(); i++) {
            if (!"Inbox".equalsIgnoreCase(allCategories.get(i).getName())) {
                catNames.add(allCategories.get(i).getName());
            }
        }
        int selectedIdx = 0;
        for (int i = 0; i < catNames.size(); i++) {
            if (catNames.get(i).equals(task.getCategory())) selectedIdx = i;
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, catNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setSelection(selectedIdx);

        // Priority buttons
        com.google.android.material.button.MaterialButton btnHigh =
                view.findViewById(R.id.btnEditPriHigh);
        com.google.android.material.button.MaterialButton btnMed =
                view.findViewById(R.id.btnEditPriMed);
        com.google.android.material.button.MaterialButton btnLow =
                view.findViewById(R.id.btnEditPriLow);
        com.google.android.material.button.MaterialButton btnNone =
                view.findViewById(R.id.btnEditPriNone);

        final String[] pri = {task.getPriority()};
        updatePriorityUI(btnHigh, btnMed, btnLow, btnNone, pri[0]);

        btnHigh.setOnClickListener(v -> { pri[0] = "HIGH";   updatePriorityUI(btnHigh, btnMed, btnLow, btnNone, pri[0]); });
        btnMed .setOnClickListener(v -> { pri[0] = "MEDIUM"; updatePriorityUI(btnHigh, btnMed, btnLow, btnNone, pri[0]); });
        btnLow .setOnClickListener(v -> { pri[0] = "LOW";    updatePriorityUI(btnHigh, btnMed, btnLow, btnNone, pri[0]); });
        btnNone.setOnClickListener(v -> { pri[0] = "NONE";   updatePriorityUI(btnHigh, btnMed, btnLow, btnNone, pri[0]); });

        AlertDialog dialog = new AlertDialog.Builder(this).setView(view).create();
        if (dialog.getWindow() != null)
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        // Date & Time setup
        final Calendar selectedDateTime = Calendar.getInstance();
        if (task.getDueDate() != null) {
            selectedDateTime.setTime(new Date(task.getDueDate()));
        }
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());

        TextView tvDate = view.findViewById(R.id.tvEditDate);
        TextView tvTime = view.findViewById(R.id.tvEditTime);
        tvDate.setText(dateFormat.format(selectedDateTime.getTime()));
        tvTime.setText(timeFormat.format(selectedDateTime.getTime()));

        view.findViewById(R.id.layoutEditDate).setOnClickListener(v -> {
            new DatePickerDialog(this, (dialogView, year, month, dayOfMonth) -> {
                selectedDateTime.set(Calendar.YEAR, year);
                selectedDateTime.set(Calendar.MONTH, month);
                selectedDateTime.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                tvDate.setText(dateFormat.format(selectedDateTime.getTime()));
            }, selectedDateTime.get(Calendar.YEAR), selectedDateTime.get(Calendar.MONTH), selectedDateTime.get(Calendar.DAY_OF_MONTH)).show();
        });

        view.findViewById(R.id.layoutEditTime).setOnClickListener(v -> {
            new TimePickerDialog(this, (dialogView, hourOfDay, minute) -> {
                selectedDateTime.set(Calendar.HOUR_OF_DAY, hourOfDay);
                selectedDateTime.set(Calendar.MINUTE, minute);
                tvTime.setText(timeFormat.format(selectedDateTime.getTime()));
            }, selectedDateTime.get(Calendar.HOUR_OF_DAY), selectedDateTime.get(Calendar.MINUTE), true).show();
        });

        // Recurrence spinner
        Spinner spinnerRecedit = view.findViewById(R.id.spinnerEditRecurrence);
        LinearLayout layoutCustomedit = view.findViewById(R.id.layoutEditCustomRecurrence);
        EditText etCustomDaysedit = view.findViewById(R.id.etEditCustomDays);
        TextView tvCustomEndDateedit = view.findViewById(R.id.tvEditCustomEndDate);

        String[] recOptionsEdit = {"Không lặp", "Hàng ngày", "Hàng tuần", "Hàng tháng", "Tùy chỉnh"};
        ArrayAdapter<String> recAdapterEdit = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, recOptionsEdit);
        recAdapterEdit.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerRecedit.setAdapter(recAdapterEdit);

        final Date[] customEndDateEdit = {null};

        String curRec = task.getRecurrence();
        int selId = 0;
        if ("DAILY".equals(curRec)) selId = 1;
        else if ("WEEKLY".equals(curRec)) selId = 2;
        else if ("MONTHLY".equals(curRec)) selId = 3;
        else if (curRec != null && curRec.startsWith("CUSTOM:")) {
            selId = 4;
            String[] pts = curRec.split(":");
            if (pts.length >= 3) {
                etCustomDaysedit.setText(pts[1]);
                try {
                    long ems = Long.parseLong(pts[2]);
                    if (ems > 0) {
                        customEndDateEdit[0] = new Date(ems);
                        tvCustomEndDateedit.setText(dateFormat.format(customEndDateEdit[0]));
                    }
                } catch(Exception ignored){}
            }
        }
        spinnerRecedit.setSelection(selId);

        spinnerRecedit.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            public void onItemSelected(android.widget.AdapterView<?> parent, View v, int pos, long id) {
                layoutCustomedit.setVisibility(pos == 4 ? View.VISIBLE : View.GONE);
            }
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });

        tvCustomEndDateedit.setOnClickListener(v -> {
            Calendar c = Calendar.getInstance();
            if (customEndDateEdit[0] != null) c.setTime(customEndDateEdit[0]);
            new DatePickerDialog(this, (dialogView, year, month, dayOfMonth) -> {
                Calendar c2 = Calendar.getInstance();
                c2.set(year, month, dayOfMonth, 23, 59, 59);
                customEndDateEdit[0] = c2.getTime();
                tvCustomEndDateedit.setText(dateFormat.format(customEndDateEdit[0]));
            }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
        });

        view.findViewById(R.id.btnDeleteTask).setOnClickListener(v ->
            new AlertDialog.Builder(this)
                .setMessage("Xóa \"" + task.getTitle() + "\"?")
                .setPositiveButton("Xóa", (d2, w) -> {
                    taskViewModel.delete(task);
                    showActionNotification("Xóa Task", "Đã xóa vĩnh viễn: " + task.getTitle(), android.R.drawable.ic_menu_delete);
                    updateUI();
                    dialog.dismiss();
                })
                .setNegativeButton("Hủy", null)
                .show());

        view.findViewById(R.id.btnEditCancel).setOnClickListener(v -> dialog.dismiss());
        view.findViewById(R.id.btnEditSave).setOnClickListener(v -> {
            String newTitle = etTitle.getText().toString().trim();
            if (!newTitle.isEmpty()) {
                task.setTitle(newTitle);
                task.setDueDate(selectedDateTime.getTime().getTime());
                task.setPriority(pri[0]);
                if (!catNames.isEmpty())
                    task.setCategory(catNames.get(spinner.getSelectedItemPosition()));

                // Update reminderTime to match new dueDate
                task.setReminderTime(selectedDateTime.getTime().getTime());
                
                // Đảm bảo có mốc nhắc trước deadline nếu chưa được set trước đó
                if (task.getReminderOffsets() == null) {
                    task.setReminderOffsets("[3600000, 600000]");
                }

                int rp2 = spinnerRecedit.getSelectedItemPosition();
                String rs2 = "NONE";
                if (rp2 == 1) rs2 = "DAILY";
                else if (rp2 == 2) rs2 = "WEEKLY";
                else if (rp2 == 3) rs2 = "MONTHLY";
                else if (rp2 == 4) {
                    String d2 = etCustomDaysedit.getText().toString();
                    if (d2.isEmpty()) d2 = "1";
                    long ems2 = customEndDateEdit[0] != null ? customEndDateEdit[0].getTime() : 0;
                    rs2 = "CUSTOM:" + d2 + ":" + ems2;
                }
                task.setRecurrence(rs2);

                taskViewModel.update(task);
                
                String dateStr = dateFormat.format(selectedDateTime.getTime()) + " " + timeFormat.format(selectedDateTime.getTime());
                showActionNotification("Cập nhật Task", "Đã sửa: " + newTitle + " (" + dateStr + ")", android.R.drawable.ic_menu_edit);
                
                updateUI();
                dialog.dismiss();
            } else {
                etTitle.setError("Vui lòng nhập tên");
            }
        });
        dialog.show();
    }

    /**
     * Update visual state of 4 priority buttons:
     * - Selected: filled with color + white text
     * - Others: outlined
     */
    private void updatePriorityUI(
            com.google.android.material.button.MaterialButton btnHigh,
            com.google.android.material.button.MaterialButton btnMed,
            com.google.android.material.button.MaterialButton btnLow,
            com.google.android.material.button.MaterialButton btnNone,
            String selected) {

        int red    = ContextCompat.getColor(this, R.color.priority_high);
        int orange = ContextCompat.getColor(this, R.color.priority_medium);
        int green  = ContextCompat.getColor(this, R.color.priority_low);
        int gray   = ContextCompat.getColor(this, R.color.priority_none);
        int white  = ContextCompat.getColor(this, R.color.white);
        int transparent = android.graphics.Color.TRANSPARENT;

        // Reset all to outlined
        setButtonState(btnHigh, transparent, red,    red);
        setButtonState(btnMed,  transparent, orange, orange);
        setButtonState(btnLow,  transparent, green,  green);
        setButtonState(btnNone, transparent, gray,   gray);

        // Fill the selected one
        switch (selected) {
            case "HIGH":   setButtonState(btnHigh, red,    red,    white); break;
            case "MEDIUM": setButtonState(btnMed,  orange, orange, white); break;
            case "LOW":    setButtonState(btnLow,  green,  green,  white); break;
            case "NONE":   setButtonState(btnNone, gray,   gray,   white); break;
        }
    }

    private void setButtonState(com.google.android.material.button.MaterialButton btn,
                                int bgColor, int strokeColor, int textColor) {
        btn.setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(bgColor));
        btn.setStrokeColor(
                android.content.res.ColorStateList.valueOf(strokeColor));
        btn.setTextColor(textColor);
    }

    private void showActionNotification(String title, String message, int iconResId) {
        Intent intent = new Intent(this, MainActivity.class);
        android.app.PendingIntent pendingIntent = android.app.PendingIntent.getActivity(
                this, 0, intent, android.app.PendingIntent.FLAG_UPDATE_CURRENT | android.app.PendingIntent.FLAG_IMMUTABLE);

        androidx.core.app.NotificationCompat.Builder builder = new androidx.core.app.NotificationCompat.Builder(this, hcmute.edu.vn.mytasklist.service.NotificationHelper.CHANNEL_SYSTEM_ID)
                .setSmallIcon(iconResId)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(androidx.core.app.NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        try {
            androidx.core.app.NotificationManagerCompat.from(this).notify((int) System.currentTimeMillis(), builder.build());
        } catch (SecurityException e) {
            // Ignore if permission not granted
        }
    }

    // ════════════════════════════════════════════════════════════
    //  UPDATE UI
    // ════════════════════════════════════════════════════════════

    private void updateUI() {
        List<TaskEntity> filtered = getFilteredTasks();
        int incomplete = 0;
        for (TaskEntity t : filtered) if (!t.isCompleted()) incomplete++;
        tvTaskCount.setText(incomplete > 0 ? incomplete + " tasks" : "");

        if (filtered.isEmpty()) {
            rvTasks.setVisibility(View.GONE);
            layoutEmpty.setVisibility(View.VISIBLE);
        } else {
            rvTasks.setVisibility(View.VISIBLE);
            layoutEmpty.setVisibility(View.GONE);
            taskAdapter.submitTasks(filtered);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(android.view.Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(android.view.MenuItem item) {
        if (item.getItemId() == R.id.action_test_services) {
            showTestServicesDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showTestServicesDialog() {
        String[] options = {
                "Test Pomodoro Service",
                "Test Background SyncWorker",
                "Test Daily Summary Worker",
                "Test Auto Cleanup Worker"
        };

        new AlertDialog.Builder(this)
                .setTitle("Test Background Services")
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            Intent intent = new Intent(this, hcmute.edu.vn.mytasklist.service.pomodoro.PomodoroForegroundService.class);
                            intent.setAction(hcmute.edu.vn.mytasklist.service.pomodoro.PomodoroForegroundService.ACTION_START);
                            intent.putExtra(hcmute.edu.vn.mytasklist.service.pomodoro.PomodoroForegroundService.EXTRA_TASK_ID, 999L);
                            intent.putExtra(hcmute.edu.vn.mytasklist.service.pomodoro.PomodoroForegroundService.EXTRA_TASK_TITLE, "Test Focus Session");
                            intent.putExtra(hcmute.edu.vn.mytasklist.service.pomodoro.PomodoroForegroundService.EXTRA_DURATION, 1000L * 60);
                            startService(intent);
                            Toast.makeText(this, "🍅 Pomodoro started", Toast.LENGTH_SHORT).show();
                            break;
                        case 1:
                            androidx.work.WorkManager.getInstance(this)
                                    .enqueue(new androidx.work.OneTimeWorkRequest.Builder(hcmute.edu.vn.mytasklist.service.sync.SyncWorker.class).build());
                            Toast.makeText(this, "🔄 Sync started", Toast.LENGTH_SHORT).show();
                            break;
                        case 2:
                            androidx.work.WorkManager.getInstance(this)
                                    .enqueue(new androidx.work.OneTimeWorkRequest.Builder(hcmute.edu.vn.mytasklist.service.summary.DailySummaryWorker.class).build());
                            Toast.makeText(this, "📊 Daily Summary started", Toast.LENGTH_SHORT).show();
                            break;
                        case 3:
                            androidx.work.WorkManager.getInstance(this)
                                    .enqueue(new androidx.work.OneTimeWorkRequest.Builder(hcmute.edu.vn.mytasklist.service.cleanup.AutoCleanupWorker.class).build());
                            Toast.makeText(this, "🧹 Cleanup started", Toast.LENGTH_SHORT).show();
                            break;
                    }
                })
                .show();
    }

    private List<TaskEntity> getFilteredTasks() {
        List<TaskEntity> result = new ArrayList<>();
        if (allTasks == null) return result;
        Date todayStart = getDayStart(new Date());
        Calendar c7 = Calendar.getInstance();
        c7.add(Calendar.DAY_OF_YEAR, 7);
        Date nextWeekStart = getDayStart(c7.getTime());

        for (TaskEntity t : allTasks) {
            if ("Completed".equals(currentFilter)) {
                if (t.isCompleted()) result.add(t);
            } else {
                if (t.isCompleted()) continue;

                boolean include = false;
                if ("Today".equals(currentFilter)) {
                    if (t.getDueDate() != null && getDayStart(new Date(t.getDueDate())).equals(todayStart)) include = true;
                } else if ("Next 7 Days".equals(currentFilter)) {
                    if (t.getDueDate() != null && !getDayStart(new Date(t.getDueDate())).before(todayStart) && getDayStart(new Date(t.getDueDate())).before(nextWeekStart)) include = true;
                } else if ("Inbox".equals(currentFilter)) {
                    if ("Inbox".equalsIgnoreCase(t.getCategory())) include = true;
                } else {
                    if (currentFilter.equals(t.getCategory())) include = true;
                }

                if (include) result.add(t);
            }
        }
        return result;
    }

    private Date getDayStart(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);      cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }

    private int dp(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }
    
    private AlertDialog currentPomodoroDialog;
    private hcmute.edu.vn.mytasklist.service.pomodoro.MusicAdapter currentMusicAdapter;

    private void showPomodoroSettingsDialog(TaskEntity task) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_pomodoro_settings, null);
        builder.setView(view);

        EditText etFocus = view.findViewById(R.id.etFocusTime);
        EditText etBreak = view.findViewById(R.id.etBreakTime);
        RecyclerView rvMusic = view.findViewById(R.id.rvMusicList);
        TextView tvEmpty = view.findViewById(R.id.tvEmptyMusic);
        TextView tvWarning = view.findViewById(R.id.tvMusicPermissionWarning);
        com.google.android.material.button.MaterialButton btnRequestPerm = view.findViewById(R.id.btnRequestMusicPermission);

        rvMusic.setLayoutManager(new LinearLayoutManager(this));

        currentPomodoroDialog = builder.create();
        if (currentPomodoroDialog.getWindow() != null) {
            currentPomodoroDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        String requiredPermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                ? Manifest.permission.READ_MEDIA_AUDIO
                : Manifest.permission.READ_EXTERNAL_STORAGE;

        if (ContextCompat.checkSelfPermission(this, requiredPermission) != PackageManager.PERMISSION_GRANTED) {
            tvWarning.setVisibility(View.VISIBLE);
            btnRequestPerm.setVisibility(View.VISIBLE);
            rvMusic.setVisibility(View.GONE);
            btnRequestPerm.setOnClickListener(v -> {
                ActivityCompat.requestPermissions(this, new String[]{requiredPermission}, 202);
            });
        } else {
            loadMusicIntoDialog(rvMusic, tvEmpty);
        }

        view.findViewById(R.id.btnCancelPomodoro).setOnClickListener(v -> currentPomodoroDialog.dismiss());
        view.findViewById(R.id.btnStartPomodoro).setOnClickListener(v -> {
            int focusMins = 25;
            int breakMins = 5;
            try { focusMins = Integer.parseInt(etFocus.getText().toString()); } catch (Exception ignored) {}
            try { breakMins = Integer.parseInt(etBreak.getText().toString()); } catch (Exception ignored) {}

            ArrayList<String> selectedMusicURIs = new ArrayList<>();
            if (currentMusicAdapter != null) {
                selectedMusicURIs = currentMusicAdapter.getSelectedUris();
            }

            Intent intent = new Intent(this, hcmute.edu.vn.mytasklist.service.pomodoro.PomodoroForegroundService.class);
            intent.setAction(hcmute.edu.vn.mytasklist.service.pomodoro.PomodoroForegroundService.ACTION_START);
            intent.putExtra(hcmute.edu.vn.mytasklist.service.pomodoro.PomodoroForegroundService.EXTRA_TASK_ID, task.getId());
            intent.putExtra(hcmute.edu.vn.mytasklist.service.pomodoro.PomodoroForegroundService.EXTRA_TASK_TITLE, task.getTitle());
            intent.putExtra(hcmute.edu.vn.mytasklist.service.pomodoro.PomodoroForegroundService.EXTRA_DURATION, focusMins * 60L * 1000L);
            intent.putExtra("EXTRA_BREAK_DURATION", breakMins * 60L * 1000L);
            intent.putStringArrayListExtra("EXTRA_MUSIC_URIS", selectedMusicURIs);
            
            startService(intent);
            Toast.makeText(this, "🍅 Bắt đầu Pomodoro: " + task.getTitle(), Toast.LENGTH_SHORT).show();
            currentPomodoroDialog.dismiss();
        });

        currentPomodoroDialog.show();
    }

    private void loadMusicIntoDialog(RecyclerView rvMusic, TextView tvEmpty) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            List<hcmute.edu.vn.mytasklist.service.pomodoro.MusicItem> musicList = hcmute.edu.vn.mytasklist.service.pomodoro.MusicContentHelper.scanLocalMusic(this);
            runOnUiThread(() -> {
                if (musicList.isEmpty()) {
                    tvEmpty.setVisibility(View.VISIBLE);
                    rvMusic.setVisibility(View.GONE);
                } else {
                    currentMusicAdapter = new hcmute.edu.vn.mytasklist.service.pomodoro.MusicAdapter(musicList);
                    rvMusic.setAdapter(currentMusicAdapter);
                    rvMusic.setVisibility(View.VISIBLE);
                }
            });
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @androidx.annotation.NonNull String[] permissions, @androidx.annotation.NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 202) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (currentPomodoroDialog != null && currentPomodoroDialog.isShowing()) {
                    currentPomodoroDialog.findViewById(R.id.tvMusicPermissionWarning).setVisibility(View.GONE);
                    currentPomodoroDialog.findViewById(R.id.btnRequestMusicPermission).setVisibility(View.GONE);
                    loadMusicIntoDialog(currentPomodoroDialog.findViewById(R.id.rvMusicList), currentPomodoroDialog.findViewById(R.id.tvEmptyMusic));
                }
            } else {
                Toast.makeText(this, "Cần cấp quyền để đọc file nhạc!", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
