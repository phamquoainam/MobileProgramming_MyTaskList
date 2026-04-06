package hcmute.edu.vn.mytasklist.service.pomodoro;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class MusicContentHelper {

    private static final String TAG = "MusicContentHelper";

    public static List<MusicItem> scanLocalMusic(Context context) {
        List<MusicItem> musicList = new ArrayList<>();
        ContentResolver contentResolver = context.getContentResolver();

        Uri collection = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q
                ? MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
                : MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;

        String[] projection = new String[]{
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.DURATION
        };

        // Xóa điều kiện IS_MUSIC != 0 vì file tải trên giả lập thường bị HĐH bỏ quên cờ này
        String selection = null;
        String sortOrder = MediaStore.Audio.Media.TITLE + " ASC";

        try (Cursor cursor = contentResolver.query(collection, projection, selection, null, sortOrder)) {
            if (cursor != null) {
                int idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID);
                int titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE);
                int artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST);
                int durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION);

                while (cursor.moveToNext()) {
                    long id = cursor.getLong(idColumn);
                    String title = cursor.getString(titleColumn);
                    String artist = cursor.getString(artistColumn);
                    long duration = cursor.getLong(durationColumn);

                    Uri contentUri = ContentUris.withAppendedId(collection, id);

                    // Bỏ qua chặn duration vì file tải qua giả lập có thể chưa quét được duration ngay
                    musicList.add(new MusicItem(id, title != null ? title : "Unknown Track", 
                            artist != null ? artist : "Unknown Artist", duration, contentUri));
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to scan local music", e);
        }

        return musicList;
    }
}
