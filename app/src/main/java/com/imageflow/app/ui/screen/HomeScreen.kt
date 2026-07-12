package com.imageflow.app.ui.screen

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.imageflow.app.data.entity.HistoryEntity
import com.imageflow.app.network.GeminiModel
import com.imageflow.app.util.TimeFormatter
import com.imageflow.app.viewmodel.GenerateViewModel
import com.imageflow.app.viewmodel.SettingsViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    generateVm: GenerateViewModel,
    settingsVm: SettingsViewModel,
    onOpenSettings: () -> Unit,
    onOpenHistory: (Long) -> Unit
) {
    val prompt by generateVm.prompt.collectAsState()
    val inputImages by generateVm.inputImages.collectAsState()
    val selectedModelId by generateVm.selectedModelId.collectAsState()
    val aspectRatio by generateVm.aspectRatio.collectAsState()
    val imageSize by generateVm.imageSize.collectAsState()
    val outputMime by generateVm.outputMime.collectAsState()
    val isGenerating by generateVm.isGenerating.collectAsState()
    val lastError by generateVm.lastError.collectAsState()
    val recentHistory by generateVm.recentHistory.collectAsState()
    val apiKey by settingsVm.apiKey.collectAsState()

    val context = LocalContext.current
    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        uris.forEach { generateVm.addInputImage(it) }
    }

    var showParamsSheet by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("ImageFlow", style = MaterialTheme.typography.headlineMedium)
                        Text(
                            GeminiModel.byId(selectedModelId).displayName,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, "设置")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            // === 主画布区（占大部分空间）===
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                if (recentHistory.isEmpty()) {
                    EmptyCanvas()
                } else {
                    // 显示最近一次生成的结果
                    val latest = recentHistory.first()
                    CanvasDisplay(
                        history = latest,
                        onClick = { onOpenHistory(latest.id) }
                    )
                }

                // 生成中遮罩
                if (isGenerating) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(16.dp))
                            .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.7f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.height(12.dp))
                            Text("生成中…", color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }

            // === 错误提示 ===
            lastError?.let { err ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = err,
                        modifier = Modifier.padding(12.dp),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            // === 输入图缩略图条（如果有上传）===
            if (inputImages.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(inputImages) { index, img ->
                        Box {
                            // 输入图是 base64，需要转 data uri 让 Coil 加载
                            val dataUri = remember(img) {
                                "data:${img.mimeType};base64,${img.base64Data}"
                            }
                            AsyncImage(
                                model = dataUri,
                                contentDescription = "输入图 ${index + 1}",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                            )
                            // 删除按钮
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.error)
                                    .clickable { generateVm.removeInputImage(index) },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Filled.Close,
                                    contentDescription = "删除",
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    }
                }
            }

            // === 输入区 ===
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    OutlinedTextField(
                        value = prompt,
                        onValueChange = generateVm::updatePrompt,
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = {
                            Text(
                                if (apiKey.isBlank()) "请先去设置页填写 API Key…"
                                else "描述你想要的图片…",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        minLines = 2,
                        maxLines = 5,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )

                    Spacer(Modifier.height(8.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // 上传图片
                        IconButton(
                            onClick = { photoPicker.launch("image/*") },
                            enabled = inputImages.size < GeminiModel.byId(selectedModelId).maxInputImages
                        ) {
                            Icon(Icons.Filled.AddPhotoAlternate, "上传图片")
                        }
                        // 参数按钮
                        IconButton(onClick = { showParamsSheet = true }) {
                            Icon(Icons.Filled.Tune, "参数")
                        }
                        Spacer(Modifier.weight(1f))
                        // 生成按钮
                        Button(
                            onClick = { generateVm.generate() },
                            enabled = !isGenerating && prompt.isNotBlank() && apiKey.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            shape = RoundedCornerShape(50)
                        ) {
                            Icon(Icons.Filled.AutoAwesome, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("生成")
                        }
                    }

                    // 参数概览
                    Text(
                        text = "${GeminiModel.byId(selectedModelId).displayName} · ${aspectRatio} · ${imageSize} · ${if (outputMime == "image/png") "PNG" else "JPEG"}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // === 历史时间线（最近 20 条缩略图）===
            if (recentHistory.isNotEmpty()) {
                Text(
                    "最近生成",
                    modifier = Modifier.padding(start = 16.dp, top = 4.dp, bottom = 4.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(recentHistory.take(20)) { h ->
                        HistoryThumb(history = h, onClick = { onOpenHistory(h.id) })
                    }
                }
            }
        }
    }

    // 参数面板（底部弹窗）
    if (showParamsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showParamsSheet = false },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            ParamSheetContent(generateVm)
        }
    }
}

@Composable
private fun EmptyCanvas() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Filled.Image,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "还没有生成过图片",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "在下方输入描述，点「生成」开始",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CanvasDisplay(history: HistoryEntity, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (history.success && history.outputImagePath != null) {
            AsyncImage(
                model = File(history.outputImagePath),
                contentDescription = "生成结果",
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Text(
                text = history.errorMessage ?: "生成失败",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun HistoryThumb(history: HistoryEntity, onClick: () -> Unit) {
    val path = history.outputImagePath
    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
    ) {
        if (path != null) {
            AsyncImage(
                model = File(path),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.ErrorOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
private fun ParamSheetContent(vm: GenerateViewModel) {
    val selectedModelId by vm.selectedModelId.collectAsState()
    val aspectRatio by vm.aspectRatio.collectAsState()
    val imageSize by vm.imageSize.collectAsState()
    val outputMime by vm.outputMime.collectAsState()
    val googleSearch by vm.enableGoogleSearch.collectAsState()

    val model = GeminiModel.byId(selectedModelId)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("生成参数", style = MaterialTheme.typography.titleLarge)

        // 模型选择
        Text("模型", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            GeminiModel.ALL.forEach { m ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable { vm.selectModel(m.id) }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = m.id == selectedModelId,
                        onClick = { vm.selectModel(m.id) },
                        colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary)
                    )
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(m.displayName, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "最多 ${m.maxInputImages} 张输入 · ${m.supportedImageSizes.joinToString("/")}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Aspect Ratio
        Text("宽高比", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        ChipRowFlex(model.supportedAspectRatios, aspectRatio, vm::setAspectRatio)

        // Image Size
        Text("分辨率", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        ChipRowFlex(model.supportedImageSizes, imageSize, vm::setImageSize)

        // Output
        Text("输出格式", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        ChipRowFlex(listOf("image/png", "image/jpeg"), outputMime, vm::setOutputMime) {
            if (it == "image/png") "PNG" else "JPEG"
        }

        // Google Search
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("启用 Google 搜索", style = MaterialTheme.typography.bodyLarge)
            Switch(
                checked = googleSearch,
                onCheckedChange = vm::toggleGoogleSearch,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                    checkedTrackColor = MaterialTheme.colorScheme.primary
                )
            )
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun ChipRowFlex(
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
    displayTransform: (String) -> String = { it }
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { opt ->
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(
                        if (opt == selected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
                    .clickable { onSelect(opt) }
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text(
                    text = displayTransform(opt),
                    color = if (opt == selected) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}
