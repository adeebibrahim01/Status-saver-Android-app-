package com.mariaxcodexpert.whatsdownloadplus.Helper;

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


    public void setupDrawer(NavController navController) {
        binding.navView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();

            binding.drawerLayout.closeDrawer(GravityCompat.START);

            binding.drawerLayout.postDelayed(() -> {

                if (id == R.id.nav_share_app) {
                    shareApp();
                }
                else if (id == R.id.nav_feedback) {
                    try {
                        String channelUrl = "https://whatsapp.com/channel/0029Vb8H3yQ7IUYTjwD9uL1M";
                        Intent intent = new Intent(Intent.ACTION_VIEW);
                        intent.setData(Uri.parse(channelUrl));
                        activity.startActivity(intent);

                    } catch (Exception e) {
                        openPlayStore();
                    }
                }
                else if (id == R.id.nav_rate_app) {
                    String packageName = activity.getPackageName();
                    try {
                        Intent rateIntent = new Intent(Intent.ACTION_VIEW,
                                Uri.parse("market://details?id=" + packageName + "&show_reviews=true"));
                        rateIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        activity.startActivity(rateIntent);
                    } catch (Exception e) {
                        activity.startActivity(new Intent(Intent.ACTION_VIEW,
                                Uri.parse("https://play.google.com/store/apps/details?id=" + packageName)));
                    }
                } else {
                    if (navController.getCurrentDestination() != null &&
                            navController.getCurrentDestination().getId() == id) return;
                    try {
                        if (id == R.id.nav_home) {
                            navController.popBackStack(R.id.nav_home, false);
                        }
                        else if (id == R.id.nav_gallery) {
                            Bundle bundle = new Bundle();
                            bundle.putBoolean("showVideos", true);
                            navController.navigate(id, bundle, getInstantOptions());
                        }
                        else {
                            navController.navigate(id, null, getInstantOptions());
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Navigation failed for ID: " + id, e);
                    }
                }
            }, 10);

            return true;
        });
    }

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
            activity.startActivity(new Intent(Intent.ACTION_VIEW,
                    Uri.parse("market://details?id=" + packageName)));
        } catch (Exception e) {
            activity.startActivity(new Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/apps/details?id=" + packageName)));
        }
    }
}