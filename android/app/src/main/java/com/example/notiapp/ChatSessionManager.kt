package com.example.notiapp

import android.content.Context
import android.content.SharedPreferences

class ChatSessionManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("chat_sessions", Context.MODE_PRIVATE)

    // 세션 정보 저장
    fun saveSession(savedFileName: String, sessionId: Long) {
        prefs.edit()
            .putLong(savedFileName, sessionId)
            .apply()
    }

    // 세션 ID 가져오기
    fun getSessionId(savedFileName: String): Long? {
        val sessionId = prefs.getLong(savedFileName, -1L)
        return if (sessionId == -1L) null else sessionId
    }

    // 세션 존재 여부 확인
    fun hasSession(savedFileName: String): Boolean {
        return prefs.contains(savedFileName)
    }

    // 특정 세션 삭제
    fun removeSession(savedFileName: String) {
        prefs.edit()
            .remove(savedFileName)
            .apply()
    }

    // 모든 세션 삭제
    fun clearAllSessions() {
        prefs.edit()
            .clear()
            .apply()
    }
}