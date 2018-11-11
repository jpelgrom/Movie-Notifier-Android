package nl.jpelgrm.movienotifier.data;

import android.app.Activity;
import android.content.Context;
import android.location.Location;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import nl.jpelgrm.movienotifier.R;
import nl.jpelgrm.movienotifier.models.Cinema;
import nl.jpelgrm.movienotifier.util.LocationUtil;

public class CinemaIDAdapter extends ArrayAdapter<Cinema> {
    private List<Cinema> cinemas;
    private int viewResourceID;
    private Location location;

    private List<Cinema> originals;
    private CinemaIDFilter filter;

    public CinemaIDAdapter(Context context, int viewResourceID, List<Cinema> cinemas) {
        super(context, viewResourceID, cinemas);
        this.cinemas = cinemas;
        this.viewResourceID = viewResourceID;
    }

    public void setLocation(Location location) {
        this.location = location;
        if(originals != null) {
            sortCinemas();
        }
    }

    public void setCinemas(List<Cinema> newCinemas) {
        cinemas.clear();
        cinemas.addAll(newCinemas);

        boolean wasSorting = (originals != null);
        originals = null;
        if(wasSorting) {
            sortCinemas();
        }
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        if (convertView == null) {
            LayoutInflater inflater = ((Activity) getContext()).getLayoutInflater();
            convertView = inflater.inflate(viewResourceID, parent, false);
        }

        Cinema cinema = getItem(position);
        if(cinema != null) {
            TextView name = convertView.findViewById(R.id.name);
            name.setText(cinema.getName());

            TextView distance = convertView.findViewById(R.id.distance);
            if(cinema.getLat() == null || cinema.getLon() == null || location == null) {
                distance.setVisibility(View.GONE);
            } else {
                distance.setVisibility(View.VISIBLE);
                distance.setText(LocationUtil.getFormattedDistance(location, cinema.getLat(), cinema.getLon()));
            }
        }

        return convertView;
    }

    @NonNull
    @Override
    public Filter getFilter() {
        if (filter == null) {
            filter = new CinemaIDFilter();
        }
        return filter;
    }

    private void sortCinemas() {
        Collections.sort(cinemas, new Comparator<Cinema>() {
            @Override
            public int compare(Cinema c1, Cinema c2) {
                if(location != null) {
                    if(c1.getLat() != null && c1.getLon() != null && c2.getLat() != null && c2.getLon() != null) {
                        return Double.compare(LocationUtil.getDistance(location, c1.getLat(), c1.getLon()),
                                LocationUtil.getDistance(location, c2.getLat(), c2.getLon()));
                    } else if(c1.getLat() != null && c1.getLon() != null) { // c2 null, and c2 should go to bottom
                        return -1;
                    } else if(c2.getLat() != null && c2.getLon() != null) { // c1 null, and c1 should go to bottom
                        return 1;
                    }
                }
                return c1.getName().compareToIgnoreCase(c2.getName()); // Default fallback
            }
        });

        notifyDataSetChanged();
    }

    // Based on https://github.com/aosp-mirror/platform_frameworks_base/blob/master/core/java/android/widget/ArrayAdapter.java#L545
    private class CinemaIDFilter extends Filter {
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            final FilterResults results = new FilterResults();
            final List<Cinema> values = new ArrayList<>();

            if(originals == null) {
                originals = new ArrayList<>(cinemas);
            }

            values.addAll(originals);

            if(constraint == null || constraint.length() == 0) {
                results.values = values;
                results.count = values.size();
            } else {
                final String prefixString = constraint.toString().toLowerCase();
                final List<Cinema> newValues = new ArrayList<>();

                for(int i = 0; i < values.size(); i++) {
                    final Cinema cinema = values.get(i);
                    final String valueText = cinema.getName().toLowerCase();

                    // First match against the whole, non-splitted value
                    if(valueText.startsWith(prefixString)) {
                        newValues.add(cinema);
                    } else {
                        final String[] words = valueText.split(" ");
                        for(String word : words) {
                            if(word.startsWith(prefixString)) {
                                newValues.add(cinema);
                                break;
                            }
                        }
                    }
                }

                results.values = newValues;
                results.count = newValues.size();
            }

            return results;
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            //noinspection unchecked
            List<Cinema> received = (List<Cinema>) results.values;

            cinemas.clear();
            cinemas.addAll(received);

            sortCinemas();
        }
    }
}
