package com.mariaxcodexpert.whatsdownloadplus;

import android.Manifest;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.graphics.drawable.Drawable;
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
    private ActivityResultLauncher<String> requestStoragePermissionLauncher;

    private AppUpdateChecker appUpdateChecker;

    // Performance Handler to manage delayed tasks safely
    private final Handler performanceHandler = new Handler(Looper.getMainLooper());
    public static boolean isUIReady = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // 1. THEME & WINDOW OPTIMIZATION
        setTheme(R.style.Theme_WhatsDownloadPlus);
        super.onCreate(savedInstanceState);

        // 🔥 STATUS BAR & BUTTONS COLOR FIX (Edge-to-Edge)
        // Isse aapka gradient/color bars ke peeche nazar aayega
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        // Window background ko white set karna (Safe side)
        getWindow().setBackgroundDrawableResource(android.R.color.white);

        // 2. GDPR + ADS HANDLING
        ConsentFormManager.init(this);
        ConsentFormManager.getInstance().requestConsentForm(() -> {
            if (AdManager.canRequestAds()) {
                AdManager.init(this);
            }
        });

        // 3. ViewBinding & UI Setup
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.appBarMain.toolbar);

        // 4. 🔥 NAVIGATION ENGINE SETUP (CRASH FIX)
        // NavHostFragment ke zariye NavController dhoondhein taake IllegalStateException na aaye
        androidx.navigation.fragment.NavHostFragment navHostFragment = (androidx.navigation.fragment.NavHostFragment)
                getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment_content_main);

        if (navHostFragment != null) {
            navController = navHostFragment.getNavController();

            // Navigation dependent setups
            setupAppBarConfiguration();
            setupDrawerActions();
            setupToolbarTitleUpdater();

            // --- FAB ANIMATION SETUP ---
            setupFAB();
        } else {
            // Log ya Toast agar fragment na miley (Debugging ke liye)
            android.util.Log.e("MainActivity", "NavHostFragment not found!");
        }

        // 5. Critical Systems & Notification Handling
        setupNotificationLauncher();

        setupBackPressedHandling();

        if (getIntent() != null) {
            handleNotificationIntent(getIntent());
        }

        // 6. Splash Screen Sync
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            isUIReady = true;
        }, 300);

        // 7. Secondary Tasks
        performanceHandler.postDelayed(this::initSecondaryTasks, 1200);
    }

    private void setupFAB() {
        // 1. Click Listener
        binding.appBarMain.fab.setOnClickListener(v -> navController.navigate(R.id.nav_download));

        // 2. 🔥 Animated Vector / Attractive Animation
        // Agar aapne avd_download.xml banaya hai toh ye use karein:
        try {
            Drawable drawable = ContextCompat.getDrawable(this, R.drawable.avd_download);
            if (drawable instanceof AnimatedVectorDrawable) {
                binding.appBarMain.fab.setImageDrawable(drawable);
                ((AnimatedVectorDrawable) drawable).start();
            } else {
                // Fallback: Agar AVD nahi hai toh Pulse Animation chalao (Bina file ke attractive lagega)
                startFabPulseAnimation();
            }
        } catch (Exception e) {
            startFabPulseAnimation();
        }
    }

    private void startFabPulseAnimation() {
        // AnimatorSet use karne se dono animation ek sath start/stop hongi
        AnimatorSet pulseAnimation = new AnimatorSet();

        ObjectAnimator scaleX = ObjectAnimator.ofFloat(binding.appBarMain.fab, "scaleX", 1.0f, 1.1f, 1.0f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(binding.appBarMain.fab, "scaleY", 1.0f, 1.1f, 1.0f);

        scaleX.setRepeatCount(ValueAnimator.INFINITE);
        scaleY.setRepeatCount(ValueAnimator.INFINITE);

        pulseAnimation.playTogether(scaleX, scaleY);
        pulseAnimation.setDuration(1500);
        pulseAnimation.start();
    }



    private void handleNotificationIntent(Intent intent) {
        if (intent != null && intent.hasExtra("openFragment")) {
            String fragmentName = intent.getStringExtra("openFragment");
            boolean isVideo = intent.getBooleanExtra("isVideo", false);

            if ("ImagesAndVideo".equals(fragmentName)) {
                Bundle bundle = new Bundle();
                bundle.putBoolean("showVideos", isVideo);
                navController.navigate(R.id.nav_gallery, bundle);
                intent.removeExtra("openFragment");
            }
        }
    }

    private void initSecondaryTasks() {
        if (isFinishing() || isDestroyed()) return;

        appUpdateChecker = new AppUpdateChecker(this);
        appUpdateChecker.checkForUpdate();

        new FeedbackPromptManager(this).start();

        // 1. Notification permission mangi jayegi (Zaroori hai)
        askNotificationPermission();

    }


    private void setupBackPressedHandling() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    binding.drawerLayout.closeDrawer(GravityCompat.START);
                } else {
                    int currentId = (navController.getCurrentDestination() != null)
                            ? navController.getCurrentDestination().getId() : -1;

                    if (currentId == R.id.nav_home || currentId == -1) {
                        finish();
                    } else {
                        navController.popBackStack(R.id.nav_home, false);
                    }
                }
            }
        });
    }

    private void setupDrawerActions() {
        binding.navView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            if (navController.getCurrentDestination() != null &&
                    navController.getCurrentDestination().getId() == id) {
                binding.drawerLayout.closeDrawers();
                return false;
            }
            binding.drawerLayout.closeDrawers();
            performanceHandler.postDelayed(() -> {
                if (isFinishing() || isDestroyed()) return;
                if (id == R.id.nav_home) {
                    navController.popBackStack(navController.getGraph().getStartDestinationId(), false);
                } else if (id == R.id.nav_gallery) {
                    Bundle bundle = new Bundle();
                    bundle.putBoolean("showVideos", true);
                    navController.navigate(R.id.nav_gallery, bundle);
                } else if (id == R.id.nav_download || id == R.id.nav_privacy_policy) {
                    NavigationUI.onNavDestinationSelected(item, navController);
                } else {
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

    private void setToolbarTitle(String title) {
        if (getSupportActionBar() != null) getSupportActionBar().setTitle(title);
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
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleNotificationIntent(intent);
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
        performanceHandler.removeCallbacksAndMessages(null);
        if (navController != null && destinationListener != null) {
            navController.removeOnDestinationChangedListener(destinationListener);
        }
        super.onDestroy();
        binding = null;
        isUIReady = false;
    }
}