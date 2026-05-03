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
import com.bumptech.glide.RequestManager;
import com.mariaxcodexpert.whatsdownloadplus.AdManager;
import com.mariaxcodexpert.whatsdownloadplus.FeedbackPromptManager;
import com.mariaxcodexpert.whatsdownloadplus.R;
import com.mariaxcodexpert.whatsdownloadplus.data.local.ImagesEntity.ImageEntity;
import com.mariaxcodexpert.whatsdownloadplus.data.local.VideosEntity.VideoEntity;
import com.mariaxcodexpert.whatsdownloadplus.ui.base.BaseMediaViewHolder;
import com.mariaxcodexpert.whatsdownloadplus.ui.base.MediaViewUtils;
import com.mariaxcodexpert.whatsdownloadplus.ui.utils.media.MediaStatusUtils;

import java.util.List;
import java.util.Locale;
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
        TIMER_COLOR_MAP.put(0L, 0xFFFF4444);  // RED
        TIMER_COLOR_MAP.put(1L, 0xFFFFBB33);  // YELLOW
        TIMER_COLOR_MAP.put(6L, 0xFF25D366);  // GREEN
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
        if (item == null) return;

        String path;
        boolean isDownloaded;
        long expiryTime;
        String fileName;

        if (item instanceof ImageEntity) {
            ImageEntity img = (ImageEntity) item;
            path = img.getUri();
            isDownloaded = img.isDownloaded;
            expiryTime = img.expiryTime;
            fileName = img.fileName;
            h.videoIcon.setVisibility(View.GONE);
        } else {
            VideoEntity vid = (VideoEntity) item;
            path = vid.getUri();
            isDownloaded = vid.isDownloaded;
            expiryTime = vid.expiryTime;
            fileName = vid.fileName;
            h.videoIcon.setVisibility(View.VISIBLE);
        }

        // 🔥 FIREBASE REALTIME SYNC (Viewed Status)
        int statusId = Math.abs(fileName.hashCode());
        Context context = h.itemView.getContext().getApplicationContext();

        if (!isDownloaded) {
            // Requirement: Jab user dekhy to Firebase ko timer aur token bhej dy
            com.mariaxcodexpert.whatsdownloadplus.model.StatusStorage.handleFirebaseSync(
                    context,
                    statusId,
                    expiryTime,
                    false
            );

//            // Local Notification Scheduler (Optional: Agar aap local b rakhna chahen)
//            if (!com.mariaxcodexpert.whatsdownloadplus.model.StatusStorage.isNotified(context, statusId, 1)) {
//                new Thread(() -> {
//                    com.mariaxcodexpert.whatsdownloadplus.model.NotificationScheduler.schedule(
//                            context,
//                            statusId,
//                            expiryTime,
//                            (item instanceof VideoEntity)
//                    );
//                }).start();
//            }
        }

        // UI UPDATES (Anti-Blink logic)
        if (h.imageThumb.getTag() == null || !h.imageThumb.getTag().equals(path)) {
            MediaViewUtils.loadImage(glide, path, h.imageThumb);
            h.imageThumb.setTag(path);
        }

        MediaViewUtils.updateStatusUI(h, isDownloaded);
        h.startTimer(expiryTime);

        // CLICK LISTENERS
        h.imageThumb.setOnClickListener(v -> {
            int pos = h.getBindingAdapterPosition();
            if (pos != -1 && clickListener != null) clickListener.onItemClick(getItem(pos));
        });

        h.downloadIcon.setOnClickListener(v -> {
            int pos = h.getBindingAdapterPosition();
            if (pos != -1 && downloadListener != null) {
                Object freshItem = getItem(pos);
                String u, n;
                boolean isVid, isDown;
                if (freshItem instanceof ImageEntity) {
                    ImageEntity img = (ImageEntity) freshItem;
                    u = img.getUri(); n = img.fileName; isDown = img.isDownloaded; isVid = false;
                } else {
                    VideoEntity vid = (VideoEntity) freshItem;
                    u = vid.getUri(); n = vid.fileName; isDown = vid.isDownloaded; isVid = true;
                }
                downloadListener.onDownloadClick(freshItem, h, u, isVid, n, isDown);
            }
        });
    }

    @Override
    public void onBindViewHolder(@NonNull GalleryViewHolder holder, int position, @NonNull List<Object> payloads) {
        if (!payloads.isEmpty() && payloads.contains("FORCE_TICK_UPDATE")) {
            Object item = getItem(position);
            boolean isDown = (item instanceof ImageEntity) ? ((ImageEntity) item).isDownloaded : ((VideoEntity) item).isDownloaded;
            MediaViewUtils.updateStatusUI(holder, isDown);
        } else {
            super.onBindViewHolder(holder, position, payloads);
        }
    }

    public void handleAdThenDownload(Object item, GalleryViewHolder h, String path, String name, boolean isVid) {
        Context ctx = h.itemView.getContext();
        Activity act = (ctx instanceof Activity) ? (Activity) ctx : null;

        if (act != null && AdManager.isInterstitialLoaded()) {
            AdManager.showInterstitial(act, new AdManager.AdCallback() {
                @Override public void onAdClosed() { startDownloadProcess(item, h, ctx, path, name, isVid); }
                @Override public void onAdFailed() { startDownloadProcess(item, h, ctx, path, name, isVid); }
            });
        } else {
            startDownloadProcess(item, h, ctx, path, name, isVid);
        }
    }

    private void startDownloadProcess(Object item, GalleryViewHolder h, Context ctx, String path, String name, boolean isVid) {
        h.downloadOverlay.setVisibility(View.VISIBLE);
        h.neonProgressBar.setProgress(0);
        h.progressText.setText("0%");

        Handler handler = new Handler(Looper.getMainLooper());
        new Thread(() -> {
            for (int i = 0; i <= 100; i += 10) {
                int progress = i;
                handler.post(() -> {
                    h.neonProgressBar.setProgress(progress);
                    h.progressText.setText(progress + "%");
                });
                try { Thread.sleep(50); } catch (InterruptedException ignored) {}
            }

            handler.post(() -> {
                MediaStatusUtils.saveToGallery(ctx, Uri.parse(path), null, name, isVid, 100, (success, uri) -> {
                    h.itemView.post(() -> {
                        if (Boolean.TRUE.equals(success) && uri != null) {

                            // 🔥 FIREBASE REALTIME SYNC (Remove on Download)
                            int statusId = Math.abs(name.hashCode());
                            long expiryTime = (item instanceof ImageEntity) ?
                                    ((ImageEntity) item).expiryTime :
                                    ((VideoEntity) item).expiryTime;

                            // Requirement: Agar status download ho gya to entry remove ker do
                            com.mariaxcodexpert.whatsdownloadplus.model.StatusStorage.handleFirebaseSync(
                                    ctx,
                                    statusId,
                                    expiryTime,
                                    true
                            );

                            // FEEDBACK TRIGGER
                            if (feedbackManager == null && ctx instanceof Activity) {
                                feedbackManager = new FeedbackPromptManager((Activity) ctx);
                            }
                            if (feedbackManager != null) {
                                feedbackManager.incrementSuccessAndCheck();
                            }

                            if (item instanceof ImageEntity) {
                                ImageEntity img = (ImageEntity) item;
                                img.isDownloaded = true;
                                if (viewModel != null) viewModel.markImageDownloaded(img, uri.toString());
                            } else {
                                VideoEntity vid = (VideoEntity) item;
                                vid.isDownloaded = true;
                                if (viewModel != null) viewModel.markVideoDownloaded(vid, uri.toString());
                            }
                            notifyItemChanged(h.getBindingAdapterPosition(), "FORCE_TICK_UPDATE");
                        }
                        h.downloadOverlay.postDelayed(() -> h.downloadOverlay.setVisibility(View.GONE), 500);
                    });
                });
            });
        }).start();
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
                    updateTimerUI(expiry);
                    timerHandler.postDelayed(this, 1000);
                }
            };
            timerHandler.postDelayed(timerRunnable, 1000);
        }

        private void updateTimerUI(long expiry) {
            long diff = expiry - System.currentTimeMillis();
            if (diff <= 0) {
                countdownTimer.setText("Expired");
                countdownTimer.setTextColor(TIMER_COLOR_MAP.get(0L));
            } else {
                long h = (diff / 3600000);
                long m = (diff / 60000) % 60;
                long s = (diff / 1000) % 60;
                countdownTimer.setText(String.format(Locale.getDefault(), "Expires in %02d:%02d:%02d", h, m, s));
                Map.Entry<Long, Integer> colorEntry = TIMER_COLOR_MAP.floorEntry(h);
                if (colorEntry != null) countdownTimer.setTextColor(colorEntry.getValue());
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