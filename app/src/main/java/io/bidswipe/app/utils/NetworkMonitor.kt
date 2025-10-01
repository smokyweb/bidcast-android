package io.bidswipe.app.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ✅ PRODUCTION-READY: Network connectivity monitor
 * 
 * Features:
 * - Reactive connectivity monitoring
 * - Works on all Android versions
 * - Detects actual internet (not just WiFi/Data connection)
 * - Singleton pattern for efficiency
 * 
 * Usage in ViewModel:
 * ```
 * @HiltViewModel
 * class MyViewModel @Inject constructor(
 *     private val networkMonitor: NetworkMonitor
 * ) : ViewModel() {
 *     
 *     val isOnline = networkMonitor.isConnected
 *     
 *     fun loadData() {
 *         if (networkMonitor.hasInternet()) {
 *             // Make API call
 *         } else {
 *             _error.value = "No internet connection"
 *         }
 *     }
 * }
 * ```
 */
@Singleton
class NetworkMonitor @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    
    private val _isConnected = MutableLiveData(checkCurrentConnection())
    val isConnected: LiveData<Boolean> = _isConnected
    
    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        
        override fun onAvailable(network: Network) {
            _isConnected.postValue(true)
            Alerts.log("NetworkMonitor", "Network connected")
        }
        
        override fun onLost(network: Network) {
            _isConnected.postValue(false)
            Alerts.log("NetworkMonitor", "Network disconnected")
        }
        
        override fun onCapabilitiesChanged(
            network: Network,
            networkCapabilities: NetworkCapabilities
        ) {
            val hasInternet = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            val isValidated = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            _isConnected.postValue(hasInternet && isValidated)
            Alerts.log("NetworkMonitor", "Network capabilities changed - Internet: $hasInternet, Validated: $isValidated")
        }
    }
    
    init {
        registerNetworkCallback()
    }
    
    /**
     * ✅ Register network callback to monitor connectivity changes
     */
    private fun registerNetworkCallback() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                // Android 7.0+
                connectivityManager.registerDefaultNetworkCallback(networkCallback)
            } else {
                // Android 5.0-6.0
                val request = NetworkRequest.Builder()
                    .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    .build()
                connectivityManager.registerNetworkCallback(request, networkCallback)
            }
            Alerts.log("NetworkMonitor", "Network callback registered")
        } catch (e: Exception) {
            Alerts.log("NetworkMonitor", "Failed to register network callback: ${e.message}")
        }
    }
    
    /**
     * ✅ Check current connection status (synchronous)
     * Use this for immediate checks before API calls
     */
    fun hasInternet(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
            
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        } else {
            // Fallback for older Android versions
            @Suppress("DEPRECATION")
            val networkInfo = connectivityManager.activeNetworkInfo
            networkInfo?.isConnectedOrConnecting == true
        }
    }
    
    /**
     * ✅ Check connection type
     */
    fun getConnectionType(): ConnectionType {
        if (!hasInternet()) return ConnectionType.NONE
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return ConnectionType.NONE
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return ConnectionType.NONE
            
            return when {
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> ConnectionType.WIFI
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> ConnectionType.MOBILE
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> ConnectionType.ETHERNET
                else -> ConnectionType.UNKNOWN
            }
        } else {
            @Suppress("DEPRECATION")
            return when (connectivityManager.activeNetworkInfo?.type) {
                ConnectivityManager.TYPE_WIFI -> ConnectionType.WIFI
                ConnectivityManager.TYPE_MOBILE -> ConnectionType.MOBILE
                ConnectivityManager.TYPE_ETHERNET -> ConnectionType.ETHERNET
                else -> ConnectionType.UNKNOWN
            }
        }
    }
    
    /**
     * ✅ Check current connection (for initialization)
     */
    private fun checkCurrentConnection(): Boolean {
        return hasInternet()
    }
    
    /**
     * ✅ Cleanup (call this if needed, though not typically necessary for singleton)
     */
    fun unregister() {
        try {
            connectivityManager.unregisterNetworkCallback(networkCallback)
            Alerts.log("NetworkMonitor", "Network callback unregistered")
        } catch (e: Exception) {
            Alerts.log("NetworkMonitor", "Failed to unregister network callback: ${e.message}")
        }
    }
    
    enum class ConnectionType {
        WIFI, MOBILE, ETHERNET, NONE, UNKNOWN
    }
}

