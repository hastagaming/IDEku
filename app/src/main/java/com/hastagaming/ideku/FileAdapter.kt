package com.hastagaming.ideku

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.io.File

class FileAdapter(
    private var fileList: List<File>,
    private val onFileClicked: (File) -> Unit
) : RecyclerView.Adapter<FileAdapter.FileViewHolder>() {

    // ViewHolder menggunakan ImageView sesuai item_file.xml terbaru
    class FileViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivFileIcon: ImageView = view.findViewById(R.id.ivFileIcon)
        val tvFileName: TextView = view.findViewById(R.id.tvFileName)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FileViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_file, parent, false)
        return FileViewHolder(view)
    }

    override fun onBindViewHolder(holder: FileViewHolder, position: Int) {
        val file = fileList[position]
        holder.tvFileName.text = file.name

        // Logika Ikon: Menggunakan Gambar dari Drawable (Bukan Emoji)
        if (file.isDirectory) {
            holder.ivFileIcon.setImageResource(R.drawable.ic_folder)
        } else {
            holder.ivFileIcon.setImageResource(R.drawable.ic_file)
        }

        holder.itemView.setOnClickListener { onFileClicked(file) }
    }

    override fun getItemCount() = fileList.size

    // Helper untuk memperbarui daftar file saat navigasi folder
    fun updateFiles(newFiles: List<File>) {
        fileList = newFiles
        notifyDataSetChanged()
    }
}
