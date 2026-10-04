# ButterQueen Android app

ButterQueen is a Java Android cake ordering app. The current code includes Firebase Authentication, Firestore product and cart data, checkout, orders, profile image upload, delivery area maps, and **PayHere sandbox** checkout.

## Local setup

1. Open this folder in Android Studio. Keep the generated `local.properties` file outside Git.
2. Add `app/google-services.json` from the correct Firebase Android app, whose package name is `lk.jiat.butterqueen`. This file is intentionally ignored by Git. Do not confuse it with a Firebase Admin service-account JSON, which must never be placed in an Android app.
3. Copy the two entries from `local.properties.example` into your existing `local.properties`. Set `MAPS_API_KEY` to a newly issued key restricted to the **Maps SDK for Android**, package `lk.jiat.butterqueen`, and the SHA-1 of the signing certificate(s). Set `PAYHERE_MERCHANT_ID` to the sandbox Merchant ID if you want to test online checkout.
4. Sync Gradle and run the app. Missing local values allow a build, but maps or sandbox checkout will be unavailable.

## Catalog and ordering

- Create the starter categories **Cakes**, **Cupcakes**, and **Brownies** in Bakery Studio, or add custom categories with a chosen price unit. A product's `unitType` is `kg` or `pcs`; `price` always means the price for **1 kg** or **1 piece**.
- Cake products can be marked **Birthday**, **Wedding**, or **Other**. Customers choose cake weight in 0.5 kg steps. Cupcakes and brownies use whole pieces. The cart and order items store both the unit and numeric quantity.
- Existing `box` and `pack` products are hidden from customer browsing until an admin edits the product and confirms its new per-piece price. Remove and re-add old box/pack cart items. Review old product prices before taking orders.
- The admin menu appears only to a user with a trusted `role: "admin"` in their `users/{uid}` document. The activity checks that role from the server as well. Create and audit admin roles using the Firebase Console or a trusted server; customers must never be able to write their own role. **Deploy and test the candidate rules in `security/` before relying on admin access.**
- Bakery Studio adds categories and products, lists recent orders, shows their unit-aware line items, and lets an admin change order status. PayHere sandbox orders cannot progress beyond payment pending until a trusted server marks payment verified. Product images currently accept an HTTPS image URL; uploading product photos directly requires a separate reviewed Storage rule and workflow.

## Payment and data safety

- The client uses the PayHere **sandbox URL only**. A successful SDK activity result records `verification_pending`, never `paid`.
- There is no trusted payment notification endpoint in this repository. Do not switch to live payments until a server verifies PayHere's notification signature and updates the order.
- The app currently calculates item totals and delivery fees on the device. A trusted server must recalculate amounts from product data before charging or fulfilling a real order. Client-submitted totals are not authoritative.
- Review and deploy Firebase Firestore and Storage rules in the actual Firebase project. Candidate rules and their assumptions are in `security/`; they are **not deployed** by this repository.
- Account deletion in the current Android client does not reliably purge nested Firestore data, orders, and profile images. A trusted backend deletion workflow is required before calling that a complete deletion.

## Credential cleanup

The repository's original import included a Maps key in the manifest. The manifest now uses a local build placeholder. **Revoke the old key and create a restricted replacement in Google Cloud.** Removing it from a commit or branch history cannot invalidate copies already made.
