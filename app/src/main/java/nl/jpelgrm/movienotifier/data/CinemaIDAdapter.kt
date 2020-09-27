package nl.jpelgrm.movienotifier.data

import android.app.Activity
import android.content.Context
import android.location.Location
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Filter
import android.widget.TextView
import nl.jpelgrm.movienotifier.R
import nl.jpelgrm.movienotifier.models.Cinema
import nl.jpelgrm.movienotifier.util.LocationUtil
import java.util.*

class CinemaIDAdapter(context: Context, private val viewResourceID: Int, private val cinemas: MutableList<Cinema>) : ArrayAdapter<Cinema>(context, viewResourceID, cinemas) {
    private var location: Location? = null
    private var originals: List<Cinema>? = null
    private var filter: CinemaIDFilter? = null
    fun setLocation(location: Location?) {
        this.location = location
        if (originals != null) {
            sortCinemas()
        }
    }

    fun setCinemas(newCinemas: List<Cinema>?) {
        cinemas.clear()
        cinemas.addAll(newCinemas!!)
        val wasSorting = originals != null
        originals = null
        if (wasSorting) {
            sortCinemas()
        }
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var theView = convertView
        if (theView == null) {
            val inflater = (context as Activity).layoutInflater
            theView = inflater.inflate(viewResourceID, parent, false)!!
        }
        val cinema = getItem(position)
        if (cinema != null) {
            val name = theView.findViewById<TextView>(R.id.name)
            name.text = cinema.name
            val distance = theView.findViewById<TextView>(R.id.distance)
            if (cinema.lat == null || cinema.lon == null || location == null) {
                distance.visibility = View.GONE
            } else {
                distance.visibility = View.VISIBLE
                distance.text = LocationUtil.getFormattedDistance(location, cinema.lat, cinema.lon)
            }
        }
        return theView
    }

    override fun getFilter(): Filter {
        if (filter == null) {
            filter = CinemaIDFilter()
        }
        return filter!!
    }

    private fun sortCinemas() {
        Collections.sort(cinemas, Comparator { c1, c2 ->
            if (location != null) {
                if (c1.lat != null && c1.lon != null && c2.lat != null && c2.lon != null) {
                    return@Comparator java.lang.Double.compare(LocationUtil.getDistance(location, c1.lat, c1.lon).toDouble(),
                            LocationUtil.getDistance(location, c2.lat, c2.lon).toDouble())
                } else if (c1.lat != null && c1.lon != null) { // c2 null, and c2 should go to bottom
                    return@Comparator -1
                } else if (c2.lat != null && c2.lon != null) { // c1 null, and c1 should go to bottom
                    return@Comparator 1
                }
            }
            c1.name.compareTo(c2.name, ignoreCase = true) // Default fallback
        })
        notifyDataSetChanged()
    }

    // Based on https://github.com/aosp-mirror/platform_frameworks_base/blob/master/core/java/android/widget/ArrayAdapter.java#L545
    private inner class CinemaIDFilter : Filter() {
        override fun performFiltering(constraint: CharSequence): FilterResults {
            val results = FilterResults()
            val values: MutableList<Cinema> = ArrayList()
            if (originals == null) {
                originals = ArrayList(cinemas)
            }
            values.addAll(originals!!)
            if (constraint.isEmpty()) {
                results.values = values
                results.count = values.size
            } else {
                val prefixString = constraint.toString().toLowerCase(Locale.getDefault())
                val newValues: MutableList<Cinema> = ArrayList()
                for (i in values.indices) {
                    val cinema = values[i]
                    val valueText = cinema.name.toLowerCase(Locale.getDefault())

                    // First match against the whole, non-splitted value
                    if (valueText.startsWith(prefixString)) {
                        newValues.add(cinema)
                    } else {
                        val words = valueText.split(" ").toTypedArray()
                        for (word in words) {
                            if (word.startsWith(prefixString)) {
                                newValues.add(cinema)
                                break
                            }
                        }
                    }
                }
                results.values = newValues
                results.count = newValues.size
            }
            return results
        }

        @Suppress("UNCHECKED_CAST")
        override fun publishResults(constraint: CharSequence, results: FilterResults) {
            val received = results.values as List<Cinema>
            cinemas.clear()
            cinemas.addAll(received)
            sortCinemas()
        }
    }
}