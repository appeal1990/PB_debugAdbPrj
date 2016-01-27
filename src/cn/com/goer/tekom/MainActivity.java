package cn.com.goer.tekom;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.NetworkUtils;
import android.net.NetworkInfo.State;
import android.net.wifi.IWifiManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
/**
 * Description:
 *
 * @package : com.goer.tekom / PB_debugAdbPrj
 * @author : jaime
 * @email : appeal1990@hotmail.com
 * @since : 2016-01-27
 */
public class MainActivity extends Activity {

    private static final String TAG = MainActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 加载PrefFragment
        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new AdbSettingFragment())
                .addToBackStack(null)
                .commit();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter();
        filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        filter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        filter.addAction(Intent.ACTION_BOOT_COMPLETED);
        registerReceiver(wifiBroadcastReceiver, filter);

        boolean isAdbWifiChecked = Settings.Global.getInt(this.getContentResolver(), AdbSettingFragment.ADB_WIFI_ENABLED_KEY_PORT, 0) != 0;
        AdbSettingFragment.mEnableWifiAdb.setChecked(isAdbWifiChecked);
        Log.i(TAG, "isAdbWifiChecked:" + isAdbWifiChecked);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(wifiBroadcastReceiver);
    }

    BroadcastReceiver wifiBroadcastReceiver = new BroadcastReceiver() {
        @SuppressWarnings("deprecation")
        @Override
        public void onReceive(Context context, Intent intent) {
            String TAG = "wifiBroadcastReceiver";
            WifiInfo wifiInfo = null;
            int port = Settings.Global.getInt(context.getContentResolver(), AdbSettingFragment.ADB_WIFI_ENABLED_KEY_PORT, 0);
            boolean isAdbWifiChecked = AdbSettingFragment.mEnableWifiAdb.isChecked();
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo net = cm.getActiveNetworkInfo();
            if (net == null) {
                Log.i(TAG, "No net type");
                if (isAdbWifiChecked) {
                    AdbSettingFragment.mEnableWifiAdb.setChecked(false);
                    AdbSettingFragment.mEnableWifiAdb.setSummary(getResources().getString(R.string.enable_wifi_adb_openwifi));
                }
            } else {
                Log.i(TAG, "Net Type:" + net.getTypeName());
            }
            State wifi = cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI).getState();
            if (wifi == State.CONNECTED || wifi == State.CONNECTING) {
                Log.i(TAG, "wifi connected");
                IWifiManager wifiManager = IWifiManager.Stub.asInterface(ServiceManager.getService(Context.WIFI_SERVICE));
                try {
                    wifiInfo = wifiManager.getConnectionInfo();
                } catch (RemoteException e) {
                    Log.e(TAG, "wifiManager, getConnectionInfo()", e);
                }
                String ipAddressString = NetworkUtils.intToInetAddress(wifiInfo.getIpAddress()).getHostAddress();
                if (isAdbWifiChecked) {
                    if ("0.0.0.0".equals(ipAddressString)) {
                        AdbSettingFragment.mEnableWifiAdb.setChecked(false);
                        AdbSettingFragment.mEnableWifiAdb.setSummary(getResources().getString(R.string.enable_wifi_adb_openwifi));
                    } else {
                        AdbSettingFragment.mEnableWifiAdb.setSummary(getResources().getString(R.string.enable_wifi_adb_connected_summary, ipAddressString));
                    }
                }
                Log.i(TAG, getResources().getString(R.string.enable_wifi_adb_connected_summary) + "   " + ipAddressString + String.valueOf(port));
            } else if (wifi == State.DISCONNECTED || wifi == State.DISCONNECTING) {
                Log.i(TAG, "wifi disconnected");
                if (isAdbWifiChecked) {
                    AdbSettingFragment.mEnableWifiAdb.setChecked(false);
                    AdbSettingFragment.mEnableWifiAdb.setSummary(getResources().getString(R.string.enable_wifi_adb_openwifi));
                }
                Log.i(TAG, getResources().getString(R.string.enable_wifi_adb_connected_summary));
            }

        }
    };
}
