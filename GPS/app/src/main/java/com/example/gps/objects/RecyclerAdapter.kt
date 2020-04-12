package com.example.gps.objects

import android.bluetooth.BluetoothDevice
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.example.gps.R
import kotlinx.android.synthetic.main.ble_cells.view.*

class RecyclerAdapter(private val items : ArrayList<BluetoothDevice>, private val listener: (BluetoothDevice) -> Unit) : RecyclerView.Adapter<ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CustomViewHolder {
        return CustomViewHolder(LayoutInflater.from(parent.context), parent)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        (holder as CustomViewHolder).bind(items[position], listener)
    }

    override fun getItemCount(): Int = items.size

    override fun getItemViewType(position: Int): Int = position

    class CustomViewHolder(inflater: LayoutInflater, parent: ViewGroup) : RecyclerView.ViewHolder(inflater.inflate(R.layout.ble_cells, parent, false)) {

        fun bind(item: BluetoothDevice, clickListener: (BluetoothDevice) -> Unit) {

            val name: TextView? = itemView.findViewById(R.id.ble_name_label)
            val mac: TextView? = itemView.findViewById(R.id.ble_mac_label)
            val status: TextView? = itemView.findViewById(R.id.ble_bond_label)

            itemView.connect_btn.setOnClickListener { clickListener(item) }

            if (item.name != null && item.name.isNotEmpty()) name?.text = item.name
            else name?.text = "N/A"
            mac?.text = item.address
        }
    }
}
