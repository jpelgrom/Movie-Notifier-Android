package nl.jpelgrm.movienotifier.data

import androidx.recyclerview.widget.DiffUtil
import nl.jpelgrm.movienotifier.models.User

class AccountListDiffCallback(private val oldList: List<User>, private val newList: List<User>, private val oldActive: String, private val newActive: String) : DiffUtil.Callback() {
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
        val oldUser = oldList[oldItemPosition]
        val newUser = newList[newItemPosition]
        return oldUser.id == newUser.id && oldUser.name == newUser.name
                && oldUser.email == newUser.email && oldUser.apikey == newUser.apikey && oldActive == newActive
                && (oldActive == oldUser.id || newActive == newUser.id)
    }
}