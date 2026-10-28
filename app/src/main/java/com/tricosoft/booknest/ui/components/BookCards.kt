package com.tricosoft.booknest.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tricosoft.booknest.data.BookUi
import com.tricosoft.booknest.ui.bn
import com.tricosoft.booknest.ui.formatCount

@Composable
private fun Stat(icon: ImageVector, count: Int, iconSize: Dp, textSize: Int) {
    val c = bn()
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = c.mutedForeground, modifier = Modifier.size(iconSize))
        Spacer(Modifier.width(4.dp))
        Txt(formatCount(count), size = textSize, color = c.mutedForeground)
    }
}

/** Web BookCard variant="featured" */
@Composable
fun FeaturedBookCard(b: BookUi, onClick: () -> Unit) {
    val c = bn()
    val book = b.book
    Column(
        modifier = Modifier
            .width(176.dp)
            .clickable(onClick = onClick),
    ) {
        BookCover(
            url = book.coverUrl, title = book.title, author = b.authorName,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(3f / 4f)
                .shadow(3.dp, RoundedCornerShape(12.dp)),
            shape = RoundedCornerShape(12.dp), titleSize = 14, padding = 16.dp,
        )
        Column(modifier = Modifier.padding(start = 2.dp, end = 2.dp, top = 8.dp)) {
            Txt(
                book.title, size = 14, weight = FontWeight.SemiBold, display = true,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            Txt(b.authorName, size = 12, color = c.mutedForeground, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Row(
                modifier = Modifier.padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FreePill(size = 10, horizontal = 8.dp)
                Stat(Icons.Outlined.FavoriteBorder, book.likesCount, 12.dp, 10)
            }
        }
    }
}

/** Web BookCard default variant */
@Composable
fun BookCard(b: BookUi, onClick: () -> Unit) {
    val c = bn()
    val book = b.book
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .bnCard(12.dp, 1.dp)
            .clickable(onClick = onClick)
            .padding(12.dp)
            .height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        BookCover(
            url = book.coverUrl, title = book.title, author = null,
            modifier = Modifier
                .width(80.dp)
                .height(112.dp),
            shape = RoundedCornerShape(12.dp), titleSize = 10, padding = 8.dp,
        )
        Column(modifier = Modifier
            .weight(1f)
            .fillMaxHeight()) {
            Txt(book.title, size = 16, weight = FontWeight.SemiBold, display = true, lineHeight = 21.sp)
            Txt(b.authorName, size = 12, color = c.mutedForeground, modifier = Modifier.padding(top = 2.dp))
            Txt(
                book.description ?: "", size = 12, color = c.mutedForeground.copy(alpha = 0.8f),
                maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 4.dp),
            )
            Spacer(Modifier.weight(1f))
            Row(
                modifier = Modifier.padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Stat(Icons.Outlined.FavoriteBorder, book.likesCount, 14.dp, 12)
                Stat(Icons.Outlined.ChatBubbleOutline, book.commentsCount, 14.dp, 12)
                Stat(Icons.Outlined.Visibility, book.readsCount, 14.dp, 12)
                Spacer(Modifier.weight(1f))
                FreePill(size = 12, horizontal = 10.dp)
            }
        }
    }
}

/** Tab / genre chip (rounded-full ba rounded-lg) */
@Composable
fun Chip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(50),
    horizontal: Dp = 16.dp,
    vertical: Dp = 6.dp,
    size: Int = 14,
    unselectedTransparent: Boolean = false,
    selectedBg: androidx.compose.ui.graphics.Color = bn().primary,
) {
    val c = bn()
    val bg = if (selected) selectedBg else if (unselectedTransparent) androidx.compose.ui.graphics.Color.Transparent else c.secondary
    val fg = when {
        selected -> c.primaryForeground
        unselectedTransparent -> c.mutedForeground
        else -> c.secondaryForeground
    }
    androidx.compose.foundation.layout.Box(
        modifier = modifier
            .clip(shape)
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = horizontal, vertical = vertical),
    ) {
        Txt(text, size = size, weight = FontWeight.Medium, color = fg, maxLines = 1)
    }
}
