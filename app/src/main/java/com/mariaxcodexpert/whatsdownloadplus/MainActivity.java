package com.mariaxcodexpert.whatsdownloadplus;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.core.view.WindowCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.NavOptions;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.mariaxcodexpert.whatsdownloadplus.Ads.AdManager;
import com.mariaxcodexpert.whatsdownloadplus.Ads.ConsentFormManager;
import com.mariaxcodexpert.whatsdownloadplus.Helper.AppUpdateChecker;
import com.mariaxcodexpert.whatsdownloadplus.Helper.WhatsNewDialog;
import com.mariaxcodexpert.whatsdownloadplus.databinding.ActivityMainBinding;
import com.mariaxcodexpert.whatsdownloadplus.ui.Profile.ProfileSetupActivity;
import com.mariaxcodexpert.whatsdownloadplus.ui.Profile.ProfileViewModel;
import com.mariaxcodexpert.whatsdownloadplus.ui.Subscription.BillingManager;
import com.mariaxcodexpert.whatsdownloadplus.ui.language.LanguageManager;
import com.mariaxcodexpert.whatsdownloadplus.Helper.NavigationHelper;
import com.mariaxcodexpert.whatsdownloadplus.Helper.PermissionManager;
import com.mariaxcodexpert.whatsdownloadplus.Helper.UiUtils;

public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding binding;
    private NavController navController;
    private AppBarConfiguration appBarConfiguration;
    private PermissionManager permissionManager;
    private NavigationHelper navHelper;
    private BillingManager billingManager;

    public static String cachedPhoto = "";
    private ProfileViewModel profileViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.Theme_WhatsDownloadPlus);
        LanguageManager.initAppLanguage(this);
        super.onCreate(savedInstanceState);

        //-----------------------------------------------------------------
        new Thread(() -> {
            boolean isProfileSetupDone = getSharedPreferences("UserPrefs", MODE_PRIVATE)
                    .getBoolean("is_profile_setup_done", false);

            com.mariaxcodexpert.whatsdownloadplus.data.local.Database.AppDatabase db =
                    com.mariaxcodexpert.whatsdownloadplus.data.local.Database.AppDatabase.getInstance(this);

            boolean userExistsInDb = db.profileDao().isUserExists();

            if (!isProfileSetupDone || !userExistsInDb) {
                runOnUiThread(() -> {
                    Intent intent = new Intent(MainActivity.this, ProfileSetupActivity.class);
                    startActivity(intent);
                    finish();
                });
            }
        }).start();

//----------------------------------------------------------------------------



//----------------------------------------------------------------------------
        //        new Handler(Looper.getMainLooper()).postDelayed(() -> {
//            if (!isFinishing()) {
//                WhatsNewDialog.display(this, BuildConfig.VERSION_CODE);
//                new AppUpdateChecker(this, updateLauncher).checkForUpdate();
//                handleNotificationIntent(getIntent());
//            }
//        }, 10000);
        
// ----------------------------------------------------------------------------
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.appBarMain.toolbar);


        permissionManager = new PermissionManager(this, binding);
        navHelper = new NavigationHelper(this, binding);
        billingManager = new BillingManager(this, null);

        if (!AdManager.isPremiumUser) {
            com.google.android.gms.ads.MobileAds.initialize(this, initializationStatus -> {});

            ConsentFormManager.init(this);
            ConsentFormManager.getInstance().requestConsentForm(() -> {
                com.unity3d.ads.metadata.MetaData gdprMetaData = new com.unity3d.ads.metadata.MetaData(this);
                gdprMetaData.set("gdpr.consent", true);
                gdprMetaData.commit();

                if (ConsentFormManager.getInstance().canRequestAds()) {
                    AdManager.init(this);
                } else {
                    Log.d("AdFlow", "Consent not granted or restricted.");
                }
            });
        } else {
            AdManager.releaseAllAds();
        }

        profileViewModel = new ViewModelProvider(this).get(ProfileViewModel.class);
        setupNavigation();
        setupToolbarActions();


    }

    private void setupNavigation() {
        NavHostFragment navHost = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment_content_main);

        if (navHost != null) {
            navController = navHost.getNavController();


            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayShowTitleEnabled(false);
            }

            appBarConfiguration = new AppBarConfiguration.Builder(
                    R.id.nav_home, R.id.nav_gallery, R.id.nav_download, R.id.nav_privacy_policy
            ).setOpenableLayout(binding.drawerLayout).build();

            NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);

            NavigationUI.setupWithNavController(binding.appBarMain.bottomNavigation, navController);
            binding.appBarMain.bottomNavigation.setOnItemSelectedListener(item -> {
                int id = item.getItemId();
                if (id == R.id.nav_home) {
                    if (navController.getCurrentDestination() != null && navController.getCurrentDestination().getId() != R.id.nav_home) {
                        navController.navigate(R.id.nav_home, null, new NavOptions.Builder()
                                .setPopUpTo(R.id.nav_home, true)
                                .setLaunchSingleTop(true).build());
                    }
                    return true;
                }
                return NavigationUI.onNavDestinationSelected(item, navController);
            });

            binding.navView.setNavigationItemSelectedListener(item -> {
                int selectedId = item.getItemId();
                int currentId = (navController.getCurrentDestination() != null) ? navController.getCurrentDestination().getId() : -1;
                if (selectedId == currentId) {
                    binding.drawerLayout.closeDrawer(GravityCompat.START);
                    return true;
                }
                binding.drawerLayout.closeDrawer(GravityCompat.START);
                binding.drawerLayout.addDrawerListener(new DrawerLayout.SimpleDrawerListener() {
                    @Override
                    public void onDrawerClosed(View drawerView) {
                        super.onDrawerClosed(drawerView);
                        NavigationUI.onNavDestinationSelected(item, navController);
                        binding.drawerLayout.removeDrawerListener(this);
                    }
                });
                return true;
            });

            binding.navView.setItemIconTintList(null);
            binding.drawerLayout.addDrawerListener(new DrawerLayout.SimpleDrawerListener() {
                @Override
                public void onDrawerStateChanged(int newState) {
                    binding.drawerLayout.setLayerType(newState != DrawerLayout.STATE_IDLE ? View.LAYER_TYPE_HARDWARE : View.LAYER_TYPE_NONE, null);
                }
            });

            if (navHelper != null) navHelper.setupDrawer(navController);

            refreshProfileUI(profileViewModel);
            navController.addOnDestinationChangedListener((c, d, a) -> refreshProfileUI(profileViewModel));
        }


        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    binding.drawerLayout.closeDrawer(GravityCompat.START);
                } else if (navController != null && navController.getCurrentDestination() != null && navController.getCurrentDestination().getId() != R.id.nav_home) {
                    navController.popBackStack(R.id.nav_home, false);
                } else {
                    finish();
                }
            }
        });
    }
    private void refreshProfileUI(ProfileViewModel profileViewModel) {
        TextView tvProfileInitial = binding.appBarMain.toolbar.findViewById(R.id.toolbar_profile_text);
        ImageView ivProfile = binding.appBarMain.toolbar.findViewById(R.id.toolbar_profile_image);

        profileViewModel.getProfileLiveData().observe(this, profile -> {
            if (profile == null) {
                showFallbackInitial(tvProfileInitial, ivProfile, "U");
                return;
            }

            String photoUrl = profile.getPhotoUrl();

            if (photoUrl != null && !photoUrl.trim().isEmpty()) {
                ivProfile.setVisibility(View.VISIBLE);
                tvProfileInitial.setVisibility(View.GONE);

                com.bumptech.glide.Glide.with(this)
                        .load(photoUrl)
                        .circleCrop()
                        .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.ALL)
                        .signature(new com.bumptech.glide.signature.ObjectKey(photoUrl))
                        .placeholder(ivProfile.getDrawable())
                        .dontAnimate()
                        .into(ivProfile);
            } else {
                showFallbackInitial(tvProfileInitial, ivProfile, profile.getName());
            }
        });
    }

    private void showFallbackInitial(TextView tvInitial, ImageView ivProfile, String name) {
        ivProfile.setVisibility(View.GONE);
        tvInitial.setVisibility(View.VISIBLE);

        String initial = (name != null && !name.trim().isEmpty())
                ? String.valueOf(name.trim().charAt(0)).toUpperCase() : "U";

        tvInitial.setText(initial);
    }

    private void handleNotificationIntent(Intent intent) {
        if (intent != null && intent.hasExtra("openFragment")) {
            if ("ImagesAndVideo".equals(intent.getStringExtra("openFragment"))) {
                if (navController != null) navController.navigate(R.id.nav_gallery);
            }
        }
    }

    private void setupToolbarActions() {
        View toolbar = binding.appBarMain.toolbar;
        LinearLayout iconContainer = toolbar.findViewById(R.id.icon_list_container);
        ImageView btnToggle = toolbar.findViewById(R.id.btn_toggle_menu);

        // FrameLayout ko find karein jo pura touch area hai
        View btnNotificationLayout = toolbar.findViewById(R.id.custom_notif_layout);

        if (btnToggle != null && iconContainer != null) {
            btnToggle.setOnClickListener(v -> {
                v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                boolean isVisible = iconContainer.getVisibility() == View.VISIBLE;
                iconContainer.setVisibility(isVisible ? View.GONE : View.VISIBLE);
                btnToggle.animate().rotation(isVisible ? 0 : 180).setDuration(300).start();
            });
        }

        // Notification container click listener with permission check
        if (btnNotificationLayout != null) {
            btnNotificationLayout.setOnClickListener(v -> {
                v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                Log.e("MainActivity", "Notification bell layout clicked.");

                if (permissionManager != null) {
                    // Pehle check karein ke notification permission di hai ya nahi
                    if (!permissionManager.isNotificationPermissionGranted(this)) {
                        Log.e("MainActivity", "Notification permission not granted. Prompting user.");
                        permissionManager.checkAndShowNotificationPrompt(this);
                    }
                    else if (permissionManager.isBatteryOptimized()) {
                        // Agar notification permission di hui hai, toh battery optimization dialog show karein
                        Log.e("MainActivity", "Notification granted, showing battery optimization dialog.");
                        permissionManager.showBatteryOptimizationDialog();
                    } else {
                        Log.e("MainActivity", "Both notification and battery optimization requirements are met.");
                    }

                    // Dot ki state ko foran update karein
                    permissionManager.updateNotificationDot();
                }
            });
        }

        View customWhatsApp = toolbar.findViewById(R.id.custom_whatsapp);
        if (customWhatsApp != null) {
            customWhatsApp.setOnClickListener(v -> UiUtils.openWhatsApp(this));
        }

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (!isFinishing() && permissionManager != null) {
                if (!permissionManager.isNotificationPermissionGranted(this)) {
                    permissionManager.checkAndShowNotificationPrompt(this);
                }
            }
        }, 20000);
    }
    @Override
    protected void onResume() {
        super.onResume();
        if (billingManager != null) billingManager.startConnection();

        if (permissionManager != null) {
            permissionManager.updateNotificationDot();
        }
        if (profileViewModel != null) {
            refreshProfileUI(profileViewModel); 
        }
    }

    private final ActivityResultLauncher<IntentSenderRequest> updateLauncher =
            registerForActivityResult(new ActivityResultContracts.StartIntentSenderForResult(), result -> {});

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