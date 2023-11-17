package app.ladefuchs.android.helper

import android.content.Context
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.preference.PreferenceManager
import app.ladefuchs.android.BuildConfig
import app.ladefuchs.android.dataClasses.AllCardsRequest
import app.ladefuchs.android.dataClasses.AllCardsResponse
import app.ladefuchs.android.dataClasses.Banner
import app.ladefuchs.android.dataClasses.ChargeType
import app.ladefuchs.android.dataClasses.Operator
import com.beust.klaxon.Klaxon
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.nio.file.Files
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit


private const val apiToken: String = BuildConfig.apiKey

// current configuration
private var apiBaseURL: String = "https://api.ladefuchs.app"
private var apiVersionPath: String = "v2"

// production settings
private const val apiBaseRegularURL: String = "https://api.ladefuchs.app/"
private const val apiVersionRegularPath: String = ""

// beta settings
private const val apiBaseBetaURL: String = "https://beta.api.ladefuchs.app/"
private const val apiVersionBetaPath: String = ""

private var allCardsCache: MutableMap<String, AllCardsResponse> = mutableMapOf()

private val client = OkHttpClient()
private val jsonType = "application/json; charset=utf-8".toMediaType();

/**
 * This function switches to production API
 */

fun useProd() {
    apiBaseURL = apiBaseRegularURL
    apiVersionPath = apiVersionRegularPath
}

/**
 * This function switches to beta API
 */
fun useBeta() {
    apiBaseURL = apiBaseBetaURL
    apiVersionPath = apiVersionBetaPath
}

public fun isOffline(context: Context): Boolean {
    val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val networkCapabilities = connectivityManager.activeNetwork ?: return true
    val capabilities =
        connectivityManager.getNetworkCapabilities(networkCapabilities) ?: return true

    return !capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
}

/**
 * This function retrieves the prices for chargecards-ac/dc
 */
fun retrieveCards(
    pocOperatorId: String,
    context: Context,
    forceDownload: Boolean = false,
): AllCardsResponse {
    val found = if (!forceDownload) allCardsCache.getOrDefault(pocOperatorId, null) else null
    if (found != null) {
        printLog("Found cards for $pocOperatorId in cache")
        return found
    }

    val cardFileName = getCardsFileName(pocOperatorId)
    var chargeCards = AllCardsResponse(pocOperatorId, emptyList(), emptyList())

    if (forceDownload && !isOffline(context)) {
        printLog("$pocOperatorId force download triggered")
        val url = "$apiBaseURL/$apiVersionPath/cards/de/$pocOperatorId"
        val acJson = downloadJson("$url/${ChargeType.AC}", context)
        val dcJson = downloadJson("$url/${ChargeType.DC}", context)
        val klaxon = Klaxon()
        if (acJson.isNotEmpty()) {
            chargeCards.ac = klaxon.parseArray(acJson) ?: emptyList()
        }
        if (dcJson.isNotEmpty()) {
            chargeCards.dc = klaxon.parseArray(dcJson) ?: emptyList()
        }
        allCardsCache[pocOperatorId] = chargeCards
        printLog("Write ac/dc cards for operator: $pocOperatorId to file: $cardFileName")
        writeJsonToStorage(klaxon.toJsonString(chargeCards), cardFileName, context)
        return chargeCards;
    }
    val cardsFile = File(context.getFileStreamPath(cardFileName).toString())

    try {
        val json = cardsFile.readText()
        if (json.isEmpty()) {
            return chargeCards
        }
        val cards = Klaxon().parse<AllCardsResponse>(json);
        if (cards != null) {
            chargeCards = cards
            allCardsCache[pocOperatorId] = cards
        }
    } catch (e: Exception) {
        printLog("Error while reading cards from file $cardsFile, error: ${e.message}")
        if (BuildConfig.DEBUG) {
            e.printStackTrace()
        }
    }

    return chargeCards
}

private fun downloadJson(JSONUrl: String, context: Context): String {
    if (isOffline(context)) {
        printLog("Device is offline", "network")
        return ""
    }

    printLog("Downloading to Internal Storage $JSONUrl", "network")
    var jsonResponse = ""

    val url = URL(JSONUrl)
    val request = Request.Builder()
        .url(url)
        .get()
        .header("Authorization", "Bearer $apiToken")
        .build()
    val t = Thread {
        try {
            client.newCall(request).execute().use { response ->
                if (response.code == 200) {
                    response.body?.byteStream()?.use { body ->
                        jsonResponse = body.bufferedReader().use { it.readText() }

                    }
                } else {
                    printLog("Downloading failed! StatusCode: ${response.code} Message: ${response.message}")
                }
            }
        } catch (e: Exception) {
            e.message?.let { printLog("downloadJSONToInternalStorage $it", "error") };
            if (BuildConfig.DEBUG) {
                e.printStackTrace()
            }
        }
    }

    t.start()
    t.join()

    return jsonResponse;
}

fun retrieveOperatorList(context: Context): List<Operator> {
    val operatorFileName = "operators.json"
    val operatorJson = downloadJson("$apiBaseURL/$apiVersionPath/operators/enabled", context)
    val json =
        if (operatorJson.isEmpty()) {
            try {
                val operatorsFile = File(context.getFileStreamPath(operatorFileName).toString())
                operatorsFile.readText()
            } catch (e: Exception) {
                printLog("Could not read: $operatorFileName, error: ${e.message}", "error")
                ""
            }
        } else {
            val sharedPreferences = context?.let {
                PreferenceManager.getDefaultSharedPreferences(
                    it
                )
            }
            val timeDifferenceHours =
                getHoursForPreference(sharedPreferences, "cached_operator_timestamp")
            if (timeDifferenceHours > 24) {
                Thread {
                    writeJsonToStorage(operatorFileName, operatorJson, context)
                    writeTimeToPreferences(sharedPreferences, "cached_operator_timestamp")
                }.start()
            }
            operatorJson
        }

    if (json.isEmpty()) {
        return emptyList()
    }
    try {
        return Klaxon().parseArray<Operator>(json)?.sortedBy { it.displayName.lowercase() }
            ?: emptyList()
    } catch (e: Exception) {
        if (BuildConfig.DEBUG) {
            e.printStackTrace()
        }
    }
    return emptyList()
}


/**
 * This function retrieves the current list of operators
 */


fun downloadAllCards(operatorList: List<Operator>, context: Context) {

    if (isOffline(context)) {
        return
    }
    var allCardsResponse = listOf<AllCardsResponse>()
    val t = Thread {
        try {
            val operatorIds = operatorList.map { it.identifier }
            printLog("Downloading all charge cards", "network")
            val requestBody =
                Klaxon().toJsonString(AllCardsRequest(operatorIds)).toRequestBody(jsonType);
            val request = Request.Builder()
                .url("$apiBaseURL/$apiVersionPath/cards/de")
                .post(requestBody)
                .header("Authorization", "Bearer $apiToken")
                .build()
            client.newCall(request).execute().use { response ->
                if (response.code != 200) {
                    printLog("Couldn't retrieve all cards: ${response.code}:$response", "error")
                    return@Thread;
                }
                allCardsResponse = response.body!!.byteStream().use { body ->
                    Klaxon().parseArray(body)
                } ?: emptyList()

                allCardsCache = allCardsResponse.associateBy { it.operator }.toMutableMap()

            }
        } catch (e: Exception) {
            allCardsCache = mutableMapOf()
            printLog("exception retrieve all cards, error: ${e.message}", "error")
        }
    }
    t.start()
    t.join()


    val sharedPreferences = context.let {
        PreferenceManager.getDefaultSharedPreferences(
            it
        )
    }

    val timeDifferenceHours = getHoursForPreference(sharedPreferences, "cached_card_timestamp")

    // only cache every 12h
    if (timeDifferenceHours < 12) {
        printLog("Skip caching all charge cards", "network")
        return
    }

    Thread {
        for (cards in allCardsResponse) {
            val fileName = getCardsFileName(cards.operator)
            printLog("write card for ${cards.operator} ac/dc with filename $fileName to disk");
            writeJsonToStorage(fileName, Klaxon().toJsonString(cards), context)
        }
        writeTimeToPreferences(sharedPreferences, "cached_card_timestamp")
    }.start()

}

private fun writeTimeToPreferences(sharedPreferences: SharedPreferences?, key: String) {
    val currentTimestamp = System.currentTimeMillis()
    val editor = sharedPreferences?.edit()
    editor?.putLong(key, currentTimestamp)
    editor?.apply()
}

private fun getHoursForPreference(sharedPreferences: SharedPreferences?, key: String): Long {
    val cachedCardTimestamp = sharedPreferences?.getLong(key, 0) ?: 0
    if (cachedCardTimestamp == 0L) {
        return 25L
    }
    val currentTimestamp = System.currentTimeMillis()
    val timeDifferenceMillis = currentTimestamp - cachedCardTimestamp
    return TimeUnit.MILLISECONDS.toHours(timeDifferenceMillis)
}

private fun writeJsonToStorage(
    fileName: String,
    json: String,
    context: Context
) {
    if (json.isEmpty()) {
        return
    }
    try {
        val path = context.getFileStreamPath(fileName).toPath()
        Files.write(path, json.toByteArray())
    } catch (e: Exception) {
        printLog(
            "An error occurred while writing the file: ${fileName}, error ${e.message}",
            "error"
        )
        if (BuildConfig.DEBUG) {
            e.printStackTrace()
        }
    }
}


/**
 * This function downloads an image from the API and saves it in local storage
 */
fun downloadImageToInternalStorage(
    imageURL: URL,
    context: Context,
    imgPath: File? = null,
    cpo: Boolean = false
) {

    if (isOffline(context)) {
        printLog("Device is offline", "network")
        return
    }
    val storagePath = if (imgPath !== null) imgPath else getImagePath(imageURL, context)
    printLog("Downloading image: ${imageURL.path}", "network")

    val t = Thread {
        printLog("Getting Image Path: $storagePath")
        try {
            val request = Request.Builder()
                .url(imageURL)
                .get()
                .header("Authorization", "Bearer $apiToken")
                .build()
            client.newCall(request).execute().use { response ->
                if (response.code == 200) {
                    response.body!!.byteStream().use { input ->
                        FileOutputStream(storagePath).use { output ->
                            input.copyTo(output)
                        }
                    }
                } else {
                    printLog("Couldn't retrieve image: ${response.code}:$response", "error")
                }
            }
        } catch (e: Exception) {
            printLog("Couldn't open stream $imageURL, error: ${e.message}", "error")
            if (BuildConfig.DEBUG) {
                e.printStackTrace()
            }
        }
    }
    t.start()
    t.join()
}

private fun getCardsFileName(pocOperatorId: String): String {
    return "de-$pocOperatorId.json"
}


fun retrieveBanners(context: Context): Banner? {
    if (isOffline(context)) {
        return null
    }

    val probabilities: MutableList<Banner> = mutableListOf()
    val t = Thread {
        printLog("Getting Banners")
        try {
            val request = Request.Builder()
                .url("$apiBaseURL/banners")
                .get()
                .header("Authorization", "Bearer $apiToken")
                .build()
            client.newCall(request).execute().use { response ->
                if (response.code == 200) {
                    Klaxon().parseArray<Banner>(response.body!!.string())?.forEach { banner ->
                        val file = getImagePath(URL(banner.image), context)
                        printLog(
                            "Downloading img new banner: ${banner.id}, file: ${file.canonicalFile}",
                            "debug"
                        )
                        if (!file.exists()) {
                            downloadImageToInternalStorage(
                                URL(banner.image),
                                context,
                                file
                            )
                        }
                        for (i in 0..banner.frequency) {
                            probabilities.add(banner)
                        }
                    }
                } else {
                    printLog("Couldn't retrieve banners,: ${response.code}:$response", "error")
                }
            }
        } catch (e: Exception) {
            printLog("Couldn't retrieve banners, error: ${e.message}", "error")
        }
    }
    t.start()
    t.join()

    return probabilities.randomOrNull()
}
