package com.hastagaming.ideku

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.numetriclabz.terminalview.TerminalView
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
    private lateinit var terminalView: TerminalView
    private lateinit var tvCurrentFile: TextView

    // Path Logic (Standalone Isolation)
    private lateinit var homeDir: File
    private lateinit var binDir: File
    private var currentDirectory: File? = null
    private var openedFile: File? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 1. Inisialisasi Lingkungan & BusyBox
        setupIsolatedEnv()
        bootstrapBusyBox()

        // 2. Inisialisasi UI & Editor
        initUI()

        // 3. Inisialisasi Terminal Mandiri
        startTerminal()

        // 4. Cek Izin & Muat File
        checkStoragePermission()
        loadDirectory(homeDir)
    }

    private fun setupIsolatedEnv() {
        // Folder bin: /data/data/com.hastagaming.ideku/files/usr/bin
        binDir = File(filesDir, "usr/bin")
        // Folder home: /data/data/com.hastagaming.ideku/home (Sesuai request)
        homeDir = File(filesDir.parentFile, "home")

        val dirs = listOf(binDir, homeDir, File(filesDir, "tmp"))
        dirs.forEach { 
            if (!it.exists()) it.mkdirs()
            it.setExecutable(true, false)
            it.setReadable(true, false)
            it.setWritable(true, false)
        }
    }

    private fun bootstrapBusyBox() {
        val busybox = File(binDir, "busybox")
        
        // Memindahkan biner BusyBox dari assets ke /usr/bin
        if (!busybox.exists()) {
            try {
                assets.open("busybox").use { input ->
                    busybox.outputStream().use { output -> input.copyTo(output) }
                }
                busybox.setExecutable(true, false)
                
                // Install Symlinks (Menciptakan perintah ls, cp, rm, dll)
                Runtime.getRuntime().exec("${busybox.absolutePath} --install -s ${binDir.absolutePath}").waitFor()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun initUI() {
        editor = findViewById(R.id.codeEditor)
        drawerLayout = findViewById(R.id.drawerLayout)
        rvFiles = findViewById(R.id.rvFiles)
        tvCurrentFile = findViewById(R.id.tvCurrentFileName)
        terminalView = findViewById(R.id.terminalView)

        // Editor Config
        editor.colorScheme = SchemeDarcula()
        editor.setEditorLanguage(JavaLanguage())
        editor.isLineNumberEnabled = true
        editor.setBackgroundColor(0xFF1E1E1E.toInt())

        // Sidebar Config
        rvFiles.layoutManager = LinearLayoutManager(this)
        fileAdapter = FileAdapter(emptyList()) { file ->
            if (file.isDirectory) loadDirectory(file) else openFile(file)
        }
        rvFiles.adapter = fileAdapter

        // Button Actions
        findViewById<Button>(R.id.btnSave).setOnClickListener { saveFile() }
        findViewById<Button>(R.id.btnRun).setOnClickListener { runBuildCommand() }
        findViewById<Button>(R.id.btnClone).setOnClickListener { 
            // Contoh trigger clone
            startGitClone("https://github.com/hastagaming/example.git") 
        }
    }

    private fun startTerminal() {
        val env = arrayOf(
            "PATH=${binDir.absolutePath}:/system/bin:/system/xbin",
            "HOME=${homeDir.absolutePath}",
            "TERM=xterm-256color"
        )
        // Jalankan shell mandiri di folder home isolasi
        terminalView.createSession("/system/bin/sh", env, homeDir.absolutePath)
        terminalView.start()
        
        terminalView.write("\r\n[ NASA-IDE SYSTEM READY ]\r\n")
        terminalView.write("WorkDir: ${homeDir.path}\r\n\n")
    }

    private fun runBuildCommand() {
        // Otomatis mengetik perintah build ke terminal
        val projectPath = currentDirectory?.absolutePath ?: homeDir.absolutePath
        terminalView.write("bw build --dir $projectPath\n")
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

    // --- GIT LOGIC ---
    fun startGitClone(url: String) {
        val platform = if (url.contains("github.com")) "github" else "gitlab"
        val repoName = url.substringAfterLast("/").substringBefore(".git")
        val destination = File(homeDir, repoName)

        Toast.makeText(this, "mengclone project dari $platform", Toast.LENGTH_SHORT).show()

        Executors.newSingleThreadExecutor().execute {
            try {
                Git.cloneRepository()
                    .setURI(url)
                    .setDirectory(destination)
                    .call()

                runOnUiThread {
                    Toast.makeText(this, "project berhasil di clone $repoName", Toast.LENGTH_LONG).show()
                    fixPermissions(destination)
                    loadDirectory(homeDir)
                    terminalView.write("cd $repoName\n")
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this, "Gagal: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // --- FILE SYSTEM LOGIC ---
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
            tvCurrentFile.text = file.name
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