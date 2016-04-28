package io.walther.virtualtouch;

import android.app.DialogFragment;
import android.bluetooth.BluetoothClass;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.punchthrough.bean.sdk.Bean;
import com.punchthrough.bean.sdk.BeanDiscoveryListener;
import com.punchthrough.bean.sdk.BeanManager;

import java.util.ArrayList;
import java.util.List;

import io.walther.virtualtouch.model.HardwareManager;

public class MainActivity extends AppCompatActivity implements DeviceTypeDialogFragment.ListDialogListener {

    final List<Bean> beans = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        TextView appTitleTextView = (TextView) findViewById(R.id.appTitle);
        appTitleTextView.setText(R.string.app_name);

        BeanDiscoveryListener listener = new BeanDiscoveryListener() {
            @Override
            public void onBeanDiscovered(Bean bean, int rssi) {
                beans.add(bean);
            }

            @Override
            public void onDiscoveryComplete() {
                TextView numBeansFoundTextView = (TextView) findViewById(R.id.num_beans_found);
                int numFound = beans.size();
                numBeansFoundTextView.setText(numFound + " bean" + (numFound != 1 ? "s" : "") + " found");
                for (Bean bean : beans) {
                    Log.d("BRENTBRENT", bean.getDevice().getName());
                    Log.d("BRENTBRENT", bean.getDevice().getAddress());
                }

                if (numFound > 0) {
                    Button connectButton = (Button) findViewById(R.id.bean_connect_button);
                    connectButton.setVisibility(View.VISIBLE);
                }
            }
        };

        BeanManager.getInstance().startDiscovery(listener);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void launchSearchActivity(View view) {
        Intent intent = new Intent(this, SearchActivity.class);
        startActivity(intent);
    }

    public void launchLoadActivity(View view) {
        Intent intent = new Intent(this, LoadActivity.class);
        startActivity(intent);
    }

    public void connectToBean(View view) {
        Bundle bundle = new Bundle();
        bundle.putString("device_name", beans.get(0).getDevice().getName());

        DialogFragment newFragment = new DeviceTypeDialogFragment();
        newFragment.setArguments(bundle);
        newFragment.show(getFragmentManager(), "device_type_dialog");
    }

    @Override
    public void onListItemSelected(String choice) {
        if (choice.equals("Cancel")) {
            return;
        }

        HardwareManager.DeviceType type = (choice.equals("Input Device")
                ? HardwareManager.DeviceType.INPUT_DEVICE
                : HardwareManager.DeviceType.OUTPUT_DEVICE);

        HardwareManager.getInstance().connectDevice(
                type,
                this,
                beans.get(0));

        new WaitForBeanConnectTask().execute(HardwareManager.getInstance().getDevice(type));
    }

    private class WaitForBeanConnectTask extends AsyncTask<HardwareManager.Device, Void, Boolean> {

        @Override
        protected void onPreExecute() {
            TextView numBeansFoundTextView = (TextView) findViewById(R.id.num_beans_found);
            numBeansFoundTextView.setText("Connecting...");
        }

        @Override
        protected Boolean doInBackground(HardwareManager.Device... params) {
            HardwareManager.Device device = params[0];
            while(device.isAttemptingConnect()) { /* noop */ }
            return device.isConnected();
        }

        @Override
        protected void onPostExecute(Boolean connectSucceeded) {
            if (connectSucceeded) {
                findViewById(R.id.bean_connect_container).setVisibility(View.GONE);
                findViewById(R.id.bean_connected).setVisibility(View.VISIBLE);
            } else {
                TextView numBeansFoundTextView = (TextView) findViewById(R.id.bean_connected);
                numBeansFoundTextView.setText("Connection failed.");
                numBeansFoundTextView.setVisibility(View.VISIBLE);
            }
        }
    }
}
