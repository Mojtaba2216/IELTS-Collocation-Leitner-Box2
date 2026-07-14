package com.example.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Collocation
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.platform.testTag
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.AccentOrange
import com.example.ui.theme.AccentTeal
import com.example.ui.theme.NavyDarkBg
import com.example.ui.theme.NavySurface
import com.example.ui.theme.NavySurfaceVariant
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun HomeScreen(
    collocations: List<Collocation>,
    onStartStudyClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Force RTL for Persian Interface
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        
        val totalCards = collocations.size
        val studiedCards = collocations.filter { it.boxIndex > 0 }
        val masteredCards = collocations.filter { it.boxIndex == 5 && it.nextReviewTime == Long.MAX_VALUE }
        val learningCards = collocations.filter { it.boxIndex in 1..4 }
        val unstudiedCards = collocations.filter { it.boxIndex == 0 }

        val progressPercent = if (totalCards > 0) (masteredCards.size.toFloat() / totalCards.toFloat()) else 0f
        var progressAnimated by remember { mutableStateOf(0f) }
        
        LaunchedEffect(progressPercent) {
            progressAnimated = progressPercent
        }
        
        val animatedProgress by animateFloatAsState(
            targetValue = progressAnimated,
            animationSpec = tween(durationMillis = 1000),
            label = "ProgressAnimation"
        )

        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .background(NavyDarkBg)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Spacer for Status Bar safe area
            item { Spacer(modifier = Modifier.height(30.dp)) }

            // Header Section
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "خوش آمدید",
                            color = TextSecondary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "IELTS Collocation Leitner",
                            color = TextPrimary,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Streak Display
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(NavySurfaceVariant)
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalFireDepartment,
                            contentDescription = "روزهای مطالعه پیوسته",
                            tint = AccentOrange,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "۲ روز پیاپی",
                            color = TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Welcome Gradient card
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(AccentTeal, AccentGreen)
                            )
                        )
                        .padding(24.dp)
                ) {
                    Column {
                        Text(
                            text = "تسلط بر کالوکیشن‌های آیلتس",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "با استفاده از سیستم علمی جعبه لایتنر ۵ مرحله‌ای، واژگان پرتکرار را به حافظه بلندمدت خود بسپارید.",
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 13.sp,
                            lineHeight = 22.sp
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White)
                                .clickable { onStartStudyClick() }
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "شروع",
                                tint = NavyDarkBg,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "شروع مطالعه امروز",
                                color = NavyDarkBg,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Progress Circular Stats Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = NavySurface),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "پیشرفت کلی شما",
                                color = TextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "شما تسلط کامل بر روی ${masteredCards.size} کالوکیشن از کل $totalCards واژه را دارید.",
                                color = TextSecondary,
                                fontSize = 12.sp,
                                lineHeight = 20.sp
                            )
                        }

                        Spacer(modifier = Modifier.width(20.dp))

                        Box(contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(
                                progress = { animatedProgress },
                                modifier = Modifier.size(80.dp),
                                color = AccentGreen,
                                strokeWidth = 8.dp,
                                trackColor = NavySurfaceVariant,
                            )
                            Text(
                                text = "${(progressAnimated * 100).toInt()}%",
                                color = TextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Javaneyar promotional banner
            item {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    JavaneyarBanner()
                }
            }

            // Cards Breakdown Grid style
            item {
                Text(
                    text = "وضعیت کل جعبه لایتنر",
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Box 1
                    StatBox(
                        title = "نیاز به یادگیری",
                        count = unstudiedCards.size,
                        color = TextSecondary,
                        icon = Icons.Default.Book,
                        modifier = Modifier.weight(1f)
                    )
                    // Box 2
                    StatBox(
                        title = "در حال مرور",
                        count = learningCards.size,
                        color = AccentTeal,
                        icon = Icons.Default.TrendingUp,
                        modifier = Modifier.weight(1f)
                    )
                    // Box 3
                    StatBox(
                        title = "تسلط کامل",
                        count = masteredCards.size,
                        color = AccentGreen,
                        icon = Icons.Default.CheckCircle,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Quick Tips List
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = NavySurface),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "قانون",
                                tint = AccentOrange,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "قانون قطعی مطالعه روزانه",
                                color = TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "هر روز دقیقاً ۱۰ کارت جدید از هر موضوع دریافت می‌کنید. تکرار و مرور منظم کارت‌ها در زمان‌های معین کلید تضمین موفقیت شما در رایتینگ و اسپیکینگ آیلتس است.",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            lineHeight = 22.sp,
                            textAlign = TextAlign.Justify
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(40.dp)) }
        }
    }
}

@Composable
fun StatBox(
    title: String,
    count: Int,
    color: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = NavySurface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.15f))
                    .padding(8.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = color,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = count.toString(),
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                color = TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun JavaneyarBanner(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val openUrl = {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://javaneyar.ir/"))
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "مرورگری برای باز کردن لینک پیدا نشد", Toast.LENGTH_SHORT).show()
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .widthIn(max = 600.dp)
            .clickable { openUrl() }
            .testTag("javaneyar_promo_banner"),
        colors = CardDefaults.cardColors(containerColor = NavySurface),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.5.dp, Brush.linearGradient(listOf(AccentTeal.copy(alpha = 0.5f), AccentGreen.copy(alpha = 0.5f))))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                // Small Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(AccentGreen.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "معرفی پلتفرم جوانه",
                        color = AccentGreen,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Heading
                Text(
                    text = "ایده داری؟ ایده‌ات رو رشد بده",
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Short Description
                Text(
                    text = "ایده‌ات را ثبت کن، ارزیابی بگیر و با متخصصان و سازمان‌ها برای ساختنش همکاری کن.",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 20.sp,
                    textAlign = TextAlign.Justify
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Call To Action Button (Pill shaped)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Brush.linearGradient(listOf(AccentTeal, AccentGreen)))
                        .clickable { openUrl() }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "ورود به جوانه",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Graphical Sprout/Leaf element
            Box(
                modifier = Modifier
                    .size(70.dp)
                    .align(Alignment.CenterVertically),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    // Custom Organic Growing Leaves
                    val width = size.width
                    val height = size.height
                    
                    // Left Leaf
                    val pathLeft = Path().apply {
                        moveTo(width * 0.5f, height * 0.85f)
                        cubicTo(
                            width * 0.15f, height * 0.55f,
                            width * 0.15f, height * 0.25f,
                            width * 0.5f, height * 0.15f
                        )
                        cubicTo(
                            width * 0.38f, height * 0.4f,
                            width * 0.42f, height * 0.65f,
                            width * 0.5f, height * 0.85f
                        )
                    }
                    drawPath(
                        path = pathLeft,
                        brush = Brush.linearGradient(
                            colors = listOf(AccentTeal, AccentGreen)
                        )
                    )

                    // Right Leaf
                    val pathRight = Path().apply {
                        moveTo(width * 0.5f, height * 0.85f)
                        cubicTo(
                            width * 0.85f, height * 0.5f,
                            width * 0.8f, height * 0.2f,
                            width * 0.55f, height * 0.05f
                        )
                        cubicTo(
                            width * 0.6f, height * 0.35f,
                            width * 0.58f, height * 0.65f,
                            width * 0.5f, height * 0.85f
                        )
                    }
                    drawPath(
                        path = pathRight,
                        brush = Brush.linearGradient(
                            colors = listOf(AccentGreen, Color(0xFFC0CA33))
                        )
                    )

                    // Growing Stem
                    drawLine(
                        brush = Brush.linearGradient(listOf(AccentTeal, AccentGreen)),
                        start = Offset(width * 0.5f, height * 0.95f),
                        end = Offset(width * 0.5f, height * 0.15f),
                        strokeWidth = 3.dp.toPx()
                    )
                    
                    // Sparkling node
                    drawCircle(
                        color = Color.White,
                        radius = 4.dp.toPx(),
                        center = Offset(width * 0.55f, height * 0.05f)
                    )
                }
            }
        }
    }
}
