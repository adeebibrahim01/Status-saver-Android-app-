package com.mariaxcodexpert.whatsdownloadplus.ui.Profile;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.mariaxcodexpert.whatsdownloadplus.R;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Calendar;

public class ProfileDetailsActivity extends AppCompatActivity {

    private TextInputEditText etFullName, etDOB, etCountry;
    private AutoCompleteTextView etGender;
    private MaterialButton btnSave;
    private ProgressBar progressBar;
    private View mainContentLayout;
    private ImageView imgProfile, imgAddIcon;

    private String email, photoBase64 = "";
    private Uri selectedImageUri;

    private final androidx.activity.result.ActivityResultLauncher<Intent> imagePickerLauncher = registerForActivityResult(
            new androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    selectedImageUri = result.getData().getData();
                    processAndSetImage(selectedImageUri);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_details);

        initViews();
        setupAutoFill();
    }

    private void initViews() {
        mainContentLayout = findViewById(R.id.mainContentLayout);
        etFullName = findViewById(R.id.etFullName);
        etDOB = findViewById(R.id.etDOB);
        etCountry = findViewById(R.id.etCountry);
        etGender = findViewById(R.id.etGender);
        btnSave = findViewById(R.id.btnSave);
        progressBar = findViewById(R.id.progressBarDetails);
        imgProfile = findViewById(R.id.imgProfile);
        imgAddIcon = findViewById(R.id.imgAddIcon);

        etFullName.setText(getIntent().getStringExtra("USER_NAME"));
        email = getIntent().getStringExtra("USER_EMAIL");

        // Google Photo Auto-fetch
        String photoUrl = getIntent().getStringExtra("USER_PHOTO");
        if (photoUrl != null && !photoUrl.isEmpty()) {
            fetchGooglePhoto(photoUrl);
        }

        View.OnClickListener pickListener = v -> imagePickerLauncher.launch(
                new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI));
        imgProfile.setOnClickListener(pickListener);
        imgAddIcon.setOnClickListener(pickListener);

        String[] genders = {"Male", "Female", "Other"};
        etGender.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, genders));

        etDOB.setOnClickListener(v -> {
            Calendar cal = Calendar.getInstance();
            new DatePickerDialog(this, (view, year, month, day) ->
                    etDOB.setText(day + "/" + (month + 1) + "/" + year),
                    cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
        });

       // btnSave.setOnClickListener(v -> validateAndSave());
        checkExistingUser();
    }

    private void fetchGooglePhoto(String url) {
        Glide.with(this).asBitmap().load(url).circleCrop().into(new CustomTarget<Bitmap>() {
            @Override
            public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                imgProfile.setImageBitmap(resource);
                photoBase64 = encodeBitmap(resource);
            }
            @Override
            public void onLoadCleared(@Nullable Drawable placeholder) {}
        });
    }

    private void processAndSetImage(Uri uri) {
        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
            Glide.with(this).load(bitmap).circleCrop().into(imgProfile);
            photoBase64 = encodeBitmap(bitmap);
        } catch (IOException e) { e.printStackTrace(); }
    }

    private String encodeBitmap(Bitmap bitmap) {
        Bitmap resized = Bitmap.createScaledBitmap(bitmap, 400, 400, true);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        resized.compress(Bitmap.CompressFormat.JPEG, 70, baos);
        return Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT);
    }

    private void checkExistingUser() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) return;
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseFirestore.getInstance().collection("users").document(uid)
                .collection("profile").document("info").get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) syncFirestoreToRoom(uid, documentSnapshot);
                    else {
                        progressBar.setVisibility(View.GONE);
                        mainContentLayout.setVisibility(View.VISIBLE);
                    }
                }).addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    mainContentLayout.setVisibility(View.VISIBLE);
                });
    }

//    private void validateAndSave() {
//        String name = etFullName.getText().toString().trim();
//        String dob = etDOB.getText().toString().trim();
//        String gender = etGender.getText().toString().trim();
//        String country = etCountry.getText().toString().trim();
//
//        if (name.isEmpty() || dob.isEmpty() || gender.isEmpty() || country.isEmpty()) {
//            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
//            return;
//        }
//        saveToFirestore(name, dob, gender, country);
//    }

//    private void saveToFirestore(String name, String dob, String gender, String country) {
//        progressBar.setVisibility(View.VISIBLE);
//        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
//        Map<String, Object> user = new HashMap<>();
//        user.put("name", name); user.put("dob", dob); user.put("gender", gender);
//        user.put("country", country); user.put("email", email); user.put("photo", photoBase64);
//
//        FirebaseFirestore.getInstance().collection("users").document(uid)
//                .collection("profile").document("info").set(user)
//                .addOnSuccessListener(aVoid -> saveToRoom(uid, name, dob, gender, country))
//                .addOnFailureListener(e -> {
//                    progressBar.setVisibility(View.GONE);
//                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
//                });
//    }

//    private void saveToRoom(String uid, String name, String dob, String gender, String country) {
//        AppDatabase.databaseWriteExecutor.execute(() -> {
//            ProfileEntity profile = new ProfileEntity(uid, name, dob, gender, country, email, photoBase64);
//            AppDatabase.getInstance(this).profileDao().insertProfile(profile);
//            runOnUiThread(() -> {
//                getSharedPreferences("UserPrefs", MODE_PRIVATE).edit().putBoolean("is_profile_setup_done", true).apply();
//                startActivity(new Intent(this, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
//                finish();
//            });
//        });
//    }

    private void syncFirestoreToRoom(String uid, DocumentSnapshot doc) { /* As per previous code */ }
    private void setupAutoFill() { /* Placeholder */ }
}