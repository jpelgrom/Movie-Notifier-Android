package nl.jpelgrm.movienotifier.ui;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Patterns;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.util.Pair;
import androidx.core.view.ViewCompat;

import com.google.android.material.chip.Chip;
import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.DateValidatorPointForward;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;

import org.apache.commons.text.WordUtils;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import nl.jpelgrm.movienotifier.BuildConfig;
import nl.jpelgrm.movienotifier.R;
import nl.jpelgrm.movienotifier.data.APIHelper;
import nl.jpelgrm.movienotifier.data.AppDatabase;
import nl.jpelgrm.movienotifier.data.CinemaIDAdapter;
import nl.jpelgrm.movienotifier.databinding.ActivityWatcherBinding;
import nl.jpelgrm.movienotifier.models.Cinema;
import nl.jpelgrm.movienotifier.models.Notification;
import nl.jpelgrm.movienotifier.models.Watcher;
import nl.jpelgrm.movienotifier.models.WatcherFilterValue;
import nl.jpelgrm.movienotifier.models.WatcherFilters;
import nl.jpelgrm.movienotifier.ui.settings.AccountActivity;
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

    private ActivityWatcherBinding binding;

    private Snackbar snackbar;
    private boolean snackbarForPermission = false;

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
    private boolean updatingViews = false;

    private LocationUtil locationUtil = new LocationUtil();

    Handler validateCinemaIDHandler = new Handler();
    Runnable validateCinemaIDRunnable = () -> validateCinemaID(false);

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityWatcherBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        int systemUiFlags = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            systemUiFlags = systemUiFlags | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION;
        }
        binding.getRoot().setSystemUiVisibility(systemUiFlags);

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
                v.setPadding(insets.getSystemWindowInsetLeft(), 0, insets.getSystemWindowInsetRight(), 0);
                return insets;
            });
            ViewCompat.setOnApplyWindowInsetsListener(binding.main, (v, insets) -> {
                v.setPadding(0, 0, 0, insets.getSystemWindowInsetBottom());
                return insets;
            });
            ViewCompat.setOnApplyWindowInsetsListener(binding.fab, (v, insets) -> {
                CoordinatorLayout.LayoutParams params = (CoordinatorLayout.LayoutParams) v.getLayoutParams();
                int margin = getResources().getDimensionPixelSize(R.dimen.fab_margin);
                params.setMargins(0, 0, margin, margin + insets.getSystemWindowInsetBottom());
                v.setLayoutParams(params);
                return insets;
            });
        }

        settings = getSharedPreferences("settings", MODE_PRIVATE);

        setSupportActionBar(binding.toolbar);
        if(getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_close);
            getSupportActionBar().setTitle("");
        }

        setupSharedInfo();
        AppDatabase.Companion.getInstance(this).cinemas().getCinemas().observe(this, cinemas -> {
            this.cinemas = cinemas;

            if(cinemaIDAdapter != null) {
                cinemaIDAdapter.setCinemas(cinemas);
            }

            updateViews(true);
        });

        binding.watcherName.addTextChangedListener(new TextWatcher() {
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
        binding.watcherMovieID.addTextChangedListener(new TextWatcher() {
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
                    watcher.setMovieid(Integer.parseInt(editable.toString()));
                }
            }
        });
        cinemaIDAdapter = new CinemaIDAdapter(this, R.layout.spinner_cinema, cinemas);
        binding.watcherCinemaID.setAdapter(cinemaIDAdapter);
        binding.watcherCinemaID.setThreshold(0);
        binding.watcherCinemaID.addTextChangedListener(new TextWatcher() {
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

        int autocompletePref = settings.getInt("prefAutocompleteLocation", -1);
        if(autocompletePref == -1 || (autocompletePref == 1 && !isFinishing()
                && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)) {
            binding.autocompleteSuggestion.setOnClickListener(view -> askForLocation());
            binding.autocompleteSuggestionCancel.setOnClickListener(view -> {
                settings.edit().putInt("prefAutocompleteLocation", 0).apply();
                binding.autocompleteSuggestion.setVisibility(View.GONE);
            });
        } else {
            binding.autocompleteSuggestion.setVisibility(View.GONE);
            if(autocompletePref == 1) {
                startLocation();
            }
        }

        binding.watcherStart.setOnClickListener(view -> {
            InterfaceUtil.clearFocus(WatcherActivity.this); // Prevent scroll after popup close due to focusing again
            showDatePicker(false);
        });
        binding.startAfterDate.setOnClickListener(view -> {
            InterfaceUtil.clearFocus(WatcherActivity.this); // Prevent scroll after popup close due to focusing again
            showDatePicker(false);
        });
        binding.startAfterTime.setOnClickListener(v -> {
            InterfaceUtil.clearFocus(WatcherActivity.this); // Prevent scroll after popup close due to focusing again
            showTimePicker(false, true, watcher.getFilters().getStartafter());
        });
        binding.startBeforeDate.setOnClickListener(view -> {
            InterfaceUtil.clearFocus(WatcherActivity.this); // Prevent scroll after popup close due to focusing again
            showDatePicker(false);
        });
        binding.startBeforeTime.setOnClickListener(v -> {
            InterfaceUtil.clearFocus(WatcherActivity.this); // Prevent scroll after popup close due to focusing again
            showTimePicker(false, false, watcher.getFilters().getStartbefore());
        });
        binding.active.setOnClickListener(view -> {
            InterfaceUtil.clearFocus(WatcherActivity.this); // Prevent scroll after popup close due to focusing again
            showDatePicker(true);
        });
        binding.beginDate.setOnClickListener(view -> {
            InterfaceUtil.clearFocus(WatcherActivity.this); // Prevent scroll after popup close due to focusing again
            showDatePicker(true);
        });
        binding.beginTime.setOnClickListener(v -> {
            InterfaceUtil.clearFocus(WatcherActivity.this); // Prevent scroll after popup close due to focusing again
            showTimePicker(true, true, watcher.getBegin());
        });
        binding.endDate.setOnClickListener(view -> {
            InterfaceUtil.clearFocus(WatcherActivity.this); // Prevent scroll after popup close due to focusing again
            showDatePicker(true);
        });
        binding.endTime.setOnClickListener(v -> {
            InterfaceUtil.clearFocus(WatcherActivity.this); // Prevent scroll after popup close due to focusing again
            showTimePicker(true, false, watcher.getEnd());
        });

        binding.filterRegularShowing.setOnCheckedChangeListener((buttonView, isChecked) -> validateAndUpdateExperiences());
        binding.filterIMAX.setOnCheckedChangeListener((buttonView, isChecked) -> validateAndUpdateExperiences());
        binding.filterDolbyCinema.setOnCheckedChangeListener((buttonView, isChecked) -> validateAndUpdateExperiences());
        binding.filter4DX.setOnCheckedChangeListener((buttonView, isChecked) -> validateAndUpdateExperiences());
        binding.filterScreenX.setOnCheckedChangeListener((buttonView, isChecked) -> validateAndUpdateExperiences());
        binding.filter2D.setOnCheckedChangeListener((buttonView, isChecked) -> validateAndUpdate3D());
        binding.filter3D.setOnCheckedChangeListener((buttonView, isChecked) -> validateAndUpdate3D());
        setOnPropClickListener(binding.filter4K, value -> watcher.getFilters().set_4k(value));
        setOnPropClickListener(binding.filterLaser, value -> watcher.getFilters().setLaser(value));
        setOnPropClickListener(binding.filterHFR, value -> watcher.getFilters().setHfr(value));
        setOnPropClickListener(binding.filterDolbyAtmos, value -> watcher.getFilters().setDolbyatmos(value));
        setOnPropClickListener(binding.filterOV, value -> watcher.getFilters().setOv(value));
        setOnPropClickListener(binding.filterNL, value -> watcher.getFilters().setNl(value));

        binding.fab.setOnClickListener(view -> {
            if(mode == Mode.VIEWING) {
                mode = Mode.EDITING;
                updateViews();

                binding.watcherName.requestFocus();
                InterfaceUtil.showKeyboard(WatcherActivity.this, binding.watcherName);
            } else {
                InterfaceUtil.hideKeyboard(WatcherActivity.this);
                if(snackbar != null && snackbar.isShown()) {
                    snackbar.dismiss();
                }

                if(!settings.getString("userID", "").equals("")) {
                    saveWatcher();
                } else {
                    snackbar = Snackbar.make(binding.coordinator, R.string.watchers_empty_account, Snackbar.LENGTH_INDEFINITE);
                    snackbar.setAction(R.string.add, view1 -> startActivity(new Intent(WatcherActivity.this, AccountActivity.class)));
                    snackbar.setAnchorView(binding.fab);
                    snackbar.show();
                }
            }
        });

        binding.loaderErrorAccount.setOnClickListener(view -> startActivity(new Intent(WatcherActivity.this, AccountActivity.class)));
        binding.loaderErrorSettings.setOnClickListener(view -> {
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startActivity(new Intent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY));
            } else {
                startActivity(new Intent(Settings.ACTION_WIRELESS_SETTINGS));
            }
        });
        binding.loaderErrorButton.setOnClickListener(view -> {
            binding.loaderErrorButton.setEnabled(false);
            binding.progress.setVisibility(View.VISIBLE);
            setupWatcher();
        });

        // All ready to go!
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

    @SuppressLint("NewApi")
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
                watcher.setMovieid(sharedMovieID);
            }

            watcher.setBegin(System.currentTimeMillis());
            LocalDate nextMonday = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.MONDAY));
            LocalTime ninePm = LocalTime.of(21, 0);
            watcher.setEnd(LocalDateTime.of(nextMonday, ninePm).atZone(ZoneId.systemDefault()).toEpochSecond() * 1000L);

            watcher.getFilters().setCinemaid(settings.getInt("prefSelectedCinema", 0));

            LocalDate nextWednesday = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.WEDNESDAY));
            LocalDate nextNextWednesday = nextWednesday.with(TemporalAdjusters.next(DayOfWeek.WEDNESDAY));
            LocalTime tenAm = LocalTime.of(10, 0);
            LocalTime elevenPm = LocalTime.of(23, 0);
            watcher.getFilters().setStartafter(LocalDateTime.of(nextWednesday, tenAm).atZone(ZoneId.systemDefault()).toEpochSecond() * 1000L);
            watcher.getFilters().setStartbefore(LocalDateTime.of(nextNextWednesday, elevenPm).atZone(ZoneId.systemDefault()).toEpochSecond() * 1000L);

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

        if(snackbar != null && snackbar.isShown() && !snackbarForPermission) {
            snackbar.dismiss();
        }
        snackbarForPermission = false;
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
        Call<Watcher> call = APIHelper.INSTANCE.getInstance().getWatcher(settings.getString("userAPIKey", ""), id);
        call.enqueue(new Callback<Watcher>() {
            @Override
            public void onResponse(@NonNull Call<Watcher> call, @NonNull Response<Watcher> response) {
                if(response.code() == 200) {
                    watcher = response.body();
                    id = watcher.getId();

                    if(watcher.getUserid().equals(settings.getString("userID", ""))) {
                        mode = Mode.VIEWING;
                    } else {
                        mode = Mode.EDITING;
                    }

                    updateViews();
                    doneLoading();

                    if(mode == Mode.EDITING) {
                        binding.watcherName.requestFocus();
                        InterfaceUtil.showKeyboard(WatcherActivity.this, binding.watcherName);
                    }
                } else {
                    binding.progress.setVisibility(View.GONE);
                    binding.loaderErrorSettings.setVisibility(View.GONE);

                    if(response.code() == 400) {
                        binding.loaderErrorAccount.setVisibility(View.GONE);
                        binding.loaderErrorText.setText(R.string.error_watcher_400);
                        binding.loaderErrorButton.setVisibility(View.GONE);
                    } else {
                        if(response.code() == 401) {
                            binding.loaderErrorText.setText(R.string.error_watcher_401);
                            binding.loaderErrorAccount.setVisibility(View.VISIBLE);
                        } else {
                            binding.loaderErrorText.setText(getString(R.string.error_general_server, "H" + response.code()));
                            binding.loaderErrorAccount.setVisibility(View.GONE);
                        }

                        binding.loaderErrorButton.setEnabled(true);
                        binding.loaderErrorButton.setVisibility(View.VISIBLE);
                    }

                    binding.loaderError.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onFailure(@NonNull Call<Watcher> call, @NonNull Throwable t) {
                t.printStackTrace();

                binding.progress.setVisibility(View.GONE);

                binding.loaderErrorText.setText(R.string.error_general_exception);
                binding.loaderErrorAccount.setVisibility(View.GONE);
                binding.loaderErrorSettings.setVisibility(View.VISIBLE);
                binding.loaderErrorButton.setEnabled(true);
                binding.loaderErrorButton.setVisibility(View.VISIBLE);
                binding.loaderError.setVisibility(View.VISIBLE);
            }
        });
        clearNotifications();
    }

    private void clearNotifications() {
        AsyncTask.execute(() -> {
            List<Notification> notifications = AppDatabase.Companion.getInstance(WatcherActivity.this).notifications().getNotificationsForWatcher(id);
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
            if(cinema.getId() == watcher.getFilters().getCinemaid()) {
                foundCinema = cinema.getName();
            }
        }
        if(foundCinema.equals("") && watcher.getFilters().getCinemaid() != 0) { // We don't know this cinema ID's display name
            foundCinema = String.valueOf(watcher.getFilters().getCinemaid());
        }
        binding.watcherCinemaID.setText(foundCinema);
        if(cinemaOnly) { return; }

        // Errors
        binding.loaderError.setVisibility(View.GONE);
        binding.watcherError.setVisibility(View.GONE);

        // Input values
        binding.watcherName.setText(watcher.getName());
        binding.watcherMovieID.setText(watcher.getMovieid() == null ? "" : String.valueOf(watcher.getMovieid()));

        if(!binding.watcherNameWrapper.isHintAnimationEnabled()) { binding.watcherNameWrapper.setHintAnimationEnabled(true); }
        if(!binding.watcherMovieIDWrapper.isHintAnimationEnabled()) { binding.watcherMovieIDWrapper.setHintAnimationEnabled(true); }
        if(!binding.watcherCinemaIDWrapper.isHintAnimationEnabled()) { binding.watcherCinemaIDWrapper.setHintAnimationEnabled(true); }

        int autocompletePref = settings.getInt("prefAutocompleteLocation", -1);
        boolean autocompleteShouldBeShown = (autocompletePref == -1 || (autocompletePref == 1 && !isFinishing()
            && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED));
        binding.autocompleteSuggestion.setVisibility((mode == Mode.EDITING && autocompleteShouldBeShown) ? View.VISIBLE : View.GONE);

        DateFormat dateFormat = SimpleDateFormat.getDateInstance(DateFormat.MEDIUM);
        DateFormat timeFormat = SimpleDateFormat.getTimeInstance(DateFormat.SHORT);
        Date watcherBegin = new Date(watcher.getBegin());
        Date watcherEnd = new Date(watcher.getEnd());
        binding.beginDate.setText(dateFormat.format(watcherBegin));
        binding.beginTime.setText(timeFormat.format(watcherBegin));
        binding.endDate.setText(dateFormat.format(watcherEnd));
        binding.endTime.setText(timeFormat.format(watcherEnd));
        Date watcherStartAfter = new Date(watcher.getFilters().getStartafter());
        Date watcherStartBefore = new Date(watcher.getFilters().getStartbefore());
        binding.startAfterDate.setText(dateFormat.format(watcherStartAfter));
        binding.startAfterTime.setText(timeFormat.format(watcherStartAfter));
        binding.startBeforeDate.setText(dateFormat.format(watcherStartBefore));
        binding.startBeforeTime.setText(timeFormat.format(watcherStartBefore));

        updateViewsFilters();

        // Buttons
        if(binding != null && binding.toolbar != null && binding.toolbar.getMenu() != null && watcher != null && settings != null) {
            for(int i = 0; i < binding.toolbar.getMenu().size(); i++) {
                if(binding.toolbar.getMenu().getItem(i).getItemId() == R.id.watcherMenuShare || binding.toolbar.getMenu().getItem(i).getItemId() == R.id.watcherMenuDuplicate) {
                    binding.toolbar.getMenu().getItem(i).setVisible(mode == Mode.VIEWING && id != null);
                } else if(binding.toolbar.getMenu().getItem(i).getItemId() == R.id.watcherMenuDelete) {
                    binding.toolbar.getMenu().getItem(i).setVisible(id != null && !id.equals("") && watcher.getUserid() != null
                        && watcher.getUserid().equals(settings.getString("userID", "")));
                }
            }
        }

        binding.fab.setImageResource(mode == Mode.EDITING ? R.drawable.ic_save : R.drawable.ic_edit);

        setFieldsEditable(mode == Mode.EDITING);
    }

    private void updateViewsFilters() {
        updatingViews = true;
        if(watcher.getFilters() != null) {
            boolean experienceCheckIfNoPref = watcher.getFilters().getRegularshowing() != WatcherFilterValue.YES
                    && watcher.getFilters().getImax() != WatcherFilterValue.YES
                    && watcher.getFilters().getDolbycinema() != WatcherFilterValue.YES
                    && watcher.getFilters().get_4dx() != WatcherFilterValue.YES
                    && watcher.getFilters().getScreenx() != WatcherFilterValue.YES;
            binding.filterRegularShowing.setChecked(watcher.getFilters().getRegularshowing() == WatcherFilterValue.NOPREFERENCE ? experienceCheckIfNoPref :
                    watcher.getFilters().getRegularshowing() == WatcherFilterValue.YES);
            binding.filterIMAX.setChecked(watcher.getFilters().getImax() == WatcherFilterValue.NOPREFERENCE ? experienceCheckIfNoPref :
                    watcher.getFilters().getImax() == WatcherFilterValue.YES);
            binding.filterDolbyCinema.setChecked(watcher.getFilters().getDolbycinema() == WatcherFilterValue.NOPREFERENCE ? experienceCheckIfNoPref :
                    watcher.getFilters().getDolbycinema() == WatcherFilterValue.YES);
            binding.filter4DX.setChecked(watcher.getFilters().get_4dx() == WatcherFilterValue.NOPREFERENCE ? experienceCheckIfNoPref :
                    watcher.getFilters().get_4dx() == WatcherFilterValue.YES);
            binding.filterScreenX.setChecked(watcher.getFilters().getScreenx() == WatcherFilterValue.NOPREFERENCE ? experienceCheckIfNoPref :
                    watcher.getFilters().getScreenx() == WatcherFilterValue.YES);
            binding.filter2D.setChecked(watcher.getFilters().get_3d() == WatcherFilterValue.NOPREFERENCE || watcher.getFilters().get_3d() == WatcherFilterValue.NO);
            binding.filter3D.setChecked(watcher.getFilters().get_3d() == WatcherFilterValue.NOPREFERENCE || watcher.getFilters().get_3d() == WatcherFilterValue.YES);
            binding.filter4K.setValue(watcher.getFilters().get_4k() == WatcherFilterValue.NOPREFERENCE ? R.string.watcher_filter_value_nopreference :
                    (watcher.getFilters().get_4k() == WatcherFilterValue.YES ? R.string.watcher_filter_value_yes : R.string.watcher_filter_value_no));
            binding.filterLaser.setValue(watcher.getFilters().getLaser() == WatcherFilterValue.NOPREFERENCE ? R.string.watcher_filter_value_nopreference :
                    (watcher.getFilters().getLaser() == WatcherFilterValue.YES ? R.string.watcher_filter_value_yes : R.string.watcher_filter_value_no));
            binding.filterHFR.setValue(watcher.getFilters().getHfr() == WatcherFilterValue.NOPREFERENCE ? R.string.watcher_filter_value_nopreference :
                    (watcher.getFilters().getHfr() == WatcherFilterValue.YES ? R.string.watcher_filter_value_yes : R.string.watcher_filter_value_no));
            binding.filterDolbyAtmos.setValue(watcher.getFilters().getDolbyatmos() == WatcherFilterValue.NOPREFERENCE ? R.string.watcher_filter_value_nopreference :
                    (watcher.getFilters().getDolbyatmos() == WatcherFilterValue.YES ? R.string.watcher_filter_value_yes : R.string.watcher_filter_value_no));
            binding.filterOV.setValue(watcher.getFilters().getOv() == WatcherFilterValue.NOPREFERENCE ? R.string.watcher_filter_value_nopreference :
                    (watcher.getFilters().getOv() == WatcherFilterValue.YES ? R.string.watcher_filter_value_yes : R.string.watcher_filter_value_no));
            binding.filterNL.setValue(watcher.getFilters().getNl() == WatcherFilterValue.NOPREFERENCE ? R.string.watcher_filter_value_nopreference :
                    (watcher.getFilters().getNl() == WatcherFilterValue.YES ? R.string.watcher_filter_value_yes : R.string.watcher_filter_value_no));
        } else {
            binding.filterRegularShowing.setChecked(true);
            binding.filterIMAX.setChecked(true);
            binding.filterDolbyCinema.setChecked(true);
            binding.filter4DX.setChecked(true);
            binding.filterScreenX.setChecked(true);
            binding.filter2D.setChecked(true);
            binding.filter3D.setChecked(true);
            binding.filter4K.setValue(R.string.watcher_filter_value_nopreference);
            binding.filterLaser.setValue(R.string.watcher_filter_value_nopreference);
            binding.filterHFR.setValue(R.string.watcher_filter_value_nopreference);
            binding.filterDolbyAtmos.setValue(R.string.watcher_filter_value_nopreference);
            binding.filterOV.setValue(R.string.watcher_filter_value_nopreference);
            binding.filterNL.setValue(R.string.watcher_filter_value_nopreference);
        }
        updatingViews = false;
    }

    private void setFieldsEnabled(boolean enabled) {
        binding.watcherNameWrapper.setEnabled(enabled);
        binding.watcherMovieIDWrapper.setEnabled(enabled);
        binding.watcherCinemaIDWrapper.setEnabled(enabled);
    }

    private void setFieldsEditable(boolean editable) {
        binding.watcherName.setFocusable(editable);
        binding.watcherName.setFocusableInTouchMode(editable);
        binding.watcherName.setCursorVisible(editable);
        binding.watcherMovieID.setFocusable(editable);
        binding.watcherMovieID.setFocusableInTouchMode(editable);
        binding.watcherMovieID.setCursorVisible(editable);
        binding.watcherCinemaID.setFocusable(editable);
        binding.watcherCinemaID.setFocusableInTouchMode(editable);
        binding.watcherCinemaID.setCursorVisible(editable);

        binding.watcherStart.setClickable(editable);
        binding.startAfterDate.setClickable(editable);
        binding.startAfterTime.setClickable(editable);
        binding.startBeforeDate.setClickable(editable);
        binding.startBeforeTime.setClickable(editable);
        binding.active.setClickable(editable);
        binding.beginDate.setClickable(editable);
        binding.beginTime.setClickable(editable);
        binding.endDate.setClickable(editable);
        binding.endTime.setClickable(editable);

        binding.filterRegularShowing.setClickable(editable);
        binding.filterRegularShowing.setVisibility(editable ? View.VISIBLE : (binding.filterRegularShowing.isChecked() ? View.VISIBLE : View.GONE));
        binding.filterRegularShowing.setCheckedIconVisible(editable);
        binding.filterIMAX.setClickable(editable);
        binding.filterIMAX.setVisibility(editable ? View.VISIBLE : (binding.filterIMAX.isChecked() ? View.VISIBLE : View.GONE));
        binding.filterIMAX.setCheckedIconVisible(editable);
        binding.filterDolbyCinema.setClickable(editable);
        binding.filterDolbyCinema.setVisibility(editable ? View.VISIBLE : (binding.filterDolbyCinema.isChecked() ? View.VISIBLE : View.GONE));
        binding.filterDolbyCinema.setCheckedIconVisible(editable);
        binding.filter4DX.setClickable(editable);
        binding.filter4DX.setVisibility(editable ? View.VISIBLE : (binding.filter4DX.isChecked() ? View.VISIBLE : View.GONE));
        binding.filter4DX.setCheckedIconVisible(editable);
        binding.filterScreenX.setClickable(editable);
        binding.filterScreenX.setVisibility(editable ? View.VISIBLE : (binding.filterScreenX.isChecked() ? View.VISIBLE : View.GONE));
        binding.filterScreenX.setCheckedIconVisible(editable);
        binding.filter2D.setClickable(editable);
        binding.filter2D.setVisibility(editable ? View.VISIBLE : (binding.filter2D.isChecked() ? View.VISIBLE : View.GONE));
        binding.filter2D.setCheckedIconVisible(editable);
        binding.filter3D.setClickable(editable);
        binding.filter3D.setVisibility(editable ? View.VISIBLE : (binding.filter3D.isChecked() ? View.VISIBLE : View.GONE));
        binding.filter3D.setCheckedIconVisible(editable);
        binding.filter4K.setClickable(editable);
        binding.filterLaser.setClickable(editable);
        binding.filterHFR.setClickable(editable);
        binding.filterDolbyAtmos.setClickable(editable);
        binding.filterOV.setClickable(editable);
        binding.filterNL.setClickable(editable);
    }

    private void doneLoading() {
        binding.progress.setVisibility(View.GONE);
        binding.loaderError.setVisibility(View.GONE);
        binding.loaderErrorButton.setEnabled(true);

        binding.main.setVisibility(View.VISIBLE);
        binding.fab.show();
    }

    @SuppressLint("NewApi")
    private void showDatePicker(boolean checkingValue) {
        MaterialDatePicker.Builder<Pair<Long, Long>> builder = MaterialDatePicker.Builder.dateRangePicker();
        Pair<Long, Long> selectedRange;
        if(checkingValue) {
            selectedRange = new Pair<>(watcher.getBegin(), watcher.getEnd());
        } else {
            selectedRange = new Pair<>(watcher.getFilters().getStartafter(), watcher.getFilters().getStartbefore());
        }

        long startOfFirstDateInMillis = ZonedDateTime.ofInstant(Instant.ofEpochSecond(selectedRange.first/1000), ZoneId.systemDefault())
                .with(ChronoField.HOUR_OF_DAY, 0).with(ChronoField.MINUTE_OF_DAY, 0).with(ChronoField.SECOND_OF_MINUTE, 0)
                .toEpochSecond() * 1000L;
        long startOfTodayInMillis = LocalDate.now().atStartOfDay(ZoneId.systemDefault())
                .toEpochSecond() * 1000L;

        builder.setSelection(selectedRange);
        builder.setCalendarConstraints(new CalendarConstraints.Builder()
                .setStart(Math.min(System.currentTimeMillis() - 1000L, selectedRange.first))
                .setOpenAt(selectedRange.first)
                .setValidator(DateValidatorPointForward.from(Math.min(startOfFirstDateInMillis, startOfTodayInMillis)))
                .build());
        builder.setTitleText(checkingValue ? R.string.watcher_date_title_dialog : R.string.watcher_filter_title_startafter_dialog);
        MaterialDatePicker<Pair<Long, Long>> picker = builder.build();
        picker.addOnPositiveButtonClickListener(selection -> {
            LocalTime startTime, endTime;
            if(checkingValue) {
                startTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(watcher.getBegin()), ZoneId.systemDefault()).toLocalTime();
                endTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(watcher.getEnd()), ZoneId.systemDefault()).toLocalTime();
            } else {
                startTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(watcher.getFilters().getStartafter()), ZoneId.systemDefault()).toLocalTime();
                endTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(watcher.getFilters().getStartbefore()), ZoneId.systemDefault()).toLocalTime();
            }

            Pair<Long, Long> newDates = (Pair<Long, Long>) selection;
            LocalDate startDate = LocalDateTime.ofInstant(Instant.ofEpochMilli(newDates.first), ZoneId.systemDefault()).toLocalDate();
            LocalDate endDate = LocalDateTime.ofInstant(Instant.ofEpochMilli(newDates.second), ZoneId.systemDefault()).toLocalDate();

            long newStart = LocalDateTime.of(startDate, startTime).atZone(ZoneId.systemDefault()).toEpochSecond() * 1000L;
            long newEnd = LocalDateTime.of(endDate, endTime).atZone(ZoneId.systemDefault()).toEpochSecond() * 1000L;

            if(checkingValue) {
                watcher.setBegin(newStart);
                watcher.setEnd(newEnd);
                validateAndFixEnd();
            } else {
                watcher.getFilters().setStartafter(newStart);
                watcher.getFilters().setStartbefore(newEnd);
                validateAndFixStartBefore();
            }

            updateViews();
        });
        picker.show(getSupportFragmentManager(), checkingValue ? "watcherActivePicker" : "watcherStartPicker");
    }

    private void showTimePicker(boolean checkingValue, boolean beginValue, long currentValue) {
        ZonedDateTime current = ZonedDateTime.ofInstant(Instant.ofEpochSecond(currentValue/1000), ZoneId.systemDefault());

        MaterialTimePicker picker = new MaterialTimePicker.Builder()
                .setTimeFormat(android.text.format.DateFormat.is24HourFormat(this) ? TimeFormat.CLOCK_24H : TimeFormat.CLOCK_12H)
                .setHour(current.getHour())
                .setMinute(current.getMinute())
                .build();

        picker.addOnPositiveButtonClickListener(view -> {
            ZonedDateTime newValue = LocalDateTime.of(current.getYear(), current.getMonth(), current.getDayOfMonth(),
                    picker.getHour(), picker.getMinute(), 0).atZone(ZoneId.systemDefault());
            long setTo = newValue.toEpochSecond() * 1000L;

            if(checkingValue) {
                if(beginValue) {
                    watcher.setBegin(setTo);
                    validateAndFixEnd();
                } else {
                    watcher.setEnd(setTo);
                    validateAndFixBegin();
                }
            } else {
                if(beginValue) {
                    watcher.getFilters().setStartafter(setTo);
                    validateAndFixStartBefore();
                } else {
                    watcher.getFilters().setStartbefore(setTo);
                    validateAndFixStartAfter();
                }
            }

            updateViews();
        });

        picker.show(getSupportFragmentManager(), "timePicker");
    }

    private boolean validateName(boolean forced) {
        if(binding.watcherName.getText().toString().length() >= 3 && binding.watcherName.getText().toString().length() <= 50) {
            binding.watcherNameWrapper.setErrorEnabled(false);
            return true;
        } else {
            if(forced) {
                binding.watcherNameWrapper.setError(getString(R.string.watcher_validate_name));
                binding.watcherNameWrapper.setErrorEnabled(true);
            }
            return false;
        }
    }

    private boolean validateMovieID(boolean forced) {
        try {
            if(binding.watcherMovieID.getText().toString().length() > 0 && Integer.parseInt(binding.watcherMovieID.getText().toString()) > 0) {
                binding.watcherMovieIDWrapper.setErrorEnabled(false);
                return true;
            } else {
                if(binding.watcherMovieID.getText().toString().length() == 0 && !forced) {
                    return false;
                }
                binding.watcherMovieIDWrapper.setError(getString(R.string.watcher_validate_movieid));
                binding.watcherMovieIDWrapper.setErrorEnabled(true);
                return false;
            }
        } catch(NumberFormatException e) {
            if(binding.watcherMovieID.getText().toString().length() == 0 && !forced) {
                return false;
            }
            binding.watcherMovieIDWrapper.setError(getString(R.string.watcher_validate_movieid));
            binding.watcherMovieIDWrapper.setErrorEnabled(true);
            return false;
        }
    }

    private boolean validateCinemaID(boolean forced) {
        if(binding.watcherCinemaID.getText().toString().length() > 0) {
            String foundName = binding.watcherCinemaID.getText().toString();
            int foundID = 0;
            if(cinemas != null) {
                for(Cinema cinema : cinemas) {
                    if(cinema.getName().equals(foundName)) {
                        foundID = cinema.getId();
                    }
                }
            }
            if(foundID != 0) {
                watcher.getFilters().setCinemaid(foundID);

                binding.watcherCinemaIDWrapper.setErrorEnabled(false);
                return true;
            } else {
                if(forced) {
                    binding.watcherCinemaIDWrapper.setError(getString(R.string.watcher_validate_cinemaid));
                    binding.watcherCinemaIDWrapper.setErrorEnabled(true);
                } else {
                    watcher.getFilters().setCinemaid(0);
                }
                return false;
            }
        } else {
            if(forced) {
                binding.watcherCinemaIDWrapper.setError(getString(R.string.watcher_validate_cinemaid));
                binding.watcherCinemaIDWrapper.setErrorEnabled(true);
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
            snackbar = Snackbar.make(binding.coordinator, R.string.watcher_validate_begin, Snackbar.LENGTH_LONG);
            snackbar.setAnchorView(binding.fab);
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
            snackbar = Snackbar.make(binding.coordinator, R.string.watcher_validate_end, Snackbar.LENGTH_LONG);
            snackbar.setAnchorView(binding.fab);
            snackbar.show();
            updateViews();
        }
    }

    private void validateAndFixStartAfter() {
        boolean updated = false;

        if(watcher.getFilters().getStartafter() >= watcher.getFilters().getStartbefore()) {
            watcher.getFilters().setStartafter(watcher.getFilters().getStartbefore() - oneMinute);
            updated = true;
        } else if(watcher.getFilters().getStartbefore() - watcher.getFilters().getStartafter() > (oneWeek + oneWeek)) {
            watcher.getFilters().setStartafter(watcher.getFilters().getStartbefore() - oneWeek - oneWeek + oneMinute);
            updated = true;
        }

        if(updated) {
            snackbar = Snackbar.make(binding.coordinator, R.string.watcher_validate_startafter, Snackbar.LENGTH_LONG);
            snackbar.setAnchorView(binding.fab);
            snackbar.show();
            updateViews();
        }
    }

    private void validateAndFixStartBefore() {
        boolean updated = false;

        if(watcher.getFilters().getStartbefore() <= watcher.getFilters().getStartafter()) {
            watcher.getFilters().setStartbefore(watcher.getFilters().getStartafter() + oneMinute);
            updated = true;
        } else if(watcher.getFilters().getStartbefore() - watcher.getFilters().getStartafter() > (oneWeek + oneWeek)) {
            watcher.getFilters().setStartbefore(watcher.getFilters().getStartafter() + oneWeek + oneWeek - oneMinute);
            updated = true;
        }

        if(updated) {
            snackbar = Snackbar.make(binding.coordinator, R.string.watcher_validate_startbefore, Snackbar.LENGTH_LONG);
            snackbar.setAnchorView(binding.fab);
            snackbar.show();
            updateViews();
        }
    }

    private boolean validateAndUpdate3D() {
        if(updatingViews || watcher == null) {
            return false;
        }
        if(watcher.getFilters() == null) {
            watcher.setFilters(new WatcherFilters());
        }
        if(binding.filter2D.isChecked() && !binding.filter3D.isChecked()) {
            watcher.getFilters().set_3d(WatcherFilterValue.NO);
        } else if(!binding.filter2D.isChecked() && binding.filter3D.isChecked()) {
            watcher.getFilters().set_3d(WatcherFilterValue.YES);
        } else {
            watcher.getFilters().set_3d(WatcherFilterValue.NOPREFERENCE);
        }
        return true;
    }

    private boolean validateAndUpdateExperiences() {
        if(updatingViews || watcher == null) {
            return false;
        }

        int numberChecked = 0;
        List<Chip> experiences = Arrays.asList(binding.filterRegularShowing, binding.filterIMAX, binding.filterDolbyCinema, binding.filter4DX, binding.filterScreenX);
        for(int i = 0; i < experiences.size(); i++) {
            if(experiences.get(i).isChecked()) {
                numberChecked++;
            }
        }
        if(watcher.getFilters() == null) {
            watcher.setFilters(new WatcherFilters());
        }
        WatcherFilterValue useForChecked = (numberChecked == 1 ? WatcherFilterValue.YES : WatcherFilterValue.NOPREFERENCE);
        WatcherFilterValue useForNotChecked = (numberChecked <= 1 ? WatcherFilterValue.NOPREFERENCE : WatcherFilterValue.NO);
        watcher.getFilters().setRegularshowing(binding.filterRegularShowing.isChecked() ? useForChecked : useForNotChecked);
        watcher.getFilters().setImax(binding.filterIMAX.isChecked() ? useForChecked : useForNotChecked);
        watcher.getFilters().setDolbycinema(binding.filterDolbyCinema.isChecked() ? useForChecked : useForNotChecked);
        watcher.getFilters().set_4dx(binding.filter4DX.isChecked() ? useForChecked : useForNotChecked);
        watcher.getFilters().setScreenx(binding.filterScreenX.isChecked() ? useForChecked : useForNotChecked);
        return true;
    }

    private void saveWatcher() {
        if(validateName(true) && validateMovieID(true) && validateCinemaID(true)
                && validateAndUpdate3D() && validateAndUpdateExperiences()) {
            binding.fab.setEnabled(false);
            binding.progress.setVisibility(View.VISIBLE);
            binding.watcherError.setVisibility(View.GONE);
            setFieldsEnabled(false);

            Watcher toSave = new Watcher();
            toSave.setName(binding.watcherName.getText().toString());
            toSave.setMovieid(Integer.parseInt(binding.watcherMovieID.getText().toString()));
            toSave.setBegin(watcher.getBegin());
            toSave.setEnd(watcher.getEnd());

            toSave.setFilters(watcher.getFilters()); // Validation set the cinema ID correctly

            Call<Watcher> call;

            if(id == null || id.equals("") || !watcher.getUserid().equals(settings.getString("userID", ""))) { // Create
                toSave.setUserid(settings.getString("userID", ""));
                call = APIHelper.INSTANCE.getInstance().addWatcher(settings.getString("userAPIKey", ""), toSave);
            } else { // Update
                call = APIHelper.INSTANCE.getInstance().updateWatcher(settings.getString("userAPIKey", ""), id, toSave);
            }

            call.enqueue(new Callback<Watcher>() {
                @Override
                public void onResponse(@NonNull Call<Watcher> call, @NonNull Response<Watcher> response) {
                    binding.fab.setEnabled(true);
                    binding.progress.setVisibility(View.GONE);
                    setFieldsEnabled(true);

                    if(response.code() == 200) {
                        watcher = response.body();
                        id = watcher.getId();

                        mode = Mode.VIEWING;

                        snackbar = Snackbar.make(binding.coordinator, R.string.watcher_saved, Snackbar.LENGTH_SHORT);
                        snackbar.setAnchorView(binding.fab);
                        snackbar.show();

                        updateViews();
                        doneLoading();
                    } else {
                        setFieldsEditable(true);

                        binding.watcherError.setText(ErrorUtil.getErrorMessage(WatcherActivity.this, response));
                        binding.watcherError.setVisibility(View.VISIBLE);

                        binding.main.smoothScrollTo(0, 0);
                    }
                }

                @Override
                public void onFailure(@NonNull Call<Watcher> call, @NonNull Throwable t) {
                    t.printStackTrace();

                    binding.fab.setEnabled(true);
                    binding.progress.setVisibility(View.GONE);
                    setFieldsEnabled(true);

                    binding.watcherError.setText(ErrorUtil.getErrorMessage(WatcherActivity.this, null));
                    binding.watcherError.setVisibility(View.VISIBLE);

                    binding.main.smoothScrollTo(0, 0);
                }
            });
        }
    }

    private void deleteWatcher() {
        binding.fab.setEnabled(false);
        binding.progress.setVisibility(View.VISIBLE);
        binding.watcherError.setVisibility(View.GONE);
        setFieldsEnabled(false);

        Call<ResponseBody> call = APIHelper.INSTANCE.getInstance().deleteWatcher(settings.getString("userAPIKey", ""), id);
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                if(response.code() == 200) {
                    WatcherActivity.this.finish();
                } else {
                    binding.fab.setEnabled(true);
                    binding.progress.setVisibility(View.GONE);
                    setFieldsEnabled(true);

                    if(response.code() == 400) {
                        binding.watcherError.setText(R.string.error_watcher_400);
                    } else if(response.code() == 401) {
                        binding.watcherError.setText(R.string.error_watcher_401);
                    } else {
                        binding.watcherError.setText(getString(R.string.error_general_server, "H" + response.code()));
                    }

                    binding.watcherError.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                t.printStackTrace();

                binding.fab.setEnabled(true);
                binding.progress.setVisibility(View.GONE);
                binding.watcherError.setText(R.string.error_general_exception);
                binding.watcherError.setVisibility(View.VISIBLE);
                setFieldsEnabled(true);
            }
        });
    }

    private void shareWatcher() {
        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, BuildConfig.SERVER_BASE_URL + "w/" + id);
        sendIntent.putExtra(Intent.EXTRA_TITLE, watcher.getName());
        sendIntent.setType("text/plain");
        startActivity(Intent.createChooser(sendIntent, getString(R.string.watcher_share)));
    }

    private void duplicateWatcher() {
        binding.progress.setVisibility(View.VISIBLE);
        binding.watcherError.setVisibility(View.GONE);

        snackbar = Snackbar.make(binding.coordinator, R.string.watcher_duplicate, Snackbar.LENGTH_INDEFINITE);
        snackbar.setAnchorView(binding.fab);
        snackbar.show();

        watcher.setName(getString(R.string.watcher_copy, watcher.getName()));

        id = null;
        mode = Mode.EDITING;
        binding.main.smoothScrollTo(0, 0);
        doneLoading();
        updateViews();

        binding.watcherName.requestFocus();
        InterfaceUtil.showKeyboard(WatcherActivity.this, binding.watcherName);
    }

    private void setOnPropClickListener(final View click, final PropResultListener callback) {
        click.setOnClickListener(view -> {
            InterfaceUtil.clearFocus(WatcherActivity.this); // Prevent scroll after menu close due to focusing again

            PopupMenu popupMenu = new PopupMenu(WatcherActivity.this, click);
            popupMenu.getMenuInflater().inflate(R.menu.menu_prop, popupMenu.getMenu());
            popupMenu.setOnMenuItemClickListener(item -> {
                if(watcher.getFilters() == null) {
                    watcher.setFilters(new WatcherFilters());
                }
                switch(item.getItemId()) {
                    case R.id.filter_yes:
                        callback.gotResult(WatcherFilterValue.YES);
                        break;
                    case R.id.filter_no:
                        callback.gotResult(WatcherFilterValue.NO);
                        break;
                    case R.id.filter_nopreference:
                    default:
                        callback.gotResult(WatcherFilterValue.NOPREFERENCE);
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

    private interface PropResultListener {
        void gotResult(WatcherFilterValue value);
    }

    private void askForLocation() {
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            settings.edit().putInt("prefAutocompleteLocation", 1).apply();
            binding.autocompleteSuggestion.setVisibility(View.GONE);
            startLocation();
        } else {
            if(!isFinishing()) {
                if(ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                    snackbar = Snackbar.make(binding.coordinator, R.string.settings_general_location_permission_rationale, Snackbar.LENGTH_LONG);
                    snackbar.setAction(R.string.ok, view -> ActivityCompat.requestPermissions(WatcherActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_LOCATION_AUTOCOMPLETE));
                    snackbar.setAnchorView(binding.fab);
                    snackbar.show();
                } else {
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_LOCATION_AUTOCOMPLETE);
                }
            }
        }
    }

    private void startLocation() {
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
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
                    binding.autocompleteSuggestion.setVisibility(View.GONE);
                } else {
                    snackbarForPermission = true;
                    snackbar = Snackbar.make(binding.coordinator, R.string.settings_general_location_permission_denied, Snackbar.LENGTH_LONG);
                    snackbar.setAnchorView(binding.fab);
                    if(ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                        snackbar.setAction(R.string.settings_general_location_permission_systemsettings, view -> ActivityCompat.requestPermissions(WatcherActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_LOCATION_AUTOCOMPLETE));
                    } else {
                        Intent settingsIntent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + getPackageName()));
                        settingsIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        snackbar.setAction(R.string.settings_general_location_permission_systemsettings, view -> startActivity(settingsIntent));
                    }
                    snackbar.show();
                    binding.autocompleteSuggestion.setVisibility(settings.getInt("prefAutocompleteLocation", -1) == 0 ? View.GONE : View.VISIBLE);
                }
                break;
            }
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
                break;
        }
    }
}
