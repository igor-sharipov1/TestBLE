package com.example.myapplication

import android.app.Activity
import android.bluetooth.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView


class MainActivity : AppCompatActivity() {

    var mDeviceAdapter : DeviceAdapter? = null
    val mDeviceList = mutableListOf<BluetoothDevice>()
    var instance : Activity? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        instance = this
        //val btManager = baseContext.getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
       // getCurrentBluetoothConnection()
        mDeviceAdapter = DeviceAdapter(mDeviceList,applicationContext, instance)
        val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        registerReceiver(receiver, filter)
        var mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        mBluetoothAdapter.startDiscovery()
        val refreshButton = findViewById<Button>(R.id.refreshBluetoothSearch)
        refreshButton.setOnClickListener {
            mBluetoothAdapter.cancelDiscovery()
            mDeviceList.clear()
            mBluetoothAdapter.startDiscovery()

        }
    }



    private val receiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {
            val rv = findViewById<RecyclerView>(R.id.listOfDevices)
            val action: String? = intent.action
            when(action) {
                BluetoothDevice.ACTION_FOUND -> {
                    // Discovery has found a device. Get the BluetoothDevice
                    // object and its info from the Intent.

                    val device: BluetoothDevice? =
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    val deviceName = device?.name
                    val deviceHardwareAddress = device?.address // MAC address
                    Log.d(deviceName, "deviceName")
                    mDeviceList.add(device!!)
                    mDeviceAdapter = DeviceAdapter(mDeviceList,applicationContext, instance)
                    mDeviceAdapter?.notifyItemRangeChanged(0,mDeviceList.size-1)
                    rv.adapter = mDeviceAdapter
                    val mLayoutManager = LinearLayoutManager(
                        applicationContext
                    )
                    rv.layoutManager = mLayoutManager
                }
            }
        }

    }

}





