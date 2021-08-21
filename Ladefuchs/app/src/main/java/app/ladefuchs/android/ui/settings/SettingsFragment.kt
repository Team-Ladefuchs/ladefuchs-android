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
            "Allego",
            "Chargemap",
            "Stadtwerke München (SWM)",
            "Enel X",
            "eins",
            "EV Rodau"
        )
        val entryValues =  arrayOf(
            "allego",
            "chargemap",
            "stadtwerke_munchen_swm_",
            "enel_x",
            "eins",
            "ev_rodau"
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
