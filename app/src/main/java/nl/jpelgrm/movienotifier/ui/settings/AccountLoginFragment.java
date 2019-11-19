package nl.jpelgrm.movienotifier.ui.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.Fragment;
import androidx.work.WorkManager;

import nl.jpelgrm.movienotifier.R;
import nl.jpelgrm.movienotifier.data.APIHelper;
import nl.jpelgrm.movienotifier.data.AppDatabase;
import nl.jpelgrm.movienotifier.databinding.FragmentAccountLoginBinding;
import nl.jpelgrm.movienotifier.models.User;
import nl.jpelgrm.movienotifier.models.UserLogin;
import nl.jpelgrm.movienotifier.service.FcmRefreshWorker;
import nl.jpelgrm.movienotifier.util.ErrorUtil;
import nl.jpelgrm.movienotifier.util.InterfaceUtil;
import nl.jpelgrm.movienotifier.util.UserValidation;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AccountLoginFragment extends Fragment {
    SharedPreferences settings;

    private FragmentAccountLoginBinding binding;

    Handler validateNameHandler = new Handler();
    Handler validatePasswordHandler = new Handler();
    Runnable validateNameRunnable = this::validateName;
    Runnable validatePasswordRunnable = this::validatePassword;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        settings = getActivity().getSharedPreferences("settings", Context.MODE_PRIVATE);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentAccountLoginBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ViewCompat.setOnApplyWindowInsetsListener(binding.main, (v, insets) -> {
                v.setPadding(0, 0, 0, insets.getSystemWindowInsetBottom());
                return insets;
            });
            ViewCompat.requestApplyInsets(binding.main);
        }

        binding.name.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }
            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }
            @Override
            public void afterTextChanged(Editable editable) {
                validateNameHandler.removeCallbacks(validateNameRunnable);
                validateNameHandler.postDelayed(validateNameRunnable, 1000);
            }
        });
        binding.password.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }
            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }
            @Override
            public void afterTextChanged(Editable editable) {
                validatePasswordHandler.removeCallbacks(validatePasswordRunnable);
                validatePasswordHandler.postDelayed(validatePasswordRunnable, 1000);
            }
        });

        binding.go.setOnClickListener(view1 -> {
            binding.error.setVisibility(View.GONE);
            InterfaceUtil.hideKeyboard(getActivity());

            if(validateName() && validatePassword()) {
                UserLogin toLogin = new UserLogin(binding.name.getText().toString(), binding.password.getText().toString());
                login(toLogin);
            }
        });
    }

    @Override
    public void onDestroy() {
        validateNameHandler.removeCallbacksAndMessages(null);
        validatePasswordHandler.removeCallbacksAndMessages(null);

        super.onDestroy();
    }

    private boolean validateName() {
        if(UserValidation.validateName(binding.name.getText().toString())) {
            binding.nameWrapper.setErrorEnabled(false);
            return true;
        } else {
            binding.nameWrapper.setError(getString(binding.name.getText().toString().length() >= 4 && binding.name.getText().toString().length() <= 16 ?
                    R.string.user_validate_name_regex : R.string.user_validate_name_length));
            binding.nameWrapper.setErrorEnabled(true);
            return false;
        }
    }

    private boolean validatePassword() {
        if(UserValidation.validatePassword(binding.password.getText().toString())) {
            binding.passwordWrapper.setErrorEnabled(false);
            return true;
        } else {
            binding.passwordWrapper.setError(getString(R.string.user_validate_password));
            binding.passwordWrapper.setErrorEnabled(true);
            return false;
        }
    }

    private void login(UserLogin user) {
        setFieldsEnabled(false);
        setProgressVisible(true);

        Call<User> call = APIHelper.getInstance().login(user);
        call.enqueue(new Callback<User>() {
            @Override
            public void onResponse(@NonNull Call<User> call, @NonNull Response<User> response) {
                if(response.isSuccessful()) {
                    User received = response.body();
                    AsyncTask.execute(() -> {
                        AppDatabase.getInstance(getContext()).users().add(received);
                        settings.edit().putString("userID", received.getId()).putString("userAPIKey", received.getApikey()).apply();
                        if(getActivity() != null && !getActivity().isFinishing()) {
                            SharedPreferences notificationSettings = getActivity().getSharedPreferences("notifications", Context.MODE_PRIVATE);
                            notificationSettings.edit().putBoolean("disabled-" + received.getId(), false).apply();
                            String token = notificationSettings.getString("token", "");
                            WorkManager.getInstance().enqueue(FcmRefreshWorker.getRequestToUpdateImmediately(token, received.getId()));
                            getActivity().runOnUiThread(() -> getActivity().finish());
                        }
                    });
                } else {
                    setFieldsEnabled(true);
                    setProgressVisible(false);

                    binding.error.setText(ErrorUtil.getErrorMessage(getContext(), response, true));
                    binding.error.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onFailure(@NonNull Call<User> call, @NonNull Throwable t) {
                setFieldsEnabled(true);
                setProgressVisible(false);

                t.printStackTrace();

                binding.error.setText(ErrorUtil.getErrorMessage(getContext(), null, true));
                binding.error.setVisibility(View.VISIBLE);
            }
        });
    }

    private void setFieldsEnabled(boolean enabled) {
        binding.nameWrapper.setEnabled(enabled);
        binding.passwordWrapper.setEnabled(enabled);
        binding.go.setEnabled(enabled);
    }

    private void setProgressVisible(boolean visible) {
        binding.progress.setVisibility(visible ? View.VISIBLE : View.GONE);
    }
}
