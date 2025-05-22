package com.example.notiapp

import java.io.File

data class RecordingItem(
    val file: File?, // 로컬 파일인 경우에만 존재
    val filename: String, // 파일 이름
    val date: String, // 녹음 일자
    val duration: String, // 파일 길이
    val filePath: String, // 파일 경로
    val isServerFile: Boolean = false, // 서버 파일 여부
    val savedFileName: String = "", // 서버에 저장된 파일명 (UUID 포함)
    val fileSize: Long = 0L, // 파일 크기
    val uploadDate: String = "", // 서버에 업로드 된 날짜
    val isDownloaded: Boolean = false // 서버 파일이 다운로드되었는지 여부
)