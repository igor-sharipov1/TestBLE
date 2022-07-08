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

    var mDeviceAdapter: DeviceAdapter? = null
    val mDeviceList = mutableListOf<BluetoothDevice>()
    var instance: Activity? = null
    var mBluetoothDevice : BluetoothDevice? = null
    var mBluetoothGatt : BluetoothGatt? = null
    var mBluetoothAdapter : BluetoothAdapter? = null
    //private val connectedDevice

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        instance = this
        //val btManager = baseContext.getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        // getCurrentBluetoothConnection()
        mDeviceAdapter = DeviceAdapter(mDeviceList, applicationContext, instance)
        val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        registerReceiver(receiver, filter)
        val filter2 = IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
        registerReceiver(bondedReceiver, filter2)
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        mBluetoothAdapter?.startDiscovery()
        val refreshButton = findViewById<Button>(R.id.refreshBluetoothSearch)
        refreshButton.setOnClickListener {
            mBluetoothAdapter?.cancelDiscovery()
            mDeviceList.clear()
            mBluetoothAdapter?.startDiscovery()

        }
    }


    private val receiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {
            val rv = findViewById<RecyclerView>(R.id.listOfDevices)
            val action: String? = intent.action
            when (action) {
                BluetoothDevice.ACTION_FOUND -> {
                    val device: BluetoothDevice? =
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    val deviceName = device?.name
                    val deviceHardwareAddress = device?.address // MAC address
                    Log.d(deviceName, "deviceName")
                    mDeviceList.add(device!!)
                    mDeviceAdapter = DeviceAdapter(mDeviceList, applicationContext, instance)
                    mDeviceAdapter?.notifyItemRangeChanged(0, mDeviceList.size - 1)
                    rv.adapter = mDeviceAdapter
                    val mLayoutManager = LinearLayoutManager(
                        applicationContext
                    )
                    rv.layoutManager = mLayoutManager
                }
            }
        }

    }

    private val bondedReceiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {
            val action: String? = intent.action
            when (action) {
                BluetoothDevice.ACTION_BOND_STATE_CHANGED -> {
                    val state:  Int? = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR)
                    when (state) {
                        BluetoothDevice.BOND_BONDING -> {
                            Log.d("ASD","ASD")
                        }
                        BluetoothDevice.BOND_BONDED -> {
                            Log.d("ASD2","ASD2")
                            setUpBondedDevice()
                        }
                        BluetoothDevice.BOND_NONE -> {
                            Log.d("ASD3","ASD3")
                        }
                    }
                }
            }
        }

    }

    fun getConnectData(device : BluetoothDevice?){
        mBluetoothDevice = device
    }

    private fun setUpBondedDevice(){
        findViewById<TextView>(R.id.deviceName).text = mBluetoothDevice?.name
        val flag = mBluetoothAdapter?.getProfileConnectionState(BluetoothProfile.A2DP) == BluetoothProfile.STATE_DISCONNECTED
        mBluetoothAdapter?.getProfileProxy(instance, cServiceListener(applicationContext), BluetoothProfile.GATT)
      //  mBluetoothGatt = mBluetoothDevice?.connectGatt(this, false, mBluetoothGattCallback)
    }

    private val mBluetoothGattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.d("signal","Connected!")
            }
            else if (newState == BluetoothProfile.STATE_CONNECTING){
                Log.d("signal","Connecting...")
            }
            else if (newState == BluetoothProfile.STATE_DISCONNECTED){
                Log.d("signal","Disconnected(")
            }
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?,
            status: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS){
                Log.d("characteristic", characteristic?.uuid.toString())
            }
        }

    }
}





