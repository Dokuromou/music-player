package com.musica.player.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

object LocalStore {

    private const val PREFS_NAME = "musica_store"
    private const val KEY_FAVORITES = "favorites"
    private const val KEY_RECENT = "recently_played"
    private const val KEY_PLAYLISTS = "playlists"
    private const val KEY_AMOLED = "amoled"
    private const val RECENT_LIMIT = 50

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun loadFavorites(context: Context): Set<Long> =
        prefs(context).getStringSet(KEY_FAVORITES, emptySet())
            ?.mapNotNull { it.toLongOrNull() }
            ?.toSet()
            ?: emptySet()

    fun saveFavorites(context: Context, favorites: Set<Long>) {
        prefs(context).edit()
            .putStringSet(KEY_FAVORITES, favorites.map { it.toString() }.toSet())
            .apply()
    }

    fun loadRecentlyPlayed(context: Context): List<Long> {
        val raw = prefs(context).getString(KEY_RECENT, null) ?: return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            (0 until arr.length()).mapNotNull { arr.optLong(it, -1L).takeIf { v -> v >= 0 } }
        }.getOrDefault(emptyList())
    }

    fun saveRecentlyPlayed(context: Context, ids: List<Long>) {
        val arr = JSONArray()
        ids.take(RECENT_LIMIT).forEach { arr.put(it) }
        prefs(context).edit().putString(KEY_RECENT, arr.toString()).apply()
    }

    fun loadPlaylists(context: Context): List<Playlist> {
        val raw = prefs(context).getString(KEY_PLAYLISTS, null) ?: return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { i ->
                val obj = arr.getJSONObject(i)
                val ids = obj.getJSONArray("songIds")
                val songIds = mutableListOf<Long>()
                for (j in 0 until ids.length()) songIds.add(ids.getLong(j))
                Playlist(
                    id = obj.getLong("id"),
                    name = obj.getString("name"),
                    songIds = songIds
                )
            }
        }.getOrDefault(emptyList())
    }

    fun savePlaylists(context: Context, playlists: List<Playlist>) {
        val arr = JSONArray()
        playlists.forEach { p ->
            val obj = JSONObject()
            obj.put("id", p.id)
            obj.put("name", p.name)
            val ids = JSONArray()
            p.songIds.forEach { ids.put(it) }
            obj.put("songIds", ids)
            arr.put(obj)
        }
        prefs(context).edit().putString(KEY_PLAYLISTS, arr.toString()).apply()
    }

    fun loadAmoled(context: Context): Boolean = prefs(context).getBoolean(KEY_AMOLED, false)

    fun saveAmoled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_AMOLED, enabled).apply()
    }
}
