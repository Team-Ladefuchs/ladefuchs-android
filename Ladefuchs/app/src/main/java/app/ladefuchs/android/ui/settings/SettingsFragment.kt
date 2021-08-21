package app.ladefuchs.android.ui.settings

import android.os.Bundle
import androidx.preference.MultiSelectListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import app.ladefuchs.android.R

class SettingsFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey)
        val listPreference = findPreference<Preference>("chargecards") as MultiSelectListPreference
        val entries =  arrayOf(
            "ADAC e-CHARGE",
            "Alpiq Get Charge",
            "CONNECT",
            "Charge & GO",
            "Chargemap",
            "Chargepoint",
            "E-Mobil",
            "EWE Go",
            "EWE Go",
            "EinfachStromLaden (Privat)",
            "Eins E-Mobil",
            "EnBW mobility+",
            "FahrStrom Unterwegs Standard",
            "FairstromEmobil (App)",
            "GEW E-Tankkarte",
            "IONITY",
            "InCharge",
            "JuicePass",
            "Lade TüStrom",
            "Ladekarte",
            "Ladeverbund+",
            "MVV eMotion",
            "Maingau",
            "Mein Autostrom",
            "Member / Guest",
            "Pay as you charge",
            "Plugsurfing",
            "Rodaustrom",
            "SWI e-Motion",
            "SWK-Ladekarte",
            "Shell Recharge",
            "Shell Recharge New Motion",
            "Smoov",
            "Sperto",
            "Stadtwerke Ilmenau",
            "Stadtwerke München (SWM)",
            "Supercharger",
            "Swisscharge",
            "We-Drive Easy",
            "default",
            "e-laden (Stadtwerke Baden-Baden)",
            "evway",
            "mobility+ Standard"
        )
        val entryValues =  arrayOf(
            "adac",
            "alpiq_get_charge",
            "gp_joule",
            "elli",
            "chargemap",
            "chargepoint",
            "eins",
            "ewe_go",
            "ewe_swb",
            "maingau_energie",
            "eins_e_mobil",
            "enbw_mobility",
            "lichtblick_se",
            "fairenergie",
            "gew_wilhelmshaven",
            "ionity",
            "vattenfall",
            "enel_x",
            "stadtwerke_tubingen",
            "emc",
            "ladeverbund_",
            "mvv_emotion",
            "maingau",
            "enviam",
            "fastned",
            "e_flux",
            "plugsurfing",
            "ev_rodau",
            "stadtwerke_ingolstadt",
            "stadtwerke_kaiserslautern",
            "shell_recharge",
            "shell_recharge_new_motion",
            "allego",
            "sperto",
            "stadtwerke_ilmenau",
            "stadtwerke_munchen_swm_",
            "tesla",
            "swisscharge",
            "alperia_neogy_",
            "default",
            "e_laden",
            "evway_route220_",
            "enbw"
        )
        listPreference.entries = entries
        listPreference.entryValues = entryValues
        listPreference.setOnPreferenceChangeListener { preference, result ->
            if (preference is MultiSelectListPreference) {
                PreferenceManager.getDefaultSharedPreferences(context).edit().putString("selectedChargeCards", result.toString()).apply()
            }
            true
        }
    }
}
