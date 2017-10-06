package nl.jpelgrm.movienotifier.ui.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import nl.jpelgrm.movienotifier.R;
import nl.jpelgrm.movienotifier.data.APIHelper;
import nl.jpelgrm.movienotifier.data.DBHelper;
import nl.jpelgrm.movienotifier.models.NotificationType;
import nl.jpelgrm.movienotifier.models.User;
import nl.jpelgrm.movienotifier.ui.view.NotificationTypeView;
import nl.jpelgrm.movienotifier.util.ErrorUtil;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AccountNotificationsFragment extends Fragment {
    SharedPreferences settings;

    @BindView(R.id.progress) ProgressBar progress;
    @BindView(R.id.error) TextView error;

    @BindView(R.id.types) LinearLayout types;

    @BindView(R.id.go) Button go;

    private List<String> userActivated = new ArrayList<>();
    private List<NotificationType> typeList = new ArrayList<>();

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        settings = getActivity().getSharedPreferences("settings", Context.MODE_PRIVATE);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_account_notifications, container, false);
        ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        User current = DBHelper.getInstance(getContext()).getUserByID(settings.getString("userID", ""));
        if (current != null) {
            userActivated = current.getNotifications();
        }

        getTypes();

        go.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                error.setVisibility(View.GONE);
                save();
            }
        });
    }

    private void getTypes() {
        Call<List<NotificationType>> call = APIHelper.getInstance().getNotificationTypes(settings.getString("userAPIKey", ""));
        call.enqueue(new Callback<List<NotificationType>>() {
            @Override
            public void onResponse(Call<List<NotificationType>> call, Response<List<NotificationType>> response) {
                setFieldsEnabled(true);
                setProgressVisible(false);

                if(response.code() == 200) {
                    typeList = response.body();
                    updateTypesList();

                    error.setVisibility(View.GONE);
                } else {
                    error.setText(ErrorUtil.getErrorMessage(getContext(), response));
                    error.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onFailure(Call<List<NotificationType>> call, Throwable t) {
                t.printStackTrace();

                setFieldsEnabled(true);
                setProgressVisible(false);

                error.setText(ErrorUtil.getErrorMessage(getContext(), null));
                error.setVisibility(View.VISIBLE);
            }
        });
    }

    private void updateTypesList() {
        types.removeAllViews();

        for(int i = 0; i < typeList.size(); i++) {
            NotificationType type = typeList.get(i);
            boolean activated = userActivated.contains(type.getKey());

            NotificationTypeView view = new NotificationTypeView(getContext(), activated, type);
            view.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

            types.addView(view);
        }
    }

    private void save() {
        setFieldsEnabled(false);
        setProgressVisible(true);

        ArrayList<String> enabled = new ArrayList<>();
        for(int i = 0; i < types.getChildCount(); i++) {
            View view = types.getChildAt(i);
            if(view instanceof NotificationTypeView) {
                if(((NotificationTypeView) view).getValue()) {
                    enabled.add(((NotificationTypeView) view).getNotificationType().getKey());
                }
            }
        }

        User toUpdate = new User();
        toUpdate.setNotifications(enabled);
        Call<User> call = APIHelper.getInstance().updateUser(settings.getString("userAPIKey", ""),
                settings.getString("userID", ""), toUpdate);
        call.enqueue(new Callback<User>() {
            @Override
            public void onResponse(Call<User> call, Response<User> response) {
                if(response.code() == 200) {
                    User received = response.body();
                    DBHelper.getInstance(getActivity()).addUser(received);
                    getActivity().finish();
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

                error.setText(ErrorUtil.getErrorMessage(getContext(), null));
                error.setVisibility(View.VISIBLE);
            }
        });
    }

    private void setFieldsEnabled(boolean enabled) {
        for(int i = 0; i < types.getChildCount(); i++) {
            View view = types.getChildAt(i);
            if(view instanceof NotificationTypeView) {
                view.setEnabled(enabled);
            }
        }

        go.setEnabled(enabled);
    }

    private void setProgressVisible(boolean visible) {
        progress.setVisibility(visible ? View.VISIBLE : View.GONE);
    }
}
