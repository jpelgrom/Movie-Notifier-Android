package nl.jpelgrm.movienotifier.ui.view;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetDialogFragment;
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

public class SortBottomSheet extends BottomSheetDialogFragment {
    @BindView(R.id.sortBegin) LinearLayout sortBegin;
    @BindView(R.id.sortBeginText) TextView sortBeginText;
    @BindView(R.id.sortEnd) LinearLayout sortEnd;
    @BindView(R.id.sortEndText) TextView sortEndText;
    @BindView(R.id.sortStartAfter) LinearLayout sortStartAfter;
    @BindView(R.id.sortStartAfterText) TextView sortStartAfterText;
    @BindView(R.id.sortAZ) LinearLayout sortAZ;
    @BindView(R.id.sortAZText) TextView sortAZText;

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
        View view = View.inflate(getContext(), R.layout.fragment_bottomsheet_sort, null);
        ButterKnife.bind(this, view);
        dialog.setContentView(view);

        setupViews();
    }

    private void setupViews() {
        int pref = settings.getInt("listSort", 0);
        switch(pref) {
            case 1: // End (stop checking)
                sortEndText.setTextColor(ContextCompat.getColor(getContext(), R.color.colorAccent));
                break;
            case 2: // Start after (first showing)
                sortStartAfterText.setTextColor(ContextCompat.getColor(getContext(), R.color.colorAccent));
                break;
            case 3: // A-Z
                sortAZText.setTextColor(ContextCompat.getColor(getContext(), R.color.colorAccent));
                break;
            case 0: // Begin (start checking)
            default:
                sortBeginText.setTextColor(ContextCompat.getColor(getContext(), R.color.colorAccent));
                break;
        }

        sortBegin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                settings.edit().putInt("listSort", 0).apply();
                dismiss();
            }
        });
        sortEnd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                settings.edit().putInt("listSort", 1).apply();
                dismiss();
            }
        });
        sortStartAfter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                settings.edit().putInt("listSort", 2).apply();
                dismiss();
            }
        });
        sortAZ.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                settings.edit().putInt("listSort", 3).apply();
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
