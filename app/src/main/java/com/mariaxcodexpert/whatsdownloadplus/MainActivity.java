package com.mariaxcodexpert.whatsdownloadplus;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Menu;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.mariaxcodexpert.whatsdownloadplus.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private NavController navController;
    private AppBarConfiguration appBarConfiguration;
    private NavController.OnDestinationChangedListener destinationListener;

    private ActivityResultLauncher<String> requestNotificationPermissionLauncher;
    private AppUpdateChecker appUpdateChecker;

    // Performance Handler to manage delayed tasks safely
    private final Handler performanceHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 1. GDPR + ADS HANDLING (Priority Execution)
        ConsentFormManager.init(this);
        ConsentFormManager.getInstance().requestConsentForm(() -> {
            if (AdManager.canRequestAds()) {
                AdManager.init(this);
            }
        });

        // 2. ViewBinding & UI Setup
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.appBarMain.toolbar);

        // 3. Navigation Engine Setup
        navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        setupAppBarConfiguration();
        setupDrawerActions();
        setupToolbarTitleUpdater();
        setupFAB();

        // 4. Critical Systems
        setupNotificationLauncher();
        setupBackPressedHandling();

        // 5. Performance Optimization: Delay heavy non-UI tasks
        // 1200ms is the sweet spot to let the Home Fragment settle first
        performanceHandler.postDelayed(this::initSecondaryTasks, 1200);
    }

    private void initSecondaryTasks() {
        if (isFinishing() || isDestroyed()) return;

        // Background Checkers
        appUpdateChecker = new AppUpdateChecker(this);
        appUpdateChecker.checkForUpdate();

        new FeedbackPromptManager(this).start();

        // Android 13+ Permission request
        askNotificationPermission();
    }

    private void setupBackPressedHandling() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // 1. First priority: Close Drawer if open
                if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    binding.drawerLayout.closeDrawer(GravityCompat.START);
                } else {
                    // 2. Fragment Navigation Logic
                    int currentId = (navController.getCurrentDestination() != null)
                            ? navController.getCurrentDestination().getId() : -1;

                    if (currentId == R.id.nav_home || currentId == -1) {
                        // Directly exit if on Home (Prevents going back to Splash/Permissions)
                        finish();
                    } else {
                        // Smoothly go back to Home from any other fragment
                        navController.popBackStack(R.id.nav_home, false);
                    }
                }
            }
        });
    }

    private void setupDrawerActions() {
        binding.navView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();

            // Optimization: Don't reload if already on the same fragment
            if (navController.getCurrentDestination() != null &&
                    navController.getCurrentDestination().getId() == id) {
                binding.drawerLayout.closeDrawers();
                return false;
            }

            // Close drawer immediately for visual feedback
            binding.drawerLayout.closeDrawers();

            // 🔥 LAG FIX: Execute navigation AFTER drawer close animation (approx 280ms)
            performanceHandler.postDelayed(() -> {
                if (isFinishing() || isDestroyed()) return;

                if (id == R.id.nav_home) {
                    navController.popBackStack(navController.getGraph().getStartDestinationId(), false);
                } else if (id == R.id.nav_gallery) {
                    // Custom bundle for direct Video Tab access
                    Bundle bundle = new Bundle();
                    bundle.putBoolean("showVideos", true);
                    navController.navigate(R.id.nav_gallery, bundle);
                } else if (id == R.id.nav_download || id == R.id.nav_privacy_policy) {
                    NavigationUI.onNavDestinationSelected(item, navController);
                } else {
                    // Static actions (Share/Rate/Feedback)
                    handleMenuActions(id);
                }
            }, 280);

            return true;
        });
    }

    private void handleMenuActions(int id) {
        if (id == R.id.nav_share_app) shareApp();
        else if (id == R.id.nav_rate_app) openUrl("market://details?id=" + getPackageName(),
                "https://play.google.com/store/apps/details?id=" + getPackageName());
        else if (id == R.id.nav_feedback) sendFeedback();
    }

    private void setupToolbarTitleUpdater() {
        destinationListener = (controller, destination, args) -> {
            int id = destination.getId();
            if (id == R.id.nav_home) setToolbarTitle("Home");
            else if (id == R.id.nav_gallery || id == R.id.nav_download) setToolbarTitle(getString(R.string.app_name));
            else if (id == R.id.nav_privacy_policy) setToolbarTitle("Privacy Policy");
        };
        navController.addOnDestinationChangedListener(destinationListener);
    }

    private void setupAppBarConfiguration() {
        appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_home, R.id.nav_gallery, R.id.nav_download, R.id.nav_privacy_policy
        ).setOpenableLayout(binding.drawerLayout).build();

        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(binding.navView, navController);
    }

    // --- System & Helper Methods ---

    private void setToolbarTitle(String title) {
        if (getSupportActionBar() != null) getSupportActionBar().setTitle(title);
    }

    private void setupFAB() {
        binding.appBarMain.fab.setOnClickListener(v -> navController.navigate(R.id.nav_download));
    }

    private void setupNotificationLauncher() {
        requestNotificationPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (!isGranted) Toast.makeText(this, "Enable notifications for status alerts.", Toast.LENGTH_SHORT).show();
                }
        );
    }

    private void askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        }
    }

    private void openUrl(String url, String fallback) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (Exception e) {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(fallback)));
        }
    }

    private void shareApp() {
        String url = "https://play.google.com/store/apps/details?id=" + getPackageName();
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, "Download Status Saver:\n" + url);
        startActivity(Intent.createChooser(intent, "Share via"));
    }

    private void sendFeedback() {
        openUrl("market://details?id=" + getPackageName(), "https://play.google.com/store/apps/details?id=" + getPackageName());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onSupportNavigateUp() {
        return NavigationUI.navigateUp(navController, appBarConfiguration) || super.onSupportNavigateUp();
    }

    @Override
    protected void onDestroy() {
        // Clean up to prevent memory leaks
        performanceHandler.removeCallbacksAndMessages(null);
        if (navController != null && destinationListener != null) {
            navController.removeOnDestinationChangedListener(destinationListener);
        }
        super.onDestroy();
        binding = null;
    }
}