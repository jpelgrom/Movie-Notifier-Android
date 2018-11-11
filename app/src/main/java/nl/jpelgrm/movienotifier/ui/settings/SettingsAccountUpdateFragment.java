package nl.jpelgrm.movienotifier.ui.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import androidx.annotation.Nullable;
import com.google.android.material.textfield.TextInputLayout;
import androidx.fragment.app.Fragment;
import androidx.appcompat.widget.AppCompatEditText;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;

import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import nl.jpelgrm.movienotifier.R;
import nl.jpelgrm.movienotifier.data.APIHelper;
import nl.jpelgrm.movienotifier.data.AppDatabase;
import nl.jpelgrm.movienotifier.models.User;
import nl.jpelgrm.movienotifier.util.ErrorUtil;
import nl.jpelgrm.movienotifier.util.InterfaceUtil;
import nl.jpelgrm.movienotifier.util.UserValidation;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SettingsAccountUpdateFragment extends Fragment {
    public enum UpdateMode {
        NAME, EMAIL, PHONE, PASSWORD
    }

    @BindView(R.id.progress) ProgressBar progress;
    @BindView(R.id.error) TextView error;

    @BindView(R.id.textWrapper) TextInputLayout textWrapper;
    @BindView(R.id.text) AppCompatEditText text;
    @BindView(R.id.passwordWrapper) TextInputLayout passwordWrapper;
    @BindView(R.id.password) AppCompatEditText password;

    @BindView(R.id.update) Button update;

    private User user;
    private String id;
    private UpdateMode mode;

    private SharedPreferences settings;
    Handler validateTextHandler = new Handler();
    Handler validatePasswordHandler = new Handler();
    Runnable validateNameRunnable = new Runnable() {
        @Override
        public void run() {
            validateName();
        }
    };
    Runnable validateEmailRunnable = new Runnable() {
        @Override
        public void run() {
            validateEmail();
        }
    };
    Runnable validatePhoneRunnable = new Runnable() {
        @Override
        public void run() {
            if(validatePhone()) {
                if(!text.getText().toString().equals(getRFCPhoneNumber())) {
                    text.setText(getRFCPhoneNumber());
                }
            }
        }
    };
    Runnable validatePasswordRunnable = new Runnable() {
        @Override
        public void run() {
            validatePassword();
        }
    };

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
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings_account_update, container, false);
        ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        AppDatabase.getInstance(getContext()).users().getUserById(id).observe(this, user -> {
            this.user = user;
            updateDefaultTextValue();
        });

        if(mode == UpdateMode.PASSWORD) {
            textWrapper.setVisibility(View.GONE);
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
            passwordWrapper.setVisibility(View.VISIBLE);

            password.requestFocus();
            InterfaceUtil.showKeyboard(getActivity(), password);
        } else {
            passwordWrapper.setVisibility(View.GONE);

            switch(mode) {
                case NAME:
                    text.setInputType(InputType.TYPE_CLASS_TEXT);
                    textWrapper.setHint(getString(R.string.user_input_name));
                    break;
                case EMAIL:
                    text.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
                    textWrapper.setHint(getString(R.string.user_input_email));
                    break;
                case PHONE:
                    text.setInputType(InputType.TYPE_CLASS_PHONE);
                    textWrapper.setHint(getString(R.string.user_input_phone));
                    break;
            }
            updateDefaultTextValue();
            text.addTextChangedListener(new TextWatcher() {
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
                        case PHONE:
                            validateTextHandler.removeCallbacks(validatePhoneRunnable);
                            validateTextHandler.postDelayed(validatePhoneRunnable, 1000);
                            break;
                    }
                }
            });

            textWrapper.setVisibility(View.VISIBLE);

            text.requestFocus();
            InterfaceUtil.showKeyboard(getActivity(), text);
        }

        update.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                error.setVisibility(View.GONE);
                InterfaceUtil.hideKeyboard(getActivity());

                if(validate()) {
                    User toUpdate = new User();
                    switch(mode) {
                        case NAME:
                            toUpdate.setName(text.getText().toString());
                            break;
                        case EMAIL:
                            toUpdate.setEmail(text.getText().toString());
                            break;
                        case PHONE:
                            toUpdate.setPhonenumber(text.getText().toString());
                            break;
                        case PASSWORD:
                            toUpdate.setPassword(password.getText().toString());
                            break;
                        default:
                            return;
                    }
                    update(toUpdate);
                }
            }
        });
    }

    private void updateDefaultTextValue() {
        if(user != null) {
            switch(mode) {
                case NAME:
                    text.setText(user.getName());
                    break;
                case EMAIL:
                    text.setText(user.getEmail());
                    break;
                case PHONE:
                    text.setText(user.getPhonenumber());
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
            case PHONE:
                return validatePhone();
            case PASSWORD:
                return validatePassword();
            default:
                return false;
        }
    }

    private boolean validateName() {
        if(UserValidation.validateName(text.getText().toString())) {
            textWrapper.setErrorEnabled(false);
            return true;
        } else {
            textWrapper.setError(getString(text.getText().toString().length() >= 4 && text.getText().toString().length() <= 16 ?
                    R.string.user_validate_name_regex : R.string.user_validate_name_length));
            textWrapper.setErrorEnabled(true);
            return false;
        }
    }

    private boolean validateEmail() {
        if(UserValidation.validateEmail(text.getText().toString())) {
            textWrapper.setErrorEnabled(false);
            return true;
        } else {
            textWrapper.setError(getString(R.string.user_validate_email));
            textWrapper.setErrorEnabled(true);
            return false;
        }
    }

    private boolean validatePhone() {
        if(UserValidation.validatePhone(text.getText().toString())) {
            textWrapper.setErrorEnabled(false);
            return true;
        } else {
            textWrapper.setError(getString(R.string.user_validate_phone));
            textWrapper.setErrorEnabled(true);
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

    private String getRFCPhoneNumber() {
        PhoneNumberUtil util = PhoneNumberUtil.getInstance();
        Phonenumber.PhoneNumber number;
        try {
            number = util.parse(text.getText().toString(), Locale.getDefault().getCountry());
        } catch(NumberParseException e) {
            // Should not happen, otherwise validation would have failed
            return "";
        }
        return util.format(number, PhoneNumberUtil.PhoneNumberFormat.E164);
    }

    private void update(User toUpdate) {
        progress.setVisibility(View.VISIBLE);
        setFieldsEnabled(false);

        Call<User> call = APIHelper.getInstance().updateUser(user.getApikey(), user.getId(), toUpdate);
        call.enqueue(new Callback<User>() {
            @Override
            public void onResponse(Call<User> call, Response<User> response) {
                progress.setVisibility(View.GONE);
                setFieldsEnabled(true);

                if(response.code() == 200) {
                    User received = response.body();
                    AsyncTask.execute(() -> AppDatabase.getInstance(getContext()).users().update(received));
                    user = received;

                    if(getActivity() != null && !getActivity().isFinishing()) {
                        ((SettingsActivity) getActivity()).updatedUser(mode);
                    }
                } else {
                    setFieldsEnabled(true);
                    setProgressVisible(false);

                    error.setText(ErrorUtil.getErrorMessage(getContext(), response));
                    error.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onFailure(Call<User> call, Throwable t) {
                setFieldsEnabled(true);
                setProgressVisible(false);

                t.printStackTrace();

                error.setText(R.string.error_general_exception);
                error.setVisibility(View.VISIBLE);
            }
        });
    }

    private void setFieldsEnabled(boolean enabled) {
        textWrapper.setEnabled(enabled);
        passwordWrapper.setEnabled(enabled);
        update.setEnabled(enabled);
    }

    private void setProgressVisible(boolean visible) {
        progress.setVisibility(visible ? View.VISIBLE : View.GONE);
    }
}
