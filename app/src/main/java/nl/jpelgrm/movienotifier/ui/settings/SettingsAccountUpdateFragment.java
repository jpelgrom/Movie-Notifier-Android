package nl.jpelgrm.movienotifier.ui.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.Fragment;

import nl.jpelgrm.movienotifier.R;
import nl.jpelgrm.movienotifier.data.APIHelper;
import nl.jpelgrm.movienotifier.data.AppDatabase;
import nl.jpelgrm.movienotifier.databinding.FragmentSettingsAccountUpdateBinding;
import nl.jpelgrm.movienotifier.models.User;
import nl.jpelgrm.movienotifier.util.ErrorUtil;
import nl.jpelgrm.movienotifier.util.InterfaceUtil;
import nl.jpelgrm.movienotifier.util.NotificationUtil;
import nl.jpelgrm.movienotifier.util.UserValidation;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SettingsAccountUpdateFragment extends Fragment {
    public enum UpdateMode {
        NAME, EMAIL, PASSWORD
    }

    private FragmentSettingsAccountUpdateBinding binding;

    private User user;
    private String id;
    private UpdateMode mode;

    private SharedPreferences settings;
    Handler validateTextHandler = new Handler();
    Handler validatePasswordHandler = new Handler();
    Runnable validateNameRunnable = this::validateName;
    Runnable validateEmailRunnable = this::validateEmail;
    Runnable validatePasswordRunnable = this::validatePassword;

    public static SettingsAccountUpdateFragment newInstance(String id, UpdateMode mode) {
        SettingsAccountUpdateFragment fragment = new SettingsAccountUpdateFragment();
        Bundle args = new Bundle();
        args.putString("id", id);
        args.putSerializable("mode", mode);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        id = getArguments().getString("id");
        mode = (UpdateMode) getArguments().getSerializable("mode");

        settings = getContext().getSharedPreferences("settings", Context.MODE_PRIVATE);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentSettingsAccountUpdateBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ViewCompat.setOnApplyWindowInsetsListener(binding.main, (v, insets) -> {
                v.setPadding(0, 0, 0, insets.getSystemWindowInsetBottom());
                return insets;
            });
            ViewCompat.requestApplyInsets(binding.main);
        }

        AppDatabase.Companion.getInstance(getContext()).users().getUserById(id).observe(this, user -> {
            this.user = user;
            updateDefaultTextValue();
        });

        if(mode == UpdateMode.PASSWORD) {
            binding.textWrapper.setVisibility(View.GONE);
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
            binding.passwordWrapper.setVisibility(View.VISIBLE);

            binding.password.requestFocus();
            InterfaceUtil.showKeyboard(getActivity(), binding.password);
        } else {
            binding.passwordWrapper.setVisibility(View.GONE);

            switch(mode) {
                case NAME:
                    binding.text.setInputType(InputType.TYPE_CLASS_TEXT);
                    binding.textWrapper.setHint(getString(R.string.user_input_name));
                    break;
                case EMAIL:
                    binding.text.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
                    binding.textWrapper.setHint(getString(R.string.user_input_email));
                    break;
            }
            updateDefaultTextValue();
            binding.text.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                }
                @Override
                public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                }
                @Override
                public void afterTextChanged(Editable editable) {
                    switch(mode) {
                        case NAME:
                            validateTextHandler.removeCallbacks(validateNameRunnable);
                            validateTextHandler.postDelayed(validateNameRunnable, 1000);
                            break;
                        case EMAIL:
                            validateTextHandler.removeCallbacks(validateEmailRunnable);
                            validateTextHandler.postDelayed(validateEmailRunnable, 1000);
                            break;
                    }
                }
            });

            binding.textWrapper.setVisibility(View.VISIBLE);

            binding.text.requestFocus();
            InterfaceUtil.showKeyboard(getActivity(), binding.text);
        }

        binding.update.setOnClickListener(view1 -> {
            binding.error.setVisibility(View.GONE);
            InterfaceUtil.hideKeyboard(getActivity());

            if(validate()) {
                User toUpdate = new User();
                switch(mode) {
                    case NAME:
                        toUpdate.setName(binding.text.getText().toString());
                        break;
                    case EMAIL:
                        toUpdate.setEmail(binding.text.getText().toString());
                        break;
                    case PASSWORD:
                        toUpdate.setPassword(binding.password.getText().toString());
                        break;
                    default:
                        return;
                }
                update(toUpdate);
            }
        });
    }

    private void updateDefaultTextValue() {
        if(user != null) {
            switch(mode) {
                case NAME:
                    binding.text.setText(user.getName());
                    break;
                case EMAIL:
                    if(user.getEmail() != null && !user.getEmail().equals("")) {
                        binding.text.setText(user.getEmail());
                    }
                    break;
            }
        }
    }

    @Override
    public void onDestroy() {
        validateTextHandler.removeCallbacksAndMessages(null);
        validatePasswordHandler.removeCallbacksAndMessages(null);

        super.onDestroy();
    }

    private boolean validate() {
        switch(mode) {
            case NAME:
                return validateName();
            case EMAIL:
                return validateEmail();
            case PASSWORD:
                return validatePassword();
            default:
                return false;
        }
    }

    private boolean validateName() {
        if(UserValidation.validateName(binding.text.getText().toString())) {
            binding.textWrapper.setErrorEnabled(false);
            return true;
        } else {
            binding.textWrapper.setError(getString(binding.text.getText().toString().length() >= 4 && binding.text.getText().toString().length() <= 16 ?
                    R.string.user_validate_name_regex : R.string.user_validate_name_length));
            binding.textWrapper.setErrorEnabled(true);
            return false;
        }
    }

    private boolean validateEmail() {
        if(UserValidation.validateEmail(binding.text.getText().toString())) {
            binding.textWrapper.setErrorEnabled(false);
            return true;
        } else {
            binding.textWrapper.setError(getString(R.string.user_validate_email));
            binding.textWrapper.setErrorEnabled(true);
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

    private void update(User toUpdate) {
        binding.progress.setVisibility(View.VISIBLE);
        setFieldsEnabled(false);

        Call<User> call = APIHelper.INSTANCE.getInstance().updateUser(user.getApikey(), user.getId(), toUpdate);
        call.enqueue(new Callback<User>() {
            @Override
            public void onResponse(@NonNull Call<User> call, @NonNull Response<User> response) {
                binding.progress.setVisibility(View.GONE);
                setFieldsEnabled(true);

                if(response.code() == 200) {
                    User received = response.body();
                    AsyncTask.execute(() -> AppDatabase.Companion.getInstance(getContext()).users().update(received));
                    if(!user.getName().equals(received.getName())) {
                        NotificationUtil.createUserGroup(getContext(), received);
                    }
                    user = received;

                    if(getActivity() != null && !getActivity().isFinishing()) {
                        ((SettingsActivity) getActivity()).updatedUser(mode);
                    }
                } else {
                    setFieldsEnabled(true);
                    setProgressVisible(false);

                    binding.error.setText(ErrorUtil.getErrorMessage(getContext(), response));
                    binding.error.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onFailure(@NonNull Call<User> call, @NonNull Throwable t) {
                setFieldsEnabled(true);
                setProgressVisible(false);

                t.printStackTrace();

                binding.error.setText(R.string.error_general_exception);
                binding.error.setVisibility(View.VISIBLE);
            }
        });
    }

    private void setFieldsEnabled(boolean enabled) {
        binding.textWrapper.setEnabled(enabled);
        binding.passwordWrapper.setEnabled(enabled);
        binding.update.setEnabled(enabled);
    }

    private void setProgressVisible(boolean visible) {
        binding.progress.setVisibility(visible ? View.VISIBLE : View.GONE);
    }
}
