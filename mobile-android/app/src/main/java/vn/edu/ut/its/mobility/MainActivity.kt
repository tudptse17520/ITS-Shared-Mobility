package vn.edu.ut.its.mobility

import android.animation.ValueAnimator
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import vn.edu.ut.its.mobility.model.RideSessionDto
import vn.edu.ut.its.mobility.ui.ItsEase
import vn.edu.ut.its.mobility.ui.ItsTheme
import vn.edu.ut.its.mobility.ui.MainViewModel
import vn.edu.ut.its.mobility.ui.screens.ActiveRideScreen
import vn.edu.ut.its.mobility.ui.screens.AuthScreen
import vn.edu.ut.its.mobility.ui.screens.HomeMapScreen
import vn.edu.ut.its.mobility.ui.screens.OpeningFilm
import vn.edu.ut.its.mobility.ui.screens.PaymentVnpayScreen

private sealed interface Screen {
    data object SignIn : Screen
    data object Map : Screen
    data class Ride(val ride: RideSessionDto) : Screen
    data class Payment(val ride: RideSessionDto) : Screen
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ItsTheme {
                val vm: MainViewModel = viewModel()
                var showOpeningFilm by rememberSaveable { mutableStateOf(ValueAnimator.areAnimatorsEnabled()) }
                val payment = vm.completedRideForPayment
                val ride = vm.activeRide
                val screen = when {
                    !vm.isAuthenticated -> Screen.SignIn
                    payment != null -> Screen.Payment(payment)
                    ride != null -> Screen.Ride(ride)
                    else -> Screen.Map
                }
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    AnimatedContent(
                        targetState = showOpeningFilm,
                        transitionSpec = {
                            fadeIn(tween(240)).togetherWith(fadeOut(tween(240)))
                        },
                        label = "openingFilm",
                    ) { showFilm ->
                        if (showFilm) {
                            OpeningFilm { showOpeningFilm = false }
                        } else {
                            AnimatedContent(
                                targetState = screen,
                                contentKey = { it::class },
                                transitionSpec = {
                                    (fadeIn(tween(200)) + slideInVertically(tween(260, easing = ItsEase)) { it / 32 })
                                        .togetherWith(fadeOut(tween(100)))
                                },
                                label = "screen",
                            ) { current ->
                                when (current) {
                                    Screen.SignIn -> AuthScreen(vm)
                                    Screen.Map -> HomeMapScreen(vm)
                                    is Screen.Ride -> ActiveRideScreen(current.ride, vm)
                                    is Screen.Payment -> PaymentVnpayScreen(current.ride, vm)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
