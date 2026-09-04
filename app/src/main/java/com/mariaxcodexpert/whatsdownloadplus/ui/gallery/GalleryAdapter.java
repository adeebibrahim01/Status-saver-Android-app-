package com.mariaxcodexpert.whatsdownloadplus.ui.gallery;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.RequestManager;
import com.mariaxcodexpert.whatsdownloadplus.Ads.AdManager;
import com.mariaxcodexpert.whatsdownloadplus.Helper.FeedbackPromptManager;
import com.mariaxcodexpert.whatsdownloadplus.R;
import com.mariaxcodexpert.whatsdownloadplus.data.local.ImagesEntity.ImageEntity;
import com.mariaxcodexpert.whatsdownloadplus.data.local.VideosEntity.VideoEntity;
import com.mariaxcodexpert.whatsdownloadplus.ui.base.BaseMediaViewHolder;
import com.mariaxcodexpert.whatsdownloadplus.ui.base.MediaViewUtils;
import com.mariaxcodexpert.whatsdownloadplus.ui.utils.media.MediaStatusUtils;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class GalleryAdapter extends ListAdapter<Object, GalleryAdapter.GalleryViewHolder> {

    private final RequestManager glide;
    private final OnItemClickListener clickListener;
    private final OnDownloadClickListener downloadListener;
    private final GalleryViewModel viewModel;
    private FeedbackPromptManager feedbackManager;

    private static final TreeMap<Long, Integer> TIMER_COLOR_MAP = new TreeMap<>();
    static {
        TIMER_COLOR_MAP.put(0L, 0xFFFF4444);
        TIMER_COLOR_MAP.put(1L, 0xFFFFBB33);
        TIMER_COLOR_MAP.put(6L, 0xFF25D366);
    }

    public interface OnItemClickListener {
        void onItemClick(Object item);
    }

    public interface OnDownloadClickListener {
        void onDownloadClick(Object item, GalleryViewHolder holder, String uri, boolean isVideo, String name, boolean isDownloaded);
    }

    public GalleryAdapter(RequestManager glide, GalleryViewModel vm, OnItemClickListener itemListener, OnDownloadClickListener downloadListener) {
        super(new GalleryDiffCallback());
        this.glide = glide;
        this.viewModel = vm;
        this.clickListener = itemListener;
        this.downloadListener = downloadListener;
    }

    @NonNull @Override
    public GalleryViewHolder onCreateViewHolder(@NonNull ViewGroup p, int t) {
        View v = LayoutInflater.from(p.getContext()).inflate(R.layout.item_images_videos, p, false);
        return new GalleryViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull GalleryViewHolder h, int p) {
        Object item = getItem(p);
        if (item == null || h.videoIcon == null) return;

        String path = null;
        boolean isDownloaded = false;
        long expiryTime = 0L;
        String fileName = null;

        if (item instanceof ImageEntity) {
            ImageEntity img = (ImageEntity) item;
            if (img != null) {
                path = img.getUri();
                isDownloaded = img.isDownloaded;
                expiryTime = img.expiryTime;
                fileName = img.fileName;
            }
            h.videoIcon.setVisibility(View.GONE);
        } else if (item instanceof VideoEntity) {
            VideoEntity vid = (VideoEntity) item;
            if (vid != null) {
                path = vid.getUri();
                isDownloaded = vid.isDownloaded;
                expiryTime = vid.expiryTime;
                fileName = vid.fileName;
            }
            h.videoIcon.setVisibility(View.VISIBLE);
        } else {
            return;
        }

        if (fileName != null && !fileName.isEmpty()) {
            int statusId = Math.abs(fileName.hashCode());
            Context context = h.itemView.getContext().getApplicationContext();

            if (!isDownloaded && context != null) {
                try {
                    com.mariaxcodexpert.whatsdownloadplus.model.StatusStorage.handleFirebaseSync(
                            context,
                            statusId,
                            expiryTime,
                            false
                    );
                } catch (Exception ignored) {}
            }
        }

        if (path != null && !path.isEmpty()) {
            if (h.imageThumb.getTag() == null || !h.imageThumb.getTag().equals(path)) {
                MediaViewUtils.loadImage(glide, path, h.imageThumb);
                h.imageThumb.setTag(path);
            }
        }

        MediaViewUtils.updateStatusUI(h, isDownloaded);
        h.startTimer(expiryTime);

        h.imageThumb.setOnClickListener(v -> {
            int pos = h.getBindingAdapterPosition();
            if (pos != RecyclerView.NO_POSITION && clickListener != null) {
                Object freshItem = getItem(pos);
                if (freshItem != null) clickListener.onItemClick(freshItem);
            }
        });

        h.downloadIcon.setOnClickListener(v -> {
            int pos = h.getBindingAdapterPosition();
            if (pos != RecyclerView.NO_POSITION && downloadListener != null) {
                Object freshItem = getItem(pos);
                if (freshItem == null) return;

                String u = null, n = null;
                boolean isVid = false, isDown = false;

                if (freshItem instanceof ImageEntity) {
                    ImageEntity img = (ImageEntity) freshItem;
                    if (img != null) {
                        u = img.getUri(); n = img.fileName; isDown = img.isDownloaded; isVid = false;
                    }
                } else if (freshItem instanceof VideoEntity) {
                    VideoEntity vid = (VideoEntity) freshItem;
                    if (vid != null) {
                        u = vid.getUri(); n = vid.fileName; isDown = vid.isDownloaded; isVid = true;
                    }
                }

                if (u != null && n != null) {
                    downloadListener.onDownloadClick(freshItem, h, u, isVid, n, isDown);
                }
            }
        });
    }

    @Override
    public void onBindViewHolder(@NonNull GalleryViewHolder holder, int position, @NonNull List<Object> payloads) {
        if (!payloads.isEmpty() && payloads.contains("FORCE_TICK_UPDATE")) {
            Object item = getItem(position);
            if (item != null) {
                boolean isDown = false;
                if (item instanceof ImageEntity) {
                    isDown = ((ImageEntity) item).isDownloaded;
                } else if (item instanceof VideoEntity) {
                    VideoEntity vid = (VideoEntity) item;
                    if (vid != null) isDown = vid.isDownloaded;
                }
                MediaViewUtils.updateStatusUI(holder, isDown);
            }
        } else {
            super.onBindViewHolder(holder, position, payloads);
        }
    }

    public void handleAdThenDownload(Object item, GalleryViewHolder h, String path, String name, boolean isVid) {
        if (item == null || path == null || name == null) return;
        Context ctx = h.itemView.getContext();
        Activity act = (ctx instanceof Activity) ? (Activity) ctx : null;

        if (act != null && !act.isFinishing() && AdManager.isInterstitialLoaded()) {
            final Activity finalAct = act;
            AdManager.showInterstitial(finalAct, new AdManager.AdCallback() {
                @Override public void onAdClosed() { startDownloadProcess(item, h, finalAct, path, name, isVid); }
                @Override public void onAdFailed() { startDownloadProcess(item, h, finalAct, path, name, isVid); }
            });
        } else {
            startDownloadProcess(item, h, ctx, path, name, isVid);
        }
    }

    private void startDownloadProcess(Object item, GalleryViewHolder h, Context ctx, String path, String name, boolean isVid) {
        final int originalPosition = h.getBindingAdapterPosition();
        if (originalPosition == RecyclerView.NO_POSITION || ctx == null || path == null || name == null || item == null) return;

        if (h.downloadOverlay != null) {
            h.downloadOverlay.setVisibility(View.VISIBLE);
            if (h.neonProgressBar != null) h.neonProgressBar.setProgress(0);
            if (h.progressText != null) h.progressText.setText("0%");
        }

        Handler handler = new Handler(Looper.getMainLooper());
        new Thread(() -> {
            for (int i = 0; i <= 100; i += 10) {
                int progress = i;
                handler.post(() -> {
                    if (h.getBindingAdapterPosition() == originalPosition) {
                        if (h.neonProgressBar != null) h.neonProgressBar.setProgress(progress);
                        if (h.progressText != null) h.progressText.setText(progress + "%");
                    }
                });
                try { Thread.sleep(50); } catch (InterruptedException ignored) {}
            }

            handler.post(() -> {
                MediaStatusUtils.saveToGallery(ctx, Uri.parse(path), null, name, isVid, 100, (success, uri) -> {
                    h.itemView.post(() -> {
                        int currentPos = h.getBindingAdapterPosition();
                        if (currentPos == RecyclerView.NO_POSITION || currentPos != originalPosition) {
                            if (h.downloadOverlay != null) h.downloadOverlay.setVisibility(View.GONE);
                            return;
                        }

                        if (Boolean.TRUE.equals(success) && uri != null) {
                            long expiryTime = 0L;
                            if (item instanceof ImageEntity) {
                                expiryTime = ((ImageEntity) item).expiryTime;
                            } else if (item instanceof VideoEntity) {
                                VideoEntity vid = (VideoEntity) item;
                                if (vid != null) expiryTime = vid.expiryTime;
                            }

                            try {
                                com.mariaxcodexpert.whatsdownloadplus.model.StatusStorage.handleFirebaseSync(ctx.getApplicationContext(), Math.abs(name.hashCode()), expiryTime, true);
                            } catch (Exception ignored) {}

                            if (ctx instanceof Activity && !((Activity) ctx).isFinishing()) {
                                if (feedbackManager == null) feedbackManager = new FeedbackPromptManager((Activity) ctx);
                                feedbackManager.incrementSuccessAndCheck();
                            }

                            updateItemDownloadedState(item, uri.toString());
                            notifyItemChanged(currentPos, "FORCE_TICK_UPDATE");
                        }
                        if (h.downloadOverlay != null) {
                            h.downloadOverlay.postDelayed(() -> h.downloadOverlay.setVisibility(View.GONE), 500);
                        }
                    });
                });
            });
        }).start();
    }

    private void updateItemDownloadedState(Object item, String uriStr) {
        if (item instanceof ImageEntity) {
            ImageEntity img = (ImageEntity) item;
            if (img != null) {
                img.isDownloaded = true;
                if (viewModel != null) viewModel.markImageDownloaded(img, uriStr);
            }
        } else if (item instanceof VideoEntity) {
            VideoEntity vid = (VideoEntity) item;
            if (vid != null) {
                vid.isDownloaded = true;
                if (viewModel != null) viewModel.markVideoDownloaded(vid, uriStr);
            }
        }
    }

    @Override
    public void onViewRecycled(@NonNull GalleryViewHolder holder) {
        super.onViewRecycled(holder);
        holder.stopTimer();
    }

    public static class GalleryViewHolder extends BaseMediaViewHolder {
        TextView countdownTimer, progressText;
        RelativeLayout downloadOverlay;
        ProgressBar neonProgressBar;
        Runnable timerRunnable;
        private final Handler timerHandler = new Handler(Looper.getMainLooper());

        public GalleryViewHolder(@NonNull View v) {
            super(v);
            countdownTimer = v.findViewById(R.id.countdownTimer);
            downloadOverlay = v.findViewById(R.id.downloadOverlay);
            neonProgressBar = v.findViewById(R.id.neonProgressBar);
            progressText = v.findViewById(R.id.progressText);
        }

        void startTimer(long expiry) {
            stopTimer();
            updateTimerUI(expiry);
            timerRunnable = new Runnable() {
                @Override
                public void run() {
                    if (itemView.isAttachedToWindow()) {
                        updateTimerUI(expiry);
                        timerHandler.postDelayed(this, 1000);
                    } else {
                        stopTimer();
                    }
                }
            };
            timerHandler.postDelayed(timerRunnable, 1000);
        }

        private void updateTimerUI(long expiry) {
            if (countdownTimer == null) return;
            long diff = expiry - System.currentTimeMillis();
            Context context = itemView.getContext();
            if (context == null) return;

            if (diff <= 0) {
                countdownTimer.setText(context.getString(R.string.timer_expired));
                Integer color = TIMER_COLOR_MAP.get(0L);
                if (color != null) countdownTimer.setTextColor(color);
            } else {
                long h = (diff / 3600000);
                long m = (diff / 60000) % 60;
                long s = (diff / 1000) % 60;

                countdownTimer.setText(context.getString(R.string.timer_expires_in, h, m, s));

                Map.Entry<Long, Integer> colorEntry = TIMER_COLOR_MAP.floorEntry(h);
                if (colorEntry != null) {
                    countdownTimer.setTextColor(colorEntry.getValue());
                }
            }
        }

        void stopTimer() {
            if (timerRunnable != null) {
                timerHandler.removeCallbacks(timerRunnable);
                timerRunnable = null;
            }
        }
    }
}