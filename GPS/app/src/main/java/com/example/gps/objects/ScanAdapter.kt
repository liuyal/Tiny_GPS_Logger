package com.example.gps.objects

import android.bluetooth.BluetoothDevice
import android.bluetooth.le.ScanResult
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.example.gps.R
import kotlinx.android.synthetic.main.ble_cells.view.*

class ScanAdapter(private val devices : ArrayList<BluetoothDevice>, private val results: ArrayList<ScanResult>, private val listener: (BluetoothDevice) -> Unit) : RecyclerView.Adapter<ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CustomViewHolder {
        return CustomViewHolder(LayoutInflater.from(parent.context), parent)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        (holder as CustomViewHolder).bind(devices[position], results[position], listener)
    }

    override fun getItemCount(): Int = devices.size

    override fun getItemViewType(position: Int): Int = position

    class CustomViewHolder(inflater: LayoutInflater, parent: ViewGroup) : RecyclerView.ViewHolder(inflater.inflate(R.layout.ble_cells, parent, false)) {

        fun bind(device: BluetoothDevice, result: ScanResult, clickListener: (BluetoothDevice) -> Unit) {

            val name: TextView? = itemView.findViewById(R.id.ble_name_label)
            val mac: TextView? = itemView.findViewById(R.id.ble_mac_label)
            val status: TextView? = itemView.findViewById(R.id.ble_db_label)
            val rssi = "RSSI: " + result.rssi.toString() + "dbm"

            itemView.connect_btn.setOnClickListener { clickListener(device) }

            if (device.name != null && device.name.isNotEmpty()) name?.text = device.name
            else name?.text = "N/A"
            mac?.text = device.address
            status?.text = rssi
        }
    }
}
