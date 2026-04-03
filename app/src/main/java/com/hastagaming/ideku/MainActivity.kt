package com.hastagaming.ideku

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.github.rosemoe.sora.langs.java.JavaLanguage
import io.github.rosemoe.sora.widget.CodeEditor
import io.github.rosemoe.sora.widget.schemes.SchemeDarcula
import org.eclipse.jgit.api.Git
import java.io.File
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    // UI Components
    private lateinit var editor: CodeEditor
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var rvFiles: RecyclerView
    private lateinit var fileAdapter: FileAdapter

    // Path Logic (Termux Style)
    private lateinit var homeDir: File
    private lateinit var binDir: File
    private var currentDirectory: File? = null
    private var openedFile: File? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 1. Inisialisasi Lingkungan Terisolasi
        setupIsolatedEnv()

        // 2. Inisialisasi UI & Editor
        initUI()

        // 3. Cek Izin Bypass (Advanced Protect)
        checkStoragePermission()

        // 4. Muat File Pertama Kali
        loadDirectory(homeDir)
    }

    private fun setupIsolatedEnv() {
        // Direktori internal yang aman tapi bisa di-intip lewat Provider
        homeDir = File(filesDir, "home")
        binDir = File(filesDir, "usr/bin")
        
        val dirs = listOf(homeDir, binDir, File(filesDir, "tmp"))
        dirs.forEach { 
            if (!it.exists()) it.mkdirs()
            it.setExecutable(true, false)
            it.setReadable(true, false)
            it.setWritable(true, false)
        }

        // Contoh inisialisasi wrapper 'bw' (Bubblewrap)
        val bwFile = File(binDir, "bw")
        if (!bwFile.exists()) {
            bwFile.writeText("#!/system/bin/sh\necho 'BW Build System Active'")
            bwFile.setExecutable(true, false)
        }
    }

    private fun initUI() {
        editor = findViewById(R.id.codeEditor)
        drawerLayout = findViewById(R.id.drawerLayout)
        rvFiles = findViewById(R.id.rvFiles)

        // Konfigurasi Editor
        editor.colorScheme = SchemeDarcula()
        editor.setEditorLanguage(JavaLanguage())
        editor.isLineNumberEnabled = true
        editor.setBackgroundColor(0xFF1E1E1E.toInt())

        // Konfigurasi File Manager (Sidebar)
        rvFiles.layoutManager = LinearLayoutManager(this)
        fileAdapter = FileAdapter(emptyList()) { file ->
            if (file.isDirectory) loadDirectory(file) else openFile(file)
        }
        rvFiles.adapter = fileAdapter
    }

    private fun checkStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                intent.data = Uri.parse("package:$packageName")
                startActivity(intent)
            }
        }
    }

    // --- FITUR GIT CLONE ---
    fun startGitClone(url: String) {
        val platform = if (url.contains("github.com")) "github" else "gitlab"
        val repoName = url.substringAfterLast("/").substringBefore(".git")
        val destination = File(homeDir, repoName)

        // Toast Sesuai Request
        Toast.makeText(this, "mengclone project dari $platform", Toast.LENGTH_SHORT).show()

        Executors.newSingleThreadExecutor().execute {
            try {
                Git.cloneRepository()
                    .setURI(url)
                    .setDirectory(destination)
                    .call()

                runOnUiThread {
                    // Toast Sesuai Request
                    Toast.makeText(this, "project berhasil di clone $repoName", Toast.LENGTH_LONG).show()
                    fixPermissions(destination)
                    loadDirectory(homeDir)
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this, "Gagal: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // --- FILE I/O LOGIC ---
    private fun loadDirectory(directory: File) {
        currentDirectory = directory
        val files = directory.listFiles()?.toList() ?: emptyList()
        val sortedFiles = files.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
        fileAdapter.updateFiles(sortedFiles)
    }

    private fun openFile(file: File) {
        try {
            editor.setText(file.readText())
            openedFile = file
            drawerLayout.closeDrawers()
            Toast.makeText(this, "Membuka: ${file.name}", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun saveFile() {
        openedFile?.let {
            it.writeText(editor.text.toString())
            Toast.makeText(this, "Tersimpan", Toast.LENGTH_SHORT).show()
        }
    }

    private fun fixPermissions(file: File) {
        file.setExecutable(true, false)
        file.setReadable(true, false)
        file.setWritable(true, false)
        if (file.isDirectory) file.listFiles()?.forEach { fixPermissions(it) }
    }

    override fun onBackPressed() {
        if (drawerLayout.isOpen) {
            drawerLayout.closeDrawers()
        } else if (currentDirectory != homeDir) {
            currentDirectory?.parentFile?.let { loadDirectory(it) }
        } else {
            super.onBackPressed()
        }
    }
}