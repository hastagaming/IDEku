package com.hastagaming.ideku

import android.os.Handler
import android.os.Looper
import org.eclipse.jgit.api.Git
import java.io.File
import java.util.concurrent.Executors

class GitHandler(private val callback: GitCallback) {

    // Executor untuk menjalankan proses download di background thread
    private val executor = Executors.newSingleThreadExecutor()
    // Handler untuk mengirim perintah kembali ke Main Thread (UI)
    private val mainHandler = Handler(Looper.getMainLooper())

    /**
     * Interface untuk komunikasi antara proses Git dan MainActivity
     */
    interface GitCallback {
        fun onStart(platform: String)
        fun onFinished(repoName: String, destination: File)
        fun onError(errorMessage: String)
    }

    /**
     * Fungsi utama untuk melakukan clone repository
     * @param repoUrl Link GitHub atau GitLab (contoh: https://github.com/user/repo.git)
     * @param rootDirectory Direktori target (biasanya folder 'home' yang terisolasi)
     */
    fun cloneRepo(repoUrl: String, rootDirectory: File) {
        // Deteksi platform untuk notifikasi
        val platform = if (repoUrl.contains("gitlab.com", ignoreCase = true)) "gitlab" else "github"
        
        // Ambil nama repo dari URL (menghilangkan .git jika ada)
        val repoName = repoUrl.substringAfterLast("/").replace(".git", "")
        val destination = File(rootDirectory, repoName)

        // Trigger callback start di UI thread
        mainHandler.post { callback.onStart(platform) }

        executor.execute {
            try {
                // Proses pembersihan jika folder tujuan sudah ada (opsional)
                if (destination.exists()) {
                    destination.deleteRecursively()
                }

                // Eksekusi JGit Clone
                Git.cloneRepository()
                    .setURI(repoUrl)
                    .setDirectory(destination)
                    .setCloneAllBranches(true)
                    .call()

                // Jika sukses, kirim balik ke UI thread
                mainHandler.post {
                    callback.onFinished(repoName, destination)
                }
            } catch (e: Exception) {
                // Jika error (koneksi putus, repo privat, dll), kirim pesan error
                mainHandler.post {
                    callback.onError(e.message ?: "Terjadi kesalahan saat meng-clone project")
                }
            }
        }
    }
}