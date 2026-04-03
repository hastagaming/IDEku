package com.hastagaming.ideku

import android.content.Context
import java.io.File
import java.util.concurrent.Executors

class BuildEngine(private val context: Context) {

    private val executor = Executors.newSingleThreadExecutor()

    fun runBuild(projectPath: String, onResult: (String) -> Unit) {
        executor.execute {
            try {
                // Lokasi binari bw di folder usr/bin (terisolasi)
                val binDir = File(context.filesDir, "usr/bin")
                val bwPath = File(binDir, "bw").absolutePath
                
                // Menjalankan perintah shell: ./bw build [path_project]
                val process = Runtime.getRuntime().exec("$bwPath build --dir $projectPath")
                
                val output = process.inputStream.bufferedReader().readText()
                val error = process.errorStream.bufferedReader().readText()
                process.waitFor()

                val finalLog = if (error.isNotEmpty()) "ERROR:\n$error" else "SUCCESS:\n$output"
                onResult(finalLog)
            } catch (e: Exception) {
                onResult("Failed to execute build: ${e.message}")
            }
        }
    }
}