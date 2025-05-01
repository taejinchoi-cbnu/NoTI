package com.example.notiapp

import java.io.File

data class RecordingItem(
    val file: File,
    val filename: String,
    val date: String,
    val duration: String,
    val filePath: String
)