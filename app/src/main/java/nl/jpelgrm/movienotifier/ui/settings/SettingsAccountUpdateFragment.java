package nl.jpelgrm.movienotifier.ui.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.Fragment;
import android.support.v7.widget.AppCompatEditText;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;

import java.io.IOException;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import nl.jpelgrm.movienotifier.R;
import nl.jpelgrm.movienotifier.data.APIHelper;
import nl.jpelgrm.movienotifier.data.DBHelper;
import nl.jpelgrm.movienotifier.models.User;
import nl.jpelgrm.movienotifier.models.error.Errors;
import nl.jpelgrm.movienotifier.models.error.Message;
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

        user = DBHelper.getInstance(getContext()).getUserByID(id);

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
                    text.setText(user.getName());
                    text.setInputType(InputType.TYPE_CLASS_TEXT);
                    textWrapper.setHint(getString(R.string.user_input_name));
                    break;
                case EMAIL:
                    text.setText(user.getEmail());
                    text.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
                    textWrapper.setHint(getString(R.string.user_input_email));
                    break;
                case PHONE:
                    text.setText(user.getPhonenumber());
                    text.setInputType(InputType.TYPE_CLASS_PHONE);
                    textWrapper.setHint(getString(R.string.user_input_phone));
                    break;
            }
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

        Call<User> call = APIHelper.getInstance().updateUser(user.getApikey(), user.getID(), toUpdate);
        call.enqueue(new Callback<User>() {
            @Override
            public void onResponse(Call<User> call, Response<User> response) {
                progress.setVisibility(View.GONE);
                setFieldsEnabled(true);

                if(response.code() == 200) {
                    DBHelper db = DBHelper.getInstance(getContext());
                    User received = response.body();

                    db.updateUser(received);
                    user = received;

                    if(getActivity() != null && !getActivity().isFinishing()) {
                        ((SettingsActivity) getActivity()).updatedUser(mode);
                    }
                } else {
                    setFieldsEnabled(true);
                    setProgressVisible(false);

                    Gson gson = new GsonBuilder().create();
                    StringBuilder errorBuilder = new StringBuilder();

                    if(response.code() == 400) {
                        if(response.errorBody() != null) {
                            try {
                                Errors errors = gson.fromJson(response.errorBody().string(), Errors.class);
                                for(String errorString : errors.getErrors()) {
                                    if(!errorBuilder.toString().equals("")) {
                                        errorBuilder.append("\n");
                                    }
                                    errorBuilder.append(errorString);
                                }
                            } catch(IOException e) {
                                errorBuilder.append(getString(R.string.error_general_server, "I400"));
                            }
                        } else {
                            errorBuilder.append(getString(R.string.error_general_server, "N400"));
                        }

                        error.setText(errorBuilder.toString());
                    } else if(response.code() == 500){
                        if(response.errorBody() != null) {
                            try {
                                Message message = gson.fromJson(response.errorBody().string(), Message.class);
                                errorBuilder.append(message.getMessage());
                            } catch(IOException e) {
                                errorBuilder.append("I500");
                            }
                        } else {
                            errorBuilder.append("N500");
                        }

                        error.setText(getString(R.string.error_general_message, errorBuilder.toString()));
                    } else {
                        errorBuilder.append(getString(R.string.error_general_server, "H" + response.code()));
                        error.setText(errorBuilder.toString());
                    }

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
