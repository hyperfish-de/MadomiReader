package com.example.madomireader.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.madomireader.di.AppContainer
import com.example.madomireader.domain.model.GeminiModel
import com.example.madomireader.domain.model.OverlayStyle
import com.example.madomireader.domain.model.ReaderMode
import com.example.madomireader.domain.model.SourceLanguage
import kotlin.math.roundToInt

/**
 * Màn hình Cài đặt: API key Gemini, model, ngôn ngữ gốc, kiểu nền lớp dịch,
 * chế độ đọc mặc định, số cột lưới và dọn dữ liệu đọc.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    container: AppContainer,
    onBack: () -> Unit,
) {
    val viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.factory(container))
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.message) {
        val message = state.message
        if (!message.isNullOrBlank()) {
            snackbarHostState.showSnackbar(message)
            viewModel.consumeMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Cài đặt") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Quay lại",
                        )
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            // ------------------------------------------------------------ Gemini
            SectionTitle(text = "Gemini AI")

            OutlinedTextField(
                value = state.apiKeyInput,
                onValueChange = viewModel::setApiKeyInput,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(text = "Gemini API key") },
                placeholder = { Text(text = "AIza…") },
                singleLine = true,
                visualTransformation = if (state.apiKeyVisible) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done,
                ),
                trailingIcon = {
                    TextButton(onClick = viewModel::toggleApiKeyVisibility) {
                        Text(text = if (state.apiKeyVisible) "Ẩn" else "Hiện")
                    }
                },
                shape = RoundedCornerShape(14.dp),
            )

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Lấy key miễn phí tại aistudio.google.com/app/apikey. " +
                    "Key chỉ được lưu trên máy này.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = viewModel::saveApiKey,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(text = "Lưu key")
                }
                OutlinedButton(
                    onClick = viewModel::testApiKey,
                    enabled = !state.isTesting,
                    modifier = Modifier.weight(1f),
                ) {
                    if (state.isTesting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(text = "Kiểm tra")
                }
            }

            if (state.apiKeyInput.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                TextButton(onClick = viewModel::clearApiKey) {
                    Text(text = "Xoá API key khỏi máy")
                }
            }

            state.testResult?.let { result ->
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text(
                        text = result,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (state.testSucceeded) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.error
                        },
                        modifier = Modifier.padding(12.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            SettingLabel(
                title = "Model dịch",
                subtitle = "Flash cho chất lượng cao, Flash Lite cho tốc độ và tiết kiệm.",
            )
            ChipSelector(
                options = GeminiModel.entries.toList(),
                selected = state.settings.model,
                label = { it.label },
                onSelect = viewModel::setModel,
            )

            // -------------------------------------------------- nhận diện & dịch
            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
            SectionTitle(text = "Nhận diện chữ & dịch")

            SettingLabel(
                title = "Ngôn ngữ gốc của truyện",
                subtitle = "Chọn đúng ngôn ngữ sẽ nhận diện chữ nhanh và chính xác hơn. " +
                    "Tự động sẽ chạy cả bốn bộ nhận diện.",
            )
            ChipSelector(
                options = SourceLanguage.entries.toList(),
                selected = state.settings.sourceLanguage,
                label = { it.label },
                onSelect = viewModel::setSourceLanguage,
            )

            Spacer(modifier = Modifier.height(16.dp))
            SettingLabel(
                title = "Nền che chữ gốc",
                subtitle = "Kiểu nền vẽ đè lên chữ gốc trước khi ghi bản dịch.",
            )
            ChipSelector(
                options = OverlayStyle.entries.toList(),
                selected = state.settings.overlayStyle,
                label = { it.label },
                onSelect = viewModel::setOverlayStyle,
            )

            Spacer(modifier = Modifier.height(12.dp))
            SettingSwitch(
                title = "Tự động dịch trang đang xem",
                subtitle = "Mỗi khi mở một trang mới, tự động nhận diện chữ và dịch.",
                checked = state.settings.autoTranslate,
                onCheckedChange = viewModel::setAutoTranslate,
            )

            Spacer(modifier = Modifier.height(12.dp))
            SettingLabel(
                title = "Cỡ chữ bản dịch: ${"%.1f".format(state.settings.fontSizeScale)}×",
                subtitle = "Chữ trong bong bóng sẽ tự thu nhỏ cho vừa khung.",
            )
            Slider(
                value = state.settings.fontSizeScale,
                onValueChange = { viewModel.setFontSizeScale((it * 10f).roundToInt() / 10f) },
                valueRange = 0.6f..1.8f,
                steps = 11,
            )

            // -------------------------------------------------------------- đọc
            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
            SectionTitle(text = "Đọc truyện")

            SettingLabel(
                title = "Chế độ đọc mặc định",
                subtitle = "Áp dụng khi mở một chương mới.",
            )
            ChipSelector(
                options = ReaderMode.entries.toList(),
                selected = state.settings.defaultReaderMode,
                label = { it.label },
                onSelect = viewModel::setDefaultReaderMode,
            )

            Spacer(modifier = Modifier.height(12.dp))
            SettingLabel(
                title = "Số cột lưới truyện: ${state.settings.gridColumns}",
                subtitle = "Áp dụng cho màn hình Khám phá và Đã lưu.",
            )
            Slider(
                value = state.settings.gridColumns.toFloat(),
                onValueChange = { viewModel.setGridColumns(it.roundToInt()) },
                valueRange = 1f..4f,
                steps = 2,
            )

            // ------------------------------------------------------------ nguồn
            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
            SectionTitle(text = "Nguồn truyện")
            container.sourceRegistry.all.forEach { source ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = source.name,
                            style = MaterialTheme.typography.titleSmall,
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = source.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Muốn thêm nguồn mới: viết một class MangaSource rồi đăng ký trong " +
                    "SourceRegistry — không cần sửa giao diện.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            // ------------------------------------------------------------ dữ liệu
            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
            SectionTitle(text = "Dữ liệu")
            OutlinedButton(
                onClick = viewModel::clearReadingData,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = "Xoá lịch sử đọc và truyện đã lưu")
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Madomi Reader 1.0.0 • Kotlin + Jetpack Compose • " +
                    "OCR bằng ML Kit (chạy trên máy) • Dịch bằng Google Gemini.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 12.dp, bottom = 8.dp),
    )
}

@Composable
private fun SettingLabel(title: String, subtitle: String? = null) {
    Column(modifier = Modifier.padding(bottom = 8.dp)) {
        Text(text = title, style = MaterialTheme.typography.bodyLarge)
        if (!subtitle.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SettingSwitch(
    title: String,
    subtitle: String?,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            if (!subtitle.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun <T> ChipSelector(
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.forEach { option ->
            FilterChip(
                selected = option == selected,
                onClick = { onSelect(option) },
                label = { Text(text = label(option)) },
            )
        }
    }
}
