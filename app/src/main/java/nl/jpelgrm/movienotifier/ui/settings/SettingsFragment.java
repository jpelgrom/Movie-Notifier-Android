package nl.jpelgrm.movienotifier.ui.settings;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatDelegate;
import android.support.v7.widget.AppCompatImageView;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import nl.jpelgrm.movienotifier.R;
import nl.jpelgrm.movienotifier.data.APIHelper;
import nl.jpelgrm.movienotifier.data.DBHelper;
import nl.jpelgrm.movienotifier.models.User;
import nl.jpelgrm.movienotifier.ui.adapter.AccountsAdapter;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SettingsFragment extends Fragment {
    @BindView(R.id.dayNight) RelativeLayout dayNight;
    @BindView(R.id.dayNightIcon) AppCompatImageView dayNightIcon;
    @BindView(R.id.dayNightValue) TextView dayNightValue;

    @BindView(R.id.accountsRecycler) public RecyclerView accountsRecycler;
    @BindView(R.id.accountFlow) LinearLayout addAccount;

    private ArrayList<User> users = new ArrayList<>();
    private AccountsAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);
        ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        updateValues();
        dayNight.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                CharSequence[] items = { getString(R.string.settings_general_daynight_auto), getString(R.string.settings_general_daynight_day),
                        getString(R.string.settings_general_daynight_night) };
                int currentValueIndex = getContext().getSharedPreferences("settings", Context.MODE_PRIVATE).getInt("prefDayNight", AppCompatDelegate.MODE_NIGHT_AUTO);

                new AlertDialog.Builder(getContext()).setTitle(R.string.settings_general_daynight_title).setSingleChoiceItems(items, currentValueIndex, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int which) {
                        dialogInterface.dismiss();
                        setDayNight(which);
                    }
                }).setNegativeButton(R.string.cancel, null).show();
            }
        });

        adapter = new AccountsAdapter(getContext(), users);
        accountsRecycler.setAdapter(adapter);
        accountsRecycler.setLayoutManager(new LinearLayoutManager(getContext()));
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(accountsRecycler.getContext(), LinearLayoutManager.VERTICAL);
        accountsRecycler.addItemDecoration(dividerItemDecoration);

        adapter.swapItems(DBHelper.getInstance(getContext()).getUsers());
        updateAccountsList();

        addAccount.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(getActivity(), AccountActivity.class));
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        adapter.swapItems(DBHelper.getInstance(getContext()).getUsers());
    }

    private void updateValues() {
        int dayNightPreference = getContext().getSharedPreferences("settings", Context.MODE_PRIVATE).getInt("prefDayNight", AppCompatDelegate.MODE_NIGHT_AUTO);
        switch(dayNightPreference) {
            case AppCompatDelegate.MODE_NIGHT_NO:
                dayNightIcon.setImageResource(R.drawable.ic_brightness_day);
                dayNightValue.setText(R.string.settings_general_daynight_day);
                break;
            case AppCompatDelegate.MODE_NIGHT_YES:
                dayNightIcon.setImageResource(R.drawable.ic_brightness_night);
                dayNightValue.setText(R.string.settings_general_daynight_night);
                break;
            case AppCompatDelegate.MODE_NIGHT_AUTO:
            default:
                dayNightIcon.setImageResource(R.drawable.ic_brightness_auto);
                dayNightValue.setText(R.string.settings_general_daynight_auto);
                break;
        }
    }

    private void updateAccountsList() {
        final DBHelper db = DBHelper.getInstance(getContext());
        final List<User> active = db.getUsers();

        for(final User user : active) {
            Call<User> call = APIHelper.getInstance().getUser(user.getApikey(), user.getID());
            call.enqueue(new Callback<User>() {
                @Override
                public void onResponse(Call<User> call, Response<User> response) {
                    if(response.code() == 200) {
                        if(response.body() != null) {
                            db.updateUser(response.body());
                        }
                    } else if(response.code() == 401) {
                        // Authentication failed, which cannot happen unless the user has been deleted, so make sure to delete it here as well
                        db.deleteUser(user.getID());

                        SharedPreferences settings = getContext().getSharedPreferences("settings", Context.MODE_PRIVATE);
                        if(settings.getString("userID", "").equals(user.getID())) {
                            settings.edit().putString("userID", "").putString("userAPIKey", "").apply();
                        }
                    } // else: failed with user facing error, but do nothing because it is a background task

                    adapter.swapItems(db.getUsers());
                }

                @Override
                public void onFailure(Call<User> call, Throwable t) {
                    t.printStackTrace();
                }
            });
        }
    }

    private void setDayNight(int value) {
        SharedPreferences settings = getContext().getSharedPreferences("settings", Context.MODE_PRIVATE);
        int currentValue = settings.getInt("prefDayNight", AppCompatDelegate.MODE_NIGHT_AUTO);

        settings.edit().putInt("prefDayNight", value).apply();
        AppCompatDelegate.setDefaultNightMode(value);

        if(value != currentValue) {
            getActivity().recreate();
        } else {
            updateValues();
        }
    }
}
