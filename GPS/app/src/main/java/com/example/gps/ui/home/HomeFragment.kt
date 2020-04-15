package com.example.gps.ui.home

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.example.gps.R
import com.example.gps.objects.GlobalApplication
import kotlinx.android.synthetic.main.fragment_home.view.*

class HomeFragment : Fragment() {

    private lateinit var homeViewModel: HomeViewModel

    @ExperimentalUnsignedTypes
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        homeViewModel = ViewModelProviders.of(this).get(HomeViewModel::class.java)
        val view = inflater.inflate(R.layout.fragment_home, container, false)
        val textView: TextView = view.findViewById(R.id.text_home)
        homeViewModel.text.observe(viewLifecycleOwner, Observer { textView.text = it })

        view.test_button.setOnClickListener { View ->
            Log.d("", "Clicked button")
            val data: EditText? = view.findViewById(R.id.editText)
            val value = ByteArray(1)
            value[0] = data?.text.toString().toByte()
            GlobalApplication.BLE?.writeValue(value)
        }

        view.test_button2.setOnClickListener { View ->
            Log.d("", "Clicked button 2")
            val returnVal = GlobalApplication.BLE?.readValue()

            if (returnVal != null) {
                for (it in returnVal){
                    Log.d("", it.toString())
                }
            }

        }
        return view
    }

    @ExperimentalUnsignedTypes
    fun ByteArray.toHexString() = asUByteArray().joinToString("") { it.toString(16).padStart(2, '0') }
}
