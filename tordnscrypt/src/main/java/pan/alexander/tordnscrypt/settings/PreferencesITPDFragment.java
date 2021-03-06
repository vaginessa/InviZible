package pan.alexander.tordnscrypt.settings;
/*
    This file is part of InviZible Pro.

    InviZible Pro is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    InviZible Pro is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with InviZible Pro.  If not, see <http://www.gnu.org/licenses/>.

    Copyright 2019 by Garmatin Oleksandr invizible.soft@gmail.com
*/

import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.support.v7.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import pan.alexander.tordnscrypt.R;
import pan.alexander.tordnscrypt.SettingsActivity;
import pan.alexander.tordnscrypt.utils.PrefManager;
import pan.alexander.tordnscrypt.utils.file_operations.FileOperations;
import pan.alexander.tordnscrypt.modules.ModulesRestarter;

import static pan.alexander.tordnscrypt.utils.RootExecService.LOG_TAG;

public class PreferencesITPDFragment extends PreferenceFragmentCompat implements Preference.OnPreferenceChangeListener {

    ArrayList<String> key_itpd;
    ArrayList<String> val_itpd;
    ArrayList<String> key_itpd_orig;
    ArrayList<String> val_itpd_orig;
    String appDataDir;
    String itpdPath;
    String busyboxPath;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setRetainInstance(true);

        addPreferencesFromResource(R.xml.preferences_i2pd);

        try {
            findPreference("Allow incoming connections").setOnPreferenceChangeListener(this);
            findPreference("incoming port").setOnPreferenceChangeListener(this);
            findPreference("incoming host").setOnPreferenceChangeListener(this);
            findPreference("ipv4").setOnPreferenceChangeListener(this);
            findPreference("ipv6").setOnPreferenceChangeListener(this);
            findPreference("notransit").setOnPreferenceChangeListener(this);
            findPreference("floodfill").setOnPreferenceChangeListener(this);
            findPreference("bandwidth").setOnPreferenceChangeListener(this);
            findPreference("share").setOnPreferenceChangeListener(this);
            findPreference("ssu").setOnPreferenceChangeListener(this);
            findPreference("ntcp").setOnPreferenceChangeListener(this);
            findPreference("Enable ntcpproxy").setOnPreferenceChangeListener(this);
            findPreference("ntcpproxy").setOnPreferenceChangeListener(this);
            findPreference("HTTP proxy").setOnPreferenceChangeListener(this);
            findPreference("HTTP proxy port").setOnPreferenceChangeListener(this);
            findPreference("Socks proxy").setOnPreferenceChangeListener(this);
            findPreference("Socks proxy port").setOnPreferenceChangeListener(this);
            findPreference("SAM interface").setOnPreferenceChangeListener(this);
            findPreference("SAM interface port").setOnPreferenceChangeListener(this);
            findPreference("elgamal").setOnPreferenceChangeListener(this);
            findPreference("UPNP").setOnPreferenceChangeListener(this);
            findPreference("ntcp2 enabled").setOnPreferenceChangeListener(this);
            findPreference("verify").setOnPreferenceChangeListener(this);
            findPreference("transittunnels").setOnPreferenceChangeListener(this);
            findPreference("openfiles").setOnPreferenceChangeListener(this);
            findPreference("coresize").setOnPreferenceChangeListener(this);
            findPreference("ntcpsoft").setOnPreferenceChangeListener(this);
            findPreference("ntcphard").setOnPreferenceChangeListener(this);
            findPreference("defaulturl").setOnPreferenceChangeListener(this);
        } catch (Exception e) {
            Log.e(LOG_TAG, "PreferencesITPDFragment setOnPreferenceChangeListener exception " + e.getMessage() + " " + e.getCause());
            Toast.makeText(getActivity(), "PreferencesITPDFragment exception " + e.getMessage(), Toast.LENGTH_LONG).show();
        }


        if (getArguments() != null) {
            key_itpd = getArguments().getStringArrayList("key_itpd");
            val_itpd = getArguments().getStringArrayList("val_itpd");
            key_itpd_orig = new ArrayList<>(key_itpd);
            val_itpd_orig = new ArrayList<>(val_itpd);
        }
    }

    @Override
    public void onCreatePreferences(Bundle bundle, String s) {

    }

    @Override
    public void onResume() {
        super.onResume();

        if (getActivity() == null) {
            return;
        }

        getActivity().setTitle(R.string.drawer_menu_I2PDSettings);

        PathVars pathVars = new PathVars(getActivity());
        appDataDir = pathVars.appDataDir;
        itpdPath = pathVars.itpdPath;
        busyboxPath = pathVars.busyboxPath;
    }

    public void onStop() {
        super.onStop();

        if (getActivity() == null) {
            return;
        }

        List<String> itpd_conf = new LinkedList<>();
        boolean isChanged = false;
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getActivity());

        if (key_itpd.indexOf("subscriptions") >= 0)
            val_itpd.set(key_itpd.indexOf("subscriptions"), sp.getString("subscriptions", ""));


        for (int i = 0; i < key_itpd.size(); i++) {
            if (!(key_itpd_orig.get(i).equals(key_itpd.get(i)) && val_itpd_orig.get(i).equals(val_itpd.get(i))) && !isChanged) {
                isChanged = true;
            }

            switch (key_itpd.get(i)) {
                case "incoming host":
                    key_itpd.set(i, "host");
                    break;
                case "incoming port":
                case "Socks proxy port":
                case "HTTP proxy port":
                case "SAM interface port":
                    key_itpd.set(i, "port");
                    break;
                case "ntcp2 enabled":
                case "SAM interface":
                case "Socks proxy":
                case "http enabled":
                case "HTTP proxy":
                case "UPNP":
                    key_itpd.set(i, "enabled");
                    break;
            }

            if (val_itpd.get(i).isEmpty()) {
                itpd_conf.add(key_itpd.get(i));
            } else {
                itpd_conf.add(key_itpd.get(i) + " = " + val_itpd.get(i));
            }

        }

        if (!isChanged) return;

        FileOperations.writeToTextFile(getActivity(), appDataDir + "/app_data/i2pd/i2pd.conf", itpd_conf, SettingsActivity.itpd_conf_tag);

        boolean itpdRunning = new PrefManager(getActivity()).getBoolPref("I2PD Running");

        if (itpdRunning) {
            ModulesRestarter.restartITPD(getActivity());
        }


    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        try {
            if (Objects.equals(preference.getKey(), "Allow incoming connections")) {
                if (Boolean.valueOf(newValue.toString())) {
                    key_itpd.set(key_itpd.indexOf("#host"), "incoming host");
                    key_itpd.set(key_itpd.indexOf("#port"), "incoming port");
                } else {
                    key_itpd.set(key_itpd.indexOf("incoming host"), "#host");
                    key_itpd.set(key_itpd.indexOf("incoming port"), "#port");
                }
                return true;
            } else if (Objects.equals(preference.getKey(), "Enable ntcpproxy")) {
                if (Boolean.valueOf(newValue.toString())) {
                    key_itpd.set(key_itpd.indexOf("#ntcpproxy"), "ntcpproxy");
                } else {
                    key_itpd.set(key_itpd.indexOf("ntcpproxy"), "#ntcpproxy");
                }
                return true;
            }

            if (key_itpd.contains(preference.getKey().trim())) {
                val_itpd.set(key_itpd.indexOf(preference.getKey()), newValue.toString());
                return true;
            } else {
                Toast.makeText(getActivity(), R.string.pref_itpd_not_exist, Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, "PreferencesITPDFragment onPreferenceChange exception " + e.getMessage() + " " + e.getCause());
            Toast.makeText(getActivity(), R.string.wrong, Toast.LENGTH_LONG).show();
        }


        return false;
    }
}
