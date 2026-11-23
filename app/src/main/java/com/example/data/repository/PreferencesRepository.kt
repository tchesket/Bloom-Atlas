package com.example.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.AudienceMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.bloomDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "bloom_atlas_prefs"
)

data class UserPreferencesState(
    val bookmarkedArticleIds: Set<String> = emptySet(),
    val bookmarkedStudyNumbers: Set<Int> = emptySet(),
    val masteredMythIds: Set<String> = emptySet(),
    val audienceMode: AudienceMode = AudienceMode.EVERYONE
)

class PreferencesRepository(private val context: Context) {

    private object Keys {
        val BOOKMARKED_ARTICLES = stringSetPreferencesKey("bookmarked_articles")
        val BOOKMARKED_STUDIES = stringSetPreferencesKey("bookmarked_studies")
        val MASTERED_MYTHS = stringSetPreferencesKey("mastered_myths")
        val AUDIENCE_MODE = stringPreferencesKey("audience_mode")
    }

    val preferencesFlow: Flow<UserPreferencesState> = context.bloomDataStore.data.map { prefs ->
        val articleIds = prefs[Keys.BOOKMARKED_ARTICLES] ?: emptySet()
        val studyNumbers = (prefs[Keys.BOOKMARKED_STUDIES] ?: emptySet())
            .mapNotNull { it.toIntOrNull() }
            .toSet()
        val mythIds = prefs[Keys.MASTERED_MYTHS] ?: emptySet()
        val modeName = prefs[Keys.AUDIENCE_MODE] ?: AudienceMode.EVERYONE.name
        val audienceMode = AudienceMode.entries.firstOrNull { it.name == modeName }
            ?: AudienceMode.EVERYONE
        UserPreferencesState(
            bookmarkedArticleIds = articleIds,
            bookmarkedStudyNumbers = studyNumbers,
            masteredMythIds = mythIds,
            audienceMode = audienceMode
        )
    }

    suspend fun toggleArticleBookmark(articleId: String) {
        context.bloomDataStore.edit { prefs ->
            val current = prefs[Keys.BOOKMARKED_ARTICLES] ?: emptySet()
            prefs[Keys.BOOKMARKED_ARTICLES] =
                if (articleId in current) current - articleId else current + articleId
        }
    }

    suspend fun toggleStudyBookmark(studyNumber: Int) {
        val keyStr = studyNumber.toString()
        context.bloomDataStore.edit { prefs ->
            val current = prefs[Keys.BOOKMARKED_STUDIES] ?: emptySet()
            prefs[Keys.BOOKMARKED_STUDIES] =
                if (keyStr in current) current - keyStr else current + keyStr
        }
    }

    suspend fun markMythMastered(mythId: String) {
        context.bloomDataStore.edit { prefs ->
            val current = prefs[Keys.MASTERED_MYTHS] ?: emptySet()
            prefs[Keys.MASTERED_MYTHS] = current + mythId
        }
    }

    suspend fun setAudienceMode(mode: AudienceMode) {
        context.bloomDataStore.edit { prefs ->
            prefs[Keys.AUDIENCE_MODE] = mode.name
        }
    }
}
