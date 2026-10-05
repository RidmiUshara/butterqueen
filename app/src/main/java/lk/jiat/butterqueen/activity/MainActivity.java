package lk.jiat.butterqueen.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import lk.jiat.butterqueen.AppLockGate;
import lk.jiat.butterqueen.R;
import lk.jiat.butterqueen.fragment.AboutFragment;
import lk.jiat.butterqueen.fragment.CategoryFragment;
import lk.jiat.butterqueen.fragment.DeliveryAreasFragment;
import lk.jiat.butterqueen.fragment.HomeFragment;
import lk.jiat.butterqueen.fragment.HelpFragment;
import lk.jiat.butterqueen.fragment.OrdersFragment;
import lk.jiat.butterqueen.fragment.ProfileFragment;
import lk.jiat.butterqueen.fragment.SettingFragment;
import lk.jiat.butterqueen.fragment.WishlistFragment;
import lk.jiat.butterqueen.fragment.cartFragment;
import lk.jiat.butterqueen.fragment.messageFragment;
import lk.jiat.butterqueen.model.Category;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private MaterialToolbar toolbar;
    private BottomNavigationView bottomNavigationView;
    private ActionBarDrawerToggle toggle;

    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firebaseFirestore;

    private ImageView headerAvatar;
    private TextView headerName;
    private TextView headerEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        firebaseAuth = FirebaseAuth.getInstance();
        firebaseFirestore = FirebaseFirestore.getInstance();

        drawerLayout = findViewById(R.id.drawerLayout);
        navigationView = findViewById(R.id.navigationView);
        toolbar = findViewById(R.id.toolbar);
        bottomNavigationView = findViewById(R.id.bottomNavigation);

        setSupportActionBar(toolbar);

        toggle = new ActionBarDrawerToggle(
                this,
                drawerLayout,
                toolbar,
                R.string.open_drawer,
                R.string.close_drawer
        );

        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        navigationView.setNavigationItemSelectedListener(this);
        navigationView.getMenu().findItem(R.id.nav_admin).setVisible(false);

        initDrawerHeader();
        listenToDrawerHeaderUser();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.mainContent), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
            return insets;
        });

        if (savedInstanceState == null) {
            loadFragment(new HomeFragment(), "Home");
            navigationView.setCheckedItem(R.id.nav_home);
        }

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.bottom_home) {
                loadFragment(new HomeFragment(), "Home");
                return true;

            } else if (id == R.id.bottom_cart) {
                loadFragment(new cartFragment(), "Cart");
                return true;

            } else if (id == R.id.bottom_wishlist) {
                loadFragment(new WishlistFragment(), "Wishlist");
                return true;

            } else if (id == R.id.bottom_profile) {
                loadFragment(new ProfileFragment(), "Profile");
                return true;
            }

            return false;
        });
    }

    private void initDrawerHeader() {
        if (navigationView.getHeaderCount() > 0) {
            View headerView = navigationView.getHeaderView(0);
            headerAvatar = headerView.findViewById(R.id.headerAvatar);
            headerName = headerView.findViewById(R.id.headerName);
            headerEmail = headerView.findViewById(R.id.headerEmail);
        }
    }

    private void listenToDrawerHeaderUser() {
        if (headerAvatar == null || headerName == null || headerEmail == null) {
            return;
        }

        if (firebaseAuth.getCurrentUser() == null) {
            headerName.setText("Guest User");
            headerEmail.setText("Please sign in");
            headerAvatar.setImageResource(R.drawable.account_circle_24px);
            return;
        }

        String userId = firebaseAuth.getCurrentUser().getUid();

        firebaseFirestore.collection("users")
                .document(userId)
                .addSnapshotListener(this, (documentSnapshot, error) -> {
                    if (error != null) {
                        return;
                    }

                    if (documentSnapshot != null && documentSnapshot.exists()) {
                        String firstName = documentSnapshot.getString("firstName");
                        String lastName = documentSnapshot.getString("lastName");
                        String email = documentSnapshot.getString("email");
                        String imageUrl = documentSnapshot.getString("profileImageUrl");

                        String fullName = ((firstName != null ? firstName : "") + " " +
                                (lastName != null ? lastName : "")).trim();

                        if (fullName.isEmpty()) {
                            fullName = "ButterQueen User";
                        }

                        navigationView.getMenu().findItem(R.id.nav_admin).setVisible(
                                "admin".equals(documentSnapshot.getString("role")));
                        headerName.setText(fullName);
                        headerEmail.setText(email != null ? email : "");

                        if (imageUrl != null && !imageUrl.isEmpty()) {
                            Glide.with(this)
                                    .load(imageUrl)
                                    .placeholder(R.drawable.account_circle_24px)
                                    .error(R.drawable.account_circle_24px)
                                    .into(headerAvatar);
                        } else {
                            headerAvatar.setImageResource(R.drawable.account_circle_24px);
                        }
                    }
                });
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_home) {
            loadFragment(new HomeFragment(), "Home");

        } else if (id == R.id.nav_profile) {
            loadFragment(new ProfileFragment(), "Profile");

        } else if (id == R.id.nav_orders) {
            loadFragment(new OrdersFragment(), "Orders");

        } else if (id == R.id.nav_admin) {
            startActivity(new Intent(this, AdminDashboardActivity.class));

        } else if (id == R.id.nav_category) {
            loadFragment(new CategoryFragment(), "Category");

        } else if (id == R.id.nav_wishlist) {
            loadFragment(new WishlistFragment(), "Wishlist");

        } else if (id == R.id.nav_cart) {
            loadFragment(new cartFragment(), "Cart");

        } else if (id == R.id.nav_delivery_areas) {
                loadFragment(new DeliveryAreasFragment(), "Delivery Areas");


        } else if (id == R.id.nav_message) {
            loadFragment(new messageFragment(), "Message");

        } else if (id == R.id.nav_setting) {
            loadFragment(new SettingFragment(), "Settings");

        } else if (id == R.id.nav_help) {
            openHelpScreen();

        } else if (id == R.id.nav_about) {
            openAboutScreen();

        } else if (id == R.id.nav_login) {
            Intent intent = new Intent(MainActivity.this, SignInActivity.class);
            startActivity(intent);

        } else if (id == R.id.nav_logout) {
            showLogoutDialog();
        }

        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    public void showCategoryProducts(Category category) {
        HomeFragment fragment = new HomeFragment();
        Bundle args = new Bundle();
        args.putString("category_id", category.getId());
        args.putString("category_name", category.getName());
        args.putString("category_unit", category.getUnitType());
        fragment.setArguments(args);
        loadFragment(fragment, category.getName());
    }

    public void openHelpScreen() {
        loadFragment(new HelpFragment(), "Help");
    }

    public void openAboutScreen() {
        loadFragment(new AboutFragment(), "About");
    }

    private void loadFragment(Fragment fragment, String title) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.contentFrame, fragment)
                .commit();

        toolbar.setTitle(title);
    }

    private void showLogoutDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Log Out")
                .setMessage("Are you sure do you want to log out?")
                .setCancelable(true)
                .setPositiveButton("Yes", (dialog, which) -> {
                    AppLockGate.lockNow();
                    firebaseAuth.signOut();

                    Toast.makeText(MainActivity.this, "Logged out successfully", Toast.LENGTH_SHORT).show();

                    Intent intent = new Intent(MainActivity.this, SignInActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }
}
