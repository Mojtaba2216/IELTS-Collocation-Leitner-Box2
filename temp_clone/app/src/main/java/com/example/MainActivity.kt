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

// Central Constants for Javaneyar Ad Timing based on Build Mode (Debug/Release)
val FIRST_AD_DELAY_MILLIS = if (BuildConfig.DEBUG) 20_000L else 60_000L
val REPEAT_AD_INTERVAL_MILLIS = if (BuildConfig.DEBUG) 30_000L else 420_000L
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
    val context = LocalContext.current
    var isAdVisible by rememberSaveable { mutableStateOf(false) }
    var isFirstAdShown by rememberSaveable { mutableStateOf(false) }
    var elapsedActiveMillis by rememberSaveable { mutableStateOf(0L) }
    
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var isAppActive by remember { mutableStateOf(lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) }

    // Track App Foreground/Background state and log
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            isAppActive = lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    Log.d("JAVANEH_AD", "App resumed")
                }
                Lifecycle.Event.ON_PAUSE -> {
                    Log.d("JAVANEH_AD", "App paused")
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(Unit) {
        if (BuildConfig.DEBUG) {
            Log.d("JAVANEH_AD", "Scheduler created")
        }
    }

    // Single Active Usage Timer Scheduler
    LaunchedEffect(isAppActive, isAdVisible, isFirstAdShown) {
        if (isAppActive && !isAdVisible) {
            if (BuildConfig.DEBUG) {
                Log.d("JAVANEH_AD", "Timer started")
            }
            
            val targetDelayMillis = if (!isFirstAdShown) {
                val delayVal = FIRST_AD_DELAY_MILLIS
                if (BuildConfig.DEBUG) {
                    Log.d("JAVANEH_AD", "First delay selected: $delayVal")
                }
                delayVal
            } else {
                val delayVal = REPEAT_AD_INTERVAL_MILLIS
                delayVal
            }

            while (elapsedActiveMillis < targetDelayMillis) {
                delay(100L)
                if (isAppActive && !isAdVisible) {
                    elapsedActiveMillis += 100L
                }
            }

            if (BuildConfig.DEBUG) {
                Log.d("JAVANEH_AD", "Timer reached")
            }

            // Verify safe display conditions
            val activity = context.findActivity()
            val isActivityValid = activity != null && !activity.isFinishing && !activity.isDestroyed
            val isForeground = isAppActive
            
            if (isActivityValid && isForeground && !isAdVisible) {
                if (BuildConfig.DEBUG) {
                    Toast.makeText(context, "TEST: AD TIMER REACHED", Toast.LENGTH_SHORT).show()
                    Log.d("JAVANEH_AD", "Changing isAdVisible to true")
                }
                isAdVisible = true
            }
        }
    }

    val startNextInterval = {
        isFirstAdShown = true
        elapsedActiveMillis = 0L
        if (BuildConfig.DEBUG) {
            val nextDelay = REPEAT_AD_INTERVAL_MILLIS
            Log.d("JAVANEH_AD", "Next delay selected: $nextDelay")
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
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
        
        // Show the Javaneyar Ad Dialog if triggered
        if (isAdVisible) {
            JavanehAdDialog(
                onFinished = {
                    isAdVisible = false
                    startNextInterval()
                }
            )
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
fun JavanehAdDialog(
    onFinished: () -> Unit
) {
    val context = LocalContext.current
    var countdownSeconds by remember { mutableStateOf(15) }

    // Memory-Optimized Ad Image Loader (decodes only once per dialog show, prevents OOM)
    val adImageBitmap = remember {
        try {
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeResource(context.resources, R.drawable.javaneh_ad_poster, options)
            
            if (options.outWidth <= 0 || options.outHeight <= 0) {
                Log.e("JAVANEH_AD", "Error showing ad: image asset not found or invalid.")
                null
            } else {
                options.inSampleSize = calculateInSampleSize(options, 600, 900)
                options.inJustDecodeBounds = false
                val bitmap = BitmapFactory.decodeResource(context.resources, R.drawable.javaneh_ad_poster, options)
                bitmap?.asImageBitmap()
            }
        } catch (e: Throwable) {
            Log.e("JAVANEH_AD", "Error showing ad: ${e.message}", e)
            null
        }
    }

    LaunchedEffect(Unit) {
        if (BuildConfig.DEBUG) {
            Log.d("JAVANEH_AD", "Ad dialog composed")
            Log.d("JAVANEH_AD", "Countdown started")
        }
        val startTime = SystemClock.elapsedRealtime()
        val adEndTime = startTime + 15_000L
        
        while (countdownSeconds > 0) {
            val now = SystemClock.elapsedRealtime()
            val remainingMillis = adEndTime - now
            val remaining = ((remainingMillis + 999) / 1000).coerceIn(0, 15).toInt()
            countdownSeconds = remaining
            if (remaining <= 0) {
                break
            }
            delay(100L)
        }
        if (BuildConfig.DEBUG) {
            Log.d("JAVANEH_AD", "Countdown finished")
            Log.d("JAVANEH_AD", "Ad closed")
        }
        onFinished()
    }

    Dialog(
        onDismissRequest = {}, // Empty lambda prevents dismissal on outside tap or back press
        properties = DialogProperties(
            usePlatformDefaultWidth = false, // Allow full screen layout
            dismissOnBackPress = false, // Disable dismiss via Android physical/gesture back button
            dismissOnClickOutside = false // Disable dismiss via tapping on background
        )
    ) {
        BackHandler(enabled = true) {
            // Completely intercept back button and gesture to lock the ad for 15 seconds
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.95f))
                .safeDrawingPadding()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 480.dp), // Max width to ensure nice proportion on tablets and foldables
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Info Bar (Semi-transparent small bar)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(
                            progress = countdownSeconds / 15f,
                            modifier = Modifier.size(18.dp).testTag("ad_progress_indicator"),
                            color = AccentGreen,
                            trackColor = Color.White.copy(alpha = 0.2f),
                            strokeWidth = 2.dp
                        )
                        Text(
                            text = "این تبلیغ تا $countdownSeconds ثانیه دیگر بسته میشود",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.semantics {
                                contentDescription = "این تبلیغ تا $countdownSeconds ثانیه دیگر بسته میشود"
                            }
                        )
                    }
                }

                // Javaneyar Ad Poster Image
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .clickable {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://javaneyar.ir/")).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "مرورگری برای بازکردن سایت جوانه پیدا نشد.", Toast.LENGTH_SHORT).show()
                            }
                        }
                        .testTag("ad_image_card"),
                    contentAlignment = Alignment.Center
                ) {
                    if (adImageBitmap != null) {
                        Image(
                            bitmap = adImageBitmap,
                            contentDescription = "تبلیغ پلتفرم جوانه",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    } else {
                        Image(
                            painter = painterResource(R.drawable.javaneh_ad_poster),
                            contentDescription = "تبلیغ پلتفرم جوانه",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    }
                }

                // Bottom Brand Text
                Text(
                    text = "javaneyar.ir",
                    color = Color.White.copy(alpha = 0.4f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Light,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
        }
    }
}

