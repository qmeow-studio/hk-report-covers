@file:OptIn(ExperimentalMaterial3Api::class)

package com.tx.reportcovers

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.imageLoader
import coil.request.ImageRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainVM : ViewModel() {
    var years by mutableStateOf(listOf<Pair<Int, Int>>())
    var year by mutableStateOf(0)
    var all by mutableStateOf(listOf<Report>())
    var loading by mutableStateOf(false)
    var error by mutableStateOf<String?>(null)
    var type by mutableStateOf("ALL")
    var query by mutableStateOf("")
    var sortByDate by mutableStateOf(false)

    init {
        load()
    }

    fun load(selectYear: Int = year) {
        viewModelScope.launch {
            loading = true
            error = null
            try {
                val ys = withContext(Dispatchers.IO) { DataSource.years() }
                years = ys
                val y = if (ys.any { it.first == selectYear }) selectYear else (ys.firstOrNull()?.first ?: 0)
                year = y
                all = if (y == 0) emptyList() else withContext(Dispatchers.IO) { DataSource.reports(y) }
            } catch (e: Exception) {
                error = e.message ?: e.toString()
                all = emptyList()
            }
            loading = false
        }
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                MainScreen()
            }
        }
    }
}

@Composable
fun MainScreen(vm: MainVM = viewModel()) {
    var showSettings by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf<Report?>(null) }
    var yearMenu by remember { mutableStateOf(false) }

    val shown = remember(vm.all, vm.type, vm.query, vm.sortByDate) {
        val q = vm.query.trim().lowercase()
        val list = vm.all.filter { r ->
            r.cover.isNotEmpty() &&
                (vm.type == "ALL" || r.type == vm.type) &&
                (q.isEmpty() || r.code.contains(q) || r.name.lowercase().contains(q) || r.title.lowercase().contains(q))
        }
        if (vm.sortByDate) list.sortedByDescending { it.date } else list
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("港股年報封面庫") },
                actions = {
                    TextButton(onClick = { vm.load() }) { Text("更新") }
                    TextButton(onClick = { showSettings = true }) { Text("設定") }
                }
            )
        }
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box {
                    OutlinedButton(
                        onClick = { yearMenu = true },
                        contentPadding = PaddingValues(horizontal = 12.dp)
                    ) {
                        Text(if (vm.year == 0) "年份" else "${vm.year} ▾")
                    }
                    DropdownMenu(expanded = yearMenu, onDismissRequest = { yearMenu = false }) {
                        vm.years.forEach { (y, n) ->
                            DropdownMenuItem(
                                text = { Text("$y 年（$n）") },
                                onClick = {
                                    yearMenu = false
                                    vm.load(y)
                                }
                            )
                        }
                    }
                }
                listOf("ALL" to "全部", "AR" to "年報", "IR" to "中期").forEach { (k, label) ->
                    FilterChip(
                        selected = vm.type == k,
                        onClick = { vm.type = k },
                        label = { Text(label) }
                    )
                }
            }
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = vm.query,
                    onValueChange = { vm.query = it },
                    placeholder = { Text("股份代號 / 公司名") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(6.dp))
                FilterChip(
                    selected = vm.sortByDate,
                    onClick = { vm.sortByDate = !vm.sortByDate },
                    label = { Text("最新先") }
                )
            }
            Text(
                "共 ${shown.size} 份",
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 2.dp)
            )

            when {
                vm.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                vm.error != null -> Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("載入失敗：${vm.error}")
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = { vm.load() }) { Text("再試") }
                    }
                }
                shown.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("未有封面")
                }
                else -> LazyVerticalGrid(
                    columns = GridCells.Adaptive(108.dp),
                    contentPadding = PaddingValues(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(shown, key = { it.id }) { r ->
                        CoverCell(r) { selected = r }
                    }
                }
            }
        }
    }

    selected?.let { r -> DetailDialog(r) { selected = null } }
    if (showSettings) {
        SettingsDialog(
            onDismiss = { showSettings = false },
            onSaved = {
                showSettings = false
                vm.load()
            }
        )
    }
}

@Composable
fun CoverCell(r: Report, onClick: () -> Unit) {
    Column(Modifier.clickable(onClick = onClick)) {
        AsyncImage(
            model = DataSource.url(r.cover),
            contentDescription = r.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.707f)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        )
        Text(
            "${r.code} · ${if (r.type == "AR") "年報" else "中期"}",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
        Text(
            r.name,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun DetailDialog(r: Report, onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize()) {
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AsyncImage(
                    model = DataSource.url(r.cover),
                    contentDescription = r.title,
                    contentScale = ContentScale.FillWidth,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                Text("${r.code} ${r.name}", style = MaterialTheme.typography.titleMedium)
                Text(r.title, style = MaterialTheme.typography.bodyMedium)
                Text("刊發日期：${r.date}", style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = {
                        scope.launch {
                            val ok = saveCover(ctx, r)
                            Toast.makeText(
                                ctx,
                                if (ok) "已儲存到 Pictures/年報封面" else "儲存失敗",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }) { Text("儲存封面") }
                    OutlinedButton(onClick = {
                        try {
                            ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(r.pdf)))
                        } catch (e: Exception) {
                            Toast.makeText(ctx, "開唔到連結", Toast.LENGTH_SHORT).show()
                        }
                    }) { Text("開報告 PDF") }
                    TextButton(onClick = onDismiss) { Text("關閉") }
                }
            }
        }
    }
}

@Composable
fun SettingsDialog(onDismiss: () -> Unit, onSaved: () -> Unit) {
    var repo by remember { mutableStateOf(DataSource.repo) }
    var token by remember { mutableStateOf(DataSource.token) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("設定") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = repo,
                    onValueChange = { repo = it },
                    label = { Text("封面資料 repo（owner/name）") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = token,
                    onValueChange = { token = it },
                    label = { Text("GitHub 唯讀 token（私人 repo 先要）") },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                DataSource.repo = repo
                DataSource.token = token
                onSaved()
            }) { Text("儲存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

suspend fun saveCover(ctx: Context, r: Report): Boolean = withContext(Dispatchers.IO) {
    try {
        val req = ImageRequest.Builder(ctx)
            .data(DataSource.url(r.cover))
            .allowHardware(false)
            .build()
        val bmp = (ctx.imageLoader.execute(req).drawable as? BitmapDrawable)?.bitmap
            ?: return@withContext false
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "${r.code}_${r.date}_${r.type}_${r.id}.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/年報封面")
        }
        val uri = ctx.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            ?: return@withContext false
        ctx.contentResolver.openOutputStream(uri)?.use { bmp.compress(Bitmap.CompressFormat.JPEG, 92, it) }
        true
    } catch (e: Exception) {
        false
    }
}
