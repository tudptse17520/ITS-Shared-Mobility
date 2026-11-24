package vn.edu.ut.its.mobility.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import vn.edu.ut.its.mobility.BuildConfig
import vn.edu.ut.its.mobility.model.RideSessionDto
import vn.edu.ut.its.mobility.ui.ItsButton
import vn.edu.ut.its.mobility.ui.ItsNotice
import vn.edu.ut.its.mobility.ui.MainViewModel
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveRideScreen(
    ride: RideSessionDto,
    viewModel: MainViewModel
) {
    val colors = MaterialTheme.colorScheme
    val vndFormat = remember { NumberFormat.getNumberInstance(Locale("vi", "VN")) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Đang thuê xe", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.background)
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
                colors = CardDefaults.cardColors(containerColor = colors.primaryContainer),
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
                            text = ride.vehicleCode ?: "Xe đang thuê",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = colors.onPrimaryContainer
                        )
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = colors.surface
                        ) {
                            Text(
                                text = if (ride.lockStatus == "LOCKED") "Đã khóa" else "Đã mở khóa",
                                color = colors.primary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                    Text(
                        text = "${ride.vehicleBrand ?: ""} · ${ride.plate ?: ""}",
                        color = colors.onPrimaryContainer.copy(alpha = .82f)
                    )
                    Text(
                        text = "Mã chuyến: ${ride.sessionCode}",
                        color = colors.onPrimaryContainer.copy(alpha = .82f),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .clip(RoundedCornerShape(16.dp))
            ) {
                val startLat = ride.startLatitude ?: ride.currentLatitude ?: 10.8045
                val startLng = ride.startLongitude ?: ride.currentLongitude ?: 106.7166
                val curLat = ride.currentLatitude ?: startLat
                val curLng = ride.currentLongitude ?: startLng
                val mapHtml = remember(startLat, startLng, curLat, curLng, ride.vehicleCode) {
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
                          L.tileLayer('https://maps.vietmap.vn/api/tm/{z}/{x}/{y}@2x.png?apikey=${BuildConfig.VIETMAP_API_KEY}', {
                            maxZoom: 20, tileSize: 256
                          }).addTo(map);
                          L.polyline([[$startLat, $startLng], [$curLat, $curLng]], {
                            color: '#ac6840', weight: 4, dashArray: '6 6'
                          }).addTo(map);
                          L.circleMarker([$startLat, $startLng], {
                            radius: 6, color: '#fff', weight: 2, fillColor: '#64748b', fillOpacity: 1
                          }).addTo(map);
                          L.circleMarker([$curLat, $curLng], {
                            radius: 9, color: '#fff', weight: 3, fillColor: '#294353', fillOpacity: 1
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

            // Các chỉ số thời gian thực (IoT Telemetry)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricBox(
                    title = "Thời gian thuê",
                    value = "${ride.durationMinutes} phút",
                    modifier = Modifier.weight(1f)
                )
                MetricBox(
                    title = "Quãng đường",
                    value = "${ride.distanceKm} km",
                    modifier = Modifier.weight(1f)
                )
                MetricBox(
                    title = "Pin",
                    value = "${ride.currentBatteryLevel ?: 100}%",
                    modifier = Modifier.weight(1f)
                )
            }

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = colors.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Vị trí hiện tại",
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = listOfNotNull(ride.currentLatitude, ride.currentLongitude).joinToString(", ").ifBlank { "Chưa có vị trí" },
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Cước tạm tính", fontWeight = FontWeight.Medium)
                        Text(
                            text = "${vndFormat.format(ride.estimatedFee)} đ",
                            fontWeight = FontWeight.Bold,
                            color = colors.primary,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            }

            ItsNotice(viewModel.errorMessage ?: viewModel.statusMessage, isError = viewModel.errorMessage != null)
            ItsButton(
                onClick = { viewModel.simulateIotRideStep() },
                enabled = !viewModel.isLoading,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                outlined = true,
            ) {
                Icon(Icons.Outlined.NearMe, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Thử di chuyển 300 m")
            }
            ItsButton(
                onClick = { viewModel.endCurrentRide() },
                enabled = !viewModel.isLoading,
                modifier = Modifier.fillMaxWidth().heightIn(min = 49.dp),
            ) {
                Icon(Icons.Outlined.Lock, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Kết thúc chuyến")
            }
        }
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
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        }
    }
}

