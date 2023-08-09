package org.jshobbysoft.cloudflarednsupdate

import android.app.Notification.EXTRA_NOTIFICATION_ID
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.widget.Toast
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.net.URL

//  https://medium.com/@Codeible/understanding-and-using-services-in-android-background-foreground-services-8130f6bbf2a5

class MyForegroundService : Service() {

//    https://stackoverflow.com/questions/68353369/coroutine-doesnt-stop-when-the-service-containing-it-stops
//    https://developer.android.com/kotlin/coroutines/coroutines-adv
    private val scope = CoroutineScope(Job() + Dispatchers.IO)
    private val mainHandler = Handler(Looper.getMainLooper())

//    https://stackoverflow.com/questions/66742265/the-correct-way-to-determine-if-service-is-running
//    https://stackoverflow.com/questions/57326315/how-to-check-in-foreground-service-if-the-app-is-running
    companion object {
        // this can be used to check if the app is running or not
        @JvmField  var isActivityRunning: Boolean = false
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        val actionStopListen = "action_stop_listen"

//        https://stackoverflow.com/questions/62082842/how-to-stop-a-foreground-service-from-the-notification-in-android
        if (intent != null && intent.action == actionStopListen) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            isActivityRunning = false
            return START_NOT_STICKY
        }

        val channelId = "Foreground Service ID"
        val channel = NotificationChannel(
            channelId,
            channelId,
            NotificationManager.IMPORTANCE_DEFAULT
        )

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)

//        https://developer.android.com/develop/ui/views/notifications/build-notification#Actions
//        https://stackoverflow.com/questions/30422452/how-to-stop-service-from-its-own-foreground-notification
//        https://stackoverflow.com/questions/59879040/how-to-stop-a-foreground-service-using-a-broadcast-receiver
//        https://stackoverflow.com/questions/76407866/how-to-stop-a-foreground-service-with-addaction-from-notification-building-us
//        https://medium.com/huawei-developers/foreground-services-with-notification-channel-in-android-7a272f07ad1
        val stopIntent = Intent(this, MyForegroundService::class.java).apply {
            action = actionStopListen
            putExtra(EXTRA_NOTIFICATION_ID, 0)
        }

        val stopPendingIntent: PendingIntent =
            PendingIntent.getForegroundService(this, 0, stopIntent, PendingIntent.FLAG_IMMUTABLE)

        val builder = NotificationCompat.Builder(this, channelId)
            .setContentTitle("CloudFlare DNS updater is running")
            .setContentText("Click notification to end the running updater")
            .setSmallIcon(R.drawable.ic_stat_name)
            .setContentIntent(stopPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        val serviceZoneId = DataStoreManager(applicationContext).getFromDataStoreServiceZoneID()
        val serviceHostId = DataStoreManager(applicationContext).getFromDataStoreServiceHostID()
        val serviceHostName = DataStoreManager(applicationContext).getFromDataStoreHN()
        val serviceApiKey = DataStoreManager(applicationContext).getFromDataStoreAPI()
        val serviceRefreshInterval =
            DataStoreManager(applicationContext).getFromDataStoreRI().toLong() * 1000

        var myIP: String

        val serviceUrl =
            "https://api.cloudflare.com/client/v4/zones/$serviceZoneId/dns_records/$serviceHostId"

        val headerMap = mutableMapOf<String, String>()
        headerMap["Authorization"] = "Bearer $serviceApiKey"
        headerMap["Content-Type"] = "application/json"

        mainHandler.post(object : Runnable {
            override fun run() {
                try {
                    scope.launch {
                        myIP = URL("http://whatismyip.akamai.com/").readText()

                        val hostUrl =
                            "https://api.cloudflare.com/client/v4/zones/" +
                                    serviceZoneId +
                                    "/dns_records?name=" +
                                    serviceHostName +
                                    "&type=A"
                        val hostIDTestResult =
                            CloudflareAPIHostID.retrofitService.checkAPIHostID(
                                hostUrl,
                                headerMap
                            )
                        if (hostIDTestResult.success) {
                            for (item in hostIDTestResult.result) {
                                if (item.content != myIP) {
                                    val mediaType = "application/json".toMediaTypeOrNull()
                                    val jsonObject = JSONObject()
                                    jsonObject.put("content", myIP)
                                    jsonObject.put("name", serviceHostName)
                                    jsonObject.put("proxied", false)
                                    jsonObject.put("type", "A")
                                    jsonObject.put("ttl", 1)
                                    val apiBody = jsonObject.toString().toRequestBody(mediaType)

                                    CloudflareAPIDNS.retrofitServiceDNS.updateDNS(
                                        serviceUrl,
                                        headerMap,
                                        apiBody
                                    )
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    println(e)
                    Toast.makeText(
                        applicationContext,
                        "DNS update error: $e",
                        Toast.LENGTH_LONG
                    ).show()
                }
                mainHandler.postDelayed(this, serviceRefreshInterval)
            }
        })

        startForeground(400, builder.build())
        isActivityRunning = true

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        TODO("Not yet implemented")
    }

    override fun onDestroy() {
        mainHandler.removeCallbacksAndMessages(null)
        scope.cancel()
        isActivityRunning = false
        super.onDestroy()
    }
}
