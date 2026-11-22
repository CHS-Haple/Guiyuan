package com.chaners.guiyuan.ui.screens

import android.app.LocaleConfig
import android.content.Context
import android.content.res.Configuration
import android.icu.text.Transliterator
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

private fun foldSearch(text: String): String =
    text.lowercase(Locale.ROOT).filter(Char::isLetterOrDigit)

// Compiled once for all locale titles and aliases when the search index is built.
private val SEARCH_TOKEN_SEPARATOR = Regex("[^a-z0-9]+")

// Index terms, not configuration values. The native ICU table supplies Han pronunciations.
internal class SearchTerm(
    text: String,
    romanized: String,
    foreign: Boolean = false,
) {
    private val allowInitials = !foreign || text.any { it.isLetter() && it.code > 127 }
    private val plain = foldSearch(text)
    private val tokens = romanized.lowercase(Locale.ROOT)
        .split(SEARCH_TOKEN_SEPARATOR)
        .filter(String::isNotEmpty)
    private val full = tokens.joinToString("")
    private val initials = tokens.joinToString("") { it.take(1) }

    fun matches(input: String, includeInitials: Boolean = true): Boolean =
        matchesFolded(foldSearch(input), includeInitials)

    fun matchesInitials(input: String): Boolean =
        matchesInitialsFolded(foldSearch(input))

    internal fun matchesFolded(term: String, includeInitials: Boolean = true): Boolean =
        term.isNotEmpty() &&
            (term in plain || term in full || (includeInitials && allowInitials && term in initials))

    internal fun matchesInitialsFolded(term: String): Boolean =
        term.isNotEmpty() && allowInitials && term in initials
}

internal fun isSearchFiltering(active: Boolean, query: String): Boolean =
    active && query.any(Char::isLetterOrDigit)


// Official translated names always participate; foreign pinyin initials are
// only a fallback when no title in either language matches more explicitly.
internal class SearchMatches(
    ids: IntArray,
    private val current: (Int) -> Boolean,
    private val aliases: (Int) -> Boolean,
    private val translated: (Int) -> Boolean,
    private val translatedInitials: (Int) -> Boolean,
) {
    private val useForeignInitials = ids.none { current(it) || aliases(it) || translated(it) }

    fun matches(id: Int): Boolean =
        current(id) || aliases(id) || translated(id) ||
            (useForeignInitials && translatedInitials(id))
}

internal class PageSearch(private val context: Context) {
    var active by mutableStateOf(false)
        private set
    var query by mutableStateOf("")
        private set

    val filtering get() = isSearchFiltering(active, query)

    private data class LocalizedTerms(
        val current: SearchTerm,
        val other: List<SearchTerm>,
    )

    private data class Index(
        val titles: Map<Int, LocalizedTerms>,
        val aliases: Map<Int, List<SearchTerm>>,
    )

    private var index by mutableStateOf<Index?>(null)

    // ICU and locale-specific resources are resolved once, before keystrokes, off the UI thread.
    suspend fun prepare() {
        if (index != null) return
        val built = withContext(Dispatchers.Default) { buildIndex() }
        index = built
    }

    private val matchSet by derivedStateOf {
        if (!filtering) {
            null
        } else {
            val terms = index
            val foldedQuery = foldSearch(query)
            SearchMatches(
                FeatureSearchIds.all,
                { id -> terms?.titles?.get(id)?.current?.matchesFolded(foldedQuery) ?: quickMatch(id, foldedQuery) },
                { id -> terms?.aliases?.get(id)?.any { it.matchesFolded(foldedQuery) } ?: quickAlias(id, foldedQuery) },
                { id -> terms?.titles?.get(id)?.other?.any { it.matchesFolded(foldedQuery, includeInitials = false) } == true },
                { id -> terms?.titles?.get(id)?.other?.any { it.matchesInitialsFolded(foldedQuery) } == true },
            )
        }
    }

    fun open() { active = true }
    fun close() {
        active = false
        query = ""
    }

    fun update(value: String) { query = value.take(80) }

    fun matches(@StringRes title: Int): Boolean =
        matchSet?.matches(title) ?: true

    fun any(vararg ids: Int): Boolean = anyFrom(ids)

    // Reuse stable feature ID arrays instead of copying them via vararg spread
    // during Compose recomposition.
    fun anyFrom(ids: IntArray): Boolean =
        !filtering || ids.any(::matches)

    // The index may still be preparing when the user types immediately after opening the page.
    // Use only cheap current-language and curated-name checks until the complete index arrives.
    private fun quickMatch(@StringRes id: Int, foldedQuery: String): Boolean =
        foldSearch(context.getString(id)).contains(foldedQuery)

    private fun quickAlias(@StringRes id: Int, foldedQuery: String): Boolean =
        searchAliases(id).any { foldSearch(it).contains(foldedQuery) }

    private fun buildIndex(): Index {
        val romanizer = runCatching {
            Transliterator.getInstance("Han-Latin; Latin-ASCII")
        }.getOrNull()
        val contexts = buildList {
            add(context)
            val supported = runCatching { LocaleConfig(context).supportedLocales }.getOrNull()
            if (supported != null) {
                for (i in 0 until supported.size()) {
                    val localized = runCatching {
                        val config = Configuration(context.resources.configuration)
                        config.setLocale(supported[i])
                        context.createConfigurationContext(config)
                    }.getOrNull()
                    if (localized != null) add(localized)
                }
            }
        }
        fun term(text: String, foreign: Boolean = false) = SearchTerm(
            text,
            runCatching { romanizer?.transliterate(text) }.getOrNull() ?: text,
            foreign,
        )
        return Index(
            titles = FeatureSearchIds.all.associateWith { id ->
                val names = contexts.map { it.getString(id) }.distinct()
                LocalizedTerms(
                    current = term(names.first()),
                    other = names.drop(1).map { term(it, foreign = true) },
                )
            },
            aliases = FeatureSearchIds.all.associateWith { id ->
                searchAliases(id).map { term(it) }
            },
        )
    }
}

// Curated task vocabulary has lower recall than free-text descriptions but avoids
// pulling unrelated controls into searches through incidental summary words.
@Composable
internal fun rememberPageSearch(): PageSearch {
    val context = LocalContext.current
    val config = LocalConfiguration.current
    val search = remember(context, config) { PageSearch(context) }
    LaunchedEffect(search) { search.prepare() }
    return search
}
