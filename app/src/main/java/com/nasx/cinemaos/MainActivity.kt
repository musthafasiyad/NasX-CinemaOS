
package com.nasx.cinemaos

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nasx.cinemaos.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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
    val colors: List<Color>,
    val posterResId: Int? = null
)

private val hiddenPackages = setOf(
    "com.android.settings",
    "com.android.tv.settings",
    "com.android.tv.remote.service",
    "com.google.android.tv.remote.service",
    "com.nasx.cinemaos"
)

private val streamingServices = listOf(
    StreamingService("Netflix", "N", listOf("com.netflix.ninja"), Color(0xFFB20710)),
    StreamingService("Prime Video", "prime video", listOf("com.amazon.amazonvideo.livingroom"), Color(0xFF0879B9)),
    StreamingService("Disney+", "Disney+", listOf("com.disney.disneyplus"), Color(0xFF182A70)),
    StreamingService("Apple TV", "Apple TV", listOf("com.apple.atve.androidtv.appletv"), Color(0xFF33343A)),
    StreamingService("YouTube", "▶ YouTube", listOf("com.google.android.youtube.tv"), Color(0xFFB51220)),
    StreamingService("JioHotstar", "JioHotstar", listOf("in.startv.hotstar"), Color(0xFF5420A8)),
    StreamingService("Sony LIV", "SONY liv", listOf("com.sonyliv"), Color(0xFF182B59)),
    StreamingService("ZEE5", "ZEE5", listOf("com.graymatrix.did"), Color(0xFF39205C)),
    StreamingService("Spotify", "Spotify", listOf("com.spotify.tv.android"), Color(0xFF125A35)),
    StreamingService("VLC", "VLC", listOf("org.videolan.vlc"), Color(0xFF8D4A12))
)

private val movies = listOf(
    Movie("THE BATMAN", "DARK KNIGHT", listOf(Color(0xFF580D16), Color(0xFF090B12)), R.drawable.poster_batman),
    Movie("OPPENHEIMER", "A STORY OF FIRE", listOf(Color(0xFFB44A16), Color(0xFF17100D)), R.drawable.poster_oppenheimer),
    Movie("INTERSTELLAR", "BEYOND THE STARS", listOf(Color(0xFF263B50), Color(0xFF080D18)), R.drawable.poster_interstellar),
    Movie("JOKER", "PUT ON A HAPPY FACE", listOf(Color(0xFF70202A), Color(0xFF140D14)), R.drawable.poster_joker),
    Movie("INCEPTION", "DREAM WITHIN A DREAM", listOf(Color(0xFF153D51), Color(0xFF080D18))),
    Movie("TENET", "TIME IS THE ENEMY", listOf(Color(0xFF53616C), Color(0xFF10151C))),
    Movie("DUNE PART TWO", "THE DESERT AWAKENS", listOf(Color(0xFFB65C28), Color(0xFF27130C))),
    Movie("AVATAR", "THE WAY OF WATER", listOf(Color(0xFF12617A), Color(0xFF071923)))
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
                packageManager.getLeanbackLaunchIntentForPackage(app.packageName)
                    ?: packageManager.getLaunchIntentForPackage(app.packageName)

            if (launchIntent != null) {
                startActivity(launchIntent)
            } else {
                Toast.makeText(this, "${app.name} cannot be opened", Toast.LENGTH_SHORT).show()
            }
        } catch (_: Exception) {
            Toast.makeText(this, "Unable to open ${app.name}", Toast.LENGTH_SHORT).show()
        }
    }
}

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

        val activities = packageManager.queryIntentActivities(intent, 0)

        activities.forEach { activity ->
            val packageName = activity.activityInfo.packageName

            try {
                val appName = activity.loadLabel(packageManager).toString()

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

    return result.values.sortedBy { it.name.lowercase() }
}

@Composable
fun NaSXHome(
    onLaunchApp: (InstalledApp) -> Unit
) {
    val context = LocalContext.current

    var apps by remember { mutableStateOf<List<InstalledApp>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var selectedTab by remember { mutableStateOf("Home") }
    var savedToList by remember { mutableStateOf(false) }

    val watchlist = remember { mutableStateListOf<Movie>() }

    val tabs = listOf(
        "Home", "Movies", "Series", "Apps", "Library", "Settings"
    )

    BackHandler(enabled = selectedTab != "Home") {
        selectedTab = "Home"
    }

    LaunchedEffect(Unit) {
        loading = true
        apps = withContext(Dispatchers.IO) {
            discoverInstalledApps(context.packageManager)
        }
        loading = false
    }

    val installedServices = streamingServices.mapNotNull { service ->
        val installed = apps.firstOrNull {
            it.packageName in service.packageNames
        }
        if (installed != null) service to installed else null
    }

    val promotionalServices = streamingServices.filter { service ->
        apps.none { it.packageName in service.packageNames }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF08090D)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 30.dp, vertical = 12.dp)
        ) {

            // HEADER AND NAVIGATION

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
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
                    "│ CINEMA OS 2.1",
                    color = Color.Gray,
                    fontSize = 10.sp
                )

                Spacer(Modifier.weight(1f))

                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    tabs.forEach { tab ->
                        val selected = selectedTab == tab

                        FocusTab(
                            title = tab,
                            selected = selected,
                            onClick = { selectedTab = tab }
                        )
                    }
                }
            }

            Spacer(Modifier.height(15.dp))

            // SCREEN CONTENT

            when (selectedTab) {

                "Home" -> {
                    HomeScreen(
                        apps = apps,
                        loading = loading,
                        installedServices = installedServices,
                        promotionalServices = promotionalServices,
                        savedToList = savedToList,
                        onSaveList = { saved ->
                            savedToList = saved
                            if (saved) {
                                if (movies[3] !in watchlist) watchlist.add(movies[3])
                            } else {
                                watchlist.remove(movies[3])
                            }
                        },
                        onLaunchApp = onLaunchApp
                    )
                }

                "Movies" -> {
                    MoviesScreen(
                        onSaveMovie = { movie ->
                            if (movie !in watchlist) {
                                watchlist.add(movie)
                                Toast.makeText(
                                    context,
                                    "${movie.title} added to My List",
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                watchlist.remove(movie)
                                Toast.makeText(
                                    context,
                                    "${movie.title} removed from My List",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        },
                        watchlist = watchlist
                    )
                }

                "Series" -> {
                    SeriesScreen()
                }

                "Apps" -> {
                    AppsScreen(
                        apps = apps,
                        loading = loading,
                        onLaunchApp = onLaunchApp
                    )
                }

                "Library" -> {
                    LibraryScreen(
                        watchlist = watchlist,
                        onRemove = { watchlist.remove(it) }
                    )
                }

                "Settings" -> {
                    SettingsScreen()
                }
            }

            Spacer(Modifier.height(22.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "NaSX CINEMA OS 2.1",
                    color = Color(0xFF777B86),
                    fontSize = 9.sp
                )

                Text(
                    if (loading) "DISCOVERING APPS..."
                    else "${apps.size} APPLICATIONS DISCOVERED",
                    color = Color(0xFF777B86),
                    fontSize = 9.sp
                )
            }
        }
    }
}

@Composable
fun FocusTab(
    title: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .padding(horizontal = 3.dp)
            .onFocusChanged { focused = it.isFocused }
            .clip(RoundedCornerShape(22.dp))
            .background(
                if (selected) Color(0xFF47151D)
                else Color.Transparent
            )
            .border(
                if (focused) 2.dp else if (selected) 1.dp else 0.dp,
                if (focused) Color(0xFFFFD166)
                else if (selected) Color(0xFFE52A40)
                else Color.Transparent,
                RoundedCornerShape(22.dp)
            )
            .clickable { onClick() }
            .focusable()
            .padding(horizontal = 12.dp, vertical = 9.dp)
    ) {
        Text(
            title,
            color = if (selected || focused) Color.White else Color.LightGray,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

// HOME SCREEN

@Composable
fun HomeScreen(
    apps: List<InstalledApp>,
    loading: Boolean,
    installedServices: List<Pair<StreamingService, InstalledApp>>,
    promotionalServices: List<StreamingService>,
    savedToList: Boolean,
    onSaveList: (Boolean) -> Unit,
    onLaunchApp: (InstalledApp) -> Unit
) {
    val context = LocalContext.current

    HeroBanner(
        savedToList = savedToList,
        onSaveList = onSaveList
    )

    Spacer(Modifier.height(20.dp))

    SectionTitle("Streaming Apps")

    Spacer(Modifier.height(10.dp))

    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        installedServices.forEach { pair ->
            ServiceCard(
                name = pair.first.name,
                label = pair.first.shortName,
                color = pair.first.color,
                installed = true,
                onClick = { onLaunchApp(pair.second) }
            )
        }

        promotionalServices.forEach { service ->
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

        ServiceCard(
            name = "All Apps",
            label = "▦",
            color = Color(0xFF292B33),
            installed = true,
            onClick = {
                Toast.makeText(context, "Open Apps tab to view all apps", Toast.LENGTH_SHORT).show()
            }
        )
    }

    Spacer(Modifier.height(20.dp))

    SectionTitle("Trending Movies")

    Spacer(Modifier.height(10.dp))

    MovieRow(movies.take(4))

    Spacer(Modifier.height(18.dp))

    SectionTitle("Discover More")

    Spacer(Modifier.height(8.dp))

    Text(
        "Explore movies, series and entertainment on NaSX Cinema OS.",
        color = Color.LightGray,
        fontSize = 13.sp
    )
}

// HERO BANNER

@Composable
fun HeroBanner(
    savedToList: Boolean,
    onSaveList: (Boolean) -> Unit
) {
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(275.dp)
            .clip(RoundedCornerShape(12.dp))
    ) {
        Image(
            painter = painterResource(R.drawable.nasx_dune_hero),
            contentDescription = "NaSX Dune cinematic hero artwork",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        0.0f to Color(0xFF08090D),
                        0.38f to Color(0xCC08090D),
                        0.68f to Color(0x3308090D),
                        1.0f to Color.Transparent
                    )
                )
        )

        Column(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 30.dp, end = 20.dp)
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
                "Paul Atreides unites with the Fremen to seek revenge against those who destroyed his family.",
                color = Color.LightGray,
                fontSize = 12.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(17.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FocusAction(
                    text = "▶ Watch Now",
                    color = Color(0xFFE51B35),
                    onClick = {
                        Toast.makeText(context, "Featured movie demo", Toast.LENGTH_SHORT).show()
                    }
                )

                FocusAction(
                    text = if (savedToList) "✓ My List" else "+ My List",
                    color = Color(0x66333333),
                    onClick = { onSaveList(!savedToList) }
                )
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
}

// MOVIES DASHBOARD

@Composable
fun MoviesScreen(
    onSaveMovie: (Movie) -> Unit,
    watchlist: List<Movie>
) {
    Text(
        "MOVIES",
        color = Color.White,
        fontSize = 27.sp,
        fontWeight = FontWeight.Black
    )

    Spacer(Modifier.height(5.dp))

    Text(
        "Your cinema discovery destination",
        color = Color.Gray,
        fontSize = 13.sp
    )

    Spacer(Modifier.height(20.dp))

    HeroBanner(
        savedToList = false,
        onSaveList = {}
    )

    Spacer(Modifier.height(22.dp))

    SectionTitle("Trending Now")

    Spacer(Modifier.height(10.dp))

    MovieRow(movies.take(4), onSaveMovie, watchlist)

    Spacer(Modifier.height(22.dp))

    SectionTitle("Popular Movies")

    Spacer(Modifier.height(10.dp))

    MovieRow(movies.drop(4), onSaveMovie, watchlist)

    Spacer(Modifier.height(22.dp))

    SectionTitle("Recently Added")

    Spacer(Modifier.height(10.dp))

    MovieRow(
        listOf(movies[2], movies[3], movies[0], movies[1]),
        onSaveMovie,
        watchlist
    )
}

// SERIES SCREEN

@Composable
fun SeriesScreen() {
    Text(
        "SERIES",
        color = Color.White,
        fontSize = 27.sp,
        fontWeight = FontWeight.Black
    )

    Spacer(Modifier.height(6.dp))

    Text(
        "Discover your next favourite series",
        color = Color.Gray,
        fontSize = 13.sp
    )

    Spacer(Modifier.height(25.dp))

    SectionTitle("Coming Soon")

    Spacer(Modifier.height(14.dp))

    EmptyStateCard(
        title = "SERIES COLLECTION",
        description = "Your series discovery experience is under development."
    )
}

// APPS SCREEN

@Composable
fun AppsScreen(
    apps: List<InstalledApp>,
    loading: Boolean,
    onLaunchApp: (InstalledApp) -> Unit
) {
    Text(
        "APPLICATIONS",
        color = Color.White,
        fontSize = 27.sp,
        fontWeight = FontWeight.Black
    )

    Spacer(Modifier.height(6.dp))

    Text(
        "Your installed Android TV applications",
        color = Color.Gray,
        fontSize = 13.sp
    )

    Spacer(Modifier.height(20.dp))

    if (loading) {
        Text("Discovering applications...", color = Color.White)
    } else if (apps.isEmpty()) {
        EmptyStateCard(
            "NO APPLICATIONS FOUND",
            "Install applications to see them here."
        )
    } else {
        apps.forEach { app ->
            FocusListItem(
                title = app.name,
                subtitle = app.packageName,
                onClick = { onLaunchApp(app) }
            )
            Spacer(Modifier.height(8.dp))
        }
    }
}

// LIBRARY SCREEN

@Composable
fun LibraryScreen(
    watchlist: List<Movie>,
    onRemove: (Movie) -> Unit
) {
    Text(
        "MY LIBRARY",
        color = Color.White,
        fontSize = 27.sp,
        fontWeight = FontWeight.Black
    )

    Spacer(Modifier.height(6.dp))

    Text(
        "Your saved movies and favourites",
        color = Color.Gray,
        fontSize = 13.sp
    )

    Spacer(Modifier.height(20.dp))

    SectionTitle("My Watchlist")

    Spacer(Modifier.height(12.dp))

    if (watchlist.isEmpty()) {
        EmptyStateCard(
            title = "YOUR LIST IS EMPTY",
            description = "Save movies from the Movies screen to see them here."
        )
    } else {
        MovieRow(
            watchlist,
            onSaveMovie = onRemove,
            watchlist = watchlist
        )
    }
}

// SETTINGS SCREEN

@Composable
fun SettingsScreen() {
    var showClock by remember { mutableStateOf(true) }
    var cinematicMode by remember { mutableStateOf(true) }

    Text(
        "SETTINGS",
        color = Color.White,
        fontSize = 27.sp,
        fontWeight = FontWeight.Black
    )

    Spacer(Modifier.height(6.dp))

    Text(
        "Personalise your NaSX experience",
        color = Color.Gray,
        fontSize = 13.sp
    )

    Spacer(Modifier.height(22.dp))

    SectionTitle("Display Preferences")

    Spacer(Modifier.height(12.dp))

    SettingToggle(
        title = "Show Clock",
        description = "Display the clock in the launcher",
        checked = showClock,
        onCheckedChange = { showClock = it }
    )

    Spacer(Modifier.height(10.dp))

    SettingToggle(
        title = "Cinematic Mode",
        description = "Use the cinematic launcher appearance",
        checked = cinematicMode,
        onCheckedChange = { cinematicMode = it }
    )

    Spacer(Modifier.height(22.dp))

    SectionTitle("About")

    Spacer(Modifier.height(10.dp))

    Text(
        "NaSX Cinema OS 2.1",
        color = Color.White,
        fontSize = 15.sp,
        fontWeight = FontWeight.Bold
    )

    Text(
        "Cinema Beyond Reality",
        color = Color.Gray,
        fontSize = 12.sp
    )
}

// REUSABLE COMPONENTS

@Composable
fun SectionTitle(title: String) {
    Text(
        text = title,
        color = Color.White,
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold
    )
}

@Composable
fun FocusAction(
    text: String,
    color: Color,
    onClick: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .onFocusChanged { focused = it.isFocused }
            .clip(RoundedCornerShape(9.dp))
            .background(color)
            .border(
                if (focused) 2.dp else 0.dp,
                Color(0xFFFFD166),
                RoundedCornerShape(9.dp)
            )
            .clickable { onClick() }
            .focusable()
            .padding(horizontal = 17.dp, vertical = 11.dp)
    ) {
        Text(
            text,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun ServiceCard(
    name: String,
    label: String,
    color: Color,
    installed: Boolean,
    onClick: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .width(112.dp)
            .height(64.dp)
            .onFocusChanged { focused = it.isFocused }
            .clip(RoundedCornerShape(10.dp))
            .background(
                Brush.verticalGradient(listOf(color, Color(0xFF11131B)))
            )
            .border(
                if (focused) 3.dp else 1.dp,
                if (focused) Color(0xFFFFD166) else color.copy(alpha = 0.8f),
                RoundedCornerShape(10.dp)
            )
            .clickable { onClick() }
            .focusable(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                label,
                color = Color.White,
                fontSize = if (label.length > 7) 12.sp else 17.sp,
                fontWeight = FontWeight.Black
            )

            Spacer(Modifier.height(3.dp))

            Text(
                if (installed) "INSTALLED" else "EXPLORE",
                color = if (installed) Color(0xFF79DFA6) else Color.LightGray,
                fontSize = 7.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun MovieRow(
    movieList: List<Movie>,
    onSaveMovie: (Movie) -> Unit = {},
    watchlist: List<Movie> = emptyList()
) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        movieList.forEach { movie ->
            MovieCard(
                movie = movie,
                saved = movie in watchlist,
                onClick = { onSaveMovie(movie) }
            )
        }
    }
}

@Composable
fun MovieCard(
    movie: Movie,
    saved: Boolean = false,
    onClick: () -> Unit = {}
) {
    var focused by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .width(132.dp)
            .height(164.dp)
            .onFocusChanged { focused = it.isFocused }
            .clip(RoundedCornerShape(10.dp))
            .background(Brush.verticalGradient(movie.colors))
            .border(
                if (focused) 3.dp else 1.dp,
                if (focused) Color(0xFFFFD166) else Color(0xFF46464D),
                RoundedCornerShape(10.dp)
            )
            .clickable { onClick() }
            .focusable(),
        contentAlignment = Alignment.BottomStart
    ) {
        if (movie.posterResId != null) {
            Image(
                painter = painterResource(movie.posterResId),
                contentDescription = "${movie.title} movie poster",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.0f to Color.Transparent,
                        0.55f to Color.Transparent,
                        1.0f to Color(0xE6000000)
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
        ) {
            if (saved) {
                Text(
                    "✓ SAVED",
                    color = Color(0xFF79DFA6),
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Text(
                movie.title,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(3.dp))

            Text(
                movie.subtitle,
                color = Color(0xFFE7B98B),
                fontSize = 7.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun FocusListItem(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged { focused = it.isFocused }
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF151821))
            .border(
                if (focused) 2.dp else 1.dp,
                if (focused) Color(0xFFFFD166) else Color(0xFF30343F),
                RoundedCornerShape(10.dp)
            )
            .clickable { onClick() }
            .focusable()
            .padding(15.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                subtitle,
                color = Color.Gray,
                fontSize = 10.sp
            )
        }

        Text(
            "OPEN  ›",
            color = Color(0xFFE7B98B),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun EmptyStateCard(
    title: String,
    description: String
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF151821))
            .border(1.dp, Color(0xFF30343F), RoundedCornerShape(12.dp))
            .padding(28.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                title,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(8.dp))

            Text(
                description,
                color = Color.Gray,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
fun SettingToggle(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    var focused by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged { focused = it.isFocused }
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF151821))
            .border(
                if (focused) 2.dp else 1.dp,
                if (focused) Color(0xFFFFD166) else Color(0xFF30343F),
                RoundedCornerShape(10.dp)
            )
            .clickable { onCheckedChange(!checked) }
            .focusable()
            .padding(15.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                description,
                color = Color.Gray,
                fontSize = 10.sp
            )
        }

        Text(
            if (checked) "ON" else "OFF",
            color = if (checked) Color(0xFF79DFA6) else Color.Gray,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
