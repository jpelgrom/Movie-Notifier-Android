package nl.jpelgrm.movienotifier.ui.adapter;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;
import butterknife.ButterKnife;
import nl.jpelgrm.movienotifier.R;
import nl.jpelgrm.movienotifier.data.AccountListDiffCallback;
import nl.jpelgrm.movienotifier.models.User;
import nl.jpelgrm.movienotifier.ui.settings.SettingsActivity;

public class AccountsAdapter extends RecyclerView.Adapter<AccountsAdapter.ViewHolder> {
    private List<User> users;
    private Context context;
    private String oldActive = "";

    public AccountsAdapter(Context context, List<User> users) {
        this.users = users;
        this.context = context;
    }

    private Context getContext() {
        return context;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);

        View itemView = inflater.inflate(R.layout.list_account, parent, false);
        return new ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        final User user = users.get(position);
        final SharedPreferences settings = getContext().getSharedPreferences("settings", Context.MODE_PRIVATE);
        final boolean isCurrentUser = settings.getString("userID", "").equals(user.getId());

        holder.name.setText(user.getName());
        holder.status.setVisibility(isCurrentUser ? View.VISIBLE : View.GONE);

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(getContext() instanceof SettingsActivity) {
                    ((SettingsActivity) getContext()).showUser(user.getId(), isCurrentUser);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return users.size();
    }

    public void swapItems(List<User> users) {
        String newActive = getContext().getSharedPreferences("settings", Context.MODE_PRIVATE).getString("userID", "");
        final AccountListDiffCallback callback = new AccountListDiffCallback(this.users, users, oldActive, newActive);
        final DiffUtil.DiffResult result = DiffUtil.calculateDiff(callback);

        this.users.clear();
        this.users.addAll(users);
        result.dispatchUpdatesTo(this);

        oldActive = newActive;
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.accountName) TextView name;
        @BindView(R.id.accountStatus) TextView status;

        public ViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }
}
