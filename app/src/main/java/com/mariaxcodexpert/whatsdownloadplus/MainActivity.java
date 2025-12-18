package com.mariaxcodexpert.whatsdownloadplus;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
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
    private ConsentFormManager consentFormManager;
    // AppUpdateChecker instance
    private AppUpdateChecker updateChecker;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // ==============================
        // GDPR + ADS HANDLING (Simplified)
        // ==============================
        // Consent + Ads
        ConsentFormManager.init(this);

        ConsentFormManager.getInstance().requestConsentForm(() -> {
            // After consent is ready
            if (AdManager.canRequestAds()) {
                AdManager.init(this); // ensure ad is loaded
            }

        });

        // ViewBinding
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.appBarMain.toolbar);

        // Navigation setup
        navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        setupAppBarConfiguration();
        setupDrawerActions();
        setupToolbarTitleUpdater();
        setupFAB();


        // Initialize update checker
        updateChecker = new AppUpdateChecker(this);
        updateChecker.checkForUpdate(); // ✅ This is now called properly


        // Start feedback prompt manager
        FeedbackPromptManager feedbackManager = new FeedbackPromptManager(this);
        feedbackManager.start();


    }



    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (updateChecker != null) {
            updateChecker.onActivityResult(requestCode, resultCode, data);
        }
    }

    // =========================
    // AppBarConfiguration
    // =========================
    private void setupAppBarConfiguration() {
        DrawerLayout drawer = binding.drawerLayout;
        appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_home,
                R.id.nav_gallery,
                R.id.nav_download,
                R.id.nav_privacy_policy
        ).setOpenableLayout(drawer).build();

        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(binding.navView, navController);
    }

    // =========================
    // Drawer Actions
    // =========================
    private void setupDrawerActions() {
        Map<Integer, Runnable> drawerActions = new HashMap<>();
        drawerActions.put(R.id.nav_home, () -> navController.popBackStack(navController.getGraph().getStartDestinationId(), false));
        drawerActions.put(R.id.nav_gallery, () -> NavigationUI.onNavDestinationSelected(binding.navView.getMenu().findItem(R.id.nav_gallery), navController));
        drawerActions.put(R.id.nav_download, () -> NavigationUI.onNavDestinationSelected(binding.navView.getMenu().findItem(R.id.nav_download), navController));
        drawerActions.put(R.id.nav_privacy_policy, () -> NavigationUI.onNavDestinationSelected(binding.navView.getMenu().findItem(R.id.nav_privacy_policy), navController));
        drawerActions.put(R.id.nav_share_app, this::shareApp);
        drawerActions.put(R.id.nav_rate_app, () -> openUrl(
                "market://details?id=" + getPackageName(),
                "https://play.google.com/store/apps/details?id=" + getPackageName()));
        drawerActions.put(R.id.nav_feedback, this::sendFeedback);

        binding.navView.setNavigationItemSelectedListener(item -> {
            Runnable action = drawerActions.get(item.getItemId());
            if (action != null) action.run();
            binding.drawerLayout.closeDrawers();
            return true;
        });

        binding.navView.setCheckedItem(R.id.nav_home);
    }

    // =========================
    // Toolbar Titles
    // =========================
    private void setupToolbarTitleUpdater() {
        navController.addOnDestinationChangedListener((controller, destination, args) -> {
            int id = destination.getId();
            if (id == R.id.nav_home) {
                setToolbarTitle("Home");
            }else if (id == R.id.nav_gallery) {
                String appName = getString(R.string.app_name); // dynamically fetch app name
                setToolbarTitle(appName);
            }
            else if (id == R.id.nav_download) {
                setToolbarTitle("Download");
            } else if (id == R.id.nav_privacy_policy) {
                setToolbarTitle("Privacy Policy");
            }
        });
    }

    private void setToolbarTitle(String title) {
        binding.appBarMain.toolbar.setTitle(title);
    }

    // =========================
    // FAB Shortcut
    // =========================
    private void setupFAB() {
        binding.appBarMain.fab.setOnClickListener(v ->
                navController.navigate(R.id.nav_download)
        );
    }

    // =========================
    // Utility Methods
    // =========================
    private void shareApp() {
        String url = "https://play.google.com/store/apps/details?id=" + getPackageName();
        Intent sendIntent = new Intent(Intent.ACTION_SEND);
        sendIntent.setType("text/plain");
        sendIntent.putExtra(Intent.EXTRA_TEXT, "Download this amazing Status Downloader app:\n" + url);
        startActivity(Intent.createChooser(sendIntent, "Share App"));
    }

    private void sendFeedback() {
        Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
        emailIntent.setData(Uri.parse("mailto:mariaadeeb982@gmail.com"));
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Feedback - WhatsDownload Plus");
        startActivity(emailIntent);
    }

    private void openUrl(String url, String fallbackUrl) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (Exception e) {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(fallbackUrl)));
        }
    }

    // =========================
    // Open Gallery Tab
    // =========================
    public void openGalleryTab(boolean showVideos) {
        Bundle args = new Bundle();
        args.putBoolean("showVideos", showVideos);
        navController.navigate(R.id.nav_gallery, args);
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
