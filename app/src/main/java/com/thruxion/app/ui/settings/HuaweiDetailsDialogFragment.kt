package com.thruxion.app.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.thruxion.app.R
import com.thruxion.app.utils.HuaweiHealthProvider
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale

class HuaweiDetailsDialogFragment : DialogFragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, android.R.style.Theme_Translucent_NoTitleBar)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_huawei_details, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tvSteps = view.findViewById<TextView>(R.id.tvHuaweiSteps)
        val tvDistance = view.findViewById<TextView>(R.id.tvHuaweiDistance)
        val tvCalories = view.findViewById<TextView>(R.id.tvHuaweiCalories)
        val tvHeartRate = view.findViewById<TextView>(R.id.tvHuaweiHeartRate)
        val tvWeight = view.findViewById<TextView>(R.id.tvHuaweiWeight)
        val tvSleep = view.findViewById<TextView>(R.id.tvHuaweiSleep)
        val tvStress = view.findViewById<TextView>(R.id.tvHuaweiStress)
        val tvSpO2 = view.findViewById<TextView>(R.id.tvHuaweiSpO2)
        val tvBP = view.findViewById<TextView>(R.id.tvHuaweiBP)
        val tvTemp = view.findViewById<TextView>(R.id.tvHuaweiTemp)
        val btnClose = view.findViewById<MaterialButton>(R.id.btnCloseHuawei)
        val rootContainer = view.findViewById<View>(R.id.root_container_huawei)

        btnClose.setOnClickListener { dismiss() }
        rootContainer.setOnClickListener { dismiss() }

        fetchHuaweiData(tvSteps, tvDistance, tvCalories, tvHeartRate, tvWeight, tvSleep, tvStress, tvSpO2, tvBP, tvTemp)
    }

    private fun fetchHuaweiData(
        tvSteps: TextView,
        tvDistance: TextView,
        tvCalories: TextView,
        tvHeartRate: TextView,
        tvWeight: TextView,
        tvSleep: TextView,
        tvStress: TextView,
        tvSpO2: TextView,
        tvBP: TextView,
        tvTemp: TextView
    ) {
        val provider = HuaweiHealthProvider()
        val context = requireContext()
        val startOfDay = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant()
        val now = Instant.now()

        lifecycleScope.launch {
            try {
                val steps = provider.getSteps(context, startOfDay, now)
                val distance = provider.getDistance(context, startOfDay, now)
                val calories = provider.getCalories(context, startOfDay, now)
                val heartRate = provider.getLatestHeartRate(context)
                val weight = provider.getLatestWeight(context)
                val sleepMinutes = provider.getDailySleep(context)
                val stress = provider.getLatestStress(context)
                val spo2 = provider.getLatestSpO2(context)
                val bp = provider.getLatestBloodPressure(context)
                val temp = provider.getLatestSkinTemperature(context)

                tvSteps.text = String.format(Locale.getDefault(), "%,d", steps)
                tvDistance.text = String.format(Locale.getDefault(), "%.1f", distance)
                tvCalories.text = String.format(Locale.getDefault(), "%.0f", calories)
                tvHeartRate.text = heartRate?.toString() ?: "--"
                
                tvWeight.text = if (weight != null) String.format(Locale.getDefault(), "%.1f", weight) else "--"
                tvSleep.text = if (sleepMinutes > 0) "${sleepMinutes / 60}h ${sleepMinutes % 60}m" else "--"
                tvStress.text = stress?.toString() ?: "--"
                tvSpO2.text = if (spo2 != null) "$spo2%" else "--"
                tvBP.text = if (bp != null) "${bp.first}/${bp.second}" else "--"
                tvTemp.text = if (temp != null) String.format(Locale.getDefault(), "%.1f", temp) else "--"

            } catch (e: Exception) {
                // Log or show error
            }
        }
    }
}
