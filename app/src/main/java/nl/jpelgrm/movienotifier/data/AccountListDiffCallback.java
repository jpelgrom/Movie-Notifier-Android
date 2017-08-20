package nl.jpelgrm.movienotifier.data;

import android.support.v7.util.DiffUtil;

import java.util.List;

import nl.jpelgrm.movienotifier.models.User;

public class AccountListDiffCallback extends DiffUtil.Callback {
    private List<User> oldList;
    private List<User> newList;
    private String oldActive;
    private String newActive;

    public AccountListDiffCallback(List<User> oldList, List<User> newList, String oldActive, String newActive) {
        this.oldList = oldList;
        this.newList = newList;
        this.oldActive = oldActive;
        this.newActive = newActive;
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
        User oldUser = oldList.get(oldItemPosition);
        User newUser = newList.get(newItemPosition);

        return oldUser != null && newUser != null && oldUser.getID().equals(newUser.getID()) && oldUser.getName().equals(newUser.getName())
                && oldUser.getEmail().equals(newUser.getEmail()) && oldUser.getNotifications().equals(newUser.getNotifications())
                && oldUser.getApikey().equals(newUser.getApikey()) && (oldActive.equals(newActive) && (oldActive.equals(oldUser.getID()) || newActive.equals(newUser.getID())));
    }
}
