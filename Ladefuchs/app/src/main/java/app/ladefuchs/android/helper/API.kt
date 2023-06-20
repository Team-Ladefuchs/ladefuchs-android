package app.ladefuchs.android.helper

import android.content.Context
import android.provider.Settings
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

    private var allCardsCache: Map<String, AllCardsResponse> = mapOf()

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
        val wifiOn =
            Settings.System.getInt(context.contentResolver, Settings.Global.WIFI_ON, 0) != 0
        if (wifiOn) {
            return false
        }

        val airplaneOn = Settings.System.getInt(
            context.contentResolver,
            Settings.Global.AIRPLANE_MODE_ON,
            0
        ) != 0
        if (airplaneOn) {
            return true
        }
        return Settings.Secure.getInt(context.contentResolver, "mobile_data", 0) == 0
    }

    private inline fun downloadJSONToInternalStorage(
        JSONUrl: String,
        JSONFileName: String,
    ): String {

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

        if (!jsonResponse.isEmpty()) {
            val t2 = Thread {
                storeFileInInternalStorage(jsonResponse, JSONFileName, context)
            }
            t2.start()
        }

        return jsonResponse;
    }


    /**
     * This function retrieves the current list of operators
     */
    fun retrieveOperatorList(): List<Operator> {
        val JSONUrl = "$apiBaseURL/$apiVersionPath/operators/enabled"
        val json = downloadJSONToInternalStorage(JSONUrl, "operators.json")

        return if (json.isNullOrEmpty()) {
            // Operators are presorted from the API.
            emptyList()
        } else {
            Klaxon().parseArray<Operator>(json)?.sortedBy { it.displayName.lowercase() }
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

                    allCardsCache = allCardsResponse.associateBy { it.operator }

                }
            } catch (e: Exception) {
                printLog("exception retrieve all cards, error: ${e.message}", "error")
            }
        }

        t.start();
        t.join()

        Thread {
            for (cards in allCardsResponse) {
                writeCardToStorage(cards.operator, ChargeType.AC, cards)
            }
        }.start()

    }

    private fun writeCardToStorage(
        operatorId: String,
        chargeType: ChargeType,
        cards: AllCardsResponse
    ) {
        val JSONFileName = getCardFileName(operatorId);
        val path = context.getFileStreamPath(JSONFileName).toPath()
        val JSONFile = File(path.toString())
        if (JSONFile.exists()) {
            return
        }
        printLog("write card for $operatorId ac/dc to disk");
        try {
            Files.write(path, Klaxon().toJsonString(cards).toByteArray())
        } catch (e: Exception) {
            println("An error occurred while writing the file: ")
            printLog(
                "An error occurred while writing the file $chargeType, $JSONFile, $operatorId: ${e.message}",
                "error"
            )

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
            }
        }.start()
    }

    private fun getCardFileName(pocOperatorId: String): String {
        return "de-$pocOperatorId.json"
    }

    /**
     * This function retrieves the prices for a specific chargecard-ac/dc combination
     */
    fun readPrices(
        pocOperatorId: String,
        forceDownload: Boolean = false,
        skipDownload: Boolean = false
    ): AllCardsResponse {

        printLog("read Prices for operator $pocOperatorId")
        //Load Prices JSON from File

        val found = if (!forceDownload) allCardsCache.getOrDefault(pocOperatorId, null) else null
        if (found != null) {
            printLog("Found cards for $pocOperatorId in cache")
            return found
        }
        
        var forceInitialDownload = forceDownload
        val JSONFileName = getCardFileName(pocOperatorId)
        var chargeCards = AllCardsResponse(pocOperatorId, emptyList(), emptyList())

        // check whether forceDownload was activated
        if (!forceDownload || skipDownload) {
            val JSONFile = File(context.getFileStreamPath(JSONFileName).toString())
            val JSONFileExists = JSONFile.exists()

            if (!JSONFileExists) {
                printLog("No current file Found")
                forceInitialDownload = true
            } else {
                try {
                    printLog("JSON file ${context.getFileStreamPath(JSONFileName)}")
                    chargeCards = JSONFile.let { Klaxon().parse(it) }!!
                } catch (e: Exception) {
                    forceInitialDownload = true
                    printLog("Error reading prices from cache ${e.message}")
                    if (BuildConfig.DEBUG) {
                        e.message?.let { printLog(it, "error") }
                    }
                }
            }
        }
        if (!skipDownload && (
                    (chargeCards.dc.isNotEmpty() && chargeCards.ac.isNotEmpty() && (System.currentTimeMillis() / 1000L - chargeCards.dc.first().updated > 86400))
                            || forceDownload
                            || forceInitialDownload
                    )
        ) {
            val JSONUrl = "$apiBaseURL/$apiVersionPath/cards/de/$pocOperatorId/currentType"
            printLog("Data in $JSONFileName is outdated or update was forced, Updating from API: $JSONUrl")

            var json = downloadJSONToInternalStorage(JSONUrl, JSONFileName)

            val value = Klaxon().parse<AllCardsResponse>(json)
            chargeCards = value ?: chargeCards

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