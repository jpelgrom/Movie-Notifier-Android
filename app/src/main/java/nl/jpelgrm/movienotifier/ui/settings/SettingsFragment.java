package nl.jpelgrm.movienotifier.ui.settings;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import nl.jpelgrm.movienotifier.R;
import nl.jpelgrm.movienotifier.data.APIHelper;
import nl.jpelgrm.movienotifier.data.DBHelper;
import nl.jpelgrm.movienotifier.models.User;
import nl.jpelgrm.movienotifier.ui.adapter.AccountsAdapter;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SettingsFragment extends Fragment {
    @BindView(R.id.accountsRecycler) public RecyclerView accountsRecycler;
    @BindView(R.id.accountFlow) LinearLayout addAccount;

    private ArrayList<User> users = new ArrayList<>();
    private AccountsAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);
        ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        adapter = new AccountsAdapter(getContext(), users);
        accountsRecycler.setAdapter(adapter);
        accountsRecycler.setLayoutManager(new LinearLayoutManager(getContext()));
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(accountsRecycler.getContext(), LinearLayoutManager.VERTICAL);
        accountsRecycler.addItemDecoration(dividerItemDecoration);

        adapter.swapItems(DBHelper.getInstance(getContext()).getUsers());
        updateAccountsList();

        addAccount.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(getActivity(), AccountActivity.class));
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        adapter.swapItems(DBHelper.getInstance(getContext()).getUsers());
    }

    private void updateAccountsList() {
        final DBHelper db = DBHelper.getInstance(getContext());
        final List<User> active = db.getUsers();

        for(final User user : active) {
            Call<User> call = APIHelper.getInstance().getUser(user.getApikey(), user.getUuid());
            call.enqueue(new Callback<User>() {
                @Override
                public void onResponse(Call<User> call, Response<User> response) {
                    if(response.code() == 200) {
                        if(response.body() != null) {
                            db.updateUser(response.body());
                        }
                    } else if(response.code() == 401) {
                        // Authentication failed, which cannot happen unless the user has been deleted, so make sure to delete it here as well
                        db.deleteUser(user.getUuid());

                        SharedPreferences settings = getContext().getSharedPreferences("settings", Context.MODE_PRIVATE);
                        if(settings.getString("userID", "").equals(user.getUuid())) {
                            settings.edit().putString("userID", "").putString("userAPIKey", "").apply();
                        }
                    } // else: failed with user facing error, but do nothing because it is a background task

                    adapter.swapItems(db.getUsers());
                }

                @Override
                public void onFailure(Call<User> call, Throwable t) {
                    t.printStackTrace();
                }
            });
        }
    }
}
