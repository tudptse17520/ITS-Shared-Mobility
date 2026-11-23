package vn.edu.ut.its.mobility.ui.screens

import android.annotation.SuppressLint
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import vn.edu.ut.its.mobility.model.RideSessionDto
import vn.edu.ut.its.mobility.ui.MainViewModel
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentVnpayScreen(
    ride: RideSessionDto,
    viewModel: MainViewModel
) {
    val vndFormat = remember { NumberFormat.getNumberInstance(Locale("vi", "VN")) }
    val amount = ride.totalFee ?: ride.estimatedFee
    val paymentUrl = viewModel.vnpayPaymentUrl

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Thanh toán Cước chuyến đi", fontWeight = FontWeight.Bold) }
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
                Button(
                    onClick = { viewModel.finishVnpayFlow() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F766E))
                ) {
                    Text("Hoàn tất & Quay về Bản đồ")
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF16A34A),
                    modifier = Modifier.size(64.dp)
                )
                Text(
                    text = "Đã khóa xe an toàn!",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        InvoiceRow("Mã chuyến", ride.sessionCode)
                        InvoiceRow("Phương tiện", "${ride.vehicleCode} · ${ride.vehicleBrand}")
                        InvoiceRow("Thời gian thuê", "${ride.durationMinutes} phút")
                        InvoiceRow("Quãng đường", "${ride.distanceKm} km")
                        HorizontalDivider()
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Tổng tiền thanh toán", fontWeight = FontWeight.Bold)
                            Text(
                                text = "${vndFormat.format(amount)} đ",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F766E),
                                style = MaterialTheme.typography.titleLarge
                            )
                        }
                    }
                }

                viewModel.errorMessage?.let { err ->
                    Text(err, color = MaterialTheme.colorScheme.error)
                }

                Spacer(modifier = Modifier.weight(1f))

                Button(
                    onClick = { viewModel.initiatePayment("Vnpay") },
                    enabled = !viewModel.isLoading,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF005BAA)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.Payment, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Thanh toán qua VNPay Sandbox", fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = { viewModel.initiatePayment("Cash") },
                    enabled = !viewModel.isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Thanh toán nhanh (Demo / Tiền mặt)")
                }
            }
        }
    }
}

@Composable
private fun InvoiceRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = Color(0xFF64748B))
        Text(value, fontWeight = FontWeight.Medium)
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

