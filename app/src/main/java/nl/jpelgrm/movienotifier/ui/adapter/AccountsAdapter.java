package nl.jpelgrm.movienotifier.ui.adapter;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.util.DiffUtil;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import nl.jpelgrm.movienotifier.R;
import nl.jpelgrm.movienotifier.data.APIHelper;
import nl.jpelgrm.movienotifier.data.AccountListDiffCallback;
import nl.jpelgrm.movienotifier.data.DBHelper;
import nl.jpelgrm.movienotifier.models.User;
import nl.jpelgrm.movienotifier.models.error.Errors;
import nl.jpelgrm.movienotifier.models.error.Message;
import nl.jpelgrm.movienotifier.ui.settings.SettingsActivity;
import nl.jpelgrm.movienotifier.ui.settings.SettingsMainFragment;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

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
        final boolean isCurrentUser = settings.getString("userID", "").equals(user.getID());

        holder.name.setText(user.getName());
        holder.status.setVisibility(isCurrentUser ? View.VISIBLE : View.GONE);

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(getContext() instanceof SettingsActivity) {
                    ((SettingsActivity) getContext()).showUser(user.getID());
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
