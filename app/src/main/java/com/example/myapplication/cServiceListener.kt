package com.example.myapplication

import android.app.Activity
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothProfile
import android.bluetooth.BluetoothProfile.ServiceListener
import android.content.Context
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import java.util.*



class cServiceListener(private val context : Context) : ServiceListener {



    override fun onServiceConnected(profile: Int, bluetoothProfile: BluetoothProfile){
        val Devices = bluetoothProfile.getDevicesMatchingConnectionStates(states)
        for (loop in Devices) {
            Log.i("myTag", loop.name)
            //Toast.makeText(context, "Connected to ${loop.name}",Toast.LENGTH_SHORT).show()
           // (context as Activity).findViewById<TextView>(R.id.deviceName).text =  loop.name
            val gatt = loop.connectGatt(context, false, mBluetoothGattCallback)
            val log = gatt.getService(UUID.fromString("00002a29-0000-1000-8000-00805f9b34fb"))
            Log.i("myCharacteristic", log.toString())
            Toast.makeText(context, log.toString(), Toast.LENGTH_LONG).show()
        }
    }

    override fun onServiceDisconnected(profile: Int) {}

    companion object {
        val intArray = intArrayOf(BluetoothProfile.STATE_DISCONNECTING,
            BluetoothProfile.STATE_DISCONNECTED ,  BluetoothProfile.STATE_CONNECTED,
            BluetoothProfile.STATE_CONNECTING)

        private val states = intArrayOf(
            BluetoothProfile.STATE_CONNECTED,
            BluetoothProfile.STATE_CONNECTING
        )

    }



    private var mBluetoothGattCallback = object : BluetoothGattCallback() {
        override fun onCharacteristicRead(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?,
            status: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS){
                Log.d("characteristic", characteristic?.uuid.toString())
                Toast.makeText(context, characteristic?.uuid.toString(), Toast.LENGTH_LONG).show()
            }
        }
    }
}