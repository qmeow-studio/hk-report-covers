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
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
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
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

const val ALL_YEARS = -1

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
                val y = when {
                    selectYear == ALL_YEARS && ys.isNotEmpty() -> ALL_YEARS
                    ys.any { it.first == selectYear } -> selectYear
                    else -> ys.firstOrNull()?.first ?: 0
                }
                year = y
                all = when (y) {
                    0 -> emptyList()
                    ALL_YEARS -> coroutineScope {
                        ys.map { async(Dispatchers.IO) { DataSource.reports(it.first) } }
                            .awaitAll().flatten().distinctBy { it.id }
                    }
                    else -> withContext(Dispatchers.IO) { DataSource.reports(y) }
                }
            } catch (e: Exception) {
                error = e.message ?: e.toString()
                all = emptyList()
            }
            loading = false
        }
    }
}

private val LightColors = lightColorScheme(
    primary = Color(0xFF1F3A5F),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD6E3FF),
    onPrimaryContainer = Color(0xFF001B3D),
    secondary = Color(0xFF545F71),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD8E3F8),
    onSecondaryContainer = Color(0xFF111C2B),
    tertiary = Color(0xFF9A6A12),
    onTertiary = Color.White,
    background = Color(0xFFF7F8FB),
    onBackground = Color(0xFF191C20),
    surface = Color(0xFFF7F8FB),
    onSurface = Color(0xFF191C20),
    surfaceVariant = Color(0xFFE0E2EC),
    onSurfaceVariant = Color(0xFF44474E),
    surfaceContainerLow = Color(0xFFF1F3F9),
    surfaceContainer = Color(0xFFECEEF4),
    surfaceContainerHigh = Color(0xFFE6E8EF),
    surfaceContainerHighest = Color(0xFFE0E2E9),
    outline = Color(0xFF74777F),
    outlineVariant = Color(0xFFD3D6DF),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFA9C7FF),
    onPrimary = Color(0xFF05305F),
    primaryContainer = Color(0xFF254777),
    onPrimaryContainer = Color(0xFFD6E3FF),
    secondary = Color(0xFFBCC7DB),
    onSecondary = Color(0xFF263141),
    secondaryContainer = Color(0xFF3C4758),
    onSecondaryContainer = Color(0xFFD8E3F8),
    tertiary = Color(0xFFF2BE6A),
    onTertiary = Color(0xFF432C00),
    background = Color(0xFF111318),
    onBackground = Color(0xFFE2E2E9),
    surface = Color(0xFF111318),
    onSurface = Color(0xFFE2E2E9),
    surfaceVariant = Color(0xFF44474E),
    onSurfaceVariant = Color(0xFFC4C6D0),
    surfaceContainerLow = Color(0xFF191C20),
    surfaceContainer = Color(0xFF1D2024),
    surfaceContainerHigh = Color(0xFF282A2F),
    surfaceContainerHighest = Color(0xFF33353A),
    outline = Color(0xFF8E9099),
    outlineVariant = Color(0xFF3A3D44),
)

private val EsgGreen = Color(0xFF2E7D32)

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        content = content
    )
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            AppTheme {
                MainScreen()
            }
        }
    }
}

fun typeLabel(type: String): String = when (type) {
    "AR" -> "年報"
    "IR" -> "中期"
    "ESG" -> "ESG"
    else -> type
}

@Composable
fun MainScreen(vm: MainVM = viewModel()) {
    var showSettings by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf<Report?>(null) }

    val shown = remember(vm.all, vm.type, vm.query, vm.sortByDate, vm.year) {
        val q = vm.query.trim().lowercase()
        val list = vm.all.filter { r ->
            r.cover.isNotEmpty() &&
                (vm.type == "ALL" || r.type == vm.type) &&
                (q.isEmpty() || r.code.contains(q) || r.name.lowercase().contains(q) || r.title.lowercase().contains(q))
        }
        when {
            vm.sortByDate -> list.sortedByDescending { it.date }
            vm.year == ALL_YEARS -> list.sortedWith(compareBy<Report> { it.code }.thenByDescending { it.date })
            else -> list
        }
    }

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            // 標題列
            Row(
                Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "年報封面庫",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { vm.load() }) {
                    Icon(Icons.Filled.Refresh, contentDescription = "更新")
                }
                IconButton(onClick = { showSettings = true }) {
                    Icon(Icons.Filled.Settings, contentDescription = "設定")
                }
            }

            // 搜尋
            SearchPill(
                value = vm.query,
                onChange = { vm.query = it },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)
            )

            // 年份 + 排序 + 數量
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                YearPill(vm)
                TogglePill(label = "最新先", selected = vm.sortByDate) { vm.sortByDate = !vm.sortByDate }
                Spacer(Modifier.weight(1f))
                Text(
                    "${shown.size} 份",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // 類型（四格平均分，一定啱闊度）
            SegmentedControl(
                options = listOf("ALL" to "全部", "AR" to "年報", "IR" to "中期", "ESG" to "ESG"),
                selected = vm.type,
                onSelect = { vm.type = it },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)
            )

            when {
                vm.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                vm.error != null -> Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("載入失敗", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            vm.error ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(14.dp))
                        Button(onClick = { vm.load() }) { Text("再試") }
                    }
                }
                shown.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Filled.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text("搵唔到封面", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                else -> LazyVerticalGrid(
                    columns = GridCells.Adaptive(108.dp),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(shown, key = { it.id }) { r ->
                        CoverCell(r, showYear = vm.year == ALL_YEARS) { selected = r }
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
fun SearchPill(value: String, onChange: (String) -> Unit, modifier: Modifier = Modifier) {
    TextField(
        value = value,
        onValueChange = onChange,
        placeholder = { Text("搜尋股份代號 / 公司名") },
        singleLine = true,
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
        trailingIcon = {
            if (value.isNotEmpty()) {
                IconButton(onClick = { onChange("") }) {
                    Icon(Icons.Filled.Close, contentDescription = "清除")
                }
            }
        },
        shape = CircleShape,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
        ),
        modifier = modifier
    )
}

@Composable
fun YearPill(vm: MainVM) {
    var open by remember { mutableStateOf(false) }
    Box {
        Row(
            Modifier
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer)
                .clickable { open = true }
                .padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                when (vm.year) {
                    0 -> "年份"
                    ALL_YEARS -> "全部年份"
                    else -> "${vm.year} 年"
                },
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Icon(
                Icons.Filled.ArrowDropDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            if (vm.years.size > 1) {
                DropdownMenuItem(
                    text = { Text("全部年份（${vm.years.sumOf { it.second }}）") },
                    onClick = {
                        open = false
                        vm.load(ALL_YEARS)
                    }
                )
            }
            vm.years.forEach { (y, n) ->
                DropdownMenuItem(
                    text = { Text("$y 年（$n）") },
                    onClick = {
                        open = false
                        vm.load(y)
                    }
                )
            }
        }
    }
}

@Composable
fun TogglePill(label: String, selected: Boolean, onClick: () -> Unit) {
    val bg = if (selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent
    val fg = if (selected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        Modifier
            .clip(CircleShape)
            .background(bg)
            .border(1.dp, if (selected) Color.Transparent else MaterialTheme.colorScheme.outlineVariant, CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (selected) {
            Icon(Icons.Filled.Check, contentDescription = null, tint = fg, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
        }
        Text(label, style = MaterialTheme.typography.labelLarge, color = fg)
    }
}

@Composable
fun SegmentedControl(
    options: List<Pair<String, String>>,
    selected: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(4.dp)
    ) {
        options.forEach { (key, label) ->
            val on = key == selected
            Box(
                Modifier
                    .weight(1f)
                    .clip(CircleShape)
                    .background(if (on) MaterialTheme.colorScheme.primary else Color.Transparent)
                    .clickable { onSelect(key) }
                    .padding(vertical = 9.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    label,
                    maxLines = 1,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (on) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun TypeBadge(type: String, modifier: Modifier = Modifier) {
    val (bg, fg) = when (type) {
        "AR" -> MaterialTheme.colorScheme.primary to MaterialTheme.colorScheme.onPrimary
        "IR" -> MaterialTheme.colorScheme.tertiary to MaterialTheme.colorScheme.onTertiary
        "ESG" -> EsgGreen to Color.White
        else -> MaterialTheme.colorScheme.secondary to MaterialTheme.colorScheme.onSecondary
    }
    Text(
        typeLabel(type),
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.SemiBold,
        color = fg,
        modifier = modifier
            .clip(CircleShape)
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 2.dp)
    )
}

@Composable
fun CoverCell(r: Report, showYear: Boolean = false, onClick: () -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    Column(Modifier.clip(shape).clickable(onClick = onClick)) {
        Box {
            AsyncImage(
                model = DataSource.url(r.cover),
                contentDescription = r.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.707f)
                    .clip(shape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
            )
            TypeBadge(r.type, Modifier.align(Alignment.TopStart).padding(8.dp))
        }
        Spacer(Modifier.height(6.dp))
        Text(
            if (showYear) "${r.code} · ${r.date.take(4)}" else r.code,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
        Text(
            r.name,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 4.dp, end = 4.dp, bottom = 4.dp)
        )
    }
}

@Composable
fun DetailDialog(r: Report, onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var zoom by remember { mutableStateOf(false) }
    if (zoom) ZoomDialog(r) { zoom = false }
    val shape = RoundedCornerShape(18.dp)
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(Modifier.fillMaxSize()) {
                Row(
                    Modifier.fillMaxWidth().padding(start = 4.dp, end = 16.dp, top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = "關閉")
                    }
                    Column(Modifier.weight(1f)) {
                        Text(
                            "${r.code} ${r.name}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            "刊發日期 ${r.date}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    TypeBadge(r.type)
                }
                Column(
                    Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    AsyncImage(
                        model = DataSource.url(r.cover),
                        contentDescription = r.title,
                        contentScale = ContentScale.FillWidth,
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(10.dp, shape)
                            .clip(shape)
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                            .clickable { zoom = true }
                    )
                    Spacer(Modifier.height(14.dp))
                    Text(r.title, style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "撳圖片可以放大",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = {
                            scope.launch {
                                val ok = saveCover(ctx, r)
                                Toast.makeText(
                                    ctx,
                                    if (ok) "已儲存到 Pictures/年報封面" else "儲存失敗",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) { Text("儲存封面") }
                    FilledTonalButton(
                        onClick = {
                            try {
                                ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(r.pdf)))
                            } catch (e: Exception) {
                                Toast.makeText(ctx, "開唔到連結", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) { Text("開報告 PDF") }
                }
            }
        }
    }
}

@Composable
fun ZoomDialog(r: Report, onDismiss: () -> Unit) {
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black)
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, gestureZoom, _ ->
                        val newScale = (scale * gestureZoom).coerceIn(1f, 6f)
                        val maxX = size.width * (newScale - 1f) / 2f
                        val maxY = size.height * (newScale - 1f) / 2f
                        scale = newScale
                        offset = Offset(
                            (offset.x + pan.x).coerceIn(-maxX, maxX),
                            (offset.y + pan.y).coerceIn(-maxY, maxY)
                        )
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures(
                        onDoubleTap = {
                            if (scale > 1f) {
                                scale = 1f
                                offset = Offset.Zero
                            } else {
                                scale = 3f
                            }
                        },
                        onTap = { if (scale == 1f) onDismiss() }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = DataSource.url(r.cover),
                contentDescription = r.title,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        translationX = offset.x
                        translationY = offset.y
                    }
            )
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
