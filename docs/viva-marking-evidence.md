# Butter Queen — marking rubric evidence and viva demonstration

**Reviewed against:** the marking-criteria screenshots supplied by the student (reported total **82**), and the `janithcd/butterqueen` Android/Java source on GitHub in October 2026.

**Important:** Those marks are from the screenshot of an assessment and are **not** a recalculated grade for this newer app version. "Implemented" below means code-level evidence exists; it is **not** proof the feature was demonstrated or worked on every device. Real hardware, Firebase rules, Maps credentials, and live payment setup have not all been independently tested.

## Project overview

- App: **Butter Queen Cakes**, Java Android, package `lk.jiat.butterqueen`.
- Architecture: Material 3 Activities + Fragments; role-based routing to customer app versus separate Bakery Studio admin Home/Profile; repository/data layers; Firebase Auth + Cloud Firestore; Room local cart snapshot.
- Customer: product/category browsing, wishlist, search/filter, custom cake requests, cart, checkout (Cash on Delivery and **PayHere sandbox only**), order tracking, receipt PDF, delivery-area map, local-only profile photo, themes and app lock.
- Admin: real-order analytics, donut/monthly bars, product/category management, custom cake inbox, order status updates.
- Payment safety: SDK success is **verification pending**, not a trusted server-side 'paid' event. No live payment processing is implemented.

## Rubric-to-source evidence

Legend: **Present** = corresponding code exists; **Partial** = not all wording/features evidenced; **Demo needed** = end-to-end/hardware verification required; **Non-code** = presentation/document assessment.

| Criterion | Screenshot marks | Evidence in repository | Evidence state / demonstration |
| --- | ---: | --- | --- |
| Documentation | 19/30 | `README.md`, `docs/viva-marking-evidence.md`, `security/` | Non-code: use this as a reference, but a submitted university report still needs diagrams, screenshots, citations and testing results |
| 1.1 Layouts | 3/3 | `res/layout/fragment_home.xml`, `activity_main.xml`, `activity_checkout.xml` | Present: Linear, Frame, Constraint layouts; inspect layout editor |
| 1.2 Activities | 3/3 | `activity/MainActivity.java`, `CheckoutActivity.java`, `OrderReceiptActivity.java` | Present: `onCreate/onResume/onDestroy` etc.; rotate emulator |
| 1.3 Fragments | 3/3 | `fragment/HomeFragment.java`, `ProfileFragment.java`, `OrdersFragment.java` | Present: lifecycle and Activity/Fragment navigation |
| 1.4 Intents | 1/1 | `data/ContactActions.java`, `activity/OrderReceiptActivity.java` | Present: explicit Activity navigation; ACTION_DIAL, ACTION_SENDTO, ACTION_SEND |
| 1.5 Basic views | 1/1 | Checkout and profile layouts | Present: text inputs, text, buttons, radio group, images |
| 1.6 Resources | 1/1 | `res/values/themes.xml`, `res/drawable/`, `res/mipmap/` | Present: styles, colors, icons; show light/dark |
| 1.7 Navigation bars | 1/1 | `activity/MainActivity.java`, `res/layout/activity_main.xml`, `AdminOverviewActivity.java` | Present: customer bottom navigation/drawer and separate admin Home/Profile |
| 2.1 Advanced views | 2/2 | `activity_main.xml` NavigationView and bottom navigation; Snackbar in MainActivity | Present; show offline Snackbar |
| 2.2 Collections | 2/3 | `adapter/ProductAdapter.java`, `CategoryAdapter.java`; Custom Cake Spinners | Present: RecyclerView and Spinner; check empty/loading states during demo |
| 2.3 Dialogs | 2/2 | `SignUpActivity.java`, `ProfileFragment.java`, `AdminDashboardActivity.java` | Present: date picker, alert/confirmation dialogs |
| 2.4 Libraries/animations/custom views | 1/2 | `adapter/ProductAdapter.java` heart scale animation; `activity/AdminOverviewActivity.java` custom DonutView; Material 3 | Present, demonstrate double-tap heart and animated feedback; no extra animation library is required for the current implementation |
| 2.5 Camera | 1/1 | `fragment/ProfileFragment.java`, `activity/CustomCakeActivity.java` | Present: photo capture and gallery; **video capture not evidenced** |
| 2.6 Notifications | 1/1 | `notification/OrderNotificationHelper.java`, `activity/CheckoutActivity.java` | Present: Android notification links back to Orders; grant permission Android 13+ |
| 2.7 Reports/charts | 3/3 | `activity/AdminSalesReportsActivity.java`, `AdminOverviewActivity.java` | Present: real Firestore order status and monthly charts, donut |
| 3.1 Threads | 3/3 | `CheckoutActivity.java` ExecutorService, `ProfileFragment.java` image worker, `OrderReceiptActivity.java` PDF worker | Present: background work and UI-thread callbacks |
| 3.2 Events | 2/2 | `ProductAdapter.java`, `MainActivity.java`, `SettingFragment.java` | Present: onClick and other listener callbacks |
| 3.3 Gestures | 2/2 | `ProductAdapter.java` GestureDetector; `HomeFragment.java` shake | Present: double-tap image to wishlist, touch interactions |
| 3.4 Broadcast receivers | 2/3 | `MainActivity.java` battery receiver for BATTERY_LOW/POWER_CONNECTED/DISCONNECTED | Present: registered/unregistered with Activity; connectivity handled through NetworkCallback (not a broadcast receiver). Show Logcat or controlled emulator battery events |
| 3.5 Permissions | 2/2 | `AndroidManifest.xml`, `CheckoutActivity.java` | Present: runtime POST_NOTIFICATIONS and API-level checks; camera/gallery via system intents |
| 4.1 SharedPreferences | 0/1 | `data/AppSettings.java`, `AppLockStore.java`, `ButterQueenApplication.java`, `SettingFragment.java` | **Present now**: saved theme/shake preferences and per-UID lock options; demonstrate app restart |
| 4.2 SQLite/offline sync | 3/3 | `offline/OfflineCartDatabase.java`, `OfflineCartDao.java`, `OfflineCartEntity.java` | **Partial to rubric wording**: Room-backed local cart snapshot/offline read is evidenced. Full queued-write conflict-resolving bidirectional offline sync is **not** established |
| 4.3 File storage | 0.5/1 | `data/LocalProfilePhotoStore.java`, `fragment/ProfileFragment.java` | **Present now**: Camera/Gallery JPEG is saved per UID to app-private internal files; no Firebase Storage |
| 5.1 Async server requests | 3/3 | Firestore task listeners in `CheckoutActivity.java`, `OrdersFragment.java` | Present; show online/offline states |
| 5.2 Cloud | 2/3 | Firebase Authentication + Firestore in `SignInActivity.java`, `AdminDashboardActivity.java` | Present; Firebase Storage not required by the chosen **local-only** photo design |
| 5.3 CRUD | 3/3 | `AdminDashboardActivity.java`, `AdminProductActivity.java`, `AdminCustomCakeRequestsActivity.java` | Present: admin creates/updates/deletes catalog and updates order status; rules must be deployed |
| 5.4 JSON encoding/decoding | 1/1 | `data/OrderSummaryJson.java`, `activity/OrderReceiptActivity.java`, `OrderSummaryJsonTest.java` | **Present now**: user taps **Copy order summary as JSON** on receipt; Gson encodes a contact-free summary, a parser decodes/validates it; two unit tests check roundtrip/schema and privacy |
| 6.1 Maps | 4/4 | `fragment/DeliveryAreasFragment.java` | Present: Google Map, markers, circles, click selection, distance/delivery bands. **Current GPS location, routes/polylines not evidenced**; avoid claiming all Google Maps examples |
| 7.1 Telecom operations | 2/2 | `data/ContactActions.java`, `fragment/HelpFragment.java` | Present: dialer and email client implicit intents (not silent calls/SMS) |
| 7.2 Sensors | 2/2 | `fragment/HomeFragment.java` accelerometer + `SettingFragment.java` | Present: opt-in shake to refresh, lifecycle unregister |
| 8.1 Bluetooth/Wi-Fi exchange | 0.5/1 | `activity/OrderReceiptActivity.java` **Share PDF** | New: Android Sharesheet can transfer PDF through Quick Share on supported devices using Bluetooth/Wi-Fi. **Only count full exchange if demonstrated successfully with two devices**; no bespoke Bluetooth/Wi-Fi socket protocol |
| 9.1 Finalization | 1/1 | `AndroidManifest.xml`, Gradle, `.github/workflows/android-build.yml` | CI can compile debug APK and run tests; confirm current run passes |
| 10.1 Oral questions | 3/4 | This document's viva questions below | Non-code: oral performance cannot be proven by repository inspection |
| 11.1 Presentation skills | 1/1 | Practice a 5–7 minute demo with the outline below | Non-code |
| 11.2 Presentation materials | 1/1 | Include real emulator screenshots, diagrams and code excerpts | Non-code |

## Practical viva demo script (about 7 minutes)

1. **Login & roles (45 seconds):** sign in as customer to show Home/Cart/Wishlist/Profile, sign out and use authorized admin account to show **Bakery Studio Home/Profile** only.
2. **Customer journey (1 minute):** browse and search products, select kg or pieces, change quantity, add to cart and demonstrate wishlist double tap heart.
3. **Offline + preferences (1 minute):** show Settings theme toggle, shake setting, restart app to prove SharedPreferences; show Room local cart snapshot while offline (read only; don't claim queued writes sync).
4. **Device capabilities (1 minute):** open Camera/Gallery from Profile, save picture on-device; open delivery area Google Map and tap a pin; show call/email implicit intents.
5. **Cloud CRUD (1 minute):** admin adds/edits a product and category; customer orders; admin reviews and changes an order status; explain Firestore Security Rules.
6. **Graphs and receipts (1 minute):** admin donut/monthly chart; open customer order receipt and Download PDF. **Share PDF** opens Android Sharesheet; select Quick Share on a supported phone to demonstrate real wireless exchange. Tap **Copy order summary as JSON** to show Gson serialization without personal contact data.
7. **Security/limitations (1 minute):** Firebase roles prevent customer admin privileges; photo is local-only; PayHere is sandbox and payment verification requires a trusted backend.

## Quick Q&A for the examiner

**What is an Activity?** An Android UI entry point with lifecycle callbacks such as onCreate, onResume and onDestroy.

**What is a Fragment?** A reusable portion of UI hosted by an Activity. It has a lifecycle and can be replaced inside the same Activity.

**What is a thread?** A separate execution path for non-UI operations. Example: image compression and receipt PDF generation use ExecutorService; UI changes return to the main thread.

**Where are SharedPreferences?** `AppSettings.java` saves theme and shake refresh options. `AppLockStore.java` saves a per-user salted passcode verifier and retry limits.

**What is Room?** A structured SQLite abstraction using Entity, Dao and Database. Local cart snapshot can be read offline; it does not prove full conflict-free server synchronization.

**Where do BroadcastReceivers appear?** In `MainActivity.java` for battery changes while visible. Connectivity monitoring uses `ConnectivityManager.NetworkCallback`, which is not a BroadcastReceiver.

**What is different between admin and customer?** A role-based router sends admins to Bakery Studio while customers use shopping screens. Server-side Firestore rules, not merely hidden UI buttons, enforce access.

**Does Share PDF implement Bluetooth?** It prepares a secure Android share URI and invokes Sharesheet. Quick Share can use Bluetooth/Wi-Fi on a supported device; actual transfer must be shown. It does not implement a custom Bluetooth stack.

**Where is the profile picture stored?** App-private internal files in `profile_photos/<uid>.jpg`; not in Firebase Storage. Device uninstallation clears it.

**What isn't production-ready?** Live payment verification, fully authoritative server pricing, full offline write sync, consistent receipt auditing, complete account-data deletion, multi-device profile photo sync.

## Test evidence checklist

Record emulator screenshots and date/result in the university submission. CI success alone does not verify these interactions.

| ID | Scenario | Expected |
| --- | --- | --- |
| T01 | Customer and admin login in succession | Different Home and bottom tabs |
| T02 | Change theme, restart app | Preference persists |
| T03 | Turn on shake, shake phone on Home | Catalog refreshes; no crash |
| T04 | Capture camera profile picture, restart | Local picture still visible in Profile and drawer |
| T05 | Open a different customer account | Does not show another user's local photo |
| T06 | Disconnect internet, open existing cart | Offline snapshot is readable; no false "order placed" |
| T07 | Start checkout and place a test COD order | Correct units/total and Firestore document |
| T08 | Admin updates order status | Customer tracking reflects change |
| T09 | Open Sales Reports | Charts match real order statuses |
| T10 | Save receipt PDF | Android document picker saves a valid PDF |
| T11 | Share receipt PDF to another device | Sharesheet opens; Quick Share wireless transfer verified if available |
| T11b | Copy order summary as JSON | Clipboard contains schemaVersion/orderId/items/totals; excludes customer name, address, phone and notes |
| T12 | Try an unauthorized user against admin data | Denied by Firestore rules |
| T13 | Rotate screen, increase system font | Inputs/buttons remain accessible |
| T14 | Enable/disable notification permission | No crash; notifications appear only if granted |
| T15 | Android Studio Build + GitHub Actions | Unit tests and debug APK pass |

## Highest-value next enhancements (after stable viva build)

1. **Improved documentation:** ER/schema diagram, architecture diagram, UML Activity/Fragment lifecycle, screenshots, test evidence table, limits and references.
2. **JSON demonstration:** run `OrderSummaryJsonTest` and demonstrate **Copy order summary as JSON**; future enhancement could add a validated JSON import with user consent.
3. **Actual wireless transfer test:** verify Quick Share PDF between two Android devices. If direct Bluetooth or Wi-Fi APIs are explicitly required, plan permission/device pairing/testing rather than claiming Sharesheet fulfills every requirement.
4. **Offline write queue & reconciliation:** WorkManager + Room pending actions, conflict detection, and server-authoritative totals; much bigger scope than a cosmetic change.
5. **Cloud backend hardening:** authoritative payment callback/webhook for PayHere, validated pricing on trusted server, audit trail for status updates and deletion workflow.
6. **Accessibility and UI testing:** Espresso tests, larger font sizes, TalkBack content descriptions, low-memory and rotation checks.

