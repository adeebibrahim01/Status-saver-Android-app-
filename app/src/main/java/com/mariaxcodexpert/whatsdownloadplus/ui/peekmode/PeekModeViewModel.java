package com.mariaxcodexpert.whatsdownloadplus.ui.peekmode;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;
import com.mariaxcodexpert.whatsdownloadplus.data.local.Database.AppDatabase;
import com.mariaxcodexpert.whatsdownloadplus.data.local.PeekMessageEntity.PeekMessageEntity;

import java.util.List;

public class PeekModeViewModel extends AndroidViewModel {
    private final MutableLiveData<List<PeekMessageEntity>> messagesLiveData = new MutableLiveData<>();
    private final AppDatabase db;

    public PeekModeViewModel(@NonNull Application application) {
        super(application);
        db = AppDatabase.getInstance(application);
        loadMessages();
    }

    public void loadMessages() {
        new Thread(() -> {
            String uid = db.profileDao().getFirstUserUid();
            if (uid != null) {

                List<PeekMessageEntity> messages = db.peekDao().getDashboardMessages(uid);
                if (messages != null) {
                    for (PeekMessageEntity msg : messages) {
                        msg.totalUnreadSum = db.peekDao().getSumUnreadCount(msg.senderName, uid);
                    }
                }
                messagesLiveData.postValue(messages);
            }
        }).start();
    }

    public MutableLiveData<List<PeekMessageEntity>> getMessagesLiveData() {
        return messagesLiveData;
    }
}