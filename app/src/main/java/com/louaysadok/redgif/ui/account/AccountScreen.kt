@file:OptIn(ExperimentalMaterial3Api::class)

package com.louaysadok.redgif.ui.account

import android.webkit.CookieManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.louaysadok.redgif.ui.common.appContainer
import kotlinx.coroutines.launch

/**
 * Account screen. RedGIFs has no public username/password API, so sign-in happens on the real
 * RedGIFs site inside [LoginScreen]. The password is entered on RedGIFs' own page and never
 * touches this app.
 */
@Composable
fun AccountScreen(
    onMenu: () -> Unit,
    onLogin: () -> Unit,
) {
    val container = LocalContext.current.appContainer()
    val scope = rememberCoroutineScope()
    val settings by container.settings.settings.collectAsStateWithLifecycle(initialValue = null)
    val signedIn = settings?.signedIn == true

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Account") },
                navigationIcon = {
                    IconButton(onClick = onMenu) {
                        Icon(Icons.Filled.Menu, contentDescription = "Menu")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
            )
        },
    ) { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = if (signedIn) "Signed in to RedGIFs" else "Not signed in",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = "Sign in with your RedGIFs account. The official RedGIFs login page opens inside " +
                            "this app. Your password is typed on RedGIFs' page and is never seen or stored by RedGIF.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }

            if (signedIn) {
                OutlinedButton(onClick = {
                    CookieManager.getInstance().removeAllCookies(null)
                    CookieManager.getInstance().flush()
                    scope.launch { container.settings.update { it.copy(signedIn = false) } }
                }) {
                    Text("Sign out and clear RedGIFs cookies")
                }
            } else {
                Button(onClick = onLogin) {
                    Text("Sign in to RedGIFs")
                }
            }
        }
    }
}
