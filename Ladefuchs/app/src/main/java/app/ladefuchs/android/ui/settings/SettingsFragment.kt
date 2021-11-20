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
            "Alpiq Get Charge",
            "CONNECT",
            "Charge & GO",
            "Chargemap",
            "Chargepoint",
            "e-laden (Stadtwerke Baden-Baden)",
            "E-Mobil",
            "EinfachStromLaden (Privat)",
            "EnBW mobility+",
            "evway",
            "EWE Go",
            "FahrStrom Unterwegs Standard",
            "FairstromEmobil (App)",
            "Fastened",
            "GEW E-Tankkarte",
            "IONITY",
            "InCharge",
            "JuicePass",
            "Lade TüStrom",
            "Ladekarte",
            "Ladeverbund+",
            "Mein Autostrom",
            "mobility+ Standard",
            "MVV eMotion",
            "Pay as you charge",
            "Plugsurfing",
            "Rodaustrom",
            "Shell Recharge",
            "Smoov",
            "Sperto",
            "Stadtwerke Ilmenau",
            "Stadtwerke München (SWM)",
            "Supercharger",
            "Swisscharge",
            "SWI e-Motion",
            "SWK-Ladekarte",
            "We-Drive Easy"
            )
        val entryValues =  arrayOf(
            "alpiq_get_charge",
            "gp_joule",
            "elli",
            "chargemap",
            "chargepoint",
            "e_laden",
            "eins,eins_e_mobil",
            "maingau_energie",
            "enbw_mobility",
            "evway_route220_",
            "ewe_go, ewe_swb",
            "lichtblick_se",
            "fairenergie",
            "fastned",
            "gew_wilhelmshaven",
            "ionity",
            "vattenfall",
            "enel_x",
            "stadtwerke_tubingen",
            "emc",
            "ladeverbund_",
            "enviam",
            "enbw",
            "mvv_emotion",
            "e_flux",
            "plugsurfing",
            "ev_rodau",
            "shell_recharge, shell_recharge_new_motion",
            "allego",
            "sperto",
            "stadtwerke_ilmenau",
            "stadtwerke_munchen_swm_",
            "tesla",
            "swisscharge",
            "stadtwerke_ingolstadt",
            "stadtwerke_kaiserslautern",
            "alperia_neogy_"
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
