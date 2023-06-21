package app.ladefuchs.android.helper

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
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
import java.nio.file.Paths


class API(private var context: Context) {
    private val apiToken: String = BuildConfig.apiKey

    // current configuration
    private var apiBaseURL: String = "https://api.ladefuchs.app"
    private var apiVersionPath: String = "v2"

    // production settings
    private val apiBaseRegularURL: String = "https://api.ladefuchs.app/"
    private val apiVersionRegularPath: String = ""

    // beta settings
    private val apiBaseBetaURL: String = "https://beta.api.ladefuchs.app/"
    private val apiVersionBetaPath: String = ""

    private var allCardsCache: MutableMap<String, AllCardsResponse> = mutableMapOf()

    private val jsonType = "application/json; charset=utf-8".toMediaType();
    private val client = OkHttpClient()

    /**
     * This function switches to production API
     */
    fun useProd() {
        this.apiBaseURL = apiBaseRegularURL
        this.apiVersionPath = apiVersionRegularPath
    }

    /**
     * This function switches to beta API
     */
    fun useBeta() {
        this.apiBaseURL = apiBaseBetaURL
        this.apiVersionPath = apiVersionBetaPath
    }

    private fun isOffline(): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkCapabilities = connectivityManager.activeNetwork ?: return true
        val capabilities =
            connectivityManager.getNetworkCapabilities(networkCapabilities) ?: return true

        return !capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private inline fun downloadJson(JSONUrl: String): String {

        if (isOffline()) {
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


    /**
     * This function retrieves the current list of operators
     */
    fun retrieveOperatorList(): List<Operator> {
        val operatorFileName = "operators.json"
        val operatorJson = if (isOffline()) {
            try {
                val operatorsFile = File(context.getFileStreamPath(operatorFileName).toString())
                printLog("Trying to read $operatorsFile")
                operatorsFile.readText()
            } catch (e: Exception) {
                printLog("Could not read: $operatorFileName", "error")
                printLog(e.toString(), "error")
                ""
            }
        } else {
            val json = downloadJson(
                "$apiBaseURL/$apiVersionPath/operators/enabled",
            )
            writeJsonToStorage(operatorFileName, json)
            json
        }
        return if (operatorJson.isEmpty()) {
            emptyList()
        } else {
            Klaxon().parseArray<Operator>(operatorJson)?.sortedBy { it.displayName.lowercase() }
                ?: emptyList()
        }
    }

    fun downloadAllCards(operatorList: List<Operator>) {

        if (isOffline()) {
            return
        }

        var allCardsResponse = listOf<AllCardsResponse>()
        val t = Thread {
            try {
                val operatorIds = operatorList.map { it.identifier }
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
                printLog("exception retrieve all cards, error: ${e.message}", "error")
            }
        }

        t.start();
        t.join()
        Thread {
            for (cards in allCardsResponse) {
                val fileName = getCardsFileName(cards.operator)
                printLog("write card for ${cards.operator} ac/dc with filename $fileName to disk");
                writeJsonToStorage(fileName, Klaxon().toJsonString(cards))
            }
        }.start()
    }

    private fun writeJsonToStorage(
        fileName: String,
        json: String
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
    fun downloadImageToInternalStorage(imageURL: URL, imgPath: File? = null, cpo: Boolean = false) {

        if (isOffline()) {
            printLog("Device is offline", "network")
            return
        }
        val storagePath = if (imgPath !== null) imgPath else getImagePath(imageURL, context, cpo)
        printLog("Downloading image: ${imageURL.path}", "network")

        Thread {
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
        }.start()
    }

    private fun getCardsFileName(pocOperatorId: String): String {
        return "de-$pocOperatorId.json"
    }


    /**
     * This function retrieves the prices for chargecards-ac/dc
     */
    fun retrieveCards(
        pocOperatorId: String,
        forceDownload: Boolean = false,
    ): AllCardsResponse {

        val found = if (!forceDownload) allCardsCache.getOrDefault(pocOperatorId, null) else null
        if (found != null) {
            printLog("Found cards for $pocOperatorId in cache")
            return found
        }

        val cardFileName = getCardsFileName(pocOperatorId)
        var chargeCards = AllCardsResponse(pocOperatorId, emptyList(), emptyList())

        if (forceDownload && !isOffline()) {
            val url = "$apiBaseURL/$apiVersionPath/cards/de/$pocOperatorId"

            val acJson = downloadJson("$url/${ChargeType.AC}")
            val dcJson = downloadJson("$url/${ChargeType.DC}")
            val klaxon = Klaxon()
            if (acJson.isNotEmpty()) {
                chargeCards.ac = klaxon.parseArray(acJson) ?: emptyList()
            }
            if (dcJson.isNotEmpty()) {
                chargeCards.dc = klaxon.parseArray(dcJson) ?: emptyList()
            }
            printLog("Write ac/dc cards for operator: $pocOperatorId to file: $cardFileName")
            writeJsonToStorage(klaxon.toJsonString(chargeCards), cardFileName)
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

    fun retrieveBanners(): Banner? {

        if (isOffline()) {
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
                            if (!File("${context.filesDir}/${banner.filename}").exists()) {
                                downloadImageToInternalStorage(
                                    URL(banner.image),
                                    File("${context.filesDir}/${banner.filename}")
                                )
                            } else if (Files.getLastModifiedTime(Paths.get(context.filesDir.toString() + "/" + banner.filename))
                                    .toInstant().toEpochMilli() < banner.updated
                            ) {
                                printLog("Updating img for ${banner.id} to newest version", "debug")
                                Files.deleteIfExists(Paths.get(context.filesDir.toString() + "/" + banner.filename))
                                downloadImageToInternalStorage(
                                    URL(banner.image),
                                    File("${context.filesDir}/${banner.filename}")
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
}