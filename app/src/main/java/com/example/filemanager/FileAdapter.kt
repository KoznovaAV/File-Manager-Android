package com.example.filemanager

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FileAdapter(
    private var items: List<File>,
    private val onClick: (File) -> Unit,
    private val onMoreClick: (File, View) -> Unit
) : RecyclerView.Adapter<FileAdapter.FileViewHolder>() {

    class FileViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val icon: ImageView = view.findViewById(R.id.icon)
        val name: TextView = view.findViewById(R.id.name)
        val details: TextView = view.findViewById(R.id.details)
        val moreButton: ImageButton = view.findViewById(R.id.moreButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FileViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_file, parent, false)
        return FileViewHolder(view)
    }

    override fun onBindViewHolder(holder: FileViewHolder, position: Int) {
        val file = items[position]
        holder.name.text = file.name
        holder.icon.setImageResource(
            if (file.isDirectory) android.R.drawable.ic_menu_agenda
            else android.R.drawable.ic_menu_edit
        )
        holder.details.text = if (file.isDirectory) {
            "Папка"
        } else {
            val sdf = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
            "${file.length() / 1024} КБ · ${sdf.format(Date(file.lastModified()))}"
        }
        holder.itemView.setOnClickListener { onClick(file) }
        holder.moreButton.setOnClickListener { onMoreClick(file, it) }
    }

    override fun getItemCount() = items.size

    fun updateData(newItems: List<File>) {
        items = newItems
        notifyDataSetChanged()
    }
}