package vn.edu.ut.its.mobility.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.DirectionsBike
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import vn.edu.ut.its.mobility.ui.ItsButton
import vn.edu.ut.its.mobility.ui.ItsEase
import vn.edu.ut.its.mobility.ui.ItsNotice
import vn.edu.ut.its.mobility.ui.MainViewModel

@Composable
fun AuthScreen(viewModel: MainViewModel) {
    var registering by rememberSaveable { mutableStateOf(false) }
    var username by rememberSaveable { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var fullName by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }

    val colors = MaterialTheme.colorScheme
    BoxWithConstraints(
        Modifier.fillMaxSize()
            .background(colors.background)
            .safeDrawingPadding()
            .imePadding(),
    ) {
        val minimumHeight = maxHeight
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).heightIn(min = minimumHeight).padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            AuthBanner(Modifier.widthIn(max = 480.dp).fillMaxWidth())

            Column(
                Modifier.widthIn(max = 480.dp).fillMaxWidth().padding(horizontal = 3.dp, vertical = 7.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    if (registering) "Tạo tài khoản" else "Đăng nhập",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                )
                OutlinedTextField(username, { username = it }, label = { Text("Tên đăng nhập") },
                    enabled = !viewModel.isLoading, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(
                    password, { password = it }, label = { Text("Mật khẩu") }, singleLine = true,
                    enabled = !viewModel.isLoading,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showPassword = !showPassword }) {
                            Icon(if (showPassword) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                if (showPassword) "Ẩn mật khẩu" else "Hiện mật khẩu")
                        }
                    },
                )
                AnimatedVisibility(
                    visible = registering,
                    enter = expandVertically(tween(240, easing = ItsEase)) + fadeIn(tween(190)),
                    exit = shrinkVertically(tween(170, easing = ItsEase)) + fadeOut(tween(100)),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                        OutlinedTextField(fullName, { fullName = it }, label = { Text("Họ và tên") },
                            enabled = !viewModel.isLoading, singleLine = true, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(phone, { phone = it }, label = { Text("Số điện thoại") },
                            enabled = !viewModel.isLoading, singleLine = true, modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone))
                    }
                }
                ItsNotice(viewModel.errorMessage, isError = true)
                ItsButton(
                    onClick = {
                        if (registering) viewModel.register(username, password, fullName, phone)
                        else viewModel.login(username, password)
                    },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    enabled = !viewModel.isLoading && username.isNotBlank() && password.isNotEmpty(),
                ) {
                    if (viewModel.isLoading) {
                        CircularProgressIndicator(Modifier.size(17.dp).clearAndSetSemantics {}, strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                        Text("Đang xử lý")
                    } else {
                        Text(if (registering) "Tạo tài khoản" else "Đăng nhập")
                    }
                }

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    if (!registering) {
                        TextButton(
                            onClick = { username = "user"; password = "password"; viewModel.clearMessages() },
                            enabled = !viewModel.isLoading,
                            contentPadding = PaddingValues(horizontal = 8.dp),
                        ) { Text("Điền tài khoản thử") }
                    }
                    TextButton(
                        onClick = { registering = !registering; viewModel.clearMessages() },
                        enabled = !viewModel.isLoading,
                        contentPadding = PaddingValues(horizontal = 8.dp),
                    ) { Text(if (registering) "Quay lại đăng nhập" else "Đăng ký") }
                }
            }
        }
    }
}



@Composable
private fun AuthBanner(modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val warm = colors.primary
    val reveal = remember { Animatable(0f) }
    LaunchedEffect(Unit) { reveal.animateTo(1f, tween(600, easing = ItsEase)) }
    Surface(modifier, shape = MaterialTheme.shapes.extraLarge, color = colors.primaryContainer, contentColor = colors.onPrimaryContainer) {
        BoxWithConstraints(Modifier.fillMaxWidth().heightIn(min = 142.dp)) {
            val showIllustration = maxWidth >= 285.dp
            if (showIllustration) {
                Canvas(Modifier.matchParentSize()) {
                    val origin = Offset(size.width * .84f, size.height * .52f)
                    for (radius in listOf(33.dp, 54.dp, 75.dp)) {
                        drawCircle(warm.copy(alpha = .27f * reveal.value), radius.toPx() * (.78f + .22f * reveal.value),
                            origin, style = Stroke(1.dp.toPx()))
                    }
                    val sweep = reveal.value
                    drawLine(warm.copy(alpha = .4f), Offset(size.width * .6f, size.height),
                        Offset(size.width * (.6f + .4f * sweep), size.height * (1f - sweep)), 1.dp.toPx())
                    drawCircle(warm.copy(alpha = reveal.value), 3.dp.toPx(), Offset(size.width * .78f, size.height * .18f))
                }
                Icon(Icons.AutoMirrored.Outlined.DirectionsBike, null,
                    Modifier.align(Alignment.CenterEnd).padding(end = 23.dp).size(48.dp).graphicsLayer {
                        alpha = reveal.value
                        translationX = (1f - reveal.value) * 10.dp.toPx()
                    }, tint = warm)
            }
            Column(
                Modifier.align(Alignment.CenterStart).fillMaxWidth(if (showIllustration) 0.68f else 1f).padding(18.dp).graphicsLayer {
                    alpha = reveal.value
                    translationY = (1f - reveal.value) * 7.dp.toPx()
                },
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Text("ITS Mobility", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Text("Thuê xe đạp và xe máy điện", color = colors.onPrimaryContainer.copy(alpha = .83f), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

