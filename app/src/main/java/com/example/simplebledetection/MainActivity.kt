package com.example.simplebledetection

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.simplebledetection.databinding.ActivityMainBinding
import kotlin.system.exitProcess

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val TAG = "MainActivity"

    private lateinit var scanService: ScanService
    private lateinit var adapter: DeviceListAdapter
    private lateinit var deviceList: ArrayList<Any>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.scanBtn.setOnClickListener { startScan() }
        binding.exitBtn.setOnClickListener { exitApp() }
        val recycleView: RecyclerView = findViewById(R.id.deviceList)
        deviceList = ArrayList()
        this.adapter = DeviceListAdapter(this.deviceList)
        recycleView.adapter = this.adapter

        // check for permission to scan BLE
        if (isPermissionGranted(this)) {
            Log.d(TAG, "@onCreate init scan service")
            scanService = ScanService(this, this.deviceList, this.adapter)
        }
    }

    private fun exitApp() {
        // exit application
        if (scanService.isScanning()) {
            binding.scanBtn.text = resources.getString(R.string.label_scan)
            scanService.stopBLEScan()
        }
        this@MainActivity.finish()
        exitProcess(0)
    }

    private fun startScan() {
        // check Bluetooth
        if (!scanService.isBluetoothEnabled()) {
            Log.d(TAG, "@startScan Bluetooth is disabled")
            val intent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            requestBluetooth.launch(intent)
        } else {

            scanService.initScanner()
            // start scanning BLE device
            if (scanService.isScanning()) {
                binding.scanBtn.text = resources.getString(R.string.label_scan)
                scanService.stopBLEScan()
            } else {
                scanService.startBLEScan()
                binding.scanBtn.text = resources.getString(R.string.label_scanning)
            }
        }
    }

    fun onRadioAllClicked(view: View) {

    }

    fun onRadioiBeaconClicked(view: View) {

    }

    // necessary permissions on Android <12
    private val BLE_PERMISSIONS = arrayOf(
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_FINE_LOCATION
    )

    // necessary permissions on Android >=12
    private val ANDROID_12_BLE_PERMISSIONS = arrayOf(
        Manifest.permission.BLUETOOTH_SCAN,
        Manifest.permission.BLUETOOTH_CONNECT,
        Manifest.permission.ACCESS_FINE_LOCATION
    )

    /**
     * Determine whether the location permission has been granted
     * if not, request the permission
     *
     * @param context
     * @return true if user has granted permission
     */
    private fun isPermissionGranted(context: Context): Boolean {
        Log.d(TAG, "@isPermissionGranted: checking bluetooth")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if ((ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED) ||
                (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.BLUETOOTH_SCAN
                ) != PackageManager.PERMISSION_GRANTED)
            ) {
                Log.d(TAG, "@isPermissionGranted: requesting Bluetooth on Android >= 12")
                ActivityCompat.requestPermissions(this, ANDROID_12_BLE_PERMISSIONS, 2)
                return false
            }
        } else {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                Log.d(TAG, "@isPermissionGranted: requesting Location on Android < 12")
                ActivityCompat.requestPermissions(this, BLE_PERMISSIONS, 3)
                return false
            }
        }
        Log.d(TAG, "@isPermissionGranted Bluetooth permission is ON")
        return true
    }

    private var requestBluetooth =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                Log.d(TAG, "@requestBluetooth Bluetooth is enabled")
            } else {
                Log.d(TAG, "@requestBluetooth Bluetooth usage is denied")
            }
        }
}