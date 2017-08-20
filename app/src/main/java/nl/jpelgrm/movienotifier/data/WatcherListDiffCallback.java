package nl.jpelgrm.movienotifier.data;

import android.support.v7.util.DiffUtil;

import java.util.List;

import nl.jpelgrm.movienotifier.models.Watcher;

public class WatcherListDiffCallback extends DiffUtil.Callback {
    private List<Watcher> oldList;
    private List<Watcher> newList;

    public WatcherListDiffCallback(List<Watcher> oldList, List<Watcher> newList) {
        this.oldList = oldList;
        this.newList = newList;
    }

    @Override
    public int getOldListSize() {
        return oldList.size();
    }

    @Override
    public int getNewListSize() {
        return newList.size();
    }

    @Override
    public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
        return oldList.get(oldItemPosition).getID().equals(newList.get(newItemPosition).getID());
    }

    @Override
    public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
        Watcher oldWatcher = oldList.get(oldItemPosition);
        Watcher newWatcher = newList.get(newItemPosition);

        return oldWatcher != null && newWatcher != null && oldWatcher.equals(newWatcher);
    }
}
