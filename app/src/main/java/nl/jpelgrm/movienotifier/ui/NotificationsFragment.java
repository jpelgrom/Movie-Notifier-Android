package nl.jpelgrm.movienotifier.ui;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import androidx.paging.LivePagedListBuilder;
import androidx.paging.PagedList;
import androidx.recyclerview.widget.LinearLayoutManager;

import nl.jpelgrm.movienotifier.data.AppDatabase;
import nl.jpelgrm.movienotifier.databinding.FragmentNotificationsBinding;
import nl.jpelgrm.movienotifier.models.Notification;
import nl.jpelgrm.movienotifier.ui.adapter.NotificationsAdapter;

import static android.content.Context.MODE_PRIVATE;

public class NotificationsFragment extends Fragment {
    private FragmentNotificationsBinding binding;

    private LiveData<PagedList<Notification>> notifications;
    private MutableLiveData<String> userId = new MutableLiveData<>();
    private NotificationsAdapter adapter;

    private AppDatabase db;
    private SharedPreferences settings;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        settings = getContext().getSharedPreferences("settings", MODE_PRIVATE);

        db = AppDatabase.getInstance(getContext());
        notifications = Transformations.switchMap(userId, input -> {
            PagedList.Config config = new PagedList.Config.Builder().setPageSize(10).setEnablePlaceholders(false).build();
            return new LivePagedListBuilder<>(db.notifications().getNotificationsForUser(input), config).build();
        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentNotificationsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        adapter = new NotificationsAdapter(getContext());
        notifications.observe(this, notifications -> adapter.submitList(notifications, () -> {
            if(notifications.size() > 0) {
                binding.emptyView.setVisibility(View.GONE);
                binding.listRecycler.setVisibility(View.VISIBLE);
            } else {
                binding.listRecycler.setVisibility(View.GONE);
                binding.emptyView.setVisibility(View.VISIBLE);
            }
        }));
        binding.listRecycler.setAdapter(adapter);
        binding.listRecycler.setLayoutManager(new LinearLayoutManager(getContext()));
    }

    public void scrollListToTop() {
        if(binding != null && binding.listRecycler != null && binding.listRecycler.getLayoutManager() instanceof LinearLayoutManager) {
            ((LinearLayoutManager) binding.listRecycler.getLayoutManager()).scrollToPositionWithOffset(0, 0);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        userId.postValue(settings.getString("userID", ""));
    }
}
