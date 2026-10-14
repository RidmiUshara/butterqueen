# Butter Queen — Notification deployment checklist

## What is in the Android app

- Customer Home: bell + real-time unread badge.
- Bakery Studio Admin Home: bell + real-time unread badge.
- Notification inbox (both roles): last 100 notifications, with newest first. Opening the inbox marks all pending unread records read in batches; bell count clears.
- Tap a notification to open the relevant order, product, custom cake screen, or admin order manager.
- App reads `users/{uid}/notifications` only. The customer cannot write alerts or assign their own role.

## Server-side triggers

`functions/index.js` contains five server-side Firestore triggers:
1. New order -> **all admin accounts**.
2. Status change -> **customer who owns order**.
3. New available product -> **customer accounts**.
4. New custom cake request -> **admin accounts**.
5. Custom cake request status change -> **customer who owns request**.

The Firestore inbox is persistent and authoritative. FCM (Firebase Cloud Messaging)
additionally alerts registered Android devices.

## Owner action REQUIRED — not performed by a GitHub commit

**This code alone does not make events start appearing.** The owner must:

1. Confirm the Firebase project is `butter-queen`, not `butter-queen-admin`.
2. Upgrade to **Blaze**, if acceptable to the owner. Deployment of Cloud Functions
   requires a billing account; cloud usage could incur charges.
3. In Firebase Console → Firestore Database → Rules, merge
   `security/notifications.rules.fragment` inside the existing
   `match /users/{uid}` block and Publish. **Do not replace the project's
   working order/category/custom cake rules** with the fragment.
4. Install Node.js 20 and Firebase CLI:
   `npm install -g firebase-tools`, then `firebase login`.
5. From project root:
   `cd functions && npm install && cd ..`
   `firebase deploy --project butter-queen --only functions`
6. Firebase Console → Firestore → examine
   `users/{uid}/notifications` when test changes are made.
7. Android 13+: grant `POST_NOTIFICATIONS` when prompted/through App Settings
   to see operating-system notification banners; the in-app bell works without it.
8. Use *two separate accounts* (admin + customer) to test the five event triggers.

## Security model

- Do **not** put FCM server keys, service-account keys or the Firebase Admin SDK in Android.
- Cloud Functions query admin users using the trusted Firestore `role` field.
- Notification create/delete is denied to the Android client.
- Notification updates allow only setting `read: true`.
- Device push tokens are stored in a per-user private subcollection.
- No phone/address or personal customer details are sent in notification payloads.
- Users can revoke OS notifications; Firestore inbox still works.
- The original checkout-local "Order placed" notification is separate from the inbox.

## Caveats

- Without deploying Cloud Functions, no automatic Firestore notification
  documents are created from new orders, products or status changes. The bell
  and empty inbox still render.
- If an Android device is offline, fresh records appear when connected again.
- FCM high-priority data messages are best-effort, may be delayed by OS battery
  optimizations, and are not guaranteed on every closed/killed app.
- Large production catalogs should paginate new-product fan-out rather than load
  all users in a single function invocation. This sample targets a university demo.
- Old events are not backfilled: only new events after function deployment fire.
- Count is capped at **99+** in the badge.
