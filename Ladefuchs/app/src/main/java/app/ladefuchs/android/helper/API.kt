package app.ladefuchs.android.helper

import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Debug
import android.preference.PreferenceManager
import android.provider.Settings
import androidx.annotation.RequiresApi
import app.ladefuchs.android.BuildConfig
import app.ladefuchs.android.dataClasses.ChargeCards
import app.ladefuchs.android.dataClasses.Operator
import com.beust.klaxon.Klaxon
import okhttp3.OkHttpClient
import okhttp3.Request
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
    private var apiBaseURL: String = "https://api.ladefuchs.app/"
    private var apiVersionPath: String = "v2/"

    // production settings
    private val apiBaseRegularURL: String = "https://api.ladefuchs.app/"
    private val apiVersionRegularPath: String = ""

    // beta settings
    private val apiBaseBetaURL: String = "https://beta.api.ladefuchs.app/"
    private val apiVersionBetaPath: String = ""

    // file paths
    private var imgDir = "/images"

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
            return false;
        }

        val airplaneOn = Settings.System.getInt(context.contentResolver, Settings.Global.AIRPLANE_MODE_ON, 0) != 0
        if (airplaneOn){
            return true
        }
        return Settings.Secure.getInt(context.contentResolver, "mobile_data", 0) == 0
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun downloadJSONToInternalStorage(
        JSONUrl: String,
        JSONFileName: String,
    ) {

        if (isOffline()){
            printLog("Device is offline", "network")
            return
        }

        printLog("Downloading to Internal Storage $JSONUrl", "network")
        val client = OkHttpClient()
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
                            if (BuildConfig.DEBUG){
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
    fun retrieveOperatorList(): List<String> {

        val JSONUrl = apiBaseURL + apiVersionPath + "operators/enabled"
        val JSONFileName = "operators.json"
        // download the latest operator list
        downloadJSONToInternalStorage(JSONUrl, JSONFileName)
        // read list into pocOperatorList variable
        printLog("Reading $JSONFileName")
        var operators: List<Operator>? = null
        try {
            val operatorsFile: File? = File(context.getFileStreamPath(JSONFileName).toString())
            printLog("Trying to read " + operatorsFile.toString())
            operators = operatorsFile?.let { Klaxon().parseArray<Operator>(it) }!!
        } catch (e: Exception) {
            printLog("Could not read: $JSONFileName", "error")
            printLog(e.toString(), "error")
        }
        if (operators == null) {
            operators = context.assets?.open(JSONFileName)?.let {
                Klaxon().parseArray<Operator>(
                    it
                )
            }
        }
        if (operators != null) {
            // operators are presorted from the API .
            return operators.map { item -> item.displayName }.sortedBy { it.lowercase() }
        }
        return listOf()
    }

    /**
     * This function downloads an image from the API and saves it in local storage
     */
    fun downloadImageToInternalStorage(imageURL: URL) {

        if (isOffline()){
            printLog("Device is offline", "network")
            return
        }
        var storagePath = getImagePath(imageURL, context)
        printLog("Downloading image: ${imageURL.path}", "network")

        Thread {
            printLog("Getting Image Path: $storagePath")
            try {
                val request = Request.Builder()
                    .url(imageURL)
                    .get()
                    .header("Authorization", "Bearer $apiToken")
                    .build()
                val client = OkHttpClient()
                client.newCall(request).execute().use { response ->
                    if (response.code == 200) {
                        response.body!!.byteStream().use { input ->
                            FileOutputStream(storagePath).use { output ->
                                input.copyTo(output)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                printLog("Couldn't open stream $imageURL, error: ${e.message}", "error")
            }
        }.start()
    }

    /**
     * This function retrieves the prices for a specific chargecard-chargetype combination
     */
    fun readPrices(
        pocOperator: String,
        currentType: String,
        forceDownload: Boolean = false,
        skipDownload: Boolean = false
    ): List<ChargeCards>? {

        //Load Prices JSON from File
        val country = "de"
        val replaceRule = Regex("[^A-Za-z0-9.+-]")
        val pocOperatorClean = replaceRule.replace(pocOperator, "").lowercase(Locale.getDefault())
        printLog("ReadPrices for $pocOperatorClean")
        val JSONFileName = "$country-$pocOperatorClean-$currentType.json"
        var chargeCards: List<ChargeCards> = listOf()
        var forceInitialDownload = forceDownload

        // check whether forceDownload was activated
        if (!forceDownload || skipDownload) {
            val JSONFile: File? = File(context.getFileStreamPath(JSONFileName).toString())
            val JSONFileExists = JSONFile?.exists()

            if (!JSONFileExists!!) {
                printLog("No current file Found")
                forceInitialDownload = true
            } else {
                try {
                    printLog("JSON file ${context.getFileStreamPath(JSONFileName)}")
                    chargeCards = JSONFile.let { Klaxon().parseArray<ChargeCards>(it) }!!
                    printLog("carrrds ${chargeCards[0]}")
                } catch (e: Exception) {
                    forceInitialDownload = true
                    printLog("Error reading prices from cache ${e.message}")
                    if (BuildConfig.DEBUG){
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
            val JSONUrl =
                apiBaseURL + apiVersionPath + "cards/" + country.lowercase() + "/" + pocOperatorClean.lowercase() + "/" + currentType.lowercase()
            printLog("Data in $JSONFileName is outdated or update was forced, Updating from API: $JSONUrl")
            downloadJSONToInternalStorage(JSONUrl, JSONFileName)
            // load the freshly downloaded JSON file
            val JSONFile = File(context.getFileStreamPath(JSONFileName).toString())

            try {
                printLog("Reloading chargeCards after Download")
                chargeCards = JSONFile.let { Klaxon().parseArray<ChargeCards>(it) }!!
            } catch (e: Exception) {
//                if (BuildConfig.DEBUG)
//                    e.printStackTrace()
            }
        }


        val maingauPrices = getMaingauPrices(currentType, pocOperatorClean, context)
        if (maingauPrices.name.isNotEmpty() && pocOperatorClean.lowercase() != "ladeverbund+") {
            chargeCards = chargeCards.toMutableList()
            chargeCards.add(maingauPrices)
        }

        return chargeCards
    }
}