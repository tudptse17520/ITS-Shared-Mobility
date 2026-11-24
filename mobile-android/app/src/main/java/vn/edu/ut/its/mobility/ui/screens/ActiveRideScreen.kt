package vn.edu.ut.its.mobility.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import vn.edu.ut.its.mobility.BuildConfig
import vn.edu.ut.its.mobility.model.RideSessionDto
import vn.edu.ut.its.mobility.ui.MainViewModel
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveRideScreen(
    ride: RideSessionDto,
    viewModel: MainViewModel
) {
    val vndFormat = remember { NumberFormat.getNumberInstance(Locale("vi", "VN")) }
    var showFeedbackDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Chuyến đi đang diễn ra", fontWeight = FontWeight.Bold) }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F766E)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = ride.vehicleCode ?: "ITS-VEHICLE",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = Color(0xFF14B8A6)
                        ) {
                            Text(
                                text = ride.lockStatus ?: "UNLOCKED",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                    Text(
                        text = "${ride.vehicleBrand ?: ""} · ${ride.plate ?: ""}",
                        color = Color(0xFFCCFBF1)
                    )
                    Text(
                        text = "Mã chuyến: ${ride.sessionCode}",
                        color = Color(0xFF99F6E4),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            // Các chỉ số thời gian thực (IoT Telemetry)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricBox(
                    title = "Thời gian",
                    value = "${ride.durationMinutes} phút",
                    modifier = Modifier.weight(1f)
                )
                MetricBox(
                    title = "Quãng đường",
                    value = "${ride.distanceKm} km",
                    modifier = Modifier.weight(1f)
                )
                MetricBox(
                    title = "Dung lượng pin",
                    value = "${ride.currentBatteryLevel ?: 100}%",
                    modifier = Modifier.weight(1f)
                )
            }

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Tọa độ GPS hiện tại (IoT Telemetry)",
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Lat: ${ride.currentLatitude ?: 0.0}  |  Lng: ${ride.currentLongitude ?: 0.0}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF334155)
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Cước phí tạm tính:", fontWeight = FontWeight.Medium)
                        Text(
                            text = "${vndFormat.format(ride.estimatedFee)} đ",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F766E),
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(16.dp))
            ) {
                val startLat = ride.startLatitude ?: ride.currentLatitude ?: 10.8045
                val startLng = ride.startLongitude ?: ride.currentLongitude ?: 106.7166
                val curLat = ride.currentLatitude ?: startLat
                val curLng = ride.currentLongitude ?: startLng
                val vietmapLayerScript = if (BuildConfig.VIETMAP_API_KEY.isNotBlank()) {
                    "L.tileLayer('https://maps.vietmap.vn/api/tm/{z}/{x}/{y}@2x.png?apikey=${BuildConfig.VIETMAP_API_KEY}', { maxZoom: 20, tileSize: 256 }).addTo(map);"
                } else ""
                val mapHtml = remember(startLat, startLng, curLat, curLng, ride.vehicleCode, vietmapLayerScript) {
                    """
                    <!DOCTYPE html>
                    <html>
                    <head>
                      <meta charset="utf-8" />
                      <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
                      <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css"/>
                      <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
                      <style>
                        html, body, #map { margin: 0; padding: 0; width: 100vw; height: 100vh; background: #e2e8f0; }
                      </style>
                    </head>
                    <body>
                      <div id="map"></div>
                      <script>
                        (function() {
                          if (typeof L === 'undefined') return;
                          var map = L.map('map', { zoomControl: false }).setView([$curLat, $curLng], 15);
                          L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', { maxZoom: 19 }).addTo(map);
                          $vietmapLayerScript
                          L.polyline([[$startLat, $startLng], [$curLat, $curLng]], {
                            color: '#2563eb', weight: 4, dashArray: '6 6'
                          }).addTo(map);
                          L.circleMarker([$startLat, $startLng], {
                            radius: 6, color: '#fff', weight: 2, fillColor: '#64748b', fillOpacity: 1
                          }).addTo(map);
                          L.circleMarker([$curLat, $curLng], {
                            radius: 9, color: '#fff', weight: 3, fillColor: '#0f766e', fillOpacity: 1
                          }).bindPopup('${ride.vehicleCode ?: "Xe đang đi"}').addTo(map);
                          setTimeout(function() { map.invalidateSize(); }, 150);
                        })();
                      </script>
                    </body>
                    </html>
                    """.trimIndent()
                }
                LeafletWebView(html = mapHtml, modifier = Modifier.fillMaxSize())
            }

            viewModel.statusMessage?.let { msg ->
                Text(msg, color = Color(0xFF166534), style = MaterialTheme.typography.bodySmall)
            }
            viewModel.errorMessage?.let { err ->
                Text(err, color = Color(0xFF991B1B), style = MaterialTheme.typography.bodySmall)
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Nút mô phỏng IoT di chuyển GPS
            OutlinedButton(
                onClick = { viewModel.simulateIotRideStep() },
                enabled = !viewModel.isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Default.Navigation, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Mô phỏng di chuyển GPS (+300m / IoT Sim)")
            }

            // Nút báo cáo sự cố chuyến đi
            TextButton(
                onClick = { showFeedbackDialog = true },
                enabled = !viewModel.isLoading,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Icon(Icons.Default.ReportProblem, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Báo cáo sự cố chuyến đi")
            }

            // Nút khóa xe & kết thúc chuyến đi
            Button(
                onClick = { viewModel.endCurrentRide() },
                enabled = !viewModel.isLoading,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Default.Lock, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Khóa xe & Kết thúc chuyến đi", fontWeight = FontWeight.Bold)
            }
        }
    }

    if (showFeedbackDialog) {
        ReportIssueDialog(
            ride = ride,
            isSubmitting = viewModel.isLoading,
            onDismiss = { showFeedbackDialog = false },
            onSubmit = { issueType, description ->
                viewModel.submitRideFeedback(
                    rideSessionId = ride.sessionId,
                    issueType = issueType,
                    description = description,
                    onSuccess = { showFeedbackDialog = false }
                )
            }
        )
    }
}

@Composable
private fun MetricBox(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(title, style = MaterialTheme.typography.labelSmall, color = Color(0xFF64748B))
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        }
    }
}

