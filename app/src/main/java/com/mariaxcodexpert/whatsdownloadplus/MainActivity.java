package com.mariaxcodexpert.whatsdownloadplus;

import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.core.view.WindowCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;


import com.google.firebase.messaging.FirebaseMessaging;
import com.mariaxcodexpert.whatsdownloadplus.databinding.ActivityMainBinding;
import com.mariaxcodexpert.whatsdownloadplus.ui.Search.OnlineSearchFragment;
import com.mariaxcodexpert.whatsdownloadplus.utils.NavigationHelper;
import com.mariaxcodexpert.whatsdownloadplus.utils.PermissionManager;
import com.mariaxcodexpert.whatsdownloadplus.utils.UiUtils;
import com.mariaxcodexpert.whatsdownloadplus.BuildConfig;
public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding binding;
    private NavController navController;
    private AppBarConfiguration appBarConfiguration;
    private PermissionManager permissionManager;
    private NavigationHelper navHelper;
    private FeedbackPromptManager feedbackManager;
    private int pendingNavigationId = -1;
    private AppUpdateChecker appUpdateChecker;
    private static final String TAG = "MainActivity_Log";
    // 🔥 Ye line add karein (Error khatam ho jaye ga)
    private BillingManager billingManager;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.Theme_WhatsDownloadPlus);
        super.onCreate(savedInstanceState);

        // Hardware Acceleration for smooth UI
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        // Call WhatsNewDialog
        // Tip: BuildConfig.VERSION_CODE use karein taake har update pe auto-detect ho
        WhatsNewDialog.display(this, BuildConfig.VERSION_CODE);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.appBarMain.toolbar);

        permissionManager = new PermissionManager(this, binding);
        navHelper = new NavigationHelper(this, binding);
        feedbackManager = new FeedbackPromptManager(this);
        // 6. App Update System (Modern Launcher Approach)
        appUpdateChecker = new AppUpdateChecker(this, updateLauncher);
        appUpdateChecker.checkForUpdate();
        setupNavigation();
        setupToolbarActions();
        setupFAB();

        // 🔥 Notification Permission (Android 13+)
        checkAndRequestNotificationPermission();

        // 🔥 Firebase Token Logging
        fetchFirebaseToken();

        // 🔥 Handle Notification Click (Deep Linking)
        handleNotificationIntent(getIntent());




        Looper.myQueue().addIdleHandler(() -> {
            if (!isFinishing() && !isDestroyed()) {
                initSecondaryServices();
            }
            return false;
        });
    }

    private void fetchFirebaseToken() {
        FirebaseMessaging.getInstance().getToken().addOnCompleteListener(task -> {
            if (!task.isSuccessful()) {
                Log.w(TAG, "Fetching FCM registration token failed", task.getException());
                return;
            }
            String token = task.getResult();
            Log.d("FCM_TOKEN", "Device Token: " + token);
        });
    }

    private void checkAndRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }
    }

    private void handleNotificationIntent(Intent intent) {
        if (intent != null && intent.hasExtra("openFragment")) {
            String fragmentName = intent.getStringExtra("openFragment");
            Log.d(TAG, "Notification Intent received for: " + fragmentName);

            if ("ImagesAndVideo".equals(fragmentName)) {
                // Wait for NavController to be ready
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    if (navController != null) {
                        navController.navigate(R.id.nav_gallery);
                    }
                }, 500);
            }
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent); // Set new intent for handleNotificationIntent
        handleNotificationIntent(intent);
    }

    private void setupToolbarActions() {
        View toolbar = binding.appBarMain.toolbar;

        // Find the new views
        LinearLayout iconContainer = toolbar.findViewById(R.id.icon_list_container);
        ImageView btnToggle = toolbar.findViewById(R.id.btn_toggle_menu);

        // --- Toggle Logic for Icons List ---
        btnToggle.setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY); // Haptic feedback for premium feel

            if (iconContainer.getVisibility() == View.GONE) {
                // List ko show karo
                iconContainer.setVisibility(View.VISIBLE);
                // Arrow ko 180 degree rotate karo (smooth animation)
                btnToggle.animate().rotation(180).setDuration(300).start();
            } else {
                // List ko hide karo
                iconContainer.setVisibility(View.GONE);
                // Arrow ko wapis 0 degree pe lao
                btnToggle.animate().rotation(0).setDuration(300).start();
            }
        });

        // --- Existing Button Listeners (Now inside the container) ---
        toolbar.findViewById(R.id.custom_more_apps).setOnClickListener(v -> showOurAppsDialog());
       // toolbar.findViewById(R.id.custom_how_to_use).setOnClickListener(v -> TutorialHelper.show(this));
        toolbar.findViewById(R.id.custom_whatsapp).setOnClickListener(v -> UiUtils.openWhatsApp(this));
        toolbar.findViewById(R.id.custom_notif_layout).setOnClickListener(v -> {
            permissionManager.handleNotificationButtonClick();
        });

        permissionManager.updateNotificationDot();
    }
    private void showOurAppsDialog() {
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_more_apps);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
            lp.copyFrom(dialog.getWindow().getAttributes());
            lp.width = (int) (getResources().getDisplayMetrics().widthPixels * 0.90);
            lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
            dialog.getWindow().setAttributes(lp);
        }

        dialog.findViewById(R.id.app_item_photo_enhancer).setOnClickListener(v -> {
            launchPlayStore("com.mariaxcodexpert.easyclickcounter");
            dialog.dismiss();
        });
        dialog.findViewById(R.id.btn_close_dialog).setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void launchPlayStore(String packageName) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + packageName)));
        } catch (Exception e) {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + packageName)));
        }
    }

    private void setupNavigation() {
        NavHostFragment navHost = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment_content_main);

        // Icons ka original color barkrar rakhne k liye
        binding.navView.setItemIconTintList(null);

        if (navHost != null) {
            navController = navHost.getNavController();
            appBarConfiguration = new AppBarConfiguration.Builder(
                    R.id.nav_home, R.id.nav_gallery, R.id.nav_download, R.id.nav_privacy_policy
            ).setOpenableLayout(binding.drawerLayout).build();

            NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);

            // 🔥 Smooth Animation Listener
            binding.drawerLayout.addDrawerListener(new DrawerLayout.SimpleDrawerListener() {
                @Override
                public void onDrawerStateChanged(int newState) {
                    // Animation k doran hardware layer use krain ta k lag na ho
                    if (newState != DrawerLayout.STATE_IDLE) {
                        binding.drawerLayout.setLayerType(View.LAYER_TYPE_HARDWARE, null);
                    } else {
                        binding.drawerLayout.setLayerType(View.LAYER_TYPE_NONE, null);
                    }
                }

                @Override
                public void onDrawerClosed(View drawerView) {
                    super.onDrawerClosed(drawerView);
                    // Sirf tab navigate krain jab drawer mukamal band ho chuka ho
                    if (pendingNavigationId != -1) {
                        navController.navigate(pendingNavigationId);
                        pendingNavigationId = -1;
                    }
                }
            });

            binding.navView.setNavigationItemSelectedListener(item -> {
                int id = item.getItemId();
                int currentId = (navController.getCurrentDestination() != null) ? navController.getCurrentDestination().getId() : -1;

                if (id == currentId) {
                    binding.drawerLayout.closeDrawer(GravityCompat.START);
                    return true;
                }

                // Click hote hi navigate na krain, pehle drawer band hone dain
                pendingNavigationId = id;
                binding.drawerLayout.closeDrawer(GravityCompat.START);
                return true;
            });

            navHelper.setupDrawer(navController);
        }

        // Back Press Handling
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    binding.drawerLayout.closeDrawer(GravityCompat.START);
                } else {
                    if (navController.getCurrentDestination() != null &&
                            navController.getCurrentDestination().getId() != R.id.nav_home) {
                        navController.popBackStack(R.id.nav_home, false);
                    } else {
                        finish();
                    }
                }
            }
        });
    }
    private void setupFAB() {
        binding.appBarMain.fab.setOnClickListener(v -> {
            // Premium feel ke liye haptic feedback
            v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);

            // Click animation ke baad Fragment par navigate karein
            UiUtils.animateClick(v, () -> {
                if (navController != null) {
                    // Navigation graph (mobile_navigation.xml) mein jo ID rakhi hai wo use karein
                    navController.navigate(R.id.nav_online_search);
                }
            });
        });

        // FAB par pulse animation chalti rahegi taake user ka dhyan jaye
        UiUtils.startPulseAnimation(binding.appBarMain.fab);
    }

    private void initSecondaryServices() {
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (!isFinishing() && !isDestroyed()) {

                // 🔥 Check current status from SharedPreferences again for double safety
                android.content.SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
                AdManager.isPremiumUser = prefs.getBoolean("isPremium", false);

                if (AdManager.isPremiumUser) {
                    Log.d(TAG, "Premium User: Ads and Consent Form Bypassed.");
                } else {
                    // Free User: Load Ads and Consent
                    ConsentFormManager.init(this);
                    ConsentFormManager.getInstance().requestConsentForm(() -> {
                        if (!isFinishing() && !isDestroyed() && AdManager.canRequestAds()) {
                            AdManager.init(this);
                        }
                    });
                }

                // Update checker hamesha chalna chahiye (Premium ho ya Free)
                if (appUpdateChecker != null) {
                    appUpdateChecker.checkForUpdate();
                }
            }
        }, 1500);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // 1. Instant Cache Check
        // SharedPrefs se foran status uthain ta k UI delay na ho
        android.content.SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        AdManager.isPremiumUser = prefs.getBoolean("isPremium", false);

        // 2. Background Billing Sync (Anonymous Class)
        if (billingManager == null) {
            billingManager = new BillingManager(this, new BillingManager.BillingCallback() {
                @Override
                public void onPremiumPurchased() {
                    Log.d("MainActivity", "onResume: Billing status updated silently.");
                    if (AdManager.isPremiumUser) {
                        AdManager.releaseAllAds();
                    }
                }

                @Override
                public void onBillingError(int errorCode, String technicalMessage) {
                    // Background sync errors are logged only
                    Log.e("MainActivity", "Background Sync Error: " + technicalMessage);
                }
            });
        }

        // Refresh status from Google Play
        if (billingManager != null) {
            billingManager.startConnection();
        }

        // 3. Immediate Ads Cleanup
        if (AdManager.isPremiumUser) {
            AdManager.releaseAllAds();
            Log.d("MainActivity", "onResume: Premium Active - Ads Purged");
        } else {
            // Free user: Initialize/Resume ads
            if (AdManager.canRequestAds()) {
                AdManager.init(this);
            }
        }

        // 4. Other Maintenance
        if (permissionManager != null) {
            permissionManager.updateNotificationDot();
        }

        if (appUpdateChecker != null) {
            appUpdateChecker.onResume();
        }
    }
    // Top par variables ke saath define karein
    private final ActivityResultLauncher<IntentSenderRequest> updateLauncher =
            registerForActivityResult(new ActivityResultContracts.StartIntentSenderForResult(),
                    result -> {
                        if (result.getResultCode() != RESULT_OK) {
                            Log.e("AppUpdate", "Update flow failed or cancelled: " + result.getResultCode());
                        }
                    });

    @Override
    public boolean onSupportNavigateUp() {
        return NavigationUI.navigateUp(navController, appBarConfiguration) || super.onSupportNavigateUp();
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}