package com.axoid.observabilitysdk



import android.app.Application
import com.axoid.sdk.ObservabilitySdk // Import our SDK

class MainApp : Application() {
    override fun onCreate() {
        super.onCreate()

        // Initialize our Observability SDK as soon as the app starts.
        ObservabilitySdk.init(this)
    }
}
