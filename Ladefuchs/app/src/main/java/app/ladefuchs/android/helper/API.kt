package app.ladefuchs.android.helper

import android.content.Context
import android.provider.Settings
import app.ladefuchs.android.BuildConfig
import app.ladefuchs.android.dataClasses.AllCardsRequest
import app.ladefuchs.android.dataClasses.AllCardsResponse
import app.ladefuchs.android.dataClasses.Banner
import app.ladefuchs.android.dataClasses.ChargeCards
import app.ladefuchs.android.dataClasses.ChargeType
import app.ladefuchs.android.dataClasses.Operator
import com.beust.klaxon.Klaxon
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.URL
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*


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

    private fun downloadJSONToInternalStorage(
        JSONUrl: String,
        JSONFileName: String,
    ) {

        if (isOffline()) {
            printLog("Device is offline", "network")
            return
        }

        printLog("Downloading to Internal Storage $JSONUrl", "network")
        val url = URL(JSONUrl)
        val request = Request.Builder()
            .url(url)
            .get()
            .header("Authorization", "Bearer $apiToken")
            .build()
        try {
            val t = Thread {
                client.newCall(request).execute().use { response ->
                    if (response.code == 200) {

                        try {
                            val result =
                                Files.deleteIfExists(Paths.get(context.filesDir.toString() + "/" + JSONFileName))
                            if (result) {
                                printLog("Deletion succeeded.")
                            } else {
                                printLog("Deletion failed.")
                            }
                        } catch (e: IOException) {
                            printLog("Deletion failed.")
                            if (BuildConfig.DEBUG) {
                                e.printStackTrace()
                            }
                        }

                        storeFileInInternalStorage(
                            response.body!!.string().byteInputStream(),
                            JSONFileName,
                            context
                        )
                    } else {
                        printLog("Downloading failed! Statuscode: ${response.code} Message: ${response.message}")
                    }
                }
            }
            t.start()
            // wait till file is downloaded because otherwise everything seems to break on first execution
            t.join()
        } catch (e: Exception) {
            printLog("Couldn't download JSON Data from $JSONUrl", "error")
            printLog("Exception: $e", "error")
        }
    }


    /**
     * This function retrieves the current list of operators
     */
    fun retrieveOperatorList(): List<Operator> {

        val JSONUrl = "$apiBaseURL/$apiVersionPath/operators/enabled"
        val JSONFileName = "operators.json"
        // download the latest operator list
        downloadJSONToInternalStorage(JSONUrl, JSONFileName)
        // read list into pocOperatorList variable
        printLog("Reading $JSONFileName")
        var operators: List<Operator>? = null
        try {
            val operatorsFile= File(context.getFileStreamPath(JSONFileName).toString())
            printLog("Trying to read $operatorsFile")
            operators = operatorsFile.let { Klaxon().parseArray(it) }!!
        } catch (e: Exception) {
            printLog("Could not read: $JSONFileName", "error")
            printLog(e.toString(), "error")
        }
        if (operators == null) {
            operators = context.assets?.open(JSONFileName)?.let {
                Klaxon().parseArray(
                    it
                )
            }
        }
        if (operators != null) {
            // operators are presorted from the API .
            return operators.sortedBy { it.displayName.lowercase() }
        }
        return listOf()
    }


    fun downloadAllCards(operatorList: List<Operator>){

        val t = Thread {
            try {
                val operatorIds =  operatorList.map { it.identifier }
                val requestBody = Klaxon().toJsonString(AllCardsRequest(operatorIds)).toRequestBody(jsonType);
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
                    val allCards = response.body!!.byteStream().use { body ->
                        Klaxon().parseArray<AllCardsResponse>(body)
                    } ?: emptyList()

                    for(card in allCards){
                        writeCardToStorage(card.operator, ChargeType.AC, card.ac)
                        writeCardToStorage(card.operator, ChargeType.DC, card.dc)
                    }
                }
            } catch (e: Exception) {
                printLog("exception retrieve all cards, error: ${e.message}", "error")
            }
        }

        t.start();
        t.join();

    }
    private fun writeCardToStorage( operatorId: String, chargeType: ChargeType, cards: List<ChargeCards> ) {
        val JSONFileName = getCardFileName(operatorId, chargeType);
        val path = context.getFileStreamPath(JSONFileName).toPath()
        val JSONFile = File(path.toString())
        if (JSONFile.exists()){
            return
        }
        printLog("write card for $operatorId ac/dc to disk");
        try {
            Files.write(path, Klaxon().toJsonString(cards).toByteArray())
        }catch (e: Exception) {
            println("An error occurred while writing the file: ")
            printLog("An error occurred while writing the file $chargeType, $JSONFile, $operatorId: ${e.message}", "error")

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
                    }
                    else{
                        printLog("Couldn't retrieve image: ${response.code}:$response", "error")
                    }
                }
            } catch (e: Exception) {
                printLog("Couldn't open stream $imageURL, error: ${e.message}", "error")
            }
        }.start()
    }

    private fun getCardFileName(pocOperatorId:String, currentType: ChargeType): String {
        return "de-$pocOperatorId-$currentType.json"
    }

    /**
     * This function retrieves the prices for a specific chargecard-chargetype combination
     */
    fun readPrices(
        pocOperatorId: String,
        currentType: ChargeType,
        forceDownload: Boolean = false,
        skipDownload: Boolean = false
    ): List<ChargeCards> {

        //Load Prices JSON from File
        val JSONFileName = getCardFileName(pocOperatorId, currentType)
        var chargeCards: List<ChargeCards> = listOf()
        var forceInitialDownload = forceDownload

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
                    chargeCards = JSONFile.let { Klaxon().parseArray(it) }!!
                } catch (e: Exception) {
                    forceInitialDownload = true
                    printLog("Error reading prices from cache ${e.message}")
                    if (BuildConfig.DEBUG) {
                        e.printStackTrace()
                    }
                }
            }
        }
        if (!skipDownload && ((
                    chargeCards.isNotEmpty() &&
                            (System.currentTimeMillis() / 1000L - chargeCards[0].updated > 86400)
                    ) ||
                    forceDownload ||
                    forceInitialDownload)
        ) {
            val JSONUrl = "$apiBaseURL/$apiVersionPath/cards/de/$pocOperatorId/currentType"
            printLog("Data in $JSONFileName is outdated or update was forced, Updating from API: $JSONUrl")
            downloadJSONToInternalStorage(JSONUrl, JSONFileName)
            // load the freshly downloaded JSON file
            val JSONFile = File(context.getFileStreamPath(JSONFileName).toString())

            try {
                printLog("Reloading chargeCards after Download")
                chargeCards = JSONFile.let { Klaxon().parseArray(it) }!!
            } catch (e: Exception) {
//                if (BuildConfig.DEBUG)
//                    e.printStackTrace()
            }
        }

        return chargeCards
    }

    fun retrieveBanners(): Banner {
        val probabilities: MutableList<Banner> = arrayListOf()
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
                                )                            }
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
        return probabilities.random()
    }
}