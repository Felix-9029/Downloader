package de.felix.downloader

import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.CountDownTimer
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.google.android.material.snackbar.Snackbar
import de.felix.downloader.databinding.ActivityMainBinding
import kotlinx.android.synthetic.main.activity_main.*
import java.net.URL
import kotlin.math.roundToInt


class MainActivity : AppCompatActivity() {

    var countDownTimer: CountDownTimer? = null

    companion object {
        lateinit var mainActivity: MainActivity
            private set
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mainActivity = this
        setContentView(R.layout.activity_main)


        val binding: ActivityMainBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(toolbar)

        applySharedPreferenceSettings()

        progressBarDownload.max = 100

        val downloadServiceIntent = Intent(this@MainActivity, DownloadService::class.java)

        buttonStartDownload.setOnClickListener {view ->
            if (checkURL(textViewURL.text.toString())) {
                setSharedPreference("textViewURL", textViewURL.text.toString())
                if (!isWriteExternalStoragePermissionGranted()) {
                    ActivityCompat.requestPermissions(this@MainActivity, arrayOf(WRITE_EXTERNAL_STORAGE), 1)
                }
                else {
                    startService(downloadServiceIntent)
                    startCountDownTimer()
                }
            }
            else {
                Snackbar.make(view, R.string.wrongURL, Snackbar.LENGTH_LONG).setAction("Action", null).show()
            }
        }

        buttonCancelDownload.setOnClickListener {
            stopService(downloadServiceIntent)
            try {
                countDownTimer!!.cancel()
            } catch (_: Exception) { }
            progressBarDownload.progress = 0
        }

        setSupportActionBar(toolbar)
    }

    override fun onResume() {
        super.onResume()
        applySharedPreferenceSettings()
    }

    fun startCountDownTimer() {
        countDownTimer = object : CountDownTimer(Long.MAX_VALUE, 1) {
            override fun onTick(millisUntilFinished: Long) {
                val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mainActivity)
                val progress = sharedPreferences.getString("downloadState", "0")!!.toDouble().roundToInt()
                progressBarDownload.progress = progress
            }
            override fun onFinish() {
                start()
            }
        }.start()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent Activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> {
                val intent = Intent(this@MainActivity, SettingsActivity::class.java)
                startActivity(intent)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun setSharedPreference(key: String, value: String) {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        sharedPreferences.edit {
            putString(key, value)
        }
    }

    private fun applySharedPreferenceSettings() {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        val textSize = sharedPreferences.getString("fontsize", "19")!!.toFloat()
        textViewURIString.textSize = textSize
        textViewURL.textSize = textSize
        buttonStartDownload.textSize = textSize
        buttonCancelDownload.textSize = textSize

        val darkmode = sharedPreferences.getBoolean("darkmode", true)
        if (darkmode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
    }

    private fun checkURL(urlString: String): Boolean {
        return try {
            val url = URL(urlString)
            url.toURI()
            true
        } catch (exception: Exception) {
            false
        }
    }

    private fun isWriteExternalStoragePermissionGranted(): Boolean {
        val checkPermission = ActivityCompat.checkSelfPermission(this, WRITE_EXTERNAL_STORAGE)
        return checkPermission == PackageManager.PERMISSION_GRANTED
    }
}