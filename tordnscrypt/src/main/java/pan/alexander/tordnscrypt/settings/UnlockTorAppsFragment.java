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

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.preference.PreferenceManager;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SearchView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import pan.alexander.tordnscrypt.R;
import pan.alexander.tordnscrypt.dialogs.NotificationHelper;
import pan.alexander.tordnscrypt.utils.PrefManager;
import pan.alexander.tordnscrypt.utils.TorRefreshIPsWork;
import pan.alexander.tordnscrypt.utils.Verifier;
import pan.alexander.tordnscrypt.utils.file_operations.FileOperations;

import static pan.alexander.tordnscrypt.TopFragment.TOP_BROADCAST;
import static pan.alexander.tordnscrypt.TopFragment.appSign;
import static pan.alexander.tordnscrypt.TopFragment.wrongSign;
import static pan.alexander.tordnscrypt.utils.RootExecService.LOG_TAG;

/**
 * A simple {@link Fragment} subclass.
 */
public class UnlockTorAppsFragment extends Fragment implements CompoundButton.OnCheckedChangeListener, SearchView.OnQueryTextListener {
    boolean isChanged;
    RecyclerView rvListTorApps;
    RecyclerView.Adapter mAdapter;
    ProgressBar pbTorApp;
    boolean torTethering;
    boolean routeAllThroughTorDevice;
    String unlockAppsStr;
    ArrayList<String> unlockAppsArrListSaved;
    ArrayList<AppUnlock> appsUnlock;
    ArrayList<AppUnlock> savedAppsUnlockWhenSearch = null;
    Thread thread;


    public UnlockTorAppsFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_preferences_tor_apps, container, false);

        ((Switch) view.findViewById(R.id.swTorAppSellectorAll)).setOnCheckedChangeListener(this);
        ((SearchView) view.findViewById(R.id.searhTorApp)).setOnQueryTextListener(this);
        pbTorApp = view.findViewById(R.id.pbTorApp);

        return view;
    }


    @Override
    public void onResume() {
        super.onResume();

        if (getActivity() == null) {
            return;
        }


        ////////////////////////////////////////////////////////////////////////////////////
        ///////////////////////Reverse logic when route all through Tor!///////////////////
        //////////////////////////////////////////////////////////////////////////////////


        isChanged = false;
        appsUnlock = new ArrayList<>();

        SharedPreferences shPref = PreferenceManager.getDefaultSharedPreferences(getActivity());
        torTethering = shPref.getBoolean("pref_common_tor_tethering", false);
        routeAllThroughTorDevice = shPref.getBoolean("pref_fast_all_through_tor", true);

        if (!routeAllThroughTorDevice) {
            Objects.requireNonNull(getActivity()).setTitle(R.string.pref_tor_unlock_app);
            unlockAppsStr = "unlockApps";
        } else {
            Objects.requireNonNull(getActivity()).setTitle(R.string.pref_tor_clearnet_app);
            unlockAppsStr = "clearnetApps";
        }

        Set<String> setUnlockApps = new PrefManager(Objects.requireNonNull(getActivity())).getSetStrPref(unlockAppsStr);
        unlockAppsArrListSaved = new ArrayList<>(setUnlockApps);

        rvListTorApps = getActivity().findViewById(R.id.rvTorApps);
        rvListTorApps.requestFocus();
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getActivity());
        rvListTorApps.setLayoutManager(mLayoutManager);

        mAdapter = new TorAppsAdapter();
        rvListTorApps.setAdapter(mAdapter);

        getDeviceApps(getActivity(), mAdapter, unlockAppsArrListSaved);

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Verifier verifier = new Verifier(getActivity());
                    String appSignAlt = verifier.getApkSignature();
                    if (!verifier.decryptStr(wrongSign, appSign, appSignAlt).equals(TOP_BROADCAST)) {
                        NotificationHelper notificationHelper = NotificationHelper.setHelperMessage(
                                getActivity(), getText(R.string.verifier_error).toString(), "11");
                        if (notificationHelper != null && getFragmentManager() != null) {
                            notificationHelper.show(getFragmentManager(), NotificationHelper.TAG_HELPER);
                        }
                    }

                } catch (Exception e) {
                    NotificationHelper notificationHelper = NotificationHelper.setHelperMessage(
                            getActivity(), getText(R.string.verifier_error).toString(), "188");
                    if (notificationHelper != null && getFragmentManager() != null) {
                        notificationHelper.show(getFragmentManager(), NotificationHelper.TAG_HELPER);
                    }
                    Log.e(LOG_TAG, "UnlockTorAppsFragment fault " + e.getMessage() + " " + e.getCause() + System.lineSeparator() +
                            Arrays.toString(e.getStackTrace()));
                }
            }
        });
        thread.start();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();

        if (getActivity() == null) {
            return;
        }

        if (thread.isAlive()) {
            try {
                thread.interrupt();
            } catch (Exception e) {
                Log.e(LOG_TAG, "UnlockTorAppsFragment onStop exception " + e.getMessage() + " " + e.getCause());
            }
            return;
        }

        PathVars pathVars = new PathVars(getActivity());
        String appDataDir = pathVars.appDataDir;

        if (!isChanged)
            return;

        if (savedAppsUnlockWhenSearch != null) {
            appsUnlock = savedAppsUnlockWhenSearch;
        }

        Set<String> setAppUIDtoSave = new HashSet<>();
        for (int i = 0; i < appsUnlock.size(); i++) {
            AppUnlock app = appsUnlock.get(i);
            if (app.active)
                setAppUIDtoSave.add(app.uid);
        }
        new PrefManager(getActivity()).setSetStrPref(unlockAppsStr, setAppUIDtoSave);

        List<String> listAppUIDtoSave = new LinkedList<>(setAppUIDtoSave);
        FileOperations.writeToTextFile(getActivity(), appDataDir + "/app_data/tor/" + unlockAppsStr, listAppUIDtoSave, "ignored");
        Toast.makeText(getActivity(), getString(R.string.toastSettings_saved), Toast.LENGTH_SHORT).show();

        /////////////Refresh iptables rules/////////////////////////
        TorRefreshIPsWork torRefreshIPsWork = new TorRefreshIPsWork(getActivity(), null);
        torRefreshIPsWork.refreshIPs();
    }

    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean active) {
        if (compoundButton.getId() == R.id.swTorAppSellectorAll) {
            if (active) {
                for (int i = 0; i < appsUnlock.size(); i++) {
                    AppUnlock app = appsUnlock.get(i);
                    app.active = true;
                    appsUnlock.set(i, app);
                }
                mAdapter.notifyDataSetChanged();
            } else {
                for (int i = 0; i < appsUnlock.size(); i++) {
                    AppUnlock app = appsUnlock.get(i);
                    app.active = false;
                    appsUnlock.set(i, app);
                }
                mAdapter.notifyDataSetChanged();
            }
        }
    }

    @Override
    public boolean onQueryTextSubmit(String s) {
        return false;
    }

    @Override
    public boolean onQueryTextChange(String s) {

        if (s == null || s.isEmpty()) {
            if (savedAppsUnlockWhenSearch != null) {
                appsUnlock = savedAppsUnlockWhenSearch;
                savedAppsUnlockWhenSearch = null;
                mAdapter.notifyDataSetChanged();
            }
            return true;
        }

        if (savedAppsUnlockWhenSearch == null) {
            savedAppsUnlockWhenSearch = new ArrayList<>(appsUnlock);
        }

        appsUnlock.clear();

        for (int i = 0; i < savedAppsUnlockWhenSearch.size(); i++) {
            AppUnlock app = savedAppsUnlockWhenSearch.get(i);
            if (app.name.toLowerCase().contains(s.toLowerCase().trim())
                    || app.pack.toLowerCase().contains(s.toLowerCase().trim())) {
                appsUnlock.add(app);
            }
        }
        mAdapter.notifyDataSetChanged();

        return true;
    }

    public class AppUnlock {
        String name;
        String pack;
        String uid;
        Drawable icon;
        boolean system;
        boolean active;

        AppUnlock(String name, String pack, String uid, Drawable icon, boolean system, boolean active) {
            this.name = name;
            this.pack = pack;
            this.uid = uid;
            this.icon = icon;
            this.system = system;
            this.active = active;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            AppUnlock appUnlock = (AppUnlock) o;
            return system == appUnlock.system &&
                    active == appUnlock.active &&
                    name.equals(appUnlock.name) &&
                    pack.equals(appUnlock.pack) &&
                    uid.equals(appUnlock.uid) &&
                    icon.equals(appUnlock.icon);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, pack, uid, icon, system, active);
        }
    }

    public class TorAppsAdapter extends RecyclerView.Adapter<TorAppsAdapter.TorAppsViewHolder> {
        LayoutInflater lInflater = (LayoutInflater) Objects.requireNonNull(getActivity()).getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        TorAppsAdapter() {
        }

        @NonNull
        @Override
        public TorAppsAdapter.TorAppsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int i) {
            View view = lInflater.inflate(R.layout.item_tor_app, parent, false);
            return new TorAppsViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull TorAppsViewHolder torAppsViewHolder, int position) {
            torAppsViewHolder.bind(position);
        }

        @Override
        public int getItemCount() {
            return appsUnlock.size();
        }

        AppUnlock getItem(int position) {
            return appsUnlock.get(position);
        }

        void setActive(int position, boolean active) {
            AppUnlock appUnlock = appsUnlock.get(position);
            appUnlock.active = active;
            appsUnlock.set(position, appUnlock);

            if (savedAppsUnlockWhenSearch != null) {
                for (int i = 0; i < savedAppsUnlockWhenSearch.size(); i++) {
                    AppUnlock appUnlockSaved = savedAppsUnlockWhenSearch.get(i);
                    if (appUnlockSaved.pack.equals(appUnlock.pack)) {
                        savedAppsUnlockWhenSearch.set(i, appUnlock);
                    }
                }
            }
        }

        class TorAppsViewHolder extends RecyclerView.ViewHolder {
            ImageView imgTorApp;
            TextView tvTorAppName;
            TextView tvTorAppPackage;
            Switch swTorApp;
            CardView cardTorApps;

            TorAppsViewHolder(View itemView) {
                super(itemView);

                imgTorApp = itemView.findViewById(R.id.imgTorApp);
                tvTorAppName = itemView.findViewById(R.id.tvTorAppName);
                tvTorAppPackage = itemView.findViewById(R.id.tvTorAppPackage);
                swTorApp = itemView.findViewById(R.id.swTorApp);
                swTorApp.setOnCheckedChangeListener(onCheckedChangeListener);
                swTorApp.setFocusable(false);
                cardTorApps = itemView.findViewById(R.id.cardTorApp);
                cardTorApps.setFocusable(true);
                cardTorApps.setOnClickListener(onClickListener);
                cardTorApps.setOnFocusChangeListener(onFocusChangeListener);
                cardTorApps.setCardBackgroundColor(getResources().getColor(R.color.colorFirst));
            }

            void bind(int position) {
                AppUnlock app = getItem(position);
                tvTorAppName.setText(app.name);
                imgTorApp.setImageDrawable(app.icon);
                tvTorAppPackage.setText(app.pack);
                swTorApp.setChecked(app.active);
                if (app.pack.contains(getString(R.string.package_name))) {
                    swTorApp.setEnabled(false);
                    cardTorApps.setEnabled(false);
                } else {
                    swTorApp.setEnabled(true);
                    cardTorApps.setEnabled(true);
                }
            }

            CompoundButton.OnCheckedChangeListener onCheckedChangeListener = new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean newValue) {
                    setActive(getAdapterPosition(), newValue);
                    isChanged = true;
                }
            };

            View.OnClickListener onClickListener = new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    int appPosition = getAdapterPosition();
                    boolean appActive = getItem(appPosition).active;
                    setActive(appPosition, !appActive);
                    mAdapter.notifyItemChanged(appPosition);
                    isChanged = true;
                }
            };

            View.OnFocusChangeListener onFocusChangeListener = new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View view, boolean inFocus) {
                    if (inFocus) {
                        ((CardView) view).setCardBackgroundColor(getResources().getColor(R.color.colorSecond));
                    } else {
                        ((CardView) view).setCardBackgroundColor(getResources().getColor(R.color.colorFirst));
                    }
                }
            };
        }
    }


    void getDeviceApps(final Context context, final RecyclerView.Adapter adapter, final ArrayList<String> unlockAppsArrListSaved) {

        pbTorApp.setIndeterminate(true);
        pbTorApp.setVisibility(View.VISIBLE);

        final PackageManager pMgr = context.getPackageManager();

        List<ApplicationInfo> lAppInfo = pMgr.getInstalledApplications(0);

        final Iterator<ApplicationInfo> itAppInfo = lAppInfo.iterator();


        Runnable fillAppsList = new Runnable() {
            @Override
            public void run() {

                while (itAppInfo.hasNext()) {
                    ApplicationInfo aInfo = itAppInfo.next();
                    boolean appUseInternet = false;
                    boolean system = false;
                    boolean active = false;

                    try {
                        PackageInfo pInfo = pMgr.getPackageInfo(aInfo.packageName, PackageManager.GET_PERMISSIONS);

                        if (pInfo != null && pInfo.requestedPermissions != null) {
                            for (String permInfo : pInfo.requestedPermissions) {
                                if (permInfo.equals("android.permission.INTERNET")) {
                                    appUseInternet = true;

                                }
                            }

                        }

                    } catch (Exception e) {
                        Log.e(LOG_TAG, "UnlockTorAppsFragment getDeviceApps exception " + e.getMessage() + " " + e.getCause());
                    }

                    if ((aInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 1) {
                        //System app
                        appUseInternet = true;
                        system = true;
                    }


                    if (appUseInternet) {
                        String name;
                        try {
                            name = pMgr.getApplicationLabel(aInfo).toString();
                        } catch (Exception e) {
                            name = aInfo.packageName;
                        }
                        String pack = aInfo.packageName;
                        String uid = String.valueOf(aInfo.uid);
                        Drawable icon = pMgr.getApplicationIcon(aInfo);


                        if (unlockAppsArrListSaved.contains(uid)) {
                            active = true;
                        }

                        final AppUnlock app = new AppUnlock(name, pack, uid, icon, system, active);

                        if (getActivity() == null)
                            return;

                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                appsUnlock.add(app);
                                Collections.sort(appsUnlock, new Comparator<AppUnlock>() {
                                    @Override
                                    public int compare(AppUnlock appUnlock, AppUnlock t1) {
                                        return appUnlock.name.toLowerCase().compareTo(t1.name.toLowerCase());
                                    }
                                });
                                adapter.notifyDataSetChanged();
                            }
                        });

                    }
                }

                if (getActivity() == null) {
                    return;
                }

                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        pbTorApp.setIndeterminate(false);
                        pbTorApp.setVisibility(View.GONE);
                    }
                });
            }
        };
        thread = new Thread(fillAppsList);
        thread.start();

    }

}
