package nl.jpelgrm.movienotifier.ui.settings;

import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.Fragment;

import dev.chrisbanes.insetter.Insetter;
import dev.chrisbanes.insetter.Side;
import nl.jpelgrm.movienotifier.databinding.FragmentSettingsLicensesBinding;

public class SettingsLicensesFragment extends Fragment {
    private FragmentSettingsLicensesBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentSettingsLicensesBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Insetter.builder().applySystemWindowInsetsToPadding(Side.BOTTOM).applyToView(binding.main);
            ViewCompat.requestApplyInsets(binding.main);
        }
    }
}
