package com.iktiarnazib.lostandfoundv2

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Locale

// ============================================================
// 1. DATA MODELS & HELPERS
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
    val status: String = "",       // "Lost" or "Found"
    val title: String = "",
    val description: String = "",
    val location: String = "",
    val authorStudentId: String = "",
    val authorRoll: String = "",
    val authorSemester: String = "",
    val authorBatch: String = "",
    val claimedByName: String = "",
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
// 2. REPOSITORIES (Firebase logic)
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

    suspend fun markPostFound(postId: String, claimedByName: String): Result<Unit> = try {
        db.collection("posts").document(postId).update(
            mapOf(
                "status" to "Found",
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

    // Deterministic chat ID so both users always share ONE conversation
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

    // No server-side orderBy — sorted client-side (a server orderBy would
    // silently exclude any message missing 'createdAt')
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
// 3. VIEWMODELS
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

    fun createPost(status: String, title: String, description: String, location: String, author: UserData?) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        viewModelScope.launch {
            _isPosting.value = true
            postRepo.createPost(
                LostFoundPost(
                    userId = uid,
                    username = author?.name ?: "Anonymous",
                    authorStudentId = author?.studentId ?: "",
                    authorRoll = author?.roll ?: "",
                    authorSemester = author?.semester ?: "",
                    authorBatch = author?.batch ?: "",
                    status = status, title = title,
                    description = description, location = location
                )
            ).onFailure { _feedError.value = "Could not save post: ${it.message}" }
            _isPosting.value = false
        }
    }

    fun markPostFound(post: LostFoundPost, claimedByName: String) {
        if (claimedByName.isBlank()) return
        viewModelScope.launch {
            postRepo.markPostFound(post.id, claimedByName)
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

// CHANGED: self-healing message listener + persistent load error (not just a toast)
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
        // Re-attach if the previous attach failed; otherwise the live
        // listener is still running for this same chat — keep it.
        if (currentChatId == chatId && !lastAttachFailed) return
        currentChatId = chatId
        otherUserId = otherUid
        // Never let another conversation's history show here
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

    // Re-attach a dead/denied listener (button shown in the chat UI)
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
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigator()
                }
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

    Box(modifier = Modifier.fillMaxSize()) {
        if (isCheckingAuth) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            val user = currentUser
            if (user == null) {
                when (currentScreen) {
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
            } else {
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

                when (currentScreen) {
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
                        onMarkFound = { post, claimName ->
                            feedViewModel.markPostFound(post, claimName)
                        },
                        onMenuClick = { isDrawerOpen = true },
                        onBackClick = { currentScreen = Screen.Home }
                    )
                    else -> HomeScreen(
                        posts = posts,
                        myUid = user.uid,
                        unreadCount = totalUnread,
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
                        onMessageClick = { currentScreen = Screen.Messages },
                        onAddPostClick = { currentScreen = Screen.CreatePost },
                        onMenuClick = { isDrawerOpen = true }
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = isDrawerOpen,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.6f))
                    .clickable { isDrawerOpen = false }
            )
        }

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

// ============================================================
// 5. DRAWER
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
            .width(300.dp),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp)
    ) {
        Column(modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(id = R.drawable.findoralogo),
                    contentDescription = "App Logo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = user?.name ?: "Guest User",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = user?.email ?: "Not logged in",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            DrawerItem(icon = Icons.Default.Home, text = "Home", onClick = onHomeClick)
            DrawerItem(icon = Icons.Default.Person, text = "Profile", onClick = onProfileClick)

            Spacer(modifier = Modifier.weight(1f))

            DrawerItem(icon = Icons.Default.Logout, text = "Logout", onClick = onLogoutClick)
        }
    }
}

@Composable
fun DrawerItem(icon: ImageVector, text: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = text,
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = text,
            fontWeight = FontWeight.Medium,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

// ============================================================
// 6. LOGIN SCREEN
// ============================================================

@Composable
fun LoginScreen(authViewModel: AuthViewModel, onSignUpClick: () -> Unit, onForgotPasswordClick: () -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    val isLoading by authViewModel.isLoading.collectAsState()
    val error by authViewModel.authError.collectAsState()

    LaunchedEffect(Unit) { authViewModel.clearMessages() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(id = R.drawable.findoralogo),
            contentDescription = "App Logo",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
        )

        Text(
            text = "Findora",
            style = MaterialTheme.typography.displaySmall.copy(
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            ),
            modifier = Modifier.padding(top = 16.dp)
        )

        Text(
            text = "Welcome back! Please login.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, bottom = 32.dp)
        )

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
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
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        )

        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.CenterEnd
        ) {
            TextButton(onClick = onForgotPasswordClick) {
                Text("Forgot Password?", color = MaterialTheme.colorScheme.secondary)
            }
        }

        error?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = { authViewModel.login(email, password) },
            enabled = !isLoading && email.isNotBlank() && password.isNotBlank(),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text(text = "Login", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Don't have an account?", style = MaterialTheme.typography.bodyMedium)
            TextButton(onClick = onSignUpClick) {
                Text("Sign Up", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
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
        topBar = {
            TopAppBar(
                title = { Text("Forgot Password", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back to Login")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(48.dp))

            Image(
                painter = painterResource(id = R.drawable.findoralogo),
                contentDescription = "App Logo",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
            )

            Text(
                text = "Reset Password",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.padding(top = 24.dp, bottom = 8.dp)
            )

            Text(
                text = "Enter your email address below and we'll send you a link to reset your password.",
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
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            )

            error?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { authViewModel.sendPasswordReset(email) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                enabled = email.isNotBlank() && !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(text = "Send Reset Email", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            }
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
        topBar = {
            TopAppBar(
                title = { Text("Create Account", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back to Login")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = name, onValueChange = { name = it },
                label = { Text("Full Name") },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                singleLine = true, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = id, onValueChange = { id = it },
                label = { Text("Student ID") },
                leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null) },
                singleLine = true, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = roll, onValueChange = { roll = it },
                label = { Text("Roll Number") },
                leadingIcon = { Icon(Icons.Default.Numbers, contentDescription = null) },
                singleLine = true, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = semester, onValueChange = { semester = it },
                label = { Text("Semester") },
                leadingIcon = { Icon(Icons.Default.DateRange, contentDescription = null) },
                singleLine = true, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = batch, onValueChange = { batch = it },
                label = { Text("Batch") },
                leadingIcon = { Icon(Icons.Default.School, contentDescription = null) },
                singleLine = true, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = email, onValueChange = { email = it },
                label = { Text("Email") },
                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                singleLine = true, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()
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
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = confirmPassword, onValueChange = { confirmPassword = it },
                label = { Text("Confirm Password") },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                singleLine = true, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth(),
                isError = confirmPassword.isNotBlank() && password != confirmPassword
            )

            error?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
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
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                enabled = isFormValid && !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(text = "Sign Up", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ============================================================
// 9. HOME FEED SCREEN
// ============================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    posts: List<LostFoundPost>,
    myUid: String,
    unreadCount: Int,
    feedError: String?,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onUserMessageClick: (LostFoundPost) -> Unit,
    onLikeClick: (LostFoundPost) -> Unit,
    onCommentClick: (LostFoundPost) -> Unit,
    onMessageClick: () -> Unit,
    onAddPostClick: () -> Unit,
    onMenuClick: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedTabIndex by remember { mutableStateOf(0) }

    val filteredPosts = remember(searchQuery, posts, selectedTabIndex) {
        val statusFilter = when (selectedTabIndex) {
            1 -> "Lost"
            2 -> "Found"
            else -> null
        }
        posts.filter { post ->
            (statusFilter == null || post.status.equals(statusFilter, ignoreCase = true)) &&
                    (searchQuery.isBlank() ||
                            post.title.contains(searchQuery, ignoreCase = true) ||
                            post.description.contains(searchQuery, ignoreCase = true) ||
                            post.location.contains(searchQuery, ignoreCase = true) ||
                            post.username.contains(searchQuery, ignoreCase = true) ||
                            post.status.contains(searchQuery, ignoreCase = true))
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = R.drawable.findoralogo),
                            contentDescription = "App Logo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Findora Feed", fontWeight = FontWeight.Bold)
                    }
                },
                actions = {
                    IconButton(onClick = onRefresh, enabled = !isRefreshing) {
                        if (isRefreshing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                        }
                    }
                    IconButton(onClick = onMessageClick) {
                        BadgedBox(badge = {
                            if (unreadCount > 0) Badge { Text(unreadCount.toString()) }
                        }) {
                            Icon(imageVector = Icons.Default.Email, contentDescription = "Messages")
                        }
                    }
                    IconButton(onClick = onMenuClick) {
                        Icon(imageVector = Icons.Default.Menu, contentDescription = "Menu")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.primary
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddPostClick,
                shape = CircleShape,
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Post")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
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
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                )
            )

            TabRow(
                selectedTabIndex = selectedTabIndex,
                modifier = Modifier.fillMaxWidth(),
                containerColor = MaterialTheme.colorScheme.background,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    text = { Text("All", fontWeight = if (selectedTabIndex == 0) FontWeight.Bold else FontWeight.Normal) }
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    text = { Text("Lost", fontWeight = if (selectedTabIndex == 1) FontWeight.Bold else FontWeight.Normal) }
                )
                Tab(
                    selected = selectedTabIndex == 2,
                    onClick = { selectedTabIndex = 2 },
                    text = { Text("Found", fontWeight = if (selectedTabIndex == 2) FontWeight.Bold else FontWeight.Normal) }
                )
            }

            feedError?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            Box(modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (filteredPosts.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (searchQuery.isBlank()) "No posts here yet.\nTap + to create one!"
                                    else "No items found matching '$searchQuery'",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else {
                        items(filteredPosts, key = { it.id }) { post ->
                            PostCard(
                                post = post,
                                myUid = myUid,
                                onMessageUser = onUserMessageClick,
                                onLikeClick = onLikeClick,
                                onCommentClick = onCommentClick
                            )
                        }
                    }
                }
            }
        }
    }
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
            .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f))
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}

// ============================================================
// 11. POST CARD
// ============================================================

@Composable
fun PostCard(
    post: LostFoundPost,
    myUid: String,
    onMessageUser: (LostFoundPost) -> Unit,
    onLikeClick: (LostFoundPost) -> Unit,
    onCommentClick: (LostFoundPost) -> Unit
) {
    val context = LocalContext.current
    val likedByMe = post.likes[myUid] == true
    val likeCount = post.likes.size
    val commentCount = post.commentCount.toInt()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = post.username.take(1),
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = post.username,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(3.dp))

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
                        Spacer(modifier = Modifier.height(3.dp))
                    }

                    Text(
                        text = "${post.createdAt.timeAgo()} • ${post.location}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(onClick = { onMessageUser(post) }, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Message ${post.username}",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                val badgeColor = if (post.status == "Lost") MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(badgeColor.copy(alpha = 0.1f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = post.status,
                        color = badgeColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primaryContainer,
                                MaterialTheme.colorScheme.secondaryContainer
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Image,
                    contentDescription = "Item Image",
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.5f)
                )
            }

            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = post.title,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = post.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (post.status.equals("Found", ignoreCase = true) && post.claimedByName.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Found — given to ${post.claimedByName}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                PostInteraction(
                    icon = if (likedByMe) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    text = if (likeCount > 0) "Like • $likeCount" else "Like",
                    tint = if (likedByMe) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                    onClick = { onLikeClick(post) }
                )
                PostInteraction(
                    icon = Icons.Default.Comment,
                    text = if (commentCount > 0) "Comment • $commentCount" else "Comment",
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
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = text,
            modifier = Modifier.size(20.dp),
            tint = tint
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
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
        topBar = {
            TopAppBar(
                title = { Text("Messages", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back to Home")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            error?.let {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                    TextButton(onClick = { viewModel.retry() }) {
                        Text("Retry")
                    }
                }
            }
            if (conversations.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "No conversations yet.\nTap the send icon on a post to message its owner!",
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
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

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = otherName.take(1),
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

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
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = conversation.lastMessage.ifBlank { "Say hello!" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )

                if (unread > 0) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = unread.toString(),
                            color = MaterialTheme.colorScheme.onPrimary,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }

    HorizontalDivider(
        modifier = Modifier.padding(start = 80.dp, end = 16.dp),
        thickness = 0.5.dp,
        color = MaterialTheme.colorScheme.surfaceVariant
    )
}

// ============================================================
// 13. CHAT SCREEN (with visible load error + retry)
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
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = destination?.otherUserName?.take(1) ?: "U",
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(destination?.otherUserName ?: "Chat", fontWeight = FontWeight.Bold)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back to Messages")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.primary
                )
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 4.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = messageText,
                        onValueChange = { messageText = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Message...") },
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            cursorColor = MaterialTheme.colorScheme.primary
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            if (messageText.isNotBlank()) {
                                viewModel.sendMessage(messageText)
                                messageText = ""
                            }
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "Send Message",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background),
            state = listState,
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (messages.isEmpty()) {
                item {
                    // Load error — persistent, with a Retry button (no more silent emptiness)
                    if (loadError != null) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 48.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = loadError ?: "",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(onClick = { viewModel.retryLoad() }) {
                                Text("Retry")
                            }
                        }
                    } else {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .padding(top = 64.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "No messages yet. Say hi!",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
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
    val bubbleColor = if (isSentByMe) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
    val textColor = if (isSentByMe) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        Box(
            modifier = Modifier
                .clip(
                    RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isSentByMe) 16.dp else 4.dp,
                        bottomEnd = if (isSentByMe) 4.dp else 16.dp
                    )
                )
                .background(bubbleColor)
                .padding(12.dp)
                .widthIn(max = 280.dp)
        ) {
            Text(
                text = text,
                color = textColor,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        Text(
            text = time,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, start = 4.dp, end = 4.dp)
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
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Comments", fontWeight = FontWeight.Bold)
                        Text(
                            post?.title ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.primary
                )
            )
        },
        bottomBar = {
            Surface(tonalElevation = 4.dp, color = MaterialTheme.colorScheme.surface) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
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
                            cursorColor = MaterialTheme.colorScheme.primary
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            post?.let {
                                viewModel.addComment(it.id, commentText, user)
                                commentText = ""
                            }
                        },
                        enabled = commentText.isNotBlank(),
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "Send Comment",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            error?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = listState,
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (comments.isEmpty()) {
                    item {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .padding(top = 48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "No comments yet.\nBe the first to help!",
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
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
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = comment.authorName.take(1),
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                fontWeight = FontWeight.Bold
            )
        }
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
// 15. CREATE POST SCREEN
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
    val isPosting by viewModel.isPosting.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create Post", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Cancel Post")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primaryContainer,
                                MaterialTheme.colorScheme.secondaryContainer
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Image,
                    contentDescription = "Item Image",
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.5f)
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FilterChip(
                    selected = status == "Lost",
                    onClick = { status = "Lost" },
                    label = { Text("Lost") }
                )
                FilterChip(
                    selected = status == "Found",
                    onClick = { status = "Found" },
                    label = { Text("Found") }
                )
            }

            OutlinedTextField(
                value = title, onValueChange = { title = it },
                label = { Text("Item Title") },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = location, onValueChange = { location = it },
                label = { Text("Location") },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = description, onValueChange = { description = it },
                label = { Text("Description") },
                minLines = 4,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = {
                    viewModel.createPost(
                        status, title.trim(), description.trim(),
                        location.trim(), currentUser
                    )
                    onPostCreated()
                },
                enabled = !isPosting && title.isNotBlank() && description.isNotBlank() && location.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                if (isPosting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Post", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ============================================================
// 16. PROFILE SCREEN (info + My Posts + Mark as Found)
// ============================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    user: UserData?,
    myPosts: List<LostFoundPost>,
    onMarkFound: (LostFoundPost, String) -> Unit,
    onMenuClick: () -> Unit,
    onBackClick: () -> Unit
) {
    var postToMark by remember { mutableStateOf<LostFoundPost?>(null) }
    var claimName by remember { mutableStateOf("") }

    postToMark?.let { post ->
        AlertDialog(
            onDismissRequest = { postToMark = null; claimName = "" },
            title = { Text("Mark as Found", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Who was the item given to (or who found it)? Their name will be shown on the post.")
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
                        onMarkFound(post, claimName.trim())
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Profile", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back to Home")
                    }
                },
                actions = {
                    IconButton(onClick = onMenuClick) {
                        Icon(imageVector = Icons.Default.Menu, contentDescription = "Menu")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .align(Alignment.CenterHorizontally),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = (user?.name ?: "U").take(1).uppercase(),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = user?.name ?: "Guest User",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            Text(
                text = user?.email ?: "",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    ProfileRow(icon = Icons.Default.Badge, label = "Student ID", value = user?.studentId)
                    ProfileRow(icon = Icons.Default.Numbers, label = "Roll Number", value = user?.roll)
                    ProfileRow(icon = Icons.Default.DateRange, label = "Semester", value = user?.semester)
                    ProfileRow(icon = Icons.Default.School, label = "Batch", value = user?.batch)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "My Posts (${myPosts.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))

            if (myPosts.isEmpty()) {
                Text(
                    text = "You haven't created any posts yet.\nTap + on the Home screen to create one!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp)
                )
            } else {
                myPosts.forEach { post ->
                    MyPostItem(
                        post = post,
                        onMarkFoundClick = {
                            postToMark = post
                            claimName = ""
                        }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
fun MyPostItem(post: LostFoundPost, onMarkFoundClick: () -> Unit) {
    val isLost = post.status.equals("Lost", ignoreCase = true)
    val badgeColor = if (isLost) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(badgeColor.copy(alpha = 0.12f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = post.status,
                    color = badgeColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
            }

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
                if (!isLost && post.claimedByName.isNotBlank()) {
                    Text(
                        text = "✅ Given to ${post.claimedByName}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            }

            if (isLost) {
                Button(
                    onClick = onMarkFoundClick,
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.tertiary
                    )
                ) {
                    Text("Mark as Found", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
fun ProfileRow(icon: ImageVector, label: String, value: String?) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            icon,
            contentDescription = label,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                value?.ifBlank { "—" } ?: "—",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
        }
    }
}