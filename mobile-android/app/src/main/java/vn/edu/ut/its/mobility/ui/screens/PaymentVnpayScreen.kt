package vn.edu.ut.its.mobility.ui.screens

import android.annotation.SuppressLint
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Payment
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import vn.edu.ut.its.mobility.model.RideSessionDto
import vn.edu.ut.its.mobility.ui.ItsButton
import vn.edu.ut.its.mobility.ui.ItsPanel
import vn.edu.ut.its.mobility.ui.ItsNotice
import vn.edu.ut.its.mobility.ui.MainViewModel
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentVnpayScreen(
    ride: RideSessionDto,
    viewModel: MainViewModel
) {
    val colors = MaterialTheme.colorScheme
    val vndFormat = remember { NumberFormat.getNumberInstance(Locale("vi", "VN")) }
    val amount = ride.totalFee ?: ride.estimatedFee
    val paymentUrl = viewModel.vnpayPaymentUrl

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Thanh toán", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.background)
            )
        }
    ) { padding ->
        if (!paymentUrl.isNullOrBlank()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                VnpayWebView(
                    url = paymentUrl,
                    onReturnUrlReached = { viewModel.finishVnpayFlow() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                )
                ItsButton(
                    onClick = { viewModel.finishVnpayFlow() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                ) {
                    Text("Quay về bản đồ")
                }
            }
        } else {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
                Column(
                    Modifier.widthIn(max = 600.dp).fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(13.dp),
                ) {
                    Surface(
                        color = colors.primaryContainer,
                        contentColor = colors.onPrimaryContainer,
                        shape = MaterialTheme.shapes.extraLarge,
                    ) {
                        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Outlined.CheckCircle, null, Modifier.size(20.dp), tint = colors.primary)
                                Text("Đã kết thúc chuyến đi", color = colors.onPrimaryContainer, style = MaterialTheme.typography.bodyMedium)
                            }
                            Text("Cước chuyến đi", style = MaterialTheme.typography.labelLarge)
                            Text("${vndFormat.format(amount)} đ", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                        }
                    }
                    ItsPanel {
                        Column(Modifier.fillMaxWidth().padding(15.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Chi tiết chuyến đi", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            InvoiceRow("Mã chuyến", ride.sessionCode)
                            InvoiceRow("Xe", listOfNotNull(ride.vehicleCode, ride.vehicleBrand).joinToString(" · "))
                            InvoiceRow("Thời gian", "${ride.durationMinutes} phút")
                            InvoiceRow("Quãng đường", "${ride.distanceKm} km")
                        }
                    }
                    ItsNotice(viewModel.errorMessage, isError = true)
                    ItsButton(
                        onClick = { viewModel.initiatePayment("Vnpay") },
                        enabled = !viewModel.isLoading,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp),
                    ) {
                        Icon(Icons.Outlined.Payment, null, Modifier.size(20.dp))
                        Spacer(Modifier.width(7.dp))
                        Text("Thanh toán qua VNPay", fontWeight = FontWeight.SemiBold)
                    }
                    ItsButton(
                        onClick = { viewModel.initiatePayment("Cash") },
                        enabled = !viewModel.isLoading,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp),
                        outlined = true,
                    ) {
                        Text("Trả bằng tiền mặt")
                    }
                    TextButton(
                        onClick = { viewModel.dismissPaymentScreen() },
                        enabled = !viewModel.isLoading,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text("Để sau · Quay về bản đồ")
                    }
                }
            }
        }
    }
}


@Composable
private fun InvoiceRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(label, Modifier.weight(.75f), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, Modifier.weight(1.25f), fontWeight = FontWeight.SemiBold, textAlign = TextAlign.End)
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun VnpayWebView(
    url: String,
    onReturnUrlReached: () -> Unit,
    modifier: Modifier = Modifier
) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(
                        view: WebView?,
                        request: WebResourceRequest?
                    ): Boolean {
                        val target = request?.url?.toString().orEmpty()
                        // Tự động chuyển localhost thành 10.0.2.2 trên Android Emulator
                        if (target.startsWith("http://localhost:8080")) {
                            val emulatorUrl = target.replace("http://localhost:8080", "http://10.0.2.2:8080")
                            view?.loadUrl(emulatorUrl)
                            return true
                        }
                        return false
                    }

                    override fun onPageFinished(view: WebView?, finishedUrl: String?) {
                        super.onPageFinished(view, finishedUrl)
                        if (finishedUrl != null && finishedUrl.contains("vnpay-return") && finishedUrl.contains("vnp_ResponseCode=00")) {
                            // Cho người dùng thấy trang xác nhận 1 chút rồi bấm nút hoặc tự điều hướng
                        }
                    }
                }
                loadUrl(url)
            }
        }
    )
}

