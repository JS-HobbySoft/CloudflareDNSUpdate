//@file:OptIn(ExperimentalMaterial3Api::class)

package org.jshobbysoft.cloudflarednsupdate

import android.content.Intent
import android.content.pm.PackageManager
import android.net.InetAddresses
import android.os.Build
import android.os.Bundle
import android.util.Patterns
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
//import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.jshobbysoft.cloudflarednsupdate.ui.theme.CloudflareDNSUpdateTheme
import org.json.JSONObject
import java.net.URL
//import java.nio.charset.Charset

class MainActivity : ComponentActivity() {

    private val scope = CoroutineScope(Job() + Dispatchers.IO)
    private val actionStopListen = "action_stop_listen"
    private var updateSuccess = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            CloudflareDNSUpdateTheme {
                val snackBarHostState = remember { SnackbarHostState() }
                Scaffold(
                    content = { contentPadding ->
//                      https://medium.com/jetpack-composers/what-does-the-paddingvalues-parameter-in-a-compose-scaffold-do-3bd5592b9c6b
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(contentPadding)
                        )

                        // A surface container using the 'background' color from the theme
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            color = MaterialTheme.colorScheme.background
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(all = 10.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                var userInputIP by remember {
                                    mutableStateOf(
                                        DataStoreManager(
                                            applicationContext
                                        ).getFromDataStoreIP()
                                    )
                                }

                                var userInputHostName by remember {
                                    mutableStateOf(
                                        DataStoreManager(
                                            applicationContext
                                        ).getFromDataStoreHN()
                                    )
                                }

                                var userInputApiKey by remember {
                                    mutableStateOf(
                                        DataStoreManager(
                                            applicationContext
                                        ).getFromDataStoreAPI()
                                    )
                                }

                                var userInputRefreshInterval by remember {
                                    mutableStateOf(
                                        DataStoreManager(
                                            applicationContext
                                        ).getFromDataStoreRI().toString()
                                    )
                                }

                                val launcher = rememberLauncherForActivityResult(
                                    ActivityResultContracts.RequestPermission()
                                ) { isGranted: Boolean ->
                                    if (isGranted) {
                                        // Permission Accepted: Do something
                                        println("PERMISSION GRANTED")
                                    } else {
                                        // Permission Denied: Do something
                                        println("PERMISSION DENIED")
                                    }
                                }

                                Row(
                                    modifier = Modifier
                                        .padding(all = 10.dp)
                                        .height(64.dp)
                                        .fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Start,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    TextField(
                                        value = userInputIP,
                                        onValueChange = { userInputIP = it },
                                        label = { Text("Enter IP address") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.size(170.dp)
                                    )
                                    Spacer(modifier = Modifier.width(width = 10.dp))
                                    Button(
                                        onClick = {
                                            scope.launch {
                                                try {
                                                    val url = URL("https://icanhazip.com/").readText().replace("\n", "")
                                                    userInputIP = url
                                                } catch (e: Exception) {
                                                    snackBarHostState.showSnackbar("Error in IP address retrieval: $e")
                                                }
                                            }
                                        }
                                    ) {
                                        Text(text = "Get IP address")
                                    }
                                }
                                Row(
                                    modifier = Modifier
                                        .padding(all = 10.dp)
                                        .height(64.dp)
                                        .fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Start,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    TextField(
                                        value = userInputApiKey,
                                        onValueChange = { userInputApiKey = it },
                                        label = { Text("Enter API token") },
                                        modifier = Modifier.size(144.dp)
                                    )
                                    Spacer(modifier = Modifier.width(width = 10.dp))
                                    Button(
                                        onClick = {
                                            var formatErrors = false
                                            if (userInputApiKey.length != 40) {
                                                scope.launch {
                                                    snackBarHostState.showSnackbar("API token must be 40 characters")
                                                }
                                                formatErrors = true
                                            }
                                            val re = Regex("[_0-9A-z\\-]+")
                                            if (!userInputApiKey.matches(re)) {
                                                scope.launch {
                                                    snackBarHostState.showSnackbar("API token must only contain numbers, letters, underscore, and hyphen")
                                                }
                                                formatErrors = true
                                            }
                                            if (!formatErrors) {
                                                scope.launch {
                                                    try {
                                                        val keyTestResult =
                                                            CloudflareAPIKey.retrofitService.checkAPIKey(
                                                                getHeaderMap(userInputApiKey)
                                                            )
                                                        val reApiKey = Regex(".*success.:true.*")
                                                        if (keyTestResult.matches(reApiKey)) {
                                                            snackBarHostState.showSnackbar("API token is valid")
                                                        }
                                                    } catch (e: Exception) {
                                                        snackBarHostState.showSnackbar("Invalid token: $e")
                                                    }
                                                }
                                            }
                                        }
                                    ) {
                                        Text(text = "Test API token")
                                    }
                                }
                                Row(
                                    modifier = Modifier
                                        .padding(all = 10.dp)
                                        .height(64.dp)
                                        .fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Start,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    TextField(
                                        value = userInputHostName,
                                        onValueChange = { userInputHostName = it },
                                        label = { Text("Enter host name to be updated") }
                                    )
                                }
                                Row(
                                    modifier = Modifier
                                        .padding(all = 10.dp)
                                        .height(64.dp)
                                        .fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Button(
                                        onClick = {
                                            var formatErrors = false
                                            if (!isValidInetAddress(userInputIP)) {
                                                scope.launch {
                                                    snackBarHostState.showSnackbar("IP address format error: $userInputIP")
                                                }
                                                formatErrors = true
                                            }
                                            if (!Patterns.DOMAIN_NAME.matcher(userInputHostName)
                                                    .matches()
                                            ) {
                                                scope.launch {
                                                    snackBarHostState.showSnackbar("Host name format error")
                                                }
                                                formatErrors = true
                                            }
                                            if (userInputApiKey.length != 40) {
                                                scope.launch {
                                                    snackBarHostState.showSnackbar("API token must be 40 characters")
                                                }
                                                formatErrors = true
                                            }
                                            val re = Regex("[_0-9A-z\\-]+")
                                            if (!userInputApiKey.matches(re)) {
                                                scope.launch {
                                                    snackBarHostState.showSnackbar("API token must only contain numbers, letters, underscore, and hyphen")
                                                }
                                                formatErrors = true
                                            }
                                            if (!formatErrors) {
                                                var hasZoneId = false
                                                var hasHostId = false
                                                var ipNeedsUpdate = true
                                                scope.launch {
                                                    var zoneIdValue = "e"
                                                    var hostIdentifier = "e"
                                                    /*
                                                    **********************************************************************************
                                                    *    Get zone ID
                                                    **********************************************************************************
                                                    */
                                                    try {
                                                        val zoneIDTestResult =
                                                            CloudflareAPIZoneID.retrofitService.checkAPIZoneID(
                                                                getHeaderMap(userInputApiKey)
                                                            )
                                                        if (zoneIDTestResult.success) {
                                                            val hostDomainName =
                                                                userInputHostName.substringAfter(".")
                                                            for (item in zoneIDTestResult.result) {
                                                                if (item.name == hostDomainName) {
                                                                    snackBarHostState.showSnackbar(
                                                                        "Retrieved zone ID for $hostDomainName",
                                                                        duration = SnackbarDuration.Short
                                                                    )
                                                                    zoneIdValue = item.id!!
                                                                    hasZoneId = true
                                                                } else {
                                                                    snackBarHostState.showSnackbar(
                                                                        "Zone ID for $hostDomainName not found",
                                                                        duration = SnackbarDuration.Short
                                                                    )
                                                                }
                                                            }
                                                        }
                                                    } catch (e: Exception) {
                                                        println(e)
                                                        snackBarHostState.showSnackbar("Zone ID retrieval error: $e")
                                                    }
                                                    /*
                                                    **********************************************************************************
                                                    *    Get host ID
                                                    **********************************************************************************
                                                    */
                                                    try {
                                                        var dnsType = "A"
                                                        if (userInputIP.contains(":")) {
                                                            dnsType = "AAAA"
                                                        }

                                                        val hostUrl =
                                                            "https://api.cloudflare.com/client/v4/zones/" +
                                                                    zoneIdValue +
                                                                    "/dns_records?name=" +
                                                                    userInputHostName +
                                                                    "&type=" +
                                                                    dnsType
                                                        val hostIDTestResult =
                                                            CloudflareAPIHostID.retrofitService.checkAPIHostID(
                                                                hostUrl,
                                                                getHeaderMap(userInputApiKey)
                                                            )
                                                        if (hostIDTestResult.success) {
                                                            for (item in hostIDTestResult.result) {
                                                                if (item.name == userInputHostName) {
                                                                    snackBarHostState.showSnackbar(
                                                                        "Retrieved host ID for $userInputHostName",
                                                                        duration = SnackbarDuration.Short
                                                                    )
                                                                    hostIdentifier = item.id!!
                                                                    hasHostId = true
                                                                } else {
                                                                    snackBarHostState.showSnackbar(
                                                                        "Host ID for $userInputHostName not found",
                                                                        duration = SnackbarDuration.Short
                                                                    )
                                                                }
                                                                if (item.content == userInputIP) {
                                                                    snackBarHostState.showSnackbar("IP address is already ${item.content} and does not require update")
                                                                    ipNeedsUpdate = false
                                                                }
                                                            }
                                                        }
                                                    } catch (e: Exception) {
                                                        snackBarHostState.showSnackbar("Host ID error: $e")
                                                        println(e)
                                                    }
                                                    if (hasHostId && hasZoneId && ipNeedsUpdate) {
                                                        try {
                                                            val updateDNSResult =
                                                                CloudflareAPIDNS.retrofitServiceDNS.updateDNS(
                                                                    getDNSUrl(
                                                                        zoneIdValue,
                                                                        hostIdentifier
                                                                    ),
                                                                    getHeaderMap(userInputApiKey),
                                                                    getDNSBody(
                                                                        userInputIP,
                                                                        userInputHostName
                                                                    )
                                                                )
                                                            val reDnsKey =
                                                                Regex(".*success.:true.*")
                                                            if (updateDNSResult.matches(reDnsKey)) {
                                                                snackBarHostState.showSnackbar("DNS record successfully updated")
                                                                updateSuccess = true
                                                            }
                                                        } catch (e: Exception) {
                                                            snackBarHostState.showSnackbar("DNS record update error: $e")
                                                        }
                                                    }
                                                    if (updateSuccess) {
                                                        DataStoreManager(applicationContext).saveToDataStore(
                                                            userInputIP,
                                                            userInputApiKey,
                                                            userInputHostName,
                                                            userInputRefreshInterval.toInt()
                                                        )
                                                        DataStoreManager(applicationContext).saveToDataStoreService(
                                                            zoneIdValue,
                                                            hostIdentifier
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    ) {
                                        Text(text = "Update DNS record")
                                    }
                                }
                                HorizontalDivider(color = Color.Black, thickness = 1.dp)
                                Row(
                                    modifier = Modifier
                                        .padding(all = 10.dp)
                                        .height(64.dp)
                                        .fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Start,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    TextField(
                                        value = userInputRefreshInterval,
                                        onValueChange = {
                                            userInputRefreshInterval = it
                                        },
                                        label = { Text("Refresh interval [sec]\n600-6000, 0 to disable") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.size(170.dp)
                                    )
                                    Spacer(modifier = Modifier.width(width = 10.dp))
                                    Button(
                                        onClick = {
                                            // https://stackoverflow.com/questions/60608101/how-request-permissions-with-jetpack-compose
                                            // Check permission
                                            when (PackageManager.PERMISSION_GRANTED) {
                                                ContextCompat.checkSelfPermission(
                                                    applicationContext,
                                                    android.Manifest.permission.POST_NOTIFICATIONS
                                                ) -> {
                                                    if (Build.VERSION.SDK_INT >= 33) {
                                                        println("Permissions already granted")
                                                    } else {
                                                        println("Permissions not required for Android 12 and lower")
                                                    }
                                                }

                                                else -> {
                                                    // Asking for permission
                                                    if (Build.VERSION.SDK_INT >= 33) {
                                                        launcher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                                                    }
                                                }
                                            }

                                            var numFormatError = true
                                            try {
                                                userInputRefreshInterval.toInt()
                                                numFormatError = false
                                            } catch (e: Exception) {
                                                scope.launch {
                                                    snackBarHostState.showSnackbar("Format error: Refresh interval must be an integer number: $e")
                                                }
                                            }
                                            if (!numFormatError) {
                                                if (userInputRefreshInterval.toInt() !in 600..7200) {
                                                    scope.launch {
                                                        snackBarHostState.showSnackbar("Refresh interval must be between 600-7200")
                                                    }
                                                } else if (!updateSuccess) {
                                                    scope.launch {
                                                        snackBarHostState.showSnackbar("Automatic updating requires a successful manual update first")
                                                    }
                                                } else {
                                                    scope.launch {
                                                        if (!MyForegroundService.isActivityRunning) {
                                                            DataStoreManager(applicationContext).saveToDataStore(
                                                                userInputIP,
                                                                userInputApiKey,
                                                                userInputHostName,
                                                                userInputRefreshInterval.toInt()
                                                            )
                                                            val serviceIntent = Intent(
                                                                applicationContext,
                                                                MyForegroundService::class.java
                                                            )
                                                            applicationContext.startForegroundService(
                                                                serviceIntent
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    ) {
                                        Text(text = "Start")
                                    }
                                    Spacer(modifier = Modifier.width(width = 10.dp))
                                    Button(onClick = {
                                        if (MyForegroundService.isActivityRunning) {
                                            val serviceIntentStop = Intent(
                                                applicationContext,
                                                MyForegroundService::class.java
                                            )
                                            serviceIntentStop.action = actionStopListen
                                            applicationContext.startForegroundService(
                                                serviceIntentStop
                                            )
                                        }
                                    }
                                    ) {
                                        Text(text = "Stop")
                                    }
                                }
                            }
                        }
                    },
                    snackbarHost = { SnackbarHost(snackBarHostState) }
                )
            }
        }
    }

    //  https://www.tech-otaku.com/web-development/using-cloudflare-api-manage-dns-records/

    private fun getHeaderMap(inputKey: String): Map<String, String> {
        val headerMap = mutableMapOf<String, String>()
        headerMap["Authorization"] = "Bearer $inputKey"
        headerMap["Content-Type"] = "application/json"
        return headerMap
    }

    private fun getDNSUrl(zi: String, hi: String): String {
//    https://api.cloudflare.com/client/v4/zones/{zone_identifier}/dns_records/{identifier}
        return "https://api.cloudflare.com/client/v4/zones/$zi/dns_records/$hi"
    }

    private fun getDNSBody(updateIP: String, updateHostName: String): RequestBody {
        val mediaType = "application/json".toMediaTypeOrNull()
//    https://johncodeos.com/how-to-make-post-get-put-and-delete-requests-with-retrofit-using-kotlin/
        val jsonObject = JSONObject()
        jsonObject.put("content", updateIP)
        jsonObject.put("name", updateHostName)
        jsonObject.put("proxied", false)
        jsonObject.put("type", "A")
        jsonObject.put("ttl", 1)
        return jsonObject.toString().toRequestBody(mediaType)
    }

    private fun isValidIPV4(ip: String): Boolean {
        return Patterns.IP_ADDRESS.matcher(ip).matches()
    }

    private fun isValidIPV6(ip: String): Boolean {
        // Regex for IPv6 (full and compressed) from https://stackoverflow.com/questions/53497/regular-expression-that-matches-valid-ipv6-addresses
        val ipv6Regex =
            Regex("^(([0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4}|([0-9a-fA-F]{1,4}:){1,7}:|([0-9a-fA-F]{1,4}:){1,6}:[0-9a-fA-F]{1,4}|([0-9a-fA-F]{1,4}:){1,5}(:[0-9a-fA-F]{1,4}){1,2}|([0-9a-fA-F]{1,4}:){1,4}(:[0-9a-fA-F]{1,4}){1,3}|([0-9a-fA-F]{1,4}:){1,3}(:[0-9a-fA-F]{1,4}){1,4}|([0-9a-fA-F]{1,4}:){1,2}(:[0-9a-fA-F]{1,4}){1,5}|[0-9a-fA-F]{1,4}:((:[0-9a-fA-F]{1,4}){1,6})|:((:[0-9a-fA-F]{1,4}){1,7}|:)|fe80:(:[0-9a-fA-F]{0,4}){0,4}%[0-9a-zA-Z]+|::(ffff(:0{1,4})?:)?((25[0-5]|(2[0-4]|1?[0-9])?[0-9]).){3}(25[0-5]|(2[0-4]|1?[0-9])?[0-9])|([0-9a-fA-F]{1,4}:){1,4}:((25[0-5]|(2[0-4]|1?[0-9])?[0-9]).){3}(25[0-5]|(2[0-4]|1?[0-9])?[0-9]))$")
        return ipv6Regex.matches(ip)
    }

    private fun isValidInetAddress(ip: String): Boolean {
//        if (Build.VERSION.SDK_INT >= 29) {println(ip.toBinaryString())}
        return if (Build.VERSION.SDK_INT >= 29) {
            InetAddresses.isNumericAddress(ip)
        } else {
            isValidIPV4(ip) || isValidIPV6(ip)
        }
    }
}

//fun String.toBinaryString(charset: Charset = Charsets.UTF_8): String {
//    // Convert the string to a ByteArray using the specified character set
//    val bytes = this.toByteArray(charset)
//
//    // Use a StringBuilder to efficiently build the final binary string
//    val binaryStringBuilder = StringBuilder()
//
//    for (byte in bytes) {
//        // Convert each byte to its binary representation as an integer.
//        // The `b.toInt() and 0xFF` ensures we treat the byte as an unsigned value
//        // so that negative bytes don't interfere with the conversion.
//        val binary = Integer.toBinaryString(byte.toInt() and 0xFF)
//
//        // Pad the binary string with leading zeros to ensure it's always 8 bits long
//        val paddedBinary = binary.padStart(8, '0')
//
//        binaryStringBuilder.append(paddedBinary).append(" ")
//    }
//
//    // Return the result, trimming any trailing space
//    return binaryStringBuilder.toString().trim()
//}
