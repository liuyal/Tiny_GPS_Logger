package com.gps.ui.home

import android.content.Context
import android.content.ContextWrapper
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.gps.MainActivity
import com.gps.R
import com.gps.objects.*
import kotlinx.android.synthetic.main.fragment_home.view.*

class HomeFragment : Fragment() {

    private lateinit var homeViewModel: HomeViewModel

    private var autoCheckRunning: Boolean = false
    private var statueCheckThread: Thread? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        homeViewModel = ViewModelProvider(this).get(HomeViewModel::class.java)
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        view.action_button_A.setOnClickListener {
            val main = activity as MainActivity?
            if (main != null && main.checkBTon() && GlobalApp.BLE?.connectionState == STATE_CONNECTED) {
                if (this.statueCheckThread != null && this.statueCheckThread?.isAlive!!) this.statueCheckThread?.interrupt()
                this.statueCheckThread = null
                this.disconnectionUIHandler()
                view?.action_button_A?.setColorFilter(Color.WHITE)
            } else if (main != null && main.checkBTon()) {
                if (GlobalApp.BLE == null) {
                    GlobalApp.BLE = BLEDevice(main as Context, activity as ContextWrapper)
                    GlobalApp.BLE!!.initialize()
                }
                if (this.statueCheckThread != null && this.statueCheckThread?.isAlive!!) this.statueCheckThread?.interrupt()
                this.statueCheckThread = null
                this.statueCheckThread = Thread(Runnable { this.connectionUIHandler() })
                this.statueCheckThread!!.start()
            }
        }

        view.action_button_B.setOnClickListener {
            if (this.statueCheckThread != null && this.statueCheckThread?.isAlive!!) this.statueCheckThread?.interrupt()
            this.statueCheckThread = null
            this.statueCheckThread = Thread(Runnable { this.toggleGPS() })
            this.statueCheckThread!!.start()
        }

        view.action_button_D.setOnClickListener {
            if (this.statueCheckThread != null && this.statueCheckThread?.isAlive!!) this.statueCheckThread?.interrupt()
            this.statueCheckThread = null
            this.statueCheckThread = Thread(Runnable { this.toggleLogging() })
            this.statueCheckThread!!.start()
        }

        view.action_button_E.setOnClickListener {
            val main = activity as MainActivity?
            if (main != null && main.checkBTon() && !autoCheckRunning) {
                autoCheckRunning = true
                if (this.statueCheckThread != null && this.statueCheckThread?.isAlive!!) this.statueCheckThread?.interrupt()
                this.statueCheckThread = null
                this.statueCheckThread = Thread(Runnable { this.checkTaskLoop() })
                this.statueCheckThread!!.start()
            } else if (main != null && main.checkBTon() && this.statueCheckThread != null && this.statueCheckThread?.isAlive!!) {
                autoCheckRunning = false
                this.statueCheckThread?.interrupt()
                this.statueCheckThread = null
                view?.action_button_E?.setImageResource(R.drawable.ic_sync_disabled_black_24dp)
                view?.action_button_E?.setColorFilter(Color.WHITE)
            }
        }
        return view
    }

    override fun onStart() {
        super.onStart()
        this.updateUIDeviceInfo()
        Log.d("HOME", "Start h Fragment")
    }

    override fun onResume() {
        super.onResume()
        this.bleLinkCheck()
        Log.d("HOME", "onResume h Fragment")
    }

    override fun onPause() {
        super.onPause()
        Log.d("HOME", "Pause h Fragment")
    }

    override fun onStop() {
        super.onStop()
        if (this.statueCheckThread != null && this.statueCheckThread?.isAlive!!) this.statueCheckThread?.interrupt()
        this.statueCheckThread = null
        Log.d("HOME", "Stopped h Fragment")
    }

    private fun connectionCheck(): Boolean {
        if (GlobalApp.BLE?.bleAddress == null || GlobalApp.BLE?.bleAddress == "") {
            activity?.runOnUiThread { this.updateUIDeviceInfo() }
            activity?.runOnUiThread { this.disconnectionUIHandler() }
            Toast.makeText(activity, "No Device Paired", Toast.LENGTH_SHORT).show()
            return false
        }
        val start = System.currentTimeMillis()
        while (GlobalApp.BLE?.connectionState != STATE_CONNECTED || GlobalApp.BLE?.bleGATT == null) {
            if (System.currentTimeMillis() - start > 10 * TIME_OUT) return false
            GlobalApp.BLE?.connect(GlobalApp.BLE?.bleAddress!!)!!
            Thread.sleep(250)
        }
        activity?.runOnUiThread { this.updateUIDeviceInfo() }
        Thread.sleep(1000)
        return true
    }

    private fun updateUIAll() {
        this.updateUIStatusBar()
        this.updateUIStatusButtons()
        this.updateUIStatusInfo()
        this.updateUICoordinateInfo()
    }

    private fun toggleGPS() {
        try {
            Log.d("HOME", "Toggled GPS")
            activity?.runOnUiThread { view?.status_progress_bar?.visibility = View.VISIBLE }
            val connected = connectionCheck()
            if (GlobalApp.BLE?.connectionState == STATE_CONNECTED || connected) {
                if (GlobalApp.BLE?.gpsStatusFlags?.get(GPS_ON_FLAG_INDEX)!!) {
                    if (!GlobalApp.BLE?.GPSoff()!!) throw IllegalArgumentException("GPS off Failed")
                } else {
                    if (!GlobalApp.BLE?.GPSon()!!) throw IllegalArgumentException("GPS on Failed")
                    GlobalApp.BLE?.fetchGPSData()
                }
                GlobalApp.BLE?.fetchDeviceStatus()
                activity?.runOnUiThread { this.updateUIAll() }
            }
            activity?.runOnUiThread { view?.status_progress_bar?.visibility = View.GONE }
            if (this.autoCheckRunning) this.checkTaskLoop()
            Log.d("HOME", "Toggled GPS " + GlobalApp.BLE?.gpsStatusFlags?.get(GPS_ON_FLAG_INDEX).toString())
        } catch (e: Throwable) {
            Log.e("HOME", "TOGGLE GPS ERROR")
        }
    }

    private fun toggleLogging() {
        try {
            Log.d("HOME", "Toggled GPS Logging")
            activity?.runOnUiThread { view?.status_progress_bar?.visibility = View.VISIBLE }
            val connected = connectionCheck()
            if (GlobalApp.BLE?.connectionState == STATE_CONNECTED || connected) {
                if (GlobalApp.BLE?.gpsStatusFlags?.get(GPS_LOGGING_FLAG_INDEX)!!) {
                    if (!GlobalApp.BLE?.GPSloggingOff()!!) throw IllegalArgumentException("GPS Logging off Failed")
                } else {
                    if (!GlobalApp.BLE?.GPSloggingOn()!!) throw IllegalArgumentException("GPS Logging on Failed")
                    GlobalApp.BLE?.fetchGPSData()
                }
                GlobalApp.BLE?.fetchDeviceStatus()
                activity?.runOnUiThread { this.updateUIAll() }
            }
            activity?.runOnUiThread { view?.status_progress_bar?.visibility = View.GONE }
            if (this.autoCheckRunning) this.checkTaskLoop()
            Log.d("HOME", "Toggled GPS Logging " + GlobalApp.BLE?.gpsStatusFlags?.get(GPS_LOGGING_FLAG_INDEX).toString())
        } catch (e: Throwable) {
            Log.e("HOME", "TOGGLE GPS Logging ERROR")
        }
    }

    private fun checkTaskLoop() {
        try {
            Log.d("HOME", "STATUS CHECK START")
            activity?.runOnUiThread { view?.status_progress_bar?.visibility = View.VISIBLE }
            if (!connectionCheck()) {
                view?.status_progress_bar?.visibility = View.GONE
                return
            }
            val delay = 5 // TODO: configurable delay (in settings fragment)
            while (true) {
                try {
                    if (GlobalApp.BLE?.connectionState == STATE_CONNECTED || GlobalApp.BLE?.bleGATT != null) {
                        GlobalApp.BLE?.fetchDeviceStatus()
                        if (GlobalApp.BLE?.gpsStatusFlags?.get(GPS_FIX_FLAG_INDEX)!!) GlobalApp.BLE?.fetchGPSData()
                        else GlobalApp.BLE?.gpsData = ""
                        activity?.runOnUiThread { this.updateUIAll() }
                        activity?.runOnUiThread { view?.status_progress_bar?.visibility = View.GONE }
                        Thread.sleep((delay * TIME_OUT).toLong())
                    } else throw IllegalArgumentException()
                } catch (e: Throwable) {
                    break
                }
            }
            Log.d("HOME", "STATUS CHECK STOPPED")
        } catch (e: Throwable) {
            Log.e("HOME", "STATUS CHECK ERROR")
        }
    }

    private fun connectionUIHandler() {
        try {
            Log.d("HOME", "Connection UI Handler Starting")
            activity?.runOnUiThread { view?.status_progress_bar?.visibility = View.VISIBLE }
            val connected = connectionCheck()
            if (GlobalApp.BLE?.connectionState == STATE_CONNECTED || connected) {
                GlobalApp.BLE?.fetchDeviceStatus()
                activity?.runOnUiThread { this.updateUIAll() }
            } else {
                activity?.runOnUiThread { this.disconnectionUIHandler() }
                Log.d("HOME", "Unable to Connect to Device")
            }
            activity?.runOnUiThread { view?.status_progress_bar?.visibility = View.GONE }
            Log.d("HOME", "Connection UI Handler Complete")
        } catch (e: Throwable) {
            Log.e("MAP", "Connection Handler Error")
        }
    }

    private fun disconnectionUIHandler() {
        try {
            this.autoCheckRunning = false
            view?.status_progress_bar?.visibility = View.GONE
            GlobalApp.BLE?.disconnect()
            GlobalApp.BLE?.gpsStatusFlags?.fill(false, 0, NUMBER_OF_FLAGS)
            GlobalApp.BLE?.gpsData = ""
            this.updateUIAll()
        } catch (e: Throwable) {
            Log.e("HOME", "Disconnection UI Update Error")
        }
    }

    private fun updateUIStatusBar() {
        try {
            val statusBarArray: ArrayList<View>? = ArrayList(0)
            statusBarArray?.add(view?.statusBar1!!)
            statusBarArray?.add(view?.statusBar2!!)
            statusBarArray?.add(view?.statusBar3!!)
            statusBarArray?.add(view?.statusBar4!!)
            for (i in 0 until NUMBER_OF_FLAGS) {
                val view: View? = statusBarArray?.get(i)
                if (statusBarArray != null && GlobalApp.BLE?.gpsStatusFlags?.get(i)!!) {
                    if (view != null) {
                        statusBarArray[i].background = ContextCompat.getDrawable(requireContext(), R.drawable.status_on)
                    }
                } else if (!GlobalApp.BLE?.gpsStatusFlags?.get(i)!!) {
                    if (view != null) {
                        statusBarArray[i].background = ContextCompat.getDrawable(requireContext(), R.drawable.status_off)
                    }
                }
            }
        } catch (e: Throwable) {
            Log.e("HOME", "Update UI Status Bar Error")
        }
    }

    private fun updateUIStatusButtons() {
        try {
            if (GlobalApp.BLE?.connectionState == STATE_CONNECTED) {
                view?.action_button_A?.setImageResource(R.drawable.ic_link_black_24dp)
                view?.action_button_A?.setColorFilter(Color.GREEN)
            } else if (GlobalApp.BLE?.connectionState != STATE_CONNECTED) {
                view?.action_button_A?.setImageResource(R.drawable.ic_link_black_24dp)
                view?.action_button_A?.setColorFilter(Color.WHITE)
            }

            if (GlobalApp.BLE?.gpsStatusFlags?.get(GPS_FIX_FLAG_INDEX)!!) {
                view?.action_button_C?.setImageResource(R.drawable.ic_gps_fixed_black_24dp)
                view?.action_button_C?.setColorFilter(Color.GREEN)
            } else if (!GlobalApp.BLE?.gpsStatusFlags?.get(GPS_FIX_FLAG_INDEX)!!) {
                view?.action_button_C?.setImageResource(R.drawable.ic_gps_not_fixed_black_24dp)
                view?.action_button_C?.setColorFilter(Color.WHITE)
            }

            if (GlobalApp.BLE?.gpsStatusFlags?.get(GPS_ON_FLAG_INDEX)!!) {
                view?.action_button_B?.setImageResource(R.drawable.ic_location_on_black_24dp)
                view?.action_button_B?.setColorFilter(Color.GREEN)
            } else if (!GlobalApp.BLE?.gpsStatusFlags?.get(GPS_ON_FLAG_INDEX)!!) {
                view?.action_button_B?.setImageResource(R.drawable.ic_location_off_black_24dp)
                view?.action_button_C?.setImageResource(R.drawable.ic_gps_off_black_24dp)
                view?.action_button_B?.setColorFilter(Color.WHITE)
                view?.action_button_C?.setColorFilter(Color.WHITE)
            }

            if (GlobalApp.BLE?.gpsStatusFlags?.get(GPS_LOGGING_FLAG_INDEX)!!) {
                view?.action_button_D?.setImageResource(R.drawable.ic_fiber_manual_record_black_24dp)
                view?.action_button_D?.setColorFilter(Color.RED)
            } else if (!GlobalApp.BLE?.gpsStatusFlags?.get(GPS_LOGGING_FLAG_INDEX)!!) {
                view?.action_button_D?.setImageResource(R.drawable.ic_fiber_manual_record_black_24dp)
                view?.action_button_D?.setColorFilter(Color.WHITE)
            }

            if (this.autoCheckRunning) {
                view?.action_button_E?.setImageResource(R.drawable.ic_sync_black_24dp)
                view?.action_button_E?.setColorFilter(Color.GREEN)
            } else if (!this.autoCheckRunning) {
                view?.action_button_E?.setImageResource(R.drawable.ic_sync_disabled_black_24dp)
                view?.action_button_E?.setColorFilter(Color.WHITE)
            }

        } catch (e: Throwable) {
            Log.e("HOME", "Update UI Status Button Error")
        }
    }

    private fun updateUIDeviceInfo() {
        try {
            val macLabel: TextView? = view?.mac_text_label
            val deviceLabel: TextView? = view?.device_text_label
            val macAddress = GlobalApp.BLE?.bleAddress
            if (macAddress != "" && macLabel != null && deviceLabel != null) {
                deviceLabel.text = DEVICE_CODE_NAME
                macLabel.text = macAddress
            } else {
                GlobalApp.BLE?.initialize()
                if (macLabel != null && deviceLabel != null) {
                    deviceLabel.text = getString(R.string.initial_ble_name)
                    macLabel.text = getString(R.string.initial_ble_mac)
                }
            }
        } catch (e: Throwable) {
            Log.e("HOME", "Update Device Info Error")
        }
    }

    private fun updateUIStatusInfo() {
        try {
            val iconArray: ArrayList<AppCompatImageView>? = ArrayList(0)
            iconArray?.add(view?.connected_text_label!!)
            iconArray?.add(view?.gps_on_text_label!!)
            iconArray?.add(view?.fix_text_label!!)
            iconArray?.add(view?.log_text_label!!)
            for (i in 0 until NUMBER_OF_FLAGS) {
                val icon: AppCompatImageView? = iconArray?.get(i)
                if (GlobalApp.BLE?.gpsStatusFlags?.get(i)!!) {
                    if (icon != null) {
                        iconArray[i].setImageResource(R.drawable.ic_check_black_24dp)
                        iconArray[i].setColorFilter(Color.parseColor("#00b250"))
                    }
                } else if (iconArray != null && !GlobalApp.BLE?.gpsStatusFlags?.get(i)!!) {
                    if (icon != null) {
                        iconArray[i].setImageResource(R.drawable.ic_close_black_24dp)
                        iconArray[i].setColorFilter(Color.argb(255, 255, 0, 0))
                    }
                }
            }
        } catch (e: Throwable) {
            Log.e("HOME", "Update UI Status Info Error")
        }
    }

    private fun updateUICoordinateInfo() {
        try {
            val gpsDate: TextView? = view?.date_text_label
            val gpsTime: TextView? = view?.time_text_label
            val satellite: TextView? = view?.sat_text_label
            val latitude: TextView? = view?.lat_text_label
            val longitude: TextView? = view?.long_text_label
            var gpsData = GlobalApp.BLE?.gpsData
            if (gpsData != null && gpsData != "") {
                gpsData = gpsData.substring(gpsData.indexOf('[') + 1, gpsData.indexOf(']'))
                val gpdDataList = gpsData.split(',') as ArrayList<String>
                if (gpdDataList[DATE_MONTH_INDEX].toInt() < 10) gpdDataList[DATE_MONTH_INDEX] = "0" + gpdDataList[DATE_MONTH_INDEX]
                if (gpdDataList[DATE_DAY_INDEX].toInt() < 10) gpdDataList[DATE_DAY_INDEX] = "0" + gpdDataList[DATE_DAY_INDEX]
                if (gpdDataList[TIME_HOUR_INDEX].toInt() < 10) gpdDataList[TIME_HOUR_INDEX] = "0" + gpdDataList[TIME_HOUR_INDEX]
                if (gpdDataList[TIME_MINUTE_INDEX].toInt() < 10) gpdDataList[TIME_MINUTE_INDEX] = "0" + gpdDataList[TIME_MINUTE_INDEX]
                if (gpdDataList[TIME_SECOND_INDEX].toInt() < 10) gpdDataList[TIME_SECOND_INDEX] = "0" + gpdDataList[TIME_SECOND_INDEX]
                gpsDate?.text = getString(R.string.gpsDate, gpdDataList[DATE_YEAR_INDEX], gpdDataList[DATE_MONTH_INDEX], gpdDataList[DATE_DAY_INDEX])
                gpsTime?.text = getString(R.string.gpsTime, gpdDataList[TIME_HOUR_INDEX], gpdDataList[TIME_MINUTE_INDEX], gpdDataList[TIME_SECOND_INDEX])
                satellite?.text = gpdDataList[SATELLITES_VALUE_INDEX]
                latitude?.text = gpdDataList[LOCATION_LAT_INDEX]
                longitude?.text = gpdDataList[LOCATION_LNG_INDEX]
            } else if (gpsData == null || gpsData == "") {
                gpsDate?.text = getString(R.string.init_date)
                gpsTime?.text = getString(R.string.init_time)
                satellite?.text = getString(R.string.init_sat)
                latitude?.text = getString(R.string.init_lat)
                longitude?.text = getString(R.string.init_long)
            }
        } catch (e: Throwable) {
            Log.e("HOME", "Update UI Coordinate Info Error")
        }
    }

    private fun bleLinkCheck() {
        val main = activity as MainActivity?
        if (main != null && main.checkBTon() && GlobalApp.BLE?.connectionState == STATE_CONNECTED) {
            if (GlobalApp.BLE == null) {
                GlobalApp.BLE = BLEDevice(main as Context, activity as ContextWrapper)
                GlobalApp.BLE!!.initialize()
            }
            if (this.statueCheckThread != null && this.statueCheckThread?.isAlive!!) this.statueCheckThread?.interrupt()
            this.statueCheckThread = null
            this.statueCheckThread = Thread(Runnable { this.connectionUIHandler() })
            this.statueCheckThread!!.start()
        }
    }
}
