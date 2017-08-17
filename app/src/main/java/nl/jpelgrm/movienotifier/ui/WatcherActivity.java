package nl.jpelgrm.movienotifier.ui;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputLayout;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatEditText;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Patterns;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.TimePicker;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.apache.commons.lang3.text.WordUtils;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import butterknife.BindView;
import butterknife.ButterKnife;
import nl.jpelgrm.movienotifier.BuildConfig;
import nl.jpelgrm.movienotifier.R;
import nl.jpelgrm.movienotifier.data.APIHelper;
import nl.jpelgrm.movienotifier.models.Props;
import nl.jpelgrm.movienotifier.models.Watcher;
import nl.jpelgrm.movienotifier.models.error.Errors;
import nl.jpelgrm.movienotifier.ui.settings.AccountActivity;
import nl.jpelgrm.movienotifier.util.InterfaceUtil;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class WatcherActivity extends AppCompatActivity {
    enum Mode {
        VIEWING, EDITING
    }

    @BindView(R.id.coordinator) CoordinatorLayout coordinator;

    @BindView(R.id.toolbar) Toolbar toolbar;

    @BindView(R.id.progress) ProgressBar progress;
    @BindView(R.id.main) ScrollView main;
    @BindView(R.id.loaderError) LinearLayout loaderError;
    @BindView(R.id.loaderErrorText) TextView loaderErrorText;
    @BindView(R.id.loaderErrorButton) Button loaderErrorButton;

    @BindView(R.id.error) TextView watcherError;
    @BindView(R.id.watcherNameWrapper) TextInputLayout watcherNameWrapper;
    @BindView(R.id.watcherName) AppCompatEditText watcherName;
    @BindView(R.id.watcherMovieIDWrapper) TextInputLayout watcherMovieIDWrapper;
    @BindView(R.id.watcherMovieID) AppCompatEditText watcherMovieID;
    @BindView(R.id.watcherCinemaIDWrapper) TextInputLayout watcherCinemaIDWrapper;
    @BindView(R.id.watcherCinemaID) AppCompatEditText watcherCinemaID;

    @BindView(R.id.startAfter) RelativeLayout startAfter;
    @BindView(R.id.startAfterValue) TextView startAfterValue;
    @BindView(R.id.startBefore) RelativeLayout startBefore;
    @BindView(R.id.startBeforeValue) TextView startBeforeValue;

    @BindView(R.id.propIMAX) RelativeLayout propIMAX;
    @BindView(R.id.propIMAXValue) TextView propIMAXValue;
    @BindView(R.id.propDolbyCinema) RelativeLayout propDolbyCinema;
    @BindView(R.id.propDolbyCinemaValue) TextView propDolbyCinemaValue;
    @BindView(R.id.prop3D) RelativeLayout prop3D;
    @BindView(R.id.prop3DValue) TextView prop3DValue;
    @BindView(R.id.prop4K) RelativeLayout prop4K;
    @BindView(R.id.prop4KValue) TextView prop4KValue;
    @BindView(R.id.propLaser) RelativeLayout propLaser;
    @BindView(R.id.propLaserValue) TextView propLaserValue;
    @BindView(R.id.propHFR) RelativeLayout propHFR;
    @BindView(R.id.propHFRValue) TextView propHFRValue;
    @BindView(R.id.propDolbyAtmos) RelativeLayout propAtmos;
    @BindView(R.id.propDolbyAtmosValue) TextView propAtmosValue;
    @BindView(R.id.propOV) RelativeLayout propOV;
    @BindView(R.id.propOVValue) TextView propOVValue;
    @BindView(R.id.propNL) RelativeLayout propNL;
    @BindView(R.id.propNLValue) TextView propNLValue;
    @BindView(R.id.propDBOX) RelativeLayout propDBOX;
    @BindView(R.id.propDBOXValue) TextView propDBOXValue;

    @BindView(R.id.fab) FloatingActionButton fab;

    private Snackbar snackbar;

    private SharedPreferences settings;

    private Watcher watcher;
    private String uuid;

    private String sharedTitle;
    private Integer sharedMovieID;
    private static Long oneWeek = 60 * 60 * 24 * 7 * 1000L;

    private Mode mode = Mode.VIEWING;

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
                    watcher.setMovieid(Integer.parseInt(editable.toString()));
                }
            }
        });
        watcherCinemaID.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }
            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }
            @Override
            public void afterTextChanged(Editable editable) {
                if(watcher != null) {
                    watcher.setCinemaid(editable.toString());
                }
            }
        });

        startAfter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                InterfaceUtil.clearForcus(WatcherActivity.this); // Prevent scroll after popup close due to focusing again
                showDateTimePicker(true, watcher.getStartAfter() != null && !watcher.getStartAfter().equals("") ? Long.parseLong(watcher.getStartAfter()) : System.currentTimeMillis());
            }
        });
        startBefore.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                InterfaceUtil.clearForcus(WatcherActivity.this); // Prevent scroll after popup close due to focusing again
                showDateTimePicker(false, watcher.getStartBefore() != null && !watcher.getStartBefore().equals("") ? Long.parseLong(watcher.getStartBefore()) : System.currentTimeMillis());
            }
        });

        setOnPropClickListener(propIMAX, new PropResultListener() {
            @Override
            public void gotResult(Boolean value) {
                watcher.getProps().setIMAX(value);
            }
        });
        setOnPropClickListener(propDolbyCinema, new PropResultListener() {
            @Override
            public void gotResult(Boolean value) {
                watcher.getProps().setDolbyCinema(value);
            }
        });
        setOnPropClickListener(prop3D, new PropResultListener() {
            @Override
            public void gotResult(Boolean value) {
                watcher.getProps().set3D(value);
            }
        });
        setOnPropClickListener(prop4K, new PropResultListener() {
            @Override
            public void gotResult(Boolean value) {
                watcher.getProps().set4K(value);
            }
        });
        setOnPropClickListener(propLaser, new PropResultListener() {
            @Override
            public void gotResult(Boolean value) {
                watcher.getProps().setLaser(value);
            }
        });
        setOnPropClickListener(propHFR, new PropResultListener() {
            @Override
            public void gotResult(Boolean value) {
                watcher.getProps().setHFR(value);
            }
        });
        setOnPropClickListener(propAtmos, new PropResultListener() {
            @Override
            public void gotResult(Boolean value) {
                watcher.getProps().setDolbyAtmos(value);
            }
        });
        setOnPropClickListener(propOV, new PropResultListener() {
            @Override
            public void gotResult(Boolean value) {
                watcher.getProps().setIsOriginalVersion(value);
            }
        });
        setOnPropClickListener(propNL, new PropResultListener() {
            @Override
            public void gotResult(Boolean value) {
                watcher.getProps().setIsDutchVersion(value);
            }
        });
        setOnPropClickListener(propDBOX, new PropResultListener() {
            @Override
            public void gotResult(Boolean value) {
                watcher.getProps().setDBOX(value);
            }
        });

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(mode == Mode.VIEWING) {
                    mode = Mode.EDITING;
                    updateViews();

                    watcherName.requestFocus();
                    InterfaceUtil.showKeyboard(WatcherActivity.this, watcherName);
                } else {
                    InterfaceUtil.hideKeyboard(WatcherActivity.this);

                    if(!settings.getString("userID", "").equals("")) {
                        saveWatcher();
                    } else {
                        snackbar = Snackbar.make(coordinator, R.string.watchers_empty_account, Snackbar.LENGTH_INDEFINITE);
                        snackbar.setAction(R.string.add, new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                startActivity(new Intent(WatcherActivity.this, AccountActivity.class));
                            }
                        });
                        snackbar.show();
                    }
                }
            }
        });

        loaderErrorButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                loaderErrorButton.setEnabled(false);
                progress.setVisibility(View.VISIBLE);
                setupWatcher();
            }
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

            if(Patterns.WEB_URL.matcher(data).matches()) {
                Uri received = Uri.parse(data);
                Uri instance = Uri.parse(BuildConfig.SERVER_BASE_URL);

                if(received.getHost().equals(instance.getHost())) {
                    if(received.getPathSegments().size() == 2 && received.getPathSegments().get(1) != null && !received.getPathSegments().get(1).equals("")) {
                        uuid = received.getPathSegments().get(1);
                    }
                } else if(received.getHost().equals(getString(R.string.CINEMA_HOST))) {
                    if(received.getPathSegments().size() == 3 && received.getPathSegments().get(1) != null && !received.getPathSegments().get(1).equals("")
                            && received.getPathSegments().get(2) != null && !received.getPathSegments().get(2).equals("")) {
                        sharedMovieID = Integer.parseInt(received.getPathSegments().get(1));
                        sharedTitle = WordUtils.capitalizeFully(received.getPathSegments().get(2).replace("-", " "));
                    }
                }
            }
        }
    }

    private void setupWatcher() {
        if(getIntent().getExtras() != null && !getIntent().getExtras().getString("uuid", "").equals("")) {
            uuid = getIntent().getExtras().getString("uuid");
            getWatcher();
        } else if(uuid != null) {
            // UUID is already set
            getWatcher();
        } else {
            uuid = null;
            watcher = new Watcher();
            watcher.setProps(new Props());

            if(sharedTitle != null && !sharedTitle.equals("")) {
                watcher.setName(sharedTitle);
            }
            if(sharedMovieID != null && sharedMovieID > 0) {
                watcher.setMovieid(sharedMovieID);
            }
            watcher.setStartAfter(String.valueOf(System.currentTimeMillis() + oneWeek));
            watcher.setStartBefore(String.valueOf(System.currentTimeMillis() + oneWeek + oneWeek));

            mode = Mode.EDITING;

            updateViews();
            doneLoading();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        settings = getSharedPreferences("settings", MODE_PRIVATE);

        if(snackbar != null && snackbar.isShown()) {
            snackbar.dismiss();
        }
    }

    private void getWatcher() {
        Call<Watcher> call = APIHelper.getInstance().getWatcher(settings.getString("userAPIKey", ""), uuid);
        call.enqueue(new Callback<Watcher>() {
            @Override
            public void onResponse(Call<Watcher> call, Response<Watcher> response) {
                if(response.code() == 200) {
                    watcher = response.body();
                    uuid = watcher.getUuid();

                    if(watcher.getUser().equals(settings.getString("userID", ""))) {
                        mode = Mode.VIEWING;
                    } else {
                        mode = Mode.EDITING;
                    }

                    updateViews();
                    doneLoading();
                } else {
                    progress.setVisibility(View.GONE);

                    if(response.code() == 400) {
                        loaderErrorText.setText(R.string.error_watcher_400);
                        loaderErrorButton.setVisibility(View.GONE);
                    } else {
                        if(response.code() == 401) {
                            loaderErrorText.setText(R.string.error_watcher_401);
                        } else {
                            loaderErrorText.setText(getString(R.string.error_general_server, "H" + response.code()));
                        }

                        loaderErrorButton.setEnabled(true);
                        loaderErrorButton.setVisibility(View.VISIBLE);
                    }

                    loaderError.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onFailure(Call<Watcher> call, Throwable t) {
                t.printStackTrace();

                progress.setVisibility(View.GONE);

                loaderErrorText.setText(R.string.error_general_exception);
                loaderErrorButton.setEnabled(true);
                loaderErrorButton.setVisibility(View.VISIBLE);
                loaderError.setVisibility(View.VISIBLE);
            }
        });
    }

    private void updateViews() {
        setFieldsEditable(mode == Mode.EDITING);

        // Errors
        loaderError.setVisibility(View.GONE);
        watcherError.setVisibility(View.GONE);

        // Input values
        watcherName.setText(watcher.getName());
        watcherMovieID.setText(watcher.getMovieid() == null ? "" : String.valueOf(watcher.getMovieid()));
        watcherCinemaID.setText(watcher.getCinemaid());

        DateFormat format = SimpleDateFormat.getDateTimeInstance(java.text.DateFormat.MEDIUM, java.text.DateFormat.SHORT);
        startAfterValue.setText(format.format(new Date(Long.parseLong(watcher.getStartAfter()))));
        startBeforeValue.setText(format.format(new Date(Long.parseLong(watcher.getStartBefore()))));

        updateViewsProps();

        // Buttons
        if(toolbar != null && toolbar.getMenu() != null && watcher != null && settings != null) {
            for(int i = 0; i < toolbar.getMenu().size(); i++) {
                if(toolbar.getMenu().getItem(i).getItemId() == R.id.watcherMenuShare) {
                    toolbar.getMenu().getItem(i).setVisible(mode == Mode.VIEWING);
                } else if(toolbar.getMenu().getItem(i).getItemId() == R.id.watcherMenuDelete) {
                    toolbar.getMenu().getItem(i).setVisible(uuid != null && !uuid.equals("") && watcher.getUser() != null
                        && watcher.getUser().equals(settings.getString("userID", "")));
                }
            }
        }

        fab.setImageDrawable(ContextCompat.getDrawable(this, mode == Mode.EDITING ? R.drawable.ic_save : R.drawable.ic_edit));
    }

    private void updateViewsProps() {
        if(watcher.getProps() != null) {
            propIMAXValue.setText(watcher.getProps().isIMAX() == null ? R.string.watcher_prop_value_null : (watcher.getProps().isIMAX() ? R.string.watcher_prop_value_true : R.string.watcher_prop_value_false));
            propDolbyCinemaValue.setText(watcher.getProps().isDolbyCinema() == null ? R.string.watcher_prop_value_null : (watcher.getProps().isDolbyCinema() ? R.string.watcher_prop_value_true : R.string.watcher_prop_value_false));
            prop3DValue.setText(watcher.getProps().is3D() == null ? R.string.watcher_prop_value_null : (watcher.getProps().is3D() ? R.string.watcher_prop_value_true : R.string.watcher_prop_value_false));
            prop4KValue.setText(watcher.getProps().is4K() == null ? R.string.watcher_prop_value_null : (watcher.getProps().is4K() ? R.string.watcher_prop_value_true : R.string.watcher_prop_value_false));
            propLaserValue.setText(watcher.getProps().isLaser() == null ? R.string.watcher_prop_value_null : (watcher.getProps().isLaser() ? R.string.watcher_prop_value_true : R.string.watcher_prop_value_false));
            propHFRValue.setText(watcher.getProps().isHFR() == null ? R.string.watcher_prop_value_null : (watcher.getProps().isHFR() ? R.string.watcher_prop_value_true : R.string.watcher_prop_value_false));
            propAtmosValue.setText(watcher.getProps().isDolbyAtmos() == null ? R.string.watcher_prop_value_null : (watcher.getProps().isDolbyAtmos() ? R.string.watcher_prop_value_true : R.string.watcher_prop_value_false));
            propOVValue.setText(watcher.getProps().isOriginalVersion() == null ? R.string.watcher_prop_value_null : (watcher.getProps().isOriginalVersion() ? R.string.watcher_prop_value_true : R.string.watcher_prop_value_false));
            propNLValue.setText(watcher.getProps().isDutchVersion() == null ? R.string.watcher_prop_value_null : (watcher.getProps().isDutchVersion() ? R.string.watcher_prop_value_true : R.string.watcher_prop_value_false));
            propDBOXValue.setText(watcher.getProps().isDBOX() == null ? R.string.watcher_prop_value_null : (watcher.getProps().isDBOX() ? R.string.watcher_prop_value_true : R.string.watcher_prop_value_false));
        } else {
            propIMAXValue.setText(R.string.watcher_prop_value_null);
            propDolbyCinemaValue.setText(R.string.watcher_prop_value_null);
            prop3DValue.setText(R.string.watcher_prop_value_null);
            prop4KValue.setText(R.string.watcher_prop_value_null);
            propLaserValue.setText(R.string.watcher_prop_value_null);
            propHFRValue.setText(R.string.watcher_prop_value_null);
            propAtmosValue.setText(R.string.watcher_prop_value_null);
            propOVValue.setText(R.string.watcher_prop_value_null);
            propNLValue.setText(R.string.watcher_prop_value_null);
            propDBOXValue.setText(R.string.watcher_prop_value_null);
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

        startAfter.setClickable(editable);
        startBefore.setClickable(editable);

        propIMAX.setClickable(editable);
        propDolbyCinema.setClickable(editable);
        prop3D.setClickable(editable);
        prop4K.setClickable(editable);
        propLaser.setClickable(editable);
        propHFR.setClickable(editable);
        propAtmos.setClickable(editable);
        propOV.setClickable(editable);
        propNL.setClickable(editable);
        propDBOX.setClickable(editable);
    }

    private void doneLoading() {
        progress.setVisibility(View.GONE);
        loaderError.setVisibility(View.GONE);
        loaderErrorButton.setEnabled(true);

        main.setVisibility(View.VISIBLE);
        fab.setVisibility(View.VISIBLE);
    }

    private void showDateTimePicker(final boolean startAfterValue, long currentValue) {
        final Calendar current = Calendar.getInstance();
        current.setTimeInMillis(currentValue);

        DatePickerDialog datePickerDialog = new DatePickerDialog(this, new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker datePicker, int year, int month, int day) {
                final int mYear = year;
                final int mMonth = month;
                final int mDay = day;
                TimePickerDialog timePickerDialog = new TimePickerDialog(WatcherActivity.this, new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker timePicker, int hour, int minute) {
                        Calendar setTo = Calendar.getInstance();
                        setTo.set(mYear, mMonth, mDay, hour, minute, 0);
                        if(startAfterValue) {
                            watcher.setStartAfter(String.valueOf(setTo.getTimeInMillis()));
                        } else {
                            watcher.setStartBefore(String.valueOf(setTo.getTimeInMillis()));
                        }

                        updateViews();
                    }
                }, current.get(Calendar.HOUR_OF_DAY), current.get(Calendar.MINUTE), android.text.format.DateFormat.is24HourFormat(WatcherActivity.this));
                timePickerDialog.show();
            }
        }, current.get(Calendar.YEAR), current.get(Calendar.MONTH), current.get(Calendar.DAY_OF_MONTH));
        datePickerDialog.show();
    }

    private boolean validateName(boolean forced) {
        if(watcherName.getText().toString().length() > 0) {
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

    private void saveWatcher() {
        if(validateName(true) && validateMovieID(true)) {
            fab.setEnabled(false);
            progress.setVisibility(View.VISIBLE);
            watcherError.setVisibility(View.GONE);
            setFieldsEnabled(false);

            Watcher toSave = new Watcher();
            toSave.setName(watcherName.getText().toString());
            toSave.setMovieid(Integer.parseInt(watcherMovieID.getText().toString()));
            toSave.setCinemaid(watcherCinemaID.getText().toString());

            toSave.setStartAfter(watcher.getStartAfter());
            toSave.setStartBefore(watcher.getStartBefore());

            toSave.setProps(watcher.getProps() != null ? watcher.getProps() : new Props());

            Call<Watcher> call;

            if(uuid == null || uuid.equals("") || !watcher.getUser().equals(settings.getString("userID", ""))) { // Create
                toSave.setUser(settings.getString("userID", ""));
                call = APIHelper.getInstance().addWatcher(settings.getString("userAPIKey", ""), toSave);
            } else { // Update
                call = APIHelper.getInstance().updateWatcher(settings.getString("userAPIKey", ""), uuid, toSave);
            }

            call.enqueue(new Callback<Watcher>() {
                @Override
                public void onResponse(Call<Watcher> call, Response<Watcher> response) {
                    fab.setEnabled(true);
                    progress.setVisibility(View.GONE);
                    setFieldsEnabled(true);

                    if(response.code() == 200) {
                        watcher = response.body();
                        uuid = watcher.getUuid();

                        mode = Mode.VIEWING;

                        snackbar = Snackbar.make(coordinator, R.string.watcher_saved, Snackbar.LENGTH_SHORT);
                        snackbar.show();

                        updateViews();
                        doneLoading();
                    } else {
                        setFieldsEditable(true);
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

                            watcherError.setText(errorBuilder.toString());
                        } else {
                            watcherError.setText(getString(R.string.error_general_server, "H" + response.code()));
                        }
                        watcherError.setVisibility(View.VISIBLE);
                    }
                }

                @Override
                public void onFailure(Call<Watcher> call, Throwable t) {
                    t.printStackTrace();

                    fab.setEnabled(true);
                    progress.setVisibility(View.GONE);
                    watcherError.setText(R.string.error_general_exception);
                    watcherError.setVisibility(View.VISIBLE);
                    setFieldsEnabled(true);
                }
            });
        }
    }

    private void deleteWatcher() {
        fab.setEnabled(false);
        progress.setVisibility(View.VISIBLE);
        watcherError.setVisibility(View.GONE);
        setFieldsEnabled(false);

        Call<ResponseBody> call = APIHelper.getInstance().deleteWatcher(settings.getString("userAPIKey", ""), uuid);
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
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
            public void onFailure(Call<ResponseBody> call, Throwable t) {
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
        sendIntent.putExtra(Intent.EXTRA_TEXT, BuildConfig.SERVER_BASE_URL + "w/" + uuid);
        sendIntent.setType("text/plain");
        startActivity(Intent.createChooser(sendIntent, getString(R.string.watcher_share)));
    }

    private void setOnPropClickListener(final View click, final PropResultListener callback) {
        click.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                InterfaceUtil.clearForcus(WatcherActivity.this); // Prevent scroll after menu close due to focusing again

                PopupMenu popupMenu = new PopupMenu(WatcherActivity.this, click);
                popupMenu.getMenuInflater().inflate(R.menu.menu_prop, popupMenu.getMenu());
                popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        if(watcher.getProps() == null) {
                            watcher.setProps(new Props());
                        }
                        switch(item.getItemId()) {
                            case R.id.prop_true:
                                callback.gotResult(true);
                                break;
                            case R.id.prop_false:
                                callback.gotResult(false);
                                break;
                            case R.id.prop_null:
                            default:
                                callback.gotResult(null);
                                break;
                        }
                        updateViews();
                        return true;
                    }
                });
                popupMenu.show();
            }
        });
    }

    @Override
    public void onBackPressed() {
        if(mode == Mode.EDITING) {
            if (watcher == null) {
                super.onBackPressed();
            } else {
                InterfaceUtil.hideKeyboard(this);
                new AlertDialog.Builder(this).setMessage(R.string.watcher_discard).setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        WatcherActivity.super.onBackPressed();
                    }
                }).setNegativeButton(R.string.no, null).show();
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
            case R.id.watcherMenuDelete:
                new AlertDialog.Builder(this).setMessage(R.string.watcher_delete).setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                        deleteWatcher();
                    }
                }).setNegativeButton(R.string.no, null).show();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private abstract class PropResultListener {
        public abstract void gotResult(Boolean value);
    }
}
