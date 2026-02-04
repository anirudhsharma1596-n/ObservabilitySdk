package com.axoid.observabilitysdk

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.axoid.observabilitysdk.ui.theme.ObservabilitySdkTheme
import androidx.compose.material3.Button
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import com.axoid.sdk.ObservabilitySdk // Import our SDK

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Leave a breadcrumb when the activity is created
        ObservabilitySdk.leaveBreadcrumb("lifecycle", "MainActivity.onCreate")

        // ... (checker, enableEdgeToEdge, etc.)

        setContent {
            ObservabilitySdkTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    // For simplicity, we'll just show our test screen directly for now.
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Observability SDK Test App")
                        Spacer(Modifier.height(16.dp))

                        // A button to leave a custom breadcrumb
                        Button(onClick = {
                            ObservabilitySdk.leaveBreadcrumb(
                                type = "ui.click",
                                message = "User tapped 'Leave Custom Breadcrumb' button"
                            )
                        }) {
                            Text("Leave Custom Breadcrumb")
                        }

                        Spacer(Modifier.height(16.dp))

                        // A button that will crash the app
                        Button(onClick = {
                            ObservabilitySdk.leaveBreadcrumb(
                                type = "ui.click",
                                message = "User tapped 'Crash App' button"
                            )
                            // This will cause a NullPointerException
                            throw RuntimeException("This is a test crash from the SDK!")
                        }) {
                            Text("Crash App")
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Leave another lifecycle breadcrumb
        ObservabilitySdk.leaveBreadcrumb("lifecycle", "MainActivity.onResume")
    }
}
