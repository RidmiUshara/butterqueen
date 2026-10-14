package lk.jiat.butterqueen.notification;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

import lk.jiat.butterqueen.activity.NotificationInboxActivity;

/** User-specific FCM data push, shown while this account is signed in. */
public final class ButterQueenMessagingService extends FirebaseMessagingService {
    private static final String CHANNEL = "butterqueen_updates";

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        PushTokenRegistrar.refreshed(this, token);
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage message) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        Map<String, String> data = message.getData();
        // Old FCM tokens can survive an interrupted sign-out; never show
        // notifications intended for a different customer on a shared device.
        if (user == null || !user.getUid().equals(data.get("recipientUid"))) return;
        if (Build.VERSION.SDK_INT >= 33 &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                        != PackageManager.PERMISSION_GRANTED) return;

        NotificationManager manager = (NotificationManager)
                getSystemService(NOTIFICATION_SERVICE);
        if (manager == null) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL,
                    "Bakery updates", NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription("Order, custom cake and product updates");
            manager.createNotificationChannel(channel);
        }
        Intent intent = new Intent(this, NotificationInboxActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        int id = (message.getMessageId() == null
                ? System.nanoTime() : message.getMessageId()).hashCode();
        PendingIntent open = PendingIntent.getActivity(this, id, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        String title = data.getOrDefault("title", "Butter Queen");
        String body = data.getOrDefault("body", "You have a bakery update.");
        manager.notify(id, new NotificationCompat.Builder(this, CHANNEL)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(open)
                .setAutoCancel(true)
                .build());
    }
}
