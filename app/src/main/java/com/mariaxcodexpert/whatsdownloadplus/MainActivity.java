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
import android.view.Window;
import android.view.WindowManager;
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
import androidx.navigation.NavOptions;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.google.firebase.messaging.FirebaseMessaging;
import com.mariaxcodexpert.whatsdownloadplus.databinding.ActivityMainBinding;
import com.mariaxcodexpert.whatsdownloadplus.utils.NavigationHelper;
import com.mariaxcodexpert.whatsdownloadplus.utils.PermissionManager;
import com.mariaxcodexpert.whatsdownloadplus.utils.UiUtils;

public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding binding;
    private NavController navController;
    private AppBarConfiguration appBarConfiguration;
    private PermissionManager permissionManager;
    private NavigationHelper navHelper;
    private FeedbackPromptManager feedbackManager;
    private AppUpdateChecker appUpdateChecker;
    private static final String TAG = "MainActivity_Log";
    private BillingManager billingManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.Theme_WhatsDownloadPlus);
        super.onCreate(savedInstanceState);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.appBarMain.toolbar);

        permissionManager = new PermissionManager(this, binding);
        navHelper = new NavigationHelper(this, binding);
        feedbackManager = new FeedbackPromptManager(this);
        billingManager = new BillingManager(this, null);

        setupNavigation();
        setupToolbarActions();

        WhatsNewDialog.display(this, BuildConfig.VERSION_CODE);
        appUpdateChecker = new AppUpdateChecker(this, updateLauncher);
        appUpdateChecker.checkForUpdate();

        checkAndRequestNotificationPermission();
        fetchFirebaseToken();
        handleNotificationIntent(getIntent());

        Looper.myQueue().addIdleHandler(() -> {
            if (!isFinishing() && !isDestroyed()) {
                initSecondaryServices();
            }
            return false;
        });
    }

    private void setupNavigation() {
        NavHostFragment navHost = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment_content_main);

        if (navHost != null) {
            navController = navHost.getNavController();

            appBarConfiguration = new AppBarConfiguration.Builder(
                    R.id.nav_home, R.id.nav_gallery, R.id.nav_download, R.id.nav_privacy_policy
            ).setOpenableLayout(binding.drawerLayout).build();

            NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);

            // 🔥 FIX: Bottom Navigation Force Click Handler
            if (binding.appBarMain.bottomNavigation != null) {
                // Pehle default setup karein
                NavigationUI.setupWithNavController(binding.appBarMain.bottomNavigation, navController);

                // Ab Dashboard (Home) click ko force handle karein
                binding.appBarMain.bottomNavigation.setOnItemSelectedListener(item -> {
                    int id = item.getItemId();

                    if (id == R.id.nav_home) {
                        // Agar hum pehle se Dashboard par nahi hain, to wapas jayein aur stack clear karein
                        if (navController.getCurrentDestination() != null &&
                                navController.getCurrentDestination().getId() != R.id.nav_home) {

                            navController.navigate(R.id.nav_home, null, new NavOptions.Builder()
                                    .setPopUpTo(R.id.nav_home, true) // Purana stack clear
                                    .setLaunchSingleTop(true)       // Naya instance na banayein
                                    .build());
                        }
                        return true;
                    }

                    // Gallery aur baki options ke liye default behavior
                    return NavigationUI.onNavDestinationSelected(item, navController);
                });

                binding.appBarMain.bottomNavigation.setOnItemReselectedListener(item -> {
                    if (item.getItemId() == R.id.nav_home) {
                        navController.popBackStack(R.id.nav_home, false);
                    }
                });
            }

            NavigationUI.setupWithNavController(binding.navView, navController);
            binding.navView.setItemIconTintList(null);

            binding.drawerLayout.addDrawerListener(new DrawerLayout.SimpleDrawerListener() {
                @Override
                public void onDrawerStateChanged(int newState) {
                    if (newState != DrawerLayout.STATE_IDLE) {
                        binding.drawerLayout.setLayerType(View.LAYER_TYPE_HARDWARE, null);
                    } else {
                        binding.drawerLayout.setLayerType(View.LAYER_TYPE_NONE, null);
                    }
                }
            });

            if (navHelper != null) {
                navHelper.setupDrawer(navController);
            }
        }

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    binding.drawerLayout.closeDrawer(GravityCompat.START);
                } else if (navController != null) {
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

    // ... (Baki methods as it is rehne dein) ...
    private void fetchFirebaseToken() {
        FirebaseMessaging.getInstance().getToken().addOnCompleteListener(task -> {
            if (!task.isSuccessful()) return;
            Log.d("FCM_TOKEN", "Device Token: " + task.getResult());
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
            if ("ImagesAndVideo".equals(fragmentName)) {
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    if (navController != null) navController.navigate(R.id.nav_gallery);
                }, 500);
            }
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleNotificationIntent(intent);
    }

    private void setupToolbarActions() {
        View toolbar = binding.appBarMain.toolbar;
        LinearLayout iconContainer = toolbar.findViewById(R.id.icon_list_container);
        ImageView btnToggle = toolbar.findViewById(R.id.btn_toggle_menu);

        if (btnToggle != null && iconContainer != null) {
            btnToggle.setOnClickListener(v -> {
                v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                if (iconContainer.getVisibility() == View.GONE) {
                    iconContainer.setVisibility(View.VISIBLE);
                    btnToggle.animate().rotation(180).setDuration(300).start();
                } else {
                    iconContainer.setVisibility(View.GONE);
                    btnToggle.animate().rotation(0).setDuration(300).start();
                }
            });
        }

        toolbar.findViewById(R.id.custom_more_apps).setOnClickListener(v -> showOurAppsDialog());
        toolbar.findViewById(R.id.custom_whatsapp).setOnClickListener(v -> UiUtils.openWhatsApp(this));
        toolbar.findViewById(R.id.custom_notif_layout).setOnClickListener(v -> {
            if (permissionManager != null) permissionManager.handleNotificationButtonClick();
        });
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
            dialog.getWindow().setAttributes(lp);
        }
        dialog.findViewById(R.id.app_item_photo_enhancer).setOnClickListener(v -> launchPlayStore("com.mariaxcodexpert.easyclickcounter"));
        dialog.show();
    }

    private void launchPlayStore(String packageName) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + packageName)));
        } catch (Exception e) {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + packageName)));
        }
    }

    private void initSecondaryServices() {
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (!isFinishing() && !isDestroyed()) {
                android.content.SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
                AdManager.isPremiumUser = prefs.getBoolean("isPremium", false);
                if (!AdManager.isPremiumUser) {
                    ConsentFormManager.init(this);
                    ConsentFormManager.getInstance().requestConsentForm(() -> {
                        if (AdManager.canRequestAds()) AdManager.init(this);
                    });
                }
            }
        }, 1500);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (billingManager != null) billingManager.startConnection();
        if (permissionManager != null) permissionManager.updateNotificationDot();
    }

    private final ActivityResultLauncher<IntentSenderRequest> updateLauncher =
            registerForActivityResult(new ActivityResultContracts.StartIntentSenderForResult(),
                    result -> Log.d("AppUpdate", "Result: " + result.getResultCode()));

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