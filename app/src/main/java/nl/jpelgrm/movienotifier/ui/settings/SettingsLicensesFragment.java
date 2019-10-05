package nl.jpelgrm.movienotifier.ui.settings;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import nl.jpelgrm.movienotifier.databinding.FragmentSettingsLicensesBinding;

public class SettingsLicensesFragment extends Fragment {
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return FragmentSettingsLicensesBinding.inflate(inflater, container, false).getRoot();
    }
}
