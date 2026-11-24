package vn.edu.ut.its.mobility.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.UrlTileProvider
import com.google.maps.android.compose.*
import kotlinx.coroutines.delay
import vn.edu.ut.its.mobility.BuildConfig
import vn.edu.ut.its.mobility.model.FleetVehicleDto
import vn.edu.ut.its.mobility.model.RideSessionDto
import vn.edu.ut.its.mobility.model.StationDto
import vn.edu.ut.its.mobility.ui.MainViewModel
import java.net.URL
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeMapScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    var showQrDialog by remember { mutableStateOf(false) }
    var showHistoryDialog by remember { mutableStateOf(false) }
    var feedbackSessionTarget by remember { mutableStateOf<RideSessionDto?>(null) }
    var useVietMapWebMode by remember { mutableStateOf(true) }
    var filterMode by remember { mutableStateOf("ALL") }
    var remainingReserveSeconds by remember { mutableStateOf(0L) }

    val tryReadLastLocation = remember(context) {
        {
            runCatching {
                val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
                val loc = lm?.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                    ?: lm?.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                if (loc != null) {
                    viewModel.updateUserLocation(loc.latitude, loc.longitude)
                }
            }
            Unit
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        val granted = perms[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            perms[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            tryReadLastLocation()
        }
    }

    LaunchedEffect(Unit) {
        val fineGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (fineGranted || coarseGranted) {
            tryReadLastLocation()
        } else {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    LaunchedEffect(viewModel.reservationExpiresAtMillis) {
        val expiresAt = viewModel.reservationExpiresAtMillis
        if (expiresAt == null) {
            remainingReserveSeconds = 0L
        } else {
            while (true) {
                val rem = ((expiresAt - System.currentTimeMillis()) / 1000L).coerceAtLeast(0L)
                remainingReserveSeconds = rem
                if (rem <= 0L) break
                delay(1000L)
            }
        }
    }

    val filteredVehicles = remember(viewModel.vehicles, filterMode) {
        when (filterMode) {
            "AVAILABLE" -> viewModel.vehicles.filter { it.fleetStatus == "AVAILABLE" }
            "EBIKE" -> viewModel.vehicles.filter { it.vehicleTypeName?.contains("Đạp", ignoreCase = true) == true }
            "ESCOOTER" -> viewModel.vehicles.filter { it.vehicleTypeName?.contains("Máy", ignoreCase = true) == true }
            else -> viewModel.vehicles
        }
    }

    val vndFormat = remember { NumberFormat.getNumberInstance(Locale("vi", "VN")) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "ITS Shared Mobility",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Xin chào, ${viewModel.currentUser?.fullName ?: "Khách thuê"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF64748B),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showQrDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = "Quét QR Mở khóa"
                        )
                    }
                    IconButton(onClick = {
                        viewModel.loadRideHistory()
                        showHistoryDialog = true
                    }) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "Lịch sử chuyến đi"
                        )
                    }
                    IconButton(onClick = { useVietMapWebMode = !useVietMapWebMode }) {
                        Icon(
                            imageVector = Icons.Default.Layers,
                            contentDescription = "Chuyển lớp bản đồ VietMap"
                        )
                    }
                    IconButton(onClick = { viewModel.refreshFleet() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Làm mới")
                    }
                    IconButton(onClick = { viewModel.logout() }) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Đăng xuất")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Thanh bộ lọc nhanh
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val chips = listOf(
                    "ALL" to "Tất cả (${viewModel.vehicles.size})",
                    "AVAILABLE" to "Sẵn sàng",
                    "EBIKE" to "Xe đạp / E-Bike",
                    "ESCOOTER" to "Xe máy điện"
                )
                items(chips) { (key, label) ->
                    FilterChip(
                        selected = filterMode == key,
                        onClick = { filterMode = key },
                        label = { Text(label) }
                    )
                }
            }

            // Bản đồ VietMap + Google Maps Compose
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                if (!useVietMapWebMode) {
                    val uthCenter = LatLng(viewModel.userLat, viewModel.userLng)
                    val cameraPositionState = rememberCameraPositionState {
                        position = CameraPosition.fromLatLngZoom(uthCenter, 14f)
                    }

                    val vietMapTileProvider = remember {
                        object : UrlTileProvider(256, 256) {
                            override fun getTileUrl(x: Int, y: Int, zoom: Int): URL {
                                val url = "https://maps.vietmap.vn/api/tm/$zoom/$x/$y@2x.png?apikey=${BuildConfig.VIETMAP_API_KEY}"
                                return URL(url)
                            }
                        }
                    }

                    GoogleMap(
                        modifier = Modifier.fillMaxSize(),
                        cameraPositionState = cameraPositionState,
                        uiSettings = MapUiSettings(zoomControlsEnabled = true)
                    ) {
                        if (BuildConfig.VIETMAP_API_KEY.isNotBlank()) {
                            TileOverlay(tileProvider = vietMapTileProvider, zIndex = 0f)
                        }

                        // Marker các Trạm xe
                        viewModel.stations.forEach { st ->
                            Marker(
                                state = MarkerState(position = LatLng(st.latitude, st.longitude)),
                                title = st.stationName,
                                snippet = "Sẵn sàng: ${st.availableVehicles}/${st.totalDocks} xe",
                                icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)
                            )
                        }

                        // Marker các Phương tiện
                        filteredVehicles.forEach { v ->
                            val hue = when (v.fleetStatus) {
                                "AVAILABLE" -> BitmapDescriptorFactory.HUE_GREEN
                                "IN_USE" -> BitmapDescriptorFactory.HUE_BLUE
                                "RESERVED" -> BitmapDescriptorFactory.HUE_ORANGE
                                else -> BitmapDescriptorFactory.HUE_RED
                            }
                            Marker(
                                state = MarkerState(position = LatLng(v.latitude, v.longitude)),
                                title = "${v.vehicleCode} · ${v.brand}",
                                snippet = "Pin ${v.batteryLevel}% · ${v.fleetStatus}",
                                icon = BitmapDescriptorFactory.defaultMarker(hue),
                                onClick = {
                                    viewModel.selectVehicle(v)
                                    false
                                }
                            )
                        }
                    }
                } else {
                    VietMapLeafletWebView(
                        stations = viewModel.stations,
                        vehicles = filteredVehicles,
                        selectedVehicleId = viewModel.selectedVehicle?.id,
                        centerLat = viewModel.userLat,
                        centerLng = viewModel.userLng
                    )
                }

                // Danh sách xe ngang nổi phía trên thanh chi tiết
                LazyRow(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredVehicles) { v ->
                        val isSelected = viewModel.selectedVehicle?.id == v.id
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) Color(0xFF0F766E) else Color.White,
                            shadowElevation = 4.dp,
                            modifier = Modifier.clickable { viewModel.selectVehicle(v) }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = v.vehicleCode,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else Color(0xFF0F172A),
                                    style = MaterialTheme.typography.labelMedium
                                )
                                Text(
                                    text = "${v.batteryLevel}%",
                                    color = if (isSelected) Color(0xFFA7F3D0) else Color(0xFF16A34A),
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }
                }

                if (viewModel.selectedVehicle == null) {
                    ExtendedFloatingActionButton(
                        onClick = { showQrDialog = true },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(16.dp),
                        containerColor = Color(0xFF0F766E),
                        contentColor = Color.White,
                        icon = { Icon(Icons.Default.QrCodeScanner, contentDescription = null) },
                        text = { Text("Quét QR Mở khóa") }
                    )
                }
            }

            // Thông báo trạng thái / lỗi
            viewModel.statusMessage?.let { msg ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFDCFCE7))
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(msg, color = Color(0xFF166534), style = MaterialTheme.typography.bodySmall)
                }
            }
            viewModel.errorMessage?.let { err ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFFEE2E2))
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(err, color = Color(0xFF991B1B), style = MaterialTheme.typography.bodySmall)
                }
            }

            // Bottom Card chi tiết xe đang chọn
            viewModel.selectedVehicle?.let { vehicle ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 8.dp)
                            ) {
                                Text(
                                    text = "${vehicle.vehicleCode} · ${vehicle.brand}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${vehicle.vehicleTypeName ?: "Phương tiện"} · ${vehicle.plate}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF64748B)
                                )
                            }
                            AssistChip(
                                onClick = {},
                                label = {
                                    val holdSuffix = if (
                                        viewModel.reservedVehicleId == vehicle.id &&
                                        remainingReserveSeconds > 0
                                    ) {
                                        val mins = remainingReserveSeconds / 60
                                        val secs = remainingReserveSeconds % 60
                                        " · %02d:%02d".format(mins, secs)
                                    } else {
                                        ""
                                    }
                                    Text("${vehicle.fleetStatus}$holdSuffix")
                                }
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Pin: ${vehicle.batteryLevel}%",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = if (vehicle.batteryLevel >= 20) Color(0xFF16A34A) else Color(0xFFDC2626)
                            )
                            Text(
                                text = "Cách ${vehicle.distanceMeters?.toInt() ?: 0} m",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "Mở khóa ${vndFormat.format(vehicle.unlockFee)}đ + ${vndFormat.format(vehicle.hourlyRate)}đ/h",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF0F766E),
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            if (vehicle.fleetStatus == "RESERVED") {
                                OutlinedButton(
                                    onClick = { viewModel.cancelSelectedReservation() },
                                    enabled = !viewModel.isLoading,
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Hủy đặt giữ", maxLines = 1)
                                }
                            } else {
                                OutlinedButton(
                                    onClick = { viewModel.reserveSelectedVehicle() },
                                    enabled = !viewModel.isLoading && vehicle.fleetStatus == "AVAILABLE",
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Đặt giữ (15p)", maxLines = 1)
                                }
                            }
                            Button(
                                onClick = { viewModel.startRideByCode(vehicle.vehicleCode) },
                                enabled = !viewModel.isLoading && (vehicle.fleetStatus == "AVAILABLE" || vehicle.fleetStatus == "RESERVED"),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F766E)),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Mở khóa đi ngay", maxLines = 1)
                            }
                        }

                        FilledTonalButton(
                            onClick = { showQrDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = Color(0xFFCCFBF1),
                                contentColor = Color(0xFF0F766E)
                            )
                        ) {
                            Icon(Icons.Default.QrCodeScanner, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Quét QR Mở khóa",
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }

    if (showQrDialog) {
        QrScannerDialog(
            availableCodes = viewModel.vehicles
                .filter { it.fleetStatus == "AVAILABLE" || it.fleetStatus == "RESERVED" }
                .map { it.vehicleCode },
            initialCode = viewModel.selectedVehicle?.vehicleCode,
            onDismiss = { showQrDialog = false },
            onConfirmCode = { code ->
                showQrDialog = false
                viewModel.startRideByCode(code)
            }
        )
    }

    if (showHistoryDialog) {
        RideHistoryDialog(
            rides = viewModel.rideHistory,
            isLoading = viewModel.isLoadingHistory,
            vndFormat = vndFormat,
            onDismiss = { showHistoryDialog = false },
            onReportIssue = { ride ->
                showHistoryDialog = false
                feedbackSessionTarget = ride
            }
        )
    }

    feedbackSessionTarget?.let { targetRide ->
        ReportIssueDialog(
            ride = targetRide,
            isSubmitting = viewModel.isLoading,
            onDismiss = { feedbackSessionTarget = null },
            onSubmit = { issueType, description ->
                viewModel.submitRideFeedback(
                    rideSessionId = targetRide.sessionId,
                    issueType = issueType,
                    description = description,
                    onSuccess = { feedbackSessionTarget = null }
                )
            }
        )
    }
}

@Composable
private fun RideHistoryDialog(
    rides: List<RideSessionDto>,
    isLoading: Boolean,
    vndFormat: NumberFormat,
    onDismiss: () -> Unit,
    onReportIssue: (RideSessionDto) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Lịch sử chuyến đi", fontWeight = FontWeight.Bold)
        },
        text = {
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color(0xFF0F766E))
                }
            } else if (rides.isEmpty()) {
                Text(
                    text = "Bạn chưa có chuyến đi nào được ghi nhận.",
                    color = Color(0xFF64748B)
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 360.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(rides) { ride ->
                        val fee = ride.totalFee ?: ride.estimatedFee
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = ride.vehicleCode ?: ride.sessionCode,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = if (ride.paid) "Đã thanh toán" else "Chờ thanh toán",
                                        color = if (ride.paid) Color(0xFF16A34A) else Color(0xFFD97706),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                Text(
                                    text = "Mã: ${ride.sessionCode}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF64748B)
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "${ride.distanceKm} km · ${ride.durationMinutes} phút",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Text(
                                        text = "${vndFormat.format(fee)} đ",
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0F766E),
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    TextButton(
                                        onClick = { onReportIssue(ride) },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.ReportProblem,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Báo cáo sự cố", style = MaterialTheme.typography.labelMedium)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Đóng")
            }
        }
    )
}

@Composable
internal fun ReportIssueDialog(
    ride: RideSessionDto,
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (issueType: String, description: String) -> Unit
) {
    val issueOptions = remember {
        listOf(
            "WRONG_RETURN_LOCATION" to "Trả xe sai vị trí",
            "WRONG_PLATE" to "Sai biển số / thông tin xe",
            "RIDE_OVERTIME" to "Sự cố thời gian thuê",
            "UNPAID_RIDE" to "Sự cố thanh toán",
            "OTHER" to "Sự cố kỹ thuật khác"
        )
    }
    var selectedIssueType by remember { mutableStateOf("OTHER") }
    var description by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = { if (!isSubmitting) onDismiss() },
        title = {
            Text("Báo cáo sự cố · ${ride.vehicleCode ?: ride.sessionCode}", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Mã chuyến #${ride.sessionCode}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF64748B)
                )
                issueOptions.forEach { (code, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedIssueType = code }
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedIssueType == code,
                            onClick = { selectedIssueType = code }
                        )
                        Text(label, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Mô tả chi tiết") },
                    placeholder = { Text("Ví dụ: Xe yếu pin hoặc khóa dock khó đóng...") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(selectedIssueType, description) },
                enabled = !isSubmitting,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F766E))
            ) {
                Text(if (isSubmitting) "Đang gửi..." else "Gửi báo cáo")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSubmitting) {
                Text("Hủy")
            }
        }
    )
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun VietMapLeafletWebView(
    stations: List<StationDto>,
    vehicles: List<FleetVehicleDto>,
    selectedVehicleId: Long?,
    centerLat: Double,
    centerLng: Double
) {
    val stationsJson = remember(stations) {
        stations.joinToString(prefix = "[", postfix = "]", separator = ",") { st ->
            val name = org.json.JSONObject.quote(st.stationName)
            val addr = org.json.JSONObject.quote(st.address)
            """{"name":$name,"addr":$addr,"lat":${st.latitude},"lng":${st.longitude},"avail":${st.availableVehicles},"docks":${st.totalDocks}}"""
        }
    }
    val vehiclesJson = remember(vehicles) {
        vehicles.joinToString(prefix = "[", postfix = "]", separator = ",") { v ->
            val code = org.json.JSONObject.quote(v.vehicleCode)
            val brand = org.json.JSONObject.quote(v.brand)
            val status = org.json.JSONObject.quote(v.fleetStatus)
            """{"id":${v.id},"lat":${v.latitude},"lng":${v.longitude},"code":$code,"brand":$brand,"bat":${v.batteryLevel},"st":$status}"""
        }
    }
    val selectedVehicle = remember(vehicles, selectedVehicleId) {
        vehicles.find { it.id == selectedVehicleId }
    }
    val mapCenterLat = selectedVehicle?.latitude ?: centerLat
    val mapCenterLng = selectedVehicle?.longitude ?: centerLng
    val selectedIdValue = selectedVehicleId?.toString() ?: "null"

    val vietmapLayerScript = if (BuildConfig.VIETMAP_API_KEY.isNotBlank()) {
        """
              L.tileLayer('https://maps.vietmap.vn/api/tm/{z}/{x}/{y}@2x.png?apikey=${BuildConfig.VIETMAP_API_KEY}', {
                maxZoom: 20,
                tileSize: 256,
                attribution: '&copy; VietMap'
              }).addTo(map);
        """.trimIndent()
    } else ""

    val html = remember(stationsJson, vehiclesJson, selectedIdValue, mapCenterLat, mapCenterLng, vietmapLayerScript) {
        """
        <!DOCTYPE html>
        <html>
        <head>
          <meta charset="utf-8" />
          <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
          <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css"/>
          <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
          <style>
            html, body { margin: 0; padding: 0; width: 100%; height: 100%; background: #e2e8f0; overflow: hidden; }
            #map { width: 100vw; height: 100vh; }
            .its-station-pin, .its-vehicle-pin { background: transparent; border: none; }
          </style>
        </head>
        <body>
          <div id="map"></div>
          <script>
            (function() {
              if (typeof L === 'undefined') {
                document.getElementById('map').innerHTML = '<div style="padding:24px;font-family:sans-serif;color:#334155;">Đang tải thư viện bản đồ...</div>';
                return;
              }
              var map = L.map('map', { zoomControl: true }).setView([$mapCenterLat, $mapCenterLng], 14);
              L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
                maxZoom: 19,
                attribution: '&copy; OpenStreetMap'
              }).addTo(map);
              $vietmapLayerScript

              var stations = $stationsJson;
              stations.forEach(function(st) {
                var stIcon = L.divIcon({
                  className: 'its-station-pin',
                  html: '<div style="background:#0f172a;color:#fff;border:2px solid #38bdf8;border-radius:10px;padding:3px 8px;font-size:11px;font-weight:700;white-space:nowrap;box-shadow:0 3px 10px rgba(0,0,0,0.35);">Hub · ' + st.avail + '/' + st.docks + '</div>',
                  iconSize: [96, 24],
                  iconAnchor: [48, 12]
                });
                L.marker([st.lat, st.lng], { icon: stIcon })
                  .bindPopup('<b>' + st.name + '</b><br/>' + st.addr + '<br/>Sẵn sàng: ' + st.avail + '/' + st.docks)
                  .addTo(map);
              });

              var vehicles = $vehiclesJson;
              var selectedId = $selectedIdValue;
              vehicles.forEach(function(v) {
                var color = v.st === 'AVAILABLE' ? '#10b981'
                          : v.st === 'IN_USE' ? '#2563eb'
                          : v.st === 'RESERVED' ? '#f59e0b'
                          : v.st === 'LOW_BATTERY' ? '#f97316'
                          : '#ef4444';
                var border = (selectedId !== null && v.id === selectedId) ? '3px solid #facc15' : '2px solid #ffffff';
                var vIcon = L.divIcon({
                  className: 'its-vehicle-pin',
                  html: '<div style="display:flex;align-items:center;gap:4px;background:' + color + ';color:#fff;border:' + border + ';border-radius:999px;padding:3px 8px;font-size:11px;font-weight:700;white-space:nowrap;box-shadow:0 3px 10px rgba(0,0,0,0.3);"><span>' + v.code + '</span><span style="opacity:.9">' + v.bat + '%</span></div>',
                  iconSize: [108, 24],
                  iconAnchor: [54, 12]
                });
                L.marker([v.lat, v.lng], { icon: vIcon })
                  .bindPopup('<b>' + v.code + ' · ' + v.brand + '</b><br/>Pin: ' + v.bat + '%<br/>Trạng thái: ' + v.st)
                  .addTo(map);
              });

              setTimeout(function() { map.invalidateSize(); }, 200);
            })();
          </script>
        </body>
        </html>
        """.trimIndent()
    }

    LeafletWebView(html = html, modifier = Modifier.fillMaxSize())
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
internal fun LeafletWebView(
    html: String,
    modifier: Modifier = Modifier
) {
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            WebView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                webViewClient = WebViewClient()
                webChromeClient = WebChromeClient()
                tag = html
                loadDataWithBaseURL("https://maps.vietmap.vn", html, "text/html", "UTF-8", null)
            }
        },
        update = { webView ->
            if (webView.tag != html) {
                webView.tag = html
                webView.loadDataWithBaseURL("https://maps.vietmap.vn", html, "text/html", "UTF-8", null)
            }
        }
    )
}

