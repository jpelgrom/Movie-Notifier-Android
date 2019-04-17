package nl.jpelgrm.movienotifier.ui.adapter;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.text.Html;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.android.material.chip.Chip;

import androidx.annotation.NonNull;
import androidx.paging.PagedListAdapter;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;
import butterknife.ButterKnife;
import nl.jpelgrm.movienotifier.R;
import nl.jpelgrm.movienotifier.models.Notification;
import nl.jpelgrm.movienotifier.ui.WatcherActivity;

public class NotificationsAdapter extends PagedListAdapter<Notification, NotificationsAdapter.ViewHolder> {
    private Context context;

    public NotificationsAdapter(Context context) {
        super(DIFF_CALLBACK);
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

        View itemView = inflater.inflate(R.layout.list_notification, parent, false);
        return new ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Notification notification = getItem(position);
        if(notification != null) {
            holder.time.setText(DateUtils.getRelativeDateTimeString(getContext(), notification.getTime(), DateUtils.DAY_IN_MILLIS, DateUtils.WEEK_IN_MILLIS, 0));

            String bodyText = getContext().getString(R.string.notifications_notification_appbody, notification.getWatchername(), notification.getMatches(), notification.getBody());
            bodyText = bodyText.replace("\n","<br />");
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                holder.text.setText(Html.fromHtml(bodyText, Html.FROM_HTML_MODE_COMPACT));
            } else {
                holder.text.setText(Html.fromHtml(bodyText));
            }

            Intent patheIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("patheapp://showMovie/" + notification.getWatchermovieid()));
            ComponentName patheInfo = patheIntent.resolveActivity(getContext().getPackageManager());
            if(patheInfo != null) {
                try {
                    Drawable icon = getContext().getPackageManager().getApplicationIcon(patheInfo.getPackageName());
                    holder.actionPathe.setChipIcon(icon);
                } catch(Exception e) { }
                holder.actionPathe.setOnClickListener(v -> getContext().startActivity(patheIntent));
                holder.actionPathe.setVisibility(View.VISIBLE);
            }

            Intent watcherIntent = new Intent(getContext(), WatcherActivity.class);
            watcherIntent.putExtra("id", notification.getWatcherid());
            holder.actionView.setOnClickListener(v -> getContext().startActivity(watcherIntent));
        }
    }

    private static DiffUtil.ItemCallback<Notification> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<Notification>() {
                @Override
                public boolean areItemsTheSame(@NonNull Notification oldNotification, @NonNull Notification newNotification) {
                    return oldNotification.getId().equals(newNotification.getId());
                }

                @Override
                public boolean areContentsTheSame(@NonNull Notification oldNotification, @NonNull Notification newNotification) {
                    return oldNotification.equals(newNotification);
                }
            };

    public class ViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.notificationTime) TextView time;
        @BindView(R.id.notificationText) TextView text;
        @BindView(R.id.notificationActionPathe) Chip actionPathe;
        @BindView(R.id.notificationActionView) Chip actionView;

        public ViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }
}
