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
import nl.jpelgrm.movienotifier.models.UserCreation;
import nl.jpelgrm.movienotifier.models.error.Errors;
import nl.jpelgrm.movienotifier.models.error.Message;
import nl.jpelgrm.movienotifier.util.InterfaceUtil;
import nl.jpelgrm.movienotifier.util.UserValidation;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AccountAddFragment extends Fragment {
    SharedPreferences settings;

    @BindView(R.id.progress) ProgressBar progress;
    @BindView(R.id.error) TextView error;

    @BindView(R.id.nameWrapper) TextInputLayout nameWrapper;
    @BindView(R.id.name) AppCompatEditText name;
    @BindView(R.id.emailWrapper) TextInputLayout emailWrapper;
    @BindView(R.id.email) AppCompatEditText email;
    @BindView(R.id.phoneWrapper) TextInputLayout phoneWrapper;
    @BindView(R.id.phone) AppCompatEditText phone;
    @BindView(R.id.passwordWrapper) TextInputLayout passwordWrapper;
    @BindView(R.id.password) AppCompatEditText password;

    @BindView(R.id.go) Button go;

    Handler validateNameHandler = new Handler();
    Handler validateEmailHandler = new Handler();
    Handler validatePhoneHandler = new Handler();
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
                if(!phone.getText().toString().equals(getRFCPhoneNumber())) {
                    phone.setText(getRFCPhoneNumber());
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

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        settings = getActivity().getSharedPreferences("settings", Context.MODE_PRIVATE);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_account_add, container, false);
        ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
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
        phone.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }
            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }
            @Override
            public void afterTextChanged(Editable editable) {
                validatePhoneHandler.removeCallbacks(validatePhoneRunnable);
                validatePhoneHandler.postDelayed(validatePhoneRunnable, 1000);
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

        go.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                error.setVisibility(View.GONE);
                InterfaceUtil.hideKeyboard(getActivity());

                if(validateName() && validateEmail() && validatePhone() && validatePassword()) {
                    UserCreation toCreate = new UserCreation(name.getText().toString(), email.getText().toString(), getRFCPhoneNumber(), password.getText().toString());
                    register(toCreate);
                }
            }
        });
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

    private boolean validatePhone() {
        if(UserValidation.validatePhone(phone.getText().toString())) {
            phoneWrapper.setErrorEnabled(false);
            return true;
        } else {
            phoneWrapper.setError(getString(R.string.user_validate_phone));
            phoneWrapper.setErrorEnabled(true);
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
            number = util.parse(phone.getText().toString(), Locale.getDefault().getCountry());
        } catch(NumberParseException e) {
            // Should not happen, otherwise validation would have failed
            return "";
        }
        return util.format(number, PhoneNumberUtil.PhoneNumberFormat.E164);
    }

    private void register(UserCreation user) {
        setFieldsEnabled(false);
        setProgressVisible(true);

        Call<User> call = APIHelper.getInstance().addUser(user);
        call.enqueue(new Callback<User>() {
            @Override
            public void onResponse(Call<User> call, Response<User> response) {
                if(response.isSuccessful()) {
                    User received = response.body();
                    DBHelper.getInstance(getActivity()).addUser(received);
                    settings.edit().putString("userID", received.getUuid()).putString("userAPIKey", received.getApikey()).apply();
                    getActivity().finish();
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
        nameWrapper.setEnabled(enabled);
        emailWrapper.setEnabled(enabled);
        phoneWrapper.setEnabled(enabled);
        passwordWrapper.setEnabled(enabled);
        go.setEnabled(enabled);
    }

    private void setProgressVisible(boolean visible) {
        progress.setVisibility(visible ? View.VISIBLE : View.GONE);
    }
}