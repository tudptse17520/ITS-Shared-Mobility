package vn.edu.ut.its.mobility

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.lifecycle.viewmodel.compose.viewModel
import vn.edu.ut.its.mobility.ui.MainViewModel
import vn.edu.ut.its.mobility.ui.screens.ActiveRideScreen
import vn.edu.ut.its.mobility.ui.screens.AuthScreen
import vn.edu.ut.its.mobility.ui.screens.HomeMapScreen
import vn.edu.ut.its.mobility.ui.screens.PaymentVnpayScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                val vm: MainViewModel = viewModel()
                when {
                    !vm.isAuthenticated -> AuthScreen(viewModel = vm)
                    vm.completedRideForPayment != null -> PaymentVnpayScreen(
                        ride = vm.completedRideForPayment!!,
                        viewModel = vm
                    )
                    vm.activeRide != null -> ActiveRideScreen(
                        ride = vm.activeRide!!,
                        viewModel = vm
                    )
                    else -> HomeMapScreen(viewModel = vm)
                }
            }
        }
    }
}

