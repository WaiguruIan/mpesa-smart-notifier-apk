package com.example.smartnotify

import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.view.ViewGroup
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.smartnotify.ui.theme.SmartNotifyTheme
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SmartNotifyTheme {
                var isPermissionGranted by remember { mutableStateOf(isNotificationServiceEnabled()) }

                val lifecycleOwner = LocalLifecycleOwner.current
                DisposableEffect(lifecycleOwner) {
                    val observer = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_RESUME) {
                            isPermissionGranted = isNotificationServiceEnabled()
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose {
                        lifecycleOwner.lifecycle.removeObserver(observer)
                    }
                }

                if (!isPermissionGranted) {
                    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                        NotificationSettingsScreen(
                            modifier = Modifier.padding(innerPadding),
                            onOpenSettings = {
                                startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                            }
                        )
                    }
                } else {
                    MainAppScreen()
                }
            }
        }
    }

    private fun isNotificationServiceEnabled(): Boolean {
        val pkgName = packageName
        val flat = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
        if (!TextUtils.isEmpty(flat)) {
            val names = flat.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            for (name in names) {
                val cn = ComponentName.unflattenFromString(name)
                if (cn != null && TextUtils.equals(pkgName, cn.packageName)) return true
            }
        }
        return false
    }
}

@Composable
fun NotificationSettingsScreen(modifier: Modifier = Modifier, onOpenSettings: () -> Unit) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "M-LEDGER needs Notification Access.",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(16.dp)
        )
        Button(onClick = onOpenSettings) {
            Text("Grant Permission")
        }
    }
}

sealed class BottomNavItem(val route: String, val icon: ImageVector, val label: String) {
    data object Queued : BottomNavItem("queued", Icons.AutoMirrored.Filled.List, "Queued")
    data object Sent : BottomNavItem("sent", Icons.AutoMirrored.Filled.Send, "Sent")
    data object James : BottomNavItem("james", Icons.Default.Person, "James")
}

@Composable
fun MainAppScreen() {
    val navController = rememberNavController()
    Scaffold(
        bottomBar = { BottomNavigationBar(navController) }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = BottomNavItem.Queued.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(BottomNavItem.Queued.route) { MessageListScreen("QUEUED") }
            composable(BottomNavItem.Sent.route) { MessageListScreen("SENT") }
            composable(BottomNavItem.James.route) { JamesWebViewScreen() }
        }
    }
}

@Composable
fun BottomNavigationBar(navController: NavHostController) {
    val items = listOf(BottomNavItem.Queued, BottomNavItem.Sent, BottomNavItem.James)
    NavigationBar {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route
        items.forEach { item ->
            NavigationBarItem(
                icon = { Icon(item.icon, contentDescription = item.label) },
                label = { Text(item.label) },
                selected = currentRoute == item.route,
                onClick = {
                    navController.navigate(item.route) {
                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    }
}

@Composable
fun MessageListScreen(status: String) {
    val context = LocalContext.current
    val dao = remember { AppDatabase.getDatabase(context).mpesaDao() }
    val messages by if (status == "QUEUED") {
        dao.getQueuedMessagesFlow().collectAsState(initial = emptyList())
    } else {
        dao.getSentMessagesFlow().collectAsState(initial = emptyList())
    }

    if (messages.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No $status messages found.")
        }
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(messages) { message ->
                MessageItem(message)
                HorizontalDivider()
            }
        }
    }
}

@Composable
fun MessageItem(message: MpesaMessage) {
    val sdf = SimpleDateFormat("dd/MM/yy HH:mm", Locale.getDefault())
    val dateString = sdf.format(Date(message.timestamp))
    
    Column(modifier = Modifier.padding(16.dp)) {
        Text(text = message.rawBody, style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = dateString, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
    }
}

@Composable
fun JamesWebViewScreen() {
    AndroidView(
        factory = { context ->
            WebView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                webViewClient = WebViewClient()
                
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    databaseEnabled = true
                    loadWithOverviewMode = true
                    useWideViewPort = true
                    cacheMode = WebSettings.LOAD_DEFAULT
                }

                loadUrl("https://ntfy.sh/Monthly-Mpesa_report")
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}
