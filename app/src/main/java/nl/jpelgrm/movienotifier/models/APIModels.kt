package nl.jpelgrm.movienotifier.models

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

data class UserLogin(
        @Expose val name: String,
        @Expose val password: String
)

data class Watcher(
        @Expose(serialize = false) var id: String? = null,
        @Expose var userid: String? = null,
        @Expose var name: String? = null,
        @Expose var movieid: Int? = null,
        @Expose var begin: Long? = null,
        @Expose var end: Long? = null,
        @Expose var filters: WatcherFilters? = null
)

enum class WatcherFilterValue {
    @SerializedName("yes") YES,
    @SerializedName("no-preference") NOPREFERENCE,
    @SerializedName("no") NO
}

data class WatcherFilters(
        @Expose var cinemaid: Int? = null,
        @Expose var startafter: Long? = null,
        @Expose var startbefore: Long? = null,
        @Expose var ov: WatcherFilterValue = WatcherFilterValue.NOPREFERENCE,
        @Expose var nl: WatcherFilterValue = WatcherFilterValue.NOPREFERENCE,
        @Expose var imax: WatcherFilterValue = WatcherFilterValue.NOPREFERENCE,
        @Expose @SerializedName("3d") var _3d: WatcherFilterValue = WatcherFilterValue.NOPREFERENCE,
        @Expose var hfr: WatcherFilterValue = WatcherFilterValue.NOPREFERENCE,
        @Expose @SerializedName("4k") var _4k: WatcherFilterValue = WatcherFilterValue.NOPREFERENCE,
        @Expose var laser: WatcherFilterValue = WatcherFilterValue.NOPREFERENCE,
        @Expose @SerializedName("4dx") var _4dx: WatcherFilterValue = WatcherFilterValue.NOPREFERENCE,
        @Expose var screenx: WatcherFilterValue = WatcherFilterValue.NOPREFERENCE,
        @Expose var dolbycinema: WatcherFilterValue = WatcherFilterValue.NOPREFERENCE,
        @Expose var dolbyatmos: WatcherFilterValue = WatcherFilterValue.NOPREFERENCE,
        @Expose var regularshowing: WatcherFilterValue = WatcherFilterValue.NOPREFERENCE
)