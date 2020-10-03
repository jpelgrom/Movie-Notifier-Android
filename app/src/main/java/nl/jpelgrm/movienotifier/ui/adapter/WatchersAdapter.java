package nl.jpelgrm.movienotifier.ui.adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import nl.jpelgrm.movienotifier.R;
import nl.jpelgrm.movienotifier.data.WatcherListDiffCallback;
import nl.jpelgrm.movienotifier.databinding.ListWatcherBinding;
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

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);

        View itemView = inflater.inflate(R.layout.list_watcher, parent, false);
        return new WatchersAdapter.ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        final Watcher watcher = watchers.get(position);

        holder.binding.watcherName.setText(watcher.getName());

        String status;
        if(watcher.getBegin() <= System.currentTimeMillis() && watcher.getEnd() > System.currentTimeMillis()) {
            status = "\uD83D\uDD34"; // Red Circle ('live', active watcher)
        } else if(watcher.getEnd() < System.currentTimeMillis()) {
            status = "\uD83D\uDCE6"; // Package ('archive', watcher is done and will not be active again)
        } else {
            status = "⏰"; // Alarm Clock ('planned', watcher will become active in the future)
        }
        DateFormat format = SimpleDateFormat.getDateInstance(DateFormat.MEDIUM);
        String startDate = format.format(new Date(watcher.getFilters().getStartafter()));
        String endDate = format.format(new Date(watcher.getFilters().getStartbefore()));
        holder.binding.watcherSubtext.setText(getContext().getString(R.string.watchers_list_subtitle_date, status, startDate, endDate));

        holder.itemView.setOnClickListener(view -> getContext().startActivity(new Intent(getContext(), WatcherActivity.class).putExtra("id", watcher.getId())));
        holder.itemView.setOnLongClickListener(view -> {
            WatcherBottomSheet sheet = WatcherBottomSheet.newInstance(watcher);
            sheet.show(((AppCompatActivity) getContext()).getSupportFragmentManager(), sheet.getTag());

            return true;
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
        ListWatcherBinding binding;

        public ViewHolder(View itemView) {
            super(itemView);
            binding = ListWatcherBinding.bind(itemView);
        }
    }
}
