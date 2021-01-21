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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.Fragment;
import androidx.work.WorkManager;

import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.ArrayList;
import java.util.Collections;

import nl.jpelgrm.movienotifier.BuildConfig;
import nl.jpelgrm.movienotifier.R;
import nl.jpelgrm.movienotifier.data.APIHelper;
import nl.jpelgrm.movienotifier.data.AppDatabase;
import nl.jpelgrm.movienotifier.databinding.FragmentSettingsAccountOverviewBinding;
import nl.jpelgrm.movienotifier.models.User;
import nl.jpelgrm.movienotifier.service.FcmRefreshWorker;
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

    private FragmentSettingsAccountOverviewBinding binding;

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
        binding = FragmentSettingsAccountOverviewBinding.inflate(inflater, container, false);
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
            if(user != null) {
                this.user = user;
                updateValues();
            }
        });

        binding.accountSwitch.setOnClickListener(v -> switchToThis());
        binding.accountName.setOnClickListener(v -> editDetail(SettingsAccountUpdateFragment.UpdateMode.NAME));
        binding.accountPassword.setOnClickListener(v -> editPassword());
        binding.accountLogout.setOnClickListener(v -> logout());
        binding.accountDelete.setOnClickListener(v -> delete());
        binding.accountSwitch.setVisibility(isCurrentUser ? View.GONE : View.VISIBLE);

        binding.notificationsPush.setOnSwitchClickListener(v -> togglePushNotifications());
        binding.notificationsPushReset.setOnClickListener(v -> resetPushNotifications());
        binding.notificationsPushSystem.setOnClickListener(v -> {
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationUtil.createChannelWatchersPush(getContext(), user);
                Intent intent = new Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
                        .putExtra(Settings.EXTRA_APP_PACKAGE, BuildConfig.APPLICATION_ID)
                        .putExtra(Settings.EXTRA_CHANNEL_ID, NotificationUtil.NOTIFICATION_CHANNEL_PREFIX + user.getId() + NotificationUtil.NOTIFICATION_CHANNEL_WATCHERS_PUSH);
                startActivity(intent);
            }
        });
        binding.notificationsPushHeadsup.setOnCheckedChangeListener((v, isChecked) -> {
            if(user == null) { return; }
            notificationSettings.edit().putBoolean("headsup-" + user.getId(), isChecked).apply();
        });
        binding.notificationsPushSound.setOnCheckedChangeListener((v, isChecked) -> {
            if(user == null) { return; }
            notificationSettings.edit().putBoolean("sound-" + user.getId(), isChecked).apply();
            updateValues();
        });
        binding.notificationsPushVibrate.setOnCheckedChangeListener((v, isChecked) -> {
            if(user == null) { return; }
            notificationSettings.edit().putBoolean("vibrate-" + user.getId(), isChecked).apply();
            updateValues();
        });
        binding.notificationsPushLights.setOnCheckedChangeListener((v, isChecked) -> {
            if(user == null) { return; }
            notificationSettings.edit().putBoolean("lights-" + user.getId(), isChecked).apply();
        });
        binding.notificationsEmail.setOnSwitchClickListener(v -> toggleEmailNotifications());
        binding.notificationsEmailAddress.setOnClickListener(v -> editDetail(SettingsAccountUpdateFragment.UpdateMode.EMAIL));
    }

    void updatedUser() {
        if(binding != null) {
            binding.error.setVisibility(View.GONE);
            // UI updates are triggered via LiveData which will detect a change
        }
    }

    private void updateValues() {
        binding.accountSwitch.setVisibility(user.getId().equals(settings.getString("userID", "")) ? View.GONE : View.VISIBLE);
        binding.accountName.setValue(user.getName());
        binding.notificationsPush.setChecked(user.getFcmTokens().contains(notificationSettings.getString("token", "")));
        binding.notificationsPushReset.setVisibility(BuildConfig.DEBUG && user.getFcmTokens().size() > 0 ? View.VISIBLE : View.GONE);
        binding.notificationsPushSystem.setVisibility(binding.notificationsPush.isChecked() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ? View.VISIBLE : View.GONE);
        binding.notificationsPushSound.setVisibility(binding.notificationsPush.isChecked() && Build.VERSION.SDK_INT < Build.VERSION_CODES.O ? View.VISIBLE : View.GONE);
        binding.notificationsPushSound.setChecked(notificationSettings.getBoolean("sound-" + user.getId(), true));
        binding.notificationsPushVibrate.setVisibility(binding.notificationsPush.isChecked() && Build.VERSION.SDK_INT < Build.VERSION_CODES.O ? View.VISIBLE : View.GONE);
        binding.notificationsPushVibrate.setChecked(notificationSettings.getBoolean("vibrate-" + user.getId(), true));
        binding.notificationsPushLights.setVisibility(binding.notificationsPush.isChecked() && Build.VERSION.SDK_INT < Build.VERSION_CODES.O ? View.VISIBLE : View.GONE);
        binding.notificationsPushLights.setChecked(notificationSettings.getBoolean("lights-" + user.getId(), true));
        binding.notificationsPushHeadsup.setVisibility(binding.notificationsPush.isChecked() && (binding.notificationsPushSound.isChecked() || binding.notificationsPushVibrate.isChecked())
                && Build.VERSION.SDK_INT < Build.VERSION_CODES.O ? View.VISIBLE : View.GONE);
        binding.notificationsPushHeadsup.setChecked(notificationSettings.getBoolean("headsup-" + user.getId(), true));
        binding.notificationsEmail.setChecked(!user.getEmail().equals(""));
        binding.notificationsEmailAddress.setVisibility(!user.getEmail().equals("") ? View.VISIBLE : View.GONE);
        binding.notificationsEmailAddress.setValue(user.getEmail());
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
        if(user == null) { return; }
        String token = notificationSettings.getString("token", "");
        User toUpdate = new User();
        toUpdate.setId(user.getId());
        toUpdate.setFcmTokens(new ArrayList<>(user.getFcmTokens()));
        boolean changed = false;
        boolean setToEnabled = binding.notificationsPush.isChecked();
        if(setToEnabled) {
            if(!toUpdate.getFcmTokens().contains(token)) {
                if(!token.equals("")) {
                    changed = toUpdate.getFcmTokens().add(token);
                } else {
                    binding.notificationsPush.setChecked(false);
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
        toUpdate.setId(user.getId());
        toUpdate.setFcmTokens(Collections.emptyList());
        update(toUpdate, success -> {
            if(success) {
                notificationSettings.edit().putBoolean("disabled-" + user.getId(), true).apply();
            }
        });
    }

    private void toggleEmailNotifications() {
        if(user == null) { return; }
        if(binding.notificationsEmail.isChecked()) {
            binding.notificationsEmail.setChecked(false);
            editDetail(SettingsAccountUpdateFragment.UpdateMode.EMAIL);
        } else {
            User toUpdate = new User();
            toUpdate.setId(user.getId());
            toUpdate.setEmail("");
            update(toUpdate, null);
        }
    }

    private void update(User toUpdate, @Nullable OnUpdatedListener listener) {
        binding.error.setVisibility(View.GONE);
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
                    user = received;

                    Snackbar.make(binding.accountCoordinator, R.string.user_settings_general_success, Snackbar.LENGTH_SHORT).show();
                } else {
                    binding.error.setVisibility(View.VISIBLE);
                    binding.error.setText(ErrorUtil.getErrorMessage(getContext(), response));

                    binding.main.smoothScrollTo(0, 0);
                }

                updateValues();
                if(listener != null) {
                    listener.onResult(response.code() == 200);
                }
            }

            @Override
            public void onFailure(@NonNull Call<User> call, @NonNull Throwable t) {
                binding.progress.setVisibility(View.GONE);
                setFieldsEnabled(true);

                t.printStackTrace();

                binding.error.setVisibility(View.VISIBLE);
                binding.error.setText(ErrorUtil.getErrorMessage(getContext(), null));

                binding.main.smoothScrollTo(0, 0);

                updateValues();
                if(listener != null) {
                    listener.onResult(false);
                }
            }
        });
    }

    private void switchToThis() {
        binding.error.setVisibility(View.GONE);
        binding.progress.setVisibility(View.VISIBLE);
        setFieldsEnabled(false);

        Call<User> call = APIHelper.INSTANCE.getInstance().getUser(user.getApikey(), user.getId());
        call.enqueue(new Callback<User>() {
            @Override
            public void onResponse(@NonNull Call<User> call, @NonNull Response<User> response) {
                binding.progress.setVisibility(View.GONE);
                setFieldsEnabled(true);

                if(response.code() == 200 && response.body() != null) {
                    User received = response.body();
                    if(received != null) {
                        AsyncTask.execute(() -> AppDatabase.Companion.getInstance(getContext()).users().update(received));
                        if(!user.getName().equals(received.getName())) {
                            NotificationUtil.createUserGroup(getContext(), received);
                        }
                        user = received;
                        isCurrentUser = true;
                        settings.edit().putString("userID", received.getId()).putString("userAPIKey", received.getApikey()).apply();

                        Snackbar.make(binding.accountCoordinator, R.string.user_settings_general_switch_success, Snackbar.LENGTH_SHORT).show();

                        updateValues();
                    } else {
                        binding.error.setVisibility(View.VISIBLE);
                        binding.error.setText(getString(R.string.error_general_server, "N200"));

                        binding.main.smoothScrollTo(0, 0);
                    }
                } else {
                    binding.error.setVisibility(View.VISIBLE);
                    binding.error.setText(getString(R.string.error_general_server, "N" + response.code()));

                    binding.main.smoothScrollTo(0, 0);
                }
            }

            @Override
            public void onFailure(@NonNull Call<User> call, @NonNull Throwable t) {
                binding.progress.setVisibility(View.GONE);
                setFieldsEnabled(true);

                t.printStackTrace();

                binding.error.setVisibility(View.VISIBLE);
                binding.error.setText(ErrorUtil.getErrorMessage(getContext(), null));

                binding.main.smoothScrollTo(0, 0);
            }
        });
    }

    private void logout() {
        binding.error.setVisibility(View.GONE);
        binding.progress.setVisibility(View.VISIBLE);
        setFieldsEnabled(false);

        String token = notificationSettings.getString("token", "");
        User toUpdate = new User();
        toUpdate.setId(user.getId());
        toUpdate.setFcmTokens(new ArrayList<>(user.getFcmTokens()));
        if(toUpdate.getFcmTokens().contains(token)) {
            toUpdate.getFcmTokens().remove(token);

            Call<User> call = APIHelper.INSTANCE.getInstance().updateUser(user.getApikey(), user.getId(), toUpdate);
            call.enqueue(new Callback<User>() {
                @Override
                public void onResponse(@NonNull Call<User> call, @NonNull Response<User> response) {
                    binding.progress.setVisibility(View.GONE);
                    setFieldsEnabled(true);

                    if(response.code() == 200) {
                        logoutLocal();
                    } else {
                        binding.error.setVisibility(View.VISIBLE);
                        binding.error.setText(ErrorUtil.getErrorMessage(getContext(), response));

                        binding.main.smoothScrollTo(0, 0);
                    }
                }

                @Override
                public void onFailure(@NonNull Call<User> call, @NonNull Throwable t) {
                    binding.progress.setVisibility(View.GONE);
                    setFieldsEnabled(true);

                    t.printStackTrace();

                    binding.error.setVisibility(View.VISIBLE);
                    binding.error.setText(ErrorUtil.getErrorMessage(getContext(), null));

                    binding.main.smoothScrollTo(0, 0);
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
            AppDatabase.Companion.getInstance(getContext()).users().delete(user);
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
        binding.error.setVisibility(View.GONE);

        new AlertDialog.Builder(getContext()).setMessage(R.string.user_settings_security_delete_confirm).setPositiveButton(R.string.yes, (dialogInterface, i) -> {
            binding.progress.setVisibility(View.VISIBLE);
            setFieldsEnabled(false);

            final boolean isThisUser = user.getId().equals(settings.getString("userID", ""));

            Call<ResponseBody> call = APIHelper.INSTANCE.getInstance().deleteUser(user.getApikey(), user.getId());
            call.enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                    binding.progress.setVisibility(View.GONE);
                    setFieldsEnabled(true);

                    if(response.code() == 200 || response.code() == 401) {
                        // 200: OK
                        // 401: Unauthorized, but because API keys don't change it seems the user was already deleted
                        if(user.getId().equals(settings.getString("userID", ""))) {
                            settings.edit().putString("userID", "").putString("userAPIKey", "").apply();
                        }
                        AsyncTask.execute(() -> {
                            AppDatabase.Companion.getInstance(getContext()).users().delete(user);
                            NotificationUtil.cleanupPreferencesForUser(getContext(), user.getId());

                            if(getActivity() != null && !getActivity().isFinishing()) {
                                getActivity().runOnUiThread(() -> ((SettingsActivity) getActivity()).hideUserWithMessage(isThisUser,
                                        user.getId(),
                                        getString(R.string.user_settings_security_delete_success))
                                );
                            }
                        });
                    } else {
                        binding.error.setVisibility(View.VISIBLE);
                        binding.error.setText(getString(R.string.error_general_server, "N" + response.code()));

                        binding.main.smoothScrollTo(0, 0);
                    }
                }

                @Override
                public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                    binding.progress.setVisibility(View.GONE);
                    setFieldsEnabled(true);

                    t.printStackTrace();

                    binding.error.setVisibility(View.VISIBLE);
                    binding.error.setText(ErrorUtil.getErrorMessage(getContext(), null));

                    binding.main.smoothScrollTo(0, 0);
                }
            });
        }).setNegativeButton(R.string.no, null).show();
    }

    private void verifyFCMToken() {
        FirebaseMessaging.getInstance().getToken().addOnCompleteListener(task -> {
            if(task.isSuccessful() && task.getResult() != null) {
                String storedToken = notificationSettings.getString("token", "");
                String receivedToken = task.getResult();
                if(!storedToken.equals(receivedToken) && getContext() != null) {
                    WorkManager.getInstance(getContext()).cancelAllWorkByTag("fcmRefresh");
                    WorkManager.getInstance(getContext()).enqueue(FcmRefreshWorker.getRequestToUpdateImmediately(receivedToken, null));
                }
            }
        });
    }

    private void setFieldsEnabled(boolean enabled) {
        binding.accountSwitch.setClickable(enabled);
        binding.accountName.setClickable(enabled);
        binding.accountPassword.setClickable(enabled);
        binding.accountDelete.setClickable(enabled);
        binding.accountLogout.setClickable(enabled);

        binding.notificationsPush.setClickable(enabled);
        binding.notificationsPushReset.setClickable(enabled);
        binding.notificationsPushSystem.setClickable(enabled);
        binding.notificationsPushHeadsup.setEnabled(enabled);
        binding.notificationsPushSound.setEnabled(enabled);
        binding.notificationsPushVibrate.setEnabled(enabled);
        binding.notificationsPushLights.setEnabled(enabled);
        binding.notificationsEmail.setClickable(enabled);
        binding.notificationsEmailAddress.setClickable(enabled);
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
                    Snackbar.make(binding.accountCoordinator, R.string.user_settings_security_authenticate, Snackbar.LENGTH_LONG).show();
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
