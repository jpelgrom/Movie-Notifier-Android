package nl.jpelgrm.movienotifier.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class UserLogin(
        val name: String,
        val password: String
)

@JsonClass(generateAdapter = true)
data class Watcher(
        var id: String? = null,
        var userid: String? = null,
        var name: String? = null,
        var movieid: Int? = null,
        var begin: Long? = null,
        var end: Long? = null,
        var filters: WatcherFilters? = null
)

@JsonClass(generateAdapter = false)
enum class WatcherFilterValue {
    @Json(name = "yes") YES,
    @Json(name = "no-preference") NOPREFERENCE,
    @Json(name = "no") NO
}

@JsonClass(generateAdapter = true)
data class WatcherFilters(
        var cinemaid: Int? = null,
        var startafter: Long? = null,
        var startbefore: Long? = null,
        var ov: WatcherFilterValue = WatcherFilterValue.NOPREFERENCE,
        var nl: WatcherFilterValue = WatcherFilterValue.NOPREFERENCE,
        var imax: WatcherFilterValue = WatcherFilterValue.NOPREFERENCE,
        @Json(name = "3d") var _3d: WatcherFilterValue = WatcherFilterValue.NOPREFERENCE,
        var hfr: WatcherFilterValue = WatcherFilterValue.NOPREFERENCE,
        @Json(name = "4k") var _4k: WatcherFilterValue = WatcherFilterValue.NOPREFERENCE,
        var laser: WatcherFilterValue = WatcherFilterValue.NOPREFERENCE,
        @Json(name = "4dx") var _4dx: WatcherFilterValue = WatcherFilterValue.NOPREFERENCE,
        var screenx: WatcherFilterValue = WatcherFilterValue.NOPREFERENCE,
        var dolbycinema: WatcherFilterValue = WatcherFilterValue.NOPREFERENCE,
        var dolbyatmos: WatcherFilterValue = WatcherFilterValue.NOPREFERENCE,
        var regularshowing: WatcherFilterValue = WatcherFilterValue.NOPREFERENCE
)