package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIos
import androidx.compose.material.icons.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Collocation
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.AccentOrange
import com.example.ui.theme.AccentRed
import com.example.ui.theme.AccentTeal
import com.example.ui.theme.NavyDarkBg
import com.example.ui.theme.NavySurface
import com.example.ui.theme.NavySurfaceVariant
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun StudyScreen(
    categories: List<String>,
    selectedCategory: String,
    onCategorySelected: (String) -> Unit,
    activeDeck: List<List<Collocation>>, // We can pass a tuple/deck or list
    todayNewCards: List<Collocation>,
    dueCards: List<Collocation>,
    sessionIndex: Int,
    showAnswer: Boolean,
    onToggleAnswer: () -> Unit,
    onRateCard: (Collocation, String) -> Unit,
    boxCounts: Map<Int, Int>,
    onResetProgress: () -> Unit,
    onSimulateNewDay: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Force RTL for outer wrapper, but inside the card we will override for English parts
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        
        // Compute statistics for the selected category
        val totalNewToday = todayNewCards.size // This is generally 10
        val reviewedNewToday = todayNewCards.count { it.isReviewedToday }
        
        // Compile the active session list (unreviewed new + due cards)
        val activeSessionCards = (todayNewCards.filter { !it.isReviewedToday } + dueCards.filter { !it.isReviewedToday })
        val activeCard: Collocation? = if (activeSessionCards.isNotEmpty() && sessionIndex < activeSessionCards.size) {
            activeSessionCards[sessionIndex]
        } else {
            null
        }

        // Card session progress e.g. "کارت ۳ از ۱۴" - STABLE PROGRESS CALCULATION
        val totalSessionCount = todayNewCards.size + dueCards.size
        val reviewedCount = todayNewCards.count { it.isReviewedToday } + dueCards.count { it.isReviewedToday }
        val currentCardIndexInSession = if (activeCard != null) reviewedCount + 1 else totalSessionCount

        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .background(NavyDarkBg)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Spacer for Status Bar safe area
            item { Spacer(modifier = Modifier.height(30.dp)) }

            // Title Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "مرور فلش‌کارت",
                            color = TextPrimary,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "روز دوم مطالعه — موضوع: $selectedCategory",
                            color = AccentTeal,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // Reset stats button for convenience / testing
                    Row {
                        TextButton(
                            onClick = onSimulateNewDay,
                            modifier = Modifier.testTag("simulate_day_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "روز جدید",
                                tint = AccentOrange,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("روز بعد", color = AccentOrange, fontSize = 11.sp)
                        }
                    }
                }
            }

            // Horizontal Scroll Category Selector
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categories.forEach { category ->
                        val isSelected = category == selectedCategory
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (isSelected) AccentTeal else NavySurface)
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) AccentTeal else NavySurfaceVariant,
                                    shape = RoundedCornerShape(20.dp)
                                )
                                .clickable { onCategorySelected(category) }
                                .padding(horizontal = 16.dp, vertical = 10.dp)
                                .testTag("category_pill_$category"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = category,
                                color = if (isSelected) Color.White else TextSecondary,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // Top Info/Progress Status Bar
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = NavySurface),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "پیشرفت جلسه امروز",
                                color = TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "کارت $currentCardIndexInSession از $totalSessionCount",
                                color = AccentTeal,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(10.dp))
                        
                        val sessionProgressValue = if (totalSessionCount > 0) {
                            (reviewedCount.toFloat() / totalSessionCount.toFloat())
                        } else {
                            1f
                        }
                        
                        LinearProgressIndicator(
                            progress = { sessionProgressValue },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(CircleShape),
                            color = AccentTeal,
                            trackColor = NavySurfaceVariant,
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "کارت‌های جدید امروز: ${todayNewCards.size} از 10",
                                color = TextSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(AccentOrange.copy(alpha = 0.15f))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "آماده مرور: ${dueCards.count { !it.isReviewedToday }} کارت",
                                    color = AccentOrange,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // MAIN FLASHCARD VIEW
            item {
                if (activeCard != null) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onToggleAnswer() }
                            .testTag("main_flashcard"),
                        colors = CardDefaults.cardColors(containerColor = NavySurface),
                        shape = RoundedCornerShape(24.dp),
                        border = borderBrush()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Top Badges Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Box and review status
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(AccentTeal.copy(alpha = 0.15f))
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    val boxName = when (activeCard.boxIndex) {
                                        1 -> "جعبه ۱ — یادگیری و مرور روزانه"
                                        2 -> "جعبه ۲ — مرور اول (۱ روز بعد)"
                                        3 -> "جعبه ۳ — مرور دوم (۳ روز بعد)"
                                        4 -> "جعبه ۴ — تثبیت (۷ روز بعد)"
                                        5 -> "جعبه ۵ — تسلط کامل (۱۴ روز)"
                                        else -> "کارت جدید"
                                    }
                                    Text(
                                        text = boxName,
                                        color = AccentTeal,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                // Level Badge (C1/B2)
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(AccentGreen.copy(alpha = 0.2f))
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = activeCard.level,
                                        color = AccentGreen,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(28.dp))

                            // English Collocation (FORCE LTR)
                            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = activeCard.english,
                                        color = TextPrimary,
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.testTag("collocation_english")
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = activeCard.pronunciation,
                                        color = TextSecondary,
                                        fontSize = 14.sp,
                                        fontStyle = FontStyle.Italic,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            // Tap Hint
                            if (!showAnswer) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(NavySurfaceVariant)
                                        .padding(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Visibility,
                                        contentDescription = "مشاهده معنی",
                                        tint = TextSecondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "لمس کارت برای مشاهده معنی",
                                        color = TextSecondary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }

                            // Expanded Answer Section (Persian and IELTS Example)
                            AnimatedVisibility(
                                visible = showAnswer,
                                enter = fadeIn() + expandVertically(),
                                exit = fadeOut() + shrinkVertically()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 20.dp)
                                ) {
                                    // Divider line
                                    Spacer(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(1.dp)
                                            .background(NavySurfaceVariant)
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))

                                    // Persian Translation
                                    Text(
                                        text = "معنی فارسی:",
                                        color = TextSecondary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = activeCard.persian,
                                        color = AccentGreen,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    Spacer(modifier = Modifier.height(18.dp))

                                    // IELTS Writing Task 2 Example
                                    Text(
                                        text = "مثال IELTS Writing Task 2:",
                                        color = TextSecondary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    
                                    // Force English content to LTR
                                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                        Text(
                                            text = activeCard.example,
                                            color = TextPrimary,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            lineHeight = 22.sp,
                                            modifier = Modifier.fillMaxWidth(),
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                textDirection = TextDirection.Ltr
                                            )
                                        )
                                    }
                                    
                                    Spacer(modifier = Modifier.height(6.dp))
                                    
                                    // Persian Example Translation
                                    Text(
                                        text = activeCard.exampleTranslation,
                                        color = TextSecondary,
                                        fontSize = 13.sp,
                                        lineHeight = 22.sp,
                                        modifier = Modifier.fillMaxWidth(),
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            textDirection = TextDirection.Rtl
                                        )
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // Empty / Success State for Today's Session!
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = NavySurface),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(36.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(AccentGreen.copy(alpha = 0.15f))
                                    .padding(16.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "پایان مرور",
                                    tint = AccentGreen,
                                    modifier = Modifier.size(48.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(20.dp))
                            Text(
                                text = "آفرین! کارت‌های امروز تمام شد",
                                color = TextPrimary,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "شما تمام کارت‌های مربوط به موضوع «$selectedCategory» را برای امروز مرور کرده‌اید. می‌توانید موضوع دیگری را از بالای صفحه انتخاب کنید یا منتظر فردا بمانید.",
                                color = TextSecondary,
                                fontSize = 12.sp,
                                lineHeight = 22.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            // Spelling Practice Section
            if (activeCard != null) {
                item {
                    SpellingPracticeCard(activeCard = activeCard)
                }
            }

            // LEITNER BUTTONS BELOW THE CARD
            item {
                if (activeCard != null && showAnswer) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // "بلد نبودم" (Red)
                            Button(
                                onClick = { onRateCard(activeCard, "HARD") },
                                colors = ButtonDefaults.buttonColors(containerColor = AccentRed),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("rate_hard_btn"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = "بلد نبودم",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // "سخت بود" (Orange)
                            Button(
                                onClick = { onRateCard(activeCard, "MEDIUM") },
                                colors = ButtonDefaults.buttonColors(containerColor = AccentOrange),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("rate_medium_btn"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = "سخت بود",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // "یاد گرفتم" (Green)
                            Button(
                                onClick = { onRateCard(activeCard, "EASY") },
                                colors = ButtonDefaults.buttonColors(containerColor = AccentGreen),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("rate_easy_btn"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = "یاد گرفتم",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // LEITNER BOXES STATUS LABELS SECTION
            item {
                Text(
                    text = "وضعیت جعبه‌های لایتنر موضوع $selectedCategory",
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    BoxItem(label = "جعبه ۱", count = boxCounts[1] ?: 0, color = AccentRed, modifier = Modifier.weight(1f))
                    BoxItem(label = "جعبه ۲", count = boxCounts[2] ?: 0, color = AccentOrange, modifier = Modifier.weight(1f))
                    BoxItem(label = "جعبه ۳", count = boxCounts[3] ?: 0, color = AccentTeal, modifier = Modifier.weight(1f))
                    BoxItem(label = "جعبه ۴", count = boxCounts[4] ?: 0, color = AccentGreen, modifier = Modifier.weight(1f))
                    BoxItem(label = "جعبه ۵", count = boxCounts[5] ?: 0, color = Color(0xFF8B5CF6), modifier = Modifier.weight(1f))
                }
            }

            // Spacer for Navigation safety
            item { Spacer(modifier = Modifier.height(100.dp)) }
        }
    }
}

@Composable
fun BoxItem(
    label: String,
    count: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = NavySurface),
        shape = RoundedCornerShape(12.dp),
        border = borderBrush(color.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                color = TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "$count کارت",
                color = color,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// Utility to create glowing modern borders
@Composable
fun borderBrush(color: Color = AccentTeal.copy(alpha = 0.3f)) = BorderStroke(
    width = 1.dp,
    brush = Brush.linearGradient(
        colors = listOf(color, color.copy(alpha = 0.1f))
    )
)

@Composable
fun SpellingPracticeCard(
    activeCard: Collocation,
    modifier: Modifier = Modifier
) {
    var spellingInput by remember(activeCard) { mutableStateOf("") }
    var spellingChecked by remember(activeCard) { mutableStateOf(false) }
    var isCorrect by remember(activeCard) { mutableStateOf(false) }
    var isExpanded by remember(activeCard) { mutableStateOf(true) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("spelling_practice_card"),
        colors = CardDefaults.cardColors(containerColor = NavySurface),
        shape = RoundedCornerShape(20.dp),
        border = borderBrush(AccentTeal.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header with toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "املا",
                        tint = AccentTeal,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "✍️ تمرین املای عبارت انگلیسی",
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Text(
                    text = if (isExpanded) "بستن" else "باز کردن",
                    color = AccentTeal,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                ) {
                    Text(
                        text = "عبارت انگلیسی بالا را بدون نگاه کردن اینجا بنویسید تا املای خود را برای رایتینگ آیلتس قوی کنید:",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        lineHeight = 18.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        OutlinedTextField(
                            value = spellingInput,
                            onValueChange = {
                                if (!spellingChecked) {
                                    spellingInput = it
                                }
                            },
                            placeholder = {
                                Text(
                                    text = "Type collocation here...",
                                    color = TextSecondary.copy(alpha = 0.6f),
                                    fontSize = 13.sp
                                )
                            },
                            singleLine = true,
                            enabled = !spellingChecked,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedContainerColor = NavyDarkBg,
                                unfocusedContainerColor = NavyDarkBg,
                                disabledContainerColor = NavyDarkBg.copy(alpha = 0.5f),
                                focusedBorderColor = AccentTeal,
                                unfocusedBorderColor = NavySurfaceVariant,
                                disabledBorderColor = NavySurfaceVariant
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("spelling_input_field")
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (!spellingChecked) {
                            Button(
                                onClick = {
                                    if (spellingInput.isNotBlank()) {
                                        isCorrect = spellingInput.trim().equals(activeCard.english.trim(), ignoreCase = true)
                                        spellingChecked = true
                                    }
                                },
                                enabled = spellingInput.isNotBlank(),
                                colors = ButtonDefaults.buttonColors(containerColor = AccentTeal),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("spelling_check_btn")
                            ) {
                                Text("بررسی صحت املا", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Button(
                                onClick = {
                                    spellingInput = ""
                                    spellingChecked = false
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = NavySurfaceVariant),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("spelling_retry_btn")
                            ) {
                                Text("تلاش مجدد", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    if (spellingChecked) {
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (isCorrect) AccentGreen.copy(alpha = 0.1f) else AccentRed.copy(alpha = 0.1f)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(
                                1.dp,
                                if (isCorrect) AccentGreen.copy(alpha = 0.4f) else AccentRed.copy(alpha = 0.4f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (isCorrect) Icons.Default.Check else Icons.Default.Close,
                                        contentDescription = if (isCorrect) "صحیح" else "اشتباه",
                                        tint = if (isCorrect) AccentGreen else AccentRed,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isCorrect) "آفرین! املا کاملاً صحیح است." else "املای نوشته شده نادرست است.",
                                        color = if (isCorrect) AccentGreen else AccentRed,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                
                                if (!isCorrect) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "املای صحیح:",
                                        color = TextSecondary,
                                        fontSize = 11.sp
                                    )
                                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                        Text(
                                            text = activeCard.english,
                                            color = AccentGreen,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(top = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
