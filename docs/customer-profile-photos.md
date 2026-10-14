# Customer profile photos — local storage only

Customer Profile -> **Change Photo** -> **Take photo with camera** or
**Choose from gallery**.

The app takes a full-resolution camera photo using the existing FileProvider, or
copies the photo from the Android system gallery picker. It normalizes the selected
image to an approximately 768 px JPEG and saves it in the Android app's private
internal storage:

```text
<app-internal-files>/profile_photos/<firebase-auth-uid>.jpg
```

The camera's temporary image is under `<app-cache>/profile_camera` and is removed
after processing. The gallery photo is **copied** into the app; the original is
not modified or moved.

**No Firebase Storage, Firestore photo URL, billing upgrade, Storage Rules, or
image upload is needed.** Firebase Authentication is used only to separate each
customer's local file by account UID; existing Firestore-backed personal details
and order management are unchanged.

The customer Profile and the customer navigation drawer both display this local
file. Saving a new photo replaces the previous file. The drawer refreshes
immediately, and both screens load the same saved photo when the app reopens.

## Limitations

- Photo remains on **this phone/emulator only**. Signing in on another device
  does not transfer it.
- Android **Clear storage** or **Uninstall** deletes the app-private file.
- Logging out **does not delete** the image, so the same account can still see
  its photo after signing back in on the same device.
- A different account on the same device gets a separate UID-based file.
- Deleting the Firebase account through the Profile screen also deletes that
  account's local photo if Firebase account deletion succeeds.
- Prior `profileImageUrl` values may remain in existing Firestore user
  documents from an old cloud-photo implementation, but this version of the app
  no longer loads or updates them.

## Test steps

1. Sign in to a **customer** account.
2. Go to Profile -> Change Photo -> Choose from gallery. Pick an image.
3. Confirm **Profile photo saved on this device**.
4. Open the navigation drawer: both avatars should now match.
5. Force-close and reopen the app. The saved photo should remain.
6. Profile -> Change Photo -> Take photo with camera. Capture and confirm.
7. Confirm the previous local photo is replaced and still visible after restart.
8. Try the app without internet: photo selection and display should still work
   (profile details may need their existing Firestore cache).

No additional gallery or camera runtime permissions are needed because image
selection/capture uses Android system activity-result contracts and FileProvider.
Android build CI tests compilation only; camera and gallery actions must be tested
on a real device or emulator.
