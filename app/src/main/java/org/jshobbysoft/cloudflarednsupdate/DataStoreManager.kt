package org.jshobbysoft.cloudflarednsupdate

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class DataStoreManager(private val context: Context) {
    //    https://medium.com/jetpack-composers/android-jetpack-datastore-5dfdfea4a3ea
    companion object {
        val INPUT_IP = stringPreferencesKey("input_IP")
        val API_KEY = stringPreferencesKey("api_key")
        val HOSTNAME = stringPreferencesKey("hostname")
        val REFRESH_INTERVAL = intPreferencesKey("refresh_interval")
        val SERVICE_ZONE_ID = stringPreferencesKey("service_zoneId")
        val SERVICE_HOST_ID = stringPreferencesKey("service_hostId")
    }

    suspend fun saveToDataStore(
        inputIP: String,
        apiKey: String,
        hostname: String,
        refreshInterval: Int
    ) {
        context.dataStore.edit { settings -> settings[INPUT_IP] = inputIP }
        context.dataStore.edit { settings -> settings[API_KEY] = apiKey }
        context.dataStore.edit { settings -> settings[HOSTNAME] = hostname }
        context.dataStore.edit { settings -> settings[REFRESH_INTERVAL] = refreshInterval }
    }

    suspend fun saveToDataStoreService(
        zoneId: String,
        hostId: String
    ) {
        context.dataStore.edit { settings -> settings[SERVICE_ZONE_ID] = zoneId }
        context.dataStore.edit { settings -> settings[SERVICE_HOST_ID] = hostId }
    }

    private val preferences = runBlocking { context.dataStore.data.first() }

    //    https://amir-raza.medium.com/preference-datastore-android-an-implementation-guide-610645153696
    fun getFromDataStoreIP(): String {
        return preferences[INPUT_IP] ?: "192.168.111.111"
    }

    fun getFromDataStoreAPI(): String {
        return preferences[API_KEY] ?: "XX"
    }

    fun getFromDataStoreHN(): String {
        return preferences[HOSTNAME] ?: "host.example.com"
    }

    fun getFromDataStoreRI(): Int {
        return preferences[REFRESH_INTERVAL] ?: 0
    }

    fun getFromDataStoreServiceZoneID(): String {
        return preferences[SERVICE_ZONE_ID] ?: "XX"
    }

    fun getFromDataStoreServiceHostID(): String {
        return preferences[SERVICE_HOST_ID] ?: "XX"
    }

}