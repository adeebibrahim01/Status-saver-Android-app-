package com.mariaxcodexpert.whatsdownloadplus.ui.base;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SimpleItemAnimator;

import com.mariaxcodexpert.whatsdownloadplus.R;

public abstract class BaseMediaFragment extends Fragment {

    protected RecyclerView activeRecyclerView;
    protected ProgressBar commonProgressBar;

    protected View commonEmptyMessage;
    protected View commonEmptyLayout;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        commonProgressBar = view.findViewById(R.id.firstLoadProgress);
        if (commonProgressBar == null) commonProgressBar = view.findViewById(R.id.progressBar);

        commonEmptyMessage = view.findViewById(R.id.tvEmptyMessage);
        commonEmptyLayout = view.findViewById(R.id.emptyStateLayout);
    }

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