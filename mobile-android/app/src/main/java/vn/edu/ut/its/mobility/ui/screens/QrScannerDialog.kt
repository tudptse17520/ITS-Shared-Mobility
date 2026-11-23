package vn.edu.ut.its.mobility.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color as AndroidColor
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.QrCodeScanner
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
import vn.edu.ut.its.mobility.ui.extractVehicleCodeFromQr

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

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val normalizedPreviewCode = remember(codeInput) {
        extractVehicleCodeFromQr(codeInput).ifBlank { "ITS-EB-001" }
    }
    val qrPayload = "its://unlock?code=$normalizedPreviewCode"
    val qrBitmap = remember(qrPayload) { generateQrBitmap(qrPayload, sizePx = 360) }

    fun handleDecodedBitmap(bitmap: Bitmap) {
        val rawDecoded = decodeQrFromBitmap(bitmap)
        if (rawDecoded.isNullOrBlank()) {
            isDecodingError = true
            decodedStatus = "Không tìm thấy mã QR hợp lệ trong ảnh."
            return
        }
        val extracted = extractVehicleCodeFromQr(rawDecoded)
        if (extracted.isBlank()) {
            isDecodingError = true
            decodedStatus = "Mã QR không chứa mã phương tiện ITS hợp lệ: $rawDecoded"
            return
        }
        isDecodingError = false
        codeInput = extracted
        decodedStatus = "Đã quét: $rawDecoded -> Mã xe: $extracted"
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

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Quét mã QR Mở khóa xe", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF0F172A),
                    border = BorderStroke(2.dp, Color(0xFF14B8A6)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Tem QR thực tế của xe (Quét thử trên máy ảo)",
                            color = Color(0xFF5EEAD4),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )

                        if (qrBitmap != null) {
                            Box(
                                modifier = Modifier
                                    .background(Color.White, RoundedCornerShape(12.dp))
                                    .padding(8.dp)
                            ) {
                                Image(
                                    bitmap = qrBitmap.asImageBitmap(),
                                    contentDescription = "Mã QR $normalizedPreviewCode",
                                    modifier = Modifier.size(148.dp)
                                )
                            }
                        }

                        Text(
                            text = qrPayload,
                            color = Color(0xFFCBD5E1),
                            style = MaterialTheme.typography.labelSmall
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    qrBitmap?.let { handleDecodedBitmap(it) }
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF14B8A6),
                                    contentColor = Color(0xFF0F172A)
                                ),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.QrCodeScanner,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Quét & Giải mã (ZXing)",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                            }

                            OutlinedButton(
                                onClick = { imagePickerLauncher.launch("image/*") },
                                border = BorderStroke(1.dp, Color(0xFF5EEAD4)),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF5EEAD4)),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PhotoLibrary,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Ảnh QR",
                                    style = MaterialTheme.typography.labelMedium,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                decodedStatus?.let { msg ->
                    Surface(
                        color = if (isDecodingError) Color(0xFFFEE2E2) else Color(0xFFDCFCE7),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = msg,
                            color = if (isDecodingError) Color(0xFF991B1B) else Color(0xFF166534),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                OutlinedTextField(
                    value = codeInput,
                    onValueChange = { codeInput = it },
                    label = { Text("Mã xe (QR Code / URI)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (availableCodes.isNotEmpty()) {
                    Text(
                        text = "Chọn nhanh mã xe gần bạn:",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFF475569)
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(availableCodes.take(5)) { code ->
                            SuggestionChip(
                                onClick = { codeInput = code },
                                label = { Text(code) }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirmCode(codeInput) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F766E))
            ) {
                Text("Mở khóa ngay")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Hủy")
            }
        }
    )
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

