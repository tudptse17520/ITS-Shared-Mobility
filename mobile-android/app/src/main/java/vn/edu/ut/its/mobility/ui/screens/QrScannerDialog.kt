package vn.edu.ut.its.mobility.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color as AndroidColor
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.QrCode
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.MultiFormatWriter
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import vn.edu.ut.its.mobility.ui.ItsButton
import vn.edu.ut.its.mobility.ui.ItsNotice
import vn.edu.ut.its.mobility.ui.extractVehicleCodeFromQr

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QrScannerDialog(
    availableCodes: List<String>,
    initialCode: String? = null,
    onDismiss: () -> Unit,
    onConfirmCode: (String) -> Unit
) {
    val defaultCode = initialCode ?: availableCodes.firstOrNull() ?: "ITS-EB-001"
    var codeInput by remember { mutableStateOf(defaultCode) }
    var decodedStatus by remember { mutableStateOf<String?>(null) }
    var isDecodingError by remember { mutableStateOf(false) }

    var isConfirming by remember { mutableStateOf(false) }
    var showSample by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val normalizedPreviewCode = remember(codeInput) {
        extractVehicleCodeFromQr(codeInput).ifBlank { "ITS-EB-001" }
    }
    val qrPayload = "its://unlock?code=$normalizedPreviewCode"
    val qrBitmap = remember(qrPayload) { generateQrBitmap(qrPayload, sizePx = 360) }

    fun handleDecodedBitmap(bitmap: Bitmap) {
        if (isConfirming) return
        val rawDecoded = decodeQrFromBitmap(bitmap)
        if (rawDecoded.isNullOrBlank()) {
            isDecodingError = true
            decodedStatus = "Không tìm thấy mã QR hợp lệ trong ảnh."
            return
        }
        val extracted = extractVehicleCodeFromQr(rawDecoded)
        if (extracted.isBlank()) {
            isDecodingError = true
            decodedStatus = "Mã QR không chứa mã xe hợp lệ."
            return
        }
        isDecodingError = false
        codeInput = extracted
        decodedStatus = "Đã nhận mã $extracted"
        isConfirming = true
        scope.launch {
            delay(500)
            onConfirmCode(extracted)
        }
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            val bmp = runCatching {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream)
                }
            }.getOrNull()
            if (bmp != null) {
                handleDecodedBitmap(bmp)
            } else {
                isDecodingError = true
                decodedStatus = "Không thể đọc tệp ảnh đã chọn."
            }
        }
    }

    val colors = MaterialTheme.colorScheme
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = colors.surface,
    ) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(start = 18.dp, end = 18.dp, bottom = 18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Mở khóa bằng mã QR", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("Chọn ảnh mã QR của xe hoặc nhập mã xe.", style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
            OutlinedTextField(codeInput, { codeInput = it }, label = { Text("Mã xe") }, singleLine = true,
                enabled = !isConfirming, modifier = Modifier.fillMaxWidth())
            if (availableCodes.isNotEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(availableCodes.take(5), key = { it }) { code ->
                        FilterChip(selected = codeInput == code, onClick = { codeInput = code },
                            enabled = !isConfirming, label = { Text(code) })
                    }
                }
            }
            ItsNotice(decodedStatus, isError = isDecodingError)
            ItsButton(
                onClick = { onConfirmCode(codeInput) },
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                enabled = codeInput.isNotBlank() && !isConfirming,
            ) { Text("Mở khóa") }
            ItsButton(
                onClick = { imagePickerLauncher.launch("image/*") },
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                enabled = !isConfirming,
                outlined = true,
            ) {
                Icon(Icons.Outlined.PhotoLibrary, null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Chọn ảnh QR")
            }
            HorizontalDivider()
            TextButton(onClick = { showSample = !showSample }, enabled = !isConfirming) {
                Icon(Icons.Outlined.QrCode, null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(if (showSample) "Ẩn mã QR dùng thử" else "Xem mã QR dùng thử")
            }
            AnimatedVisibility(showSample) {
                if (qrBitmap != null) {
                    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        Image(qrBitmap.asImageBitmap(), "Mã QR $normalizedPreviewCode",
                            Modifier.background(Color.White, MaterialTheme.shapes.medium).padding(10.dp).size(132.dp))
                        TextButton(onClick = { handleDecodedBitmap(qrBitmap) }, enabled = !isConfirming) { Text("Dùng mã này") }
                    }
                }
            }
        }
    }
}

private fun generateQrBitmap(content: String, sizePx: Int): Bitmap? {
    return runCatching {
        val bitMatrix = MultiFormatWriter().encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx)
        val width = bitMatrix.width
        val height = bitMatrix.height
        val pixels = IntArray(width * height)
        for (y in 0 until height) {
            val offset = y * width
            for (x in 0 until width) {
                pixels[offset + x] = if (bitMatrix[x, y]) AndroidColor.BLACK else AndroidColor.WHITE
            }
        }
        Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply {
            setPixels(pixels, 0, width, 0, 0, width, height)
        }
    }.getOrNull()
}

private fun decodeQrFromBitmap(bitmap: Bitmap): String? {
    return runCatching {
        val softwareBitmap = if (bitmap.config == Bitmap.Config.HARDWARE) {
            bitmap.copy(Bitmap.Config.ARGB_8888, false)
        } else {
            bitmap
        }
        val w = softwareBitmap.width
        val h = softwareBitmap.height
        val pixels = IntArray(w * h)
        softwareBitmap.getPixels(pixels, 0, w, 0, 0, w, h)
        val source = RGBLuminanceSource(w, h, pixels)
        val binaryBitmap = BinaryBitmap(HybridBinarizer(source))
        QRCodeReader().decode(binaryBitmap).text
    }.getOrNull()
}

