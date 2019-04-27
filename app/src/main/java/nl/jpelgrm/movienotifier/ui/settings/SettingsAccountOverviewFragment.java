package nl.jpelgrm.movienotifier.ui.settings;

import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SwitchCompat;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.fragment.app.Fragment;
import androidx.work.WorkManager;

import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.iid.FirebaseInstanceId;

import java.util.ArrayList;
import java.util.Collections;

import butterknife.BindView;
import butterknife.ButterKnife;
import nl.jpelgrm.movienotifier.BuildConfig;
import nl.jpelgrm.movienotifier.R;
import nl.jpelgrm.movienotifier.data.APIHelper;
import nl.jpelgrm.movienotifier.data.AppDatabase;
import nl.jpelgrm.movienotifier.models.User;
import nl.jpelgrm.movienotifier.service.FcmRefreshWorker;
import nl.jpelgrm.movienotifier.ui.view.DoubleRowIconPreferenceView;
import nl.jpelgrm.movienotifier.ui.view.IconSwitchView;
import nl.jpelgrm.movienotifier.util.ErrorUtil;
import nl.jpelgrm.movienotifier.util.NotificationUtil;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static android.app.Activity.RESULT_OK;

public class SettingsAccountOverviewFragment extends Fragment {
    public static final int INTENT_AUTHENTICATE_PASSWORD = 160;
    public static final int INTENT_AUTHENTICATE_DELETE = 161;

    @BindView(R.id.accountCoordinator) CoordinatorLayout coordinator;

    @BindView(R.id.progress) ProgressBar progress;
    @BindView(R.id.main) ScrollView main;
    @BindView(R.id.error) TextView error;

    @BindView(R.id.accountSwitch) LinearLayout accountSwitch;
    @BindView(R.id.accountName) DoubleRowIconPreferenceView accountName;
    @BindView(R.id.accountPassword) LinearLayout accountPassword;
    @BindView(R.id.accountLogout) LinearLayout accountLogout;
    @BindView(R.id.accountDelete) LinearLayout accountDelete;

    @BindView(R.id.notificationsPush) IconSwitchView notificationsPush;
    @BindView(R.id.notificationsPushReset) TextView notificationsPushReset;
    @BindView(R.id.notificationsPushSystem) TextView notificationsPushSystem;
    @BindView(R.id.notificationsPushHeadsup) SwitchCompat notificationsPushHeadsup;
    @BindView(R.id.notificationsPushSound) SwitchCompat notificationsPushSound;
    @BindView(R.id.notificationsPushVibrate) SwitchCompat notificationsPushVibrate;
    @BindView(R.id.notificationsPushLights) SwitchCompat notificationsPushLights;
    @BindView(R.id.notificationsEmail) IconSwitchView notificationsEmail;
    @BindView(R.id.notificationsEmailAddress) DoubleRowIconPreferenceView notificationsEmailAddress;

    private User user;
    private String id;
    private boolean isCurrentUser;

    private SharedPreferences settings;
    private SharedPreferences notificationSettings;

    public static SettingsAccountOverviewFragment newInstance(String id, boolean isCurrentUser) {
        SettingsAccountOverviewFragment fragment = new SettingsAccountOverviewFragment();
        Bundle args = new Bundle();
        args.putString("id", id);
        args.putBoolean("isCurrentUser", isCurrentUser);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        id = getArguments().getString("id");
        isCurrentUser = getArguments().getBoolean("isCurrentUser");

        settings = getContext().getSharedPreferences("settings", Context.MODE_PRIVATE);
        notificationSettings = getContext().getSharedPreferences("notifications", Context.MODE_PRIVATE);

        verifyFCMToken();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings_account_overview, container, false);
        ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        AppDatabase.getInstance(getContext()).users().getUserById(id).observe(this, user -> {
            if(user != null) {
                this.user = user;
                updateValues();
            }
        });

        accountSwitch.setOnClickListener(v -> switchToThis());
        accountName.setOnClickListener(v -> editDetail(SettingsAccountUpdateFragment.UpdateMode.NAME));
        accountPassword.setOnClickListener(v -> editPassword());
        accountLogout.setOnClickListener(v -> logout());
        accountDelete.setOnClickListener(v -> delete());
        accountSwitch.setVisibility(isCurrentUser ? View.GONE : View.VISIBLE);

        notificationsPush.setOnSwitchClickListener(v -> togglePushNotifications());
        notificationsPushReset.setOnClickListener(v -> resetPushNotifications());
        notificationsPushSystem.setOnClickListener(v -> {
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationUtil.createChannelWatchersPush(getContext(), user);
                Intent intent = new Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
                        .putExtra(Settings.EXTRA_APP_PACKAGE, BuildConfig.APPLICATION_ID)
                        .putExtra(Settings.EXTRA_CHANNEL_ID, NotificationUtil.NOTIFICATION_CHANNEL_PREFIX + user.getId() + NotificationUtil.NOTIFICATION_CHANNEL_WATCHERS_PUSH);
                startActivity(intent);
            }
        });
        notificationsPushHeadsup.setOnCheckedChangeListener((v, isChecked) -> notificationSettings.edit().putBoolean("headsup-" + user.getId(), isChecked).apply());
        notificationsPushSound.setOnCheckedChangeListener((v, isChecked) -> {
            notificationSettings.edit().putBoolean("sound-" + user.getId(), isChecked).apply();
            updateValues();
        });
        notificationsPushVibrate.setOnCheckedChangeListener((v, isChecked) -> {
            notificationSettings.edit().putBoolean("vibrate-" + user.getId(), isChecked).apply();
            updateValues();
        });
        notificationsPushLights.setOnCheckedChangeListener((v, isChecked) -> notificationSettings.edit().putBoolean("lights-" + user.getId(), isChecked).apply());
        notificationsEmail.setOnSwitchClickListener(v -> toggleEmailNotifications());
        notificationsEmailAddress.setOnClickListener(v -> editDetail(SettingsAccountUpdateFragment.UpdateMode.EMAIL));
    }

    public void updatedUser() {
        error.setVisibility(View.GONE);
        // UI updates are triggered via LiveData which will detect a change
    }

    private void updateValues() {
        accountSwitch.setVisibility(user.getId().equals(settings.getString("userID", "")) ? View.GONE : View.VISIBLE);
        accountName.setValue(user.getName());
        notificationsPush.setChecked(user.getFcmTokens().contains(notificationSettings.getString("token", "")));
        notificationsPushReset.setVisibility(BuildConfig.DEBUG && user.getFcmTokens().size() > 0 ? View.VISIBLE : View.GONE);
        notificationsPushSystem.setVisibility(notificationsPush.isChecked() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ? View.VISIBLE : View.GONE);
        notificationsPushSound.setVisibility(notificationsPush.isChecked() && Build.VERSION.SDK_INT < Build.VERSION_CODES.O ? View.VISIBLE : View.GONE);
        notificationsPushSound.setChecked(notificationSettings.getBoolean("sound-" + user.getId(), true));
        notificationsPushVibrate.setVisibility(notificationsPush.isChecked() && Build.VERSION.SDK_INT < Build.VERSION_CODES.O ? View.VISIBLE : View.GONE);
        notificationsPushVibrate.setChecked(notificationSettings.getBoolean("vibrate-" + user.getId(), true));
        notificationsPushLights.setVisibility(notificationsPush.isChecked() && Build.VERSION.SDK_INT < Build.VERSION_CODES.O ? View.VISIBLE : View.GONE);
        notificationsPushLights.setChecked(notificationSettings.getBoolean("lights-" + user.getId(), true));
        notificationsPushHeadsup.setVisibility(notificationsPush.isChecked() && (notificationsPushSound.isChecked() || notificationsPushVibrate.isChecked())
                && Build.VERSION.SDK_INT < Build.VERSION_CODES.O ? View.VISIBLE : View.GONE);
        notificationsPushHeadsup.setChecked(notificationSettings.getBoolean("headsup-" + user.getId(), true));
        notificationsEmail.setChecked(!user.getEmail().equals(""));
        notificationsEmailAddress.setVisibility(!user.getEmail().equals("") ? View.VISIBLE : View.GONE);
        notificationsEmailAddress.setValue(user.getEmail());
    }

    private void editDetail(SettingsAccountUpdateFragment.UpdateMode mode) {
        ((SettingsActivity) getActivity()).editUserDetail(user.getId(), mode);
    }

    private void editPassword() {
        if(getContext() != null) {
            KeyguardManager keyguardManager = (KeyguardManager) getContext().getSystemService(Context.KEYGUARD_SERVICE);
            if(keyguardManager != null) {
                Intent authenticationIntent = keyguardManager.createConfirmDeviceCredentialIntent(null, null);
                if(authenticationIntent != null) {
                    startActivityForResult(authenticationIntent, INTENT_AUTHENTICATE_PASSWORD);
                } else {
                    editDetail(SettingsAccountUpdateFragment.UpdateMode.PASSWORD);
                }
            } else {
                editDetail(SettingsAccountUpdateFragment.UpdateMode.PASSWORD);
            }
        }
    }

    private void togglePushNotifications() {
        String token = notificationSettings.getString("token", "");
        User toUpdate = new User();
        toUpdate.setFcmTokens(new ArrayList<>(user.getFcmTokens()));
        boolean changed = false;
        boolean setToEnabled = notificationsPush.isChecked();
        if(setToEnabled) {
            if(!toUpdate.getFcmTokens().contains(token)) {
                if(!token.equals("")) {
                    changed = toUpdate.getFcmTokens().add(token);
                } else {
                    notificationsPush.setChecked(false);
                }
            }
        } else {
            if(toUpdate.getFcmTokens().contains(token)) {
                changed = toUpdate.getFcmTokens().remove(token);
            }
        }
        if(changed) {
            update(toUpdate, success -> {
                if(success) {
                    notificationSettings.edit().putBoolean("disabled-" + user.getId(), !setToEnabled).apply();
                }
            });
        }
    }

    private void resetPushNotifications() {
        User toUpdate = new User();
        toUpdate.setFcmTokens(Collections.emptyList());
        update(toUpdate, success -> {
            if(success) {
                notificationSettings.edit().putBoolean("disabled-" + user.getId(), true).apply();
            }
        });
    }

    private void toggleEmailNotifications() {
        if(notificationsEmail.isChecked()) {
            notificationsEmail.setChecked(false);
            editDetail(SettingsAccountUpdateFragment.UpdateMode.EMAIL);
        } else {
            User toUpdate = new User();
            toUpdate.setEmail("");
            update(toUpdate, null);
        }
    }

    private void update(User toUpdate, @Nullable OnUpdatedListener listener) {
        error.setVisibility(View.GONE);
        progress.setVisibility(View.VISIBLE);
        setFieldsEnabled(false);

        Call<User> call = APIHelper.getInstance().updateUser(user.getApikey(), user.getId(), toUpdate);
        call.enqueue(new Callback<User>() {
            @Override
            public void onResponse(@NonNull Call<User> call, @NonNull Response<User> response) {
                progress.setVisibility(View.GONE);
                setFieldsEnabled(true);

                if(response.code() == 200) {
                    User received = response.body();
                    AsyncTask.execute(() -> AppDatabase.getInstance(getContext()).users().update(received));
                    user = received;

                    Snackbar.make(coordinator, R.string.user_settings_general_success, Snackbar.LENGTH_SHORT).show();
                } else {
                    error.setVisibility(View.VISIBLE);
                    error.setText(ErrorUtil.getErrorMessage(getContext(), response));

                    main.smoothScrollTo(0, 0);
                }

                updateValues();
                if(listener != null) {
                    listener.onResult(response.code() == 200);
                }
            }

            @Override
            public void onFailure(@NonNull Call<User> call, @NonNull Throwable t) {
                progress.setVisibility(View.GONE);
                setFieldsEnabled(true);

                t.printStackTrace();

                error.setVisibility(View.VISIBLE);
                error.setText(ErrorUtil.getErrorMessage(getContext(), null));

                main.smoothScrollTo(0, 0);

                updateValues();
                if(listener != null) {
                    listener.onResult(false);
                }
            }
        });
    }

    private void switchToThis() {
        error.setVisibility(View.GONE);
        progress.setVisibility(View.VISIBLE);
        setFieldsEnabled(false);

        Call<User> call = APIHelper.getInstance().getUser(user.getApikey(), user.getId());
        call.enqueue(new Callback<User>() {
            @Override
            public void onResponse(@NonNull Call<User> call, @NonNull Response<User> response) {
                progress.setVisibility(View.GONE);
                setFieldsEnabled(true);

                if(response.code() == 200 && response.body() != null) {
                    User received = response.body();
                    if(received != null) {
                        AsyncTask.execute(() -> AppDatabase.getInstance(getContext()).users().update(received));
                        if(!user.getName().equals(received.getName())) {
                            NotificationUtil.createUserGroup(getContext(), received);
                        }
                        user = received;
                        isCurrentUser = true;
                        settings.edit().putString("userID", received.getId()).putString("userAPIKey", received.getApikey()).apply();

                        Snackbar.make(coordinator, R.string.user_settings_general_switch_success, Snackbar.LENGTH_SHORT).show();

                        updateValues();
                    } else {
                        error.setVisibility(View.VISIBLE);
                        error.setText(getString(R.string.error_general_server, "N200"));

                        main.smoothScrollTo(0, 0);
                    }
                } else {
                    error.setVisibility(View.VISIBLE);
                    error.setText(getString(R.string.error_general_server, "N" + response.code()));

                    main.smoothScrollTo(0, 0);
                }
            }

            @Override
            public void onFailure(@NonNull Call<User> call, @NonNull Throwable t) {
                progress.setVisibility(View.GONE);
                setFieldsEnabled(true);

                t.printStackTrace();

                error.setVisibility(View.VISIBLE);
                error.setText(ErrorUtil.getErrorMessage(getContext(), null));

                main.smoothScrollTo(0, 0);
            }
        });
    }

    private void logout() {
        error.setVisibility(View.GONE);
        progress.setVisibility(View.VISIBLE);
        setFieldsEnabled(false);

        String token = notificationSettings.getString("token", "");
        User toUpdate = new User();
        toUpdate.setFcmTokens(new ArrayList<>(user.getFcmTokens()));
        if(toUpdate.getFcmTokens().contains(token)) {
            toUpdate.getFcmTokens().remove(token);

            Call<User> call = APIHelper.getInstance().updateUser(user.getApikey(), user.getId(), toUpdate);
            call.enqueue(new Callback<User>() {
                @Override
                public void onResponse(@NonNull Call<User> call, @NonNull Response<User> response) {
                    progress.setVisibility(View.GONE);
                    setFieldsEnabled(true);

                    if(response.code() == 200) {
                        logoutLocal();
                    } else {
                        error.setVisibility(View.VISIBLE);
                        error.setText(ErrorUtil.getErrorMessage(getContext(), response));

                        main.smoothScrollTo(0, 0);
                    }
                }

                @Override
                public void onFailure(@NonNull Call<User> call, @NonNull Throwable t) {
                    progress.setVisibility(View.GONE);
                    setFieldsEnabled(true);

                    t.printStackTrace();

                    error.setVisibility(View.VISIBLE);
                    error.setText(ErrorUtil.getErrorMessage(getContext(), null));

                    main.smoothScrollTo(0, 0);
                }
            });
        } else {
            logoutLocal();
        }
    }

    private void logoutLocal() {
        boolean isThisUser = user.getId().equals(settings.getString("userID", ""));
        if(isThisUser) {
            settings.edit().putString("userID", "").putString("userAPIKey", "").apply();
        }

        AsyncTask.execute(() -> {
            AppDatabase.getInstance(getContext()).users().delete(user);
            NotificationUtil.cleanupPreferencesForUser(getContext(), user.getId());

            if(getActivity() != null && !getActivity().isFinishing()) {
                getActivity().runOnUiThread(() -> ((SettingsActivity) getActivity()).hideUserWithMessage(isThisUser,
                        user.getId(),
                        getString(R.string.user_settings_security_logout_success))
                );
            }
        });
    }

    private void delete() {
        if(getContext() != null) {
            KeyguardManager keyguardManager = (KeyguardManager) getContext().getSystemService(Context.KEYGUARD_SERVICE);
            if(keyguardManager != null) {
                Intent authenticationIntent = keyguardManager.createConfirmDeviceCredentialIntent(null, null);
                if(authenticationIntent != null) {
                    startActivityForResult(authenticationIntent, INTENT_AUTHENTICATE_DELETE);
                } else {
                    doDelete();
                }
            } else {
                doDelete();
            }
        }
    }

    private void doDelete() {
        error.setVisibility(View.GONE);

        new AlertDialog.Builder(getContext()).setMessage(R.string.user_settings_security_delete_confirm).setPositiveButton(R.string.yes, (dialogInterface, i) -> {
            progress.setVisibility(View.VISIBLE);
            setFieldsEnabled(false);

            final boolean isThisUser = user.getId().equals(settings.getString("userID", ""));

            Call<ResponseBody> call = APIHelper.getInstance().deleteUser(user.getApikey(), user.getId());
            call.enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                    progress.setVisibility(View.GONE);
                    setFieldsEnabled(true);

                    if(response.code() == 200 || response.code() == 401) {
                        // 200: OK
                        // 401: Unauthorized, but because API keys don't change it seems the user was already deleted
                        if(user.getId().equals(settings.getString("userID", ""))) {
                            settings.edit().putString("userID", "").putString("userAPIKey", "").apply();
                        }
                        AsyncTask.execute(() -> {
                            AppDatabase.getInstance(getContext()).users().delete(user);
                            NotificationUtil.cleanupPreferencesForUser(getContext(), user.getId());

                            if(getActivity() != null && !getActivity().isFinishing()) {
                                getActivity().runOnUiThread(() -> ((SettingsActivity) getActivity()).hideUserWithMessage(isThisUser,
                                        user.getId(),
                                        getString(R.string.user_settings_security_delete_success))
                                );
                            }
                        });
                    } else {
                        error.setVisibility(View.VISIBLE);
                        error.setText(getString(R.string.error_general_server, "N" + response.code()));

                        main.smoothScrollTo(0, 0);
                    }
                }

                @Override
                public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                    progress.setVisibility(View.GONE);
                    setFieldsEnabled(true);

                    t.printStackTrace();

                    error.setVisibility(View.VISIBLE);
                    error.setText(ErrorUtil.getErrorMessage(getContext(), null));

                    main.smoothScrollTo(0, 0);
                }
            });
        }).setNegativeButton(R.string.no, null).show();
    }

    private void verifyFCMToken() {
        FirebaseInstanceId.getInstance().getInstanceId().addOnCompleteListener(task -> {
            if(task.isSuccessful() && task.getResult() != null) {
                String storedToken = notificationSettings.getString("token", "");
                String receivedToken = task.getResult().getToken();
                if(!storedToken.equals(receivedToken)) {
                    WorkManager.getInstance().cancelAllWorkByTag("fcmRefresh");
                    WorkManager.getInstance().enqueue(FcmRefreshWorker.getRequestToUpdateImmediately(receivedToken, null));
                }
            }
        });
    }

    private void setFieldsEnabled(boolean enabled) {
        accountSwitch.setClickable(enabled);
        accountName.setClickable(enabled);
        accountPassword.setClickable(enabled);
        accountDelete.setClickable(enabled);
        accountLogout.setClickable(enabled);

        notificationsPush.setClickable(enabled);
        notificationsPushReset.setClickable(enabled);
        notificationsPushSystem.setClickable(enabled);
        notificationsPushHeadsup.setEnabled(enabled);
        notificationsPushSound.setEnabled(enabled);
        notificationsPushVibrate.setEnabled(enabled);
        notificationsPushLights.setEnabled(enabled);
        notificationsEmail.setClickable(enabled);
        notificationsEmailAddress.setClickable(enabled);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch(requestCode) {
            case INTENT_AUTHENTICATE_PASSWORD:
            case INTENT_AUTHENTICATE_DELETE:
                if(resultCode == RESULT_OK) {
                    if(requestCode == INTENT_AUTHENTICATE_PASSWORD) {
                        editDetail(SettingsAccountUpdateFragment.UpdateMode.PASSWORD);
                    } else {
                        doDelete();
                    }
                } else {
                    Snackbar.make(coordinator, R.string.user_settings_security_authenticate, Snackbar.LENGTH_LONG).show();
                }
                break;
            default:
                super.onActivityResult(requestCode, resultCode, data);
                break;
        }
    }

    private interface OnUpdatedListener {
        void onResult(boolean success);
    }
}
