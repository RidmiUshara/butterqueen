# Firebase rules review

These are candidate Firestore and Storage rules for the ButterQueen Android app. They are **not deployed**. Compare them with the live Firebase project and test customer, admin, and cross-user cases in the Firebase Emulator before publishing.

- The customer can create their own profile without a `role` field and update only profile fields. Only a trusted administrator can set `role: "admin"` in a user document. **Audit existing user documents for unauthorized admin roles before deploying:** previous permissive rules could have allowed self-promotion. Prefer server-assigned Firebase Auth custom claims for a mature admin system.
- The separate admin app can read users and carts and write products/categories when its signed-in user's document has a trusted admin role. Its order updates are limited to `orderStatus`; only a trusted backend can mark a payment paid.
- Customers may create only orders for their own UID with pending payment status, then add order items to their own order in the checkout batch. Customer-provided prices, quantities, delivery fees, and totals remain untrusted. A backend must recalculate and verify them before charging or fulfillment.
- `surpriseDeliveries` is not used in this Android repository, so the candidate rules deny it. Review its actual data fields and admin app behavior before enabling writes.
- The PayHere Merchant Secret and Firebase Admin credentials belong only on a trusted server. Verify PayHere notifications there.
- The current delete-account flow does not delete order history, cart subcollections, or profile image files. Define a retention/deletion policy and implement a trusted deletion workflow. Customer profile deletion is denied by these rules.
