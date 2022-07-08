package com.example.myapplication

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.lang.reflect.Method

class DeviceAdapter(
    private var items: List<BluetoothDevice?>,
    private val context : Context,
    private val parentActivity : Activity?
) : RecyclerView.Adapter<DeviceAdapter.ViewHolder>() {

    val mac : TextView? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =  ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.device_item, parent, false))

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }



    inner class ViewHolder(itemView : View):RecyclerView.ViewHolder(itemView){
        fun bind(device: BluetoothDevice?) {
            itemView.findViewById<TextView>(R.id.deviceMacAddress).text = device?.address
            itemView.findViewById<TextView>(R.id.deviceNameCharacteristic).text = device?.name
            itemView.setOnClickListener {
                device?.createBond()
                if (parentActivity is MainActivity) {
                    parentActivity.getConnectData(device)
                }
              //  getCurrentBluetoothConnection()
            }

        }
        private fun getCurrentBluetoothConnection(){
            var mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
            val mServiceListener = cServiceListener(context)
            mBluetoothAdapter.getProfileProxy(context, mServiceListener, BluetoothProfile.GATT)
        }
    }
}
