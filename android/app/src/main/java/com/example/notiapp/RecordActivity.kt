package com.example.notiapp

import android.Manifest
import android.app.ProgressDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

class RecordActivity : AppCompatActivity() {

    private val TAG = "recordActivity"
    private val RECORD_AUDIO_PERMISSION_CODE = 200

    private var mediaRecorder: MediaRecorder? = null
    private var isRecording = false
    private var outputFile: String = ""
    private var recordingDuration: Int = 0
    private var startTime: Long = 0

    private lateinit var recordButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_record)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.record)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // UI 요소 초기화
        recordButton = findViewById(R.id.button)

        // 녹음 버튼 클릭 리스너 설정
        recordButton.setOnClickListener {
            if (checkPermission()) {
                if (isRecording) {
                    stopRecording()
                } else {
                    startRecording()
                }
            } else {
                requestPermission()
            }
        }
    }

    private fun checkPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.RECORD_AUDIO),
            RECORD_AUDIO_PERMISSION_CODE
        )
    }

    private fun startRecording() {
        try {
            // 녹음 파일 저장 디렉토리 생성
            val recordingsDir = File(getExternalFilesDir(null), "recordings")
            if (!recordingsDir.exists()) {
                recordingsDir.mkdirs()
            }

            // 파일명 생성 (현재 시간 기반)
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            outputFile = File(recordingsDir, "REC_$timestamp.mp3").absolutePath

            // 녹음 시작 시간 기록
            startTime = System.currentTimeMillis()

            // MediaRecorder 초기화
            mediaRecorder = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(outputFile)
                prepare()
                start()
            }

            // UI 업데이트
            isRecording = true
            recordButton.text = "녹음 중지"
            findViewById<TextView>(R.id.textView4).text = "녹음 중..."

        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(this, "녹음을 시작할 수 없습니다: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun stopRecording() {
        try {
            // 녹음 종료 시간 기록 및 기간 계산
            recordingDuration = ((System.currentTimeMillis() - startTime) / 1000).toInt() // 초 단위

            mediaRecorder?.apply {
                stop()
                reset()
                release()
            }
            mediaRecorder = null
            isRecording = false

            // UI 업데이트
            recordButton.text = "녹음"
            findViewById<TextView>(R.id.textView4).text = "녹음이 완료되었습니다"

            // 녹음 파일 서버 전송 및 로딩 창 표시
            uploadRecordingAndNavigate()

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "녹음을 중지할 수 없습니다: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun uploadRecordingAndNavigate() {
        val progressDialog = ProgressDialog(this)
        progressDialog.setMessage("녹음 파일 처리 중...")
        progressDialog.setCancelable(false)
        progressDialog.show()

        // 서버에 녹음 파일 전송
        thread {
            try {
                val file = File(outputFile)
                if (!file.exists()) {
                    Log.e(TAG, "파일이 존재하지 않습니다: $outputFile")
                    showErrorAndDismissDialog(progressDialog, "파일을 찾을 수 없습니다.")
                    return@thread
                }

                Log.d(TAG, "파일 업로드 시작: ${file.name}, 크기: ${file.length()} bytes, 녹음 길이: ${recordingDuration}초")

                // JWT 토큰 가져오기
                val token = getJwtToken()
                if (token.isEmpty()) {
                    showErrorAndDismissDialog(progressDialog, "로그인이 필요합니다. 다시 로그인해주세요.")
                    navigateToLogin()
                    return@thread
                }

                // OkHttp 클라이언트 생성
                val client = OkHttpClient.Builder()
                    .connectTimeout(60, TimeUnit.SECONDS)
                    .readTimeout(60, TimeUnit.SECONDS)
                    .writeTimeout(60, TimeUnit.SECONDS)
                    .build()

                // AudioUploadRequest에 맞게 MultipartBody 구성
                val requestBody = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart(
                        "file",
                        file.name,
                        file.asRequestBody("audio/mpeg".toMediaTypeOrNull())
                    )
                    .addFormDataPart("duration", recordingDuration.toString())
                    .build()

                // 요청 생성 (Authorization 헤더에 JWT 토큰 추가)
                val request = Request.Builder()
                    .url("http://10.0.2.2:8080/file/upload/audio")
                    .post(requestBody)
                    .header("Authorization", "Bearer $token")
                    .build()

                // 요청 실행
                client.newCall(request).execute().use { response ->
                    val responseBody = response.body?.string()
                    Log.d(TAG, "서버 응답: ${response.code} - $responseBody")

                    // UI 스레드에서 결과 처리
                    runOnUiThread {
                        progressDialog.dismiss()

                        if (response.isSuccessful && responseBody != null) {
                            // 응답 메시지에서 UUID가 포함된 파일명 추출
                            try {
                                // "파일 저장 완료: UUID_원본파일명" 형식에서 파일명 추출
                                val prefix = "파일 저장 완료: "
                                if (responseBody.startsWith(prefix)) {
                                    val serverSavedFileName = responseBody.substring(prefix.length).trim()

                                    // 로컬 파일명과 서버 저장 파일명 매핑하여 SharedPreferences에 저장
                                    val sharedPreferences = getSharedPreferences("recording_files", MODE_PRIVATE)
                                    val editor = sharedPreferences.edit()
                                    editor.putString(file.name, serverSavedFileName)
                                    editor.apply()

                                    Log.d(TAG, "파일명 매핑 저장: ${file.name} -> $serverSavedFileName")

                                    Toast.makeText(this, "녹음 파일이 서버에 업로드되었습니다!", Toast.LENGTH_SHORT).show()
                                } else {
                                    Log.w(TAG, "예상치 못한 응답 형식: $responseBody")
                                    Toast.makeText(this, "녹음 파일이 업로드되었지만 파일명을 확인할 수 없습니다.", Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "응답 파싱 오류: ${e.message}", e)
                                Toast.makeText(this, "녹음 파일이 업로드되었지만 처리 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
                            }

                            // 대시보드로 이동
                            navigateToDashboard()
                        } else {
                            // 서버 오류 처리
                            val errorMessage = when (response.code) {
                                400 -> "요청 데이터가 올바르지 않습니다."
                                401, 403 -> {
                                    // 인증 오류 시 토큰 무효화 및 로그인 화면으로 이동
                                    clearJwtToken()
                                    "인증이 만료되었습니다. 다시 로그인해주세요."
                                }
                                500 -> "서버 오류가 발생했습니다. 잠시 후 다시 시도하세요."
                                else -> "업로드 실패: ${response.code}"
                            }

                            Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()

                            // 인증 오류인 경우 로그인 화면으로, 그 외의 경우 대시보드로 이동
                            if (response.code == 401 || response.code == 403) {
                                navigateToLogin()
                            } else {
                                navigateToDashboard()
                            }
                        }
                    }
                }
            } catch (e: IOException) {
                Log.e(TAG, "네트워크 오류: ${e.message}", e)
                showErrorAndDismissDialog(progressDialog, "서버 연결에 실패했습니다. 인터넷 연결을 확인하세요.")

                // 네트워크 오류 시에도 대시보드로 이동
                navigateToDashboard()
            } catch (e: Exception) {
                Log.e(TAG, "예외 발생: ${e.message}", e)
                showErrorAndDismissDialog(progressDialog, "오류가 발생했습니다: ${e.message}")

                // 예외 발생 시에도 대시보드로 이동
                navigateToDashboard()
            }
        }
    }

    // JWT 토큰 가져오기
    private fun getJwtToken(): String {
        val sharedPreferences = getSharedPreferences("auth_prefs", MODE_PRIVATE)
        return sharedPreferences.getString("jwt_token", "") ?: ""
    }

    // JWT 토큰 삭제
    private fun clearJwtToken() {
        val sharedPreferences = getSharedPreferences("auth_prefs", MODE_PRIVATE)
        sharedPreferences.edit().remove("jwt_token").apply()
    }

    private fun showErrorAndDismissDialog(dialog: ProgressDialog, message: String) {
        runOnUiThread {
            dialog.dismiss()
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        }
    }

    private fun navigateToDashboard() {
        runOnUiThread {
            val intent = Intent(this, DashBoardActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            startActivity(intent)
            finish()
        }
    }

    private fun navigateToLogin() {
        runOnUiThread {
            val intent = Intent(this, SignInActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            finish()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == RECORD_AUDIO_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "녹음 권한이 승인되었습니다", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "녹음 권한이 필요합니다", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onStop() {
        super.onStop()
        // 활동이 중지될 때 녹음 중이면 중지
        if (isRecording) {
            stopRecording()
        }
    }
}