
package com.nasx.cinemaos

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// --------------------------------------------------
// DATA MODELS
// --------------------------------------------------

data class InstalledApp(
    val name: String,
    val packageName: String,
    val icon: Drawable
)

data class StreamingService(
    val name: String,
    val shortName: String,
    val packageNames: List<String>,
    val color: Color
)

data class Movie(
    val title: String,
    val subtitle: String,
    val colors: List<Color>
)

// --------------------------------------------------
// HIDDEN SYSTEM APPLICATIONS
// --------------------------------------------------

private val hiddenPackages = setOf(
    "com.android.settings",
    "com.android.tv.settings",
    "com.android.tv.remote.service",
    "com.google.android.tv.remote.service",
    "com.nasx.cinemaos"
)

// --------------------------------------------------
// STREAMING SERVICES
// --------------------------------------------------

private val streamingServices = listOf(

    StreamingService(
        "Netflix",
        "N",
        listOf("com.netflix.ninja"),
        Color(0xFFB20710)
    ),

    StreamingService(
        "Prime Video",
        "prime video",
        listOf("com.amazon.amazonvideo.livingroom"),
        Color(0xFF0879B9)
    ),

    StreamingService(
        "Disney+",
        "Disney+",
        listOf("com.disney.disneyplus"),
        Color(0xFF182A70)
    ),

    StreamingService(
        "Apple TV",
        "Apple TV",
        listOf("com.apple.atve.androidtv.appletv"),
        Color(0xFF33343A)
    ),

    StreamingService(
        "YouTube",
        "▶ YouTube",
        listOf("com.google.android.youtube.tv"),
        Color(0xFFB51220)
    ),

    StreamingService(
        "JioHotstar",
        "JioHotstar",
        listOf("in.startv.hotstar"),
        Color(0xFF5420A8)
    ),

    StreamingService(
        "Sony LIV",
        "SONY liv",
        listOf("com.sonyliv"),
        Color(0xFF182B59)
    ),

    StreamingService(
        "ZEE5",
        "ZEE5",
        listOf("com.graymatrix.did"),
        Color(0xFF39205C)
    ),

    StreamingService(
        "Spotify",
        "Spotify",
        listOf("com.spotify.tv.android"),
        Color(0xFF125A35)
    ),

    StreamingService(
        "VLC",
        "VLC",
        listOf("org.videolan.vlc"),
        Color(0xFF8D4A12)
    )
)

// --------------------------------------------------
// TRENDING MOVIES - DEMO DATA
// --------------------------------------------------

private val movies = listOf(

    Movie(
        "THE BATMAN",
        "DARK KNIGHT",
        listOf(Color(0xFF580D16), Color(0xFF090B12))
    ),

    Movie(
        "OPPENHEIMER",
        "A STORY OF FIRE",
        listOf(Color(0xFFB44A16), Color(0xFF17100D))
    ),

    Movie(
        "INTERSTELLAR",
        "BEYOND THE STARS",
        listOf(Color(0xFF263B50), Color(0xFF080D18))
    ),

    Movie(
        "JOKER",
        "PUT ON A HAPPY FACE",
        listOf(Color(0xFF70202A), Color(0xFF140D14))
    ),

    Movie(
        "INCEPTION",
        "DREAM WITHIN A DREAM",
        listOf(Color(0xFF153D51), Color(0xFF080D18))
    ),

    Movie(
        "TENET",
        "TIME IS THE ENEMY",
        listOf(Color(0xFF53616C), Color(0xFF10151C))
    ),

    Movie(
        "DUNE PART TWO",
        "THE DESERT AWAKENS",
        listOf(Color(0xFFB65C28), Color(0xFF27130C))
    ),

    Movie(
        "AVATAR",
        "THE WAY OF WATER",
        listOf(Color(0xFF12617A), Color(0xFF071923))
    )
)

// --------------------------------------------------
// MAIN ACTIVITY
// --------------------------------------------------

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
                    "${app.name} cannot be opened",
                    Toast.LENGTH_SHORT
                ).show()
            }

        } catch (_: Exception) {

            Toast.makeText(
                this,
                "Unable to open ${app.name}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}

// --------------------------------------------------
// INSTALLED APPLICATION DISCOVERY
// --------------------------------------------------

private fun discoverInstalledApps(
    packageManager: PackageManager
): List<InstalledApp> {

    val result = mutableMapOf<String, InstalledApp>()

    val categories = listOf(
        Intent.CATEGORY_LEANBACK_LAUNCHER,
        Intent.CATEGORY_LAUNCHER
    )

    categories.forEach { category ->

        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(category)
        }

        val activities = packageManager.queryIntentActivities(
            intent,
            0
        )

        activities.forEach { activity ->

            val packageName = activity.activityInfo.packageName

            try {

                val appName =
                    activity.loadLabel(packageManager).toString()

                val isRemote = appName.trim().equals(
                    "Android TV Remote",
                    ignoreCase = true
                )

                if (
                    packageName !in hiddenPackages &&
                    !isRemote &&
                    packageName !in result
                ) {

                    result[packageName] = InstalledApp(
                        name = appName,
                        packageName = packageName,
                        icon = activity.loadIcon(packageManager)
                    )
                }

            } catch (_: Exception) {
                // Skip apps with unavailable icons.
            }
        }
    }

    return result.values.sortedBy {
        it.name.lowercase()
    }
}

// --------------------------------------------------
// NaSX HOME SCREEN
// --------------------------------------------------

@Composable
fun NaSXHome(
    onLaunchApp: (InstalledApp) -> Unit
) {

    val context = LocalContext.current

    var apps by remember {
        mutableStateOf<List<InstalledApp>>(emptyList())
    }

    var loading by remember {
        mutableStateOf(true)
    }

    var selectedTab by remember {
        mutableStateOf("Home")
    }

    var savedToList by remember {
        mutableStateOf(false)
    }

    var showAllApps by remember {
        mutableStateOf(false)
    }

    // Discover installed apps

    LaunchedEffect(Unit) {

        loading = true

        apps = withContext(Dispatchers.IO) {
            discoverInstalledApps(context.packageManager)
        }

        loading = false
    }

    // Match streaming services with installed apps

    val installedServices = streamingServices.mapNotNull { service ->

        val installed = apps.firstOrNull {
            it.packageName in service.packageNames
        }

        if (installed != null) {
            service to installed
        } else {
            null
        }
    }

    val promotionalServices = streamingServices.filter { service ->

        apps.none {
            it.packageName in service.packageNames
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
                    horizontal = 30.dp,
                    vertical = 12.dp
                )
        ) {

            // ------------------------------------------
            // TOP NAVIGATION
            // ------------------------------------------

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),

                verticalAlignment = Alignment.CenterVertically
            ) {

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Text(
                        "NaS",
                        color = Color.White,
                        fontSize = 29.sp,
                        fontWeight = FontWeight.Black
                    )

                    Text(
                        "X",
                        color = Color(0xFFE51B35),
                        fontSize = 29.sp,
                        fontWeight = FontWeight.Black
                    )
                }

                Spacer(Modifier.width(12.dp))

                Text(
                    "│ CINEMA OS 2.0",
                    color = Color.Gray,
                    fontSize = 10.sp
                )

                Spacer(Modifier.weight(1f))

                Row(
                    modifier = Modifier.horizontalScroll(
                        rememberScrollState()
                    ),

                    verticalAlignment = Alignment.CenterVertically
                ) {

                    listOf(
                        "Home",
                        "Movies",
                        "Series",
                        "Apps",
                        "Library",
                        "Settings"
                    ).forEach { tab ->

                        val selected = selectedTab == tab

                        Box(
                            modifier = Modifier
                                .padding(horizontal = 3.dp)
                                .clip(RoundedCornerShape(22.dp))
                                .background(
                                    if (selected) {
                                        Color(0xFF47151D)
                                    } else {
                                        Color.Transparent
                                    }
                                )
                                .border(
                                    width = if (selected) 1.dp else 0.dp,
                                    color = if (selected) {
                                        Color(0xFFE52A40)
                                    } else {
                                        Color.Transparent
                                    },
                                    shape = RoundedCornerShape(22.dp)
                                )
                                .clickable {
                                    selectedTab = tab
                                }
                                .padding(
                                    horizontal = 12.dp,
                                    vertical = 9.dp
                                )
                        ) {

                            Text(
                                tab,
                                color = if (selected) {
                                    Color.White
                                } else {
                                    Color.LightGray
                                },
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Spacer(Modifier.width(14.dp))

                Text(
                    "◉  01:30 AM",
                    color = Color.White,
                    fontSize = 10.sp
                )
            }

            Spacer(Modifier.height(12.dp))

            // ------------------------------------------
            // CINEMATIC HERO BANNER
            // ------------------------------------------

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(275.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                Color(0xFF100C0D),
                                Color(0xFF6A321F),
                                Color(0xFF17121A)
                            )
                        )
                    )
            ) {

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                0.0f to Color(0xFF08090D),
                                0.45f to Color.Transparent,
                                1.0f to Color(0x66000000)
                            )
                        )
                )

                Column(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(
                            start = 30.dp,
                            end = 20.dp
                        )
                ) {

                    Text(
                        "NaSX ORIGINAL",
                        color = Color(0xFFE52A40),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(Modifier.height(12.dp))

                    Text(
                        "D U N E",
                        color = Color.White,
                        fontSize = 37.sp,
                        fontWeight = FontWeight.Light
                    )

                    Text(
                        "P A R T   T W O",
                        color = Color.White,
                        fontSize = 13.sp,
                        letterSpacing = 5.sp
                    )

                    Spacer(Modifier.height(14.dp))

                    Text(
                        "4K UHD  •  DOLBY VISION  •  DOLBY ATMOS",
                        color = Color(0xFFE7D3B4),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(Modifier.height(12.dp))

                    Text(
                        "A cinematic journey beyond imagination.",
                        color = Color.LightGray,
                        fontSize = 12.sp
                    )

                    Spacer(Modifier.height(17.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(9.dp))
                                .background(Color(0xFFE51B35))
                                .clickable {

                                    Toast.makeText(
                                        context,
                                        "Featured movie demo",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                                .padding(
                                    horizontal = 19.dp,
                                    vertical = 11.dp
                                )
                        ) {

                            Text(
                                "▶  Watch Now",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(9.dp))
                                .background(Color(0x66333333))
                                .border(
                                    1.dp,
                                    Color(0xFF777777),
                                    RoundedCornerShape(9.dp)
                                )
                                .clickable {
                                    savedToList = !savedToList
                                }
                                .padding(
                                    horizontal = 17.dp,
                                    vertical = 11.dp
                                )
                        ) {

                            Text(
                                if (savedToList) {
                                    "✓ My List"
                                } else {
                                    "+ My List"
                                },
                                color = Color.White,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                Text(
                    "IMAX ENHANCED",
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(18.dp),
                    color = Color(0xFFE8D1AA),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(17.dp))

            // ------------------------------------------
            // STREAMING APPLICATIONS
            // ------------------------------------------

            SectionTitle("Streaming Apps")

            Spacer(Modifier.height(9.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(9.dp),

                contentPadding = PaddingValues(
                    horizontal = 2.dp,
                    vertical = 3.dp
                )
            ) {

                // Installed services

                items(installedServices) { pair ->

                    val service = pair.first
                    val app = pair.second

                    ServiceCard(
                        name = service.name,
                        label = service.shortName,
                        color = service.color,
                        installed = true,
                        onClick = {
                            onLaunchApp(app)
                        }
                    )
                }

                // Promotional services

                items(promotionalServices) { service ->

                    ServiceCard(
                        name = service.name,
                        label = service.shortName,
                        color = service.color,
                        installed = false,
                        onClick = {

                            Toast.makeText(
                                context,
                                "${service.name} is not installed",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    )
                }

                // All applications tile

                item {

                    ServiceCard(
                        name = "All Apps",
                        label = "▦",
                        color = Color(0xFF292B33),
                        installed = true,
                        onClick = {
                            showAllApps = !showAllApps
                        }
                    )
                }
            }

            if (showAllApps) {

                Spacer(Modifier.height(8.dp))

                Text(
                    if (loading) {
                        "Loading apps..."
                    } else {
                        "Installed apps: " +
                                apps.joinToString("  •  ") {
                                    it.name
                                }
                    },
                    color = Color.LightGray,
                    fontSize = 10.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.height(15.dp))

            // ------------------------------------------
            // TRENDING MOVIES
            // ------------------------------------------

            SectionTitle("Trending Movies")

            Spacer(Modifier.height(9.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(11.dp),

                contentPadding = PaddingValues(
                    horizontal = 2.dp,
                    vertical = 3.dp
                )
            ) {

                items(movies) { movie ->

                    MovieCard(movie)
                }
            }

            Spacer(Modifier.weight(1f))

            // ------------------------------------------
            // FOOTER
            // ------------------------------------------

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {

                Text(
                    "NaSX CINEMA OS 2.0",
                    color = Color(0xFF777B86),
                    fontSize = 9.sp
                )

                Text(
                    if (loading) {
                        "DISCOVERING APPS..."
                    } else {
                        "${apps.size} APPLICATIONS DISCOVERED"
                    },
                    color = Color(0xFF777B86),
                    fontSize = 9.sp
                )
            }
        }
    }
}

// --------------------------------------------------
// SECTION TITLE
// --------------------------------------------------

@Composable
fun SectionTitle(title: String) {

    Text(
        text = title,
        color = Color.White,
        fontSize = 17.sp,
        fontWeight = FontWeight.Bold
    )
}

// --------------------------------------------------
// STREAMING SERVICE CARD
// --------------------------------------------------

@Composable
fun ServiceCard(
    name: String,
    label: String,
    color: Color,
    installed: Boolean,
    onClick: () -> Unit
) {

    Box(
        modifier = Modifier
            .width(112.dp)
            .height(64.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(
                Brush.verticalGradient(
                    listOf(
                        color,
                        Color(0xFF11131B)
                    )
                )
            )
            .border(
                1.dp,
                color.copy(alpha = 0.8f),
                RoundedCornerShape(10.dp)
            )
            .clickable {
                onClick()
            },

        contentAlignment = Alignment.Center
    ) {

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            Text(
                text = label,
                color = Color.White,
                fontSize = if (label.length > 7) {
                    12.sp
                } else {
                    17.sp
                },
                fontWeight = FontWeight.Black
            )

            Spacer(Modifier.height(3.dp))

            Text(
                text = if (installed) {
                    "INSTALLED"
                } else {
                    "EXPLORE"
                },
                color = if (installed) {
                    Color(0xFF79DFA6)
                } else {
                    Color.LightGray
                },
                fontSize = 7.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// --------------------------------------------------
// MOVIE CARD
// --------------------------------------------------

@Composable
fun MovieCard(movie: Movie) {

    Box(
        modifier = Modifier
            .width(132.dp)
            .height(164.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(
                Brush.verticalGradient(movie.colors)
            )
            .border(
                1.dp,
                Color(0xFF46464D),
                RoundedCornerShape(10.dp)
            )
            .clickable { },

        contentAlignment = Alignment.BottomStart
    ) {

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Transparent,
                            Color(0xDD000000)
                        )
                    )
                )
                .padding(10.dp)
        ) {

            Text(
                text = movie.title,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Black
            )

            Spacer(Modifier.height(4.dp))

            Text(
                text = movie.subtitle,
                color = Color(0xFFE7B98B),
                fontSize = 7.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
