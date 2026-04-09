package com.hastagaming.ideku

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Environment
import android.provider.Settings
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.termux.terminal.TerminalSession
import com.termux.terminal.TerminalSessionClient
import com.termux.view.TerminalView
import io.github.rosemoe.sora.langs.java.JavaLanguage
import io.github.rosemoe.sora.widget.CodeEditor
import io.github.rosemoe.sora.widget.schemes.SchemeEclipse
import java.io.File
import java.io.FileOutputStream

class MainActivity : AppCompatActivity() {

    private lateinit var editor: CodeEditor
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var rvFiles: RecyclerView
    private lateinit var fileAdapter: FileAdapter
    private lateinit var terminalView: TerminalView
    private var terminalSession: TerminalSession? = null
    private lateinit var tvCurrentFile: TextView
    private var isCtrlActive = false
    private var isAltActive = false

    // Variabel Lingkungan Global
    private val workingDir = "/data/data/com.hastagaming.ideku/files/home"
    private val shellPath = "/system/bin/sh"
    private lateinit var binDir: File
    private lateinit var homeDir: File
    private var currentDirectory: File? = null
    private var openedFile: File? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 1. Inisialisasi UI & Folder
        initUI()
        setupIsolatedEnv()
        deployAssets() 

        // 2. Jalankan Terminal
        startTerminalWithBootstrap()

        checkStoragePermission()
        loadDirectory(homeDir)

        // 3. Panggil setup extra keys
        setupExtraKeys()
    }

    private fun initUI() {
        editor = findViewById(R.id.codeEditor)
        drawerLayout = findViewById(R.id.drawerLayout)
        rvFiles = findViewById(R.id.rvFiles)
        tvCurrentFile = findViewById(R.id.tvCurrentFileName)
        terminalView = findViewById(R.id.terminalView)

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

    private fun startTerminalWithBootstrap() {
        val serviceIntent = Intent(this, IDEkuService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }

        val env = arrayOf(
            "TERM=xterm-256color",
            "HOME=$workingDir",
            "PATH=${binDir.absolutePath}:${System.getenv("PATH")}"
        )

        // Setup Client dengan semua metode yang diwajibkan oleh Modul Lokal
        val sessionClient = object : TerminalSessionClient {
            override fun onTextChanged(session: TerminalSession) { terminalView.onScreenUpdated() }
            override fun onSessionFinished(session: TerminalSession) { finish() }
            override fun onCopyTextToClipboard(session: TerminalSession, text: String?) {}
            override fun onPasteTextFromClipboard(session: TerminalSession?) {}
            override fun onBell(session: TerminalSession) {}
            override fun onColorsChanged(session: TerminalSession) {}
            override fun onTitleChanged(session: TerminalSession) {}
            override fun onTerminalCursorStateChange(state: Boolean) {}
            // FIX: Tambahkan ini agar tidak error "not abstract"
            override fun setTerminalShellPid(session: TerminalSession, pid: Int) {}
            override fun logStackTrace(tag: String?, e: Exception?) {}
            override fun logVerbose(tag: String?, message: String?) {}
            override fun logDebug(tag: String?, message: String?) {}
            override fun logInfo(tag: String?, message: String?) {}
            override fun logWarn(tag: String?, message: String?) {}
            override fun getTerminalCursorStyle(): Int { return 0 }
            override fun logError(tag: String?, message: String?) {}
        }

        // Inisialisasi Session (FIX: Tambahkan parameter '0' sebagai p5)
        terminalSession = TerminalSession(
            shellPath,
            workingDir,
            null, 
            env,
            0, // Parameter integer yang diminta library
            sessionClient
        )

        terminalView.attachSession(terminalSession)
        terminalView.setTextSize(14)

        terminalSession?.write("\r\n\u001b[32m[#] NASA-IDE Initializing...\u001b[0m\r\n")
        Handler(Looper.getMainLooper()).postDelayed({
            terminalSession?.write("\u001b[H\u001b[2J")
            terminalSession?.write("\u001b[1;36m[1] IDEku Ready, Komandan Nasa!\u001b[0m\r\n")
            terminalSession?.write("IDEku:~$ ")
        }, 1500)
    }

    private fun setupExtraKeys() {
        val keys = mapOf(
            R.id.key_esc to "\u001b",
            R.id.key_home to "\u001b[H",
            R.id.key_end to "\u001b[F",
            R.id.key_pgup to "\u001b[5~",
            R.id.key_pgdn to "\u001b[6~",
            R.id.key_up to "\u001b[A",
            R.id.key_down to "\u001b[B",
            R.id.key_left to "\u001b[D",
            R.id.key_right to "\u001b[C",
            R.id.key_tab to "\t"
        )

        keys.forEach { (id, sequence) ->
            findViewById<View>(id).setOnClickListener {
                terminalSession?.write(sequence)
            }
        }

        val btnCtrl = findViewById<Button>(R.id.key_ctrl)
        btnCtrl.setOnClickListener {
            isCtrlActive = !isCtrlActive
            btnCtrl.setBackgroundColor(if (isCtrlActive) 0xFF444444.toInt() else 0x00000000)
        }

        val btnAlt = findViewById<Button>(R.id.key_alt)
        btnAlt.setOnClickListener {
            isAltActive = !isAltActive
            btnAlt.setBackgroundColor(if (isAltActive) 0xFF444444.toInt() else 0x00000000)
        }
    }

    // Fungsi setupIsolatedEnv, deployAssets, dll tetap sama seperti sebelumnya
    private fun setupIsolatedEnv() {
        binDir = File(filesDir, "usr/bin")
        homeDir = File(filesDir.parentFile, "home")
        val tmpDir = File(filesDir, "tmp")
        listOf(binDir, homeDir, tmpDir).forEach {
            if (!it.exists()) it.mkdirs()
            it.setExecutable(true, false)
        }
    }

    private fun deployAssets() {
        val abi = Build.SUPPORTED_ABIS[0]
        val busyboxSource = when {
            abi.contains("arm64") -> "busybox_arm64"
            abi.contains("armeabi") -> "busybox_arm"
            abi.contains("x86_64") -> "busybox_x86_64"
            else -> "busybox_x86"
        }
        val aapt2Source = if (abi.contains("arm64")) "aapt2_arm64" else "aapt2_x86_64"
        val assetsToDeploy = listOf(
            busyboxSource to "busybox",
            aapt2Source to "aapt2",
            "d8.jar" to "d8.jar",
            "bw" to "bw"
        )
        assetsToDeploy.forEach { (assetName, targetName) ->
            val destFile = File(binDir, targetName)
            if (!destFile.exists()) {
                try {
                    assets.open(assetName).use { input ->
                        FileOutputStream(destFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                    destFile.setExecutable(true, false)
                } catch (e: Exception) { e.printStackTrace() }
            }
        }
        bootstrapBusyBoxLinks()
    }

    private fun bootstrapBusyBoxLinks() {
        val busybox = File(binDir, "busybox").absolutePath
        try {
            Runtime.getRuntime().exec("$busybox --install -s ${binDir.absolutePath}").waitFor()
        } catch (e: Exception) { e.printStackTrace() }
    }

    private fun runBuildCommand() {
        val projectPath = currentDirectory?.absolutePath ?: homeDir.absolutePath
        terminalSession?.write("bw build --dir $projectPath\n")
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
            Toast.makeText(this, "Berhasil Tersimpan", Toast.LENGTH_SHORT).show()
        }
    }
}
