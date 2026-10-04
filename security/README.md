# Firebase rules review

These are **candidate** Firestore and Storage rules for the Android client. They are not deployed automatically. Compare them with the live Firebase Console rules and test them in the Firebase Emulator before deploying.

- Product and category writes are reserved for a trusted server/Admin SDK. A separate client-side admin app needs a reviewed role model before using these rules.
- A signed-in customer can read only their own user document, cart, and orders. The customer may create an order marked pending, but cannot set `paid` or edit an order afterward.
- This is a baseline, not full price integrity. The client still supplies prices and delivery totals; a server must compute and verify the amount before accepting an order or confirming payment.
- The PayHere Merchant Secret and Firebase Admin credentials belong only on a trusted server. The PayHere notification signature must be verified there.
- The current delete-account flow does not delete order history, cart subcollections, or profile image files. Define a retention/deletion policy and implement it on the server.
