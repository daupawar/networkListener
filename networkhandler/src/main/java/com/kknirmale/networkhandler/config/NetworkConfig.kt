package com.kknirmale.networkhandler.config

import android.content.Context
import android.content.IntentFilter
import android.net.ConnectivityManager
import com.kknirmale.networkhandler.listener.NetworkStateListener
import com.kknirmale.networkhandler.receivers.NetworkStateReceiver
import java.lang.ref.WeakReference


/**

@see <a href="https://github.com/JobGetabu/DroidNet/blob/master/droidnet/src/main/java/com/droidnet/DroidNet.java">This class refer from DroidNet</a>


 */
class NetworkConfig(context: Context) : NetworkStateReceiver.InternetCheckListener {

    override fun onInternetSpeed(speedType: Int) {
        reportNetworkSpeedType(speedType)
    }

    override fun onComplete(connected: Boolean) {
        if (connected) {
            reportInternetAvailabilityStatus(connected)
        } else {
            reportInternetAvailabilityStatus(false)
        }
    }

    companion object {
        val lock = Any()
        var mInstance: NetworkConfig? = null

        /*
            Returns instance of NetworkConfig class
         */
        fun initNetworkConfig(context: Context): NetworkConfig {
            if (mInstance == null) {
                synchronized(lock) {
                    if (mInstance == null) {
                        mInstance = NetworkConfig(context)
                    }
                }
            }
            return mInstance!!
        }

        fun getInstance(): NetworkConfig {
            if (mInstance == null) {
                throw IllegalStateException("Error in getting instance")
            }
            return mInstance!!
        }
    }

    private var configReferences: WeakReference<Context>? = null
    private var networkStateListenerWeakReferenceList: MutableList<WeakReference<NetworkStateListener>>? = null
    private var networkChangeReceiver: NetworkStateReceiver? = null
    private var isNetworkStatusRegistered = false
    private var isNetworkConnected = false
    private var networkTypeValue = 0

    init {
        val appContext = context.applicationContext
        configReferences = WeakReference(appContext)
        networkStateListenerWeakReferenceList = ArrayList()
    }

    /*
        Register broadcast receiver from here
     */
    private fun registerNetworkChangeReceiver() {
        val context = configReferences?.get()
        if (context != null && !isNetworkStatusRegistered) {
            networkChangeReceiver = NetworkStateReceiver()
            val intentFilter = IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
            networkChangeReceiver?.setInternetStateChangeListener(this)
            context.registerReceiver(networkChangeReceiver, intentFilter)
            isNetworkStatusRegistered = true
        }
    }

    /*
        Unregister broadcast receiver
     */

    private fun unregisterNetworkChangeReceiver() {
        val context = configReferences?.get()
        if (context != null && networkChangeReceiver != null && isNetworkStatusRegistered) {
            context.unregisterReceiver(networkChangeReceiver)
            networkChangeReceiver?.removeInternetStateChangeListener()
        }

        networkChangeReceiver = null
        isNetworkStatusRegistered = false
    }

    /*
        Get report of internet availability status from from listener
     */
    private fun reportInternetAvailabilityStatus(isInternetAvailable: Boolean) {
        isNetworkConnected = isInternetAvailable
        if (networkStateListenerWeakReferenceList == null) {
            return
        }

        val listenerIterator: MutableIterator<WeakReference<NetworkStateListener>>? = networkStateListenerWeakReferenceList?.iterator()
        if (listenerIterator != null) {
            while (listenerIterator.hasNext()) {

                val listenerReference: WeakReference<NetworkStateListener> = listenerIterator.next()

                val statusListener: NetworkStateListener? = listenerReference.get()
                if (statusListener == null) {
                    listenerIterator.remove()
                }

                statusListener?.onNetworkStatusChanged(isInternetAvailable)
            }
        }

        if (networkStateListenerWeakReferenceList != null) {
            if (networkStateListenerWeakReferenceList?.isEmpty()!!) {
                unregisterNetworkChangeReceiver()
            }
        }

    }

    /*
        Get report of internet speed value from from listener
     */
    private fun reportNetworkSpeedType(speedType: Int) {
        networkTypeValue = speedType
        if (networkStateListenerWeakReferenceList == null) {
            return
        }

        val listenerIterator: MutableIterator<WeakReference<NetworkStateListener>>? = networkStateListenerWeakReferenceList?.iterator()
        if (listenerIterator != null) {
            while (listenerIterator.hasNext()) {

                val listenerReference: WeakReference<NetworkStateListener> = listenerIterator.next()

                val statusListener: NetworkStateListener? = listenerReference.get()
                if (statusListener == null) {
                    listenerIterator.remove()
                }

                statusListener?.onNetworkSpeedChanged(speedType)
            }
        }

        if (networkStateListenerWeakReferenceList!!.isEmpty()) {
            unregisterNetworkChangeReceiver()
        }
    }

    /*
        Attach all connectivity listener to listen network state from broadcast receiver
    */
    fun addNetworkConnectivityListener(statusListener: NetworkStateListener?) {
        if (statusListener == null) {
            return
        }

        networkStateListenerWeakReferenceList?.add(WeakReference(statusListener))
        if (networkStateListenerWeakReferenceList?.size == 1) {
            registerNetworkChangeReceiver()
            return
        }

        reportInternetAvailabilityStatus(isNetworkConnected)
        reportNetworkSpeedType(networkTypeValue)

    }

    /*
        Remove connectivity listener and unregister broadcast receiver
    */
    fun removeNetworkConnectivityListener(statusListener: NetworkStateListener?) {
        if (statusListener == null) {
            return
        }

        if (networkStateListenerWeakReferenceList?.isNullOrEmpty()!!) {
            return
        }

        val iterable: MutableIterator<WeakReference<NetworkStateListener>>? = networkStateListenerWeakReferenceList?.iterator()
        if (iterable != null) {
            while (iterable.hasNext()) {

                val stateReference: WeakReference<NetworkStateListener> = iterable.next()

                val stateListener: NetworkStateListener? = stateReference.get()
                if (stateListener == null) {
                    stateReference.clear()
                    iterable.remove()
                    continue
                }

                if (stateListener == statusListener) {
                    stateReference.clear()
                    iterable.remove()
                    break
                }

                if (networkStateListenerWeakReferenceList?.size == 0) {
                    unregisterNetworkChangeReceiver()
                }
            }
        }
    }

    fun removeAllNetworkConnectivityListener() {
        if (networkStateListenerWeakReferenceList == null) {
            return
        }

        val listenerIterator: MutableIterator<WeakReference<NetworkStateListener>>? = networkStateListenerWeakReferenceList?.iterator()

        if (listenerIterator != null) {
            while (listenerIterator.hasNext()) {
                val listenerReference: WeakReference<NetworkStateListener> = listenerIterator.next()

                listenerReference.clear()

                listenerIterator.remove()
            }
        }

        unregisterNetworkChangeReceiver()
    }


}

