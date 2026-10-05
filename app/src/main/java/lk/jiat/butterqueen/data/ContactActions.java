package lk.jiat.butterqueen.data;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;

public final class ContactActions {
    public static final String PHONE = "0763227525";
    public static final String EMAIL = "info@butterqueen.com";

    private ContactActions() {}

    public static void call(Context context) {
        open(context, new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + PHONE)),
                "No phone app is available");
    }

    public static void email(Context context, String subject) {
        Intent intent = new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:" + EMAIL));
        intent.putExtra(Intent.EXTRA_SUBJECT, subject);
        open(context, intent, "No email app is available");
    }

    private static void open(Context context, Intent intent, String failure) {
        try {
            context.startActivity(intent);
        } catch (ActivityNotFoundException error) {
            Toast.makeText(context, failure, Toast.LENGTH_LONG).show();
        }
    }
}
