package nl.jpelgrm.movienotifier.ui.adapter;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.support.v7.app.AlertDialog;
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
        final boolean isCurrentUser = settings.getString("userID", "").equals(user.getUuid());

        holder.name.setText(user.getName());
        holder.status.setVisibility(isCurrentUser ? View.VISIBLE : View.GONE);
        holder.error.setVisibility(View.GONE);
        holder.switchTo.setText(isCurrentUser ? R.string.user_list_logout : R.string.user_list_switch);
        holder.switchTo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(isCurrentUser) { // Log out
                    setActionableInputs(holder, false);
                    String current = settings.getString("userID", "");

                    settings.edit().putString("userID", "").putString("userAPIKey", "").apply();
                    DBHelper.getInstance(getContext()).deleteUser(user.getUuid());

                    tryLoggingInNextUser(holder, current);
                } else { // Switch
                    Call<User> call = APIHelper.getInstance().getUser(user.getApikey(), user.getUuid());
                    call.enqueue(new Callback<User>() {
                        @Override
                        public void onResponse(Call<User> call, Response<User> response) {
                            setActionableInputs(holder, true);
                            if(response.code() == 200 && response.body() != null) {
                                User received = response.body();
                                if(received != null) {
                                    DBHelper.getInstance(getContext()).updateUser(received);
                                    settings.edit().putString("userID", received.getUuid()).putString("userAPIKey", received.getApikey()).apply();

                                    swapItems(DBHelper.getInstance(getContext()).getUsers());
                                } else {
                                    holder.error.setText(getContext().getString(R.string.error_general_server, "N200"));
                                    holder.error.setVisibility(View.VISIBLE);
                                }
                            } else {
                                holder.error.setText(getErrorMessage(call, response));
                                holder.error.setVisibility(View.VISIBLE);
                            }
                        }

                        @Override
                        public void onFailure(Call<User> call, Throwable t) {
                            setActionableInputs(holder, true);

                            t.printStackTrace();

                            holder.error.setText(R.string.error_general_exception);
                            holder.error.setVisibility(View.VISIBLE);
                        }
                    });
                }
            }
        });
        holder.delete.setVisibility(isCurrentUser ? View.VISIBLE : View.GONE);
        holder.delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new AlertDialog.Builder(getContext()).setMessage(R.string.user_list_delete_confirm).setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        setActionableInputs(holder, false);
                        Call<ResponseBody> call = APIHelper.getInstance().deleteUser(settings.getString("userAPIKey", ""), settings.getString("userID", ""));
                        call.enqueue(new Callback<ResponseBody>() {
                            @Override
                            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                                if(response.code() == 200 || response.code() == 401) {
                                    // 200: OK
                                    // 401: Unauthorized, but because API keys don't change it seems the user was already deleted
                                    if(user.getUuid().equals(settings.getString("userID", ""))) {
                                        settings.edit().putString("userID", "").putString("userAPIKey", "").apply();
                                    }
                                    DBHelper.getInstance(getContext()).deleteUser(user.getUuid());

                                    tryLoggingInNextUser(holder, "");
                                } else {
                                    setActionableInputs(holder, true);
                                    holder.error.setText(getContext().getString(R.string.error_general_server, "N200"));
                                    holder.error.setVisibility(View.VISIBLE);
                                }
                            }

                            @Override
                            public void onFailure(Call<ResponseBody> call, Throwable t) {
                                setActionableInputs(holder, true);

                                t.printStackTrace();

                                holder.error.setText(R.string.error_general_exception);
                                holder.error.setVisibility(View.VISIBLE);
                            }
                        });
                    }
                }).setNegativeButton(R.string.no, null).show();
            }
        });
        holder.progress.setVisibility(View.GONE);
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

    private User getNextInactiveUser(String exclude) {
        SharedPreferences settings = getContext().getSharedPreferences("settings", Context.MODE_PRIVATE);
        for(User user : users) {
            if(!user.getUuid().equals(settings.getString("userID", "")) && !user.getUuid().equals(exclude)) {
                return user;
            }
        }
        return null;
    }

    private void tryLoggingInNextUser(final ViewHolder holder, String exclude) {
        User switchTo = getNextInactiveUser(exclude);
        final SharedPreferences settings = getContext().getSharedPreferences("settings", Context.MODE_PRIVATE);

        if(switchTo != null) {
            Call<User> call = APIHelper.getInstance().getUser(switchTo.getApikey(), switchTo.getUuid());
            call.enqueue(new Callback<User>() {
                @Override
                public void onResponse(Call<User> call, Response<User> response) {
                    setActionableInputs(holder, true);
                    if(response.code() == 200 && response.body() != null) {
                        User received = response.body();
                        if(received != null) {
                            DBHelper.getInstance(getContext()).updateUser(received);
                            settings.edit().putString("userID", received.getUuid()).putString("userAPIKey", received.getApikey()).apply();
                        }
                    }

                    swapItems(DBHelper.getInstance(getContext()).getUsers());
                }

                @Override
                public void onFailure(Call<User> call, Throwable t) {
                    setActionableInputs(holder, true);
                    swapItems(DBHelper.getInstance(getContext()).getUsers());
                }
            });
        } else {
            setActionableInputs(holder, true);
            swapItems(DBHelper.getInstance(getContext()).getUsers());
        }
    }

    private String getErrorMessage(Call<User> call, Response<User> response) {
        Gson gson = new GsonBuilder().create();
        StringBuilder errorBuilder = new StringBuilder();

        if(response.code() == 400) {
            if(response.errorBody() != null) {
                try {
                    Errors errors = gson.fromJson(response.errorBody().string(), Errors.class);
                    for(String errorString : errors.getErrors()) {
                        if(!errorBuilder.toString().equals("")) {
                            errorBuilder.append("\n");
                        }
                        errorBuilder.append(errorString);
                    }
                } catch(IOException e) {
                    errorBuilder.append(getContext().getString(R.string.error_general_server, "I400"));
                }
            } else {
                errorBuilder.append(getContext().getString(R.string.error_general_server, "N400"));
            }

            return errorBuilder.toString();
        } else if(response.code() == 500){
            if(response.errorBody() != null) {
                try {
                    Message message = gson.fromJson(response.errorBody().string(), Message.class);
                    errorBuilder.append(message.getMessage());
                } catch(IOException e) {
                    errorBuilder.append("I500");
                }
            } else {
                errorBuilder.append("N500");
            }

           return getContext().getString(R.string.error_general_message, errorBuilder.toString());
        } else {
            return getContext().getString(R.string.error_general_server, "H" + response.code());
        }
    }

    private void setActionableInputs(ViewHolder holder, boolean enabled) {
        holder.error.setVisibility(View.GONE);
        holder.switchTo.setEnabled(enabled);
        holder.delete.setEnabled(enabled);
        holder.progress.setVisibility(enabled ? View.GONE : View.VISIBLE);
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.accountName) TextView name;
        @BindView(R.id.accountStatus) TextView status;
        @BindView(R.id.accountError) TextView error;
        @BindView(R.id.accountSwitch) Button switchTo;
        @BindView(R.id.accountDelete) Button delete;
        @BindView(R.id.accountProgress) ProgressBar progress;

        public ViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }
}
