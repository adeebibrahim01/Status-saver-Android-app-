package com.mariaxcodexpert.whatsdownloadplus.ui.Profile;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.signature.ObjectKey;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.mariaxcodexpert.whatsdownloadplus.R;
import com.mariaxcodexpert.whatsdownloadplus.data.local.ProfileEntity.ProfileEntity;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ProfileFragment extends Fragment {

    private static final String TAG = "ProfileFragment";
    private ProfileViewModel profileViewModel;
    private TextView tvUsername, tvProfileText, tvDateValue, tvEmail;
    private ImageView imgProfile;
    private LinearLayout btnLogOut;

    public ProfileFragment() {
        super(R.layout.fragment_profile);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        try {
            profileViewModel = new ViewModelProvider(this).get(ProfileViewModel.class);

            tvUsername = view.findViewById(R.id.tvUsername);
            tvProfileText = view.findViewById(R.id.tvProfileText);
            imgProfile = view.findViewById(R.id.imgProfile);
            tvEmail = view.findViewById(R.id.tvEmail);
            tvDateValue = view.findViewById(R.id.tvDateValue);
            btnLogOut = view.findViewById(R.id.btnLogOut);

            if (btnLogOut != null) {
                btnLogOut.setOnClickListener(v -> performLogout());
            } else {
                Log.e(TAG, "btnLogOut view is null.");
            }

            showCurrentDateTime();

            if (FirebaseAuth.getInstance().getCurrentUser() != null && profileViewModel != null) {
                profileViewModel.getProfileLiveData().observe(getViewLifecycleOwner(), profile -> {
                    try {
                        if (profile != null) {
                            Log.e(TAG, "Profile data fetched successfully from Room DB.");
                            updateUI(profile);
                        } else {
                            Log.e(TAG, "Profile data from Room DB is null.");
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error updating UI from profile LiveData observer: " + e.getMessage(), e);
                    }
                });
            }
        } catch (Exception e) {
            Log.e(TAG, "Exception occurred in onViewCreated: " + e.getMessage(), e);
        }
    }

    private void performLogout() {
        try {
            Context context = getContext();
            if (context == null) {
                Log.e(TAG, "Context is null during performLogout.");
                return;
            }

            View dialogView = LayoutInflater.from(context).inflate(R.layout.layout_logout_dialog, null);
            if (dialogView == null) {
                Log.e(TAG, "Failed to inflate logout dialog view.");
                return;
            }

            androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(context)
                    .setView(dialogView)
                    .create();

            View btnCancel = dialogView.findViewById(R.id.btnCancel);
            if (btnCancel != null) {
                btnCancel.setOnClickListener(v -> dialog.dismiss());
            }

            View btnConfirmLogout = dialogView.findViewById(R.id.btnConfirmLogout);
            if (btnConfirmLogout != null) {
                btnConfirmLogout.setOnClickListener(v -> {
                    executeSignOut();
                    dialog.dismiss();
                });
            }

            dialog.show();
        } catch (Exception e) {
            Log.e(TAG, "Exception in performLogout: " + e.getMessage(), e);
        }
    }

    private void executeSignOut() {
        try {
            if (getContext() == null || getActivity() == null) {
                Log.e(TAG, "Context or Activity is null during executeSignOut.");
                return;
            }

            GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build();
            GoogleSignIn.getClient(requireActivity(), gso).signOut().addOnCompleteListener(task -> {
                try {
                    FirebaseAuth.getInstance().signOut();

                    requireActivity().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
                            .edit()
                            .putBoolean("is_profile_setup_done", false)
                            .apply();

                    Log.e(TAG, "User signed out successfully.");
                    Intent intent = new Intent(getActivity(), ProfileSetupActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    requireActivity().finish();
                } catch (Exception e) {
                    Log.e(TAG, "Exception inside Google sign out completion listener: " + e.getMessage(), e);
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Exception in executeSignOut: " + e.getMessage(), e);
        }
    }

    private void showCurrentDateTime() {
        try {
            if (tvDateValue != null) {
                SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm:ss a", Locale.getDefault());
                String currentTime = timeFormat.format(new Date());
                tvDateValue.setText(currentTime);
            }
        } catch (Exception e) {
            Log.e(TAG, "Exception in showCurrentDateTime: " + e.getMessage(), e);
        }
    }

    private void updateUI(ProfileEntity profile) {
        try {
            if (profile == null) {
                Log.e(TAG, "ProfileEntity is null in updateUI.");
                return;
            }

            if (tvUsername != null) {
                tvUsername.setText(profile.getName() != null ? profile.getName() : "");
            }

            if (tvEmail != null) {
                tvEmail.setText(profile.getEmail() != null ? profile.getEmail() : "N/A");
            }

            String currentPhoto = profile.getPhotoUrl() != null ? profile.getPhotoUrl().trim() : "";
            if (!currentPhoto.isEmpty()) {
                if (imgProfile != null) {
                    imgProfile.setVisibility(View.VISIBLE);
                }
                if (tvProfileText != null) {
                    tvProfileText.setVisibility(View.INVISIBLE);
                }

                if (isAdded() && getContext() != null) {
                    // Glide load ke waqt default blue avatar/glitch ko hatane ke liye placeholder aur error par ic_person ya transparent set kiya hai
                    Glide.with(this)
                            .load(currentPhoto)
                            .circleCrop()
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .signature(new ObjectKey(currentPhoto))
                            .placeholder(R.drawable.ic_person) // Loading ke waqt yeh show hoga
                            .error(R.drawable.ic_person)       // Agar error aaye toh yeh show hoga
                            .dontAnimate()
                            .into(imgProfile);
                }
            } else {
                if (imgProfile != null) {
                    imgProfile.setVisibility(View.INVISIBLE);
                }
                if (tvProfileText != null) {
                    tvProfileText.setVisibility(View.VISIBLE);
                    String name = profile.getName();
                    if (name != null && !name.trim().isEmpty()) {
                        tvProfileText.setText(String.valueOf(name.trim().charAt(0)).toUpperCase());
                    } else {
                        tvProfileText.setText("U");
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Exception occurred in updateUI: " + e.getMessage(), e);
        }
    }
}