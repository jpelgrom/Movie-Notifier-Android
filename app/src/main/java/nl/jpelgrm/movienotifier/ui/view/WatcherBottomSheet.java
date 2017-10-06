package nl.jpelgrm.movienotifier.ui.view;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.NfcEvent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetDialogFragment;
import android.support.text.emoji.widget.EmojiAppCompatTextView;
import android.support.v4.app.Fragment;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import nl.jpelgrm.movienotifier.BuildConfig;
import nl.jpelgrm.movienotifier.R;
import nl.jpelgrm.movienotifier.models.Cinema;
import nl.jpelgrm.movienotifier.models.Watcher;
import nl.jpelgrm.movienotifier.ui.WatcherActivity;
import nl.jpelgrm.movienotifier.ui.WatchersFragment;

public class WatcherBottomSheet extends BottomSheetDialogFragment {
    private Watcher watcher;

    @BindView(R.id.name) EmojiAppCompatTextView name;
    @BindView(R.id.location) TextView location;
    @BindView(R.id.active) EmojiAppCompatTextView active;
    @BindView(R.id.dates) TextView dates;
    @BindView(R.id.view) LinearLayout view;
    @BindView(R.id.share) LinearLayout share;
    @BindView(R.id.delete) LinearLayout delete;
    @BindView(R.id.close) Button close;

    private List<Cinema> cinemas = null;

    private NfcAdapter nfcAdapter;

    public static WatcherBottomSheet newInstance(Watcher watcher) {
        WatcherBottomSheet fragment = new WatcherBottomSheet();
        Bundle args = new Bundle();
        args.putString("watcher", new Gson().toJson(watcher));
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        readCinemasJson();

        watcher = new Gson().fromJson(getArguments().getString("watcher"), Watcher.class);

        nfcAdapter = NfcAdapter.getDefaultAdapter(getActivity());
        if(nfcAdapter != null) {
            nfcAdapter.setNdefPushMessageCallback(new NfcAdapter.CreateNdefMessageCallback() {
                @Override
                public NdefMessage createNdefMessage(NfcEvent nfcEvent) {
                    return new NdefMessage(
                            new NdefRecord[] {
                                    NdefRecord.createUri(BuildConfig.SERVER_BASE_URL + "w/" + watcher.getID()),
                                    NdefRecord.createApplicationRecord(BuildConfig.APPLICATION_ID)
                            }
                    );
                }
            }, getActivity());
        }
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        dismissalTask();
        super.onDismiss(dialog);
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        dismissalTask();
        super.onCancel(dialog);
    }

    private void dismissalTask() {
        if(nfcAdapter != null) {
            nfcAdapter.setNdefPushMessageCallback(new NfcAdapter.CreateNdefMessageCallback() {
                @Override
                public NdefMessage createNdefMessage(NfcEvent nfcEvent) {
                    return new NdefMessage(
                            new NdefRecord[] {
                                    NdefRecord.createUri("https://play.google.com/store/apps/details?id=" + BuildConfig.APPLICATION_ID + "&feature=beam"),
                                    NdefRecord.createUri(BuildConfig.APPLICATION_ID)
                            }
                    );
                }
            }, getActivity()); // Don't keep sharing this watcher when the sheet is dismissed, return something similar to Android Beam default
        }
    }

    @Override
    public void setupDialog(Dialog dialog, int style) {
        super.setupDialog(dialog, style);
        View view = View.inflate(getContext(), R.layout.fragment_bottomsheet_watcher, null);
        ButterKnife.bind(this, view);
        dialog.setContentView(view);

        setupViews();
    }

    private void setupViews() {
        name.setText(watcher.getName());

        String foundCinema = "";
        if(cinemas != null) {
            for(Cinema cinema : cinemas) {
                if(cinema.getId().equals(watcher.getFilters().getCinemaID())) {
                    foundCinema = cinema.getName();
                }
            }
        }
        if(foundCinema.equals("")) { // We don't know this cinema ID's display name
            foundCinema = watcher.getFilters().getCinemaID();
        }
        location.setText(foundCinema);

        DateFormat format = SimpleDateFormat.getDateTimeInstance(java.text.DateFormat.MEDIUM, java.text.DateFormat.SHORT);

        String activeEmoji;
        if(watcher.getBegin() <= System.currentTimeMillis() && watcher.getEnd() > System.currentTimeMillis()) {
            activeEmoji = "\uD83D\uDD34"; // Red Circle ('live', active watcher)
            active.setText(getContext().getString(R.string.watchers_bottomsheet_watcher_active_now, activeEmoji, format.format(new Date(watcher.getEnd()))));
        } else if(watcher.getEnd() < System.currentTimeMillis()) {
            activeEmoji = "\uD83D\uDCE6"; // Package ('archive', watcher is done and will not be active again)
            active.setText(getContext().getString(R.string.watchers_bottomsheet_watcher_active_past, activeEmoji, format.format(new Date(watcher.getEnd()))));
        } else {
            activeEmoji = "⏰"; // Alarm Clock ('planned', watcher will become active in the future)
            active.setText(getContext().getString(R.string.watchers_bottomsheet_watcher_active_future, activeEmoji, format.format(new Date(watcher.getBegin()))));
        }

        String startDate = format.format(new Date(watcher.getFilters().getStartAfter()));
        String endDate = format.format(new Date(watcher.getFilters().getStartBefore()));
        dates.setText(getContext().getString(R.string.watchers_bottomsheet_watcher_dates, startDate, endDate));

        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getContext().startActivity(new Intent(getContext(), WatcherActivity.class).putExtra("id", watcher.getID()));
                dismiss();
            }
        });

        share.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent sendIntent = new Intent();
                sendIntent.setAction(Intent.ACTION_SEND);
                sendIntent.putExtra(Intent.EXTRA_TEXT, BuildConfig.SERVER_BASE_URL + "w/" + watcher.getID());
                sendIntent.setType("text/plain");
                startActivity(Intent.createChooser(sendIntent, getString(R.string.watcher_share)));
                dismiss();
            }
        });

        delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Fragment search = getActivity().getSupportFragmentManager().findFragmentByTag("watchersFragment");
                if(search != null) {
                    ((WatchersFragment) search).deleteWatcher(watcher.getID());
                }
                dismiss();
            }
        });

        close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dismiss();
            }
        });
    }

    private void readCinemasJson() {
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
    }
}
