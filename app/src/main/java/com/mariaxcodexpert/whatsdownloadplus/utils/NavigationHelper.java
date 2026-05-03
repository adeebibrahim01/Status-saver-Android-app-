package com.mariaxcodexpert.whatsdownloadplus.utils;

import static androidx.core.content.ContextCompat.startActivity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import androidx.core.view.GravityCompat;
import androidx.navigation.NavController;
import androidx.navigation.NavOptions;

import com.mariaxcodexpert.whatsdownloadplus.MainActivity;
import com.mariaxcodexpert.whatsdownloadplus.R;
import com.mariaxcodexpert.whatsdownloadplus.databinding.ActivityMainBinding;

public class NavigationHelper {
    private final MainActivity activity;
    private final ActivityMainBinding binding;
    private static final String TAG = "NavigationHelper";

    public NavigationHelper(MainActivity activity, ActivityMainBinding binding) {
        this.activity = activity;
        this.binding = binding;
    }

    /**
     * 🔥 Drawer Menu setup with 100x speed logic
     */
    public void setupDrawer(NavController navController) {
        binding.navView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();

            // 1. Drawer ko foran band karein taake user ko wait na karna pare
            binding.drawerLayout.closeDrawer(GravityCompat.START);

            // 2. 🔥 ZERO DELAY EXECUTION: UI thread ko block kiye bina kaam karein
            binding.drawerLayout.postDelayed(() -> {

                // --- CATEGORY 1: External Actions (Direct Intents) ---
                if (id == R.id.nav_share_app) {
                    shareApp();
                }
                else if (id == R.id.nav_feedback) {
                    // User ko aapka WhatsApp Channel join karwane ke liye
                    try {
                        // 🔥 Aapka updated channel link
                        String channelUrl = "https://whatsapp.com/channel/0029Vb8H3yQ7IUYTjwD9uL1M";
                        Intent intent = new Intent(Intent.ACTION_VIEW);
                        intent.setData(Uri.parse(channelUrl));

                        // Fix: context use karein startActivity call karne ke liye
                        activity.startActivity(intent);

                    } catch (Exception e) {
                        // Agar browser ya WhatsApp mein masla ho, toh fallback ke taur par Play Store khol dein
                        openPlayStore();
                    }
                }
                else if (id == R.id.nav_rate_app) {
                    String packageName = activity.getPackageName();
                    try {
                        // Ye link user ko seedha "Write a Review" section par le kar jaye ga
                        Intent rateIntent = new Intent(Intent.ACTION_VIEW,
                                Uri.parse("market://details?id=" + packageName + "&show_reviews=true"));
                        rateIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        activity.startActivity(rateIntent);
                    } catch (Exception e) {
                        // Fallback: Agar Play Store app mein masla ho toh browser open hoga
                        activity.startActivity(new Intent(Intent.ACTION_VIEW,
                                Uri.parse("https://play.google.com/store/apps/details?id=" + packageName)));
                    }
                }

                // --- CATEGORY 2: Fragment Navigation ---
                else {
                    // Check karein ke user pehle se usi fragment par to nahi hai
                    if (navController.getCurrentDestination() != null &&
                            navController.getCurrentDestination().getId() == id) return;

                    try {
                        if (id == R.id.nav_home) {
                            // Home par jane ke liye stack clear karein (Memory clean rehti hai)
                            navController.popBackStack(R.id.nav_home, false);
                        }
                        else if (id == R.id.nav_gallery) {
                            // Gallery ke liye bundle bhej rahe hain videos enable karne ke liye
                            Bundle bundle = new Bundle();
                            bundle.putBoolean("showVideos", true);
                            navController.navigate(id, bundle, getInstantOptions());
                        }
                        else {
                            // Download aur Privacy Policy ke liye direct navigation
                            navController.navigate(id, null, getInstantOptions());
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Navigation failed for ID: " + id, e);
                    }
                }
            }, 10); // 10ms delay is perfect for smooth drawer closing

            return true;
        });
    }

    /**
     * 🔥 100x FAST OPTIONS: No animations means instant switching
     */
    public NavOptions getInstantOptions() {
        return new NavOptions.Builder()
                .setEnterAnim(0)
                .setExitAnim(0)
                .setPopEnterAnim(0)
                .setPopExitAnim(0)
                .setLaunchSingleTop(true)
                .setRestoreState(true)
                .build();
    }

    // --- UTILITY METHODS (No Functionality Removed) ---

    private void shareApp() {
        try {
            String packageName = activity.getPackageName();
            String shareMessage = "Download Status Saver for WhatsApp & Video Downloader:\n" +
                    "https://play.google.com/store/apps/details?id=" + packageName;

            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_TEXT, shareMessage);
            activity.startActivity(Intent.createChooser(intent, "Share via"));
        } catch (Exception e) {
            Log.e(TAG, "Sharing failed", e);
        }
    }

    private void openPlayStore() {
        String packageName = activity.getPackageName();
        try {
            // Pehle Market app kholne ki koshish karein
            activity.startActivity(new Intent(Intent.ACTION_VIEW,
                    Uri.parse("market://details?id=" + packageName)));
        } catch (Exception e) {
            // Agar Play Store app nahi hai to browser khol dein
            activity.startActivity(new Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/apps/details?id=" + packageName)));
        }
    }
}