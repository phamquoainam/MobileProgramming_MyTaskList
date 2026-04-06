package hcmute.edu.vn.mytasklist.service.pomodoro;

import android.net.Uri;

public class MusicItem {
    private long id;
    private String title;
    private String artist;
    private long durationMs;
    private Uri uri;
    private boolean isSelected;

    public MusicItem(long id, String title, String artist, long durationMs, Uri uri) {
        this.id = id;
        this.title = title;
        this.artist = artist;
        this.durationMs = durationMs;
        this.uri = uri;
        this.isSelected = false;
    }

    public long getId() { return id; }
    public String getTitle() { return title; }
    public String getArtist() { return artist; }
    public long getDurationMs() { return durationMs; }
    public Uri getUri() { return uri; }
    
    public boolean isSelected() { return isSelected; }
    public void setSelected(boolean selected) { isSelected = selected; }
}
