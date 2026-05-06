package com.mariaxcodexpert.whatsdownloadplus;

import android.util.Log;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.mariaxcodexpert.whatsdownloadplus.ui.Home.HomeFragment;
import com.mariaxcodexpert.whatsdownloadplus.ui.gallery.GalleryFragment;
import com.mariaxcodexpert.whatsdownloadplus.ui.trending.TrendingFragment;

public class NavigationManager {

    private static final String TAG = "NavigationManager";

    public static void setupBottomNavigation(
            BottomNavigationView navView,
            FragmentManager fragmentManager,
            int containerId) {

        // 1. Initial Fragment Load (Sirf tab karein jab container khali ho)
        if (fragmentManager.findFragmentById(containerId) == null) {
            replaceFragment(fragmentManager, new HomeFragment(), containerId, "HOME");
        }

        // 2. Click Listener setup
        navView.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            String tag = "";
            int id = item.getItemId();

            if (id == R.id.nav_home) {
                selectedFragment = new HomeFragment();
                tag = "HOME";
            } else if (id == R.id.nav_gallery) {
                selectedFragment = new GalleryFragment();
                tag = "GALLERY";
            } else if (id == R.id.nav_trending) {
                selectedFragment = new TrendingFragment();
                tag = "TRENDING";
            }

            // Optimization: Check karein ke kahin wahi fragment pehle se toh load nahi?
            Fragment currentFragment = fragmentManager.findFragmentById(containerId);
            if (selectedFragment != null && (currentFragment == null || !currentFragment.getClass().equals(selectedFragment.getClass()))) {
                replaceFragment(fragmentManager, selectedFragment, containerId, tag);
                return true;
            }

            return true; // Click handle ho gaya (true return lazmi hai)
        });
    }

    private static void replaceFragment(FragmentManager fragmentManager, Fragment fragment, int containerId, String tag) {
        try {
            fragmentManager.beginTransaction()
                    .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                    .replace(containerId, fragment, tag)
                    .commitAllowingStateLoss(); // State loss se crash rukta hai
        } catch (Exception e) {
            Log.e(TAG, "Fragment Transaction Failed: " + e.getMessage());
        }
    }
}