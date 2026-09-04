package com.mariaxcodexpert.whatsdownloadplus.ui.language;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.mariaxcodexpert.whatsdownloadplus.R;
import com.mariaxcodexpert.whatsdownloadplus.ui.support.SupportActivity;

public class LanguageFragment extends Fragment {

    private View languageSelectorView;
    private Button btnApply;
    private TextView txtStatus, tvContact;
    private SwitchCompat switchAutoDetect;
    private LanguageManager.LanguageModel selectedLanguage = null;

    public LanguageFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_language, container, false);

        languageSelectorView = view.findViewById(R.id.fragmentLanguageSelector);
        btnApply = view.findViewById(R.id.btnApplyLanguage);
        txtStatus = view.findViewById(R.id.txtStatus);
        tvContact = view.findViewById(R.id.tvContact);
        switchAutoDetect = view.findViewById(R.id.switchAutoDetect);

        Context context = requireContext();
        boolean isAuto = LanguageManager.isAutoDetectEnabled(context);
        switchAutoDetect.setChecked(isAuto);
        languageSelectorView.setEnabled(!isAuto);

        refreshStatusUI();

        switchAutoDetect.setOnCheckedChangeListener((buttonView, isChecked) -> {
            languageSelectorView.setEnabled(!isChecked);
            LanguageManager.setAutoDetect(context, isChecked);
            refreshStatusUI();
            btnApply.setVisibility(View.VISIBLE);
        });

        languageSelectorView.setOnClickListener(v -> showLanguageBottomSheet());

        btnApply.setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_PRESS);

            if (switchAutoDetect.isChecked()) {
                LanguageManager.applyLanguage(context, "system");
            } else if (selectedLanguage != null) {
                LanguageManager.applyLanguage(context, selectedLanguage.getCode());
            }

            btnApply.setVisibility(View.GONE);
            Toast.makeText(context, "Language Applied", Toast.LENGTH_SHORT).show();

            requireActivity().finish();
            requireActivity().overridePendingTransition(0, 0);
            startActivity(requireActivity().getIntent());
            requireActivity().overridePendingTransition(0, 0);
        });

        tvContact.setOnClickListener(v -> startActivity(new Intent(getActivity(), SupportActivity.class)));
        return view;
    }

    private void showLanguageBottomSheet() {
        BottomSheetDialog bottomSheet = new BottomSheetDialog(requireContext(), R.style.BottomSheetDialogTheme);
        View sheetView = getLayoutInflater().inflate(R.layout.layout_language_bottom_sheet, null);
        ListView listView = sheetView.findViewById(R.id.languageListView);

        java.util.List<LanguageManager.LanguageModel> langList = LanguageManager.getSupportedLanguages(requireContext());

        ArrayAdapter<LanguageManager.LanguageModel> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, langList) {
            @NonNull
            @Override
            public View getView(int position, View convertView, @NonNull ViewGroup parent) {
                TextView textView = (TextView) super.getView(position, convertView, parent);
                textView.setText(langList.get(position).getName());
                textView.setTextColor(Color.WHITE);
                textView.setPadding(40, 40, 40, 40);
                textView.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
                return textView;
            }
        };

        listView.setAdapter(adapter);
        bottomSheet.setContentView(sheetView);
        bottomSheet.show();

        listView.setOnItemClickListener((parent, view, position, id) -> {
            selectedLanguage = langList.get(position);
            txtStatus.setText(getString(R.string.status_selected, selectedLanguage.getName()));
            btnApply.setVisibility(View.VISIBLE);
            bottomSheet.dismiss();
        });
    }

    private void refreshStatusUI() {
        Context context = requireContext();
        if (LanguageManager.isAutoDetectEnabled(context)) {
            txtStatus.setText(R.string.status_system_default);
        } else {
            String currentCode = LanguageManager.getSavedLanguageCode(context);
            for (LanguageManager.LanguageModel lang : LanguageManager.getSupportedLanguages(context)) {
                if (lang.getCode().equals(currentCode)) {
                    txtStatus.setText(getString(R.string.status_current, lang.getName()));
                    break;
                }
            }
        }
    }
}