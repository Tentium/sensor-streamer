package com.tentium.sensorstream

import android.Manifest
import android.content.*
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.BatteryManager
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.*
import com.github.kittinunf.fuel.Fuel
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import org.jetbrains.anko.doAsync
import java.lang.Exception
import java.util.*
import kotlin.concurrent.scheduleAtFixedRate

class Stream : AppCompatActivity() {
    private var isStreaming = false
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var timer: Timer = Timer()
    var prefs: Prefs? = null

    class Prefs (context: Context) {
        val PREFS_FILENAME = "com.tentium.sensorstream.prefs"
        val sharedPrefs: SharedPreferences = context.getSharedPreferences(PREFS_FILENAME, 0);

        val INTERVAL = "interval"
        val HOST = "host"
        val GPS = "gps"
        val BATTERY = "battery"
        val CLIENTID = "deviceId"

        var interval: String
            get() = sharedPrefs.getString(INTERVAL, "")
            set(value) = sharedPrefs.edit().putString(INTERVAL, value).apply()
        var host: String
            get() = sharedPrefs.getString(HOST, "")
            set(value: String) = sharedPrefs.edit().putString(HOST, value).apply()
        var gps: Boolean
            get() = sharedPrefs.getBoolean(GPS, false)
            set(value) = sharedPrefs.edit().putBoolean(GPS, value).apply()
        var battery: Boolean
            get() = sharedPrefs.getBoolean(BATTERY, false)
            set(value) = sharedPrefs.edit().putBoolean(BATTERY, value).apply()
        var deviceId: String?
            get() = sharedPrefs.getString(CLIENTID, null)
            set(value) = sharedPrefs.edit().putString(CLIENTID, value).apply()
    }

    protected fun getSaltString(): String {
        val SALTCHARS = "abcdefghijklmnopqrstuvxyz1234567890"
        val salt = StringBuilder()
        val rnd = Random()
        while (salt.length < 8) { // length of the random string.
            val index = (rnd.nextFloat() * SALTCHARS.length).toInt()
            salt.append(SALTCHARS[index])
        }
        val saltStr = salt.toString()
        return saltStr
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stream)

        // INITIALIZE SHARED-PREFERENCES
        prefs = Prefs(this)

        // INITIALIZE LOCATION PROVIDER
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // SET TENTIUM LOGO AS WEB LINK
        val img = findViewById(R.id.tentium) as ImageView
        img.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("http://tentium.se/"))
            startActivity(intent)
        }

        // BUTTON CLICK LISTENER
        val btn = findViewById(R.id.toggleStream) as Button
        btn.setOnClickListener {
            if (isStreaming) stopStreaming() else startStreaming()
        }

        // SET DEVICE ID:
        var deviceId = prefs!!.deviceId
        if (deviceId == null) {
            deviceId = getSaltString()
            prefs!!.deviceId = deviceId
        }
        findViewById<TextView>(R.id.deviceId).text = "Device ID: $deviceId"

        // SET SWITCH STATES & LISTENERS
        val gps = findViewById(R.id.sendGps) as Switch
        gps.isChecked = prefs!!.gps
        val battery = findViewById(R.id.sendBattery) as Switch
        battery.isChecked = prefs!!.battery
        gps.setOnCheckedChangeListener { _, isChecked ->
            prefs!!.gps = isChecked
        }
        battery.setOnCheckedChangeListener { _, isChecked ->
            prefs!!.battery = isChecked
        }

        // SET INTERVAL & HOST TEXT INPUTS and LISTENERS
        val intervalEditText = findViewById(R.id.interval) as EditText
        intervalEditText.setText(prefs!!.interval)
        val hostEditText = findViewById(R.id.host) as EditText
        hostEditText.setText(prefs!!.host)
        hostEditText.addTextChangedListener(object: TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                prefs!!.host = s.toString()
            }
        })
        intervalEditText.addTextChangedListener(object: TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                prefs!!.interval = s.toString()
            }
        })
    }
    private fun startStreaming() {
        isStreaming = true

        val btn: Button = findViewById(R.id.toggleStream) as Button
        btn.text = "Stop streaming"

        val hostEditText = findViewById(R.id.host) as EditText
        val host = hostEditText.text.toString()

        val gps = findViewById(R.id.sendGps) as Switch
        val battery = findViewById(R.id.sendBattery) as Switch

        if (gps.isChecked) {
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION), 444)
            }
        }

        val intervalEditText = findViewById(R.id.interval) as EditText
        val intervalString = intervalEditText.text.toString()

        prefs!!.interval = intervalString

        var interval = intervalString.toLongOrNull()

        if (interval == null || interval < 1) interval = 1000

        prefs!!.host = host

        val deviceId = prefs!!.deviceId as String

        doAsync {
            // Run timer in async to enable background (i think)
            timer = Timer() // Initialize new timer for some reason
            timer.scheduleAtFixedRate(0, interval) {
                // Run this every N ms:
                sendPost(host, gps.isChecked, battery.isChecked, deviceId)
            }
        }
    }
    private fun stopStreaming() {
        isStreaming = false
        val btn: Button = findViewById(R.id.toggleStream) as Button
        btn.text = "Start streaming"

        timer.cancel()
    }
    private fun boolToInt(boolean: Boolean): Int {
        val ret = if (boolean) 1 else 0
        return ret
    }
    private fun sendPost(host: String, gps: Boolean, battery: Boolean, deviceId: String) {
        val millis: Long = Calendar.getInstance().timeInMillis

        var body = """{ "timestamp": $millis, "deviceId": "$deviceId","""

        var toFinish = boolToInt(gps) + boolToInt(battery)
        var isFinished = 0

        fun proceed() {
            isFinished++
            if (toFinish <= isFinished) {
                body = (body.dropLast(1) + "}").trimIndent()

                Fuel.post("http://$host").body(body).header("Content-Type" to "application/json").response { request, response, result ->
                }
            }
        }

        if (toFinish == 0) return proceed()

        if (gps) {
            try {
                fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                    val lat = location?.latitude
                    val long = location?.longitude
                    val alt = location?.altitude

                    body += """"gps": { "lat" : $lat, "long": $long, "alt": $alt },"""
                    proceed()
                }.addOnFailureListener { exception: Exception ->
                    println(exception)
                    body += """"gps": {},"""
                    proceed()
                }
            } catch (e: SecurityException) {
                proceed()
            }
        }

        if (battery) {
            val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { ifilter ->
                registerReceiver(null, ifilter)
            }
            val batteryPct: Float? = batteryStatus?.let { intent ->
                val level: Int = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale: Int = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                level / scale.toFloat() * 100
            }

            val status: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
            val isCharging: Boolean = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL

            body += """"battery": { "level": $batteryPct, "isCharging": $isCharging },"""

            proceed()
        }
    }
}