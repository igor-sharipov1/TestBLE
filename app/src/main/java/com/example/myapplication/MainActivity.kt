package com.example.myapplication

import android.Manifest
import android.app.Activity
import android.bluetooth.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.nfc.NfcAdapter.EXTRA_DATA
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
    private val GLUCOSE_FEATURE = UUID.fromString("00002a51-0000-1000-8000-00805f9b34fb")
    private val CONTEXT_CHARACTERISTIC = UUID.fromString("00002a34-0000-1000-8000-00805f9b34fb")

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
                   // gatt.requestMtu(256)
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
                for (service in services){
                    val serviceUuid = service.uuid
                    if (serviceUuid.equals(GLUCOSE_SERVICE)) {
                        for (characteristic in service.characteristics) {
                            if (characteristic.uuid == GLUCOSE_CHARACTERISTIC) {
                                val charGM =
                                    gatt.getService(UUID.fromString("00001808-0000-1000-8000-00805f9b34fb"))
                                        .getCharacteristic(GLUCOSE_CHARACTERISTIC)
                                gatt.setCharacteristicNotification(charGM, true)

                                val descGM =
                                    charGM.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG)
                                descGM.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                                val flag = gatt.writeDescriptor(descGM)
                                Log.d("Glucose Character desc", flag.toString())

                            }
                        }
                    }
                }

               // val flag = gatt.writeCharacteristic(characteristic)


            }
        }


        override fun onCharacteristicRead(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {

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

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic
        ) {
            Log.i(characteristic.uuid.toString(), "Received characteristics changed event : " )
            if (characteristic.uuid.equals(GLUCOSE_CHARACTERISTIC)){
                Log.d("need Characteristic!", "YAY")
                val data = characteristic.value
                if (data != null && data.size > 0) {
                    val stringBuilder = StringBuilder(data.size)
                    for (byteChar in data) stringBuilder.append(String.format("%02X ", byteChar))
                    Log.d(stringBuilder.toString(),"asdasd")
                }
                if (data != null && data.size > 0) {
                    val readings = GlucoseReadings(data)
                    Log.i("result is ", readings.toString())
                }
            }
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?,
            status: Int
        ) {
            Log.i("BLE", "CHARACTERISTIC WRITE event : " + characteristic?.uuid + " " + status)
            Log.d("GATT_STATUS - ", (status == BluetoothGatt.GATT_SUCCESS).toString())
            val data = characteristic!!.value
            if (data != null && data.size > 0) {
                val stringBuilder = StringBuilder(data.size)
                for (byteChar in data) stringBuilder.append(String.format("%02X ", byteChar))
            }
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
            val parentCharacteristic = descriptor?.characteristic
            if(status!= BluetoothGatt.GATT_SUCCESS) {
                Log.d("DesciprotWrite","Failed");
            }
            else {

                if (parentCharacteristic?.uuid == GLUCOSE_CHARACTERISTIC) {
                    val charContextGM =
                        gatt?.getService(UUID.fromString("00001808-0000-1000-8000-00805f9b34fb"))
                            ?.getCharacteristic(CONTEXT_CHARACTERISTIC)
                    gatt?.setCharacteristicNotification(charContextGM, true)

                    val descContextGM =
                        charContextGM?.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG)
                    descContextGM?.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                    val flag = gatt?.writeDescriptor(descContextGM) // false
                    Log.d("Glucose Context desc", flag.toString())
                } else if (parentCharacteristic?.uuid == CONTEXT_CHARACTERISTIC) {
                    val charRACP =
                        gatt?.getService(UUID.fromString("00001808-0000-1000-8000-00805f9b34fb"))
                            ?.getCharacteristic(RECORDS_CHARACTERISTIC)
                    gatt?.setCharacteristicNotification(charRACP, true)
                    val descRACP =
                        charRACP?.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG)
                    descRACP?.value = BluetoothGattDescriptor.ENABLE_INDICATION_VALUE
                    val flag = gatt?.writeDescriptor(descRACP)
                    Log.d("Records Context desc", flag.toString())
                } else if (parentCharacteristic?.uuid == RECORDS_CHARACTERISTIC) {
                    val value = descriptor?.value?.get(0)

                    val writeRACPchar =
                        gatt!!.getService(UUID.fromString("00001808-0000-1000-8000-00805f9b34fb"))
                            .getCharacteristic(UUID.fromString("00002a52-0000-1000-8000-00805f9b34fb"))
                    val data = ByteArray(2)
                    data[0] = 0x01 // Report Stored records

                    data[1] = 0x01 // Last record

                    // writeRACPchar.writeType = BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE

                    writeRACPchar.value = data
                    val flag = gatt.writeCharacteristic(writeRACPchar)
                }
            }
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
