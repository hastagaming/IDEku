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
import com.termux.view.TerminalView
import io.github.rosemoe.sora.langs.java.JavaLanguage
import io.github.rosemoe.sora.widget.CodeEditor
import io.github.rosemoe.sora.widget.schemes.SchemeEclipse
import org.eclipse.jgit.api.Git
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    private lateinit var editor: CodeEditor
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var rvFiles: RecyclerView
    private lateinit var fileAdapter: FileAdapter
    private lateinit var terminalWebView: WebView
    private lateinit var tvCurrentFile: TextView

    private lateinit var homeDir: File
    private lateinit var binDir: File
    private var currentDirectory: File? = null
    private var openedFile: File? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 1. Setup Folder & Pindahkan Senjata dari Assets
        setupIsolatedEnv()
        deployAssets() 

        // 2. UI & Terminal
        initUI()
        startTerminal()

        checkStoragePermission()
        loadDirectory(homeDir)
    }

    private fun setupIsolatedEnv() {
        binDir = File(filesDir, "usr/bin")
        homeDir = File(filesDir.parentFile, "home")
        val tmpDir = File(filesDir, "tmp")

        listOf(binDir, homeDir, tmpDir).forEach {
            if (!it.exists()) it.mkdirs()
            it.setExecutable(true, false)
        }
    }

    /**
     * Menghubungkan biner dari Assets ke Sistem Internal IDE
     */
    private fun deployAssets() {
        val abi = Build.SUPPORTED_ABIS[0]
        
        // Pilih biner BusyBox sesuai Arsitektur
        val busyboxSource = when {
            abi.contains("arm64") -> "busybox_arm64"
            abi.contains("armeabi") -> "busybox_arm"
            abi.contains("x86_64") -> "busybox_x86_64"
            else -> "busybox_x86"
        }

        // Pilih AAPT2 (Umumnya arm64 atau x86_64)
        val aapt2Source = if (abi.contains("arm64")) "aapt2_arm64" else "aapt2_x86_64"

        // Daftar pemetaan (Nama di Assets -> Nama Target di bin/)
        val assetsToDeploy = listOf(
            busyboxSource to "busybox",
            aapt2Source to "aapt2",
            "d8.jar" to "d8.jar",
            "bw" to "bw"
        )

        assetsToDeploy.forEach { (assetName, targetName) ->
            val destFile = File(binDir, targetName)
            
            // Hanya copy jika belum ada (hemat resource)
            if (!destFile.exists()) {
                try {
                    assets.open(assetName).use { input ->
                        FileOutputStream(destFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                    destFile.setExecutable(true, false)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        // Jalankan Symlink BusyBox agar perintah ls, cp, dll aktif
        bootstrapBusyBoxLinks()
    }

    private fun bootstrapBusyBoxLinks() {
        val busybox = File(binDir, "busybox").absolutePath
        try {
            Runtime.getRuntime().exec("$busybox --install -s ${binDir.absolutePath}").waitFor()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun initUI() {
        editor = findViewById(R.id.codeEditor)
        drawerLayout = findViewById(R.id.drawerLayout)
        rvFiles = findViewById(R.id.rvFiles)
        tvCurrentFile = findViewById(R.id.tvCurrentFileName)
        terminalWebView = findViewById(R.id.terminalWebView)
        
        terminalWebView = findViewById(R.id.terminalWebView)
        terminalWebView.settings.javaScriptEnabled = true


        editor.colorScheme = SchemeEclipse()
        editor.setEditorLanguage(JavaLanguage())
        editor.isLineNumberEnabled = true

        rvFiles.layoutManager = LinearLayoutManager(this)
        fileAdapter = FileAdapter(emptyList()) { file ->
            if (file.isDirectory) loadDirectory(file) else openFile(file)
        }
        rvFiles.adapter = fileAdapter

        findViewById<Button>(R.id.btnSave).setOnClickListener { saveFile() }
        findViewById<Button>(R.id.btnRun).setOnClickListener { runBuildCommand() }
    }

    private fun startTerminal() {
        val env = arrayOf(
            "PATH=${binDir.absolutePath}:${System.getenv("PATH")}",
            "HOME=${homeDir.absolutePath}",
            "TERM=xterm-256color"
        )
        terminalView.createSession("/system/bin/sh", env, homeDir.absolutePath)
        terminalView.start()
        
        terminalView.write("\r\n[ IDEku | System Ready ]\r\n")
        terminalView.write("Binaries: ${binDir.path}\r\n\n")
    }

    private fun runBuildCommand() {
        // Memanggil Bubblewrap (bw) yang ada di bin/
        val projectPath = currentDirectory?.absolutePath ?: homeDir.absolutePath
        terminalView.write("bw build --dir $projectPath\n")
    }

    // --- SISANYA TETAP SAMA DENGAN KODE KAMU ---
    private fun checkStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                intent.data = Uri.parse("package:$packageName")
                startActivity(intent)
            }
        }
    }

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
}
