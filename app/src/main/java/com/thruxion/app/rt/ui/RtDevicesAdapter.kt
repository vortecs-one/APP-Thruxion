package com.thruxion.app.rt.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.thruxion.app.R
import com.thruxion.app.rt.device.DeviceDto

class RtDevicesAdapter(
    private val onRemoveClick: (DeviceDto) -> Unit
) : RecyclerView.Adapter<RtDevicesAdapter.DeviceViewHolder>() {

    private var devices: List<DeviceDto> = emptyList()

    fun submitList(newDevices: List<DeviceDto>) {
        devices = newDevices
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_rt_device, parent, false)
        return DeviceViewHolder(view)
    }

    override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {
        holder.bind(devices[position])
    }

    override fun getItemCount(): Int = devices.size

    inner class DeviceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvName: TextView = itemView.findViewById(R.id.tvDeviceName)
        private val tvDetails: TextView = itemView.findViewById(R.id.tvDeviceDetails)
        private val btnRemove: MaterialButton = itemView.findViewById(R.id.btnRemoveDevice)

        fun bind(device: DeviceDto) {
            tvName.text = device.name
            tvDetails.text = "App v${device.appVersion} | Status: ${device.status}"
            btnRemove.setOnClickListener {
                onRemoveClick(device)
            }
        }
    }
}
