package com.example.notiapp

import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import okhttp3.FormBody
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

class ChatbotActivity : AppCompatActivity() {

    private val TAG = "ChatbotActivity"

    // UI 요소들
    private lateinit var chatTitleText: TextView
    private lateinit var backButton: ImageButton

    // 1. 파일 목록 화면
    private lateinit var fileListLayout: LinearLayout
    private lateinit var chatFileCountText: TextView
    private lateinit var chatFileRecyclerView: RecyclerView
    private lateinit var chatEmptyStateLayout: LinearLayout
    private lateinit var chatFileAdapter: ChatFileAdapter

    // 2. 세션 준비 화면
    private lateinit var sessionSetupLayout: LinearLayout
    private lateinit var selectedFileNameText: TextView
    private lateinit var setupStatusText: TextView
    private lateinit var createChatbotButton: Button
    private lateinit var sessionLoadingProgress: ProgressBar
    private lateinit var loadingStatusText: TextView

    // 3. 채팅 화면
    private lateinit var chatLayout: LinearLayout
    private lateinit var currentChatFileNameText: TextView
    private lateinit var chatRecyclerView: RecyclerView
    private lateinit var messageEditText: EditText
    private lateinit var sendButton: ImageButton
    private lateinit var chatAdapter: ChatAdapter

    // 데이터
    private var allRecordings = mutableListOf<RecordingItem>()
    private var currentSelectedFile: RecordingItem? = null
    private var currentSessionId: Long? = null
    private var chatMessages = mutableListOf<ChatMessage>()

    // 상태 관리
    private enum class ScreenState {
        FILE_LIST,      // 파일 목록
        SESSION_SETUP,  // 세션 준비
        CHATTING       // 채팅 중
    }
    private var currentState = ScreenState.FILE_LIST
    private lateinit var sessionManager: ChatSessionManager

    // HTTP 클라이언트
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_chatbot)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // UI 요소 초기화
        initializeViews()
        sessionManager = ChatSessionManager(this)

        // 어댑터 설정
        setupAdapters()

        // 버튼 리스너 설정
        setupButtonListeners()

        // 하단 네비게이션 설정
        setupBottomNavigation()

        // 파일 목록 로드
        loadRecordingFiles() // Removed the undefined 'file' parameter
    }

    private fun initializeViews() {
        // 공통 UI
        chatTitleText = findViewById(R.id.chatTitleText)
        backButton = findViewById(R.id.backButton)

        // 1. 파일 목록 화면
        fileListLayout = findViewById(R.id.fileListLayout)
        chatFileCountText = findViewById(R.id.chatFileCountText)
        chatFileRecyclerView = findViewById(R.id.chatFileRecyclerView)
        chatEmptyStateLayout = findViewById(R.id.chatEmptyStateLayout)

        // 2. 세션 준비 화면
        sessionSetupLayout = findViewById(R.id.sessionSetupLayout)
        selectedFileNameText = findViewById(R.id.selectedFileNameText)
        setupStatusText = findViewById(R.id.setupStatusText)
        createChatbotButton = findViewById(R.id.createChatbotButton)
        sessionLoadingProgress = findViewById(R.id.sessionLoadingProgress)
        loadingStatusText = findViewById(R.id.loadingStatusText)

        // 3. 채팅 화면
        chatLayout = findViewById(R.id.chatLayout)
        currentChatFileNameText = findViewById(R.id.currentChatFileNameText)
        chatRecyclerView = findViewById(R.id.chatRecyclerView)
        messageEditText = findViewById(R.id.messageEditText)
        sendButton = findViewById(R.id.sendButton)
    }

    private fun setupAdapters() {
        // 파일 목록 어댑터
        chatFileAdapter = ChatFileAdapter(allRecordings, { recordingItem ->
            onFileSelected(recordingItem)
        }, sessionManager) // sessionManager 전달

        chatFileRecyclerView.layoutManager = LinearLayoutManager(this)
        chatFileRecyclerView.adapter = chatFileAdapter

        // 채팅 어댑터
        chatAdapter = ChatAdapter(chatMessages)
        chatRecyclerView.layoutManager = LinearLayoutManager(this)
        chatRecyclerView.adapter = chatAdapter
    }

    private fun setupButtonListeners() {
        // 뒤로가기 버튼
        backButton.setOnClickListener {
            when (currentState) {
                ScreenState.SESSION_SETUP -> {
                    // 세션 준비 화면에서 파일 목록으로
                    showFileListScreen()
                }
                ScreenState.CHATTING -> {
                    // 채팅 화면에서 파일 목록으로
                    showFileListScreen()
                }
                else -> {
                    // 파일 목록에서는 뒤로가기 없음 (handled by onBackPressed)
                }
            }
        }

        // 챗봇 생성 버튼
        createChatbotButton.setOnClickListener {
            currentSelectedFile?.let { file ->
                createChatbotSession(file)
            }
        }

        // 메시지 전송 버튼
        sendButton.setOnClickListener {
            sendMessage()
        }

        // 엔터키로 메시지 전송
        messageEditText.setOnEditorActionListener { _, _, _ ->
            sendMessage()
            true
        }
    }

    private fun setupBottomNavigation() {
        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        bottomNavigation.selectedItemId = R.id.navigation_chatbot

        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> {
                    val intent = Intent(this, DashBoardActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                    startActivity(intent)
                    true
                }
                R.id.navigation_user -> {
                    val intent = Intent(this, UserInfoActivity::class.java)
                    startActivity(intent)
                    true
                }
                R.id.navigation_chatbot -> {
                    // 이미 챗봇 화면
                    true
                }
                else -> false
            }
        }
    }

    // ========== 화면 전환 메서드들 ==========

    private fun showFileListScreen() {
        currentState = ScreenState.FILE_LIST

        fileListLayout.visibility = View.VISIBLE
        sessionSetupLayout.visibility = View.GONE
        chatLayout.visibility = View.GONE

        backButton.visibility = View.GONE
        chatTitleText.text = "AI 비서"

        Log.d(TAG, "파일 목록 화면 표시")
    }

    private fun showSessionSetupScreen(recordingItem: RecordingItem) {
        currentState = ScreenState.SESSION_SETUP
        currentSelectedFile = recordingItem

        fileListLayout.visibility = View.GONE
        sessionSetupLayout.visibility = View.VISIBLE
        chatLayout.visibility = View.GONE

        backButton.visibility = View.VISIBLE
        chatTitleText.text = "AI 비서 생성"
        selectedFileNameText.text = recordingItem.filename

        // 로딩 상태 초기화
        createChatbotButton.visibility = View.VISIBLE
        sessionLoadingProgress.visibility = View.GONE
        loadingStatusText.visibility = View.GONE

        Log.d(TAG, "세션 준비 화면 표시: ${recordingItem.filename}")
    }

    private fun showChatScreen(recordingItem: RecordingItem, sessionId: Long) {
        currentState = ScreenState.CHATTING
        currentSelectedFile = recordingItem
        currentSessionId = sessionId

        fileListLayout.visibility = View.GONE
        sessionSetupLayout.visibility = View.GONE
        chatLayout.visibility = View.VISIBLE

        backButton.visibility = View.VISIBLE
        chatTitleText.text = "AI 비서와 대화"
        currentChatFileNameText.text = recordingItem.filename

        Log.d(TAG, "채팅 화면 표시: ${recordingItem.filename}")
    }

    // ========== 데이터 로딩 메서드들 ==========

    private fun loadRecordingFiles() { // No parameter needed here
        val token = getJwtToken()
        if (token.isEmpty()) {
            Log.w(TAG, "JWT 토큰이 없어 파일을 가져올 수 없습니다")
            updateFileListUI(emptyList())
            return
        }

        thread {
            try {
                // TODO: API만들어주면 수정하기 - 파일 목록 조회 API
                val requestBody = FormBody.Builder().build()
                val request = Request.Builder()
                    .url("http://10.0.2.2:8080/file/get/file-information")
                    .post(requestBody)
                    .header("Authorization", "Bearer $token")
                    .build()

                client.newCall(request).execute().use { response ->
                    val responseBody = response.body?.string()
                    Log.d(TAG, "파일 목록 응답: ${response.code}")

                    if (response.isSuccessful && responseBody != null) {
                        val recordings = parseServerFileResponse(responseBody)
                        runOnUiThread {
                            updateFileListUI(recordings)
                        }
                    } else {
                        runOnUiThread {
                            updateFileListUI(emptyList())
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "파일 목록 로드 오류: ${e.message}", e)
                runOnUiThread {
                    updateFileListUI(emptyList())
                }
            }
        }
    }

    private fun parseServerFileResponse(responseBody: String): List<RecordingItem> {
        val recordings = mutableListOf<RecordingItem>()

        try {
            val jsonArray = JSONArray(responseBody)

            for (i in 0 until jsonArray.length()) {
                val jsonObject = jsonArray.getJSONObject(i)

                val originalName = jsonObject.getString("originalName")
                val savedName = jsonObject.getString("savedName")
                val uploadDate = jsonObject.getString("uploadDate")
                val duration = jsonObject.getInt("duration")
                val fileSize = jsonObject.getLong("fileSize")

                // 날짜 포맷팅
                val displayDate = try {
                    val serverDate = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS", Locale.getDefault())
                        .parse(uploadDate)
                    SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(serverDate ?: Date())
                } catch (e: Exception) {
                    uploadDate.split("T")[0]
                }

                val formattedDuration = String.format("%02d:%02d", duration / 60, duration % 60)

                recordings.add(
                    RecordingItem(
                        file = null,
                        filename = originalName,
                        date = displayDate,
                        duration = formattedDuration,
                        filePath = "",
                        isServerFile = true,
                        savedFileName = savedName,
                        fileSize = fileSize,
                        uploadDate = uploadDate,
                        isDownloaded = false
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "파일 목록 파싱 오류: ${e.message}", e)
        }

        return recordings
    }

    private fun updateFileListUI(recordings: List<RecordingItem>) {
        allRecordings.clear()
        allRecordings.addAll(recordings)

        chatFileAdapter.updateRecordings(recordings)

        // 파일 개수 업데이트
        chatFileCountText.text = if (recordings.isEmpty()) {
            "파일 없음"
        } else {
            "총 ${recordings.size}개 파일"
        }

        // 빈 상태 처리
        if (recordings.isEmpty()) {
            chatFileRecyclerView.visibility = View.GONE
            chatEmptyStateLayout.visibility = View.VISIBLE
        } else {
            chatFileRecyclerView.visibility = View.VISIBLE
            chatEmptyStateLayout.visibility = View.GONE
        }
    }

    // ========== 파일 선택 및 세션 관리 ==========

    private fun onFileSelected(recordingItem: RecordingItem) {
        Log.d(TAG, "파일 선택됨: ${recordingItem.filename}")

        // 기존 세션이 있는지 확인
        val existingSessionId = sessionManager.getSessionId(recordingItem.savedFileName)

        if (existingSessionId != null) {
            Log.d(TAG, "기존 세션 발견: $existingSessionId")
            // 바로 채팅 화면으로 이동
            loadChatHistory(existingSessionId) {
                showChatScreen(recordingItem, existingSessionId)
            }
        } else {
            Log.d(TAG, "새로운 세션 필요")
            // 세션 준비 화면으로 이동
            showSessionSetupScreen(recordingItem)
        }
    }

    // This method is unused and can be removed if not required
    // private fun checkExistingChatSession(recordingItem: RecordingItem, callback: (Boolean, Long?) -> Unit) {
    //     val token = getJwtToken()
    //     if (token.isEmpty()) {
    //         callback(false, null)
    //         return
    //     }
    //
    //     thread {
    //         try {
    //             // TODO: API만들어주면 수정하기 - 기존 세션 확인 API
    //             val request = Request.Builder()
    //                 .url("http://10.0.2.2:8080/chatbot/check-session?fileName=${recordingItem.savedFileName}")
    //                 .header("Authorization", "Bearer $token")
    //                 .build()
    //
    //             client.newCall(request).execute().use { response ->
    //                 val responseBody = response.body?.string()
    //
    //                 if (response.isSuccessful && responseBody != null) {
    //                     try {
    //                         val jsonResponse = JSONObject(responseBody)
    //                         val hasSession = jsonResponse.getBoolean("hasSession")
    //                         val sessionId = if (hasSession) jsonResponse.getLong("sessionId") else null
    //
    //                         callback(hasSession, sessionId)
    //                     } catch (e: Exception) {
    //                         Log.e(TAG, "세션 확인 응답 파싱 오류: ${e.message}", e)
    //                         callback(false, null)
    //                     }
    //                 } else {
    //                     callback(false, null)
    //                 }
    //             }
    //         } catch (e: Exception) {
    //             Log.e(TAG, "세션 확인 오류: ${e.message}", e)
    //             callback(false, null)
    //         }
    //     }
    // }

    private fun createChatbotSession(recordingItem: RecordingItem) {
        // 로딩 상태 표시
        createChatbotButton.visibility = View.GONE
        sessionLoadingProgress.visibility = View.VISIBLE
        loadingStatusText.visibility = View.VISIBLE
        loadingStatusText.text = "녹음 내용을 분석하고 있습니다..."

        val token = getJwtToken()
        if (token.isEmpty()) {
            showSessionError("로그인이 필요합니다.")
            return
        }

        thread {
            try {
                // FormData 형식으로 savedFileName 전송
                val requestBody = FormBody.Builder()
                    .add("savedFileName", recordingItem.savedFileName)
                    .build()

                val request = Request.Builder()
                    .url("http://10.0.2.2:8080/chatbot/session")
                    .post(requestBody)
                    .header("Authorization", "Bearer $token")
                    .build()

                client.newCall(request).execute().use { response ->
                    val responseBody = response.body?.string()

                    runOnUiThread {
                        if (response.isSuccessful && responseBody != null) {
                            try {
                                val jsonResponse = JSONObject(responseBody)
                                val sessionId = jsonResponse.getLong("sessionId")
                                val status = jsonResponse.getString("sessionStatus")

                                // ✅ 세션 ID 저장
                                sessionManager.saveSession(recordingItem.savedFileName, sessionId)

                                when (status) {
                                    "READY" -> {
                                        // 즉시 채팅 가능 - 히스토리 로드 후 채팅 화면 표시
                                        loadChatHistory(sessionId) {
                                            showChatScreen(recordingItem, sessionId)
                                        }
                                    }
                                    "INITIALIZING" -> {
                                        // 준비 중 - 상태 확인 반복
                                        checkSessionStatus(sessionId, recordingItem)
                                    }
                                    else -> {
                                        showSessionError("세션 생성에 실패했습니다.")
                                    }
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "세션 생성 응답 파싱 오류: ${e.message}", e)
                                showSessionError("응답 처리 중 오류가 발생했습니다.")
                            }
                        } else {
                            showSessionError("세션 생성에 실패했습니다.")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "세션 생성 오류: ${e.message}", e)
                runOnUiThread {
                    showSessionError("서버 연결에 실패했습니다.")
                }
            }
        }
    }


    private fun checkSessionStatus(sessionId: Long, recordingItem: RecordingItem) {
        val handler = Handler(Looper.getMainLooper())

        handler.postDelayed({
            thread {
                try {
                    // TODO: API만들어주면 수정하기 - 세션 상태 확인 API
                    val request = Request.Builder()
                        .url("http://10.0.2.2:8080/chatbot/session-status/$sessionId")
                        .header("Authorization", "Bearer ${getJwtToken()}")
                        .build()

                    client.newCall(request).execute().use { response ->
                        val responseBody = response.body?.string()

                        runOnUiThread {
                            if (response.isSuccessful && responseBody != null) {
                                try {
                                    val jsonResponse = JSONObject(responseBody)
                                    val status = jsonResponse.getString("status")

                                    when (status) {
                                        "READY" -> {
                                            showChatScreen(recordingItem, sessionId)
                                        }
                                        "INITIALIZING" -> {
                                            // 계속 대기
                                            checkSessionStatus(sessionId, recordingItem)
                                        }
                                        "ERROR" -> {
                                            showSessionError("AI 분석 중 오류가 발생했습니다.")
                                        }
                                    }
                                } catch (e: Exception) {
                                    showSessionError("상태 확인 중 오류가 발생했습니다.")
                                }
                            } else {
                                showSessionError("상태 확인에 실패했습니다.")
                            }
                        }
                    }
                } catch (e: Exception) {
                    runOnUiThread {
                        showSessionError("서버 연결에 실패했습니다.")
                    }
                }
            }
        }, 3000) // 3초마다 상태 확인
    }

    private fun showSessionError(message: String) {
        sessionLoadingProgress.visibility = View.GONE
        loadingStatusText.visibility = View.GONE
        createChatbotButton.visibility = View.VISIBLE

        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        Log.e(TAG, "세션 오류: $message")
    }

    // ========== 채팅 메시지 처리 ==========

    // ChatbotActivity.kt - loadChatHistory() 메서드 수정
    private fun loadChatHistory(sessionId: Long, onComplete: () -> Unit = {}) {
        thread {
            try {
                // GET 방식으로 sessionId를 URL에 포함하여 요청
                val request = Request.Builder()
                    .url("http://10.0.2.2:8080/chatbot/history/$sessionId")
                    .get() // GET 방식
                    .header("Authorization", "Bearer ${getJwtToken()}")
                    .build()

                client.newCall(request).execute().use { response ->
                    val responseBody = response.body?.string()

                    if (response.isSuccessful && responseBody != null) {
                        try {
                            val conversations = parseChatHistory(responseBody)

                            runOnUiThread {
                                chatMessages.clear()
                                chatMessages.addAll(conversations)
                                chatAdapter.updateMessages(conversations)
                                scrollToBottom()
                                onComplete()
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "채팅 기록 파싱 오류: ${e.message}", e)
                            runOnUiThread { onComplete() }
                        }
                    } else {
                        Log.d(TAG, "채팅 기록이 없거나 로드 실패 - 빈 채팅 화면 표시")
                        runOnUiThread { onComplete() }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "채팅 기록 로드 오류: ${e.message}", e)
                runOnUiThread { onComplete() }
            }
        }
    }
    // ChatbotActivity.kt - parseChatHistory() 메서드 수정
    private fun parseChatHistory(responseBody: String): List<ChatMessage> {
        val messages = mutableListOf<ChatMessage>()

        try {
            val jsonArray = JSONArray(responseBody)

            for (i in 0 until jsonArray.length()) {
                val jsonObject = jsonArray.getJSONObject(i)
                // val id = jsonObject.getInt("id") // 'id' might not be needed for display
                val question = jsonObject.getString("question") // question 필드명
                val isUser = jsonObject.getBoolean("isUser") // isUser 필드명
                val timestamp = jsonObject.getString("timestamp")

                // 시간 파싱
                val timestampLong = try {
                    SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                        .parse(timestamp)?.time ?: System.currentTimeMillis()
                } catch (e: Exception) {
                    System.currentTimeMillis()
                }

                messages.add(ChatMessage(question, isUser, timestampLong))
            }
        } catch (e: Exception) {
            Log.e(TAG, "채팅 기록 파싱 오류: ${e.message}", e)
        }

        return messages
    }

    // ChatbotActivity.kt - sendMessage() 메서드 수정
    private fun sendMessage() {
        val messageText = messageEditText.text.toString().trim()
        if (messageText.isEmpty() || currentSessionId == null) {
            return
        }

        // 사용자 메시지 즉시 표시
        val userMessage = ChatMessage(messageText, isUser = true)
        chatAdapter.addMessage(userMessage)
        messageEditText.text.clear()
        scrollToBottom()

        // 서버로 메시지 전송
        val token = getJwtToken()
        if (token.isEmpty()) {
            showAIError("로그인이 필요합니다.")
            return
        }

        thread {
            try {
                // FormData 형식으로 sessionId와 message 전송
                val requestBody = FormBody.Builder()
                    .add("sessionId", currentSessionId.toString()) // Long을 String으로 변환
                    .add("message", messageText)
                    .build()

                val request = Request.Builder()
                    .url("http://10.0.2.2:8080/chatbot/message")
                    .post(requestBody)
                    .header("Authorization", "Bearer $token")
                    .build()

                client.newCall(request).execute().use { response ->
                    val responseBody = response.body?.string()

                    runOnUiThread {
                        if (response.isSuccessful && responseBody != null) {
                            try {
                                val jsonResponse = JSONObject(responseBody)
                                val aiResponse = jsonResponse.getString("aiMessage") // 백엔드 응답 필드명

                                // AI 응답 표시
                                val aiMessage = ChatMessage(aiResponse, isUser = false)
                                chatAdapter.addMessage(aiMessage)
                                scrollToBottom()

                            } catch (e: Exception) {
                                Log.e(TAG, "AI 응답 파싱 오류: ${e.message}", e)
                                showAIError("응답 처리 중 오류가 발생했습니다.")
                            }
                        } else {
                            showAIError("메시지 전송에 실패했습니다.")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "메시지 전송 오류: ${e.message}", e)
                runOnUiThread {
                    showAIError("서버 연결에 실패했습니다.")
                }
            }
        }
    }

    private fun showAIError(message: String) {
        val errorMessage = ChatMessage("죄송합니다. $message", isUser = false)
        chatAdapter.addMessage(errorMessage)
        scrollToBottom()
    }

    private fun scrollToBottom() {
        if (chatMessages.isNotEmpty()) {
            chatRecyclerView.smoothScrollToPosition(chatMessages.size - 1)
        }
    }

    // ========== 유틸리티 메서드들 ==========

    private fun getJwtToken(): String {
        val sharedPreferences = getSharedPreferences("auth_prefs", MODE_PRIVATE)
        return sharedPreferences.getString("jwt_token", "") ?: ""
    }

    override fun onBackPressed() {
        when (currentState) {
            ScreenState.SESSION_SETUP, ScreenState.CHATTING -> {
                showFileListScreen()
            }
            ScreenState.FILE_LIST -> {
                super.onBackPressed()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "ChatbotActivity 종료")
    }
}