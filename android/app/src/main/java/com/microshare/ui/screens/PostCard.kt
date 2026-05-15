package com.microshare.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.microshare.model.Post
import com.microshare.ui.theme.*

@Composable
fun PostCard(post: Post, isLiked: Boolean = false, onClick: () -> Unit, onLike: () -> Unit, onAvatarClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 5.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Surface)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(36.dp).clip(CircleShape).background(PrimaryLight)
                        .clickableSingle { onAvatarClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        (post.nickname.firstOrNull()?.toString() ?: "U"),
                        color = White, fontSize = 14.sp, fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.clickableSingle { onAvatarClick() }) {
                    Text(post.nickname, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text(post.created_at.take(16), fontSize = 11.sp, color = TextHint)
                }
            }
            if (post.topic.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Surface(shape = RoundedCornerShape(8.dp), color = TagBg) {
                    Text("#${post.topic}", modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp), fontSize = 11.sp, color = TagText)
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(post.content, maxLines = 3, overflow = TextOverflow.Ellipsis, fontSize = 15.sp, color = TextPrimary)
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onLike, modifier = Modifier.size(32.dp)) {
                    Icon(
                        if (isLiked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = null,
                        tint = if (isLiked) LikedColor else TextHint,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Text("${post.like_count}", fontSize = 12.sp, color = TextHint)
                Spacer(Modifier.width(16.dp))
                Icon(Icons.Outlined.ChatBubbleOutline, null, tint = TextHint, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("${post.comment_count}", fontSize = 12.sp, color = TextHint)
                Spacer(Modifier.width(16.dp))
                Text("${post.favorite_count} 收藏", fontSize = 12.sp, color = TextHint)
            }
        }
    }
}

@Composable
fun Modifier.clickableSingle(onClick: () -> Unit) = this.clickable(
    indication = null, interactionSource = remember { MutableInteractionSource() }
) { onClick() }
