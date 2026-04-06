package hcmute.edu.vn.mytasklist;

import hcmute.edu.vn.mytasklist.data.entity.TaskEntity;

import android.content.res.ColorStateList;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TaskAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_HEADER = 0;
    private static final int VIEW_TYPE_TASK = 1;

    public interface OnTaskToggleListener {
        void onTaskToggled(TaskEntity task);
    }

    public interface OnTaskLongClickListener {
        void onTaskLongClick(TaskEntity task);
    }

    public interface OnPomodoroClickListener {
        void onPomodoroClick(TaskEntity task);
    }

    // Item types
    private static class HeaderItem {
        String title;
        String dateText;
        HeaderItem(String title, String dateText) {
            this.title = title;
            this.dateText = dateText;
        }
    }

    private final List<Object> items = new ArrayList<>();
    private final OnTaskToggleListener listener;
    private OnTaskLongClickListener longClickListener;
    private OnPomodoroClickListener pomodoroClickListener;

    public TaskAdapter(OnTaskToggleListener listener) {
        this.listener = listener;
    }

    public void setOnTaskLongClickListener(OnTaskLongClickListener l) {
        this.longClickListener = l;
    }

    public void setOnPomodoroClickListener(OnPomodoroClickListener p) {
        this.pomodoroClickListener = p;
    }

    public void submitTasks(List<TaskEntity> tasks) {
        items.clear();
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMM d", Locale.getDefault());

        Date today = getDayStart(new Date());
        Date tomorrow = getDayStart(addDays(new Date(), 1));
        Date nextWeek = addDays(today, 7);

        List<TaskEntity> todayTasks = new ArrayList<>();
        List<TaskEntity> tomorrowTasks = new ArrayList<>();
        List<TaskEntity> laterTasks = new ArrayList<>();

        for (TaskEntity t : tasks) {
            if (t.getDueDate() == null) {
                todayTasks.add(t);
            } else {
                Date d = getDayStart(new Date(t.getDueDate()));
                if (d.equals(today)) todayTasks.add(t);
                else if (d.equals(tomorrow)) tomorrowTasks.add(t);
                else if (d.after(tomorrow) && !d.after(nextWeek)) laterTasks.add(t);
                else todayTasks.add(t); // fallback
            }
        }

        if (!todayTasks.isEmpty()) {
            items.add(new HeaderItem("Today", dateFormat.format(today)));
            items.addAll(todayTasks);
        }
        if (!tomorrowTasks.isEmpty()) {
            items.add(new HeaderItem("Tomorrow", dateFormat.format(tomorrow)));
            items.addAll(tomorrowTasks);
        }
        if (!laterTasks.isEmpty()) {
            items.add(new HeaderItem("Next 7 Days", ""));
            items.addAll(laterTasks);
        }

        notifyDataSetChanged();
    }

    private Date getDayStart(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }

    private Date addDays(Date date, int days) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.add(Calendar.DAY_OF_YEAR, days);
        return cal.getTime();
    }

    @Override
    public int getItemViewType(int position) {
        return items.get(position) instanceof HeaderItem ? VIEW_TYPE_HEADER : VIEW_TYPE_TASK;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == VIEW_TYPE_HEADER) {
            View v = inflater.inflate(R.layout.item_section_header, parent, false);
            return new HeaderViewHolder(v);
        } else {
            View v = inflater.inflate(R.layout.item_task, parent, false);
            return new TaskViewHolder(v);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof HeaderViewHolder) {
            ((HeaderViewHolder) holder).bind((HeaderItem) items.get(position));
        } else if (holder instanceof TaskViewHolder) {
            ((TaskViewHolder) holder).bind((TaskEntity) items.get(position));
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    // --- ViewHolders ---

    static class HeaderViewHolder extends RecyclerView.ViewHolder {
        TextView tvSectionTitle, tvSectionDate;

        HeaderViewHolder(View view) {
            super(view);
            tvSectionTitle = view.findViewById(R.id.tvSectionTitle);
            tvSectionDate = view.findViewById(R.id.tvSectionDate);
        }

        void bind(HeaderItem header) {
            tvSectionTitle.setText(header.title);
            tvSectionDate.setText(header.dateText);
        }
    }

    class TaskViewHolder extends RecyclerView.ViewHolder {
        ImageView ivCheckbox;
        TextView tvTitle, tvDueDate;
        View viewPriority;
        ImageView ivPomodoro;

        TaskViewHolder(View view) {
            super(view);
            ivCheckbox = view.findViewById(R.id.ivCheckbox);
            tvTitle = view.findViewById(R.id.tvTitle);
            tvDueDate = view.findViewById(R.id.tvDueDate);
            viewPriority = view.findViewById(R.id.viewPriority);
            ivPomodoro = view.findViewById(R.id.ivPomodoro);
        }

        void bind(final TaskEntity task) {
            android.content.Context ctx = itemView.getContext();
            SimpleDateFormat dateFormat = new SimpleDateFormat("MMM d", Locale.getDefault());

            tvTitle.setText(task.getTitle());

            if (task.isCompleted()) {
                tvTitle.setPaintFlags(tvTitle.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                tvTitle.setTextColor(ContextCompat.getColor(ctx, R.color.text_completed));
                ivCheckbox.setBackground(ContextCompat.getDrawable(ctx, R.drawable.circle_checkbox_checked));
                ivCheckbox.setImageResource(R.drawable.ic_check);
                ivCheckbox.setImageTintList(ColorStateList.valueOf(
                        ContextCompat.getColor(ctx, R.color.white)));
            } else {
                tvTitle.setPaintFlags(tvTitle.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
                tvTitle.setTextColor(ContextCompat.getColor(ctx, R.color.text_primary));
                ivCheckbox.setBackground(ContextCompat.getDrawable(ctx, R.drawable.circle_checkbox));
                ivCheckbox.setImageDrawable(null);
            }

            tvDueDate.setText(task.getDueDate() != null ? dateFormat.format(task.getDueDate()) : "");

            int priorityColor;
            String priority = task.getPriority() != null ? task.getPriority() : "NONE";
            switch (priority) {
                case "HIGH": priorityColor = ContextCompat.getColor(ctx, R.color.priority_high); break;
                case "MEDIUM": priorityColor = ContextCompat.getColor(ctx, R.color.priority_medium); break;
                case "LOW": priorityColor = ContextCompat.getColor(ctx, R.color.priority_low); break;
                default: priorityColor = ContextCompat.getColor(ctx, R.color.bg_white); break;
            }
            viewPriority.setBackgroundColor(priorityColor);

            ivCheckbox.setOnClickListener(v -> {
                task.setCompleted(!task.isCompleted());
                bind(task);
                if (listener != null) listener.onTaskToggled(task);
            });

            if (ivPomodoro != null && !task.isCompleted()) {
                ivPomodoro.setVisibility(View.VISIBLE);
                ivPomodoro.setOnClickListener(v -> {
                    if (pomodoroClickListener != null) pomodoroClickListener.onPomodoroClick(task);
                });
            } else if (ivPomodoro != null) {
                 ivPomodoro.setVisibility(View.GONE);
            }

            // Long press → edit/delete dialog
            itemView.setOnLongClickListener(v -> {
                if (longClickListener != null) longClickListener.onTaskLongClick(task);
                return true;
            });
        }
    }
}
