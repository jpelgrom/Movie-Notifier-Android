package nl.jpelgrm.movienotifier.ui.adapter;

import android.content.Context;
import android.content.Intent;
import android.support.v7.util.DiffUtil;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

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

        DateFormat format = SimpleDateFormat.getDateTimeInstance(java.text.DateFormat.MEDIUM, java.text.DateFormat.SHORT);
        String startDate = format.format(new Date(Long.parseLong(watcher.getStartAfter())));
        String endDate = format.format(new Date(Long.parseLong(watcher.getStartBefore())));
        holder.subtext.setText(getContext().getString(R.string.watchers_list_subtitle_date, startDate, endDate));

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getContext().startActivity(new Intent(getContext(), WatcherActivity.class).putExtra("uuid", watcher.getUuid()));
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
        @BindView(R.id.watcherName) TextView name;
        @BindView(R.id.watcherSubtext) TextView subtext;

        public ViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }
}
