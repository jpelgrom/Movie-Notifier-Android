package nl.jpelgrm.movienotifier.ui.view;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.emoji.widget.EmojiAppCompatTextView;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.gson.Gson;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import nl.jpelgrm.movienotifier.BuildConfig;
import nl.jpelgrm.movienotifier.R;
import nl.jpelgrm.movienotifier.data.AppDatabase;
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

        AppDatabase.getInstance(getContext()).cinemas().getCinemas().observe(this, cinemas -> {
            this.cinemas = cinemas;
            setupViews(true);
        });

        watcher = new Gson().fromJson(getArguments().getString("watcher"), Watcher.class);
    }

    @Override
    public void setupDialog(Dialog dialog, int style) {
        super.setupDialog(dialog, style);
        View view = View.inflate(getContext(), R.layout.fragment_bottomsheet_watcher, null);
        ButterKnife.bind(this, view);
        dialog.setContentView(view);

        setupViews(false);
    }

    private void setupViews(boolean cinemasOnly) {
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
            foundCinema = String.valueOf(watcher.getFilters().getCinemaID());
        }
        location.setText(foundCinema);

        if(cinemasOnly) { return; }

        DateFormat format = SimpleDateFormat.getDateTimeInstance(java.text.DateFormat.MEDIUM, java.text.DateFormat.SHORT);

        String activeEmoji;
        if(watcher.getBegin() <= System.currentTimeMillis() && watcher.getEnd() > System.currentTimeMillis()) {
            activeEmoji = "\uD83D\uDD34"; // Red Circle ('live', active watcher)
            active.setText(getString(R.string.watchers_bottomsheet_watcher_active_now, activeEmoji, format.format(new Date(watcher.getEnd()))));
        } else if(watcher.getEnd() < System.currentTimeMillis()) {
            activeEmoji = "\uD83D\uDCE6"; // Package ('archive', watcher is done and will not be active again)
            active.setText(getString(R.string.watchers_bottomsheet_watcher_active_past, activeEmoji, format.format(new Date(watcher.getEnd()))));
        } else {
            activeEmoji = "⏰"; // Alarm Clock ('planned', watcher will become active in the future)
            active.setText(getString(R.string.watchers_bottomsheet_watcher_active_future, activeEmoji, format.format(new Date(watcher.getBegin()))));
        }

        String startDate = format.format(new Date(watcher.getFilters().getStartAfter()));
        String endDate = format.format(new Date(watcher.getFilters().getStartBefore()));
        dates.setText(getString(R.string.watchers_bottomsheet_watcher_dates, startDate, endDate));

        view.setOnClickListener(view -> {
            dismiss();
            getContext().startActivity(new Intent(getContext(), WatcherActivity.class).putExtra("id", watcher.getID()));
        });

        share.setOnClickListener(view -> {
            dismiss();
            Intent sendIntent = new Intent();
            sendIntent.setAction(Intent.ACTION_SEND);
            sendIntent.putExtra(Intent.EXTRA_TEXT, BuildConfig.SERVER_BASE_URL + "w/" + watcher.getID());
            sendIntent.setType("text/plain");
            startActivity(Intent.createChooser(sendIntent, getString(R.string.watcher_share)));
        });

        delete.setOnClickListener(view -> {
            dismiss();
            Fragment search = getActivity().getSupportFragmentManager().findFragmentByTag("watchersFragment");
            if(search != null) {
                ((WatchersFragment) search).deleteWatcher(watcher.getID());
            }
        });

        close.setOnClickListener(view -> dismiss());
    }
}
