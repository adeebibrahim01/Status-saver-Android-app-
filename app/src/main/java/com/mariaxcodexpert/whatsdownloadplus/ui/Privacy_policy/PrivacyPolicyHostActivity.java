package com.mariaxcodexpert.whatsdownloadplus.ui.Privacy_policy;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.mariaxcodexpert.whatsdownloadplus.R;

public class PrivacyPolicyHostActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_privacy_policy_host);

        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new PrivacyPolicyFragment())
                    .commit();
        }
    }
}
