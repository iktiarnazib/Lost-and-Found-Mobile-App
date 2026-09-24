package com.iktiarnazib.lostandfoundv2

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Comment
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ServerTimestamp
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Locale

// ============================================================
// 1. DATA MODELS & HELPERS  (unchanged logic)
// ============================================================

data class UserData(
    val uid: String = "",
    val name: String = "",
    val studentId: String = "",
    val roll: String = "",
    val semester: String = "",
    val batch: String = "",
    val email: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

data class LostFoundPost(
    val id: String = "",
    val userId: String = "",
    val username: String = "",
    val status: String = "",       // "Lost", "Found" or "Given"
    val title: String = "",
    val description: String = "",
    val location: String = "",
    val authorStudentId: String = "",
    val authorRoll: String = "",
    val authorSemester: String = "",
    val authorBatch: String = "",
    val claimedByName: String = "",
    val imageData: String = "",
    val likes: Map<String, Boolean> = emptyMap(),
    val commentCount: Long = 0,
    @ServerTimestamp val createdAt: Timestamp? = null
)

data class Comment(
    val id: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val text: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

data class Conversation(
    val id: String = "",
    val participants: List<String> = emptyList(),
    val participantNames: Map<String, String> = emptyMap(),
    val lastMessage: String = "",
    val lastMessageTime: Timestamp? = null,
    val unreadCounts: Map<String, Long> = emptyMap()
)

data class ChatMessage(
    val id: String = "",
    val senderId: String = "",
    val text: String = "",
    val createdAt: Timestamp? = null
)

data class ChatDestination(
    val chatId: String,
    val otherUserId: String,
    val otherUserName: String
)

enum class Screen { Login, SignUp, ForgotPassword, Home, Messages, Chat, CreatePost, Comments, Profile }

fun statusColor(status: String): Color = when (status.lowercase()) {
    "lost" -> Color(0xFFEF4444)
    "found" -> Color(0xFF3B82F6)
    "given" -> Color(0xFF22C55E)
    else -> Color(0xFF8E8EA3)
}

fun Conversation.otherUserName(myUid: String): String =
    participantNames.entries.firstOrNull { it.key != myUid }?.value ?: "Unknown"

fun Conversation.otherUserId(myUid: String): String =
    participants.firstOrNull { it != myUid } ?: ""

fun Conversation.unreadFor(myUid: String): Int = (unreadCounts[myUid] ?: 0L).toInt()

fun Timestamp?.timeAgo(): String {
    if (this == null) return ""
    val minutes = (System.currentTimeMillis() - toDate().time) / 60_000
    val hours = minutes / 60
    val days = hours / 24
    return when {
        minutes < 1 -> "Just now"
        minutes < 60 -> "${minutes}m ago"
        hours < 24 -> "${hours}h ago"
        days < 7 -> "${days}d ago"
        else -> SimpleDateFormat("dd MMM", Locale.getDefault()).format(toDate())
    }
}

fun Timestamp?.toChatTime(): String =
    this?.let { SimpleDateFormat("hh:mm a", Locale.getDefault()).format(it.toDate()) } ?: ""

// ============================================================
// 1b. IMAGE HELPERS (unchanged logic)
// ============================================================

@Composable
fun rememberDecodedImage(base64: String?): Bitmap? {
    return remember(base64) {
        if (base64.isNullOrBlank()) null
        else try {
            val bytes = Base64.decode(base64, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } catch (_: Exception) { null }
    }
}

sealed interface ImageEncodeResult {
    data class Success(val base64: String) : ImageEncodeResult
    data class Failure(val message: String) : ImageEncodeResult
}

suspend fun encodeImageToBase64(context: Context, uri: Uri): ImageEncodeResult =
    withContext(Dispatchers.IO) {
        try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            val boundsOk = context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, bounds)
                true
            } ?: false
            if (!boundsOk) {
                return@withContext ImageEncodeResult.Failure("Couldn't read the selected file. Try a different photo.")
            }

            var sample = 1
            while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= 900) sample *= 2
            val opts = BitmapFactory.Options().apply { inSampleSize = sample }
            val bitmap = context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, opts)
            }

            if (bitmap == null) {
                val raw = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                return@withContext if (raw != null && raw.size <= 350_000) {
                    ImageEncodeResult.Success(Base64.encodeToString(raw, Base64.NO_WRAP))
                } else {
                    ImageEncodeResult.Failure("Unsupported image format or file too large. Try a JPG/PNG photo.")
                }
            }

            val scaled = if (bitmap.width > 900 || bitmap.height > 900) {
                val scale = 900f / maxOf(bitmap.width, bitmap.height)
                Bitmap.createScaledBitmap(
                    bitmap,
                    (bitmap.width * scale).toInt().coerceAtLeast(1),
                    (bitmap.height * scale).toInt().coerceAtLeast(1),
                    true
                )
            } else bitmap

            var quality = 70
            var bytes: ByteArray
            do {
                val bos = ByteArrayOutputStream()
                scaled.compress(Bitmap.CompressFormat.JPEG, quality, bos)
                bytes = bos.toByteArray()
                quality -= 15
            } while (bytes.size > 300_000 && quality > 20)

            if (bytes.size > 400_000) {
                return@withContext ImageEncodeResult.Failure("Photo is too large to store. Try a smaller photo.")
            }

            ImageEncodeResult.Success(Base64.encodeToString(bytes, Base64.NO_WRAP))
        } catch (e: Exception) {
            ImageEncodeResult.Failure("Photo error: ${e.message ?: "unknown"}")
        }
    }

// ============================================================
// 1c. FINDORA THEME (logo pink palette) + SHARED UI COMPONENTS
// ============================================================

// ---- Brand palette, matched to the Findora logo ----
val BrandPinkDeep = Color(0xFFE0246E)   // deep hot pink (logo shadow pink)
val BrandPink = Color(0xFFF43F7F)       // the logo's main pink
val BrandPinkLight = Color(0xFFFF7FAE)  // soft pink highlight
val BrandPinkSoft = Color(0xFFFFD9E6)   // very light pink for containers

private val BrandGradient = Brush.linearGradient(
    listOf(BrandPinkDeep, BrandPinkLight)
)

// Slowly shifting brand gradient — used on splash, login hero, profile, drawer
@Composable
fun animatedBrandGradient(vertical: Boolean = false): Brush {
    val transition = rememberInfiniteTransition(label = "brandBg")
    val shift by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(5000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shift"
    )
    val c1 = lerp(BrandPinkDeep, BrandPinkLight, shift)
    val c2 = lerp(BrandPinkLight, BrandPinkDeep, shift)
    return if (vertical) Brush.verticalGradient(listOf(c1, c2))
    else Brush.linearGradient(listOf(c1, c2))
}

private val FindoraLight = lightColorScheme(
    primary = BrandPinkDeep,
    onPrimary = Color.White,
    primaryContainer = BrandPinkSoft,
    onPrimaryContainer = Color(0xFF5C0A2E),
    secondary = Color(0xFF745560),
    secondaryContainer = Color(0xFFFFDCE8),
    onSecondaryContainer = Color(0xFF2B1219),
    tertiary = Color(0xFF1F9D55),
    tertiaryContainer = Color(0xFFD3F5E2),
    background = Color(0xFFFDF4F7),          // pink-tinted off-white
    onBackground = Color(0xFF221218),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF221218),
    surfaceVariant = Color(0xFFF9E2EB),
    onSurfaceVariant = Color(0xFF745560),
    outline = Color(0xFFE5B8C8),
    error = Color(0xFFE04B4B),
    errorContainer = Color(0xFFFFE4E4)
)

private val FindoraDark = darkColorScheme(
    primary = Color(0xFFFF8AB4),
    onPrimary = Color(0xFF5C0A2E),
    primaryContainer = Color(0xFF8C1B4B),
    onPrimaryContainer = BrandPinkSoft,
    secondary = Color(0xFFE0BDC9),
    secondaryContainer = Color(0xFF4A2B36),
    onSecondaryContainer = Color(0xFFFFDCE8),
    tertiary = Color(0xFF6FE0A0),
    tertiaryContainer = Color(0xFF14532D),
    background = Color(0xFF190D12),          // deep pink-black
    onBackground = Color(0xFFF3DEE6),
    surface = Color(0xFF24141B),
    onSurface = Color(0xFFF3DEE6),
    surfaceVariant = Color(0xFF3A222C),
    onSurfaceVariant = Color(0xFFD3C0C8),
    outline = Color(0xFF5F404C),
    error = Color(0xFFFF7B7B),
    errorContainer = Color(0xFF4A1D1D)
)

@Composable
fun FindoraTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) FindoraDark else FindoraLight,
        content = content
    )
}

// Gradient pill button
@Composable
fun GradientButton(
    text: String,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    onClick: () -> Unit
) {
    val bg = if (enabled) BrandGradient
    else Brush.linearGradient(
        listOf(
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.surfaceVariant
        )
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .shadow(if (enabled) 10.dp else 0.dp, RoundedCornerShape(18.dp), spotColor = BrandPinkDeep)
            .clip(RoundedCornerShape(18.dp))
            .background(bg)
            .clickable(enabled = enabled && !isLoading) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                strokeWidth = 2.5.dp,
                color = Color.White
            )
        } else {
            Text(
                text = text,
                color = if (enabled) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// Reusable gradient circle action button (chat send, comment send)
@Composable
fun GradientCircleButton(
    icon: ImageVector,
    contentDescription: String,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val bg = if (enabled) BrandGradient
    else Brush.linearGradient(
        listOf(
            BrandPinkDeep.copy(alpha = 0.35f),
            BrandPinkLight.copy(alpha = 0.35f)
        )
    )
    Box(
        modifier = Modifier
            .size(46.dp)
            .clip(CircleShape)
            .background(bg)
            .clickable(enabled = enabled) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = contentDescription, tint = Color.White, modifier = Modifier.size(20.dp))
    }
}

// Gradient circle avatar with initial
@Composable
fun GradientAvatar(name: String, size: Dp, textStyle: androidx.compose.ui.text.TextStyle? = null) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(Brush.linearGradient(listOf(BrandPinkDeep, Color(0xFFFF9EC0)))),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = name.trim().take(1).uppercase().ifBlank { "?" },
            color = Color.White,
            fontWeight = FontWeight.Bold,
            style = textStyle ?: MaterialTheme.typography.titleMedium
        )
    }
}

// Colored status pill with icon
@Composable
fun StatusBadge(status: String, fontSize: Int = 11) {
    val color = statusColor(status)
    val icon = when (status.lowercase()) {
        "lost" -> Icons.Default.SearchOff
        "found" -> Icons.Default.CheckCircle
        "given" -> Icons.Default.TaskAlt
        else -> Icons.Default.AutoAwesome
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(13.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = status,
                color = color,
                fontWeight = FontWeight.Bold,
                fontSize = fontSize.sp
            )
        }
    }
}

// Modern empty state
@Composable
fun EmptyState(icon: ImageVector, title: String, subtitle: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(44.dp))
        }
        Spacer(modifier = Modifier.height(18.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

// Error banner chip
@Composable
fun ErrorBanner(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.errorContainer)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.Close, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(text, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
    }
}

// Pulsing "online" dot
@Composable
fun PulsingDot(color: Color) {
    val transition = rememberInfiniteTransition(label = "dot")
    val alpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
        label = "alpha"
    )
    Box(
        modifier = Modifier
            .size(8.dp)
            .clip(CircleShape)
            .background(color.copy(alpha = alpha))
    )
}

// ============================================================
// 2. REPOSITORIES (unchanged)
// ============================================================

class AuthRepository {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    fun currentUid(): String? = auth.currentUser?.uid

    fun authState(): Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser) }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    fun observeUserProfile(uid: String): Flow<UserData?> = callbackFlow {
        val reg = db.collection("users").document(uid)
            .addSnapshotListener { snapshot, _ ->
                trySend(snapshot?.toObject(UserData::class.java)?.copy(uid = uid))
            }
        awaitClose { reg.remove() }
    }

    suspend fun ensureProfile(user: FirebaseUser) {
        try {
            val ref = db.collection("users").document(user.uid)
            val snap = ref.get().await()
            if (!snap.exists()) {
                val fallback = UserData(
                    uid = user.uid,
                    name = user.displayName
                        ?: user.email?.substringBefore("@")
                        ?: "User",
                    email = user.email ?: ""
                )
                ref.set(fallback).await()
            }
        } catch (_: Exception) { }
    }

    suspend fun login(email: String, password: String): Result<Unit> = try {
        auth.signInWithEmailAndPassword(email, password).await()
        Result.success(Unit)
    } catch (e: Exception) { Result.failure(e) }

    suspend fun signUp(user: UserData, password: String): Result<Unit> = try {
        val result = auth.createUserWithEmailAndPassword(user.email, password).await()
        val uid = result.user?.uid ?: error("User creation failed")
        db.collection("users").document(uid).set(user.copy(uid = uid)).await()
        Result.success(Unit)
    } catch (e: Exception) { Result.failure(e) }

    suspend fun sendPasswordReset(email: String): Result<Unit> = try {
        auth.sendPasswordResetEmail(email).await()
        Result.success(Unit)
    } catch (e: Exception) { Result.failure(e) }

    fun logout() = auth.signOut()
}

class PostRepository {
    private val db = FirebaseFirestore.getInstance()

    fun observePosts(): Flow<Result<List<LostFoundPost>>> = callbackFlow {
        val reg = db.collection("posts")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.failure(error))
                    return@addSnapshotListener
                }
                val posts = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(LostFoundPost::class.java)?.copy(id = doc.id)
                } ?: emptyList()
                trySend(Result.success(posts))
            }
        awaitClose { reg.remove() }
    }

    suspend fun fetchPostsOnce(): Result<List<LostFoundPost>> = try {
        val snap = db.collection("posts")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get().await()
        Result.success(snap.documents.mapNotNull {
            it.toObject(LostFoundPost::class.java)?.copy(id = it.id)
        })
    } catch (e: Exception) { Result.failure(e) }

    suspend fun createPost(post: LostFoundPost): Result<Unit> = try {
        db.collection("posts").add(post).await()
        Result.success(Unit)
    } catch (e: Exception) { Result.failure(e) }

    suspend fun deletePost(postId: String): Result<Unit> = try {
        db.collection("posts").document(postId).delete().await()
        Result.success(Unit)
    } catch (e: Exception) { Result.failure(e) }

    suspend fun markPostGiven(postId: String, claimedByName: String): Result<Unit> = try {
        db.collection("posts").document(postId).update(
            mapOf(
                "status" to "Given",
                "claimedByName" to claimedByName
            )
        ).await()
        Result.success(Unit)
    } catch (e: Exception) { Result.failure(e) }

    suspend fun toggleLike(postId: String, uid: String, currentlyLiked: Boolean): Result<Unit> = try {
        val update = if (currentlyLiked)
            mapOf("likes.$uid" to FieldValue.delete())
        else
            mapOf("likes.$uid" to true)
        db.collection("posts").document(postId).update(update).await()
        Result.success(Unit)
    } catch (e: Exception) { Result.failure(e) }

    fun observeComments(postId: String): Flow<Result<List<Comment>>> = callbackFlow {
        val reg = db.collection("posts").document(postId)
            .collection("comments")
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.failure(error))
                    return@addSnapshotListener
                }
                val list = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Comment::class.java)?.copy(id = doc.id)
                } ?: emptyList()
                trySend(Result.success(list))
            }
        awaitClose { reg.remove() }
    }

    suspend fun addComment(postId: String, comment: Comment): Result<Unit> = try {
        val postRef = db.collection("posts").document(postId)
        val batch = db.batch()
        batch.set(
            postRef.collection("comments").document(),
            mapOf(
                "authorId" to comment.authorId,
                "authorName" to comment.authorName,
                "text" to comment.text,
                "createdAt" to FieldValue.serverTimestamp()
            )
        )
        batch.update(postRef, "commentCount", FieldValue.increment(1))
        batch.commit().await()
        Result.success(Unit)
    } catch (e: Exception) { Result.failure(e) }
}

class ChatRepository {
    private val db = FirebaseFirestore.getInstance()

    private fun chatIdFor(a: String, b: String) = if (a < b) "${a}_$b" else "${b}_$a"

    fun observeConversations(uid: String): Flow<Result<List<Conversation>>> = callbackFlow {
        val reg = db.collection("chats")
            .whereArrayContains("participants", uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.failure(error))
                    return@addSnapshotListener
                }
                val list = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Conversation::class.java)?.copy(id = doc.id)
                } ?: emptyList()
                trySend(Result.success(list.sortedByDescending { it.lastMessageTime?.seconds ?: 0L }))
            }
        awaitClose { reg.remove() }
    }

    suspend fun getOrCreateChat(
        myUid: String, myName: String, otherUid: String, otherName: String
    ): Result<String> = try {
        val chatId = chatIdFor(myUid, otherUid)
        val ref = db.collection("chats").document(chatId)
        ref.set(
            mapOf(
                "participants" to listOf(myUid, otherUid),
                "participantNames" to mapOf(myUid to myName, otherUid to otherName)
            ),
            SetOptions.merge()
        ).await()
        Result.success(chatId)
    } catch (e: Exception) { Result.failure(e) }

    fun observeMessages(chatId: String): Flow<Result<List<ChatMessage>>> = callbackFlow {
        val reg = db.collection("chats").document(chatId)
            .collection("messages")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.failure(error))
                    return@addSnapshotListener
                }
                val msgs = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(ChatMessage::class.java)?.copy(id = doc.id)
                } ?: emptyList()
                trySend(Result.success(msgs.sortedBy { it.createdAt?.seconds ?: 0L }))
            }
        awaitClose { reg.remove() }
    }

    suspend fun sendMessage(chatId: String, senderId: String, receiverId: String, text: String): Result<Unit> = try {
        val chatRef = db.collection("chats").document(chatId)
        val batch = db.batch()
        batch.set(
            chatRef.collection("messages").document(),
            mapOf("senderId" to senderId, "text" to text, "createdAt" to FieldValue.serverTimestamp())
        )
        batch.update(
            chatRef,
            mapOf(
                "lastMessage" to text,
                "lastMessageTime" to FieldValue.serverTimestamp(),
                "unreadCounts.$receiverId" to FieldValue.increment(1)
            )
        )
        batch.commit().await()
        Result.success(Unit)
    } catch (e: Exception) { Result.failure(e) }

    suspend fun markChatRead(chatId: String, myUid: String) {
        try { db.collection("chats").document(chatId).update("unreadCounts.$myUid", 0).await() }
        catch (_: Exception) { }
    }
}

// ============================================================
// 3. VIEWMODELS (unchanged)
// ============================================================

class AuthViewModel : ViewModel() {
    private val repo = AuthRepository()

    private val _currentUser = MutableStateFlow<UserData?>(null)
    val currentUser: StateFlow<UserData?> = _currentUser.asStateFlow()

    private val _isCheckingAuth = MutableStateFlow(true)
    val isCheckingAuth: StateFlow<Boolean> = _isCheckingAuth.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    private val _infoMessage = MutableStateFlow<String?>(null)
    val infoMessage: StateFlow<String?> = _infoMessage.asStateFlow()

    private var profileJob: Job? = null

    init {
        viewModelScope.launch {
            repo.authState().collect { firebaseUser ->
                _isCheckingAuth.value = false
                profileJob?.cancel()
                if (firebaseUser == null) {
                    _currentUser.value = null
                } else {
                    var ensured = false
                    profileJob = viewModelScope.launch {
                        repo.observeUserProfile(firebaseUser.uid).collect { profile ->
                            if (profile == null) {
                                if (!ensured) {
                                    ensured = true
                                    repo.ensureProfile(firebaseUser)
                                }
                            } else {
                                _currentUser.value = profile
                            }
                        }
                    }
                }
            }
        }
    }

    fun login(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) return
        viewModelScope.launch {
            _isLoading.value = true; _authError.value = null
            repo.login(email.trim(), password).onFailure { _authError.value = friendlyError(it) }
            _isLoading.value = false
        }
    }

    fun signUp(user: UserData, password: String) {
        viewModelScope.launch {
            _isLoading.value = true; _authError.value = null
            repo.signUp(user, password).onFailure { _authError.value = friendlyError(it) }
            _isLoading.value = false
        }
    }

    fun sendPasswordReset(email: String) {
        viewModelScope.launch {
            _isLoading.value = true; _authError.value = null
            repo.sendPasswordReset(email.trim())
                .onSuccess { _infoMessage.value = "Reset email sent to ${email.trim()}" }
                .onFailure { _authError.value = friendlyError(it) }
            _isLoading.value = false
        }
    }

    fun logout() = repo.logout()

    fun clearMessages() { _authError.value = null; _infoMessage.value = null }

    private fun friendlyError(e: Throwable): String = when ((e as? FirebaseAuthException)?.errorCode) {
        "ERROR_INVALID_EMAIL" -> "Please enter a valid email address."
        "ERROR_WRONG_PASSWORD" -> "Incorrect password. Please try again."
        "ERROR_USER_NOT_FOUND" -> "No account found with this email."
        "ERROR_EMAIL_ALREADY_IN_USE" -> "This email is already registered."
        "ERROR_WEAK_PASSWORD" -> "Password must be at least 6 characters."
        "ERROR_TOO_MANY_REQUESTS" -> "Too many attempts. Try again later."
        "ERROR_NETWORK_REQUEST_FAILED" -> "Network error. Check your connection."
        else -> e.message ?: "Something went wrong. Please try again."
    }
}

class FeedViewModel : ViewModel() {
    private val postRepo = PostRepository()

    private val _posts = MutableStateFlow<List<LostFoundPost>>(emptyList())
    val posts: StateFlow<List<LostFoundPost>> = _posts.asStateFlow()

    private val _isPosting = MutableStateFlow(false)
    val isPosting: StateFlow<Boolean> = _isPosting.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _feedError = MutableStateFlow<String?>(null)
    val feedError: StateFlow<String?> = _feedError.asStateFlow()

    private var feedJob: Job? = null

    init { startFeedListener() }

    private fun startFeedListener() {
        feedJob?.cancel()
        feedJob = viewModelScope.launch {
            postRepo.observePosts().collect { result ->
                result
                    .onSuccess { _posts.value = it; _feedError.value = null }
                    .onFailure { _feedError.value = "Feed error: ${it.message}" }
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            postRepo.fetchPostsOnce()
                .onSuccess { _posts.value = it; _feedError.value = null }
                .onFailure { _feedError.value = "Refresh failed: ${it.message}" }
            _isRefreshing.value = false
            startFeedListener()
        }
    }

    fun createPost(
        status: String, title: String, description: String,
        location: String, imageData: String, author: UserData?,
        onDone: (Boolean) -> Unit
    ) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        viewModelScope.launch {
            _isPosting.value = true
            val result = postRepo.createPost(
                LostFoundPost(
                    userId = uid,
                    username = author?.name ?: "Anonymous",
                    authorStudentId = author?.studentId ?: "",
                    authorRoll = author?.roll ?: "",
                    authorSemester = author?.semester ?: "",
                    authorBatch = author?.batch ?: "",
                    status = status, title = title,
                    description = description, location = location,
                    imageData = imageData
                )
            )
            _isPosting.value = false
            result
                .onSuccess { onDone(true) }
                .onFailure {
                    _feedError.value = "Could not save post: ${it.message}"
                    onDone(false)
                }
        }
    }

    fun deletePost(post: LostFoundPost) {
        val original = _posts.value
        _posts.value = original.filterNot { it.id == post.id }
        viewModelScope.launch {
            postRepo.deletePost(post.id).onFailure {
                _posts.value = original
                _feedError.value = "Could not delete post: ${it.message}"
            }
        }
    }

    fun markPostGiven(post: LostFoundPost, claimedByName: String) {
        if (claimedByName.isBlank()) return
        viewModelScope.launch {
            postRepo.markPostGiven(post.id, claimedByName)
                .onFailure { _feedError.value = "Could not update post: ${it.message}" }
        }
    }

    fun toggleLike(post: LostFoundPost, myUid: String) {
        if (myUid.isBlank()) return
        val currentlyLiked = post.likes[myUid] == true
        _posts.value = _posts.value.map {
            if (it.id == post.id) {
                val newLikes = it.likes.toMutableMap()
                if (currentlyLiked) newLikes.remove(myUid) else newLikes[myUid] = true
                it.copy(likes = newLikes)
            } else it
        }
        viewModelScope.launch { postRepo.toggleLike(post.id, myUid, currentlyLiked) }
    }
}

class MessagesViewModel : ViewModel() {
    private val chatRepo = ChatRepository()
    private val authRepo = AuthRepository()

    private val _conversations = MutableStateFlow<List<Conversation>>(emptyList())
    val conversations: StateFlow<List<Conversation>> = _conversations.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    val myUid: String get() = authRepo.currentUid() ?: ""

    private var convJob: Job? = null

    init { startListening() }

    private fun startListening() {
        val uid = authRepo.currentUid() ?: return
        convJob?.cancel()
        convJob = viewModelScope.launch {
            chatRepo.observeConversations(uid).collect { result ->
                result
                    .onSuccess { _conversations.value = it; _error.value = null }
                    .onFailure { _error.value = "Messages error: ${it.message}" }
            }
        }
    }

    fun retryIfNeeded() { if (_error.value != null) startListening() }
    fun retry() { _error.value = null; startListening() }

    fun startChatWith(
        otherUid: String, otherName: String, myName: String,
        onReady: (String, String) -> Unit,
        onError: (String) -> Unit
    ) {
        val uid = authRepo.currentUid()
        if (uid == null) { onError("Not logged in."); return }
        if (otherUid.isBlank()) { onError("This post has no author info."); return }
        if (otherUid == uid) { onError("This is your own post. Use a second account to test chat."); return }
        viewModelScope.launch {
            chatRepo.getOrCreateChat(uid, myName, otherUid, otherName)
                .onSuccess { onReady(it, otherName) }
                .onFailure { onError("Could not open chat: ${it.message}") }
        }
    }

    fun markChatRead(chatId: String) {
        val uid = authRepo.currentUid() ?: return
        viewModelScope.launch { chatRepo.markChatRead(chatId, uid) }
    }
}

class ChatViewModel : ViewModel() {
    private val chatRepo = ChatRepository()
    private val authRepo = AuthRepository()

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _sendError = MutableStateFlow<String?>(null)
    val sendError: StateFlow<String?> = _sendError.asStateFlow()

    private val _loadError = MutableStateFlow<String?>(null)
    val loadError: StateFlow<String?> = _loadError.asStateFlow()

    val myUid: String get() = authRepo.currentUid() ?: ""

    private var currentChatId: String? = null
    private var otherUserId: String = ""
    private var listenerJob: Job? = null
    private var lastAttachFailed = false

    fun initChat(chatId: String, otherUid: String) {
        if (currentChatId == chatId && !lastAttachFailed) return
        currentChatId = chatId
        otherUserId = otherUid
        _messages.value = emptyList()
        _sendError.value = null
        _loadError.value = null
        listenerJob?.cancel()
        listenerJob = viewModelScope.launch {
            chatRepo.observeMessages(chatId).collect { result ->
                result
                    .onSuccess {
                        _messages.value = it
                        lastAttachFailed = false
                        _loadError.value = null
                    }
                    .onFailure {
                        lastAttachFailed = true
                        _loadError.value = "Can't load messages: ${it.message}"
                    }
            }
        }
    }

    fun retryLoad() {
        val chatId = currentChatId ?: return
        initChat(chatId, otherUserId)
    }

    fun sendMessage(text: String) {
        val chatId = currentChatId ?: return
        val uid = authRepo.currentUid() ?: return
        if (text.isBlank()) return
        viewModelScope.launch {
            chatRepo.sendMessage(chatId, uid, otherUserId, text.trim())
                .onFailure { _sendError.value = "Message failed to send: ${it.message}" }
        }
    }

    fun clearError() { _sendError.value = null }
}

class CommentsViewModel : ViewModel() {
    private val postRepo = PostRepository()

    private val _comments = MutableStateFlow<List<Comment>>(emptyList())
    val comments: StateFlow<List<Comment>> = _comments.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private var currentPostId: String? = null
    private var job: Job? = null

    fun initPost(postId: String) {
        if (currentPostId == postId) return
        currentPostId = postId
        job?.cancel()
        job = viewModelScope.launch {
            postRepo.observeComments(postId).collect { result ->
                result
                    .onSuccess { _comments.value = it; _error.value = null }
                    .onFailure { _error.value = "Comments error: ${it.message}" }
            }
        }
    }

    fun addComment(postId: String, text: String, author: UserData?) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        if (text.isBlank()) return
        viewModelScope.launch {
            postRepo.addComment(
                postId,
                Comment(authorId = uid, authorName = author?.name ?: "Anonymous", text = text.trim())
            ).onFailure { _error.value = "Could not comment: ${it.message}" }
        }
    }
}

// ============================================================
// 4. MAIN ACTIVITY & NAVIGATION
// ============================================================

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FindoraTheme {
                AppNavigator()
            }
        }
    }
}

@Composable
fun AppNavigator() {
    val authViewModel: AuthViewModel = viewModel()
    val currentUser by authViewModel.currentUser.collectAsState()
    val isCheckingAuth by authViewModel.isCheckingAuth.collectAsState()

    var currentScreen by remember { mutableStateOf(Screen.Login) }
    var isDrawerOpen by remember { mutableStateOf(false) }
    var chatDestination by remember { mutableStateOf<ChatDestination?>(null) }
    var selectedPost by remember { mutableStateOf<LostFoundPost?>(null) }

    val context = LocalContext.current

    LaunchedEffect(currentUser?.uid) {
        chatDestination = null
        selectedPost = null
        currentScreen = if (currentUser != null) Screen.Home else Screen.Login
        isDrawerOpen = false
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        if (isCheckingAuth) {
            // ---- Branded animated splash ----
            val pulse = rememberInfiniteTransition(label = "splashPulse")
            val logoScale by pulse.animateFloat(
                initialValue = 0.94f,
                targetValue = 1.06f,
                animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
                label = "logoScale"
            )
            Box(
                Modifier
                    .fillMaxSize()
                    .background(animatedBrandGradient(vertical = true)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(110.dp)
                            .scale(logoScale)
                            .shadow(24.dp, CircleShape, spotColor = Color.Black.copy(alpha = 0.4f))
                            .clip(CircleShape)
                            .background(Color.White)
                            .padding(6.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.findoralogo),
                            contentDescription = "Findora",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize().clip(CircleShape)
                        )
                    }
                    Spacer(modifier = Modifier.height(30.dp))
                    CircularProgressIndicator(color = Color.White, strokeWidth = 3.dp)
                    Spacer(modifier = Modifier.height(18.dp))
                    Text("Findora", color = Color.White, fontWeight = FontWeight.Black, fontSize = 22.sp)
                }
            }
        } else {
            val user = currentUser
            if (user == null) {
                // ---------- AUTH SCREENS ----------
                Crossfade(targetState = currentScreen, animationSpec = tween(250), label = "auth") { screen ->
                    when (screen) {
                        Screen.SignUp -> SignUpScreen(
                            authViewModel = authViewModel,
                            onBackClick = { currentScreen = Screen.Login }
                        )
                        Screen.ForgotPassword -> ForgotPasswordScreen(
                            authViewModel = authViewModel,
                            onBackClick = { currentScreen = Screen.Login }
                        )
                        else -> LoginScreen(
                            authViewModel = authViewModel,
                            onSignUpClick = { currentScreen = Screen.SignUp },
                            onForgotPasswordClick = { currentScreen = Screen.ForgotPassword }
                        )
                    }
                }
            } else {
                // ---------- LOGGED-IN ----------
                val feedViewModel: FeedViewModel = viewModel(key = "feed_${user.uid}")
                val messagesViewModel: MessagesViewModel = viewModel(key = "messages_${user.uid}")
                val chatViewModel: ChatViewModel = viewModel(key = "chat_${user.uid}")
                val commentsViewModel: CommentsViewModel = viewModel(key = "comments_${user.uid}")

                val posts by feedViewModel.posts.collectAsState()
                val conversations by messagesViewModel.conversations.collectAsState()
                val feedError by feedViewModel.feedError.collectAsState()
                val isRefreshing by feedViewModel.isRefreshing.collectAsState()
                val totalUnread = conversations.sumOf { it.unreadFor(user.uid) }

                val myPosts = posts.filter { it.userId == user.uid }
                val showBottomBar = currentScreen == Screen.Home ||
                        currentScreen == Screen.Messages || currentScreen == Screen.Profile

                Column(modifier = Modifier.fillMaxSize()) {
                    Box(modifier = Modifier.weight(1f)) {
                        Crossfade(targetState = currentScreen, animationSpec = tween(260), label = "screens") { screen ->
                            when (screen) {
                                Screen.Messages -> MessageScreen(
                                    viewModel = messagesViewModel,
                                    onBackClick = { currentScreen = Screen.Home },
                                    onOpenChat = { conversation ->
                                        messagesViewModel.markChatRead(conversation.id)
                                        chatDestination = ChatDestination(
                                            chatId = conversation.id,
                                            otherUserId = conversation.otherUserId(user.uid),
                                            otherUserName = conversation.otherUserName(user.uid)
                                        )
                                        currentScreen = Screen.Chat
                                    }
                                )
                                Screen.Chat -> ChatScreen(
                                    destination = chatDestination,
                                    viewModel = chatViewModel,
                                    onBackClick = { currentScreen = Screen.Messages }
                                )
                                Screen.CreatePost -> CreatePostScreen(
                                    viewModel = feedViewModel,
                                    currentUser = user,
                                    onBackClick = { currentScreen = Screen.Home },
                                    onPostCreated = { currentScreen = Screen.Home }
                                )
                                Screen.Comments -> CommentsScreen(
                                    post = selectedPost,
                                    viewModel = commentsViewModel,
                                    user = user,
                                    onBackClick = { currentScreen = Screen.Home }
                                )
                                Screen.Profile -> ProfileScreen(
                                    user = user,
                                    myPosts = myPosts,
                                    onMarkGiven = { post, claimName ->
                                        feedViewModel.markPostGiven(post, claimName)
                                    },
                                    onDeletePost = { post -> feedViewModel.deletePost(post) },
                                    onMenuClick = { isDrawerOpen = true },
                                    onBackClick = { currentScreen = Screen.Home }
                                )
                                else -> HomeScreen(
                                    posts = posts,
                                    userName = user.name,
                                    myUid = user.uid,
                                    feedError = feedError,
                                    isRefreshing = isRefreshing,
                                    onRefresh = { feedViewModel.refresh() },
                                    onUserMessageClick = { post ->
                                        messagesViewModel.startChatWith(
                                            otherUid = post.userId,
                                            otherName = post.username,
                                            myName = user.name,
                                            onReady = { chatId, otherName ->
                                                chatDestination = ChatDestination(chatId, post.userId, otherName)
                                                currentScreen = Screen.Chat
                                            },
                                            onError = { msg ->
                                                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                            }
                                        )
                                    },
                                    onLikeClick = { post -> feedViewModel.toggleLike(post, user.uid) },
                                    onCommentClick = { post ->
                                        selectedPost = post
                                        currentScreen = Screen.Comments
                                    },
                                    onDeletePost = { post -> feedViewModel.deletePost(post) },
                                    onAddPostClick = { currentScreen = Screen.CreatePost },
                                    onMenuClick = { isDrawerOpen = true }
                                )
                            }
                        }
                    }

                    // ---- Floating pill bottom navigation ----
                    AnimatedVisibility(
                        visible = showBottomBar,
                        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
                    ) {
                        FindoraBottomBar(
                            current = currentScreen,
                            unreadCount = totalUnread,
                            onHome = { currentScreen = Screen.Home },
                            onMessages = { currentScreen = Screen.Messages },
                            onProfile = { currentScreen = Screen.Profile }
                        )
                    }
                }
            }
        }

        // ---------- Drawer scrim ----------
        AnimatedVisibility(
            visible = isDrawerOpen,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.55f))
                    .clickable { isDrawerOpen = false }
            )
        }

        // ---------- Drawer ----------
        AnimatedVisibility(
            visible = isDrawerOpen,
            enter = slideInHorizontally(initialOffsetX = { it }),
            exit = slideOutHorizontally(targetOffsetX = { it }),
            modifier = Modifier.align(Alignment.CenterEnd)
        ) {
            RightDrawerUI(
                user = currentUser,
                onHomeClick = { isDrawerOpen = false; currentScreen = Screen.Home },
                onProfileClick = { isDrawerOpen = false; currentScreen = Screen.Profile },
                onLogoutClick = { isDrawerOpen = false; authViewModel.logout() }
            )
        }
    }
}

// Floating pill bottom bar with animated selection
@Composable
fun FindoraBottomBar(
    current: Screen,
    unreadCount: Int,
    onHome: () -> Unit,
    onMessages: () -> Unit,
    onProfile: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp),
        shape = RoundedCornerShape(26.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 14.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BottomNavItem(Icons.Default.Home, "Home", current == Screen.Home, onClick = onHome)
            BottomNavItem(Icons.Default.Email, "Chats", current == Screen.Messages, badge = unreadCount, onClick = onMessages)
            BottomNavItem(Icons.Default.Person, "Profile", current == Screen.Profile, onClick = onProfile)
        }
    }
}

@Composable
fun BottomNavItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    badge: Int = 0,
    onClick: () -> Unit
) {
    val tint by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(250),
        label = "navTint"
    )
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.15f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "navScale"
    )
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 4.dp)
    ) {
        Box {
            Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(25.dp).scale(scale))
            if (badge > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 7.dp, y = (-3).dp)
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(BrandPinkDeep),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        badge.toString(),
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        Text(
            label,
            color = tint,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

// ============================================================
// 5. DRAWER (animated pink gradient header)
// ============================================================

@Composable
fun RightDrawerUI(
    user: UserData?,
    onHomeClick: () -> Unit,
    onProfileClick: () -> Unit,
    onLogoutClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxHeight()
            .width(310.dp),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, bottomStart = 28.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(animatedBrandGradient(vertical = true))
            ) {
                Row(
                    modifier = Modifier
                        .statusBarsPadding()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.25f))
                            .padding(5.dp)
                    ) {
                        GradientAvatar(user?.name ?: "G", 46.dp, MaterialTheme.typography.titleMedium)
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = user?.name ?: "Guest User",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = user?.email ?: "Not logged in",
                            color = Color.White.copy(alpha = 0.85f),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            DrawerItem(icon = Icons.Default.Home, text = "Home", onClick = onHomeClick)
            DrawerItem(icon = Icons.Default.Person, text = "Profile", onClick = onProfileClick)

            Spacer(modifier = Modifier.weight(1f))

            DrawerItem(
                icon = Icons.Default.Logout,
                text = "Logout",
                tint = MaterialTheme.colorScheme.error,
                containerTint = MaterialTheme.colorScheme.error.copy(alpha = 0.10f),
                onClick = onLogoutClick
            )
            Text(
                "Findora • v2.0",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
            )
            Spacer(modifier = Modifier.navigationBarsPadding())
        }
    }
}

@Composable
fun DrawerItem(
    icon: ImageVector,
    text: String,
    tint: Color = MaterialTheme.colorScheme.primary,
    containerTint: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(containerTint),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = text, tint = tint, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(14.dp))
        Text(text, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyLarge)
    }
}

// ============================================================
// 6. LOGIN SCREEN (animated pink hero + sheet form)
// ============================================================

@Composable
fun LoginScreen(authViewModel: AuthViewModel, onSignUpClick: () -> Unit, onForgotPasswordClick: () -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    val isLoading by authViewModel.isLoading.collectAsState()
    val error by authViewModel.authError.collectAsState()

    LaunchedEffect(Unit) { authViewModel.clearMessages() }

    val pulse = rememberInfiniteTransition(label = "loginPulse")
    val logoScale by pulse.animateFloat(
        initialValue = 0.97f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(tween(1400), RepeatMode.Reverse),
        label = "logoScale"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Animated gradient hero
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(animatedBrandGradient(vertical = true))
        ) {
            Column(
                modifier = Modifier
                    .statusBarsPadding()
                    .fillMaxWidth()
                    .padding(vertical = 40.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(104.dp)
                        .scale(logoScale)
                        .shadow(20.dp, CircleShape, spotColor = Color.Black.copy(alpha = 0.35f))
                        .clip(CircleShape)
                        .background(Color.White)
                        .padding(5.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.findoralogo),
                        contentDescription = "Findora Logo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize().clip(CircleShape)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Findora",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
                Text(
                    "Lost something? Found something? Start here.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }
        }

        // Form sheet
        Column(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Welcome back 👋",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                "Login to continue",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 28.dp)
            )

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                singleLine = true,
                shape = RoundedCornerShape(18.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                trailingIcon = {
                    val image = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(imageVector = image, contentDescription = if (passwordVisible) "Hide password" else "Show password")
                    }
                },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                singleLine = true,
                shape = RoundedCornerShape(18.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                TextButton(onClick = onForgotPasswordClick) {
                    Text("Forgot Password?", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                }
            }

            error?.let {
                ErrorBanner(it)
                Spacer(modifier = Modifier.height(8.dp))
            }

            Spacer(modifier = Modifier.height(8.dp))

            GradientButton(
                text = "Login",
                enabled = email.isNotBlank() && password.isNotBlank(),
                isLoading = isLoading
            ) { authViewModel.login(email, password) }

            Spacer(modifier = Modifier.height(28.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Don't have an account?", style = MaterialTheme.typography.bodyMedium)
                TextButton(onClick = onSignUpClick) {
                    Text("Sign Up", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

// ============================================================
// 7. FORGOT PASSWORD SCREEN
// ============================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForgotPasswordScreen(authViewModel: AuthViewModel, onBackClick: () -> Unit) {
    var email by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }

    val isLoading by authViewModel.isLoading.collectAsState()
    val error by authViewModel.authError.collectAsState()
    val infoMessage by authViewModel.infoMessage.collectAsState()

    LaunchedEffect(infoMessage) {
        infoMessage?.let {
            snackbarHostState.showSnackbar(it, duration = SnackbarDuration.Long)
            authViewModel.clearMessages()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Forgot Password", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back to Login")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(36.dp))

            Box(
                modifier = Modifier
                    .size(110.dp)
                    .shadow(16.dp, CircleShape, spotColor = BrandPinkDeep)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(44.dp)
                )
            }

            Text(
                "Reset Password",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 24.dp, bottom = 8.dp)
            )
            Text(
                "Enter your email and we'll send you a link to reset your password.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email Address") },
                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                singleLine = true,
                shape = RoundedCornerShape(18.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier.fillMaxWidth()
            )

            error?.let {
                ErrorBanner(it)
                Spacer(modifier = Modifier.height(8.dp))
            }

            Spacer(modifier = Modifier.height(24.dp))

            GradientButton(
                text = "Send Reset Email",
                enabled = email.isNotBlank(),
                isLoading = isLoading
            ) { authViewModel.sendPasswordReset(email) }
        }
    }
}

// ============================================================
// 8. SIGN UP SCREEN
// ============================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignUpScreen(authViewModel: AuthViewModel, onBackClick: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var id by remember { mutableStateOf("") }
    var roll by remember { mutableStateOf("") }
    var semester by remember { mutableStateOf("") }
    var batch by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    val isLoading by authViewModel.isLoading.collectAsState()
    val error by authViewModel.authError.collectAsState()

    val isFormValid = name.isNotBlank() && id.isNotBlank() && roll.isNotBlank() &&
            semester.isNotBlank() && batch.isNotBlank() && email.isNotBlank() &&
            password.isNotBlank() && password == confirmPassword

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Create Account", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back to Login")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            OutlinedTextField(
                value = name, onValueChange = { name = it },
                label = { Text("Full Name") },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                singleLine = true, shape = RoundedCornerShape(18.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = id, onValueChange = { id = it },
                label = { Text("Student ID") },
                leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null) },
                singleLine = true, shape = RoundedCornerShape(18.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = roll, onValueChange = { roll = it },
                label = { Text("Roll Number") },
                leadingIcon = { Icon(Icons.Default.Numbers, contentDescription = null) },
                singleLine = true, shape = RoundedCornerShape(18.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = semester, onValueChange = { semester = it },
                label = { Text("Semester") },
                leadingIcon = { Icon(Icons.Default.DateRange, contentDescription = null) },
                singleLine = true, shape = RoundedCornerShape(18.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = batch, onValueChange = { batch = it },
                label = { Text("Batch") },
                leadingIcon = { Icon(Icons.Default.School, contentDescription = null) },
                singleLine = true, shape = RoundedCornerShape(18.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = email, onValueChange = { email = it },
                label = { Text("Email") },
                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                singleLine = true, shape = RoundedCornerShape(18.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = password, onValueChange = { password = it },
                label = { Text("Password") },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                trailingIcon = {
                    val image = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(imageVector = image, contentDescription = if (passwordVisible) "Hide password" else "Show password")
                    }
                },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                singleLine = true, shape = RoundedCornerShape(18.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = confirmPassword, onValueChange = { confirmPassword = it },
                label = { Text("Confirm Password") },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                singleLine = true, shape = RoundedCornerShape(18.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                ),
                isError = confirmPassword.isNotBlank() && password != confirmPassword,
                modifier = Modifier.fillMaxWidth()
            )

            error?.let { ErrorBanner(it) }

            Spacer(modifier = Modifier.height(8.dp))

            GradientButton(
                text = "Create Account",
                enabled = isFormValid,
                isLoading = isLoading
            ) {
                authViewModel.signUp(
                    UserData(
                        name = name.trim(),
                        studentId = id.trim(),
                        roll = roll.trim(),
                        semester = semester.trim(),
                        batch = batch.trim(),
                        email = email.trim()
                    ),
                    password
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// ============================================================
// 9. HOME FEED (hero header, filter chips, modern cards)
// ============================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    posts: List<LostFoundPost>,
    userName: String,
    myUid: String,
    feedError: String?,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onUserMessageClick: (LostFoundPost) -> Unit,
    onLikeClick: (LostFoundPost) -> Unit,
    onCommentClick: (LostFoundPost) -> Unit,
    onDeletePost: (LostFoundPost) -> Unit,
    onAddPostClick: () -> Unit,
    onMenuClick: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("All") } // All, Lost, Found, Given

    val lostCount = posts.count { it.status.equals("Lost", true) }
    val foundCount = posts.count { it.status.equals("Found", true) }
    val givenCount = posts.count { it.status.equals("Given", true) }

    val filteredPosts = remember(searchQuery, posts, selectedFilter) {
        posts.filter { post ->
            (selectedFilter == "All" || post.status.equals(selectedFilter, ignoreCase = true)) &&
                    (searchQuery.isBlank() ||
                            post.title.contains(searchQuery, ignoreCase = true) ||
                            post.description.contains(searchQuery, ignoreCase = true) ||
                            post.location.contains(searchQuery, ignoreCase = true) ||
                            post.username.contains(searchQuery, ignoreCase = true) ||
                            post.status.contains(searchQuery, ignoreCase = true))
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(id = R.drawable.findoralogo),
                    contentDescription = "App Logo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(42.dp)
                        .shadow(6.dp, CircleShape, spotColor = BrandPinkDeep)
                        .clip(CircleShape)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Findora",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "Hi, ${userName.split(" ").firstOrNull() ?: "there"} 👋",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onRefresh, enabled = !isRefreshing) {
                    if (isRefreshing) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
                IconButton(onClick = onMenuClick) {
                    Icon(Icons.Default.Menu, contentDescription = "Menu")
                }
            }
        },
        floatingActionButton = {
            Box(
                modifier = Modifier
                    .size(58.dp)
                    .shadow(10.dp, CircleShape, spotColor = BrandPinkDeep)
                    .clip(CircleShape)
                    .background(BrandGradient)
                    .clickable(onClick = onAddPostClick),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Post", tint = Color.White, modifier = Modifier.size(28.dp))
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                placeholder = { Text("Search lost & found items...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear Search")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(20.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = Color.Transparent,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterPill("All", Icons.Default.Apps, MaterialTheme.colorScheme.primary, posts.size, selectedFilter) { selectedFilter = "All" }
                FilterPill("Lost", Icons.Default.SearchOff, statusColor("Lost"), lostCount, selectedFilter) { selectedFilter = "Lost" }
                FilterPill("Found", Icons.Default.CheckCircle, statusColor("Found"), foundCount, selectedFilter) { selectedFilter = "Found" }
                FilterPill("Given", Icons.Default.TaskAlt, statusColor("Given"), givenCount, selectedFilter) { selectedFilter = "Given" }
            }

            feedError?.let { ErrorBanner(it) }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (filteredPosts.isEmpty()) {
                    item {
                        EmptyState(
                            icon = if (searchQuery.isBlank()) Icons.Default.AutoAwesome else Icons.Default.Search,
                            title = if (searchQuery.isBlank()) "Nothing here yet" else "No results",
                            subtitle = if (searchQuery.isBlank())
                                "Be the first to post a lost or found item. Tap the + button!"
                            else "Try a different keyword or switch the filter."
                        )
                    }
                } else {
                    items(filteredPosts, key = { it.id }) { post ->
                        PostCard(
                            post = post,
                            myUid = myUid,
                            onMessageUser = onUserMessageClick,
                            onLikeClick = onLikeClick,
                            onCommentClick = onCommentClick,
                            onDeletePost = onDeletePost
                        )
                    }
                }
            }
        }
    }
}

// Version-safe FilterChip colors (works with all M3 versions)
@Composable
fun FilterPill(
    label: String,
    icon: ImageVector,
    color: Color,
    count: Int,
    selected: String,
    onSelect: () -> Unit
) {
    val isSelected = selected == label
    FilterChip(
        selected = isSelected,
        onClick = onSelect,
        leadingIcon = { Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp)) },
        label = {
            Text(
                "$label ($count)",
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            )
        },
        shape = RoundedCornerShape(14.dp),
        colors = FilterChipDefaults.filterChipColors(
            containerColor = MaterialTheme.colorScheme.surface,
            labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
            iconColor = color,
            selectedContainerColor = color,
            selectedLabelColor = Color.White,
            selectedLeadingIconColor = Color.White
        )
    )
}

// ============================================================
// 10. AUTHOR PROFILE HELPERS
// ============================================================

@Composable
fun rememberAuthorProfile(userId: String): UserData? {
    var author by remember(userId) { mutableStateOf<UserData?>(null) }
    LaunchedEffect(userId) {
        if (userId.isBlank()) return@LaunchedEffect
        try {
            val snap = FirebaseFirestore.getInstance()
                .collection("users").document(userId).get().await()
            author = snap.toObject(UserData::class.java)
        } catch (_: Exception) { }
    }
    return author
}

@Composable
fun AuthorInfoChip(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ============================================================
// 11. POST CARD (photo viewer, location pill, floating badge)
// ============================================================

@Composable
fun PostCard(
    post: LostFoundPost,
    myUid: String,
    onMessageUser: (LostFoundPost) -> Unit,
    onLikeClick: (LostFoundPost) -> Unit,
    onCommentClick: (LostFoundPost) -> Unit,
    onDeletePost: (LostFoundPost) -> Unit
) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val likedByMe = post.likes[myUid] == true
    val likeCount = post.likes.size
    val commentCount = post.commentCount.toInt()
    val isMyPost = post.userId == myUid

    val likeScale by animateFloatAsState(
        targetValue = if (likedByMe) 1.2f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "likeScale"
    )

    var showDeleteDialog by remember { mutableStateOf(false) }
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Post", fontWeight = FontWeight.Bold) },
            text = { Text("Delete \"${post.title}\" permanently? This cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = { showDeleteDialog = false; onDeletePost(post) }
                ) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Full-screen photo viewer
    val decodedForViewer = rememberDecodedImage(post.imageData)
    var showImageViewer by remember { mutableStateOf(false) }
    if (showImageViewer && decodedForViewer != null) {
        Dialog(
            onDismissRequest = { showImageViewer = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.94f))
                    .clickable { showImageViewer = false },
                contentAlignment = Alignment.Center
            ) {
                Image(
                    bitmap = decodedForViewer.asImageBitmap(),
                    contentDescription = "Item Photo",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize().padding(16.dp)
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .statusBarsPadding()
                        .padding(16.dp)
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.15f))
                        .clickable { showImageViewer = false },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White, modifier = Modifier.size(20.dp))
                }
            }
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // ---- Author row ----
            Row(verticalAlignment = Alignment.CenterVertically) {
                GradientAvatar(post.username, 42.dp)
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = post.username,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${post.createdAt.timeAgo()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (isMyPost) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.error.copy(alpha = 0.10f))
                            .clickable { showDeleteDialog = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete Post",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                }
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f))
                        .clickable { onMessageUser(post) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Send,
                        contentDescription = "Message ${post.username}",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // ---- Info chips ----
            val author = rememberAuthorProfile(post.userId)
            val sem = author?.semester?.takeIf { it.isNotBlank() } ?: post.authorSemester
            val batch = author?.batch?.takeIf { it.isNotBlank() } ?: post.authorBatch
            val roll = author?.roll?.takeIf { it.isNotBlank() } ?: post.authorRoll
            val studentId = author?.studentId?.takeIf { it.isNotBlank() } ?: post.authorStudentId

            if (sem.isNotBlank() || batch.isNotBlank() || roll.isNotBlank() || studentId.isNotBlank()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.horizontalScroll(rememberScrollState())
                ) {
                    if (sem.isNotBlank()) AuthorInfoChip("Sem $sem")
                    if (batch.isNotBlank()) AuthorInfoChip("Batch $batch")
                    if (roll.isNotBlank()) AuthorInfoChip("Roll $roll")
                    if (studentId.isNotBlank()) AuthorInfoChip("ID $studentId")
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            // ---- Photo with overlays ----
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(190.dp)
                    .clip(RoundedCornerShape(20.dp))
            ) {
                val decodedBitmap = rememberDecodedImage(post.imageData)
                if (decodedBitmap != null) {
                    Image(
                        bitmap = decodedBitmap.asImageBitmap(),
                        contentDescription = "Item Photo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable { showImageViewer = true }
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        BrandPinkDeep.copy(alpha = 0.16f),
                                        BrandPinkLight.copy(alpha = 0.16f)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Image,
                            contentDescription = "No Photo",
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                    }
                }

                // Bottom scrim for the location pill
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .height(56.dp)
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Color.Black.copy(alpha = 0.45f))
                            )
                        )
                )

                // Floating status badge (top-right)
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp)
                        .shadow(4.dp, RoundedCornerShape(12.dp))
                        .clip(RoundedCornerShape(12.dp))
                        .background(statusColor(post.status))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val badgeIcon = when (post.status.lowercase()) {
                            "lost" -> Icons.Default.SearchOff
                            "found" -> Icons.Default.CheckCircle
                            "given" -> Icons.Default.TaskAlt
                            else -> Icons.Default.AutoAwesome
                        }
                        Icon(badgeIcon, contentDescription = null, tint = Color.White, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            post.status,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }

                // Location pill (bottom-left, on the scrim)
                if (post.location.isNotBlank()) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(10.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.Black.copy(alpha = 0.45f))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            post.location,
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ---- Title + description ----
            Text(
                text = post.title,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = post.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // ---- Given banner ----
            if (post.status.equals("Given", ignoreCase = true) && post.claimedByName.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(statusColor("Given").copy(alpha = 0.12f))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.TaskAlt,
                        contentDescription = null,
                        tint = statusColor("Given"),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Given to ${post.claimedByName}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = statusColor("Given")
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ---- Actions ----
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                PostInteraction(
                    icon = if (likedByMe) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    text = if (likeCount > 0) "$likeCount" else "Like",
                    tint = if (likedByMe) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                    iconScale = likeScale,
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onLikeClick(post)
                    }
                )
                PostInteraction(
                    icon = Icons.Default.Comment,
                    text = if (commentCount > 0) "$commentCount" else "Comment",
                    onClick = { onCommentClick(post) }
                )
                PostInteraction(
                    icon = Icons.Default.Share,
                    text = "Share",
                    onClick = {
                        val shareText = buildString {
                            append("${post.status.uppercase()}: ${post.title}\n")
                            append("${post.description}\n")
                            append("📍 ${post.location}\n")
                            if (post.claimedByName.isNotBlank()) {
                                append("✅ Given to ${post.claimedByName}\n")
                            }
                            append("— shared from Findora")
                        }
                        val sendIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, shareText)
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "Share via"))
                    }
                )
            }
        }
    }
}

@Composable
fun PostInteraction(
    icon: ImageVector,
    text: String,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    iconScale: Float = 1f,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = text,
            modifier = Modifier
                .size(21.dp)
                .scale(iconScale),
            tint = tint
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = tint
        )
    }
}

// ============================================================
// 12. MESSAGES SCREEN
// ============================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageScreen(
    viewModel: MessagesViewModel,
    onBackClick: () -> Unit,
    onOpenChat: (Conversation) -> Unit
) {
    val conversations by viewModel.conversations.collectAsState()
    val error by viewModel.error.collectAsState()
    val myUid = viewModel.myUid

    LaunchedEffect(Unit) { viewModel.retryIfNeeded() }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Messages", fontWeight = FontWeight.Black) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            error?.let {
                ErrorBanner(it)
                TextButton(
                    onClick = { viewModel.retry() },
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) { Text("Retry") }
            }
            if (conversations.isEmpty()) {
                EmptyState(
                    icon = Icons.Default.Chat,
                    title = "No conversations yet",
                    subtitle = "Tap the send icon on a post to start a private chat with its owner!"
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(conversations, key = { it.id }) { conversation ->
                        ConversationItem(conversation, myUid) { onOpenChat(conversation) }
                    }
                }
            }
        }
    }
}

@Composable
fun ConversationItem(conversation: Conversation, myUid: String, onClick: () -> Unit) {
    val otherName = conversation.otherUserName(myUid)
    val unread = conversation.unreadFor(myUid)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = if (unread > 0) BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)) else null,
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            GradientAvatar(otherName, 50.dp)
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = otherName,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = conversation.lastMessageTime.timeAgo(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(3.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = conversation.lastMessage.ifBlank { "Say hello! 👋" },
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = if (unread > 0) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (unread > 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (unread > 0) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(BrandGradient),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = unread.toString(),
                                color = Color.White,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

// ============================================================
// 13. CHAT SCREEN (pink gradient bubbles, pulsing presence)
// ============================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(destination: ChatDestination?, viewModel: ChatViewModel, onBackClick: () -> Unit) {
    var messageText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val messages by viewModel.messages.collectAsState()
    val myUid = viewModel.myUid

    val sendError by viewModel.sendError.collectAsState()
    val loadError by viewModel.loadError.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(sendError) {
        sendError?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearError()
        }
    }

    LaunchedEffect(destination?.chatId) {
        destination?.let { viewModel.initChat(it.chatId, it.otherUserId) }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        GradientAvatar(destination?.otherUserName ?: "U", 36.dp, MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(destination?.otherUserName ?: "Chat", fontWeight = FontWeight.Bold)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                PulsingDot(statusColor("Given"))
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    "Active now",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = statusColor("Given")
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back to Messages")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        bottomBar = {
            Surface(tonalElevation = 4.dp, color = MaterialTheme.colorScheme.surface) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = messageText,
                        onValueChange = { messageText = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Message...") },
                        shape = RoundedCornerShape(24.dp),
                        maxLines = 4,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            focusedContainerColor = MaterialTheme.colorScheme.background,
                            unfocusedContainerColor = MaterialTheme.colorScheme.background,
                            cursorColor = MaterialTheme.colorScheme.primary
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    GradientCircleButton(
                        icon = Icons.Default.Send,
                        contentDescription = "Send Message",
                        enabled = messageText.isNotBlank()
                    ) {
                        viewModel.sendMessage(messageText)
                        messageText = ""
                    }
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            state = listState,
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (messages.isEmpty()) {
                item {
                    if (loadError != null) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(top = 48.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            ErrorBanner(loadError ?: "")
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(onClick = { viewModel.retryLoad() }) { Text("Retry") }
                        }
                    } else {
                        EmptyState(
                            icon = Icons.Default.Chat,
                            title = "Say hi! 👋",
                            subtitle = "This is the beginning of your conversation with ${destination?.otherUserName ?: "them"}."
                        )
                    }
                }
            }
            items(messages, key = { it.id }) { msg ->
                ChatBubble(
                    text = msg.text,
                    isSentByMe = msg.senderId == myUid,
                    time = msg.createdAt.toChatTime()
                )
            }
        }
    }
}

@Composable
fun ChatBubble(text: String, isSentByMe: Boolean, time: String) {
    val alignment = if (isSentByMe) Alignment.End else Alignment.Start
    val bubbleShape = if (isSentByMe)
        RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 20.dp, bottomEnd = 6.dp)
    else
        RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 6.dp, bottomEnd = 20.dp)

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        Box(
            modifier = Modifier
                .clip(bubbleShape)
                .then(
                    if (isSentByMe) Modifier.background(BrandGradient)
                    else Modifier.background(MaterialTheme.colorScheme.surfaceVariant)
                )
                .padding(horizontal = 14.dp, vertical = 10.dp)
                .widthIn(max = 280.dp)
        ) {
            Text(
                text = text,
                color = if (isSentByMe) Color.White else MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        Text(
            text = time,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, start = 6.dp, end = 6.dp)
        )
    }
}

// ============================================================
// 14. COMMENTS SCREEN
// ============================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentsScreen(
    post: LostFoundPost?,
    viewModel: CommentsViewModel,
    user: UserData?,
    onBackClick: () -> Unit
) {
    var commentText by remember { mutableStateOf("") }
    val comments by viewModel.comments.collectAsState()
    val error by viewModel.error.collectAsState()
    val listState = rememberLazyListState()

    LaunchedEffect(post?.id) { post?.let { viewModel.initPost(it.id) } }

    LaunchedEffect(comments.size) {
        if (comments.isNotEmpty()) listState.animateScrollToItem(comments.size - 1)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Comments", fontWeight = FontWeight.Black)
                        Text(
                            post?.title ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.primary
                )
            )
        },
        bottomBar = {
            Surface(tonalElevation = 4.dp, color = MaterialTheme.colorScheme.surface) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = commentText,
                        onValueChange = { commentText = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Add a comment...") },
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            focusedContainerColor = MaterialTheme.colorScheme.background,
                            unfocusedContainerColor = MaterialTheme.colorScheme.background,
                            cursorColor = MaterialTheme.colorScheme.primary
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    GradientCircleButton(
                        icon = Icons.Default.Send,
                        contentDescription = "Send Comment",
                        enabled = commentText.isNotBlank()
                    ) {
                        post?.let {
                            viewModel.addComment(it.id, commentText, user)
                            commentText = ""
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            error?.let { ErrorBanner(it) }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = listState,
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (comments.isEmpty()) {
                    item {
                        EmptyState(
                            icon = Icons.Default.Comment,
                            title = "No comments yet",
                            subtitle = "Be the first to share something helpful!"
                        )
                    }
                }
                items(comments, key = { it.id }) { comment ->
                    CommentItem(comment)
                }
            }
        }
    }
}

@Composable
fun CommentItem(comment: Comment) {
    Row(verticalAlignment = Alignment.Top) {
        GradientAvatar(comment.authorName, 36.dp, MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = comment.authorName,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = comment.createdAt.timeAgo(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = comment.text,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

// ============================================================
// 15. CREATE POST SCREEN (pink status cards + photo)
// ============================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePostScreen(
    viewModel: FeedViewModel,
    currentUser: UserData?,
    onBackClick: () -> Unit,
    onPostCreated: () -> Unit
) {
    var status by remember { mutableStateOf("Lost") }
    var title by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var imageBase64 by remember { mutableStateOf("") }
    var isEncodingImage by remember { mutableStateOf(false) }
    val isPosting by viewModel.isPosting.collectAsState()

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val pickImageLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            isEncodingImage = true
            scope.launch {
                when (val result = encodeImageToBase64(context, uri)) {
                    is ImageEncodeResult.Success -> imageBase64 = result.base64
                    is ImageEncodeResult.Failure -> {
                        imageBase64 = ""
                        Toast.makeText(context, result.message, Toast.LENGTH_LONG).show()
                    }
                }
                isEncodingImage = false
            }
        }
    }

    val pickedBitmap = rememberDecodedImage(imageBase64)

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Create Post", fontWeight = FontWeight.Black) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Cancel Post")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ---- Status selector cards ----
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatusSelectorCard("Lost", Icons.Default.SearchOff, statusColor("Lost"), status == "Lost", Modifier.weight(1f)) { status = "Lost" }
                StatusSelectorCard("Found", Icons.Default.CheckCircle, statusColor("Found"), status == "Found", Modifier.weight(1f)) { status = "Found" }
            }

            // ---- Photo picker ----
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(190.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .clickable {
                        if (!isEncodingImage) {
                            try {
                                pickImageLauncher.launch("image/*")
                            } catch (e: Exception) {
                                Toast.makeText(context, "Couldn't open photo picker: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                if (pickedBitmap != null) {
                    Image(
                        bitmap = pickedBitmap.asImageBitmap(),
                        contentDescription = "Selected Photo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(10.dp)
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.55f))
                            .clickable { imageBase64 = "" },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Remove Photo", tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        BrandPinkDeep.copy(alpha = 0.14f),
                                        BrandPinkLight.copy(alpha = 0.14f)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            if (isEncodingImage) {
                                CircularProgressIndicator(modifier = Modifier.size(30.dp), strokeWidth = 2.5.dp, color = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.height(10.dp))
                                Text("Processing photo...", style = MaterialTheme.typography.bodySmall)
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(52.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.AddAPhoto,
                                        contentDescription = "Add Photo",
                                        modifier = Modifier.size(24.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    "Tap to add a photo",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // ---- Fields ----
            OutlinedTextField(
                value = title, onValueChange = { title = it },
                label = { Text("Item Title") },
                singleLine = true,
                shape = RoundedCornerShape(18.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = location, onValueChange = { location = it },
                label = { Text("Location") },
                singleLine = true,
                shape = RoundedCornerShape(18.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = description, onValueChange = { description = it },
                label = { Text("Description") },
                minLines = 4,
                shape = RoundedCornerShape(18.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier.fillMaxWidth()
            )

            GradientButton(
                text = "Publish Post",
                enabled = title.isNotBlank() && description.isNotBlank() && location.isNotBlank(),
                isLoading = isPosting || isEncodingImage
            ) {
                viewModel.createPost(
                    status, title.trim(), description.trim(),
                    location.trim(), imageBase64, currentUser
                ) { success ->
                    if (success) onPostCreated()
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun StatusSelectorCard(
    label: String,
    icon: ImageVector,
    color: Color,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) color.copy(alpha = 0.10f) else MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = if (selected) color else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(26.dp))
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                label,
                fontWeight = FontWeight.Bold,
                color = if (selected) color else MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleSmall
            )
        }
    }
}

// ============================================================
// 16. PROFILE SCREEN (pink animated gradient header + tinted stats)
// ============================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    user: UserData?,
    myPosts: List<LostFoundPost>,
    onMarkGiven: (LostFoundPost, String) -> Unit,
    onDeletePost: (LostFoundPost) -> Unit,
    onMenuClick: () -> Unit,
    onBackClick: () -> Unit
) {
    var postToMark by remember { mutableStateOf<LostFoundPost?>(null) }
    var claimName by remember { mutableStateOf("") }
    var postToDelete by remember { mutableStateOf<LostFoundPost?>(null) }

    val lostCount = myPosts.count { it.status.equals("Lost", true) }
    val foundCount = myPosts.count { it.status.equals("Found", true) }
    val givenCount = myPosts.count { it.status.equals("Given", true) }

    // "Mark as Given" popup
    postToMark?.let { post ->
        AlertDialog(
            onDismissRequest = { postToMark = null; claimName = "" },
            title = { Text("Mark as Given", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Who was the item given to? Their name will be shown on the post.")
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = claimName,
                        onValueChange = { claimName = it },
                        label = { Text("Person's name") },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onMarkGiven(post, claimName.trim())
                        postToMark = null
                        claimName = ""
                    },
                    enabled = claimName.isNotBlank()
                ) { Text("Confirm") }
            },
            dismissButton = {
                TextButton(onClick = { postToMark = null; claimName = "" }) { Text("Cancel") }
            }
        )
    }

    // Delete popup
    postToDelete?.let { post ->
        AlertDialog(
            onDismissRequest = { postToDelete = null },
            title = { Text("Delete Post", fontWeight = FontWeight.Bold) },
            text = { Text("Delete \"${post.title}\" permanently? This cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeletePost(post)
                        postToDelete = null
                    }
                ) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { postToDelete = null }) { Text("Cancel") }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
    ) {
        // ---- Animated gradient header ----
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(animatedBrandGradient(vertical = true))
        ) {
            Row(
                modifier = Modifier
                    .statusBarsPadding()
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back to Home", tint = Color.White)
                }
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = onMenuClick) {
                    Icon(Icons.Default.Menu, contentDescription = "Menu", tint = Color.White)
                }
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 64.dp, bottom = 56.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .shadow(16.dp, CircleShape, spotColor = Color.Black.copy(alpha = 0.3f))
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.25f))
                        .padding(5.dp)
                ) {
                    GradientAvatar(user?.name ?: "U", 86.dp, MaterialTheme.typography.headlineMedium)
                }
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = user?.name ?: "Guest User",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = user?.email ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.85f)
                )
            }
        }

        Column(modifier = Modifier.padding(20.dp)) {
            // ---- Tinted stats row ----
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard(lostCount, "Lost", statusColor("Lost"), Modifier.weight(1f))
                StatCard(foundCount, "Found", statusColor("Found"), Modifier.weight(1f))
                StatCard(givenCount, "Given", statusColor("Given"), Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ---- Student info card ----
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    ProfileRow(icon = Icons.Default.Badge, label = "Student ID", value = user?.studentId)
                    ProfileRow(icon = Icons.Default.Numbers, label = "Roll Number", value = user?.roll)
                    ProfileRow(icon = Icons.Default.DateRange, label = "Semester", value = user?.semester)
                    ProfileRow(icon = Icons.Default.School, label = "Batch", value = user?.batch)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "My Posts",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "(${myPosts.size})",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(12.dp))

            if (myPosts.isEmpty()) {
                EmptyState(
                    icon = Icons.Default.AutoAwesome,
                    title = "No posts yet",
                    subtitle = "Tap + on the Home screen to create your first post!"
                )
            } else {
                myPosts.forEach { post ->
                    MyPostItem(
                        post = post,
                        onMarkGivenClick = {
                            postToMark = post
                            claimName = ""
                        },
                        onDeleteClick = { postToDelete = post }
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun StatCard(count: Int, label: String, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.08f)),
        border = BorderStroke(1.dp, color.copy(alpha = 0.25f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                color = color
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// Lost → Delete | Found → Mark as Given | Given → no buttons
@Composable
fun MyPostItem(
    post: LostFoundPost,
    onMarkGivenClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val status = post.status.lowercase()
    val badgeColor = statusColor(post.status)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatusBadge(post.status, fontSize = 10)
            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = post.title,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${post.createdAt.timeAgo()} • ${post.location}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (status == "given" && post.claimedByName.isNotBlank()) {
                    Text(
                        text = "✅ Given to ${post.claimedByName}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = statusColor("Given")
                    )
                }
            }

            when (status) {
                "lost" -> {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.error.copy(alpha = 0.10f))
                            .clickable { onDeleteClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete Post",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                "found" -> {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(statusColor("Given"))
                            .clickable { onMarkGivenClick() }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            "Mark as Given",
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                // "given" → nothing
            }
        }
    }
}

@Composable
fun ProfileRow(icon: ImageVector, label: String, value: String?) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = label, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(17.dp))
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column {
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                value?.ifBlank { "—" } ?: "—",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}