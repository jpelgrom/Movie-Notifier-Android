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

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import nl.jpelgrm.movienotifier.R;
import nl.jpelgrm.movienotifier.data.APIHelper;
import nl.jpelgrm.movienotifier.data.DBHelper;
import nl.jpelgrm.movienotifier.models.Cinema;
import nl.jpelgrm.movienotifier.models.User;
import nl.jpelgrm.movienotifier.ui.adapter.AccountsAdapter;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SettingsFragment extends Fragment {
    @BindView(R.id.dayNight) RelativeLayout dayNight;
    @BindView(R.id.dayNightIcon) AppCompatImageView dayNightIcon;
    @BindView(R.id.dayNightValue) TextView dayNightValue;
    @BindView(R.id.location) RelativeLayout location;
    @BindView(R.id.locationValue) TextView locationValue;

    @BindView(R.id.accountsRecycler) public RecyclerView accountsRecycler;
    @BindView(R.id.accountFlow) LinearLayout addAccount;

    private List<Cinema> cinemas = null;
    private CharSequence[] cinemaItems;

    private ArrayList<User> users = new ArrayList<>();
    private AccountsAdapter adapter;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        readCinemasJson();
    }

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
        location.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String currentPreference = getContext().getSharedPreferences("settings", Context.MODE_PRIVATE).getString("prefDefaultCinema", "");
                int currentValueIndex = 0;
                if(cinemas != null) {
                    for(int i = 0; i < cinemas.size(); i++) {
                        if(cinemas.get(i).getId().equals(currentPreference)) {
                            currentValueIndex = i + 1;
                        }
                    }
                }
                if(!currentPreference.equals("") && currentValueIndex == 0) {
                    currentValueIndex = -1; // Don't select anything
                }

                new AlertDialog.Builder(getContext()).setTitle(R.string.settings_general_location_title).setSingleChoiceItems(cinemaItems, currentValueIndex, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int which) {
                        dialogInterface.dismiss();
                        setCinemaPreference(which);
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
        SharedPreferences settings = getContext().getSharedPreferences("settings", Context.MODE_PRIVATE);

        int dayNightPreference = settings.getInt("prefDayNight", AppCompatDelegate.MODE_NIGHT_AUTO);
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

        String locationPreference = settings.getString("prefDefaultCinema", "");
        String locationPrefText = "";
        if(locationPreference.equals("")) {
            locationPrefText = getString(R.string.settings_general_location_default);
        } else {
            locationPrefText = locationPreference;
            if(cinemas != null) {
                for(Cinema cinema : cinemas) {
                    if(cinema.getId().equals(locationPreference)) {
                        locationPrefText = cinema.getName();
                    }
                }
            }
        }
        locationValue.setText(locationPrefText);
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

    private void setCinemaPreference(int value) {
        String chose = cinemaItems[value].toString();
        String setTo = ""; // Default; no preference

        if(!chose.equals(getString(R.string.settings_general_location_default))) {
            if(cinemas != null) {
                for(Cinema cinema : cinemas) {
                    if(cinema.getName().equals(chose)) {
                        setTo = cinema.getId();
                    }
                }
            }
        }

        getContext().getSharedPreferences("settings", Context.MODE_PRIVATE).edit().putString("prefDefaultCinema", setTo).apply();
        updateValues();
    }

    private void readCinemasJson() {
        // Data
        String json = null;
        try {
            InputStream inputStream = getContext().getAssets().open("cinemas.json");
            int size = inputStream.available();
            byte[] buffer = new byte[size];
            inputStream.read(buffer);
            inputStream.close();
            json = new String(buffer, "UTF-8");
        } catch (IOException e) {
            e.printStackTrace();
        }

        Type listType = new TypeToken<List<Cinema>>() {}.getType();
        cinemas = new Gson().fromJson(json, listType);

        // Dialog
        List<String> choices = new ArrayList<>();

        choices.add(getString(R.string.settings_general_location_default));
        if(cinemas != null) {
            for(Cinema cinema : cinemas) {
                choices.add(cinema.getName());
            }
        }
        cinemaItems = choices.toArray(new CharSequence[choices.size()]);
    }
}
