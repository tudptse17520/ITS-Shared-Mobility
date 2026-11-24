package vn.edu.ut.its.mobility.ui.screens

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebResourceRequest
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.FormatListBulleted
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.automirrored.outlined.DirectionsBike
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.UrlTileProvider
import com.google.maps.android.compose.*
import vn.edu.ut.its.mobility.BuildConfig
import vn.edu.ut.its.mobility.model.FleetVehicleDto
import vn.edu.ut.its.mobility.model.RideSessionDto
import vn.edu.ut.its.mobility.model.StationDto
import vn.edu.ut.its.mobility.ui.BrandTile
import vn.edu.ut.its.mobility.ui.ItsButton
import vn.edu.ut.its.mobility.ui.ItsNotice
import vn.edu.ut.its.mobility.ui.ItsPanel
import vn.edu.ut.its.mobility.ui.MainViewModel
import java.net.URL
import java.text.NumberFormat
import java.util.Locale

private enum class FleetSheet { NONE, LIST, VEHICLE, QR, HISTORY }

private enum class VehicleFilter(val title: String) {
    ALL("Tất cả xe"), AVAILABLE("Xe sẵn sàng"), BIKE("Xe đạp"), SCOOTER("Xe máy điện");

    fun includes(vehicle: FleetVehicleDto): Boolean = when (this) {
        ALL -> true
        AVAILABLE -> vehicle.fleetStatus == "AVAILABLE"
        BIKE -> vehicle.vehicleTypeName?.contains("Đạp", ignoreCase = true) == true
        SCOOTER -> vehicle.vehicleTypeName?.contains("Máy", ignoreCase = true) == true
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeMapScreen(viewModel: MainViewModel) {
    var sheet by rememberSaveable { mutableStateOf(FleetSheet.NONE) }
    var filter by rememberSaveable { mutableStateOf(VehicleFilter.ALL) }
    var nativeMap by rememberSaveable { mutableStateOf(false) }
    var showOptions by remember { mutableStateOf(false) }
    var showFilters by remember { mutableStateOf(false) }
    val colors = MaterialTheme.colorScheme
    val vehicles = remember(viewModel.vehicles, filter) { viewModel.vehicles.filter(filter::includes) }
    val selected = viewModel.selectedVehicle
    val featured = vehicles.firstOrNull { it.id == selected?.id } ?: vehicles.firstOrNull()
    val currency = remember { NumberFormat.getNumberInstance(Locale("vi", "VN")) }
    val notice = viewModel.errorMessage ?: viewModel.statusMessage?.takeUnless { it == "Đăng nhập thành công." }

    fun inspect(vehicle: FleetVehicleDto) {
        viewModel.clearMessages()
        viewModel.selectVehicle(vehicle)
        sheet = FleetSheet.VEHICLE
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val wide = maxWidth >= 680.dp

        Scaffold(
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.background),
                    navigationIcon = {
                        BrandTile(Modifier.padding(start = 14.dp, end = 10.dp), size = 39.dp)
                    },
                    title = {
                        Column {
                            Text("ITS Mobility", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("TP. Hồ Chí Minh", style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
                        }
                    },
                    actions = {
                        Box {
                            IconButton(onClick = { showOptions = true }) {
                                Icon(Icons.Outlined.MoreHoriz, "Tùy chọn")
                            }
                            DropdownMenu(expanded = showOptions, onDismissRequest = { showOptions = false }) {
                                DropdownMenuItem(
                                    text = { Text("Đổi lớp bản đồ") },
                                    leadingIcon = { Icon(Icons.Outlined.Layers, null) },
                                    onClick = { showOptions = false; nativeMap = !nativeMap },
                                )
                                DropdownMenuItem(
                                    text = { Text("Làm mới") },
                                    leadingIcon = { Icon(Icons.Outlined.Refresh, null) },
                                    onClick = { showOptions = false; viewModel.refreshFleet() },
                                )
                                DropdownMenuItem(
                                    text = { Text("Đăng xuất") },
                                    leadingIcon = { Icon(Icons.AutoMirrored.Outlined.Logout, null) },
                                    onClick = { showOptions = false; viewModel.logout() },
                                )
                            }
                        }
                    },
                )
            },
            bottomBar = {
                MobilityActionBar(
                    showList = !wide,
                    onList = { sheet = FleetSheet.LIST },
                    onScan = { sheet = FleetSheet.QR },
                    onHistory = { viewModel.loadRideHistory(); sheet = FleetSheet.HISTORY },
                )
            },
        ) { padding ->
            Column(
                Modifier.fillMaxSize().padding(padding).padding(horizontal = 14.dp),
                verticalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Xe gần bạn", Modifier.weight(1f), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Box {
                        OutlinedButton(
                            onClick = { showFilters = true },
                            shape = MaterialTheme.shapes.medium,
                            contentPadding = PaddingValues(horizontal = 9.dp),
                        ) {
                            Icon(Icons.Outlined.Tune, null, Modifier.size(17.dp))
                            Spacer(Modifier.width(5.dp))
                            Text(filter.title, style = MaterialTheme.typography.labelMedium)
                        }
                        DropdownMenu(expanded = showFilters, onDismissRequest = { showFilters = false }) {
                            VehicleFilter.entries.forEach { choice ->
                                DropdownMenuItem(
                                    text = { Text(choice.title) },
                                    onClick = { filter = choice; showFilters = false },
                                    trailingIcon = if (choice == filter) {
                                        { Icon(Icons.Outlined.Check, "Đang chọn") }
                                    } else null,
                                )
                            }
                        }
                    }
                }

                if (wide) {
                    Row(Modifier.fillMaxWidth().weight(1f).padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(13.dp)) {
                        FleetMap(viewModel, vehicles, featured, nativeMap, ::inspect, Modifier.weight(1f).fillMaxHeight())
                        ItsPanel(Modifier.width(310.dp).fillMaxHeight()) {
                            Column(Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                                Text("Danh sách xe", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                VehicleList(vehicles, featured?.id, ::inspect, Modifier.weight(1f))
                                if (sheet == FleetSheet.NONE) ItsNotice(notice, isError = viewModel.errorMessage != null)
                            }
                        }
                    }
                } else {
                    FleetMap(viewModel, vehicles, featured, nativeMap, ::inspect, Modifier.fillMaxWidth().weight(1f))
                    if (featured != null) {
                        Surface(
                            onClick = { inspect(featured) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.large,
                            color = colors.surface,
                            border = BorderStroke(1.dp, colors.outlineVariant),
                        ) {
                            VehicleRow(featured, highlighted = true)
                        }
                    } else if (!viewModel.isLoading) {
                        Text("Không có xe phù hợp.", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(vertical = 4.dp))
                    }
                    if (sheet == FleetSheet.NONE) ItsNotice(notice, isError = viewModel.errorMessage != null)
                    Spacer(Modifier.height(1.dp))
                }
            }
        }
    }

    when (sheet) {
        FleetSheet.LIST -> ModalBottomSheet(
            onDismissRequest = { sheet = FleetSheet.NONE },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = colors.surface,
        ) {
            Column(Modifier.fillMaxWidth().padding(start = 17.dp, end = 17.dp, bottom = 14.dp),
                verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Text("Danh sách xe", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                VehicleList(vehicles, featured?.id, ::inspect, Modifier.heightIn(max = 400.dp))
            }
        }
        FleetSheet.VEHICLE -> if (selected != null) {
            VehicleDetailsSheet(
                vehicle = selected,
                currency = currency,
                loading = viewModel.isLoading,
                notice = notice,
                isError = viewModel.errorMessage != null,
                onDismiss = { sheet = FleetSheet.NONE },
                onReserve = {
                    if (selected.fleetStatus == "RESERVED") viewModel.cancelSelectedReservation()
                    else viewModel.reserveSelectedVehicle()
                },
                onUnlock = { viewModel.startRideByCode(selected.vehicleCode) },
            )
        }
        FleetSheet.QR -> QrScannerDialog(
            availableCodes = viewModel.vehicles.filter { it.fleetStatus == "AVAILABLE" || it.fleetStatus == "RESERVED" }.map { it.vehicleCode },
            initialCode = featured?.vehicleCode,
            onDismiss = { sheet = FleetSheet.NONE },
            onConfirmCode = { code ->
                sheet = FleetSheet.NONE
                viewModel.startRideByCode(code)
            },
        )
        FleetSheet.HISTORY -> RideHistoryDialog(viewModel.rideHistory, viewModel.isLoadingHistory, currency) { sheet = FleetSheet.NONE }
        FleetSheet.NONE -> Unit
    }
}

@Composable
private fun MobilityActionBar(showList: Boolean, onList: () -> Unit, onScan: () -> Unit, onHistory: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Surface(color = colors.surface, shadowElevation = 5.dp) {
        Row(
            Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (showList) MobilityShortcut("Danh sách xe", Icons.AutoMirrored.Outlined.FormatListBulleted, onList, Modifier.weight(1f))
            ItsButton(onScan, Modifier.weight(1.1f).heightIn(min = 47.dp), contentPadding = PaddingValues(horizontal = 7.dp)) {
                Icon(Icons.Outlined.QrCodeScanner, null, Modifier.size(18.dp))
                Spacer(Modifier.width(5.dp))
                Text("Quét QR", style = MaterialTheme.typography.labelLarge)
            }
            MobilityShortcut("Chuyến đi", Icons.Outlined.History, onHistory, Modifier.weight(1f))
        }
    }
}

@Composable
private fun MobilityShortcut(label: String, icon: ImageVector, onClick: () -> Unit, modifier: Modifier) {
    Surface(onClick = onClick, modifier = modifier, shape = MaterialTheme.shapes.medium, color = Color.Transparent) {
        Column(Modifier.heightIn(min = 48.dp).padding(horizontal = 3.dp, vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(icon, null, Modifier.size(21.dp))
            Text(label, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun VehicleList(vehicles: List<FleetVehicleDto>, selectedId: Long?, onSelect: (FleetVehicleDto) -> Unit, modifier: Modifier = Modifier) {
    if (vehicles.isEmpty()) {
        Text("Không có xe phù hợp.", modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
        return
    }
    LazyColumn(modifier.fillMaxWidth()) {
        items(vehicles, key = { it.id }) { vehicle ->
            Surface(onClick = { onSelect(vehicle) }, shape = MaterialTheme.shapes.medium,
                color = if (vehicle.id == selectedId) MaterialTheme.colorScheme.surfaceContainer else Color.Transparent) {
                VehicleRow(vehicle, highlighted = false)
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
    }
}

@Composable
private fun VehicleRow(vehicle: FleetVehicleDto, highlighted: Boolean) {
    val colors = MaterialTheme.colorScheme
    Row(
        Modifier.fillMaxWidth().padding(horizontal = if (highlighted) 12.dp else 7.dp, vertical = 9.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(shape = MaterialTheme.shapes.medium, color = colors.secondaryContainer, contentColor = colors.onSecondaryContainer) {
            val icon = if (vehicle.vehicleTypeName?.contains("Máy", ignoreCase = true) == true)
                Icons.Outlined.ElectricScooter else Icons.AutoMirrored.Outlined.DirectionsBike
            Icon(icon, null, Modifier.padding(9.dp).size(24.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(vehicle.brand, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            val distance = vehicle.distanceMeters?.let { "Cách ${it.toInt()} m · " }.orEmpty()
            Text(distance + "Pin ${vehicle.batteryLevel}% · " + fleetStatusLabel(vehicle.fleetStatus),
                style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
        }
        Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null, Modifier.size(21.dp), tint = colors.onSurfaceVariant)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VehicleDetailsSheet(
    vehicle: FleetVehicleDto,
    currency: NumberFormat,
    loading: Boolean,
    notice: String?,
    isError: Boolean,
    onDismiss: () -> Unit,
    onReserve: () -> Unit,
    onUnlock: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val reserved = vehicle.fleetStatus == "RESERVED"
    val rentable = vehicle.fleetStatus == "AVAILABLE" || reserved

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = colors.surface,
    ) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(start = 18.dp, end = 18.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(vehicle.brand, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(vehicle.vehicleCode + " · " + (vehicle.vehicleTypeName ?: "Xe cho thuê"),
                style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
            Surface(shape = MaterialTheme.shapes.large, color = colors.surfaceContainer) {
                Column(Modifier.fillMaxWidth().padding(13.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    Text(fleetStatusLabel(vehicle.fleetStatus) + " · Pin ${vehicle.batteryLevel}%", style = MaterialTheme.typography.titleSmall)
                    HorizontalDivider(color = colors.outlineVariant)
                    Text("${currency.format(vehicle.hourlyRate)} đ/giờ", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("Phí mở khóa ${currency.format(vehicle.unlockFee)} đ", color = colors.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                }
            }
            ItsNotice(notice, isError)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ItsButton(onReserve, Modifier.weight(1f).heightIn(min = 48.dp), enabled = !loading && rentable, outlined = true) {
                    Text(if (reserved) "Hủy giữ xe" else "Giữ xe")
                }
                ItsButton(onUnlock, Modifier.weight(1f).heightIn(min = 48.dp), enabled = !loading && rentable) {
                    Text("Mở khóa")
                }
            }
        }
    }
}

@Composable
private fun FleetMap(
    viewModel: MainViewModel,
    vehicles: List<FleetVehicleDto>,
    featured: FleetVehicleDto?,
    nativeMap: Boolean,
    onSelect: (FleetVehicleDto) -> Unit,
    modifier: Modifier,
) {
    val colors = MaterialTheme.colorScheme
    Box(modifier.clip(MaterialTheme.shapes.large).background(colors.surfaceContainer)) {
        if (nativeMap) {
            val center = LatLng(viewModel.userLat, viewModel.userLng)
            val position = rememberCameraPositionState { this.position = CameraPosition.fromLatLngZoom(center, 14f) }
            val tiles = remember {
                object : UrlTileProvider(256, 256) {
                    override fun getTileUrl(x: Int, y: Int, zoom: Int): URL =
                        URL("https://maps.vietmap.vn/api/tm/$zoom/$x/$y@2x.png?apikey=${BuildConfig.VIETMAP_API_KEY}")
                }
            }
            GoogleMap(Modifier.fillMaxSize(), cameraPositionState = position, uiSettings = MapUiSettings(zoomControlsEnabled = true)) {
                TileOverlay(tileProvider = tiles, zIndex = 0f)
                viewModel.stations.forEach { station ->
                    key(station.id) {
                        Marker(
                            state = MarkerState(LatLng(station.latitude, station.longitude)),
                            title = station.stationName,
                            snippet = "${station.availableVehicles} xe sẵn sàng",
                            icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE),
                        )
                    }
                }
                vehicles.forEach { vehicle ->
                    key(vehicle.id) {
                        Marker(
                            state = MarkerState(LatLng(vehicle.latitude, vehicle.longitude)),
                            title = vehicle.vehicleCode + " · " + vehicle.brand,
                            snippet = fleetStatusLabel(vehicle.fleetStatus),
                            onClick = { onSelect(vehicle); true },
                        )
                    }
                }
            }
        } else {
            VietMapLeafletWebView(
                stations = viewModel.stations,
                vehicles = vehicles,
                selectedVehicleId = featured?.id,
                centerLat = viewModel.userLat,
                centerLng = viewModel.userLng,
                onSelect = { id -> vehicles.firstOrNull { it.id == id }?.let(onSelect) },
            )
        }
        if (viewModel.isLoading && viewModel.vehicles.isEmpty()) {
            LinearProgressIndicator(Modifier.fillMaxWidth().align(Alignment.TopCenter))
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RideHistoryDialog(
    rides: List<RideSessionDto>,
    isLoading: Boolean,
    vndFormat: NumberFormat,
    onDismiss: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = colors.surface,
    ) {
        Column(Modifier.fillMaxWidth().padding(start = 18.dp, end = 18.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(13.dp)) {
            Text("Lịch sử chuyến đi", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            when {
                isLoading -> Box(Modifier.fillMaxWidth().height(90.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                rides.isEmpty() -> Text("Chưa có chuyến đi.", color = colors.onSurfaceVariant, modifier = Modifier.padding(vertical = 15.dp))
                else -> LazyColumn(Modifier.fillMaxWidth().heightIn(max = 360.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(rides, key = { it.sessionCode }) { ride ->
                        val fee = ride.totalFee ?: ride.estimatedFee
                        Surface(shape = MaterialTheme.shapes.large, color = colors.surfaceVariant) {
                            Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(ride.vehicleCode ?: ride.sessionCode, Modifier.weight(1f), fontWeight = FontWeight.Bold)
                                    Text(if (ride.paid) "Đã thanh toán" else "Chờ thanh toán", style = MaterialTheme.typography.labelMedium)
                                }
                                Text(ride.sessionCode, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("${ride.distanceKm} km · ${ride.durationMinutes} phút", Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                                    Text("${vndFormat.format(fee)} đ", fontWeight = FontWeight.SemiBold, color = colors.primary)
                                }
                            }
                        }
                    }
                }
            }
            ItsButton(onDismiss, Modifier.fillMaxWidth().heightIn(min = 48.dp), outlined = true) { Text("Đóng") }
        }
    }
}

private fun fleetStatusLabel(status: String) = when (status) {
    "AVAILABLE" -> "Sẵn sàng"
    "RESERVED" -> "Đang giữ"
    "IN_USE" -> "Đang thuê"
    "LOW_BATTERY" -> "Pin yếu"
    "MAINTENANCE" -> "Bảo trì"
    else -> "Tạm ngưng"
}


@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun VietMapLeafletWebView(
    stations: List<StationDto>,
    vehicles: List<FleetVehicleDto>,
    selectedVehicleId: Long?,
    centerLat: Double,
    centerLng: Double,
    onSelect: (Long) -> Unit,
) {
    val markers = remember(stations, vehicles) {
        org.json.JSONArray().apply {
            stations.forEach { station ->
                put(org.json.JSONObject()
                    .put("lat", station.latitude)
                    .put("lng", station.longitude)
                    .put("title", station.stationName)
                    .put("detail", "${station.availableVehicles}/${station.totalDocks} chỗ có xe")
                    .put("kind", "station"))
            }
            vehicles.forEach { vehicle ->
                put(org.json.JSONObject()
                    .put("id", vehicle.id)
                    .put("lat", vehicle.latitude)
                    .put("lng", vehicle.longitude)
                    .put("title", vehicle.vehicleCode + " · " + vehicle.brand)
                    .put("detail", "${fleetStatusLabel(vehicle.fleetStatus)} · Pin ${vehicle.batteryLevel}%")
                    .put("kind", vehicle.fleetStatus))
            }
        }.toString()
    }
    var nativeSize by remember { mutableStateOf(IntSize.Zero) }
    val animations = android.animation.ValueAnimator.areAnimatorsEnabled()
    val resize = "window.resizeMobilityMap && window.resizeMobilityMap(${nativeSize.width}, ${nativeSize.height});"
    val script = resize + "window.updateMobilityMap && window.updateMobilityMap($markers, ${selectedVehicleId ?: "null"}, $animations);"
    val latestScript by rememberUpdatedState(script)
    val latestSelect by rememberUpdatedState(onSelect)
    val html = remember(centerLat, centerLng) {
        """
        <!DOCTYPE html>
        <html lang="vi">
        <head>
          <meta charset="utf-8" />
          <meta name="viewport" content="width=device-width, initial-scale=1" />
          <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css"/>
          <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
          <style>
            html, body { margin: 0; width: 100%; height: 100%; background: #e4ece7; }
            #map { position: fixed; inset: 0; width: 100vw; height: 100vh; }
            .mobility-dot { display:grid; place-items:center; background:#353940; color:#fff; border:2px solid #fff;
              border-radius:50%; box-shadow:0 2px 8px #1a302957; font:700 12px system-ui; box-sizing:border-box; }
            .mobility-dot.AVAILABLE { background:#286153; }
            .mobility-dot.IN_USE { background:#315b84; }
            .mobility-dot.RESERVED { background:#825431; }
            .mobility-dot.LOW_BATTERY, .mobility-dot.MAINTENANCE { background:#964249; }
            .mobility-dot.selected { outline:2px solid #ae6237; outline-offset:1px; }
            .mobility-popup { max-height:180px; overflow:auto; min-width:140px; color:#142b29; font:13px system-ui; }
            .mobility-popup div + div { border-top:1px solid #dce7e2; margin-top:7px; padding-top:7px; }
            .mobility-popup a { display:block; font-weight:700; color:#284354; text-decoration:underline; }
            .mobility-popup small { display:block; margin-top:3px; color:#48625a; }
          </style>
        </head>
        <body>
          <div id="map" aria-label="Bản đồ xe và trạm"></div>
          <script>
            (function() {
              var container = document.getElementById('map');
              if (typeof L === 'undefined') {
                container.textContent = 'Không tải được bản đồ.';
                return;
              }
              var map = L.map(container, { zoomControl: true }).setView([$centerLat, $centerLng], 14);
              L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
                maxZoom: 19, attribution: '&copy; OpenStreetMap'
              }).addTo(map);
              L.tileLayer('https://maps.vietmap.vn/api/tm/{z}/{x}/{y}@2x.png?apikey=${BuildConfig.VIETMAP_API_KEY}', {
                maxZoom: 20, tileSize: 256, attribution: '&copy; VietMap'
              }).addTo(map);

              var layer = L.layerGroup().addTo(map);
              var markers = [], selectedId = null, previousId = null, sized = false;
              function render() {
                layer.clearLayers();
                var groups = [];
                markers.forEach(function(item) {
                  var point = map.project([item.lat, item.lng]);
                  var group = groups.find(function(candidate) { return candidate.point.distanceTo(point) < 37; });
                  if (group) group.items.push(item);
                  else groups.push({ point: point, anchor: [item.lat, item.lng], items: [item] });
                });
                groups.forEach(function(group) {
                  var selected = group.items.some(function(item) { return item.id != null && item.id === selectedId; });
                  var popup = document.createElement('div');
                  popup.className = 'mobility-popup';
                  group.items.forEach(function(item) {
                    var row = document.createElement('div');
                    var title = document.createElement(item.id == null ? 'strong' : 'a');
                    if (item.id != null) title.href = 'https://its-mobility.invalid/vehicle/' + item.id;
                    var detail = document.createElement('small');
                    title.textContent = item.title;
                    detail.textContent = item.detail;
                    row.append(title, detail);
                    popup.append(row);
                  });
                  var fleet = group.items.filter(function(item) { return item.kind !== 'station'; });
                  var count = fleet.length;
                  var kind = count === 1 ? fleet[0].kind : '';
                  var label = count ? String(count) : 'T';
                  var title = count ? count + ' xe tại đây' : group.items[0].title;
                  L.marker(group.anchor, {
                    icon: L.divIcon({ className: 'mobility-dot ' + kind + (selected ? ' selected' : ''),
                      html: label, iconSize: [29, 29], iconAnchor: [14, 14] }),
                    title: title, alt: title, zIndexOffset: selected ? 1000 : 0
                  }).bindPopup(popup, { maxWidth: 245 }).addTo(layer);
                });
              }
              window.updateMobilityMap = function(items, id, animate) {
                markers = items;
                selectedId = id;
                render();
                if (id !== previousId) {
                  previousId = id;
                  var selected = items.find(function(item) { return item.id === id; });
                  if (selected) map.panTo([selected.lat, selected.lng], { animate: animate, duration: 0.28 });
                }
              };
              window.resizeMobilityMap = function(width, height) {
                if (!width || !height) return;
                var ratio = window.devicePixelRatio || 1;
                container.style.width = Math.round(width / ratio) + 'px';
                container.style.height = Math.round(height / ratio) + 'px';
                map.invalidateSize({ animate:false });
                if (!sized) {
                  sized = true;
                  map.setView([$centerLat, $centerLng], 14, { animate:false });
                }
              };
              map.on('zoomend', render);
              new ResizeObserver(function() { map.invalidateSize({pan:false}); }).observe(container);
            })();
          </script>
        </body>
        </html>
        """.trimIndent()
    }

    key(html) {
        AndroidView(
            modifier = Modifier.fillMaxSize().onSizeChanged { nativeSize = it },
            factory = { context ->
                WebView(context).apply {
                    layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                    settings.javaScriptEnabled = true
                    webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                            val uri = request.url
                            if (request.isForMainFrame && uri.host == "its-mobility.invalid" && uri.pathSegments.firstOrNull() == "vehicle") {
                                uri.lastPathSegment?.toLongOrNull()?.let(latestSelect)
                                return true
                            }
                            return false
                        }

                        override fun onPageFinished(view: WebView, url: String?) {
                            view.evaluateJavascript(latestScript, null)
                        }
                    }
                    loadDataWithBaseURL("https://maps.vietmap.vn", html, "text/html", "UTF-8", null)
                }
            },
            onRelease = { it.destroy() },
            update = { webView ->
                if (webView.tag != script) {
                    webView.tag = script
                    webView.evaluateJavascript(script, null)
                }
            },
        )
    }
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

