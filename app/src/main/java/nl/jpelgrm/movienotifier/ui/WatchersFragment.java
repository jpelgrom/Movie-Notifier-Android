package nl.jpelgrm.movienotifier.ui;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
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
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static android.content.Context.MODE_PRIVATE;

public class WatchersFragment extends Fragment {
    @BindView(R.id.coordinator) CoordinatorLayout coordinator;

    @BindView(R.id.progress) ProgressBar progress;

    @BindView(R.id.emptyView) LinearLayout emptyView;
    @BindView(R.id.emptyText) TextView emptyText;

    @BindView(R.id.listSwiper) SwipeRefreshLayout listSwiper;
    @BindView(R.id.listRecycler) RecyclerView listRecycler;

    @BindView(R.id.fab) FloatingActionButton fab;

    private ArrayList<Watcher> watchers = new ArrayList<>();
    private WatchersAdapter adapter;

    private SharedPreferences settings;

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
                refreshList(true, true);
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

        refreshList(false, true);
    }

    private void refreshList(final boolean userTriggered, final boolean showError) {
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
                            } else {
                                if(showError) {
                                    if(response.code() == 401) {
                                        snackbar = Snackbar.make(coordinator, R.string.error_general_401, Snackbar.LENGTH_INDEFINITE);
                                        snackbar.setAction(R.string.ok, new View.OnClickListener() {
                                            @Override
                                            public void onClick(View view) {
                                                startActivity(new Intent(getActivity(), AccountActivity.class));
                                            }
                                        });
                                    } else if(response.code() >= 500 && response.code() < 600){
                                        snackbar = Snackbar.make(coordinator, R.string.error_watchers_500, Snackbar.LENGTH_INDEFINITE);
                                    } else {
                                        snackbar = Snackbar.make(coordinator, R.string.error_watchers_400, Snackbar.LENGTH_INDEFINITE);
                                    }
                                    snackbar.show();
                                }
                            }
                        }

                        @Override
                        public void onFailure(Call<List<Watcher>> call, Throwable t) {
                            t.printStackTrace();

                            listSwiper.setRefreshing(false);

                            if(showError) {
                                snackbar = Snackbar.make(coordinator, R.string.error_general_exception, Snackbar.LENGTH_INDEFINITE);
                                snackbar.show();
                            }
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

    public void deleteWatcher(final String id) {
        new AlertDialog.Builder(getContext()).setMessage(R.string.watcher_delete_confirm).setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                progress.setVisibility(View.VISIBLE);
                listRecycler.setClickable(false);

                Call<ResponseBody> call = APIHelper.getInstance().deleteWatcher(settings.getString("userAPIKey", ""), id);
                call.enqueue(new Callback<ResponseBody>() {
                    @Override
                    public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                        progress.setVisibility(View.GONE);
                        listRecycler.setClickable(true);

                        String message;
                        if(response.code() == 200) {
                            message = getString(R.string.watcher_delete_success);
                        } else {
                            if(response.code() == 400) {
                                message = getString(R.string.error_watcher_400);
                            } else if(response.code() == 401) {
                                message = getString(R.string.error_watcher_401);
                            } else {
                                message = getString(R.string.error_general_server, "H" + response.code());
                            }
                        }

                        snackbar = Snackbar.make(coordinator, message, Snackbar.LENGTH_SHORT);
                        snackbar.show();
                        refreshList(false, false);
                    }

                    @Override
                    public void onFailure(Call<ResponseBody> call, Throwable t) {
                        t.printStackTrace();

                        progress.setVisibility(View.GONE);
                        listRecycler.setClickable(true);

                        snackbar = Snackbar.make(coordinator, R.string.error_general_exception, Snackbar.LENGTH_SHORT);
                        snackbar.show();
                        refreshList(false, false);
                    }
                });
            }
        }).setNegativeButton(R.string.no, null).show();
    }
}
