package com.example.myapplication.data.repository

import com.example.myapplication.data.model.ResourceEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class NpiResourceRepository {

    //Base URL for NPI Registry
    private val baseUrl = "https://npiregistry.cms.hhs.gov/api/?version=2.1"

    suspend fun fetchMentalHealthProviders(limit: Int = 30): List<ResourceEntry> =
        withContext(Dispatchers.IO) {
            val providerList = mutableListOf<ResourceEntry>()

            try {
                val urlString = "$baseUrl&taxonomy_description=Psychologist&enumeration_type=NPI-1&limit=$limit"
                val url = URL(urlString)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 8000
                connection.readTimeout = 8000

                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    val jsonResponse = connection.inputStream.bufferedReader().use { it.readText() }
                    val rootObject = JSONObject(jsonResponse)

                    if (rootObject.has("results")) {
                        val resultsArray = rootObject.getJSONArray("results")

                        for (i in 0 until resultsArray.length()) {
                            val item = resultsArray.getJSONObject(i)

                            //Get name and credentials
                            val basic = item.optJSONObject("basic")
                            val firstName = basic?.optString("first_name", "")?.lowercase()
                                ?.replaceFirstChar { it.uppercase() } ?: ""
                            val lastName = basic?.optString("last_name", "")?.lowercase()
                                ?.replaceFirstChar { it.uppercase() } ?: ""
                            val credential = basic?.optString("credential", "") ?: ""
                            val organizationName = basic?.optString("organization_name", "") ?: ""
                            val formattedName = when {
                                firstName.isNotBlank() && lastName.isNotBlank() -> {
                                    if (credential.isNotBlank()) "$firstName $lastName, $credential" else "$firstName $lastName"
                                }

                                organizationName.isNotBlank() -> organizationName
                                else -> ""
                            }


                            //Get phone and location info
                            val addresses = item.optJSONArray("addresses")
                            var phoneNum = "N/A"
                            var locationStr = "United States"

                            if (addresses != null && addresses.length() > 0) {
                                val primaryAddr = addresses.getJSONObject(0)
                                val rawPhone = primaryAddr.optString("telephone_number", "")
                                val city = primaryAddr.optString("city", "")?.lowercase()
                                    ?.replaceFirstChar { it.uppercase() } ?: ""
                                val state = primaryAddr.optString("state", "") ?: ""

                                if (city.isNotBlank() && state.isNotBlank()) {
                                    locationStr = "$city, $state"
                                }

                                //Format phone num
                                if (rawPhone.length >= 10) {
                                    val digits = rawPhone.filter { it.isDigit() }
                                    if (digits.length == 10) {
                                        phoneNum = "(${digits.substring(0, 3)}) ${
                                            digits.substring(
                                                3,
                                                6
                                            )
                                        }-${digits.substring(6)}"
                                    } else {
                                        phoneNum = rawPhone
                                    }
                                }
                            }

                            val taxonomies = item.optJSONArray("taxonomies")
                            var specialty = "Mental Health Provider"
                            if (taxonomies != null && taxonomies.length() > 0) {
                                specialty = taxonomies.getJSONObject(0)
                                    .optString("desc", "Mental Health Provider")
                            }

                            val description =
                                "$formattedName is a licensed $specialty located in $locationStr. Verified in the National Provider Registry (NPI)."

                            if (formattedName.isNotBlank()) {
                                providerList.add(
                                    ResourceEntry(
                                        name = formattedName,
                                        description = description,
                                        phoneNum = phoneNum
                                    )
                                )
                            }
                        }
                    }
                }
                connection.disconnect()
            } catch (e: Exception) {
                e.printStackTrace()
            }

            return@withContext providerList
        }
}