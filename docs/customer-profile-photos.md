# Customer profile photos — Firebase Storage setup

The customer Profile screen supports both **Camera** (full-resolution photo via FileProvider) and
**Gallery** (Android image picker). Images are resized to max 768 px and uploaded as JPEG.

## Requirements

1. Firebase project `butter-queen` must have **Cloud Storage for Firebase** enabled.
2. Firebase Storage requires **Blaze (pay-as-you-go)** even for default buckets, as of 2026.
   This does not automatically mean a bill, but billing must be enabled and usage can be charged.
   Do not enable billing without the project owner's permission. Add budget alerts if upgrading.
3. The application's `app/google-services.json` must match project `butter-queen`
   and contain its bucket configuration. Never commit this file or private credentials.
4. Firestore security rules must allow the signed-in user to update **only their own**
   document `users/{uid}`; the app updates `profileImageUrl` and `profileImageUpdatedAtMillis`.
   Profile image updates do **not** need to change the user role.

## Storage Rules (merge into existing rules)

Add this match inside `service firebase.storage { match /b/{bucket}/o { ... } }`:

```firebase
match /profile_images/{fileName} {
  // The app stores images exactly as "{auth.uid}.jpg" and converts them to JPEG.
  allow read: if request.auth != null
              && fileName == request.auth.uid + '.jpg';
  allow write: if request.auth != null
               && fileName == request.auth.uid + '.jpg'
               && request.resource.size < 2 * 1024 * 1024
               && request.resource.contentType == 'image/jpeg';
}
```

Important: do **not** replace existing Storage Rules with the fragment alone.
Do **not** use a public wildcard such as `allow read, write: if true;`.
Publishing and testing live Storage Rules requires console access and is not performed by GitHub commits.

## Test

- Sign in as customer → Profile → Change Photo → Gallery → select photo.
- Repeat with **Take photo with camera**; capture and confirm.
- Wait for "Profile photo saved"; leave Profile and return.
- Reopen app; image should load from the Firestore `profileImageUrl`.
- Check navigation drawer avatar changes after photo upload.
- Confirm another customer account cannot overwrite a different user's profile photo.

If the image cannot be uploaded and the project is on **Spark**, Firebase will return
billing/authorization errors (often HTTP 402 or 403). This cannot be fixed by Android code
alone. For a no-billing viva demo, demonstrate the camera chooser/preview separately
and describe Cloud Storage upload as dependent on Firebase billing and rules.

Firebase guidance:
https://firebase.google.com/docs/storage/faqs-storage-changes-announced-sept-2024
https://firebase.google.com/docs/storage/security/
