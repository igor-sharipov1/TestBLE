package com.example.myapplication

import android.Manifest
import android.app.Activity
import android.bluetooth.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.yandex.metrica.YandexMetrica
import com.yandex.metrica.YandexMetricaConfig
import java.util.*


class MainActivity : AppCompatActivity() {

    var mDeviceAdapter: DeviceAdapter? = null
    val mDeviceList = mutableListOf<BluetoothDevice>()
    var instance: Activity? = null
    var mBluetoothDevice : BluetoothDevice? = null
    var mBluetoothGatt : BluetoothGatt? = null
    var mBluetoothGattService : BluetoothGattService? = null
    var mBluetoothGattCharacteristic : BluetoothGattCharacteristic? = null
    var mBluetoothAdapter : BluetoothAdapter? = null
    var characteristic : BluetoothGattCharacteristic? = null
    val API_key = "b8a153d2-467f-4336-991f-86547c468064"

    private var commandQueue: Queue<Runnable>? = object : LinkedList<Runnable>(){}
    private var commandQueueBusy = false
    private var nrTries = 0
    private var MAX_TRIES = 10
    private var isRetrying = false
    val bleHandler = Handler()

    private val GLUCOSE_SERVICE = UUID.fromString("00001808-0000-1000-8000-00805f9b34fb")
    private val GLUCOSE_CHARACTERISTIC = UUID.fromString("00002a18-0000-1000-8000-00805f9b34fb")
    private val CLIENT_CHARACTERISTIC_CONFIG = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
    private val RECORDS_CHARACTERISTIC = UUID.fromString("00002a52-0000-1000-8000-00805f9b34fb")


    private val DIS_SERVICE_UUID = UUID.fromString("0000180A-0000-1000-8000-00805f9b34fb")
    private val MANUFACTURER_NAME_CHARACTERISTIC_UUID = UUID.fromString("00002A29-0000-1000-8000-00805f9b34fb")
    private val MODEL_NUMBER_CHARACTERISTIC_UUID = UUID.fromString("00002A24-0000-1000-8000-00805f9b34fb")



    private fun setCharact(characteristic: BluetoothGattCharacteristic?){
        this.characteristic = characteristic
        val flag = mBluetoothGatt?.readCharacteristic(this.characteristic)
        val g = 7
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val config = YandexMetricaConfig.newConfigBuilder(API_key).build()
        YandexMetrica.activate(applicationContext, config)
        YandexMetrica.enableActivityAutoTracking(application);
        setContentView(R.layout.activity_main)
        instance = this
        //val btManager = baseContext.getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        // getCurrentBluetoothConnection()
        checkPermission()
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
       // val flag = mBluetoothAdapter?.getProfileConnectionState(BluetoothProfile.A2DP) == BluetoothProfile.STATE_DISCONNECTED
       // mBluetoothAdapter?.getProfileProxy(instance, cServiceListener(applicationContext), BluetoothProfile.GATT)
        mBluetoothGatt = mBluetoothDevice?.connectGatt(this, false, mBluetoothGattCallback)

    }


    private val mBluetoothGattCallback = object : BluetoothGattCallback() {

        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    val q : Boolean = gatt.discoverServices()
                    Log.d("signal","Connected!")
                }
                BluetoothProfile.STATE_CONNECTING -> {
                    Log.d("signal","Connecting...")
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    Log.d("signal","Disconnected(")
                    runOnUiThread {
                        Toast.makeText(applicationContext, "Disconnected from device ${gatt.device.name}", Toast.LENGTH_LONG).show()
                    }
                    gatt.close()
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.w("BLE", "onServicesDiscovered ")
                val services = gatt.services
                for (service in services!!){
                    for (characteristic in service.characteristics)
                        for (descriptor in characteristic.descriptors) {
                            mBluetoothGatt!!.setCharacteristicNotification(characteristic, true)

                            descriptor.value = BluetoothGattDescriptor.ENABLE_INDICATION_VALUE
                            mBluetoothGatt!!.writeDescriptor(descriptor)
                        }
                }
                //characteristic = gatt.getService(GLUCOSE_SERVICE).getCharacteristic(RECORDS_CHARACTERISTIC)
                //setNotifySensor(gatt)
            }
        }

        private fun setNotifySensor(gatt: BluetoothGatt) {
            if (mBluetoothAdapter == null || mBluetoothGatt == null) {
                return
            }
            mBluetoothGatt!!.setCharacteristicNotification(characteristic, true)
            try {
                Thread.sleep(200)
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
            val descriptor = characteristic!!.getDescriptor(
                UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
            )
            try {
                Thread.sleep(200)
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
            descriptor.value = BluetoothGattDescriptor.ENABLE_INDICATION_VALUE
            try {
                Thread.sleep(200)
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
            mBluetoothGatt!!.writeDescriptor(descriptor)
            try {
                Thread.sleep(200)
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }

        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            if (CLIENT_CHARACTERISTIC_CONFIG.equals(characteristic.uuid)) {
                Log.w(
                    "BLE",
                    "CharacteristicRead - xaccel service uuid: " + characteristic.service.uuid
                )
                Log.w(
                    "BLE",
                    "CharacteristicRead - xaccel value: " + characteristic.getIntValue(
                        BluetoothGattCharacteristic.FORMAT_UINT8,
                        0
                    )
                )
            }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic
        ) {
            Log.i("BLE", "Received characteristics changed event : " + characteristic.uuid)
            if (CLIENT_CHARACTERISTIC_CONFIG.equals(characteristic.uuid)) {
                Log.i("BLE", "Received new value for xAccel.")
            }
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?,
            status: Int
        ) {
        }

        override fun onDescriptorRead(
            gatt: BluetoothGatt?,
            descriptor: BluetoothGattDescriptor?,
            status: Int
        ) {
        }

        override fun onDescriptorWrite(
            gatt: BluetoothGatt?,
            descriptor: BluetoothGattDescriptor?,
            status: Int
        ) {
            val q = 7
        }
    }




    private fun checkPermission(){
        when (PackageManager.PERMISSION_DENIED) {
            ContextCompat.checkSelfPermission
                (this, Manifest.permission.BLUETOOTH_SCAN) -> {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.BLUETOOTH_SCAN), 101)
            }
            ContextCompat.checkSelfPermission
                (this, Manifest.permission.BLUETOOTH_CONNECT) -> {
                // You can directly ask for the permission.
                // The registered ActivityResultCallback gets the result of this request.
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.BLUETOOTH_CONNECT), 101)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mBluetoothGatt?.close()
    }
}





