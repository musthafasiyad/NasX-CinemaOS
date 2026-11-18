package com.nasx.cinemaos

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.KeyEvent as AndroidKeyEvent
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch

data class InstalledApp(
    val name: String,
    val packageName: String
)

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = android.graphics.Color.BLACK
        window.navigationBarColor = android.graphics.Color.BLACK

        setContent {
            MaterialTheme {
                NaSXHome(
                    onLaunchApp = { app ->
                        launchInstalledApp(app)
                    }
                )
            }
        }
    }

    private fun launchInstalledApp(app: InstalledApp) {
        try {
            val launchIntent =
                packageManager.getLeanbackLaunchIntentForPackage(
                    app.packageName
                ) ?: packageManager.getLaunchIntentForPackage(
                    app.packageName
                )

            if (launchIntent != null) {
                startActivity(launchIntent)
            } else {
                Toast.makeText(
                    this,
                    "${app.name} does not provide a launch activity",
                    Toast.LENGTH_LONG
                ).show()
            }

        } catch (exception: Exception) {
            Toast.makeText(
                this,
                "Unable to open ${app.name}: ${exception.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }
}

@Composable
fun NaSXHome(
    onLaunchApp: (InstalledApp) -> Unit
) {
    val context = LocalContext.current
    val apps = remember { mutableStateListOf<InstalledApp>() }

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    val appKeys = apps.map { it.packageName }

    val focusRequesters = remember(appKeys) {
        appKeys.map { FocusRequester() }
    }

    // Discover installed Android TV and launcher applications.

    LaunchedEffect(context) {
        val pm = context.packageManager
        val discoveredApps = mutableListOf<InstalledApp>()

        val categories = listOf(
            Intent.CATEGORY_LEANBACK_LAUNCHER,
            Intent.CATEGORY_LAUNCHER
        )

        categories.forEach { category ->

            val intent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(category)
            }

            val activities = pm.queryIntentActivities(
                intent,
                PackageManager.MATCH_DEFAULT_ONLY
            )

            activities.forEach { activity ->

                val packageName = activity.activityInfo.packageName

                if (
                    packageName != context.packageName &&
                    discoveredApps.none {
                        it.packageName == packageName
                    }
                ) {
                    discoveredApps.add(
                        InstalledApp(
                            name = activity.loadLabel(pm).toString(),
                            packageName = packageName
                        )
                    )
                }
            }
        }

        apps.clear()
        apps.addAll(
            discoveredApps.sortedBy {
                it.name.lowercase()
            }
        )
    }

    // Automatically focus the first app after discovery.

    LaunchedEffect(appKeys) {
        if (focusRequesters.isNotEmpty()) {
            focusRequesters.first().requestFocus()
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF08090D)
    ) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    horizontal = 42.dp,
                    vertical = 26.dp
                )
        ) {

            // TOP NAVIGATION

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {

                Text(
                    text = "NaSX",
                    color = Color(0xFFD4AF37),
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Black
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(28.dp)
                ) {
                    Text(
                        "HOME",
                        color = Color.White,
                        fontSize = 14.sp
                    )

                    Text(
                        "MOVIES",
                        color = Color.LightGray,
                        fontSize = 14.sp
                    )

                    Text(
                        "SERIES",
                        color = Color.LightGray,
                        fontSize = 14.sp
                    )

                    Text(
                        "APPS",
                        color = Color.LightGray,
                        fontSize = 14.sp
                    )
                }

                Text(
                    text = "● PROJECTOR READY",
                    color = Color(0xFF65D99A),
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // FEATURED BANNER

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(205.dp)
                    .background(
                        Color(0xFF191C25),
                        RoundedCornerShape(18.dp)
                    )
                    .padding(28.dp),
                verticalArrangement = Arrangement.Center
            ) {

                Text(
                    text = "NaSX ORIGINAL EXPERIENCE",
                    color = Color(0xFFD4AF37),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "CINEMA BEYOND REALITY",
                    color = Color.White,
                    fontSize = 29.sp,
                    fontWeight = FontWeight.Black
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Your entertainment. Your big screen.",
                    color = Color.LightGray,
                    fontSize = 15.sp
                )
            }

            Spacer(modifier = Modifier.height(25.dp))

            // APPLICATION SECTION

            Text(
                text = "YOUR APPLICATIONS",
                color = Color.White,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(14.dp))

            if (apps.isEmpty()) {

                Text(
                    text = "Searching for installed applications...",
                    color = Color.LightGray,
                    fontSize = 14.sp
                )

            } else {

                LazyRow(
                    state = listState,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(
                        vertical = 12.dp,
                        horizontal = 4.dp
                    )
                ) {

                    itemsIndexed(
                        items = apps,
                        key = { _, app -> app.packageName }
                    ) { index, app ->

                        AppCard(
                            app = app,
                            focusRequester = focusRequesters[index],
                            onLaunch = {
                                onLaunchApp(app)
                            },
                            onMoveLeft = {
                                if (index > 0) {
                                    coroutineScope.launch {
                                        listState.animateScrollToItem(
                                            index - 1
                                        )
                                        focusRequesters[index - 1]
                                            .requestFocus()
                                    }
                                }
                            },
                            onMoveRight = {
                                if (index < apps.lastIndex) {
                                    coroutineScope.launch {
                                        listState.animateScrollToItem(
                                            index + 1
                                        )
                                        focusRequesters[index + 1]
                                            .requestFocus()
                                    }
                                }
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = "NaSX CINEMA OS  •  ANDROID TV EXPERIENCE",
                color = Color(0xFF777B86),
                fontSize = 11.sp
            )
        }
    }
}

@Composable
fun AppCard(
    app: InstalledApp,
    focusRequester: FocusRequester,
    onLaunch: () -> Unit,
    onMoveLeft: () -> Unit,
    onMoveRight: () -> Unit
) {

    var isFocused by remember {
        mutableStateOf(false)
    }

    Card(
        onClick = onLaunch,

        modifier = Modifier
            .width(175.dp)
            .height(100.dp)
            .focusRequester(focusRequester)
            .onFocusChanged {
                isFocused = it.isFocused
            }
            .onPreviewKeyEvent { event ->

                if (event.type != KeyEventType.KeyDown) {
                    false
                } else {

                    when (event.key) {

                        Key.DirectionLeft -> {
                            onMoveLeft()
                            true
                        }

                        Key.DirectionRight -> {
                            onMoveRight()
                            true
                        }

                        Key.DirectionCenter,
                        Key.Enter,
                        Key.NumPadEnter -> {
                            onLaunch()
                            true
                        }

                        else -> false
                    }
                }
            },

        shape = RoundedCornerShape(14.dp),

        colors = CardDefaults.cardColors(
            containerColor = if (isFocused) {
                Color(0xFF35313A)
            } else {
                Color(0xFF1B1D25)
            }
        )
    ) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (isFocused) {
                        Modifier.border(
                            width = 3.dp,
                            color = Color(0xFFD4AF37),
                            shape = RoundedCornerShape(14.dp)
                        )
                    } else {
                        Modifier
                    }
                )
                .padding(12.dp),

            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Text(
                text = app.name,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = if (isFocused) "PRESS OK TO OPEN" else "OPEN APP",
                color = Color(0xFFD4AF37),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}