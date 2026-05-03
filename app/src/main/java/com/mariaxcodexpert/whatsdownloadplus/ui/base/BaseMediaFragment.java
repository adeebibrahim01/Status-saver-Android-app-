package com.mariaxcodexpert.whatsdownloadplus.ui.base;

import android.os.Bundle;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SimpleItemAnimator;

import com.mariaxcodexpert.whatsdownloadplus.R;

/**
 * Base Fragment jo Download aur Status Fragments ke common logic ko handle karta hai.
 */
public abstract class BaseMediaFragment extends Fragment {

    protected RecyclerView activeRecyclerView;
    protected ProgressBar commonProgressBar;

    // 🔥 FIX: TextView ko View kar diya taake ClassCastException na aaye
    // Kyunki ab tvEmptyMessage ek layout (include) ho sakta hai.
    protected View commonEmptyMessage;
    protected View commonEmptyLayout;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Common IDs
        commonProgressBar = view.findViewById(R.id.firstLoadProgress);
        if (commonProgressBar == null) commonProgressBar = view.findViewById(R.id.progressBar);

        // 🔥 Yahan ab crash nahi hoga kyunki View har tarah ke widget/layout ko handle kar leta hai
        commonEmptyMessage = view.findViewById(R.id.tvEmptyMessage);
        commonEmptyLayout = view.findViewById(R.id.emptyStateLayout);
    }

    /**
     * Standard RecyclerView setup logic
     */
    protected void setupMediaRecyclerView(RecyclerView rv, RecyclerView.Adapter<?> adapter, int spanCount) {
        this.activeRecyclerView = rv;
        GridLayoutManager layoutManager = new GridLayoutManager(getContext(), spanCount);
        rv.setLayoutManager(layoutManager);
        rv.setHasFixedSize(true);
        rv.setItemViewCacheSize(20);

        if (rv.getItemAnimator() instanceof SimpleItemAnimator) {
            ((SimpleItemAnimator) rv.getItemAnimator()).setSupportsChangeAnimations(false);
        }

        rv.setAdapter(adapter);
    }

    /**
     * UI State manage karne ke liye common function.
     */
    protected void updateUIState(boolean isLoading, int itemCount, String emptyMsg) {
        if (isLoading) {
            if (itemCount == 0 && commonProgressBar != null) commonProgressBar.setVisibility(View.VISIBLE);
            if (commonEmptyLayout != null) commonEmptyLayout.setVisibility(View.GONE);
            if (commonEmptyMessage != null) commonEmptyMessage.setVisibility(View.GONE);
            return;
        }

        if (commonProgressBar != null) commonProgressBar.setVisibility(View.GONE);

        if (itemCount == 0) {
            if (activeRecyclerView != null) activeRecyclerView.setVisibility(View.GONE);
            if (commonEmptyLayout != null) commonEmptyLayout.setVisibility(View.VISIBLE);

            if (commonEmptyMessage != null) {
                commonEmptyMessage.setVisibility(View.VISIBLE);

                // 🔥 Agar aapne commonEmptyMessage ke andar text set karna hai,
                // toh uske andar ka TextView dhoondna hoga:
                TextView tvTitle = commonEmptyMessage.findViewById(R.id.tvEmptyTitle);
                if (tvTitle != null) {
                    tvTitle.setText(emptyMsg);
                }
            }
        } else {
            if (activeRecyclerView != null) activeRecyclerView.setVisibility(View.VISIBLE);
            if (commonEmptyLayout != null) commonEmptyLayout.setVisibility(View.GONE);
            if (commonEmptyMessage != null) commonEmptyMessage.setVisibility(View.GONE);
        }
    }

    // ... baaki methods (fadeOutLoading, performHaptic, etc.) bilkul same rahenge ...

    protected void fadeOutLoading(View loadingView, View contentView) {
        if (loadingView != null && loadingView.getVisibility() == View.VISIBLE) {
            loadingView.animate().alpha(0f).setDuration(200).withEndAction(() ->
                    loadingView.setVisibility(View.GONE)).start();

            if (contentView != null) {
                contentView.setAlpha(0f);
                contentView.setVisibility(View.VISIBLE);
                contentView.animate().alpha(1f).setDuration(300).start();
            }
        }
    }

    protected void performHaptic() {
        if (isAdded() && getView() != null) {
            getView().performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
        }
    }

    protected boolean isFragmentValid() {
        return isAdded() && getView() != null && getContext() != null;
    }

    @Override
    public void onDestroyView() {
        if (activeRecyclerView != null) {
            activeRecyclerView.setAdapter(null);
        }
        super.onDestroyView();
    }
}