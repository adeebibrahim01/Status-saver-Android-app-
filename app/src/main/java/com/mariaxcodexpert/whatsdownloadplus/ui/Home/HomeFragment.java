package com.mariaxcodexpert.whatsdownloadplus.ui.Home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.mariaxcodexpert.whatsdownloadplus.R;
import com.mariaxcodexpert.whatsdownloadplus.databinding.FragmentHomeBinding;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private NavController navController;
    private HomeViewModel viewModel;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentHomeBinding.inflate(inflater, container, false);
        navController = NavHostFragment.findNavController(this);

        // Initialize ViewModel
        viewModel = new ViewModelProvider(this,
                ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().getApplication()))
                .get(HomeViewModel.class);

        setupClickListeners();
        observeViewModel();

        requireActivity().setTitle("Home");
        return binding.getRoot();
    }

    private void observeViewModel() {
        viewModel.getJoinedDate().observe(getViewLifecycleOwner(), joinedText -> {
            binding.joinedText.setText(joinedText);
        });

        viewModel.getToolbarTitle().observe(getViewLifecycleOwner(), title -> {
            requireActivity().setTitle(title);
        });
    }

    private void setupClickListeners() {
        binding.cardImages.setOnClickListener(v -> openGallery(false));  // false = Images
        binding.cardVideos.setOnClickListener(v -> openGallery(true));  // true = Videos
        binding.cardSaved.setOnClickListener(v -> navigateToDownload());
    }


    private void openGallery(boolean showVideos) {
        Bundle args = new Bundle();
        args.putBoolean("showVideos", showVideos);

        try {
            navController.navigate(R.id.nav_gallery, args);
            viewModel.setToolbarTitle(showVideos ? "Videos" : "Images");
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(requireContext(), "Navigation error", Toast.LENGTH_SHORT).show();
        }
    }

    private void navigateToDownload() {
        try {
            navController.navigate(R.id.nav_download);
            viewModel.setToolbarTitle("Download");
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(requireContext(), "Navigation error: Download", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
