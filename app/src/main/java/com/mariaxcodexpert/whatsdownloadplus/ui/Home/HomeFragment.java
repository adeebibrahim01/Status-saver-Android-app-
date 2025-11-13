package com.mariaxcodexpert.whatsdownloadplus.ui.Home;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.mariaxcodexpert.whatsdownloadplus.R;
import com.mariaxcodexpert.whatsdownloadplus.databinding.FragmentHomeBinding;

import java.util.Date;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private NavController navController;



    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentHomeBinding.inflate(inflater, container, false);
        navController = NavHostFragment.findNavController(this);

        setupClickListeners();
        setJoinedText();

        requireActivity().setTitle("Home");
        return binding.getRoot();
    }


    private void setupClickListeners() {
        // =====================
        // Card Click Listeners
        // =====================
        binding.cardImages.setOnClickListener(v -> openGallery(false)); // false = Images
        binding.cardVideos.setOnClickListener(v -> openGallery(true)); // true = Videos

        binding.cardKeyTracker.setOnClickListener(v -> {
            try {
                navController.navigate(R.id.nav_key_tracker); // fragment inside container
                requireActivity().setTitle("Key Tracker");
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(requireContext(), "Navigation error: Key Tracker", Toast.LENGTH_SHORT).show();
            }
        });


        binding.cardSaved.setOnClickListener(v -> {
            try {
                navController.navigate(R.id.nav_download);
                requireActivity().setTitle("Download");
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(requireContext(), "Navigation error: Download", Toast.LENGTH_SHORT).show();
            }
        });


        binding.cardNotifications.setOnClickListener(v -> {
            try {
                navController.navigate(R.id.nav_notifications);
                requireActivity().setTitle("Notifications");
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(requireContext(), "Navigation error: Notifications", Toast.LENGTH_SHORT).show();
            }
        });

        binding.cardComingSoon.setOnClickListener(v ->
                Toast.makeText(requireContext(), "Feature coming soon!", Toast.LENGTH_SHORT).show());
    }



    private void setJoinedText() {
        try {
            PackageInfo packageInfo = requireActivity()
                    .getPackageManager()
                    .getPackageInfo(requireActivity().getPackageName(), 0);

            long firstInstallTime = packageInfo.firstInstallTime;
            String formattedDate = DateFormat.format("MMMM dd, yyyy", new Date(firstInstallTime)).toString();
            binding.joinedText.setText("Joined Status Downloader Plus on " + formattedDate + ".");
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            binding.joinedText.setText("Joined Status Downloader Plus");
        }
    }

    /**
     * Navigate to GalleryFragment with a boolean argument
     * @param showVideos true = open Videos tab, false = open Images tab
     */
    private void openGallery(boolean showVideos) {
        Bundle args = new Bundle();
        args.putBoolean("showVideos", showVideos); // Pass the tab info
        try {
            navController.navigate(R.id.nav_gallery, args);
            requireActivity().setTitle(showVideos ? "Videos" : "Images");
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(requireContext(), "Navigation error", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
