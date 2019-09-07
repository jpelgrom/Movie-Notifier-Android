package nl.jpelgrm.movienotifier.ui;

import android.Manifest;
import android.app.DatePickerDialog;
import android.app.NotificationManager;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Patterns;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.appcompat.widget.PopupMenu;
import androidx.appcompat.widget.Toolbar;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.emoji.widget.EmojiAppCompatEditText;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputLayout;

import org.apache.commons.text.WordUtils;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import nl.jpelgrm.movienotifier.BuildConfig;
import nl.jpelgrm.movienotifier.R;
import nl.jpelgrm.movienotifier.data.APIHelper;
import nl.jpelgrm.movienotifier.data.AppDatabase;
import nl.jpelgrm.movienotifier.data.CinemaIDAdapter;
import nl.jpelgrm.movienotifier.models.Cinema;
import nl.jpelgrm.movienotifier.models.Notification;
import nl.jpelgrm.movienotifier.models.Watcher;
import nl.jpelgrm.movienotifier.models.WatcherFilters;
import nl.jpelgrm.movienotifier.ui.settings.AccountActivity;
import nl.jpelgrm.movienotifier.ui.view.DoubleRowIconPreferenceView;
import nl.jpelgrm.movienotifier.ui.view.InstantAutoComplete;
import nl.jpelgrm.movienotifier.util.ErrorUtil;
import nl.jpelgrm.movienotifier.util.InterfaceUtil;
import nl.jpelgrm.movienotifier.util.LocationUtil;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class WatcherActivity extends AppCompatActivity {
    enum Mode {
        VIEWING, EDITING
    }

    private static final int PERMISSION_LOCATION_AUTOCOMPLETE = 151;

    @BindView(R.id.coordinator) CoordinatorLayout coordinator;

    @BindView(R.id.toolbar) Toolbar toolbar;

    @BindView(R.id.progress) ProgressBar progress;
    @BindView(R.id.main) ScrollView main;
    @BindView(R.id.loaderError) LinearLayout loaderError;
    @BindView(R.id.loaderErrorText) TextView loaderErrorText;
    @BindView(R.id.loaderErrorAccount) Button loaderErrorAccount;
    @BindView(R.id.loaderErrorButton) Button loaderErrorButton;

    @BindView(R.id.error) TextView watcherError;
    @BindView(R.id.watcherNameWrapper) TextInputLayout watcherNameWrapper;
    @BindView(R.id.watcherName) EmojiAppCompatEditText watcherName;
    @BindView(R.id.watcherMovieIDWrapper) TextInputLayout watcherMovieIDWrapper;
    @BindView(R.id.watcherMovieID) AppCompatEditText watcherMovieID;
    @BindView(R.id.watcherCinemaIDWrapper) TextInputLayout watcherCinemaIDWrapper;
    @BindView(R.id.watcherCinemaID) InstantAutoComplete watcherCinemaID;
    @BindView(R.id.autocompleteSuggestion) LinearLayout autocompleteSuggestion;
    @BindView(R.id.autocompleteSuggestionCancel) AppCompatImageButton autocompleteSuggestionCancel;

    @BindView(R.id.begin) DoubleRowIconPreferenceView begin;
    @BindView(R.id.end) DoubleRowIconPreferenceView end;
    @BindView(R.id.filterStartAfter) DoubleRowIconPreferenceView filterStartAfter;
    @BindView(R.id.filterStartBefore) DoubleRowIconPreferenceView filterStartBefore;

    @BindView(R.id.filterIMAX) DoubleRowIconPreferenceView filterIMAX;
    @BindView(R.id.filterDolbyCinema) DoubleRowIconPreferenceView filterDolbyCinema;
    @BindView(R.id.filter3D) DoubleRowIconPreferenceView filter3D;
    @BindView(R.id.filter4K) DoubleRowIconPreferenceView filter4K;
    @BindView(R.id.filterLaser) DoubleRowIconPreferenceView filterLaser;
    @BindView(R.id.filterHFR) DoubleRowIconPreferenceView filterHFR;
    @BindView(R.id.filterDolbyAtmos) DoubleRowIconPreferenceView filterAtmos;
    @BindView(R.id.filterOV) DoubleRowIconPreferenceView filterOV;
    @BindView(R.id.filterNL) DoubleRowIconPreferenceView filterNL;
    @BindView(R.id.filter4DX) DoubleRowIconPreferenceView filter4DX;
    @BindView(R.id.filterDBOX) DoubleRowIconPreferenceView filterDBOX;

    @BindView(R.id.fab) FloatingActionButton fab;

    private Snackbar snackbar;

    private SharedPreferences settings;

    private Watcher watcher;
    private String id;

    private String sharedTitle;
    private Integer sharedMovieID;
    private static Long oneMinute = 60 * 1000L;
    private static Long oneWeek = oneMinute * 60 * 24 * 7;
    private static Long oneMonth = 2629746000L;

    private Mode mode = Mode.VIEWING;
    private List<Cinema> cinemas = new ArrayList<>();
    CinemaIDAdapter cinemaIDAdapter;

    private LocationUtil locationUtil = new LocationUtil();

    Handler validateCinemaIDHandler = new Handler();
    Runnable validateCinemaIDRunnable = () -> validateCinemaID(false);

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_watcher);
        ButterKnife.bind(this);

        settings = getSharedPreferences("settings", MODE_PRIVATE);

        setSupportActionBar(toolbar);
        if(getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_close);
            getSupportActionBar().setTitle("");
        }

        setupSharedInfo();
        AppDatabase.getInstance(this).cinemas().getCinemas().observe(this, cinemas -> {
            this.cinemas = cinemas;

            if(cinemaIDAdapter != null) {
                cinemaIDAdapter.setCinemas(cinemas);
            }

            updateViews(true);
        });

        watcherName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }
            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }
            @Override
            public void afterTextChanged(Editable editable) {
                validateName(false);
                if(watcher != null) {
                    watcher.setName(editable.toString());
                }
            }
        });
        watcherMovieID.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }
            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }
            @Override
            public void afterTextChanged(Editable editable) {
                validateMovieID(false);
                if(watcher != null && !editable.toString().equals("")) {
                    watcher.setMovieID(Integer.parseInt(editable.toString()));
                }
            }
        });
        cinemaIDAdapter = new CinemaIDAdapter(this, R.layout.spinner_cinema, cinemas);
        watcherCinemaID.setAdapter(cinemaIDAdapter);
        watcherCinemaID.setThreshold(0);
        watcherCinemaID.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }
            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }
            @Override
            public void afterTextChanged(Editable editable) {
                validateCinemaIDHandler.removeCallbacks(validateCinemaIDRunnable);
                validateCinemaIDHandler.postDelayed(validateCinemaIDRunnable, 1000);
            }
        });

        if(settings.getInt("prefAutocompleteLocation", -1) == -1) {
            autocompleteSuggestion.setOnClickListener(view -> askForLocation());
            autocompleteSuggestionCancel.setOnClickListener(view -> {
                settings.edit().putInt("prefAutocompleteLocation", 0).apply();
                autocompleteSuggestion.setVisibility(View.GONE);
            });
        } else {
            autocompleteSuggestion.setVisibility(View.GONE);
            if(settings.getInt("prefAutocompleteLocation", -1) == 1) {
                startLocation();
            }
        }

        begin.setOnClickListener(view -> {
            InterfaceUtil.clearForcus(WatcherActivity.this); // Prevent scroll after popup close due to focusing again
            showDateTimePicker(true, true, watcher.getBegin());
        });
        end.setOnClickListener(view -> {
            InterfaceUtil.clearForcus(WatcherActivity.this); // Prevent scroll after popup close due to focusing again
            showDateTimePicker(true, false, watcher.getEnd());
        });

        filterStartAfter.setOnClickListener(view -> {
            InterfaceUtil.clearForcus(WatcherActivity.this); // Prevent scroll after popup close due to focusing again
            showDateTimePicker(false, true, watcher.getFilters().getStartAfter());
        });
        filterStartBefore.setOnClickListener(view -> {
            InterfaceUtil.clearForcus(WatcherActivity.this); // Prevent scroll after popup close due to focusing again
            showDateTimePicker(false, false, watcher.getFilters().getStartBefore());
        });

        setOnPropClickListener(filterIMAX, new PropResultListener() {
            @Override
            public void gotResult(WatcherFilters.WatcherFilterValue value) {
                watcher.getFilters().setIMAX(value);
            }
        });
        setOnPropClickListener(filterDolbyCinema, new PropResultListener() {
            @Override
            public void gotResult(WatcherFilters.WatcherFilterValue value) {
                watcher.getFilters().setDolbyCinema(value);
            }
        });
        setOnPropClickListener(filter3D, new PropResultListener() {
            @Override
            public void gotResult(WatcherFilters.WatcherFilterValue value) {
                watcher.getFilters().set3D(value);
            }
        });
        setOnPropClickListener(filter4K, new PropResultListener() {
            @Override
            public void gotResult(WatcherFilters.WatcherFilterValue value) {
                watcher.getFilters().set4K(value);
            }
        });
        setOnPropClickListener(filterLaser, new PropResultListener() {
            @Override
            public void gotResult(WatcherFilters.WatcherFilterValue value) {
                watcher.getFilters().setLaser(value);
            }
        });
        setOnPropClickListener(filterHFR, new PropResultListener() {
            @Override
            public void gotResult(WatcherFilters.WatcherFilterValue value) {
                watcher.getFilters().setHFR(value);
            }
        });
        setOnPropClickListener(filterAtmos, new PropResultListener() {
            @Override
            public void gotResult(WatcherFilters.WatcherFilterValue value) {
                watcher.getFilters().setDolbyAtmos(value);
            }
        });
        setOnPropClickListener(filterOV, new PropResultListener() {
            @Override
            public void gotResult(WatcherFilters.WatcherFilterValue value) {
                watcher.getFilters().setOriginalVersion(value);
            }
        });
        setOnPropClickListener(filterNL, new PropResultListener() {
            @Override
            public void gotResult(WatcherFilters.WatcherFilterValue value) {
                watcher.getFilters().setDutchVersion(value);
            }
        });
        setOnPropClickListener(filter4DX, new PropResultListener() {
            @Override
            public void gotResult(WatcherFilters.WatcherFilterValue value) {
                watcher.getFilters().set4DX(value);
            }
        });
        setOnPropClickListener(filterDBOX, new PropResultListener() {
            @Override
            public void gotResult(WatcherFilters.WatcherFilterValue value) {
                watcher.getFilters().setDBOX(value);
            }
        });

        fab.setOnClickListener(view -> {
            if(mode == Mode.VIEWING) {
                mode = Mode.EDITING;
                updateViews();

                watcherName.requestFocus();
                InterfaceUtil.showKeyboard(WatcherActivity.this, watcherName);
            } else {
                InterfaceUtil.hideKeyboard(WatcherActivity.this);
                if(snackbar != null && snackbar.isShown()) {
                    snackbar.dismiss();
                }

                if(!settings.getString("userID", "").equals("")) {
                    saveWatcher();
                } else {
                    snackbar = Snackbar.make(coordinator, R.string.watchers_empty_account, Snackbar.LENGTH_INDEFINITE);
                    snackbar.setAction(R.string.add, view1 -> startActivity(new Intent(WatcherActivity.this, AccountActivity.class)));
                    snackbar.show();
                }
            }
        });

        loaderErrorAccount.setOnClickListener(view -> startActivity(new Intent(WatcherActivity.this, AccountActivity.class)));
        loaderErrorButton.setOnClickListener(view -> {
            loaderErrorButton.setEnabled(false);
            progress.setVisibility(View.VISIBLE);
            setupWatcher();
        });

        // Al ready to go!
        setupWatcher();
    }

    private void setupSharedInfo() {
        if((getIntent().getAction() != null && getIntent().getAction().equals(Intent.ACTION_SEND))
                || (getIntent().getType() != null && getIntent().getType().equals("text/plain"))
                || getIntent().getDataString() != null) {
            String data;

            if(getIntent().getAction() != null && getIntent().getAction().equals(Intent.ACTION_SEND)) {
                data = getIntent().getStringExtra(Intent.EXTRA_TEXT);
            } else {
                data = getIntent().getDataString();
            }

            try {
                Uri received = Uri.parse(data);
                if(Patterns.WEB_URL.matcher(data).matches()) {
                    Uri instance = Uri.parse(BuildConfig.SERVER_BASE_URL);

                    if(received.getHost().equals(instance.getHost())) {
                        if(received.getPathSegments().size() == 2 && received.getPathSegments().get(1) != null && !received.getPathSegments().get(1).equals("")) {
                            id = received.getPathSegments().get(1);
                        }
                    } else if(received.getHost().equals("www.pathe.nl")) {
                        if(received.getPathSegments().size() >= 2
                                && received.getPathSegments().get(0) != null && received.getPathSegments().get(0).equals("film")
                                && received.getPathSegments().get(1) != null && !received.getPathSegments().get(1).equals("")) {
                            sharedMovieID = Integer.parseInt(received.getPathSegments().get(1));

                            if(received.getPathSegments().size() >= 3 && received.getPathSegments().get(2) != null
                                    && !received.getPathSegments().get(2).equals("")) {
                                sharedTitle = WordUtils.capitalizeFully(received.getPathSegments().get(2).replace("-", " "));
                            }
                        }
                    }
                }
            } catch(Exception e) {
                // Failed, don't do anything with data
            }
        }
    }

    private void setupWatcher() {
        if(getIntent().getExtras() != null && !getIntent().getExtras().getString("id", "").equals("")) {
            id = getIntent().getExtras().getString("id");
            getWatcher();
        } else if(id != null) {
            // ID is already set
            getWatcher();
        } else {
            id = null;
            watcher = new Watcher();
            watcher.setFilters(new WatcherFilters());

            if(sharedTitle != null && !sharedTitle.equals("")) {
                watcher.setName(sharedTitle);
            }
            if(sharedMovieID != null && sharedMovieID > 0) {
                watcher.setMovieID(sharedMovieID);
            }
            watcher.setBegin(System.currentTimeMillis());
            watcher.setEnd(System.currentTimeMillis() + oneWeek);
            watcher.getFilters().setCinemaID(settings.getInt("prefSelectedCinema", 0));
            watcher.getFilters().setStartAfter(System.currentTimeMillis() + oneWeek);
            watcher.getFilters().setStartBefore(System.currentTimeMillis() + oneWeek + oneWeek);

            mode = Mode.EDITING;

            updateViews();
            doneLoading();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        settings = getSharedPreferences("settings", MODE_PRIVATE);

        if(snackbar != null && snackbar.isShown()) {
            snackbar.dismiss();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        locationUtil.onStop();
    }

    @Override
    protected void onDestroy() {
        validateCinemaIDHandler.removeCallbacksAndMessages(null);

        super.onDestroy();
    }

    private void getWatcher() {
        Call<Watcher> call = APIHelper.getInstance().getWatcher(settings.getString("userAPIKey", ""), id);
        call.enqueue(new Callback<Watcher>() {
            @Override
            public void onResponse(@NonNull Call<Watcher> call, @NonNull Response<Watcher> response) {
                if(response.code() == 200) {
                    watcher = response.body();
                    id = watcher.getID();

                    if(watcher.getUserID().equals(settings.getString("userID", ""))) {
                        mode = Mode.VIEWING;
                    } else {
                        mode = Mode.EDITING;
                    }

                    updateViews();
                    doneLoading();

                    if(mode == Mode.EDITING) {
                        watcherName.requestFocus();
                        InterfaceUtil.showKeyboard(WatcherActivity.this, watcherName);
                    }
                } else {
                    progress.setVisibility(View.GONE);

                    if(response.code() == 400) {
                        loaderErrorText.setText(R.string.error_watcher_400);
                        loaderErrorButton.setVisibility(View.GONE);
                    } else {
                        if(response.code() == 401) {
                            loaderErrorText.setText(R.string.error_watcher_401);
                            loaderErrorAccount.setVisibility(View.VISIBLE);
                        } else {
                            loaderErrorText.setText(getString(R.string.error_general_server, "H" + response.code()));
                            loaderErrorAccount.setVisibility(View.GONE);
                        }

                        loaderErrorButton.setEnabled(true);
                        loaderErrorButton.setVisibility(View.VISIBLE);
                    }

                    loaderError.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onFailure(@NonNull Call<Watcher> call, @NonNull Throwable t) {
                t.printStackTrace();

                progress.setVisibility(View.GONE);

                loaderErrorText.setText(R.string.error_general_exception);
                loaderErrorAccount.setVisibility(View.GONE);
                loaderErrorButton.setEnabled(true);
                loaderErrorButton.setVisibility(View.VISIBLE);
                loaderError.setVisibility(View.VISIBLE);
            }
        });
        clearNotifications();
    }

    private void clearNotifications() {
        AsyncTask.execute(() -> {
            List<Notification> notifications = AppDatabase.getInstance(WatcherActivity.this).notifications().getNotificationsForWatcher(id);
            if(notifications != null && notifications.size() > 0) {
                NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                if(manager == null) { return; }
                for(Notification notification : notifications) {
                    manager.cancel(notification.getId().hashCode());
                }
            }
        });
    }

    private void updateViews() {
        updateViews(false);
    }

    private void updateViews(boolean cinemaOnly) {
        if(watcher == null) { return; }

        // Input values (cinema ID)
        String foundCinema = "";
        for(Cinema cinema : cinemas) {
            if(cinema.getId().equals(watcher.getFilters().getCinemaID())) {
                foundCinema = cinema.getName();
            }
        }
        if(foundCinema.equals("") && watcher.getFilters().getCinemaID() != 0) { // We don't know this cinema ID's display name
            foundCinema = String.valueOf(watcher.getFilters().getCinemaID());
        }
        watcherCinemaID.setText(foundCinema);
        if(cinemaOnly) { return; }

        setFieldsEditable(mode == Mode.EDITING);

        // Errors
        loaderError.setVisibility(View.GONE);
        watcherError.setVisibility(View.GONE);

        // Input values
        watcherName.setText(watcher.getName());
        watcherMovieID.setText(watcher.getMovieID() == null ? "" : String.valueOf(watcher.getMovieID()));

        autocompleteSuggestion.setVisibility((mode == Mode.EDITING && settings.getInt("prefAutocompleteLocation", -1) == -1) ? View.VISIBLE : View.GONE);

        DateFormat format = SimpleDateFormat.getDateTimeInstance(java.text.DateFormat.MEDIUM, java.text.DateFormat.SHORT);
        begin.setValue(format.format(new Date(watcher.getBegin())));
        end.setValue(format.format(new Date(watcher.getEnd())));
        filterStartAfter.setValue(format.format(new Date(watcher.getFilters().getStartAfter())));
        filterStartBefore.setValue(format.format(new Date(watcher.getFilters().getStartBefore())));

        updateViewsFilters();

        // Buttons
        if(toolbar != null && toolbar.getMenu() != null && watcher != null && settings != null) {
            for(int i = 0; i < toolbar.getMenu().size(); i++) {
                if(toolbar.getMenu().getItem(i).getItemId() == R.id.watcherMenuShare || toolbar.getMenu().getItem(i).getItemId() == R.id.watcherMenuDuplicate) {
                    toolbar.getMenu().getItem(i).setVisible(mode == Mode.VIEWING && id != null);
                } else if(toolbar.getMenu().getItem(i).getItemId() == R.id.watcherMenuDelete) {
                    toolbar.getMenu().getItem(i).setVisible(id != null && !id.equals("") && watcher.getUserID() != null
                        && watcher.getUserID().equals(settings.getString("userID", "")));
                }
            }
        }

        fab.setImageResource(mode == Mode.EDITING ? R.drawable.ic_save : R.drawable.ic_edit);
    }

    private void updateViewsFilters() {
        if(watcher.getFilters() != null) {
            filterIMAX.setValue(watcher.getFilters().isIMAX() == WatcherFilters.WatcherFilterValue.NOPREFERENCE ? R.string.watcher_filter_value_nopreference :
                    (watcher.getFilters().isIMAX() == WatcherFilters.WatcherFilterValue.YES ? R.string.watcher_filter_value_yes : R.string.watcher_filter_value_no));
            filterDolbyCinema.setValue(watcher.getFilters().isDolbyCinema() == WatcherFilters.WatcherFilterValue.NOPREFERENCE ? R.string.watcher_filter_value_nopreference :
                    (watcher.getFilters().isDolbyCinema() == WatcherFilters.WatcherFilterValue.YES ? R.string.watcher_filter_value_yes : R.string.watcher_filter_value_no));
            filter3D.setValue(watcher.getFilters().is3D() == WatcherFilters.WatcherFilterValue.NOPREFERENCE ? R.string.watcher_filter_value_nopreference :
                    (watcher.getFilters().is3D() == WatcherFilters.WatcherFilterValue.YES ? R.string.watcher_filter_value_yes : R.string.watcher_filter_value_no));
            filter4K.setValue(watcher.getFilters().is4K() == WatcherFilters.WatcherFilterValue.NOPREFERENCE ? R.string.watcher_filter_value_nopreference :
                    (watcher.getFilters().is4K() == WatcherFilters.WatcherFilterValue.YES ? R.string.watcher_filter_value_yes : R.string.watcher_filter_value_no));
            filterLaser.setValue(watcher.getFilters().isLaser() == WatcherFilters.WatcherFilterValue.NOPREFERENCE ? R.string.watcher_filter_value_nopreference :
                    (watcher.getFilters().isLaser() == WatcherFilters.WatcherFilterValue.YES ? R.string.watcher_filter_value_yes : R.string.watcher_filter_value_no));
            filterHFR.setValue(watcher.getFilters().isHFR() == WatcherFilters.WatcherFilterValue.NOPREFERENCE ? R.string.watcher_filter_value_nopreference :
                    (watcher.getFilters().isHFR() == WatcherFilters.WatcherFilterValue.YES ? R.string.watcher_filter_value_yes : R.string.watcher_filter_value_no));
            filterAtmos.setValue(watcher.getFilters().isDolbyAtmos() == WatcherFilters.WatcherFilterValue.NOPREFERENCE ? R.string.watcher_filter_value_nopreference :
                    (watcher.getFilters().isDolbyAtmos() == WatcherFilters.WatcherFilterValue.YES ? R.string.watcher_filter_value_yes : R.string.watcher_filter_value_no));
            filterOV.setValue(watcher.getFilters().isOriginalVersion() == WatcherFilters.WatcherFilterValue.NOPREFERENCE ? R.string.watcher_filter_value_nopreference :
                    (watcher.getFilters().isOriginalVersion() == WatcherFilters.WatcherFilterValue.YES ? R.string.watcher_filter_value_yes : R.string.watcher_filter_value_no));
            filterNL.setValue(watcher.getFilters().isDutchVersion() == WatcherFilters.WatcherFilterValue.NOPREFERENCE ? R.string.watcher_filter_value_nopreference :
                    (watcher.getFilters().isDutchVersion() == WatcherFilters.WatcherFilterValue.YES ? R.string.watcher_filter_value_yes : R.string.watcher_filter_value_no));
            filter4DX.setValue(watcher.getFilters().is4DX() == WatcherFilters.WatcherFilterValue.NOPREFERENCE ? R.string.watcher_filter_value_nopreference :
                    (watcher.getFilters().is4DX() == WatcherFilters.WatcherFilterValue.YES ? R.string.watcher_filter_value_yes : R.string.watcher_filter_value_no));
            filterDBOX.setValue(watcher.getFilters().isDBOX() == WatcherFilters.WatcherFilterValue.NOPREFERENCE ? R.string.watcher_filter_value_nopreference :
                    (watcher.getFilters().isDBOX() == WatcherFilters.WatcherFilterValue.YES ? R.string.watcher_filter_value_yes : R.string.watcher_filter_value_no));
        } else {
            filterIMAX.setValue(R.string.watcher_filter_value_nopreference);
            filterDolbyCinema.setValue(R.string.watcher_filter_value_nopreference);
            filter3D.setValue(R.string.watcher_filter_value_nopreference);
            filter4K.setValue(R.string.watcher_filter_value_nopreference);
            filterLaser.setValue(R.string.watcher_filter_value_nopreference);
            filterHFR.setValue(R.string.watcher_filter_value_nopreference);
            filterAtmos.setValue(R.string.watcher_filter_value_nopreference);
            filterOV.setValue(R.string.watcher_filter_value_nopreference);
            filterNL.setValue(R.string.watcher_filter_value_nopreference);
            filter4DX.setValue(R.string.watcher_filter_value_nopreference);
            filterDBOX.setValue(R.string.watcher_filter_value_nopreference);
        }
    }

    private void setFieldsEnabled(boolean enabled) {
        watcherNameWrapper.setEnabled(enabled);
        watcherMovieIDWrapper.setEnabled(enabled);
        watcherCinemaIDWrapper.setEnabled(enabled);
    }

    private void setFieldsEditable(boolean editable) {
        watcherName.setFocusable(editable);
        watcherName.setFocusableInTouchMode(editable);
        watcherName.setCursorVisible(editable);
        watcherMovieID.setFocusable(editable);
        watcherMovieID.setFocusableInTouchMode(editable);
        watcherMovieID.setCursorVisible(editable);
        watcherCinemaID.setFocusable(editable);
        watcherCinemaID.setFocusableInTouchMode(editable);
        watcherCinemaID.setCursorVisible(editable);

        begin.setClickable(editable);
        end.setClickable(editable);
        filterStartAfter.setClickable(editable);
        filterStartBefore.setClickable(editable);

        filterIMAX.setClickable(editable);
        filterDolbyCinema.setClickable(editable);
        filter3D.setClickable(editable);
        filter4K.setClickable(editable);
        filterLaser.setClickable(editable);
        filterHFR.setClickable(editable);
        filterAtmos.setClickable(editable);
        filterOV.setClickable(editable);
        filterNL.setClickable(editable);
        filter4DX.setClickable(editable);
        filterDBOX.setClickable(editable);
    }

    private void doneLoading() {
        progress.setVisibility(View.GONE);
        loaderError.setVisibility(View.GONE);
        loaderErrorButton.setEnabled(true);

        main.setVisibility(View.VISIBLE);
        fab.show();
    }

    private void showDateTimePicker(final boolean checkingValue, final boolean beginValue, long currentValue) {
        final Calendar current = Calendar.getInstance();
        current.setTimeInMillis(currentValue);

        DatePickerDialog datePickerDialog = new DatePickerDialog(this, (datePicker, year, month, day) -> {
            final int mYear = year;
            final int mMonth = month;
            final int mDay = day;
            TimePickerDialog timePickerDialog = new TimePickerDialog(WatcherActivity.this, (timePicker, hour, minute) -> {
                Calendar setTo = Calendar.getInstance();
                setTo.set(mYear, mMonth, mDay, hour, minute, 0);
                if(checkingValue) {
                    if(beginValue) {
                        watcher.setBegin(setTo.getTimeInMillis());
                        validateAndFixEnd();
                    } else {
                        watcher.setEnd(setTo.getTimeInMillis());
                        validateAndFixBegin();
                    }
                } else {
                    if(beginValue) {
                        watcher.getFilters().setStartAfter(setTo.getTimeInMillis());
                        validateAndFixStartBefore();
                    } else {
                        watcher.getFilters().setStartBefore(setTo.getTimeInMillis());
                        validateAndFixStartAfter();
                    }
                }

                updateViews();
            }, current.get(Calendar.HOUR_OF_DAY), current.get(Calendar.MINUTE), android.text.format.DateFormat.is24HourFormat(WatcherActivity.this));
            timePickerDialog.show();
        }, current.get(Calendar.YEAR), current.get(Calendar.MONTH), current.get(Calendar.DAY_OF_MONTH));
        datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis() - 1000L);
        datePickerDialog.show();
    }

    private boolean validateName(boolean forced) {
        if(watcherName.getText().toString().length() >= 3 && watcherName.getText().toString().length() <= 50) {
            watcherNameWrapper.setErrorEnabled(false);
            return true;
        } else {
            if(forced) {
                watcherNameWrapper.setError(getString(R.string.watcher_validate_name));
                watcherNameWrapper.setErrorEnabled(true);
            }
            return false;
        }
    }

    private boolean validateMovieID(boolean forced) {
        try {
            if(watcherMovieID.getText().toString().length() > 0 && Integer.parseInt(watcherMovieID.getText().toString()) > 0) {
                watcherMovieIDWrapper.setErrorEnabled(false);
                return true;
            } else {
                if(watcherMovieID.getText().toString().length() == 0 && !forced) {
                    return false;
                }
                watcherMovieIDWrapper.setError(getString(R.string.watcher_validate_movieid));
                watcherMovieIDWrapper.setErrorEnabled(true);
                return false;
            }
        } catch(NumberFormatException e) {
            if(watcherMovieID.getText().toString().length() == 0 && !forced) {
                return false;
            }
            watcherMovieIDWrapper.setError(getString(R.string.watcher_validate_movieid));
            watcherMovieIDWrapper.setErrorEnabled(true);
            return false;
        }
    }

    private boolean validateCinemaID(boolean forced) {
        if(watcherCinemaID.getText().toString().length() > 0) {
            String foundName = watcherCinemaID.getText().toString();
            int foundID = 0;
            if(cinemas != null) {
                for(Cinema cinema : cinemas) {
                    if(cinema.getName().equals(foundName)) {
                        foundID = cinema.getId();
                    }
                }
            }
            if(foundID != 0) {
                watcher.getFilters().setCinemaID(foundID);

                watcherCinemaIDWrapper.setErrorEnabled(false);
                return true;
            } else {
                if(forced) {
                    watcherCinemaIDWrapper.setError(getString(R.string.watcher_validate_cinemaid));
                    watcherCinemaIDWrapper.setErrorEnabled(true);
                } else {
                    watcher.getFilters().setCinemaID(0);
                }
                return false;
            }
        } else {
            if(forced) {
                watcherCinemaIDWrapper.setError(getString(R.string.watcher_validate_cinemaid));
                watcherCinemaIDWrapper.setErrorEnabled(true);
            }
            return false;
        }
    }

    private void validateAndFixBegin() {
        boolean updatedOther = false;

        if(watcher.getBegin() >= watcher.getEnd()) {
            watcher.setBegin(watcher.getEnd() - oneMinute);
            updatedOther = true;
        } else if(watcher.getEnd() - watcher.getBegin() > oneMonth) {
            watcher.setBegin(watcher.getEnd() - oneMonth + oneMinute);
            updatedOther = true;
        }

        if(updatedOther) {
            snackbar = Snackbar.make(coordinator, R.string.watcher_validate_begin, Snackbar.LENGTH_LONG);
            snackbar.show();
            updateViews();
        }
    }

    private void validateAndFixEnd() {
        boolean updated = false;

        if(watcher.getEnd() <= watcher.getBegin()) {
            watcher.setEnd(watcher.getBegin() + oneMinute);
            updated = true;
        } else if(watcher.getEnd() - watcher.getBegin() > oneMonth) {
            watcher.setEnd(watcher.getBegin() + oneMonth - oneMinute);
            updated = true;
        }

        if(updated) {
            snackbar = Snackbar.make(coordinator, R.string.watcher_validate_end, Snackbar.LENGTH_LONG);
            snackbar.show();
            updateViews();
        }
    }

    private void validateAndFixStartAfter() {
        boolean updated = false;

        if(watcher.getFilters().getStartAfter() >= watcher.getFilters().getStartBefore()) {
            watcher.getFilters().setStartAfter(watcher.getFilters().getStartBefore() - oneMinute);
            updated = true;
        } else if(watcher.getFilters().getStartBefore() - watcher.getFilters().getStartAfter() > (oneWeek + oneWeek)) {
            watcher.getFilters().setStartAfter(watcher.getFilters().getStartBefore() - oneWeek - oneWeek + oneMinute);
            updated = true;
        }

        if(updated) {
            snackbar = Snackbar.make(coordinator, R.string.watcher_validate_startafter, Snackbar.LENGTH_LONG);
            snackbar.show();
            updateViews();
        }
    }

    private void validateAndFixStartBefore() {
        boolean updated = false;

        if(watcher.getFilters().getStartBefore() <= watcher.getFilters().getStartAfter()) {
            watcher.getFilters().setStartBefore(watcher.getFilters().getStartAfter() + oneMinute);
            updated = true;
        } else if(watcher.getFilters().getStartBefore() - watcher.getFilters().getStartAfter() > (oneWeek + oneWeek)) {
            watcher.getFilters().setStartBefore(watcher.getFilters().getStartAfter() + oneWeek + oneWeek - oneMinute);
            updated = true;
        }

        if(updated) {
            snackbar = Snackbar.make(coordinator, R.string.watcher_validate_startbefore, Snackbar.LENGTH_LONG);
            snackbar.show();
            updateViews();
        }
    }

    private void saveWatcher() {
        if(validateName(true) && validateMovieID(true) && validateCinemaID(true)) {
            fab.setEnabled(false);
            progress.setVisibility(View.VISIBLE);
            watcherError.setVisibility(View.GONE);
            setFieldsEnabled(false);

            Watcher toSave = new Watcher();
            toSave.setName(watcherName.getText().toString());
            toSave.setMovieID(Integer.parseInt(watcherMovieID.getText().toString()));
            toSave.setBegin(watcher.getBegin());
            toSave.setEnd(watcher.getEnd());

            toSave.setFilters(watcher.getFilters()); // Validation set the cinema ID correctly

            Call<Watcher> call;

            if(id == null || id.equals("") || !watcher.getUserID().equals(settings.getString("userID", ""))) { // Create
                toSave.setUserID(settings.getString("userID", ""));
                call = APIHelper.getInstance().addWatcher(settings.getString("userAPIKey", ""), toSave);
            } else { // Update
                call = APIHelper.getInstance().updateWatcher(settings.getString("userAPIKey", ""), id, toSave);
            }

            call.enqueue(new Callback<Watcher>() {
                @Override
                public void onResponse(@NonNull Call<Watcher> call, @NonNull Response<Watcher> response) {
                    fab.setEnabled(true);
                    progress.setVisibility(View.GONE);
                    setFieldsEnabled(true);

                    if(response.code() == 200) {
                        watcher = response.body();
                        id = watcher.getID();

                        mode = Mode.VIEWING;

                        snackbar = Snackbar.make(coordinator, R.string.watcher_saved, Snackbar.LENGTH_SHORT);
                        snackbar.show();

                        updateViews();
                        doneLoading();
                    } else {
                        setFieldsEditable(true);

                        watcherError.setText(ErrorUtil.getErrorMessage(WatcherActivity.this, response));
                        watcherError.setVisibility(View.VISIBLE);

                        main.smoothScrollTo(0, 0);
                    }
                }

                @Override
                public void onFailure(@NonNull Call<Watcher> call, @NonNull Throwable t) {
                    t.printStackTrace();

                    fab.setEnabled(true);
                    progress.setVisibility(View.GONE);
                    setFieldsEnabled(true);

                    watcherError.setText(ErrorUtil.getErrorMessage(WatcherActivity.this, null));
                    watcherError.setVisibility(View.VISIBLE);

                    main.smoothScrollTo(0, 0);
                }
            });
        }
    }

    private void deleteWatcher() {
        fab.setEnabled(false);
        progress.setVisibility(View.VISIBLE);
        watcherError.setVisibility(View.GONE);
        setFieldsEnabled(false);

        Call<ResponseBody> call = APIHelper.getInstance().deleteWatcher(settings.getString("userAPIKey", ""), id);
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                if(response.code() == 200) {
                    WatcherActivity.this.finish();
                } else {
                    fab.setEnabled(true);
                    progress.setVisibility(View.GONE);
                    setFieldsEnabled(true);

                    if(response.code() == 400) {
                        watcherError.setText(R.string.error_watcher_400);
                    } else if(response.code() == 401) {
                        watcherError.setText(R.string.error_watcher_401);
                    } else {
                        watcherError.setText(getString(R.string.error_general_server, "H" + response.code()));
                    }

                    watcherError.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                t.printStackTrace();

                fab.setEnabled(true);
                progress.setVisibility(View.GONE);
                watcherError.setText(R.string.error_general_exception);
                watcherError.setVisibility(View.VISIBLE);
                setFieldsEnabled(true);
            }
        });
    }

    private void shareWatcher() {
        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, BuildConfig.SERVER_BASE_URL + "w/" + id);
        sendIntent.setType("text/plain");
        startActivity(Intent.createChooser(sendIntent, getString(R.string.watcher_share)));
    }

    private void duplicateWatcher() {
        progress.setVisibility(View.VISIBLE);
        watcherError.setVisibility(View.GONE);

        snackbar = Snackbar.make(coordinator, R.string.watcher_duplicate, Snackbar.LENGTH_INDEFINITE);
        snackbar.show();

        watcher.setName(getString(R.string.watcher_copy, watcher.getName()));

        id = null;
        mode = Mode.EDITING;
        main.smoothScrollTo(0, 0);
        doneLoading();
        updateViews();

        watcherName.requestFocus();
        InterfaceUtil.showKeyboard(WatcherActivity.this, watcherName);
    }

    private void setOnPropClickListener(final View click, final PropResultListener callback) {
        click.setOnClickListener(view -> {
            InterfaceUtil.clearForcus(WatcherActivity.this); // Prevent scroll after menu close due to focusing again

            PopupMenu popupMenu = new PopupMenu(WatcherActivity.this, click);
            popupMenu.getMenuInflater().inflate(R.menu.menu_prop, popupMenu.getMenu());
            popupMenu.setOnMenuItemClickListener(item -> {
                if(watcher.getFilters() == null) {
                    watcher.setFilters(new WatcherFilters());
                }
                switch(item.getItemId()) {
                    case R.id.filter_yes:
                        callback.gotResult(WatcherFilters.WatcherFilterValue.YES);
                        break;
                    case R.id.filter_no:
                        callback.gotResult(WatcherFilters.WatcherFilterValue.NO);
                        break;
                    case R.id.filter_nopreference:
                    default:
                        callback.gotResult(WatcherFilters.WatcherFilterValue.NOPREFERENCE);
                        break;
                }
                updateViews();
                return true;
            });
            popupMenu.show();
        });
    }

    @Override
    public void onBackPressed() {
        if(mode == Mode.EDITING) {
            if (watcher == null) {
                super.onBackPressed();
            } else {
                InterfaceUtil.hideKeyboard(this);
                new AlertDialog.Builder(this).setMessage(R.string.watcher_discard)
                        .setPositiveButton(R.string.yes, (dialogInterface, i) -> WatcherActivity.super.onBackPressed())
                        .setNegativeButton(R.string.no, null)
                        .show();
            }
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_watcher, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case android.R.id.home:
            case R.id.homeAsUp:
                onBackPressed();
                return true;
            case R.id.watcherMenuShare:
                shareWatcher();
                return true;
            case R.id.watcherMenuDuplicate:
                duplicateWatcher();
                return true;
            case R.id.watcherMenuDelete:
                new AlertDialog.Builder(this).setMessage(R.string.watcher_delete_confirm)
                        .setPositiveButton(R.string.yes, (dialogInterface, i) -> {
                            dialogInterface.dismiss();
                            deleteWatcher();
                        })
                        .setNegativeButton(R.string.no, null)
                        .show();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private abstract class PropResultListener {
        public abstract void gotResult(WatcherFilters.WatcherFilterValue value);
    }

    private void askForLocation() {
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            settings.edit().putInt("prefAutocompleteLocation", 1).apply();
            autocompleteSuggestion.setVisibility(View.GONE);
            startLocation();
        } else {
            if(!isFinishing()) {
                if(ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                    snackbar = Snackbar.make(coordinator, R.string.settings_general_location_permission_rationale, Snackbar.LENGTH_LONG)
                            .setAction(R.string.ok, view -> ActivityCompat.requestPermissions(WatcherActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_LOCATION_AUTOCOMPLETE));
                    snackbar.show();
                } else {
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_LOCATION_AUTOCOMPLETE);
                }
            }
        }
    }

    private void startLocation() {
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationUtil.setupLocationClient(this);
            locationUtil.getLocation(this, new LocationUtil.LocationUtilRequest() {
                @Override
                public void onLocationReceived(Location location, boolean isCachedResult) {
                    if(cinemaIDAdapter != null) {
                        cinemaIDAdapter.setLocation(location);
                    }
                }

                @Override
                public Context getContext() {
                    return WatcherActivity.this;
                }
            });
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch(requestCode) {
            case PERMISSION_LOCATION_AUTOCOMPLETE: {
                if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    settings.edit().putInt("prefAutocompleteLocation", 1).apply();
                    startLocation();
                } else {
                    snackbar = Snackbar.make(coordinator, R.string.settings_general_location_permission_denied, Snackbar.LENGTH_LONG);
                    snackbar.show();
                    settings.edit().putInt("prefAutocompleteLocation", 0).apply();
                }
                autocompleteSuggestion.setVisibility(View.GONE);
                break;
            }
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
                break;
        }
    }
}
