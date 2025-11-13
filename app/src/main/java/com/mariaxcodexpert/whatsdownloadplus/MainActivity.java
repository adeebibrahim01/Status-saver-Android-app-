package com.mariaxcodexpert.whatsdownloadplus;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
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

import android.os.Bundle;

public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration mAppBarConfiguration;
    private ActivityMainBinding binding;
    private NavController navController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.appBarMain.toolbar);

        DrawerLayout drawer = binding.drawerLayout;
        NavigationView navigationView = binding.navView;

        navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);

        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_home, R.id.nav_gallery, R.id.nav_download, R.id.nav_notifications)
                .setOpenableLayout(drawer)
                .build();

        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);

        // ===== Navigation Drawer clicks =====
        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_home) {
                navController.popBackStack(navController.getGraph().getStartDestinationId(), false);
                navController.navigate(R.id.nav_home);
            } else if (id == R.id.nav_gallery) {
                // Pass argument to select Images tab by default
                Bundle args = new Bundle();
                args.putBoolean("showVideos", false); // false = Images tab
                navController.navigate(R.id.nav_gallery, args);



            } else if (id == R.id.nav_download) {
                navController.navigate(R.id.nav_download);
            } else if (id == R.id.nav_notifications) {
                navController.navigate(R.id.nav_notifications);
            }

            drawer.closeDrawers();
            return true;
        });

        // ===== FAB click navigates to Slideshow =====
        binding.appBarMain.fab.setOnClickListener(view -> navController.navigate(R.id.nav_download));

        // ===== Toolbar title and joined date dynamically =====
        navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
            if (destination.getId() == R.id.nav_home) {
                binding.appBarMain.toolbar.setTitle("Home");

                try {
                    TextView joinedText = findViewById(R.id.joinedText);
                    if (joinedText != null) {
                        PackageInfo packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
                        long firstInstallTime = packageInfo.firstInstallTime;
                        String formattedDate = DateFormat.format("MMMM dd, yyyy", firstInstallTime).toString();
                        joinedText.setText("Joined Status downloader plus on " + formattedDate + ".");
                    }
                } catch (PackageManager.NameNotFoundException e) {
                    e.printStackTrace();
                }

            } else if (destination.getId() == R.id.nav_gallery) {
                boolean showVideos = arguments != null && arguments.getBoolean("showVideos", false);
                binding.appBarMain.toolbar.setTitle(showVideos ? "Videos" : "Images");
            } else if (destination.getId() == R.id.nav_download) {
                binding.appBarMain.toolbar.setTitle("Download");
            } else if (destination.getId() == R.id.nav_notifications) {
                binding.appBarMain.toolbar.setTitle("Notifications");
            }
        });

        // ===== Highlight Home menu at start =====
        navigationView.setCheckedItem(R.id.nav_home);
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

    // ===== Add helper methods to navigate directly to Images or Videos tab =====
    public void openGalleryTab(boolean showVideos) {
        Bundle args = new Bundle();
        args.putBoolean("showVideos", showVideos);
        navController.navigate(R.id.nav_gallery, args);
    }
}
