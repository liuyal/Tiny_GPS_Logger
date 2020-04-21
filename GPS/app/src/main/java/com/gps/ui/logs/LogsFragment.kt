package com.gps.ui.logs

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.gps.R

class LogsFragment : Fragment() {

    private lateinit var logsViewModel: LogsViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        logsViewModel = ViewModelProvider(this).get(LogsViewModel::class.java)
        val root = inflater.inflate(R.layout.fragment_logs, container, false)
        val textView: TextView = root.findViewById(R.id.text_logs)
        logsViewModel.text.observe(viewLifecycleOwner, Observer {
            textView.text = it
        })
        return root
    }

    override fun onStart() {
        super.onStart()
        Log.e("", "Start L Fragment")
    }

    override fun onStop() {
        super.onStop()
        Log.e("", "Stopped L Fragment")
    }

}
