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

import androidx.annotation.NonNull;
import androidx.paging.PagedListAdapter;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import nl.jpelgrm.movienotifier.R;
import nl.jpelgrm.movienotifier.databinding.ListNotificationBinding;
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
            holder.binding.notificationTime.setText(DateUtils.getRelativeDateTimeString(getContext(), notification.getTime(), DateUtils.DAY_IN_MILLIS, DateUtils.WEEK_IN_MILLIS, 0));

            String bodyText = getContext().getString(R.string.notifications_notification_appbody, notification.getWatchername(), notification.getMatches(), notification.getBody());
            bodyText = bodyText.replace("\n","<br />");
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                holder.binding.notificationText.setText(Html.fromHtml(bodyText, Html.FROM_HTML_MODE_COMPACT));
            } else {
                holder.binding.notificationText.setText(Html.fromHtml(bodyText));
            }

            Intent patheIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("patheapp://showMovie/" + notification.getWatchermovieid()));
            ComponentName patheInfo = patheIntent.resolveActivity(getContext().getPackageManager());
            if(patheInfo != null) {
                try {
                    Drawable icon = getContext().getPackageManager().getApplicationIcon(patheInfo.getPackageName());
                    holder.binding.notificationActionPathe.setChipIcon(icon);
                } catch(Exception e) { }
                holder.binding.notificationActionPathe.setOnClickListener(v -> getContext().startActivity(patheIntent));
                holder.binding.notificationActionPathe.setVisibility(View.VISIBLE);
            }

            Intent watcherIntent = new Intent(getContext(), WatcherActivity.class);
            watcherIntent.putExtra("id", notification.getWatcherid());
            holder.binding.notificationActionView.setOnClickListener(v -> getContext().startActivity(watcherIntent));
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
        ListNotificationBinding binding;

        public ViewHolder(View itemView) {
            super(itemView);
            binding = ListNotificationBinding.bind(itemView);
        }
    }
}
