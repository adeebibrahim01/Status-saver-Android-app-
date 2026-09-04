package com.mariaxcodexpert.whatsdownloadplus.ui.Profile;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.DatePicker;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.mariaxcodexpert.whatsdownloadplus.MainActivity;
import com.mariaxcodexpert.whatsdownloadplus.R;
import com.mariaxcodexpert.whatsdownloadplus.data.local.Database.AppDatabase;
import com.mariaxcodexpert.whatsdownloadplus.data.local.ProfileEntity.ProfileEntity;
import java.text.DateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class ProfileSetupActivity extends AppCompatActivity {

    private static final String TAG = "ProfileSetupActivity";
    private FirebaseAuth mAuth;
    private GoogleSignInClient mGoogleSignInClient;
    private MaterialButton btnGoogleSignIn;
    private ProgressBar progressBar;

    private String selectedGender = "Male";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_setup);

        try {
            mAuth = FirebaseAuth.getInstance();
            btnGoogleSignIn = findViewById(R.id.btnGoogleSignIn);
            progressBar = findViewById(R.id.progressBar);

            GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(getString(R.string.default_web_client_id))
                    .requestEmail()
                    .build();
            mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

            ActivityResultLauncher<Intent> signInLauncher = registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        try {
                            if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                                Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                                GoogleSignInAccount account = task.getResult(ApiException.class);
                                if (account != null) {
                                    firebaseAuthWithGoogle(account.getIdToken());
                                } else {
                                    handleAuthError("Failed to retrieve Google account details. Please try again.");
                                }
                            } else {
                                resetUIState();
                                Log.w(TAG, "Google Sign-In cancelled or failed with result code: " + result.getResultCode());
                            }
                        } catch (ApiException e) {
                            Log.e(TAG, "Google Sign-In API Exception: " + e.getStatusCode(), e);
                            String friendlyMsg = getFriendlyGoogleError(e.getStatusCode());
                            handleAuthError(friendlyMsg);
                        } catch (Exception e) {
                            Log.e(TAG, "Unexpected error in sign-in result: " + e.getMessage(), e);
                            handleAuthError("An error occurred during sign-in. Please check your internet connection.");
                        }
                    }
            );

            if (btnGoogleSignIn != null) {
                btnGoogleSignIn.setOnClickListener(v -> {
                    try {
                        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
                        btnGoogleSignIn.setEnabled(false);
                        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
                        signInLauncher.launch(signInIntent);
                    } catch (Exception e) {
                        Log.e(TAG, "Error launching sign-in intent: " + e.getMessage(), e);
                        handleAuthError("Could not start Google Sign-In.");
                        resetUIState();
                    }
                });
            }
        } catch (Exception e) {
            Log.e(TAG, "Exception in onCreate: " + e.getMessage(), e);
            handleAuthError("App initialization error.");
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        try {
            AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
            mAuth.signInWithCredential(credential).addOnCompleteListener(this, task -> {
                try {
                    if (!task.isSuccessful() || mAuth.getCurrentUser() == null) {
                        String rawError = task.getException() != null ? task.getException().getMessage() : "Unknown exception";
                        Log.e(TAG, "Firebase Authentication Failed: " + rawError);
                        handleAuthError("Login Failed: " + rawError);
                        return;
                    }

                    FirebaseUser user = mAuth.getCurrentUser();
                    String uid = user.getUid();
                    GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);

                    String name = (account != null && account.getDisplayName() != null) ? account.getDisplayName() : (user.getDisplayName() != null ? user.getDisplayName() : "User");
                    String email = (account != null && account.getEmail() != null) ? account.getEmail() : (user.getEmail() != null ? user.getEmail() : "");
                    String photo = (account != null && account.getPhotoUrl() != null) ? account.getPhotoUrl().toString() : "";
                    String timestamp = DateFormat.getDateTimeInstance().format(new Date());

                    FirebaseDatabase database = FirebaseDatabase.getInstance("https://status-saver-92d48-default-rtdb.firebaseio.com/");
                    DatabaseReference profileRef = database.getReference("users").child(uid).child("profile").child("info");

                    profileRef.get().addOnCompleteListener(task1 -> {
                        try {
                            if (task1.isSuccessful()) {
                                DataSnapshot snapshot = task1.getResult();
                                if (snapshot != null && snapshot.exists() && snapshot.hasChild("gender") && snapshot.hasChild("dob")) {
                                    Log.e(TAG, "Existing profile found. Syncing to Room.");
                                    syncToRoom(uid, snapshot);
                                } else {
                                    Log.e(TAG, "Missing profile fields. Prompting dialog.");
                                    runOnUiThread(() -> {
                                        resetUIState();
                                        showUnifiedProfileDialog(uid, name, email, photo, timestamp);
                                    });
                                }
                            } else {
                                String dbError = task1.getException() != null ? task1.getException().getMessage() : "Unknown database error";
                                Log.e(TAG, "Database Fetch Failed: " + dbError);
                                handleAuthError("Database Error: " + dbError);
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error handling database snapshot: " + e.getMessage(), e);
                            handleAuthError("Processing Error: " + e.getMessage());
                        }
                    });
                } catch (Exception e) {
                    Log.e(TAG, "Exception in auth complete listener: " + e.getMessage(), e);
                    handleAuthError("Auth Error: " + e.getMessage());
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Exception in firebaseAuthWithGoogle: " + e.getMessage(), e);
            handleAuthError("Google Auth Error: " + e.getMessage());
        }
    }

    private void showUnifiedProfileDialog(String uid, String name, String email, String photo, String timestamp) {
        try {
            View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_profile_details, null);

            AlertDialog profileDialog = new MaterialAlertDialogBuilder(this)
                    .setView(dialogView)
                    .setCancelable(false)
                    .create();

            MaterialCardView cardMale = dialogView.findViewById(R.id.cardMale);
            MaterialCardView cardFemale = dialogView.findViewById(R.id.cardFemale);
            DatePicker datePicker = dialogView.findViewById(R.id.datePicker);
            MaterialButton btnConfirm = dialogView.findViewById(R.id.btnConfirm);

            selectedGender = "Male";

            cardMale.setOnClickListener(v -> {
                selectedGender = "Male";
                cardMale.setStrokeColor(getResources().getColor(android.R.color.holo_orange_dark));
                cardMale.setStrokeWidth((int) (1.5f * getResources().getDisplayMetrics().density));
                cardFemale.setStrokeColor(0xFF555555);
                cardFemale.setStrokeWidth((int) (1.0f * getResources().getDisplayMetrics().density));
            });

            cardFemale.setOnClickListener(v -> {
                selectedGender = "Female";
                cardFemale.setStrokeColor(getResources().getColor(android.R.color.holo_orange_dark));
                cardFemale.setStrokeWidth((int) (1.5f * getResources().getDisplayMetrics().density));
                cardMale.setStrokeColor(0xFF555555);
                cardMale.setStrokeWidth((int) (1.0f * getResources().getDisplayMetrics().density));
            });

            if (datePicker != null) {
                datePicker.setMaxDate(System.currentTimeMillis());
            }

            btnConfirm.setOnClickListener(v -> {
                try {
                    int day = datePicker.getDayOfMonth();
                    int month = datePicker.getMonth() + 1;
                    int year = datePicker.getYear();

                    String selectedDob = day + "/" + month + "/" + year;
                    profileDialog.dismiss();
                    if (progressBar != null) progressBar.setVisibility(View.VISIBLE);

                    uploadToCloudAndRoom(uid, name, email, photo, timestamp, selectedGender, selectedDob);
                } catch (Exception e) {
                    Log.e(TAG, "Error reading dialog inputs: " + e.getMessage(), e);
                    Toast.makeText(this, "Please select a valid date.", Toast.LENGTH_SHORT).show();
                }
            });

            profileDialog.show();
        } catch (Exception e) {
            Log.e(TAG, "Exception in showUnifiedProfileDialog: " + e.getMessage(), e);
            handleAuthError("Error loading profile dialog.");
        }
    }

    private void syncToRoom(String uid, DataSnapshot snapshot) {
        try {
            String name = String.valueOf(snapshot.child("name").getValue() != null ? snapshot.child("name").getValue(String.class) : "User");
            String email = String.valueOf(snapshot.child("email").getValue() != null ? snapshot.child("email").getValue(String.class) : "");
            String photo = String.valueOf(snapshot.child("photo").getValue() != null ? snapshot.child("photo").getValue(String.class) : "");
            String timestamp = String.valueOf(snapshot.child("timestamp").getValue() != null ? snapshot.child("timestamp").getValue(String.class) : "");
            String gender = String.valueOf(snapshot.child("gender").getValue() != null ? snapshot.child("gender").getValue(String.class) : "Not Specified");
            String dob = String.valueOf(snapshot.child("dob").getValue() != null ? snapshot.child("dob").getValue(String.class) : "N/A");

            AppDatabase.databaseWriteExecutor.execute(() -> {
                try {
                    ProfileEntity profile = new ProfileEntity(uid, name, email, photo, timestamp, gender, dob);
                    AppDatabase.getInstance(getApplicationContext()).profileDao().insertProfile(profile);
                    navigateToMain();
                } catch (Exception e) {
                    Log.e(TAG, "Room Sync Failed: " + e.getMessage(), e);
                    handleAuthError("Local Database Error: " + e.getMessage());
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Data Parsing Failed during sync: " + e.getMessage(), e);
            handleAuthError("Data Parsing Error: " + e.getMessage());
        }
    }

    private void uploadToCloudAndRoom(String uid, String name, String email, String photo, String time, String gender, String dob) {
        try {
            Map<String, Object> userData = new HashMap<>();
            userData.put("name", name);
            userData.put("email", email);
            userData.put("photo", photo);
            userData.put("timestamp", time);
            userData.put("gender", gender);
            userData.put("dob", dob);

            FirebaseDatabase database = FirebaseDatabase.getInstance("https://status-saver-92d48-default-rtdb.firebaseio.com/");
            DatabaseReference profileRef = database.getReference("users").child(uid).child("profile").child("info");

            profileRef.setValue(userData)
                    .addOnSuccessListener(aVoid -> {
                        AppDatabase.databaseWriteExecutor.execute(() -> {
                            try {
                                AppDatabase.getInstance(getApplicationContext()).profileDao().insertProfile(new ProfileEntity(uid, name, email, photo, time, gender, dob));
                                navigateToMain();
                            } catch (Exception e) {
                                Log.e(TAG, "Room Insert Failed after upload: " + e.getMessage(), e);
                                handleAuthError("Offline Database Error: " + e.getMessage());
                            }
                        });
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Cloud Upload Failed: " + e.getMessage(), e);
                        handleAuthError("Cloud Save Failed: " + e.getMessage());
                    });
        } catch (Exception e) {
            Log.e(TAG, "Exception in uploadToCloudAndRoom: " + e.getMessage(), e);
            handleAuthError("Profile Upload Error: " + e.getMessage());
        }
    }

    private void navigateToMain() {
        runOnUiThread(() -> {
            try {
                if (isFinishing() || isDestroyed()) return;
                getSharedPreferences("UserPrefs", MODE_PRIVATE).edit().putBoolean("is_profile_setup_done", true).apply();
                startActivity(new Intent(this, MainActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
                finish();
            } catch (Exception e) {
                Log.e(TAG, "Exception in navigateToMain: " + e.getMessage(), e);
            }
        });
    }

    private void handleAuthError(String message) {
        runOnUiThread(() -> {
            try {
                if (isFinishing() || isDestroyed()) return;
                resetUIState();
                Toast.makeText(this, message, Toast.LENGTH_LONG).show();
            } catch (Exception e) {
                Log.e(TAG, "Exception in handleAuthError: " + e.getMessage(), e);
            }
        });
    }

    private void resetUIState() {
        if (progressBar != null) progressBar.setVisibility(View.GONE);
        if (btnGoogleSignIn != null) btnGoogleSignIn.setEnabled(true);
    }

    private String getFriendlyGoogleError(int statusCode) {
        switch (statusCode) {
            case 7:
                return "Network connection error. Please check your internet.";
            case 12500:
                return "Google Sign-In was cancelled or is not configured properly.";
            case 10:
                return "Developer Console configuration error (SHA-1 fingerprint mismatch).";
            default:
                return "Google Sign-In failed (Error code: " + statusCode + ")";
        }
    }
}