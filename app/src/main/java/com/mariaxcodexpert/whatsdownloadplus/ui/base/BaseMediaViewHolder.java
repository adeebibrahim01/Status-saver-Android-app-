package com.mariaxcodexpert.whatsdownloadplus.ui.base;

import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.mariaxcodexpert.whatsdownloadplus.R;

public class BaseMediaViewHolder extends RecyclerView.ViewHolder {
    public final ImageView imageThumb, videoIcon, downloadStatus, downloadIcon;
    public final ProgressBar downloadProgress;
    public final TextView countdownTimer;
    public final View deleteIcon;

    public BaseMediaViewHolder(@NonNull View v) {
        super(v);
        imageThumb = v.findViewById(R.id.imageThumb);
        videoIcon = v.findViewById(R.id.videoIcon);
        downloadStatus = v.findViewById(R.id.downloadStatus);
        downloadIcon = v.findViewById(R.id.downloadIcon); // Sirf Gallery mein ho sakta hai
        downloadProgress = v.findViewById(R.id.downloadProgress);
        countdownTimer = v.findViewById(R.id.countdownTimer);
        deleteIcon = v.findViewById(R.id.deleteIcon); // Sirf Downloads mein ho sakta hai

        // Performance Optimization
        if (imageThumb != null) {
            imageThumb.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        }
    }
}