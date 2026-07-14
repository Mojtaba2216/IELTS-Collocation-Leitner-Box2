package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
fun BoxesScreen(
    categories: List<String>,
    selectedCategory: String,
    onCategorySelected: (String) -> Unit,
    collocations: List<Collocation>,
    modifier: Modifier = Modifier
) {
    var activeBoxTab by remember { mutableStateOf(1) }
    var expandedCardId by remember { mutableStateOf<Int?>(null) }

    val categoryCards = collocations.filter { it.category == selectedCategory }
    val boxedCards = categoryCards.filter { it.boxIndex == activeBoxTab }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(NavyDarkBg)
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(30.dp))

            // Screen Header
            Text(
                text = "کتابخانه جعبه‌ها",
                color = TextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Text(
                text = "کارت‌های خود را بر اساس مراحل لایتنر مرور کنید",
                color = TextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Horizontal Category Selector
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
                            .padding(horizontal = 16.dp, vertical = 10.dp),
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

            Spacer(modifier = Modifier.height(16.dp))

            // Box Tabs Selector Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val boxColors = listOf(AccentRed, AccentOrange, AccentTeal, AccentGreen, Color(0xFF8B5CF6))
                (1..5).forEach { boxId ->
                    val isTabActive = activeBoxTab == boxId
                    val boxColor = boxColors[boxId - 1]
                    val cardCount = categoryCards.count { it.boxIndex == boxId }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isTabActive) boxColor.copy(alpha = 0.15f) else NavySurface)
                            .border(
                                width = 1.dp,
                                color = if (isTabActive) boxColor else NavySurfaceVariant,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable {
                                activeBoxTab = boxId
                                expandedCardId = null
                            }
                            .padding(vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "جعبه $boxId",
                            color = if (isTabActive) boxColor else TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "$cardCount کارت",
                            color = if (isTabActive) TextPrimary else TextSecondary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Box Description Box
            Card(
                colors = CardDefaults.cardColors(containerColor = NavySurfaceVariant),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "اطلاعات جعبه",
                        tint = AccentTeal,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    val boxDesc = when (activeBoxTab) {
                        1 -> "جعبه ۱: مرور روزانه. کارت‌های جدید و مواردی که اشتباه پاسخ داده‌اید اینجا قرار می‌گیرند."
                        2 -> "جعبه ۲: مرور پس از ۱ روز. اگر کارت را امروز درست جواب دهید، فردا مجدد مرور می‌کنید."
                        3 -> "جعبه ۳: مرور پس از ۳ روز. با پاسخ صحیح، کارت برای مرور ۳ روز آینده برنامه‌ریزی می‌شود."
                        4 -> "جعبه ۴: مرور پس از ۷ روز. مرحله تثبیت؛ تمرکز عمیق روی تثبیت کلمات در ذهن."
                        5 -> "جعبه ۵: مرور پس از ۱۴ روز. مرحله تسلط کامل؛ پس از پاسخ درست، برای همیشه بایگانی می‌شود."
                        else -> ""
                    }
                    Text(
                        text = boxDesc,
                        color = TextSecondary,
                        fontSize = 11.sp,
                        lineHeight = 18.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Collocations List
            if (boxedCards.isNotEmpty()) {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(boxedCards) { card ->
                        val isExpanded = expandedCardId == card.id
                        BoxCollocationItem(
                            card = card,
                            isExpanded = isExpanded,
                            onToggleExpand = {
                                expandedCardId = if (isExpanded) null else card.id
                            }
                        )
                    }
                    
                    item { Spacer(modifier = Modifier.height(100.dp)) }
                }
            } else {
                // Empty State
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(top = 40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.Bookmark,
                        contentDescription = "خالی",
                        tint = NavySurfaceVariant,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "هیچ کارتی در این جعبه نیست",
                        color = TextSecondary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun BoxCollocationItem(
    card: Collocation,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("box_card_item_${card.id}")
            .clip(RoundedCornerShape(16.dp))
            .clickable { onToggleExpand() },
        colors = CardDefaults.cardColors(containerColor = NavySurface),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            width = 1.dp,
            color = if (isExpanded) AccentTeal.copy(alpha = 0.5f) else Color.Transparent
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // English Term (LTR)
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Column(horizontalAlignment = Alignment.Start) {
                        Text(
                            text = card.english,
                            color = TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = card.pronunciation,
                            color = TextSecondary,
                            fontSize = 11.sp,
                            fontStyle = FontStyle.Italic
                        )
                    }
                }

                // Expand Chevron or badge
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(AccentGreen.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = card.level,
                            color = AccentGreen,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = "جزئیات",
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Expanded details
            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 14.dp)
                ) {
                    Spacer(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(NavySurfaceVariant)
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "معنی فارسی:",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = card.persian,
                        color = AccentTeal,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "مثال IELTS Writing Task 2:",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        Text(
                            text = card.example,
                            color = TextPrimary,
                            fontSize = 13.sp,
                            lineHeight = 20.sp,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                textDirection = TextDirection.Ltr
                            )
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = card.exampleTranslation,
                        color = TextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 20.sp
                    )
                }
            }
        }
    }
}
