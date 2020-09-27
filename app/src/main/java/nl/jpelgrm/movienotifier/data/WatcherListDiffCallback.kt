package nl.jpelgrm.movienotifier.data

import androidx.recyclerview.widget.DiffUtil
import nl.jpelgrm.movienotifier.models.Watcher

class WatcherListDiffCallback(private val oldList: List<Watcher>, private val newList: List<Watcher>) : DiffUtil.Callback() {
    override fun getOldListSize(): Int {
        return oldList.size
    }

    override fun getNewListSize(): Int {
        return newList.size
    }

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition].id == newList[newItemPosition].id
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        val oldWatcher = oldList[oldItemPosition]
        val newWatcher = newList[newItemPosition]
        return oldWatcher == newWatcher
    }
}