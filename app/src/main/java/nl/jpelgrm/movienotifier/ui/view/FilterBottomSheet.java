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
import nl.jpelgrm.movienotifier.databinding.FragmentBottomsheetFilterBinding;
import nl.jpelgrm.movienotifier.ui.WatchersFragment;

import static android.content.Context.MODE_PRIVATE;

public class FilterBottomSheet extends BottomSheetDialogFragment {
    private FragmentBottomsheetFilterBinding binding;

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
        binding = FragmentBottomsheetFilterBinding.inflate(LayoutInflater.from(getContext()), null, false);
        dialog.setContentView(binding.getRoot());

        setupViews();
    }

    private void setupViews() {
        // Setting these in XML seems to cause crashes on some older devices
        binding.filterAllIndicator.setText("\uD83D\uDCDA");
        binding.filterPastIndicator.setText("\uD83D\uDCE6"); // Package ('archive', watcher is done and will not be active again)
        binding.filterNowIndicator.setText("\uD83D\uDD34"); // Red Circle ('live', active watcher)
        binding.filterFutureIndicator.setText("⏰"); // Alarm Clock ('planned', watcher will become active in the future)

        int pref = settings.getInt("listFilter", 0);
        switch(pref) {
            case 1: // Past
                binding.filterPastText.setTextColor(ContextCompat.getColor(getContext(), R.color.colorSecondary));
                break;
            case 2: // Now
                binding.filterNowText.setTextColor(ContextCompat.getColor(getContext(), R.color.colorSecondary));
                break;
            case 3: // Future
                binding.filterFutureText.setTextColor(ContextCompat.getColor(getContext(), R.color.colorSecondary));
                break;
            case 0: // All
            default:
                binding.filterAllText.setTextColor(ContextCompat.getColor(getContext(), R.color.colorSecondary));
                break;
        }

        binding.filterAll.setOnClickListener(view -> {
            settings.edit().putInt("listFilter", 0).apply();
            dismiss();
        });
        binding.filterPast.setOnClickListener(view -> {
            settings.edit().putInt("listFilter", 1).apply();
            dismiss();
        });
        binding.filterNow.setOnClickListener(view -> {
            settings.edit().putInt("listFilter", 2).apply();
            dismiss();
        });
        binding.filterFuture.setOnClickListener(view -> {
            settings.edit().putInt("listFilter", 3).apply();
            dismiss();
        });
        binding.close.setOnClickListener(view -> dismiss());
    }
}
