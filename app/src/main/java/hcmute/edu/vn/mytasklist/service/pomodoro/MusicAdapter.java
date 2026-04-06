package hcmute.edu.vn.mytasklist.service.pomodoro;

import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import hcmute.edu.vn.mytasklist.R;

public class MusicAdapter extends RecyclerView.Adapter<MusicAdapter.MusicViewHolder> {

    private List<MusicItem> musicList;
    
    public MusicAdapter(List<MusicItem> musicList) {
        this.musicList = musicList;
    }

    @NonNull
    @Override
    public MusicViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_music, parent, false);
        return new MusicViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MusicViewHolder holder, int position) {
        MusicItem item = musicList.get(position);
        holder.tvTitle.setText(item.getTitle());
        holder.tvArtist.setText(item.getArtist());
        
        long minutes = (item.getDurationMs() / 1000) / 60;
        long seconds = (item.getDurationMs() / 1000) % 60;
        holder.tvDuration.setText(String.format("%02d:%02d", minutes, seconds));
        
        holder.cbSelect.setChecked(item.isSelected());

        // Handle entire row click
        holder.itemView.setOnClickListener(v -> {
            item.setSelected(!item.isSelected());
            holder.cbSelect.setChecked(item.isSelected());
        });
        
        holder.cbSelect.setOnClickListener(v -> {
             item.setSelected(holder.cbSelect.isChecked());
        });
    }

    @Override
    public int getItemCount() {
        return musicList != null ? musicList.size() : 0;
    }
    
    public ArrayList<String> getSelectedUris() {
        ArrayList<String> selected = new ArrayList<>();
        if (musicList != null) {
            for (MusicItem item : musicList) {
                if (item.isSelected()) {
                    selected.add(item.getUri().toString());
                }
            }
        }
        return selected;
    }

    public ArrayList<String> getSelectedTitles() {
        ArrayList<String> selected = new ArrayList<>();
        if (musicList != null) {
            for (MusicItem item : musicList) {
                if (item.isSelected()) {
                    selected.add(item.getTitle());
                }
            }
        }
        return selected;
    }

    static class MusicViewHolder extends RecyclerView.ViewHolder {
        CheckBox cbSelect;
        TextView tvTitle, tvArtist, tvDuration;

        public MusicViewHolder(@NonNull View itemView) {
            super(itemView);
            cbSelect = itemView.findViewById(R.id.cbSelectMusic);
            tvTitle = itemView.findViewById(R.id.tvMusicTitle);
            tvArtist = itemView.findViewById(R.id.tvMusicArtist);
            tvDuration = itemView.findViewById(R.id.tvMusicDuration);
        }
    }
}
