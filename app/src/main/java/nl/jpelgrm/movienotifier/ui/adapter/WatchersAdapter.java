package nl.jpelgrm.movienotifier.ui.adapter;

import android.content.Context;
import android.content.Intent;
import android.support.text.emoji.widget.EmojiAppCompatTextView;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.util.DiffUtil;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import nl.jpelgrm.movienotifier.R;
import nl.jpelgrm.movienotifier.data.WatcherListDiffCallback;
import nl.jpelgrm.movienotifier.models.Watcher;
import nl.jpelgrm.movienotifier.ui.WatcherActivity;
import nl.jpelgrm.movienotifier.ui.view.WatcherBottomSheet;

public class WatchersAdapter extends RecyclerView.Adapter<WatchersAdapter.ViewHolder> {
    private List<Watcher> watchers;
    private Context context;

    public WatchersAdapter(Context context, List<Watcher> watchers) {
        this.watchers = watchers;
        this.context = context;
    }

    private Context getContext() {
        return context;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);

        View itemView = inflater.inflate(R.layout.list_watcher, parent, false);
        return new WatchersAdapter.ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        final Watcher watcher = watchers.get(position);

        holder.name.setText(watcher.getName());

        String status;
        if(watcher.getBegin() <= System.currentTimeMillis() && watcher.getEnd() > System.currentTimeMillis()) {
            status = "\uD83D\uDD34"; // Red Circle ('live', active watcher)
        } else if(watcher.getEnd() < System.currentTimeMillis()) {
            status = "\uD83D\uDCE6"; // Package ('archive', watcher is done and will not be active again)
        } else {
            status = "⏰"; // Alarm Clock ('planned', watcher will become active in the future)
        }
        DateFormat format = SimpleDateFormat.getDateInstance(DateFormat.MEDIUM);
        String startDate = format.format(new Date(watcher.getFilters().getStartAfter()));
        String endDate = format.format(new Date(watcher.getFilters().getStartBefore()));
        holder.subtext.setText(getContext().getString(R.string.watchers_list_subtitle_date, status, startDate, endDate));

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getContext().startActivity(new Intent(getContext(), WatcherActivity.class).putExtra("id", watcher.getID()));
            }
        });
        holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                WatcherBottomSheet sheet = WatcherBottomSheet.newInstance(watcher);
                sheet.show(((AppCompatActivity) getContext()).getSupportFragmentManager(), sheet.getTag());

                return true;
            }
        });
    }

    @Override
    public int getItemCount() {
        return watchers.size();
    }

    public void swapItems(List<Watcher> watchers) {
        final WatcherListDiffCallback callback = new WatcherListDiffCallback(this.watchers, watchers);
        final DiffUtil.DiffResult result = DiffUtil.calculateDiff(callback);

        this.watchers.clear();
        this.watchers.addAll(watchers);
        result.dispatchUpdatesTo(this);
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.watcherName) EmojiAppCompatTextView name;
        @BindView(R.id.watcherSubtext) EmojiAppCompatTextView subtext;

        public ViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }
}
