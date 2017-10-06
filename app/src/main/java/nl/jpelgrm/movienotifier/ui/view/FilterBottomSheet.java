package nl.jpelgrm.movienotifier.ui.view;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetDialogFragment;
import android.support.text.emoji.widget.EmojiAppCompatTextView;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import butterknife.BindView;
import butterknife.ButterKnife;
import nl.jpelgrm.movienotifier.R;
import nl.jpelgrm.movienotifier.ui.WatchersFragment;

import static android.content.Context.MODE_PRIVATE;

public class FilterBottomSheet extends BottomSheetDialogFragment {
    @BindView(R.id.filterAll) LinearLayout filterAll;
    @BindView(R.id.filterAllIndicator) EmojiAppCompatTextView filterAllIndicator;
    @BindView(R.id.filterAllText) TextView filterAllText;
    @BindView(R.id.filterPast) LinearLayout filterPast;
    @BindView(R.id.filterPastIndicator) EmojiAppCompatTextView filterPastIndicator;
    @BindView(R.id.filterPastText) TextView filterPastText;
    @BindView(R.id.filterNow) LinearLayout filterNow;
    @BindView(R.id.filterNowIndicator) EmojiAppCompatTextView filterNowIndicator;
    @BindView(R.id.filterNowText) TextView filterNowText;
    @BindView(R.id.filterFuture) LinearLayout filterFuture;
    @BindView(R.id.filterFutureIndicator) EmojiAppCompatTextView filterFutureIndicator;
    @BindView(R.id.filterFutureText) TextView filterFutureText;

    @BindView(R.id.close) Button close;

    private SharedPreferences settings;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        settings = getContext().getSharedPreferences("settings", MODE_PRIVATE);
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
        if(getActivity().getSupportFragmentManager().findFragmentByTag("watchersFragment") != null) {
            ((WatchersFragment) getActivity().getSupportFragmentManager().findFragmentByTag("watchersFragment")).filterAndSort();
        }
    }

    @Override
    public void setupDialog(Dialog dialog, int style) {
        super.setupDialog(dialog, style);
        View view = View.inflate(getContext(), R.layout.fragment_bottomsheet_filter, null);
        ButterKnife.bind(this, view);
        dialog.setContentView(view);

        setupViews();
    }

    private void setupViews() {
        // Setting these in XML seems to cause crashes on some older devices
        filterAllIndicator.setText("\uD83D\uDCDA");
        filterPastIndicator.setText("\uD83D\uDCE6"); // Package ('archive', watcher is done and will not be active again)
        filterNowIndicator.setText("\uD83D\uDD34"); // Red Circle ('live', active watcher)
        filterFutureIndicator.setText("⏰"); // Alarm Clock ('planned', watcher will become active in the future)

        int pref = settings.getInt("listFilter", 0);
        switch(pref) {
            case 1: // Past
                filterPastText.setTextColor(ContextCompat.getColor(getContext(), R.color.colorAccent));
                break;
            case 2: // Now
                filterNowText.setTextColor(ContextCompat.getColor(getContext(), R.color.colorAccent));
                break;
            case 3: // Future
                filterFutureText.setTextColor(ContextCompat.getColor(getContext(), R.color.colorAccent));
                break;
            case 0: // All
            default:
                filterAllText.setTextColor(ContextCompat.getColor(getContext(), R.color.colorAccent));
                break;
        }

        filterAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                settings.edit().putInt("listFilter", 0).apply();
                dismiss();
            }
        });
        filterPast.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                settings.edit().putInt("listFilter", 1).apply();
                dismiss();
            }
        });
        filterNow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                settings.edit().putInt("listFilter", 2).apply();
                dismiss();
            }
        });
        filterFuture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                settings.edit().putInt("listFilter", 3).apply();
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
}
