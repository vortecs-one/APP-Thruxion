package com.thruxion.app.rt.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton
import com.thruxion.app.R
import com.thruxion.app.rt.device.RtDeviceManager
import kotlinx.coroutines.launch

class MyDevicesDialogFragment : BottomSheetDialogFragment() {

    private lateinit var deviceManager: RtDeviceManager
    private lateinit var adapter: RtDevicesAdapter
    var onDeviceRemovedListener: (() -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.dialog_rt_my_devices, container, false)
        deviceManager = RtDeviceManager(requireContext())

        val rvDevices = view.findViewById<RecyclerView>(R.id.rvDevicesDialog)
        val btnClose = view.findViewById<MaterialButton>(R.id.btnCloseDialog)

        adapter = RtDevicesAdapter { device ->
            lifecycleScope.launch {
                val success = deviceManager.deleteDevice(device.id)
                if (success) {
                    Toast.makeText(context, "Device removed", Toast.LENGTH_SHORT).show()
                    loadDevices()
                    onDeviceRemovedListener?.invoke()
                } else {
                    Toast.makeText(context, "Failed to remove device", Toast.LENGTH_SHORT).show()
                }
            }
        }

        rvDevices.layoutManager = LinearLayoutManager(context)
        rvDevices.adapter = adapter

        btnClose.setOnClickListener {
            dismiss()
        }

        loadDevices()
        return view
    }

    private fun loadDevices() {
        lifecycleScope.launch {
            val activeDevices = deviceManager.getActiveDevices()
            adapter.submitList(activeDevices)
        }
    }

    companion object {
        fun newInstance(): MyDevicesDialogFragment {
            return MyDevicesDialogFragment()
        }
    }
}
