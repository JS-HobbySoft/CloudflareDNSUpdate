@file:OptIn(ExperimentalSerializationApi::class)

package org.jshobbysoft.cloudflarednsupdate

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody
import retrofit2.Retrofit
import retrofit2.converter.scalars.ScalarsConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.HeaderMap
import retrofit2.http.PUT
import retrofit2.http.Url


//    https://github.com/google-developer-training/basic-android-kotlin-compose-training-mars-photos/blob/repo-starter/app/src/main/java/com/example/marsphotos/network/MarsApiService.kt
private const val BASE_URL =
    "https://api.cloudflare.com/client/v4/"

/**
 * Use the Retrofit builder to build a retrofit object using a kotlinx.serialization converter
 */
//val rf = Retrofit.Builder().build()
private val retrofitKey = Retrofit.Builder()
    .addConverterFactory(ScalarsConverterFactory.create())
    .baseUrl(BASE_URL)
    .build()

@OptIn(ExperimentalSerializationApi::class)
private val retrofitJSON = Retrofit.Builder()
    .addConverterFactory(Json{
            ignoreUnknownKeys = true
        }.asConverterFactory("application/json".toMediaType()))
    .baseUrl(BASE_URL)
    .build()

/**
 * Retrofit service object for creating api calls
*/

interface CloudflareAPIServiceKeyCheck {
    @GET("user/tokens/verify")
    suspend fun checkAPIKey(@HeaderMap headers: Map<String, String>): String
}


/**
 * A public Api object that exposes the lazy-initialized Retrofit service
 */
object CloudflareAPIKey {
    val retrofitService: CloudflareAPIServiceKeyCheck by lazy {
        retrofitKey.create(CloudflareAPIServiceKeyCheck::class.java)
    }
}

private val retrofitDNS = Retrofit.Builder()
    .addConverterFactory(ScalarsConverterFactory.create())
    .baseUrl(BASE_URL)
    .build()

interface CloudflareAPIServiceUpdateDNS {
    @PUT
    suspend fun updateDNS(@Url url: String, @HeaderMap headers: Map<String, String>, @Body body:RequestBody): String
}

object CloudflareAPIDNS {
    val retrofitServiceDNS: CloudflareAPIServiceUpdateDNS by lazy {
        retrofitDNS.create(CloudflareAPIServiceUpdateDNS::class.java)
    }
}

/*
**********************************************************
*   Zone ID
**********************************************************
*/

interface CloudflareAPIServiceZoneID {
    @GET("zones")
    suspend fun checkAPIZoneID(@HeaderMap headers: Map<String, String>): CfApiQueryZone
}

object CloudflareAPIZoneID {
    val retrofitService: CloudflareAPIServiceZoneID by lazy {
        retrofitJSON.create(CloudflareAPIServiceZoneID::class.java)
    }
}

@Serializable
data class CfApiQueryZone (
    var result: ArrayList<CfApiQueryZoneArray> = ArrayList(),
    var result_info: ResultInfo? = null,
    var success: Boolean = false,
    var errors: ArrayList<String> = ArrayList(),
    var messages: ArrayList<String> = ArrayList()
)

@Serializable
data class ResultInfo (
    var page:Int = 0,
    var per_page: Int = 0,
    var total_pages: Int = 0,
    var count: Int = 0,
    var total_count: Int = 0
)

@Serializable
data class CfApiQueryZoneArray (
    var id: String? = null,
    var name: String? = null,
    var status: String? = null,
    var paused: Boolean = false,
    var type: String? = null,
    var development_mode: Int = 0,
    var name_servers: ArrayList<String> = ArrayList(),
    var original_name_servers: ArrayList<String> = ArrayList(),
    var original_registrar: String? = null,
    var original_dnshost: String? = null,
    var modified_on: String? = null,
    var created_on: String? = null,
    var activated_on: String? = null,
    var meta: Meta? = null,
    var owner: Owner? = null,
    var account: Account? = null,
    var tenant: Tenant? = null,
    var tenant_unit: TenantUnit? = null,
    var permissions: ArrayList<String> = ArrayList(),
    var plan: Plan? = null
)

@Serializable
data class Plan (
    var id: String? = null,
    var name: String? = null,
    var price: Int = 0,
    var currency: String? = null,
    var frequency: String? = null,
    var is_subscribed: Boolean = false,
    var can_subscribe: Boolean = false,
    var legacy_id: String? = null,
    var legacy_discount: Boolean = false,
    var externally_managed: Boolean = false
)

@Serializable
data class TenantUnit (
    var id: String? = null
)

@Serializable
data class Tenant (
    var id: String? = null,
    var name: String? = null
)

@Serializable
data class Account (
    var id: String? = null,
    var name: String? = null
)

@Serializable
data class Owner (
    var id: String? = null,
    var type: String? = null,
    var email: String? = null
)

@Serializable
data class Meta (
    var step: Int = 0,
    var custom_certificate_quota: Int = 0,
    var page_rule_quota: Int = 0,
    var phishing_detected: Boolean = false,
    var multiple_railguns_allowed: Boolean = false
)

/*
**********************************************************
*   Host ID
**********************************************************
*/


interface CloudflareAPIServiceHostID {
    @GET
    suspend fun checkAPIHostID(@Url url: String, @HeaderMap headers: Map<String, String>): CfApiQueryHost
}

object CloudflareAPIHostID {
    val retrofitService: CloudflareAPIServiceHostID by lazy {
        retrofitJSON.create(CloudflareAPIServiceHostID::class.java)
    }
}

@Serializable
data class CfApiQueryHost (
    var result: ArrayList<CfApiQueryHostArray> = ArrayList(),
    var success: Boolean = false,
    var errors:ArrayList<String> = ArrayList(),
    var messages:ArrayList<String> = ArrayList(),
    var result_info: ResultInfoHost? = null

)

@Serializable
data class ResultInfoHost (
    var page: Int = 0,
    var per_page: Int = 0,
    var count: Int = 0,
    var total_count: Int = 0,
    var total_pages: Int = 0

)

@Serializable
data class CfApiQueryHostArray (
    var id: String? = null,
    var zone_id: String? = null,
    var zone_name: String? = null,
    var name: String? = null,
    var type: String? = null,
    var content: String? = null,
    var proxiable: Boolean = false,
    var proxied: Boolean = false,
    var ttl: Int = 0,
    var locked: Boolean = false,
    var meta: MetaHost? = null,
    var comment: String? = null,
    var tags:ArrayList<String> = ArrayList(),
    var created_on: String? = null,
    var modified_on: String? = null
)

@Serializable
data class MetaHost (
    var auto_added: Boolean = false,
    var managed_by_apps: Boolean = false,
    var managed_by_argo_tunnel: Boolean = false,
    var source: String? = null
)
