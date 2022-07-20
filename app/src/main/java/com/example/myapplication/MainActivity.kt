package com.example.myapplication

import android.Manifest
import android.app.Activity
import android.bluetooth.*
import android.bluetooth.BluetoothGattCharacteristic.PROPERTY_NOTIFY
import android.content.BroadcastReceiver
import android.content.ContentValues.TAG
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
import kotlin.collections.ArrayList
import kotlin.experimental.and


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

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            super.onServicesDiscovered(gatt, status)
            val services = gatt?.services
            for (service in services!!) {
                for (characteristic in service.characteristics) {
                    Log.d("characteristic - ", characteristic.uuid.toString())
                    if (characteristic.value == null) {
                        Log.d("value - ", "null")
                    } else {
                        Log.d("value - ", characteristic.value.toString())
                    }
                    if (!characteristic.uuid.toString().equals("00002a00-0000-1000-8000-00805f9b34fb") &&
                        !characteristic.uuid.toString().equals("00002a01-0000-1000-8000-00805f9b34fb")&&
                        !characteristic.uuid.toString().equals("00002a04-0000-1000-8000-00805f9b34fb")&&
                        !characteristic.uuid.toString().equals("00002a23-0000-1000-8000-00805f9b34fb")&&
                        !characteristic.uuid.toString().equals("00002a24-0000-1000-8000-00805f9b34fb")&&
                        !characteristic.uuid.toString().equals("00002a25-0000-1000-8000-00805f9b34fb")&&
                        !characteristic.uuid.toString().equals("00002a26-0000-1000-8000-00805f9b34fb")&&
                        !characteristic.uuid.toString().equals("00002a28-0000-1000-8000-00805f9b34fb")&&
                        !characteristic.uuid.toString().equals("00002a29-0000-1000-8000-00805f9b34fb")&&
                        !characteristic.uuid.toString().equals("00002a2a-0000-1000-8000-00805f9b34fb")&&
                        !characteristic.uuid.toString().equals("00002a50-0000-1000-8000-00805f9b34fb")&&
                        !characteristic.uuid.toString().equals("00002a2b-0000-1000-8000-00805f9b34fb")&&
                        !characteristic.uuid.toString().equals("00001021-0002-11e2-9e96-0800200c9a66"))
                  //  Log.d("readable - ", gatt.readCharacteristic(characteristic).toString())
                    if (characteristic.uuid.toString().equals("00002a52-0000-1000-8000-00805f9b34fb")){

                        val descriptor = characteristic?.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG)
                        descriptor?.value = BluetoothGattDescriptor.ENABLE_INDICATION_VALUE
                        gatt?.writeDescriptor(descriptor);
                        gatt.setCharacteristicNotification(characteristic, false)
                    }
                    Log.d("writable - ", gatt.writeCharacteristic(characteristic).toString())
                    Log.d("--------------- ", "aboba")
                }
            }
            //setNotifySensor(gatt!!)
       //     val flag1 = gatt?.writeCharacteristic(service?.getCharacteristic(GLUCOSE_CHARACTERISTIC))
         //  val flag2 = gatt?.setCharacteristicNotification(service?.getCharacteristic(GLUCOSE_CHARACTERISTIC), true)
        //    val flag3 = gatt?.readCharacteristic(service?.getCharacteristic(GLUCOSE_CHARACTERISTIC))



            //readCharacteristic(service?.getCharacteristic(GLUCOSE_CHARACTERISTIC))
           // val descriptor = b?.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG)
           // descriptor?.value = BluetoothGattDescriptor.ENABLE_INDICATION_VALUE
           // gatt?.writeDescriptor(descriptor);
           // val c = b?.uuid.toString()
            //val c1 = b?.getDescriptor()
           // val c2 = b?.instanceId
           // val c3 = b?.permissions
           // val c4 = b?.properties
         //   val c5 = b?.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 2)

          //  val b2 = gatt?.readCharacteristic(b)
           // readChar(b,gatt)

            return
            //findViewById<TextView>(R.id.glucometerCharacteristic).text = b.toString()
        }


        private fun setNotifySensor(gatt: BluetoothGatt) {
            val characteristic = gatt.getService(GLUCOSE_SERVICE)
                .getCharacteristic(GLUCOSE_CHARACTERISTIC)
            gatt.setCharacteristicNotification(characteristic, false)
            val desc = characteristic.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG)
            Log.i("BLE", "Descriptor is $desc") // this is not null
            desc.value = BluetoothGattDescriptor.ENABLE_INDICATION_VALUE
            Log.i("BLE", "Descriptor write: " + gatt.writeDescriptor(desc)) // returns true
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?,
            status: Int
        ) {
            Log.d("onCharacteristicRead ", characteristic?.uuid.toString())
            val b = characteristic!!.value
            val f = ArrayList<Short>()
            val i = byteToHexString(b)
            runOnUiThread {
                Toast.makeText(applicationContext, "onCharacteristicRead is called", Toast.LENGTH_LONG).show()
            }
            if (status == BluetoothGatt.GATT_SUCCESS){
                Log.d("characteristic", characteristic?.uuid.toString())
                runOnUiThread {
                    Toast.makeText(applicationContext, "GATT_SUCCESS", Toast.LENGTH_LONG).show()
                    //Toast.makeText(applicationContext, "Some characteristics ${characteristic?.getDescriptor(characteristic?.uuid)?.characteristic?.getStringValue(0)}", Toast.LENGTH_LONG).show()
                }
            }
            else{
                Log.d("characteristic", characteristic?.uuid.toString())
                runOnUiThread {
                    Toast.makeText(applicationContext, "Failed to read characteristics", Toast.LENGTH_LONG).show()
                }
            }
        }

        fun byteToHexString(data: ByteArray?): String? {
            val buf = StringBuffer()
            if (data != null) {
                for (i in data.indices) {
                    var halfbyte: Int = data[i].toInt() ushr 4 and 0x0F
                    var two_halfs = 0
                    do {
                        if (0 <= halfbyte && halfbyte <= 9) {
                            buf.append(('0'.toInt() + halfbyte).toChar())
                        } else {
                            buf.append(('a'.toInt() + (halfbyte - 10)).toChar())
                        }
                        halfbyte = (data[i] and 0x0F).toInt()
                    } while (two_halfs++ < 1)
                    buf.append(' ')
                }
            }
            return buf.toString()
        }



        override fun onCharacteristicChanged(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic
        ) {
            val dataInput = characteristic.value
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?,
            status: Int
        ) {
            runOnUiThread {
                Toast.makeText(applicationContext, "onCharacteristicWrite is called", Toast.LENGTH_LONG).show()
            }
        }

    }

    fun readCharacteristic(characteristic: BluetoothGattCharacteristic?): Boolean {
        if (mBluetoothGatt == null) {
            Log.e(TAG, "ERROR: Gatt is 'null', ignoring read request")
            return false
        }

        // Check if characteristic is valid
        if (characteristic == null) {
            Log.e(TAG, "ERROR: Characteristic is 'null', ignoring read request")
            return false
        }

        // Check if this characteristic actually has READ property
        if (characteristic.properties and PROPERTY_NOTIFY == 0) {
            Log.e(TAG, "ERROR: Characteristic cannot be read")
            return false
        }

        // Enqueue the read command now that all checks have been passed
        val result = commandQueue?.add(Runnable {
            mBluetoothGatt?.setCharacteristicNotification(characteristic, true);
            val descriptor = characteristic?.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG)
            descriptor?.value = BluetoothGattDescriptor.ENABLE_INDICATION_VALUE;
            mBluetoothGatt?.writeDescriptor(descriptor);

        })
        if (result != false) {
            nextCommand()
        } else {
            Log.e(TAG, "ERROR: Could not enqueue read characteristic command")
        }
        return result!!
    }

    private fun nextCommand() {
        // If there is still a command being executed then bail out
        if (commandQueueBusy) {
            return
        }

        // Check if we still have a valid gatt object
        if (mBluetoothGatt == null) {
            commandQueue!!.clear()
            commandQueueBusy = false
            return
        }

        // Execute the next command in the queue
        if (commandQueue!!.size > 0) {
            val bluetoothCommand = commandQueue?.peek()
            commandQueueBusy = true
            nrTries = 0
            bleHandler.post(Runnable {
                try {
                    bluetoothCommand?.run()
                } catch (ex: Exception) {

                }
            })
        }
    }

    private fun completedCommand() {
        commandQueueBusy = false
        isRetrying = false
        commandQueue!!.poll()
        nextCommand()
    }

    private fun retryCommand() {
        commandQueueBusy = false
        val currentCommand = commandQueue!!.peek()
        if (currentCommand != null) {
            if (nrTries >= MAX_TRIES) {
                // Max retries reached, give up on this one and proceed
                Log.v(TAG, "Max number of tries reached")
                commandQueue?.poll()
            } else {
                isRetrying = true
            }
        }
        nextCommand()
    }

    private fun readChar(characteristic : BluetoothGattCharacteristic?, gatt : BluetoothGatt?){
        val flag = mBluetoothGatt?.readCharacteristic(characteristic)
        val q = 5
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





