package com.mariaxcodexpert.whatsdownloadplus;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.mariaxcodexpert.whatsdownloadplus.databinding.ActivityMainBinding;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private NavController navController;
    private AppBarConfiguration appBarConfiguration;

    // Notification Launcher
    private ActivityResultLauncher<String> requestNotificationPermissionLauncher;

    // App Update Checker
    private AppUpdateChecker appUpdateChecker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 1. GDPR + ADS HANDLING
        ConsentFormManager.init(this);
        ConsentFormManager.getInstance().requestConsentForm(() -> {
            if (AdManager.canRequestAds()) {
                AdManager.init(this);
            }
        });

        // 2. ViewBinding Setup
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.appBarMain.toolbar);

        // 3. Navigation Setup
        navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        setupAppBarConfiguration();
        setupDrawerActions();
        setupToolbarTitleUpdater();
        setupFAB();

        // 4. Initialize Update Checker (Fixing Variable Name)
        appUpdateChecker = new AppUpdateChecker(this);
        appUpdateChecker.checkForUpdate();

        // 5. Feedback Prompt Manager
        FeedbackPromptManager feedbackManager = new FeedbackPromptManager(this);
        feedbackManager.start();

        // 6. Notification Permission Launcher
        requestNotificationPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        Toast.makeText(this, "Notification permission granted ✅", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Enable notifications in settings for status alerts.", Toast.LENGTH_LONG).show();
                    }
                }
        );

        // Request permission for Android 13+
        askNotificationPermission();
    }

    private void askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Zaroori: Update check ko resume karein agar progress mein ho
        if (appUpdateChecker != null) {
            appUpdateChecker.onResume();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // Update result handle karein
        if (appUpdateChecker != null) {
            appUpdateChecker.onActivityResult(requestCode, resultCode, data);
        }
    }

    // =========================
    // Navigation & UI Helpers
    // =========================
    private void setupAppBarConfiguration() {
        DrawerLayout drawer = binding.drawerLayout;
        appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_home, R.id.nav_gallery, R.id.nav_download, R.id.nav_privacy_policy
        ).setOpenableLayout(drawer).build();

        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(binding.navView, navController);
    }

    private void setupDrawerActions() {
        binding.navView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                navController.popBackStack(navController.getGraph().getStartDestinationId(), false);
            } else if (id == R.id.nav_gallery || id == R.id.nav_download || id == R.id.nav_privacy_policy) {
                NavigationUI.onNavDestinationSelected(item, navController);
            } else if (id == R.id.nav_share_app) {
                shareApp();
            } else if (id == R.id.nav_rate_app) {
                openUrl("market://details?id=" + getPackageName(),
                        "https://play.google.com/store/apps/details?id=" + getPackageName());
            } else if (id == R.id.nav_feedback) {
                sendFeedback();
            }
            binding.drawerLayout.closeDrawers();
            return true;
        });
        binding.navView.setCheckedItem(R.id.nav_home);
    }

    private void setupToolbarTitleUpdater() {
        navController.addOnDestinationChangedListener((controller, destination, args) -> {
            int id = destination.getId();
            if (id == R.id.nav_home) {
                setToolbarTitle("Home");
            } else if (id == R.id.nav_gallery || id == R.id.nav_download) {
                setToolbarTitle(getString(R.string.app_name));
            } else if (id == R.id.nav_privacy_policy) {
                setToolbarTitle("Privacy Policy");
            }
        });
    }

    private void setToolbarTitle(String title) {
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(title);
        }
    }

    private void setupFAB() {
        binding.appBarMain.fab.setOnClickListener(v -> navController.navigate(R.id.nav_download));
    }

    private void shareApp() {
        String url = "https://play.google.com/store/apps/details?id=" + getPackageName();
        Intent sendIntent = new Intent(Intent.ACTION_SEND);
        sendIntent.setType("text/plain");
        sendIntent.putExtra(Intent.EXTRA_TEXT, "Download this amazing Status Downloader app:\n" + url);
        startActivity(Intent.createChooser(sendIntent, "Share App"));
    }

    private void sendFeedback() {
        openUrl("market://details?id=" + getPackageName(),
                "https://play.google.com/store/apps/details?id=" + getPackageName());
    }

    private void openUrl(String url, String fallbackUrl) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (Exception e) {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(fallbackUrl)));
        }
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
}