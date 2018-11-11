package nl.jpelgrm.movienotifier.util;

import androidx.lifecycle.LiveData;

public class EmptyLiveData extends LiveData {
    private EmptyLiveData() {
        //noinspection unchecked
        postValue(null);
    }
    public static <T> LiveData<T> create() {
        //noinspection unchecked
        return new EmptyLiveData();
    }
}