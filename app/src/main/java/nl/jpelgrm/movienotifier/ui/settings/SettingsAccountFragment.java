package nl.jpelgrm.movienotifier.ui.settings;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import nl.jpelgrm.movienotifier.R;
import nl.jpelgrm.movienotifier.data.APIHelper;
import nl.jpelgrm.movienotifier.data.DBHelper;
import nl.jpelgrm.movienotifier.models.NotificationType;
import nl.jpelgrm.movienotifier.models.User;
import nl.jpelgrm.movienotifier.models.error.Errors;
import nl.jpelgrm.movienotifier.models.error.Message;
import nl.jpelgrm.movienotifier.ui.view.DoubleRowIconPreferenceView;
import nl.jpelgrm.movienotifier.ui.view.NotificationTypeSettingView;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SettingsAccountFragment extends Fragment {
    @BindView(R.id.accountCoordinator) CoordinatorLayout coordinator;

    @BindView(R.id.progress) ProgressBar progress;

    @BindView(R.id.accountSwitch) LinearLayout accountSwitch;
    @BindView(R.id.accountName) DoubleRowIconPreferenceView accountName;
    @BindView(R.id.accountEmail) DoubleRowIconPreferenceView accountEmail;
    @BindView(R.id.accountPhone) DoubleRowIconPreferenceView accountPhone;
    @BindView(R.id.accountPassword) LinearLayout accountPassword;
    @BindView(R.id.accountLogout) LinearLayout accountLogout;
    @BindView(R.id.accountDelete) LinearLayout accountDelete;

    @BindView(R.id.notifications) LinearLayout notificationsWrapper;
    @BindView(R.id.notificationsEmpty) TextView notificationsEmpty;
    @BindView(R.id.notificationTypes) LinearLayout notificationsList;

    private User user;
    private String id;

    private List<NotificationType> typeList = new ArrayList<>();

    private SharedPreferences settings;

    public static SettingsAccountFragment newInstance(String id) {
        SettingsAccountFragment fragment = new SettingsAccountFragment();
        Bundle args = new Bundle();
        args.putString("id", id);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        id = getArguments().getString("id");
        user = DBHelper.getInstance(getContext()).getUserByID(id);

        settings = getContext().getSharedPreferences("settings", Context.MODE_PRIVATE);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings_account, container, false);
        ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        updateValues();
        getNotificationTypes();

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
        accountPhone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                editDetail(SettingsAccountUpdateFragment.UpdateMode.PHONE);
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
    }

    public void updatedUser() {
        user = DBHelper.getInstance(getContext()).getUserByID(id);
        updateNotificationsList(); // 'Reset', otherwise the calls for clicks will be triggered
        updateValues();
    }

    private void updateValues() {
        accountSwitch.setVisibility(user.getID().equals(settings.getString("userID", "")) ? View.GONE : View.VISIBLE);
        accountName.setValue(user.getName());
        accountEmail.setValue(user.getEmail());
        accountPhone.setValue(user.getPhonenumber());

        for(int i = 0; i < notificationsList.getChildCount(); i++) {
            View view = notificationsList.getChildAt(i);
            if(view instanceof NotificationTypeSettingView) {
                ((NotificationTypeSettingView) view).setValue(user.getNotifications()
                        .contains(((NotificationTypeSettingView) view).getNotificationType().getKey()));
            }
        }
    }

    private void updateNotificationsList() {
        notificationsList.removeAllViews();

        for(int i = 0; i < typeList.size(); i++) {
            NotificationType type = typeList.get(i);
            boolean activated = user != null && user.getNotifications().contains(type.getKey());

            final NotificationTypeSettingView view = new NotificationTypeSettingView(getContext(), activated, type);
            view.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            view.setCheckedChangedListener(new Runnable() {
                @Override
                public void run() {
                    ArrayList<String> enabled = new ArrayList<>();
                    for(int i = 0; i < notificationsList.getChildCount(); i++) {
                        View view = notificationsList.getChildAt(i);
                        if(view instanceof NotificationTypeSettingView) {
                            if(((NotificationTypeSettingView) view).getValue()) {
                                enabled.add(((NotificationTypeSettingView) view).getNotificationType().getKey());
                            }
                        }
                    }

                    if(!enabled.equals(user.getNotifications())) {
                        User toUpdate = new User();
                        toUpdate.setNotifications(enabled);
                        update(toUpdate);
                    }
                }
            });

            notificationsList.addView(view);
        }

        notificationsWrapper.setVisibility(View.VISIBLE);
        notificationsList.setVisibility(typeList.size() > 0 ? View.VISIBLE : View.GONE);
        notificationsEmpty.setVisibility(typeList.size() > 0 ? View.GONE : View.VISIBLE);
    }

    private void editDetail(SettingsAccountUpdateFragment.UpdateMode mode) {
        ((SettingsActivity) getActivity()).editUserDetail(user.getID(), mode);
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

                    Snackbar.make(coordinator, R.string.user_settings_general_success, Snackbar.LENGTH_SHORT).show();
                } else {
                    new AlertDialog.Builder(getContext()).setMessage(getErrorMessage(call, response))
                            .setPositiveButton(R.string.ok, null).show();
                }

                updateValues();
            }

            @Override
            public void onFailure(Call<User> call, Throwable t) {
                t.printStackTrace();

                new AlertDialog.Builder(getContext()).setMessage(R.string.error_general_exception)
                        .setPositiveButton(R.string.ok, null).show();
                updateValues();
            }
        });
    }

    private void switchToThis() {
        progress.setVisibility(View.VISIBLE);
        setFieldsEnabled(false);

        Call<User> call = APIHelper.getInstance().getUser(user.getApikey(), user.getID());
        call.enqueue(new Callback<User>() {
            @Override
            public void onResponse(Call<User> call, Response<User> response) {
                progress.setVisibility(View.GONE);
                setFieldsEnabled(true);

                if(response.code() == 200 && response.body() != null) {
                    User received = response.body();
                    if(received != null) {
                        DBHelper.getInstance(getContext()).updateUser(received);
                        user = received;
                        settings.edit().putString("userID", received.getID()).putString("userAPIKey", received.getApikey()).apply();

                        Snackbar.make(coordinator, R.string.user_settings_general_switch_success, Snackbar.LENGTH_SHORT).show();

                        updateValues();
                    } else {
                        new AlertDialog.Builder(getContext()).setMessage(getContext().getString(R.string.error_general_server, "N200"))
                                .setPositiveButton(R.string.ok, null).show();
                    }
                } else {
                    new AlertDialog.Builder(getContext())
                            .setMessage(getContext().getString(R.string.error_general_server, "N200"))
                            .setPositiveButton(R.string.ok, null)
                            .show();
                }
            }

            @Override
            public void onFailure(Call<User> call, Throwable t) {
                progress.setVisibility(View.GONE);
                setFieldsEnabled(true);

                t.printStackTrace();

                new AlertDialog.Builder(getContext())
                        .setMessage(R.string.error_general_exception)
                        .setPositiveButton(R.string.ok, null)
                        .show();
            }
        });
    }

    private void logout() {
        progress.setVisibility(View.VISIBLE);
        setFieldsEnabled(false);

        boolean isThisUser = user.getID().equals(settings.getString("userID", ""));
        if(isThisUser) {
            settings.edit().putString("userID", "").putString("userAPIKey", "").apply();
        }

        DBHelper.getInstance(getContext()).deleteUser(user.getID());

        if(getActivity() != null && !getActivity().isFinishing()) {
            ((SettingsActivity) getActivity()).hideUserWithMessage(isThisUser, user.getID(), getString(R.string.user_settings_security_logout_success));
        }
    }

    private void delete() {
        new AlertDialog.Builder(getContext()).setMessage(R.string.user_settings_security_delete_confirm).setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                progress.setVisibility(View.VISIBLE);
                setFieldsEnabled(false);

                final boolean isThisUser = user.getID().equals(settings.getString("userID", ""));

                Call<ResponseBody> call = APIHelper.getInstance().deleteUser(user.getApikey(), user.getID());
                call.enqueue(new Callback<ResponseBody>() {
                    @Override
                    public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                        progress.setVisibility(View.GONE);
                        setFieldsEnabled(true);

                        if(response.code() == 200 || response.code() == 401) {
                            // 200: OK
                            // 401: Unauthorized, but because API keys don't change it seems the user was already deleted
                            if(user.getID().equals(settings.getString("userID", ""))) {
                                settings.edit().putString("userID", "").putString("userAPIKey", "").apply();
                            }
                            DBHelper.getInstance(getContext()).deleteUser(user.getID());

                            if(getActivity() != null && !getActivity().isFinishing()) {
                                ((SettingsActivity) getActivity()).hideUserWithMessage(isThisUser, user.getID(), getString(R.string.user_settings_security_delete_success));
                            }
                        } else {
                            new AlertDialog.Builder(getContext())
                                    .setMessage(getContext().getString(R.string.error_general_server, "N200"))
                                    .setPositiveButton(R.string.ok, null)
                                    .show();
                        }
                    }

                    @Override
                    public void onFailure(Call<ResponseBody> call, Throwable t) {
                        progress.setVisibility(View.GONE);
                        setFieldsEnabled(true);

                        t.printStackTrace();

                        new AlertDialog.Builder(getContext())
                                .setMessage(R.string.error_general_exception)
                                .setPositiveButton(R.string.ok, null)
                                .show();
                    }
                });
            }
        }).setNegativeButton(R.string.no, null).show();
    }

    private void setFieldsEnabled(boolean enabled) {
        accountSwitch.setClickable(enabled);
        accountName.setClickable(enabled);
        accountEmail.setClickable(enabled);
        accountPhone.setClickable(enabled);
        accountPassword.setClickable(enabled);
        accountDelete.setClickable(enabled);
        accountLogout.setClickable(enabled);

        for(int i = 0; i < notificationsList.getChildCount(); i++) {
            View view = notificationsList.getChildAt(i);
            if(view instanceof NotificationTypeSettingView) {
                ((NotificationTypeSettingView) view).setEnabledUser(enabled);
            }
        }
    }

    private void getNotificationTypes() {
        Call<List<NotificationType>> call = APIHelper.getInstance().getNotificationTypes(user.getApikey());
        call.enqueue(new Callback<List<NotificationType>>() {
            @Override
            public void onResponse(Call<List<NotificationType>> call, Response<List<NotificationType>> response) {
                if(response.code() == 200) {
                    typeList = response.body();
                    updateNotificationsList();
                } else {
                    notificationsWrapper.setVisibility(user != null ? View.VISIBLE : View.GONE);
                    notificationsList.setVisibility(View.GONE);
                    notificationsEmpty.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onFailure(Call<List<NotificationType>> call, Throwable t) {
                t.printStackTrace();

                notificationsWrapper.setVisibility(user != null ? View.VISIBLE : View.GONE);
                notificationsList.setVisibility(View.GONE);
                notificationsEmpty.setVisibility(View.VISIBLE);
            }
        });
    }

    private String getErrorMessage(Call<User> call, Response<User> response) {
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
                    errorBuilder.append(getContext().getString(R.string.error_general_server, "I400"));
                }
            } else {
                errorBuilder.append(getContext().getString(R.string.error_general_server, "N400"));
            }

            return errorBuilder.toString();
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

            return getContext().getString(R.string.error_general_message, errorBuilder.toString());
        } else {
            return getContext().getString(R.string.error_general_server, "H" + response.code());
        }
    }
}
