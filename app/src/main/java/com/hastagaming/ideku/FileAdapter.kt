package com.hastagaming.ideku

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.io.File

class FileAdapter(
    private var fileList: List<File>,
    private val onFileClicked: (File) -> Unit
) : RecyclerView.Adapter<FileAdapter.FileViewHolder>() {

    class FileViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvIcon: TextView = view.findViewById(R.id.tvIcon)
        val tvFileName: TextView = view.findViewById(R.id.tvFileName)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FileViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_file, parent, false)
        return FileViewHolder(view)
    }

    override fun onBindViewHolder(holder: FileViewHolder, position: Int) {
        val file = fileList[position]
        holder.tvFileName.text = file.name

        // Icon Logic: Folder vs File
        holder.tvIcon.text = if (file.isDirectory) "📁" else "📄"

        holder.itemView.setOnClickListener { onFileClicked(file) }
    }

    override fun getItemCount() = fileList.size

    fun updateFiles(newFiles: List<File>) {
        fileList = newFiles
        notifyDataSetChanged()
    }
}