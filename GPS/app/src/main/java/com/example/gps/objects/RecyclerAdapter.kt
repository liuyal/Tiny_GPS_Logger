package com.example.gps.objects

import android.app.Application
import android.app.Activity
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.example.gps.R
import kotlinx.android.synthetic.main.ble_cells.view.*

class RecyclerAdapter(val items : ArrayList<BluetoothDevice>) : RecyclerView.Adapter<ViewHolder>() {

    override fun getItemCount(): Int {
        return items.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val cellForRow = layoutInflater.inflate(R.layout.ble_cells, parent, false)
        return CutsomViewHolder(cellForRow)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

    }

    override fun getItemViewType(position: Int): Int {
        return position
    }

    class CutsomViewHolder(val view : View) : RecyclerView.ViewHolder(view){

        fun bind(part : ArrayList<BluetoothDevice>, clickListener : (ArrayList<BluetoothDevice>) -> Unit) {
            view.ble_name_lable.text = "N/A"
//            view.ble_name_lable.text = p
            view.ble_bond_label.text = "NOT BONDED"
        }
    }


}
