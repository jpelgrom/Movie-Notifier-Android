package nl.jpelgrm.movienotifier.ui.settings;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import nl.jpelgrm.movienotifier.databinding.FragmentAccountStartBinding;

public class AccountStartFragment extends Fragment {
    private FragmentAccountStartBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentAccountStartBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        binding.login.setOnClickListener(view1 -> {
            if(getActivity() != null && !getActivity().isFinishing()) {
                ((AccountActivity) getActivity()).showLogin();
            }
        });
        binding.signup.setOnClickListener(view1 -> {
            if(getActivity() != null && !getActivity().isFinishing()) {
                ((AccountActivity) getActivity()).showSignup();
            }
        });
    }
}
