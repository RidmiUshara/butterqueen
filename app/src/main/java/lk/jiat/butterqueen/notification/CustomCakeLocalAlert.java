package lk.jiat.butterqueen.notification;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import lk.jiat.butterqueen.activity.NotificationInboxActivity;

/** Foreground/local alerts for the Spark (free plan) custom-cake request flow. */
public final class CustomCakeLocalAlert {
    private static final String CHANNEL = "butterqueen_custom_cakes";

    private CustomCakeLocalAlert() {}

    public static void show(Context context, String id, String title, String message) {
        if (Build.VERSION.SDK_INT >= 33 &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                        != PackageManager.PERMISSION_GRANTED) return;
        NotificationManager manager = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null) return;
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel channel = new NotificationChannel(CHANNEL,
                    "Custom Cake Requests", NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription("Custom cake request submissions and updates");
            manager.createNotificationChannel(channel);
        }

        int noticeId = id.hashCode();
        Intent target = new Intent(context, NotificationInboxActivity.class);
        target.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent open = PendingIntent.getActivity(context, noticeId, target,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        manager.notify(noticeId, new NotificationCompat.Builder(context, CHANNEL)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setContentIntent(open)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true).build());
    }
}
