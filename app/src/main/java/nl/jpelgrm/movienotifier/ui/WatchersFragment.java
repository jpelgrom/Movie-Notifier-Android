package nl.jpelgrm.movienotifier.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import nl.jpelgrm.movienotifier.R;
import nl.jpelgrm.movienotifier.data.APIHelper;
import nl.jpelgrm.movienotifier.models.Watcher;
import nl.jpelgrm.movienotifier.ui.adapter.WatchersAdapter;
import nl.jpelgrm.movienotifier.ui.settings.AccountActivity;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static android.content.Context.MODE_PRIVATE;

public class WatchersFragment extends Fragment {
    @BindView(R.id.coordinator) CoordinatorLayout coordinator;

    @BindView(R.id.emptyView) LinearLayout emptyView;
    @BindView(R.id.emptyText) TextView emptyText;

    @BindView(R.id.listSwiper) SwipeRefreshLayout listSwiper;
    @BindView(R.id.listRecycler) RecyclerView listRecycler;

    @BindView(R.id.fab) FloatingActionButton fab;

    private ArrayList<Watcher> watchers = new ArrayList<>();
    private WatchersAdapter adapter;

    private SharedPreferences settings;
    private String previousUUID;

    private Snackbar snackbar;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_watchers, container, false);
        ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        // List
        listSwiper.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                if(snackbar != null && snackbar.isShown()) {
                    snackbar.dismiss();
                }
                refreshList(true);
            }
        });
        adapter = new WatchersAdapter(getContext(), watchers);
        listRecycler.setAdapter(adapter);
        listRecycler.setLayoutManager(new LinearLayoutManager(getContext()));
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(listRecycler.getContext(), LinearLayoutManager.VERTICAL);
        listRecycler.addItemDecoration(dividerItemDecoration);

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(getContext(), WatcherActivity.class));
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        settings = getContext().getSharedPreferences("settings", MODE_PRIVATE);

        if(snackbar != null && snackbar.isShown()) {
            snackbar.dismiss();
        }

        checkUser();
        refreshList(false);
    }

    private void checkUser() {
        if(previousUUID != null && !settings.getString("userID", "").equals(previousUUID) && !settings.getString("userID", "").equals("")) {
            Snackbar.make(coordinator, R.string.account_welcome, Snackbar.LENGTH_LONG).show();
        }
        previousUUID = settings.getString("userID", "");
    }

    private void refreshList(final boolean userTriggered) {
        listSwiper.post(new Runnable() {
            @Override
            public void run() {
                listSwiper.setRefreshing(true);

                if(!settings.getString("userID", "").equals("")) {
                    Call<List<Watcher>> call = APIHelper.getInstance().getWatchers(settings.getString("userAPIKey", ""));
                    call.enqueue(new Callback<List<Watcher>>() {
                        @Override
                        public void onResponse(Call<List<Watcher>> call, Response<List<Watcher>> response) {
                            listSwiper.setRefreshing(false);
                            if(response.code() == 200) {
                                if(response.body().size() > 0) {
                                    emptyView.setVisibility(View.GONE);
                                    listRecycler.setVisibility(View.VISIBLE);
                                    adapter.swapItems(response.body());
                                } else {
                                    showEmptyView();
                                }
                            } else if(response.code() == 401) {
                                snackbar = Snackbar.make(coordinator, R.string.error_general_401, Snackbar.LENGTH_INDEFINITE);
                                snackbar.setAction(R.string.ok, new View.OnClickListener() {
                                    @Override
                                    public void onClick(View view) {
                                        startActivity(new Intent(getActivity(), AccountActivity.class));
                                    }
                                });
                                snackbar.show();
                            } else if(response.code() >= 500 && response.code() < 600){
                                snackbar = Snackbar.make(coordinator, R.string.error_watchers_500, Snackbar.LENGTH_INDEFINITE);
                                snackbar.show();
                            } else {
                                snackbar = Snackbar.make(coordinator, R.string.error_watchers_400, Snackbar.LENGTH_INDEFINITE);
                                snackbar.show();
                            }
                        }

                        @Override
                        public void onFailure(Call<List<Watcher>> call, Throwable t) {
                            t.printStackTrace();

                            listSwiper.setRefreshing(false);

                            snackbar = Snackbar.make(coordinator, R.string.error_general_exception, Snackbar.LENGTH_INDEFINITE);
                            snackbar.show();
                        }
                    });
                } else {
                    showEmptyView();

                    if(userTriggered) {
                        snackbar = Snackbar.make(coordinator, R.string.watchers_empty_account, Snackbar.LENGTH_INDEFINITE);
                        snackbar.setAction(R.string.add, new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                startActivity(new Intent(getActivity(), AccountActivity.class));
                            }
                        });
                        snackbar.show();
                    }
                }
            }
        });
    }

    private void showEmptyView() {
        listSwiper.setRefreshing(false);
        listRecycler.setVisibility(View.GONE);

        emptyView.setVisibility(View.VISIBLE);
        emptyText.setText(R.string.watchers_empty_add);
    }
}
