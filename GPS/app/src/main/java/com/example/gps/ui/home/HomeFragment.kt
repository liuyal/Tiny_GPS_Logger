package com.example.gps.ui.home

import android.content.Context
import android.os.AsyncTask
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
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {

    private lateinit var homeViewModel: HomeViewModel

    @ExperimentalUnsignedTypes
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        homeViewModel = ViewModelProviders.of(this).get(HomeViewModel::class.java)
        val view = inflater.inflate(R.layout.fragment_home, container, false)
        val textView: TextView = view.findViewById(R.id.text_home)
        homeViewModel.text.observe(viewLifecycleOwner, Observer { textView.text = it })

        view.test_button.setOnClickListener { View ->
            Log.d("", "Clicked Send button")

            val data: EditText? = view.findViewById(R.id.editText)
            val value = ByteArray(1)
            value[0] = data?.text.toString().toByte()

            GlobalScope.launch {
                val c = GlobalApplication.BLE?.writeValue(value)
                Log.d("", c.toString())
            }
        }

        view.test_button2.setOnClickListener { View ->
            Log.d("", "Clicked Read button")
            GlobalScope.launch {
                val returnVal = GlobalApplication.BLE?.readValue()
                println(returnVal!!.contentToString())
                println(returnVal.toString(Charsets.UTF_8))

            }

            val test = GlobalApplication.BLE?.loadDBMAC()
            Log.d("", test!!)

        }
        return view
    }



    // TODO: periodic functions for device status checking
    private class RunTask(c: Context) : AsyncTask<Void, Void, String>() {
        private val context: Context = c

        override fun onPreExecute() {
            super.onPreExecute()
        }

        override fun doInBackground(vararg p0: Void?): String? {
            return null
        }

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)
        }
    }
}
