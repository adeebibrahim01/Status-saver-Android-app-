package com.mariaxcodexpert.whatsdownloadplus;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.Menu;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.navigation.NavigationView;
import com.mariaxcodexpert.whatsdownloadplus.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration mAppBarConfiguration;
    private ActivityMainBinding binding;
    private NavController navController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        AdManager.init(this);
        AdManager.loadInterstitial(this);


        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.appBarMain.toolbar);

        DrawerLayout drawer = binding.drawerLayout;
        NavigationView navigationView = binding.navView;

        navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);

        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_home,
                R.id.nav_gallery,
                R.id.nav_download,

                R.id.nav_status_prediction,
                R.id.nav_privacy_policy
        )
                .setOpenableLayout(drawer)
                .build();

        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);

        // ====================================
        // 🔥 Custom Drawer Item Click Handler
        // ====================================
        navigationView.setNavigationItemSelectedListener(item -> {

            int id = item.getItemId();

                    if (id == R.id.nav_home) {
                        // Pop back stack to start destination (Home) so it always shows
                        navController.popBackStack(navController.getGraph().getStartDestinationId(), false);
                    }
                    else if (id == R.id.nav_gallery ||
                            id == R.id.nav_download ||

                            id == R.id.nav_status_prediction ||
                            id == R.id.nav_privacy_policy) {

                        NavigationUI.onNavDestinationSelected(item, navController);

                }
            // --- Share App ---
            else if (id == R.id.nav_share_app) {
                shareApp();
            }
            // --- Rate App ---
            else if (id == R.id.nav_rate_app) {
                rateApp();
            }
            // --- Feedback ---
            else if (id == R.id.nav_feedback) {
                sendFeedback();
            }

            drawer.closeDrawers();
            return true;
        });

        // ========= FAB Shortcut =========
        binding.appBarMain.fab.setOnClickListener(v ->
                navController.navigate(R.id.nav_download)
        );

        // ========= Toolbar Titles =========
        navController.addOnDestinationChangedListener((controller, destination, args) -> {

            int id = destination.getId();

            if (id == R.id.nav_home) {
                binding.appBarMain.toolbar.setTitle("Home");
                setJoinedDate();

            } else if (id == R.id.nav_gallery) {
                boolean showVideos = args != null && args.getBoolean("showVideos", false);
                binding.appBarMain.toolbar.setTitle(showVideos ? "Videos" : "Images");

            } else if (id == R.id.nav_download) {
                binding.appBarMain.toolbar.setTitle("Download");

            } else if (id == R.id.nav_status_prediction) {
                binding.appBarMain.toolbar.setTitle("AI Status Prediction");

            } else if (id == R.id.nav_privacy_policy) {
                binding.appBarMain.toolbar.setTitle("Privacy Policy");
            }
        });

        // Highlight Home by default
        navigationView.setCheckedItem(R.id.nav_home);
    }

    // =========================================
    // 🔥 Share App
    // =========================================
    private void shareApp() {
        String url = "https://play.google.com/store/apps/details?id=" + getPackageName();
        Intent sendIntent = new Intent(Intent.ACTION_SEND);
        sendIntent.setType("text/plain");
        sendIntent.putExtra(Intent.EXTRA_TEXT,
                "Download this amazing Status Downloader app:\n" + url);
        startActivity(Intent.createChooser(sendIntent, "Share App"));
    }

    // =========================================
    // 🔥 Rate App
    // =========================================
    private void rateApp() {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW,
                    Uri.parse("market://details?id=" + getPackageName())));
        } catch (Exception e) {
            startActivity(new Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/apps/details?id=" + getPackageName())));
        }
    }

    // =========================================
    // 🔥 Feedback
    // =========================================
    private void sendFeedback() {
        Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
        emailIntent.setData(Uri.parse("mailto:mariaadeeb982@gmail.com"));
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Feedback - WhatsDownload Plus");
        startActivity(emailIntent);
    }

    // =========================================
    // 🔥 Joined Date Logic
    // =========================================
    private void setJoinedDate() {
        try {
            TextView joinedText = findViewById(R.id.joinedText);
            if (joinedText != null) {
                PackageInfo info = getPackageManager().getPackageInfo(getPackageName(), 0);
                long time = info.firstInstallTime;
                String date = DateFormat.format("MMMM dd, yyyy", time).toString();
                joinedText.setText("Joined Status Downloader Plus on " + date + ".");
            }
        } catch (Throwable ignored) {
            // ignored
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onSupportNavigateUp() {
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }

    // =========================================
    // 🔥 Open Gallery with specific tab
    // =========================================
    public void openGalleryTab(boolean showVideos) {
        Bundle args = new Bundle();
        args.putBoolean("showVideos", showVideos);
        navController.navigate(R.id.nav_gallery, args);
    }
}
