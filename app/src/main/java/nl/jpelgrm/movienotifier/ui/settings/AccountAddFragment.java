package nl.jpelgrm.movienotifier.ui.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.android.material.textfield.TextInputLayout;

import java.util.Collections;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.fragment.app.Fragment;
import butterknife.BindView;
import butterknife.ButterKnife;
import nl.jpelgrm.movienotifier.R;
import nl.jpelgrm.movienotifier.data.APIHelper;
import nl.jpelgrm.movienotifier.data.AppDatabase;
import nl.jpelgrm.movienotifier.models.User;
import nl.jpelgrm.movienotifier.ui.view.IconSwitchView;
import nl.jpelgrm.movienotifier.util.ErrorUtil;
import nl.jpelgrm.movienotifier.util.InterfaceUtil;
import nl.jpelgrm.movienotifier.util.UserValidation;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AccountAddFragment extends Fragment {
    SharedPreferences settings;
    SharedPreferences notificationSettings;

    @BindView(R.id.progress) ProgressBar progress;
    @BindView(R.id.error) TextView error;

    @BindView(R.id.nameWrapper) TextInputLayout nameWrapper;
    @BindView(R.id.name) AppCompatEditText name;
    @BindView(R.id.passwordWrapper) TextInputLayout passwordWrapper;
    @BindView(R.id.password) AppCompatEditText password;
    @BindView(R.id.push) IconSwitchView push;
    @BindView(R.id.emailOn) IconSwitchView emailOn;
    @BindView(R.id.emailWrapper) TextInputLayout emailWrapper;
    @BindView(R.id.email) AppCompatEditText email;

    @BindView(R.id.go) Button go;

    Handler validateNameHandler = new Handler();
    Handler validateEmailHandler = new Handler();
    Handler validatePasswordHandler = new Handler();
    Runnable validateNameRunnable = this::validateName;
    Runnable validateEmailRunnable = this::validateEmail;
    Runnable validatePasswordRunnable = this::validatePassword;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        settings = getActivity().getSharedPreferences("settings", Context.MODE_PRIVATE);
        notificationSettings = getActivity().getSharedPreferences("notifications", Context.MODE_PRIVATE);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_account_add, container, false);
        ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        name.addTextChangedListener(new TextWatcher() {
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
        email.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }
            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }
            @Override
            public void afterTextChanged(Editable editable) {
                validateEmailHandler.removeCallbacks(validateEmailRunnable);
                validateEmailHandler.postDelayed(validateEmailRunnable, 1000);
            }
        });
        password.addTextChangedListener(new TextWatcher() {
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

        if(!notificationSettings.getString("token", "").equals("")) {
            push.setChecked(true);
        }
        emailOn.setOnSwitchClickListener(v -> {
            if(emailOn.isChecked() && emailWrapper.getVisibility() != View.VISIBLE) {
                emailWrapper.setVisibility(View.VISIBLE);
            } else if(!emailOn.isChecked()) {
                if(emailWrapper.getVisibility() != View.GONE) {
                    emailWrapper.setVisibility(View.GONE);
                }
                InterfaceUtil.hideKeyboard(getActivity());
            }
        });
        go.setOnClickListener(v -> checkForRegister(true));
    }

    @Override
    public void onDestroy() {
        validateNameHandler.removeCallbacksAndMessages(null);
        validateEmailHandler.removeCallbacksAndMessages(null);
        validatePasswordHandler.removeCallbacksAndMessages(null);

        super.onDestroy();
    }

    private void checkForRegister(boolean warnAboutNotifications) {
        error.setVisibility(View.GONE);
        InterfaceUtil.hideKeyboard(getActivity());

        if(validateName() && validatePassword()) {
            if(warnAboutNotifications && !push.isChecked() && !emailOn.isChecked()) {
                new AlertDialog.Builder(getContext()).setMessage(R.string.user_validate_notifications)
                        .setPositiveButton(R.string.yes, (dialog, which) -> checkForRegister(false))
                        .setNegativeButton(R.string.no, null).show();
                return;
            }

            if((!emailOn.isChecked() || validateEmail())) {
                User toCreate = new User(name.getText().toString(),
                        emailOn.isChecked() ? email.getText().toString() : "",
                        password.getText().toString());
                if(push.isChecked() && !notificationSettings.getString("token", "").equals("")) {
                    toCreate.setFcmTokens(Collections.singletonList(notificationSettings.getString("token", "")));
                } else {
                    toCreate.setFcmTokens(Collections.emptyList());
                }

                register(toCreate);
            }
        }
    }

    private boolean validateName() {
        if(UserValidation.validateName(name.getText().toString())) {
            nameWrapper.setErrorEnabled(false);
            return true;
        } else {
            nameWrapper.setError(getString(name.getText().toString().length() >= 4 && name.getText().toString().length() <= 16 ?
                    R.string.user_validate_name_regex : R.string.user_validate_name_length));
            nameWrapper.setErrorEnabled(true);
            return false;
        }
    }

    private boolean validateEmail() {
        if(UserValidation.validateEmail(email.getText().toString())) {
            emailWrapper.setErrorEnabled(false);
            return true;
        } else {
            emailWrapper.setError(getString(R.string.user_validate_email));
            emailWrapper.setErrorEnabled(true);
            return false;
        }
    }

    private boolean validatePassword() {
        if(UserValidation.validatePassword(password.getText().toString())) {
            passwordWrapper.setErrorEnabled(false);
            return true;
        } else {
            passwordWrapper.setError(getString(R.string.user_validate_password));
            passwordWrapper.setErrorEnabled(true);
            return false;
        }
    }

    private void register(User user) {
        setFieldsEnabled(false);
        setProgressVisible(true);

        Call<User> call = APIHelper.getInstance().addUser(user);
        call.enqueue(new Callback<User>() {
            @Override
            public void onResponse(@NonNull Call<User> call, @NonNull Response<User> response) {
                if(response.isSuccessful()) {
                    User received = response.body();
                    AsyncTask.execute(() -> {
                        AppDatabase.getInstance(getContext()).users().add(received);
                        settings.edit().putString("userID", received.getId()).putString("userAPIKey", received.getApikey()).apply();
                        notificationSettings.edit().putBoolean("disabled-" + received.getId(), false).apply();
                        if(getActivity() != null && !getActivity().isFinishing()) {
                            getActivity().runOnUiThread(() -> getActivity().finish());
                        }
                    });
                } else {
                    setFieldsEnabled(true);
                    setProgressVisible(false);

                    error.setText(ErrorUtil.getErrorMessage(getContext(), response));
                    error.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onFailure(@NonNull Call<User> call, @NonNull Throwable t) {
                setFieldsEnabled(true);
                setProgressVisible(false);

                t.printStackTrace();

                error.setText(ErrorUtil.getErrorMessage(getContext(), null));
                error.setVisibility(View.VISIBLE);
            }
        });
    }

    private void setFieldsEnabled(boolean enabled) {
        nameWrapper.setEnabled(enabled);
        passwordWrapper.setEnabled(enabled);
        push.setClickable(enabled);
        emailOn.setClickable(enabled);
        emailWrapper.setEnabled(enabled);
        go.setEnabled(enabled);
    }

    private void setProgressVisible(boolean visible) {
        progress.setVisibility(visible ? View.VISIBLE : View.GONE);
    }
}
