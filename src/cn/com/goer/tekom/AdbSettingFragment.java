package cn.com.goer.tekom;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnDismissListener;
import android.net.NetworkUtils;
import android.net.wifi.IWifiManager;
import android.net.wifi.WifiInfo;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.os.Vibrator;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.provider.Settings;
import android.util.Log;

public class AdbSettingFragment extends PreferenceFragment implements OnClickListener, OnDismissListener {

    private static final String TAG = AdbSettingFragment.class.getSimpleName();
    public static final String ADB_WIFI_ENABLED_KEY_PORT = "ADB_WIFI_ENABLED_PORT";
    private static final String ENABLE_WIFI_ADB = "enable_wifi_adb";
    public static SwitchPreference mEnableWifiAdb;   //wifi调试
    private Dialog mAdbTcpDialog;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.wifi_setprf);
        mEnableWifiAdb = (SwitchPreference) findPreference(ENABLE_WIFI_ADB);
        if (mEnableWifiAdb == null) {
            throw new IllegalArgumentException("Cannot find preference with key = " + "apply_wifi_adb");
        }
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen,
                                         Preference preference) {
        if (mEnableWifiAdb.isChecked()) {
            if (mAdbTcpDialog != null) {
                if (mAdbTcpDialog != null) {
                    mAdbTcpDialog.dismiss();
                    mAdbTcpDialog = null;
                }
            }
            mAdbTcpDialog = new AlertDialog.Builder(getActivity()).setMessage(
                    getResources().getString(R.string.adb_over_network_warning))
                    .setTitle(R.string.adb_over_network)
                    .setPositiveButton(android.R.string.yes, this)
                    .setNegativeButton(android.R.string.no, this)
                    .show();
            mAdbTcpDialog.setOnDismissListener(this);
        } else {
            Settings.Global.putInt(getActivity().getContentResolver(), ADB_WIFI_ENABLED_KEY_PORT, 0);
            updateAdbOverNetwork();
        }
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    private void updateAdbOverNetwork() {
        int adbenable = Settings.Global.getInt(getActivity().getContentResolver(), Settings.Global.ADB_ENABLED, 0);
        if (adbenable != 0) { //判断是否开启USB调试
            int port = Settings.Global.getInt(getActivity().getContentResolver(), ADB_WIFI_ENABLED_KEY_PORT, 0);
            Log.d(TAG, "  port = " + port);
            boolean enabled = port > 0;
            mEnableWifiAdb.setChecked(enabled);
            Log.d(TAG, "  enable = " + enabled);
            if (enabled) {//判断是否设置了端口，如果是，则wifi调试可用
                WifiInfo wifiInfo = null;
                Settings.Global.putInt(getActivity().getContentResolver(), ADB_WIFI_ENABLED_KEY_PORT, 5555);
                SystemProperties.set("sys.connect.adb.wifi", "1");
                IWifiManager wifiManager = IWifiManager.Stub.asInterface(ServiceManager.getService(Context.WIFI_SERVICE));
                try {
                    wifiInfo = wifiManager.getConnectionInfo();
                } catch (RemoteException e) {
                    Log.e(TAG, "wifiManager, getConnectionInfo()", e);
                }
                String ipAddressString = NetworkUtils.intToInetAddress(wifiInfo.getIpAddress()).getHostAddress();
                Log.i(TAG, "ipAddressString=" + ipAddressString);
                if ("0.0.0.0".equals(ipAddressString)) {//判断wifi是否已连接，否则连接wifi后再调试
                    mEnableWifiAdb.setChecked(false);
                    mEnableWifiAdb.setSummary(getResources().getString(R.string.enable_wifi_adb_openwifi));
                } else {
                    Vibrator vib = (Vibrator) getActivity().getSystemService(Context.VIBRATOR_SERVICE);
                    vib.vibrate(50);
                    mEnableWifiAdb.setSummary(getResources().getString(R.string.enable_wifi_adb_connected_summary, ipAddressString));
                }
            } else { //否则wifi调试不可用
                Settings.Global.putInt(getActivity().getContentResolver(), ADB_WIFI_ENABLED_KEY_PORT, 0);
                SystemProperties.set("sys.connect.adb.wifi", "0");
                mEnableWifiAdb.setSummary(getResources().getString(R.string.enable_wifi_adb_summary));
            }
        } else { //未开启则不开启wifi调试
            mEnableWifiAdb.setChecked(false);
            mEnableWifiAdb.setSummary(getResources().getString(R.string.enable_wifi_adb_openusb));
        }
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        if (which == DialogInterface.BUTTON_POSITIVE) {
            int enable = Settings.Global.getInt(getActivity().getContentResolver(), Settings.Global.ADB_ENABLED, 0);
            if (enable != 0) { //判断是否开启USB调试，未开启则不开启wifi调试
                Settings.Global.putInt(getActivity().getContentResolver(), Settings.Global.ADB_ENABLED, 1);
                Settings.Global.putInt(getActivity().getContentResolver(), ADB_WIFI_ENABLED_KEY_PORT, 5555);
            } else {
                mEnableWifiAdb.setChecked(false);
                mEnableWifiAdb.setSummary(getResources().getString(R.string.enable_wifi_adb_openusb));
            }
        }
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        updateAdbOverNetwork();
        mAdbTcpDialog = null;
    }


}
