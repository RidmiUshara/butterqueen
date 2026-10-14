package lk.jiat.butterqueen.data;

import android.content.Context;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import lk.jiat.butterqueen.R;

/**
 * Customer profile photos live in app-private internal storage on THIS device.
 * No Firebase Storage, public gallery file or storage permission is involved.
 */
public final class LocalProfilePhotoStore {
    private LocalProfilePhotoStore() {}

    private static File photoPath(Context context, String uid) {
        if (uid == null || !uid.matches("[A-Za-z0-9_-]{1,128}")) {
            throw new IllegalArgumentException("Invalid customer ID");
        }
        return new File(new File(context.getFilesDir(), "profile_photos"), uid + ".jpg");
    }

    public static File getPhoto(Context context, String uid) {
        if (uid == null) return null;
        try {
            File file = photoPath(context, uid);
            return file.isFile() && file.length() > 0 ? file : null;
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    public static void save(Context context, String uid, byte[] jpeg) throws IOException {
        if (jpeg == null || jpeg.length == 0) throw new IOException("Photo is empty");
        File photo = photoPath(context, uid);
        File directory = photo.getParentFile();
        if (directory == null || (!directory.isDirectory() && !directory.mkdirs())) {
            throw new IOException("Could not create private photo folder");
        }

        // Write a temporary file and rename it so a failed write never truncates
        // an existing customer photo. Files stay in the same private directory.
        File temporary = File.createTempFile("photo_", ".tmp", directory);
        try {
            try (FileOutputStream output = new FileOutputStream(temporary)) {
                output.write(jpeg);
                output.getFD().sync();
            }
            if (!temporary.renameTo(photo)) {
                throw new IOException("Could not replace saved profile photo");
            }
        } finally {
            if (temporary.exists()) temporary.delete();
        }
    }

    public static void display(Context context, String uid, ImageView target) {
        if (target == null) return;
        File photo = getPhoto(context, uid);
        if (photo == null) {
            Glide.with(target).clear(target);
            target.setImageResource(R.drawable.account_circle_24px);
            return;
        }
        // A customer can overwrite the same filename; never show a stale cached photo.
        Glide.with(target).load(photo)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .skipMemoryCache(true)
                .placeholder(R.drawable.account_circle_24px)
                .error(R.drawable.account_circle_24px)
                .into(target);
    }

    public static void delete(Context context, String uid) {
        File photo = getPhoto(context, uid);
        if (photo != null) photo.delete();
    }
}
