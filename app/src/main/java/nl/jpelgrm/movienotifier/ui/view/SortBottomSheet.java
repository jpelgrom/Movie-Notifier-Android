package nl.jpelgrm.movienotifier.ui.view;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import nl.jpelgrm.movienotifier.R;
import nl.jpelgrm.movienotifier.databinding.FragmentBottomsheetSortBinding;
import nl.jpelgrm.movienotifier.ui.WatchersFragment;

import static android.content.Context.MODE_PRIVATE;

public class SortBottomSheet extends BottomSheetDialogFragment {
    private FragmentBottomsheetSortBinding binding;

    private SharedPreferences settings;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        settings = getContext().getSharedPreferences("settings", MODE_PRIVATE);
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        dismissalTask();
        super.onDismiss(dialog);
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        dismissalTask();
        super.onCancel(dialog);
    }

    private void dismissalTask() {
        if(getActivity().getSupportFragmentManager().findFragmentByTag("watchersFragment") != null) {
            ((WatchersFragment) getActivity().getSupportFragmentManager().findFragmentByTag("watchersFragment")).filterAndSort(true);
        }
    }

    @Override
    public void setupDialog(Dialog dialog, int style) {
        super.setupDialog(dialog, style);
        binding = FragmentBottomsheetSortBinding.inflate(LayoutInflater.from(getContext()), null, false);
        dialog.setContentView(binding.getRoot());

        setupViews();
    }

    private void setupViews() {
        int pref = settings.getInt("listSort", 0);
        switch(pref) {
            case 1: // Begin (start checking)
                binding.sortBeginText.setTextColor(ContextCompat.getColor(getContext(), R.color.colorSecondary));
                break;
            case 2: // End (stop checking)
                binding.sortEndText.setTextColor(ContextCompat.getColor(getContext(), R.color.colorSecondary));
                break;
            case 3: // Start after (first showing)
                binding.sortStartAfterText.setTextColor(ContextCompat.getColor(getContext(), R.color.colorSecondary));
                break;
            case 4: // A-Z
                binding.sortAZText.setTextColor(ContextCompat.getColor(getContext(), R.color.colorSecondary));
                break;
            case 0: // Automagic (status, then A-Z)
            default:
                binding.sortAutoText.setTextColor(ContextCompat.getColor(getContext(), R.color.colorSecondary));
                break;
        }

        binding.sortAuto.setOnClickListener(view -> {
            settings.edit().putInt("listSort", 0).apply();
            dismiss();
        });
        binding.sortBegin.setOnClickListener(view -> {
            settings.edit().putInt("listSort", 1).apply();
            dismiss();
        });
        binding.sortEnd.setOnClickListener(view -> {
            settings.edit().putInt("listSort", 2).apply();
            dismiss();
        });
        binding.sortStartAfter.setOnClickListener(view -> {
            settings.edit().putInt("listSort", 3).apply();
            dismiss();
        });
        binding.sortAZ.setOnClickListener(view -> {
            settings.edit().putInt("listSort", 4).apply();
            dismiss();
        });
        binding.close.setOnClickListener(view -> dismiss());
    }
}
