package com.example.gps.objects

import android.app.Application
import android.app.Activity
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.example.gps.R
import kotlinx.android.synthetic.main.ble_cells.view.*

class RecyclerAdapter(private val items : ArrayList<BluetoothDevice>) : RecyclerView.Adapter<ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerAdapter.CutsomViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return CutsomViewHolder(inflater, parent)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val device : BluetoothDevice = items[position]
        (holder as CutsomViewHolder).bind(device)
    }

    override fun getItemCount(): Int = items.size

    override fun getItemViewType(position: Int): Int = position

    class CutsomViewHolder(inflater: LayoutInflater, parent: ViewGroup) : RecyclerView.ViewHolder(inflater.inflate(R.layout.ble_cells, parent, false)) {

        private var name: TextView? = null
        private var mac: TextView? = null
        private var status: TextView? = null

        init {
            name = itemView.findViewById(R.id.ble_name_lable)
            mac = itemView.findViewById(R.id.ble_mac_label)
            status = itemView.findViewById(R.id.ble_bond_label)
        }

        fun bind(item: BluetoothDevice) {
            if (item.name != null && item.name.length > 0) name?.text = item.name
            else name?.text = "N/A"
            mac?.text = item.address
        }
    }
}
