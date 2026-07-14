package com.example

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.screens.BoxesScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.ProgressScreen
import com.example.ui.screens.StudyScreen
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.AccentTeal
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.NavyDarkBg
import com.example.ui.theme.NavySurface
import com.example.ui.theme.NavySurfaceVariant
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.LeitnerViewModel
import kotlinx.coroutines.delay
import android.app.Activity
import android.content.ContextWrapper
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import android.os.SystemClock
import android.util.Log
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.ui.zIndex
import androidx.compose.material3.Surface

// Final Javaneyar ad timing: first ad after 1 minute, then every 7 minutes
const val FIRST_AD_DELAY_MILLIS = 60_000L
const val REPEAT_AD_INTERVAL_MILLIS = 420_000L
const val AD_DURATION_MILLIS = 15_000L

// Compatibility constants for existing tests
const val AD_INTERVAL_SECONDS = 420
const val AD_INTERVAL_MILLIS = 420000L
const val AD_DURATION_SECONDS = 15

fun Context.findActivity(): Activity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    return null
}

fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
    val (height: Int, width: Int) = options.outHeight to options.outWidth
    var inSampleSize = 1

    if (height > reqHeight || width > reqWidth) {
        val halfHeight = height / 2
        val halfWidth = width / 2

        while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
            inSampleSize *= 2
        }
    }
    return inSampleSize
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Instantiate the ViewModel
        val viewModel = ViewModelProvider(this)[LeitnerViewModel::class.java]

        setContent {
            MyApplicationTheme {
                MainAppContainer(viewModel)
            }
        }
    }
}

enum class AppTab {
    Home, Study, Boxes, Progress
}

@Composable
fun MainAppContainer(viewModel: LeitnerViewModel) {
    var currentTab by remember { mutableStateOf(AppTab.Study) } // "مطالعه" is active by default as requested

    val allCollocations by viewModel.allCollocations.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val todayNewCards by viewModel.assignedNewCardsToday.collectAsStateWithLifecycle()
    val dueCards by viewModel.dueCardsToday.collectAsStateWithLifecycle()
    val sessionIndex by viewModel.sessionIndex.collectAsStateWithLifecycle()
    val showAnswer by viewModel.showAnswer.collectAsStateWithLifecycle()
    val boxCounts by viewModel.boxCounts.collectAsStateWithLifecycle()
    val isRatingInProgress by viewModel.isRatingInProgress.collectAsStateWithLifecycle()

    // Javaneyar Ad Timer Logic
    // First ad: after 1 minute of active use. Later ads: every 7 minutes.
    // A single scheduler is used so recomposition cannot create overlapping timers.
    val context = LocalContext.current
    var isAdVisible by rememberSaveable { mutableStateOf(false) }
    var hasShownFirstAd by rememberSaveable { mutableStateOf(false) }
    var accumulatedActiveMillis by rememberSaveable { mutableStateOf(0L) }
    var remainingSeconds by rememberSaveable { mutableStateOf(AD_DURATION_SECONDS) }

    val lifecycleOwner = LocalLifecycleOwner.current
    var isAppActive by remember {
        mutableStateOf(
            lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
        )
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            isAppActive = lifecycleOwner.lifecycle.currentState
                .isAtLeast(Lifecycle.State.RESUMED)

            when (event) {
                Lifecycle.Event.ON_RESUME -> Log.d("JAVANEH_AD", "App resumed")
                Lifecycle.Event.ON_PAUSE -> Log.d("JAVANEH_AD", "App paused")
                else -> Unit
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Only one active-use scheduler can exist for the root app.
    LaunchedEffect(isAppActive, isAdVisible, hasShownFirstAd) {
        if (!isAppActive || isAdVisible) return@LaunchedEffect

        val requiredDelayMillis = if (hasShownFirstAd) {
            REPEAT_AD_INTERVAL_MILLIS
        } else {
            FIRST_AD_DELAY_MILLIS
        }

        Log.d(
            "JAVANEH_AD",
            "Timer started; requiredDelayMillis=$requiredDelayMillis, " +
                "alreadyElapsed=$accumulatedActiveMillis"
        )

        var lastTickMillis = SystemClock.elapsedRealtime()

        while (isAppActive && !isAdVisible) {
            delay(250L)

            val nowMillis = SystemClock.elapsedRealtime()
            val tickMillis = (nowMillis - lastTickMillis).coerceAtLeast(0L)
            lastTickMillis = nowMillis
            accumulatedActiveMillis += tickMillis

            if (accumulatedActiveMillis >= requiredDelayMillis) {
                // Reset before showing the overlay. The next interval starts only
                // after the current ad has completed its full 15 seconds.
                accumulatedActiveMillis = 0L
                remainingSeconds = AD_DURATION_SECONDS
                isAdVisible = true
                Log.d("JAVANEH_AD", "Ad interval reached; showing overlay")
                break
            }
        }
    }

    // One countdown per visible ad. It cannot schedule another ad itself.
    LaunchedEffect(isAdVisible) {
        if (!isAdVisible) return@LaunchedEffect

        val adEndTimeMillis = SystemClock.elapsedRealtime() + AD_DURATION_MILLIS
        Log.d("JAVANEH_AD", "Countdown started")

        while (true) {
            val remainingMillis = adEndTimeMillis - SystemClock.elapsedRealtime()
            remainingSeconds = ((remainingMillis + 999L) / 1_000L)
                .coerceIn(0L, AD_DURATION_SECONDS.toLong())
                .toInt()

            if (remainingMillis <= 0L) break
            delay(100L)
        }

        // Set all next-cycle state before hiding the overlay. This prevents
        // an immediate second ad caused by a stale elapsed value or a race.
        hasShownFirstAd = true
        accumulatedActiveMillis = 0L
        remainingSeconds = AD_DURATION_SECONDS
        Log.d("JAVANEH_AD", "Ad finished; next interval is 7 minutes")
        isAdVisible = false
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                bottomBar = {
                    NavigationBar(
                        containerColor = NavySurface,
                        tonalElevation = 8.dp,
                        windowInsets = WindowInsets(0, 0, 0, 0),
                        modifier = Modifier
                            .navigationBarsPadding()
                            .testTag("bottom_nav_bar")
                    ) {
                        // Home (خانه)
                        NavigationBarItem(
                            selected = currentTab == AppTab.Home,
                            onClick = { currentTab = AppTab.Home },
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.Home,
                                    contentDescription = "خانه"
                                )
                            },
                            label = { Text("خانه", fontSize = 11.sp) },
                            colors = NavigationBarItemColors(),
                            modifier = Modifier.testTag("nav_home")
                        )

                        // Study (مطالعه)
                        NavigationBarItem(
                            selected = currentTab == AppTab.Study,
                            onClick = { currentTab = AppTab.Study },
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.Book,
                                    contentDescription = "مطالعه"
                                )
                            },
                            label = { Text("مطالعه", fontSize = 11.sp) },
                            colors = NavigationBarItemColors(),
                            modifier = Modifier.testTag("nav_study")
                        )

                        // Boxes (جعبه‌ها)
                        NavigationBarItem(
                            selected = currentTab == AppTab.Boxes,
                            onClick = { currentTab = AppTab.Boxes },
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.Inbox,
                                    contentDescription = "جعبه‌ها"
                                )
                            },
                            label = { Text("جعبه‌ها", fontSize = 11.sp) },
                            colors = NavigationBarItemColors(),
                            modifier = Modifier.testTag("nav_boxes")
                        )

                        // Progress (پیشرفت)
                        NavigationBarItem(
                            selected = currentTab == AppTab.Progress,
                            onClick = { currentTab = AppTab.Progress },
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.BarChart,
                                    contentDescription = "پیشرفت"
                                )
                            },
                            label = { Text("پیشرفت", fontSize = 11.sp) },
                            colors = NavigationBarItemColors(),
                            modifier = Modifier.testTag("nav_progress")
                        )
                    }
                },
                containerColor = NavyDarkBg
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = innerPadding.calculateBottomPadding())
                        .statusBarsPadding()
                ) {
                    when (currentTab) {
                        AppTab.Home -> {
                            HomeScreen(
                                collocations = allCollocations,
                                onStartStudyClick = { currentTab = AppTab.Study }
                            )
                        }
                        AppTab.Study -> {
                            StudyScreen(
                                categories = viewModel.categories,
                                selectedCategory = selectedCategory,
                                onCategorySelected = { viewModel.setCategory(it) },
                                activeDeck = emptyList(), // managed in ViewModel
                                todayNewCards = todayNewCards,
                                dueCards = dueCards,
                                sessionIndex = sessionIndex,
                                showAnswer = showAnswer,
                                onToggleAnswer = { viewModel.toggleShowAnswer() },
                                onRateCard = { card, rating -> viewModel.rateCard(card, rating) },
                                boxCounts = boxCounts,
                                onResetProgress = { viewModel.resetProgress() },
                                onSimulateNewDay = { viewModel.simulateNewDay() }
                            )
                        }
                        AppTab.Boxes -> {
                            BoxesScreen(
                                categories = viewModel.categories,
                                selectedCategory = selectedCategory,
                                onCategorySelected = { viewModel.setCategory(it) },
                                collocations = allCollocations
                            )
                        }
                        AppTab.Progress -> {
                            ProgressScreen(
                                categories = viewModel.categories,
                                collocations = allCollocations,
                                onResetProgress = { viewModel.resetProgress() }
                            )
                        }
                    }
                }
            }

            if (isAdVisible) {
                JavanehAdOverlay(
                    remainingSeconds = remainingSeconds,
                    onImageClick = {
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://javaneyar.ir/")).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "مرورگری برای بازکردن سایت جوانه پیدا نشد.", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun NavigationBarItemColors() = NavigationBarItemDefaults.colors(
    selectedIconColor = AccentGreen,
    selectedTextColor = AccentGreen,
    indicatorColor = NavySurfaceVariant,
    unselectedIconColor = TextSecondary,
    unselectedTextColor = TextSecondary
)

@Composable
fun JavanehAdOverlay(
    remainingSeconds: Int,
    onImageClick: () -> Unit
) {
    BackHandler(enabled = true) {
        // تبلیغ قبل از پایان زمان بسته نشود
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.96f))
            .zIndex(1000f)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        // لمس فضای بیرونی هیچ کاری نکند
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(
                id = R.drawable.javaneh_ad_poster
            ),
            contentDescription = "تبلیغ پلتفرم جوانه",
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .clickable(
                    indication = null,
                    interactionSource = remember {
                        MutableInteractionSource()
                    }
                ) {
                    onImageClick()
                },
            contentScale = ContentScale.Fit
        )

        Surface(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 10.dp),
            shape = RoundedCornerShape(50),
            color = Color.Black.copy(alpha = 0.75f)
        ) {
            Text(
                text = "تبلیغ تا $remainingSeconds ثانیه دیگر بسته میشود",
                modifier = Modifier.padding(
                    horizontal = 16.dp,
                    vertical = 8.dp
                ),
                color = Color.White
            )
        }
    }
}

