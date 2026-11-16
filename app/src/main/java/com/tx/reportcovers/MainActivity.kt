@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)

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
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

// iOS 風格配色
private val LightColors = lightColorScheme(
    primary = Color(0xFF007AFF),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE5F1FF),
    onPrimaryContainer = Color(0xFF007AFF),
    secondary = Color(0xFF6C6C70),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE4E4EA),
    onSecondaryContainer = Color(0xFF007AFF),
    tertiary = Color(0xFFFF9500),
    onTertiary = Color.White,
    background = Color(0xFFF2F2F7),
    onBackground = Color.Black,
    surface = Color(0xFFF2F2F7),
    onSurface = Color.Black,
    surfaceVariant = Color(0xFFE3E3E8),
    onSurfaceVariant = Color(0xFF6C6C70),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color.White,
    surfaceContainer = Color.White,
    surfaceContainerHigh = Color(0xFFE3E3E8),
    surfaceContainerHighest = Color(0xFFE3E3E8),
    outline = Color(0xFF8E8E93),
    outlineVariant = Color(0xFFD1D1D6),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF0A84FF),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF0A2A4D),
    onPrimaryContainer = Color(0xFF0A84FF),
    secondary = Color(0xFF98989F),
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF2C2C2E),
    onSecondaryContainer = Color(0xFF0A84FF),
    tertiary = Color(0xFFFF9F0A),
    onTertiary = Color.White,
    background = Color.Black,
    onBackground = Color.White,
    surface = Color.Black,
    onSurface = Color.White,
    surfaceVariant = Color(0xFF2C2C2E),
    onSurfaceVariant = Color(0xFF98989F),
    surfaceContainerLowest = Color(0xFF1C1C1E),
    surfaceContainerLow = Color(0xFF1C1C1E),
    surfaceContainer = Color(0xFF2C2C2E),
    surfaceContainerHigh = Color(0xFF1C1C1E),
    surfaceContainerHighest = Color(0xFF2C2C2E),
    outline = Color(0xFF636366),
    outlineVariant = Color(0xFF38383A),
)

private val EsgGreen = Color(0xFF34C759)

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
    var selecting by remember { mutableStateOf(false) }
    val picked = remember { mutableStateListOf<Report>() }
    var showCollage by remember { mutableStateOf(false) }
    val ctx = LocalContext.current

    BackHandler(enabled = selecting) {
        selecting = false
        picked.clear()
    }

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

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (selecting) {
                Surface(color = MaterialTheme.colorScheme.surfaceContainerLow, shadowElevation = 8.dp) {
                    Row(
                        Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 20.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            if (picked.size < 2) "揀 2 個或以上封面" else "已選 ${picked.size} 個",
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            onClick = { showCollage = true },
                            enabled = picked.size >= 2,
                            shape = RoundedCornerShape(13.dp)
                        ) { Text("拼貼", fontSize = 16.sp, fontWeight = FontWeight.SemiBold) }
                    }
                }
            }
        }
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            // 大標題
            Row(
                Modifier.fillMaxWidth().padding(start = 20.dp, end = 6.dp, top = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    if (selecting) "已選 ${picked.size} 個" else "年報封面庫",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (selecting) {
                    TextButton(onClick = {
                        selecting = false
                        picked.clear()
                    }) { Text("取消", fontSize = 17.sp, fontWeight = FontWeight.SemiBold) }
                } else {
                    TextButton(
                        onClick = { selecting = true },
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) { Text("選取", fontSize = 17.sp) }
                    IconButton(onClick = { vm.load() }, modifier = Modifier.size(42.dp)) {
                        Icon(Icons.Filled.Refresh, contentDescription = "更新", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = { showSettings = true }, modifier = Modifier.size(42.dp)) {
                        Icon(Icons.Filled.Settings, contentDescription = "設定", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            SearchField(
                value = vm.query,
                onChange = { vm.query = it },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)
            )

            SegmentedControl(
                options = listOf("ALL" to "全部", "AR" to "年報", "IR" to "中期", "ESG" to "ESG"),
                selected = vm.type,
                onSelect = { vm.type = it },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)
            )

            // 年份 + 排序 + 數量
            Row(
                Modifier.fillMaxWidth().padding(start = 8.dp, end = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                YearMenu(vm)
                TextToggle(label = "最新先", selected = vm.sortByDate) { vm.sortByDate = !vm.sortByDate }
                Spacer(Modifier.weight(1f))
                Text(
                    "${shown.size} 份",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            when {
                vm.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(strokeWidth = 3.dp, color = MaterialTheme.colorScheme.outline)
                }
                vm.error != null -> Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("載入失敗", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            vm.error ?: "",
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(14.dp))
                        TextButton(onClick = { vm.load() }) { Text("再試", fontSize = 17.sp) }
                    }
                }
                shown.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Filled.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(44.dp)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text("冇結果", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Text("試下其他代號或者年份", fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                else -> LazyVerticalGrid(
                    columns = GridCells.Adaptive(108.dp),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 28.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(shown, key = { it.id }) { r ->
                        CoverCell(
                            r,
                            showYear = vm.year == ALL_YEARS,
                            selecting = selecting,
                            order = if (selecting) picked.indexOf(r) + 1 else 0,
                            onClick = {
                                if (!selecting) {
                                    selected = r
                                } else if (picked.contains(r)) {
                                    picked.remove(r)
                                } else if (picked.size < MAX_COLLAGE) {
                                    picked.add(r)
                                } else {
                                    Toast.makeText(ctx, "最多揀 $MAX_COLLAGE 個", Toast.LENGTH_SHORT).show()
                                }
                            },
                            onLongClick = {
                                if (!selecting) {
                                    selecting = true
                                    picked.add(r)
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    selected?.let { r -> DetailSheet(r) { selected = null } }
    if (showCollage) {
        CollageDialog(picked.toList()) { showCollage = false }
    }
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
fun SearchField(value: String, onChange: (String) -> Unit, modifier: Modifier = Modifier) {
    TextField(
        value = value,
        onValueChange = onChange,
        placeholder = { Text("搜尋", color = MaterialTheme.colorScheme.onSurfaceVariant) },
        singleLine = true,
        leadingIcon = {
            Icon(Icons.Filled.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        },
        trailingIcon = {
            if (value.isNotEmpty()) {
                IconButton(onClick = { onChange("") }) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = "清除",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        },
        shape = RoundedCornerShape(12.dp),
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
fun YearMenu(vm: MainVM) {
    var open by remember { mutableStateOf(false) }
    Box {
        Row(
            Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable { open = true }
                .padding(start = 12.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                when (vm.year) {
                    0 -> "年份"
                    ALL_YEARS -> "全部年份"
                    else -> "${vm.year} 年"
                },
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
            Icon(
                Icons.Filled.ArrowDropDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        }
        DropdownMenu(
            expanded = open,
            onDismissRequest = { open = false },
            shape = RoundedCornerShape(14.dp),
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ) {
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
fun TextToggle(label: String, selected: Boolean, onClick: () -> Unit) {
    val fg = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (selected) {
            Icon(Icons.Filled.Check, contentDescription = null, tint = fg, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(3.dp))
        }
        Text(label, fontSize = 15.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal, color = fg)
    }
}

@Composable
fun SegmentedControl(
    options: List<Pair<String, String>>,
    selected: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val thumb = if (isSystemInDarkTheme()) Color(0xFF636366) else Color.White
    val track = RoundedCornerShape(9.dp)
    val knob = RoundedCornerShape(7.dp)
    Row(
        modifier
            .clip(track)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(2.dp)
    ) {
        options.forEach { (key, label) ->
            val on = key == selected
            Box(
                Modifier
                    .weight(1f)
                    .then(if (on) Modifier.shadow(2.dp, knob) else Modifier)
                    .clip(knob)
                    .background(if (on) thumb else Color.Transparent)
                    .clickable { onSelect(key) }
                    .padding(vertical = 7.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    label,
                    maxLines = 1,
                    fontSize = 14.sp,
                    fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
fun TypeBadge(type: String, modifier: Modifier = Modifier) {
    val bg = when (type) {
        "AR" -> MaterialTheme.colorScheme.primary
        "IR" -> MaterialTheme.colorScheme.tertiary
        "ESG" -> EsgGreen
        else -> MaterialTheme.colorScheme.secondary
    }
    Text(
        typeLabel(type),
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        color = Color.White,
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .padding(horizontal = 6.dp, vertical = 1.dp)
    )
}

@Composable
fun CoverCell(
    r: Report,
    showYear: Boolean = false,
    selecting: Boolean = false,
    order: Int = 0,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {}
) {
    val shape = RoundedCornerShape(10.dp)
    Column(Modifier.combinedClickable(onClick = onClick, onLongClick = onLongClick)) {
        Box {
            AsyncImage(
                model = DataSource.url(r.cover),
                contentDescription = r.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.707f)
                    .shadow(5.dp, shape)
                    .clip(shape)
                    .background(MaterialTheme.colorScheme.surfaceContainerLow)
            )
            TypeBadge(r.type, Modifier.align(Alignment.TopStart).padding(7.dp))
            if (selecting) {
                val on = order > 0
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(7.dp)
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(if (on) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.7f))
                        .border(1.5.dp, Color.White, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (on) {
                        Text("$order", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
        Spacer(Modifier.height(7.dp))
        Text(
            r.name,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 2.dp)
        )
        Text(
            if (showYear) "${r.code} · ${r.date.take(4)}" else r.code,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 2.dp)
        )
    }
}

@Composable
fun DetailSheet(r: Report, onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var zoom by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val shape = RoundedCornerShape(14.dp)
    val buttonShape = RoundedCornerShape(13.dp)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        containerColor = MaterialTheme.colorScheme.background
    ) {
        if (zoom) ZoomDialog(r) { zoom = false }
        Column(Modifier.fillMaxWidth()) {
            Row(
                Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        r.name,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        "${r.code} · ${typeLabel(r.type)} · ${r.date}",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                TextButton(onClick = onDismiss) {
                    Text("完成", fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            Column(
                Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 28.dp, vertical = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AsyncImage(
                    model = DataSource.url(r.cover),
                    contentDescription = r.title,
                    contentScale = ContentScale.FillWidth,
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(14.dp, shape)
                        .clip(shape)
                        .background(MaterialTheme.colorScheme.surfaceContainerLow)
                        .clickable { zoom = true }
                )
                Spacer(Modifier.height(14.dp))
                Text(r.title, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(2.dp))
                Text("撳圖片可以放大", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row(
                Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 6.dp, bottom = 16.dp),
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
                    shape = buttonShape,
                    contentPadding = PaddingValues(vertical = 14.dp),
                    modifier = Modifier.weight(1f)
                ) { Text("儲存封面", fontSize = 16.sp, fontWeight = FontWeight.SemiBold) }
                FilledTonalButton(
                    onClick = {
                        try {
                            ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(r.pdf)))
                        } catch (e: Exception) {
                            Toast.makeText(ctx, "開唔到連結", Toast.LENGTH_SHORT).show()
                        }
                    },
                    shape = buttonShape,
                    contentPadding = PaddingValues(vertical = 14.dp),
                    modifier = Modifier.weight(1f)
                ) { Text("開報告 PDF", fontSize = 16.sp, fontWeight = FontWeight.SemiBold) }
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
                Spacer(Modifier.height(4.dp))
                Text(
                    "年報封面庫 v${BuildConfig.VERSION_NAME}\n製作：Terence",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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

const val MAX_COLLAGE = 40

@Composable
fun CollageDialog(items: List<Report>, onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var cols by remember { mutableStateOf(if (items.size <= 4) "2" else if (items.size <= 9) "3" else "4") }
    var style by remember { mutableStateOf("WHITE") }
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var busy by remember { mutableStateOf(true) }

    LaunchedEffect(cols, style) {
        busy = true
        bitmap = buildCollage(
            ctx, items, cols.toInt(),
            gap = if (style == "NONE") 0 else 24,
            background = if (style == "BLACK") android.graphics.Color.BLACK else android.graphics.Color.WHITE
        )
        busy = false
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(Modifier.fillMaxSize()) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) { Text("取消", fontSize = 17.sp) }
                    Text(
                        "拼貼（${items.size} 個）",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    TextButton(
                        enabled = !busy && bitmap != null,
                        onClick = {
                            val bmp = bitmap ?: return@TextButton
                            scope.launch {
                                val ok = saveBitmap(ctx, bmp, "collage_${System.currentTimeMillis()}.jpg")
                                Toast.makeText(
                                    ctx,
                                    if (ok) "已儲存到 Pictures/年報封面" else "儲存失敗",
                                    Toast.LENGTH_SHORT
                                ).show()
                                if (ok) onDismiss()
                            }
                        }
                    ) { Text("儲存", fontSize = 17.sp, fontWeight = FontWeight.SemiBold) }
                }
                Box(
                    Modifier.weight(1f).fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val bmp = bitmap
                    if (bmp != null) {
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = "拼貼預覽",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    if (busy) {
                        CircularProgressIndicator(strokeWidth = 3.dp, color = MaterialTheme.colorScheme.outline)
                    }
                }
                Text(
                    "每行幾多個",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 20.dp, top = 4.dp)
                )
                SegmentedControl(
                    options = listOf("2" to "2", "3" to "3", "4" to "4", "5" to "5"),
                    selected = cols,
                    onSelect = { cols = it },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)
                )
                Text(
                    "邊框",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 20.dp, top = 4.dp)
                )
                SegmentedControl(
                    options = listOf("WHITE" to "白邊", "BLACK" to "黑邊", "NONE" to "冇邊"),
                    selected = style,
                    onSelect = { style = it },
                    modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 20.dp)
                )
            }
        }
    }
}

suspend fun loadCoverBitmap(ctx: Context, r: Report): Bitmap? {
    val req = ImageRequest.Builder(ctx)
        .data(DataSource.url(r.cover))
        .allowHardware(false)
        .build()
    return (ctx.imageLoader.execute(req).drawable as? BitmapDrawable)?.bitmap
}

/** 將封面按次序排成格仔，每格 A4 比例，中間裁切。最後一行唔夠數會置中。 */
suspend fun buildCollage(ctx: Context, items: List<Report>, cols: Int, gap: Int, background: Int): Bitmap? =
    withContext(Dispatchers.IO) {
        try {
            val cellW = when {
                cols <= 2 -> 600
                cols == 3 -> 520
                cols == 4 -> 420
                else -> 340
            }
            val cellH = (cellW * 1.414f).toInt()
            val rows = (items.size + cols - 1) / cols
            val out = Bitmap.createBitmap(
                cols * cellW + (cols + 1) * gap,
                rows * cellH + (rows + 1) * gap,
                Bitmap.Config.ARGB_8888
            )
            val canvas = android.graphics.Canvas(out)
            canvas.drawColor(background)
            val paint = android.graphics.Paint(
                android.graphics.Paint.FILTER_BITMAP_FLAG or android.graphics.Paint.ANTI_ALIAS_FLAG
            )
            val dstRatio = cellW / cellH.toFloat()
            items.forEachIndexed { i, r ->
                val bmp = loadCoverBitmap(ctx, r) ?: return@forEachIndexed
                val src = if (bmp.width / bmp.height.toFloat() > dstRatio) {
                    val sw = (bmp.height * dstRatio).toInt()
                    android.graphics.Rect((bmp.width - sw) / 2, 0, (bmp.width + sw) / 2, bmp.height)
                } else {
                    val sh = (bmp.width / dstRatio).toInt()
                    android.graphics.Rect(0, (bmp.height - sh) / 2, bmp.width, (bmp.height + sh) / 2)
                }
                val row = i / cols
                val col = i % cols
                val inRow = if (row == rows - 1) items.size - row * cols else cols
                val shift = (cols - inRow) * (cellW + gap) / 2
                val left = gap + col * (cellW + gap) + shift
                val top = gap + row * (cellH + gap)
                canvas.drawBitmap(bmp, src, android.graphics.Rect(left, top, left + cellW, top + cellH), paint)
            }
            out
        } catch (e: Throwable) {
            null
        }
    }

suspend fun saveBitmap(ctx: Context, bmp: Bitmap, fileName: String): Boolean = withContext(Dispatchers.IO) {
    try {
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
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

suspend fun saveCover(ctx: Context, r: Report): Boolean {
    val bmp = (try {
        loadCoverBitmap(ctx, r)
    } catch (e: Exception) {
        null
    }) ?: return false
    return saveBitmap(ctx, bmp, "${r.code}_${r.date}_${r.type}_${r.id}.jpg")
}
