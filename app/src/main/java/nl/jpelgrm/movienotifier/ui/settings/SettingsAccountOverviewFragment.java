package nl.jpelgrm.movienotifier.ui.settings;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import com.google.android.material.snackbar.Snackbar;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.fragment.app.Fragment;
import butterknife.BindView;
import butterknife.ButterKnife;
import nl.jpelgrm.movienotifier.R;
import nl.jpelgrm.movienotifier.data.APIHelper;
import nl.jpelgrm.movienotifier.data.AppDatabase;
import nl.jpelgrm.movienotifier.models.User;
import nl.jpelgrm.movienotifier.ui.view.DoubleRowIconPreferenceView;
import nl.jpelgrm.movienotifier.util.ErrorUtil;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SettingsAccountOverviewFragment extends Fragment {
    @BindView(R.id.accountCoordinator) CoordinatorLayout coordinator;

    @BindView(R.id.progress) ProgressBar progress;
    @BindView(R.id.main) ScrollView main;
    @BindView(R.id.error) TextView error;

    @BindView(R.id.accountSwitch) LinearLayout accountSwitch;
    @BindView(R.id.accountName) DoubleRowIconPreferenceView accountName;
    @BindView(R.id.accountEmail) DoubleRowIconPreferenceView accountEmail;
    @BindView(R.id.accountPassword) LinearLayout accountPassword;
    @BindView(R.id.accountLogout) LinearLayout accountLogout;
    @BindView(R.id.accountDelete) LinearLayout accountDelete;

    private User user;
    private String id;
    private boolean isCurrentUser;

    private SharedPreferences settings;

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
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings_account_overview, container, false);
        ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        AppDatabase.getInstance(getContext()).users().getUserById(id).observe(this, user -> {
            if(user != null) {
                this.user = user;
                updateValues();
            }
        });

        accountSwitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                switchToThis();
            }
        });
        accountName.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                editDetail(SettingsAccountUpdateFragment.UpdateMode.NAME);
            }
        });
        accountEmail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                editDetail(SettingsAccountUpdateFragment.UpdateMode.EMAIL);
            }
        });
        accountPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                editDetail(SettingsAccountUpdateFragment.UpdateMode.PASSWORD);
            }
        });
        accountLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                logout();
            }
        });
        accountDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                delete();
            }
        });
        accountSwitch.setVisibility(isCurrentUser ? View.GONE : View.VISIBLE);
    }

    public void updatedUser() {
        error.setVisibility(View.GONE);
        // UI updates are triggered via LiveData which will detect a change
    }

    private void updateValues() {
        accountSwitch.setVisibility(user.getId().equals(settings.getString("userID", "")) ? View.GONE : View.VISIBLE);
        accountName.setValue(user.getName());
        accountEmail.setValue(user.getEmail());
    }

    private void editDetail(SettingsAccountUpdateFragment.UpdateMode mode) {
        ((SettingsActivity) getActivity()).editUserDetail(user.getId(), mode);
    }

    private void update(User toUpdate) {
        error.setVisibility(View.GONE);
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

                    Snackbar.make(coordinator, R.string.user_settings_general_success, Snackbar.LENGTH_SHORT).show();
                } else {
                    error.setVisibility(View.VISIBLE);
                    error.setText(ErrorUtil.getErrorMessage(getContext(), response));

                    main.smoothScrollTo(0, 0);
                }

                updateValues();
            }

            @Override
            public void onFailure(Call<User> call, Throwable t) {
                t.printStackTrace();

                error.setVisibility(View.VISIBLE);
                error.setText(ErrorUtil.getErrorMessage(getContext(), null));

                main.smoothScrollTo(0, 0);

                updateValues();
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
            public void onResponse(Call<User> call, Response<User> response) {
                progress.setVisibility(View.GONE);
                setFieldsEnabled(true);

                if(response.code() == 200 && response.body() != null) {
                    User received = response.body();
                    if(received != null) {
                        AsyncTask.execute(() -> AppDatabase.getInstance(getContext()).users().update(received));
                        user = received;
                        isCurrentUser = true;
                        settings.edit().putString("userID", received.getId()).putString("userAPIKey", received.getApikey()).apply();

                        Snackbar.make(coordinator, R.string.user_settings_general_switch_success, Snackbar.LENGTH_SHORT).show();

                        updateValues();
                    } else {
                        error.setVisibility(View.VISIBLE);
                        error.setText(getContext().getString(R.string.error_general_server, "N200"));

                        main.smoothScrollTo(0, 0);
                    }
                } else {
                    error.setVisibility(View.VISIBLE);
                    error.setText(getContext().getString(R.string.error_general_server, "N" + response.code()));

                    main.smoothScrollTo(0, 0);
                }
            }

            @Override
            public void onFailure(Call<User> call, Throwable t) {
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
        progress.setVisibility(View.VISIBLE);
        setFieldsEnabled(false);

        boolean isThisUser = user.getId().equals(settings.getString("userID", ""));
        if(isThisUser) {
            settings.edit().putString("userID", "").putString("userAPIKey", "").apply();
        }

        AsyncTask.execute(() -> {
            AppDatabase.getInstance(getContext()).users().delete(user);

            if(getActivity() != null && !getActivity().isFinishing()) {
                getActivity().runOnUiThread(() -> ((SettingsActivity) getActivity()).hideUserWithMessage(isThisUser,
                        user.getId(),
                        getString(R.string.user_settings_security_logout_success))
                );
            }
        });
    }

    private void delete() {
        error.setVisibility(View.GONE);

        new AlertDialog.Builder(getContext()).setMessage(R.string.user_settings_security_delete_confirm).setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                progress.setVisibility(View.VISIBLE);
                setFieldsEnabled(false);

                final boolean isThisUser = user.getId().equals(settings.getString("userID", ""));

                Call<ResponseBody> call = APIHelper.getInstance().deleteUser(user.getApikey(), user.getId());
                call.enqueue(new Callback<ResponseBody>() {
                    @Override
                    public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
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

                                if(getActivity() != null && !getActivity().isFinishing()) {
                                    getActivity().runOnUiThread(() -> ((SettingsActivity) getActivity()).hideUserWithMessage(isThisUser,
                                            user.getId(),
                                            getString(R.string.user_settings_security_delete_success))
                                    );
                                }
                            });
                        } else {
                            error.setVisibility(View.VISIBLE);
                            error.setText(getContext().getString(R.string.error_general_server, "N" + response.code()));

                            main.smoothScrollTo(0, 0);
                        }
                    }

                    @Override
                    public void onFailure(Call<ResponseBody> call, Throwable t) {
                        progress.setVisibility(View.GONE);
                        setFieldsEnabled(true);

                        t.printStackTrace();

                        error.setVisibility(View.VISIBLE);
                        error.setText(ErrorUtil.getErrorMessage(getContext(), null));

                        main.smoothScrollTo(0, 0);
                    }
                });
            }
        }).setNegativeButton(R.string.no, null).show();
    }

    private void setFieldsEnabled(boolean enabled) {
        accountSwitch.setClickable(enabled);
        accountName.setClickable(enabled);
        accountEmail.setClickable(enabled);
        accountPassword.setClickable(enabled);
        accountDelete.setClickable(enabled);
        accountLogout.setClickable(enabled);
    }
}
