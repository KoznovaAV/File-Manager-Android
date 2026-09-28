package com.example.filemanager

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.PopupMenu
import androidx.appcompat.widget.SearchView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.activity.addCallback

class MainActivity : AppCompatActivity() {

    private lateinit var viewModel: FileViewModel
    private lateinit var adapter: FileAdapter
    private lateinit var pathText: TextView
    private val repository = FileRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        viewModel = ViewModelProvider(this)[FileViewModel::class.java]

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        pathText = findViewById(R.id.pathText)
        val recyclerView: RecyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        adapter = FileAdapter(
            emptyList(),
            onClick = { file -> onFileClick(file) },
            onMoreClick = { file, anchor -> showFileMenu(file, anchor) }
        )
        recyclerView.adapter = adapter

        findViewById<FloatingActionButton>(R.id.fabAddFolder).setOnClickListener {
            showCreateFolderDialog()
        }

        viewModel.files.observe(this) { adapter.updateData(it) }
        viewModel.currentDir.observe(this) { pathText.text = it.absolutePath }

        checkPermissions()

        onBackPressedDispatcher.addCallback(this) {
            if (!goUp()) {
                isEnabled = false
                onBackPressedDispatcher.onBackPressed()
            }
        }
    }

    private fun checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                intent.data = Uri.parse("package:$packageName")
                startActivity(intent)
            } else {
                viewModel.openDir(Environment.getExternalStorageDirectory())
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), 1
                )
            } else {
                viewModel.openDir(Environment.getExternalStorageDirectory())
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            viewModel.openDir(Environment.getExternalStorageDirectory())
        } else {
            Toast.makeText(this, "Нужен доступ к файлам", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
            Environment.isExternalStorageManager() &&
            viewModel.currentDir.value == null
        ) {
            viewModel.openDir(Environment.getExternalStorageDirectory())
        }
    }

    private fun onFileClick(file: File) {
        if (file.isDirectory) {
            viewModel.openDir(file)
        } else {
            Toast.makeText(this, "Файл: ${file.name}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun goUp(): Boolean {
        val current = viewModel.currentDir.value ?: return false
        val parent = current.parentFile
        return if (parent != null && current != Environment.getExternalStorageDirectory()) {
            viewModel.openDir(parent)
            true
        } else false
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        val searchItem = menu.findItem(R.id.action_search)
        val searchView = searchItem.actionView as SearchView
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?) = true
            override fun onQueryTextChange(newText: String?): Boolean {
                viewModel.setSearchQuery(newText ?: "")
                return true
            }
        })
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        menu.findItem(R.id.action_paste)?.isVisible = ClipboardHolder.file != null
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_sort -> showSortDialog()
            R.id.action_paste -> pasteFromClipboard()
            R.id.action_about -> showAboutDialog()
        }
        return super.onOptionsItemSelected(item)
    }

    private fun showSortDialog() {
        val options = arrayOf("По имени", "По размеру", "По дате изменения")
        AlertDialog.Builder(this)
            .setTitle("Сортировка")
            .setItems(options) { _, which ->
                val type = when (which) {
                    1 -> SortType.SIZE
                    2 -> SortType.DATE
                    else -> SortType.NAME
                }
                viewModel.setSortType(type)
            }
            .show()
    }

    private fun showAboutDialog() {
        AlertDialog.Builder(this)
            .setTitle("О программе")
            .setMessage("Файловый менеджер\nКурсовая работа\nMaterial Design 3, Android, Kotlin, MVVM")
            .setPositiveButton("Ок", null)
            .show()
    }

    private fun showFileMenu(file: File, anchor: View) {
        val popup = PopupMenu(this, anchor)
        popup.menuInflater.inflate(R.menu.menu_file_item, popup.menu)
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_rename -> showRenameDialog(file)
                R.id.action_delete -> showDeleteDialog(file)
                R.id.action_copy -> {
                    ClipboardHolder.file = file
                    ClipboardHolder.isCut = false
                    Toast.makeText(this, "Скопировано: ${file.name}", Toast.LENGTH_SHORT).show()
                }
                R.id.action_cut -> {
                    ClipboardHolder.file = file
                    ClipboardHolder.isCut = true
                    Toast.makeText(this, "Вырезано: ${file.name}", Toast.LENGTH_SHORT).show()
                }
                R.id.action_properties -> showPropertiesDialog(file)
            }
            true
        }
        popup.show()
    }

    private fun showCreateFolderDialog() {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_create_folder, null)
        val input = view.findViewById<TextInputEditText>(R.id.folderNameInput)
        AlertDialog.Builder(this)
            .setTitle("Новая папка")
            .setView(view)
            .setPositiveButton("Создать") { _, _ ->
                val name = input.text?.toString()?.trim()
                val dir = viewModel.currentDir.value
                if (!name.isNullOrEmpty() && dir != null) {
                    if (repository.createFolder(dir, name)) {
                        viewModel.refreshCurrent()
                    } else {
                        Toast.makeText(this, "Не удалось создать папку", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun showRenameDialog(file: File) {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_create_folder, null)
        val input = view.findViewById<TextInputEditText>(R.id.folderNameInput)
        input.setText(file.name)
        AlertDialog.Builder(this)
            .setTitle("Переименовать")
            .setView(view)
            .setPositiveButton("Ок") { _, _ ->
                val newName = input.text?.toString()?.trim()
                if (!newName.isNullOrEmpty()) {
                    if (repository.rename(file, newName)) {
                        viewModel.refreshCurrent()
                    } else {
                        Toast.makeText(this, "Не удалось переименовать", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun showDeleteDialog(file: File) {
        AlertDialog.Builder(this)
            .setTitle("Удалить \"${file.name}\"?")
            .setMessage("Это действие нельзя отменить.")
            .setPositiveButton("Удалить") { _, _ ->
                if (repository.delete(file)) {
                    viewModel.refreshCurrent()
                } else {
                    Toast.makeText(this, "Не удалось удалить", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun pasteFromClipboard() {
        val source = ClipboardHolder.file
        val destDir = viewModel.currentDir.value
        if (source == null || destDir == null) return
        val success = if (ClipboardHolder.isCut) {
            repository.move(source, destDir)
        } else {
            repository.copy(source, destDir)
        }
        if (success) {
            if (ClipboardHolder.isCut) ClipboardHolder.file = null
            viewModel.refreshCurrent()
        } else {
            Toast.makeText(this, "Не удалось вставить", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showPropertiesDialog(file: File) {
        val sdf = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
        val message = buildString {
            append("Имя: ${file.name}\n")
            append("Путь: ${file.absolutePath}\n")
            append("Тип: ${if (file.isDirectory) "Папка" else "Файл"}\n")
            if (file.isFile) append("Размер: ${file.length() / 1024} КБ\n")
            append("Изменён: ${sdf.format(Date(file.lastModified()))}")
        }
        AlertDialog.Builder(this)
            .setTitle("Свойства")
            .setMessage(message)
            .setPositiveButton("Ок", null)
            .show()
    }
}