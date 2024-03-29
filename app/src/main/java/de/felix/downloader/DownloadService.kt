package de.felix.downloader

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.Environment
import android.os.IBinder
import android.widget.Toast
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.time.LocalDateTime
import java.util.regex.Pattern
import kotlin.math.roundToInt

class DownloadService : Service() {

    var cancel = false
    lateinit var message: String

    inner class MyBinder : Binder() {
        // Return this instance of MyService so clients can call public methods
        val service: DownloadService
            get() =// Return this instance of MyService so clients can call public methods
                this@DownloadService
    }

    private val binder: IBinder = MyBinder()

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(MainActivity.mainActivity)
        val textViewURL = sharedPreferences.getString("textViewURL", "https://www.example.com/file")!!

        val runnable = Runnable {
            downloadWorker(textViewURL)
            stopSelf()
        }
        val thread = Thread(runnable)
        thread.start()
        return super.onStartCommand(intent, flags, startId)
    }

    private fun downloadWorker(urlString: String) {
        val fileName = URLtoFilename(urlString)

        val url = URL(urlString)
        val connection = url.openConnection()
        connection.connectTimeout = 100000
        connection.doInput = true
        val max = connection.contentLength

        var progress = 0.0

        BufferedInputStream(url.openStream()).use { bufferedInputStream ->
            FileOutputStream(File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileName)).use { fileOutputStream ->
                val dataBuffer = ByteArray(1024)
                var bytesRead: Int
                while (bufferedInputStream.read(dataBuffer, 0, 1024).also { bytesRead = it } != -1) {
                    if (cancel) {
                        setSharedPreference("downloadState", "0.0")
                        break
                    }
                    fileOutputStream.write(dataBuffer, 0, bytesRead)
                    progress += 1024
                    setSharedPreference("downloadState", (progress/max*100).toString())
                }
                fileOutputStream.close()
            }
            bufferedInputStream.close()
        }
    }

    private fun setSharedPreference(key: String, value: String) {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(MainActivity.mainActivity)
        sharedPreferences.edit {
            putString(key, value)
        }
    }

    private fun URLtoFilename(url: String): String {
        val patternValue = Pattern.compile(".*[a-zA-Z\\d]+/([^/]+)", Pattern.CASE_INSENSITIVE)
        val matcherValue = patternValue.matcher(url)
        matcherValue.find()
        return if (!matcherValue.matches()) {
            val date =  LocalDateTime.now().year.toString() + "_"+ LocalDateTime.now().monthValue.toString() + "_" + LocalDateTime.now().dayOfMonth.toString() + "_" + LocalDateTime.now().hour.toString() + "_" + LocalDateTime.now().minute.toString() + "_" + LocalDateTime.now().second.toString()
            "UnknownFile-$date"
        } else {
            matcherValue.group(1)!!
        }
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        message = getString(R.string.DownloadFinished)
        Toast.makeText(this, getString(R.string.DownloadStarted), Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        cancel = true
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(MainActivity.mainActivity)
        if (sharedPreferences.getString("downloadState", "0")!!.toDouble().roundToInt() < 100) {
            message = getString(R.string.DownloadCanceled)
        }
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}