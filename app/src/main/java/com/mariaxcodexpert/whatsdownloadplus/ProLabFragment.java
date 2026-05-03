package com.mariaxcodexpert.whatsdownloadplus;

import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;



public class ProLabFragment extends Fragment {

    private Uri sourceUri;
    private Bitmap processedBitmap;
    private final int[] currentHDMode = {0};
    private final int[] currentProStyle = {0};

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_pro_lab, container, false);
    }

}