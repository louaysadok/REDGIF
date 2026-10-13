@file:OptIn(ExperimentalMaterial3Api::class)

package com.louaysadok.redgif.ui.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.louaysadok.redgif.data.local.RecentSearchEntity
import com.louaysadok.redgif.ui.common.appContainer
import kotlinx.coroutines.launch

/**
 * Search GIFs, images and creators. Recent queries are stored on the device.
 */
@Composable
fun SearchScreen(
    onBack: () -> Unit,
    onSearch: (String) -> Unit,
    onOpenCreator: (String) -> Unit,
) {
    val container = LocalContext.current.appContainer()
    val scope = rememberCoroutineScope()
    val dao = remember { container.database.recentSearches() }
    val recents by remember { dao.observe() }.collectAsStateWithLifecycle(initialValue = emptyList())

    var query by rememberSaveable { mutableStateOf("") }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }

    fun submit(raw: String) {
        val text = raw.trim()
        if (text.isEmpty()) return
        scope.launch { dao.upsert(RecentSearchEntity(text, System.currentTimeMillis())) }
        onSearch(text)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    TextField(
                        value = query,
                        onValueChange = { query = it },
                        singleLine = true,
                        placeholder = { Text("Search GIFs, images, tags") },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { submit(query) }),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focus),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = "" }) {
                            Icon(Icons.Filled.Clear, contentDescription = "Clear")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
            )
        },
    ) { inner ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner),
        ) {
            val trimmed = query.trim()
            if (trimmed.isNotEmpty()) {
                item(key = "search-now") {
                    ListItem(
                        headlineContent = { Text("Search for \"$trimmed\"") },
                        leadingContent = { Icon(Icons.Filled.Search, contentDescription = null) },
                        modifier = Modifier.clickable { submit(trimmed) },
                    )
                }
                item(key = "open-creator") {
                    ListItem(
                        headlineContent = { Text("Open creator @$trimmed") },
                        leadingContent = { Icon(Icons.Filled.Person, contentDescription = null) },
                        modifier = Modifier.clickable {
                            scope.launch { dao.upsert(RecentSearchEntity(trimmed, System.currentTimeMillis())) }
                            onOpenCreator(trimmed)
                        },
                    )
                }
            }

            item(key = "recent-header") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Recent",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.weight(1f),
                    )
                    if (recents.isNotEmpty()) {
                        TextButton(onClick = { scope.launch { dao.clear() } }) { Text("Clear all") }
                    }
                }
            }

            items(recents, key = { it.text }) { recent ->
                ListItem(
                    headlineContent = { Text(recent.text) },
                    leadingContent = { Icon(Icons.Filled.History, contentDescription = null) },
                    trailingContent = {
                        IconButton(onClick = { scope.launch { dao.delete(recent.text) } }) {
                            Icon(Icons.Filled.Clear, contentDescription = "Remove from recent")
                        }
                    },
                    modifier = Modifier.clickable { submit(recent.text) },
                )
            }
        }
    }
}
