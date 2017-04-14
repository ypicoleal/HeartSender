package com.github.ypicoleal.heartsender;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.github.javiersantos.materialstyleddialogs.MaterialStyledDialog;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;

import java.util.ArrayList;
import java.util.Locale;

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class MainActivity extends AppCompatActivity implements DevicesAdapter.ListItemClickListener, HeartGattCallback.OnDeviceCallback {

    private static final int REQUEST_ENABLE_BT = 1;
    // Stops scanning after 10 seconds.
    private static final long SCAN_PERIOD = 10000;
    private LineChart mChart;
    private BluetoothAdapter mBluetoothAdapter;
    private ArrayList<BluetoothDevice> devices;
    private ArrayList<String> devicesName;
    private DevicesAdapter mDevicesAdapter;
    private ProgressBar mProgressBar;
    private MaterialStyledDialog devicesDialog;
    //private LineChart mChart;
    private TextView mCurrentValue;
    private BluetoothDevice device;
    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(final BluetoothDevice device, int rssi,
                             byte[] scanRecord) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(MainActivity.this, device.getName(), Toast.LENGTH_LONG).show();
                    devices.add(device);
                    devicesName.add(device.getName());
                    mDevicesAdapter.setDevices(devicesName);
                }
            });
        }
    };
    private ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            Toast.makeText(MainActivity.this, result.getDevice().getName(), Toast.LENGTH_LONG).show();
            //result.getDevice().connectGatt()
            devices.add(result.getDevice());
            devicesName.add(result.getDevice().getName());
            mDevicesAdapter.setDevices(devicesName);
        }

        @Override
        public void onScanFailed(int errorCode) {
            Toast.makeText(MainActivity.this, "error: " + String.valueOf(errorCode), Toast.LENGTH_LONG).show();
        }
    };
    private BluetoothGattCallback gattCallBack = new HeartGattCallback(this);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        setChart();

        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(MainActivity.this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            finish();
        } else {
            if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }
        }
    }

    void setChart() {
        mCurrentValue = (TextView) findViewById(R.id.current_value);
        mChart = (LineChart) findViewById(R.id.chart1);
        mChart.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {
            @Override
            public void onValueSelected(Entry e, Highlight h) {

            }

            @Override
            public void onNothingSelected() {

            }
        });
        mChart.setDrawGridBackground(false);
        mChart.getDescription().setEnabled(false);

        // add an empty data object
        mChart.setData(new LineData());
        mChart.getXAxis().setDrawLabels(false);
        mChart.getXAxis().setDrawGridLines(false);

        mChart.invalidate();

    }

    public void tryReconnect(View view) {
        device.connectGatt(this, false, gattCallBack);
        devicesDialog.dismiss();
        setDeviceStatus(HeartGattCallback.STATUS_CONNECTING);
        setDeviceName();
        view.setEnabled(false);
    }

    private void setHeartRate(final float yValue) {

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (yValue > 2) {
                    mCurrentValue.setText(String.format(Locale.US, "%.1f ppm", yValue));
                }
                addEntry(yValue);
            }
        });
    }

    private void addEntry(float yValue) {

        LineData data = mChart.getData();

        ILineDataSet set = data.getDataSetByIndex(0);
        // set.addEntry(...); // can be called as well

        if (set == null) {
            set = createSet();
            data.addDataSet(set);
        }

        // choose a random dataSet
        int randomDataSetIndex = (int) (Math.random() * data.getDataSetCount());

        data.addEntry(new Entry(data.getDataSetByIndex(randomDataSetIndex).getEntryCount(), yValue), randomDataSetIndex);
        data.notifyDataChanged();

        // let the chart know it's data has changed
        mChart.notifyDataSetChanged();

        mChart.setVisibleXRangeMaximum(2000);
        //mChart.setVisibleYRangeMaximum(15, AxisDependency.LEFT);
//
//            // this automatically refreshes the chart (calls invalidate())
        mChart.moveViewTo(data.getEntryCount() - 7, 50f, YAxis.AxisDependency.LEFT);

    }

    private LineDataSet createSet() {

        LineDataSet set = new LineDataSet(null, "DataSet 1");
        set.setLineWidth(2.5f);
        set.setCircleRadius(4.5f);
        set.setColor(Color.rgb(240, 99, 99));
        set.setCircleColor(Color.rgb(240, 99, 99));
        set.setHighLightColor(Color.rgb(190, 190, 190));
        set.setAxisDependency(YAxis.AxisDependency.LEFT);
        set.setValueTextSize(10f);
        set.setDrawCircles(false);
        return set;
    }

    void setSensorLocation(final int sensorLocation) {
        MainActivity.this.runOnUiThread(new Runnable() {

            @Override
            public void run() {
                ImageView sensorIcon = (ImageView) findViewById(R.id.sensor_icon);
                TextView sensorLocationTV = (TextView) findViewById(R.id.sensor_location);
                if (sensorLocation == 0) {
                    sensorIcon.setImageResource(R.drawable.ic_favorite);
                    sensorLocationTV.setText(R.string.sensor_other);
                } else if (sensorLocation == 1) {
                    sensorIcon.setImageResource(R.drawable.ic_chest);
                    sensorLocationTV.setText(R.string.sensor_chest);
                } else if (sensorLocation == 2) {
                    sensorIcon.setImageResource(R.drawable.ic_wrist);
                    sensorLocationTV.setText(R.string.sensor_wrist);
                } else if (sensorLocation == 3) {
                    sensorIcon.setImageResource(R.drawable.ic_finger);
                    sensorLocationTV.setText(R.string.sensor_finger);
                } else if (sensorLocation == 4) {
                    sensorIcon.setImageResource(R.drawable.ic_hand);
                    sensorLocationTV.setText(R.string.sensor_hand);
                } else if (sensorLocation == 5) {
                    sensorIcon.setImageResource(R.drawable.ic_ear);
                    sensorLocationTV.setText(R.string.sensor_ear);
                } else if (sensorLocation == 6) {
                    sensorIcon.setImageResource(R.drawable.ic_foot);
                    sensorLocationTV.setText(R.string.sensor_foot);
                }
            }
        });
    }

    void setBatteryLevel(final int batteryLevel) {
        Log.i("Battery level", "" + batteryLevel);
        final TextView batteryStatus = (TextView) findViewById(R.id.batery_status);
        final ImageView batteryIcon = (ImageView) findViewById(R.id.batery_status_icon);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (batteryLevel == HeartGattCallback.BATTERY_READDING) {
                    batteryStatus.setText(R.string.reading);
                    batteryIcon.setImageResource(R.drawable.ic_battery_unknown_white_24dp);
                } else if (batteryLevel == HeartGattCallback.BATTERY_UNKNOWN) {
                    batteryStatus.setText(R.string.unknown);
                    batteryIcon.setImageResource(R.drawable.ic_battery_unknown_white_24dp);
                } else {
                    batteryStatus.setText(batteryLevel + "%");
                    int icon;
                    if (batteryLevel < 20) {
                        icon = R.drawable.ic_battery_alert_black_24dp;
                    } else if (batteryLevel < 30) {
                        icon = R.drawable.ic_battery_20_black_24dp;
                    } else if (batteryLevel < 50) {
                        icon = R.drawable.ic_battery_30_black_24dp;
                    } else if (batteryLevel < 60) {
                        icon = R.drawable.ic_battery_50_black_24dp;
                    } else if (batteryLevel < 80) {
                        icon = R.drawable.ic_battery_60_black_24dp;
                    } else if (batteryLevel < 90) {
                        icon = R.drawable.ic_battery_80_black_24dp;
                    } else if (batteryLevel < 100) {
                        icon = R.drawable.ic_battery_90_black_24dp;
                    } else {
                        icon = R.drawable.ic_battery_full_black_24dp;
                    }
                    batteryIcon.setImageResource(icon);
                }
            }
        });
    }

    private void scanLeDevice(final boolean enable) {
        devices = new ArrayList<>();
        devicesName = new ArrayList<>();
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            final BluetoothLeScanner scanner = mBluetoothAdapter.getBluetoothLeScanner();
            if (enable) {
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            scanner.stopScan(scanCallback);
                            mProgressBar.setVisibility(View.GONE);
                            Toast.makeText(MainActivity.this, "scan stop", Toast.LENGTH_LONG).show();
                        }
                    }
                }, SCAN_PERIOD);
                scanner.startScan(scanCallback);
                Toast.makeText(MainActivity.this, "scan start", Toast.LENGTH_LONG).show();
            } else {
                scanner.stopScan(scanCallback);
                mProgressBar.setVisibility(View.GONE);
                Toast.makeText(MainActivity.this, "scan stop", Toast.LENGTH_LONG).show();
            }
        } else {
            if (enable) {
                // Stops scanning after a pre-defined scan period.
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mBluetoothAdapter.stopLeScan(mLeScanCallback);
                    }
                }, SCAN_PERIOD);

                mBluetoothAdapter.startLeScan(mLeScanCallback);
            } else {
                mBluetoothAdapter.stopLeScan(mLeScanCallback);
            }
        }
    }

    private void setDeviceName() {
        TextView deviceName = (TextView) findViewById(R.id.device_name);
        deviceName.setText(device.getName());
    }

    private void setDeviceStatus(final int deviceStatus) {
        runOnUiThread(new Runnable() {

            @Override
            public void run() {
                TextView deviceStatusTV = (TextView) findViewById(R.id.device_status);
                ImageView deviceStatusIcon = (ImageView) findViewById(R.id.device_status_icon);
                switch (deviceStatus) {
                    case HeartGattCallback.STATUS_CONNECTING:
                        deviceStatusTV.setText(R.string.connecting_state);
                        deviceStatusIcon.setImageResource(R.drawable.ic_bluetooth_connecting_black_24dp);
                        break;
                    case HeartGattCallback.STATUS_DISCOVERING:
                        deviceStatusTV.setText(R.string.status_discovering);
                        deviceStatusIcon.setImageResource(R.drawable.ic_settings_bluetooth_white_24dp);
                        break;
                    case HeartGattCallback.STATUS_CONNECTED:
                        deviceStatusTV.setText(R.string.status_connected);
                        deviceStatusIcon.setImageResource(R.drawable.ic_bluetooth_connected_black_24dp);
                        break;
                    case HeartGattCallback.STATUS_DISCONNECTED:
                        deviceStatusTV.setText(R.string.status_disconected);
                        deviceStatusIcon.setImageResource(R.drawable.ic_bluetooth_disabled_black_24dp);
                        Button recconect = (Button) findViewById(R.id.reconnect_btn);
                        recconect.setEnabled(true);
                        break;
                    default:
                        deviceStatusTV.setText(R.string.unknown);
                        deviceStatusIcon.setImageResource(R.drawable.ic_bluetooth_white_24dp);
                        break;
                }
            }
        });
    }

    @Override
    public void onListItemClick(int position) {
        device = devices.get(position);
        device.connectGatt(this, false, gattCallBack);
        devicesDialog.dismiss();
        setDeviceStatus(HeartGattCallback.STATUS_CONNECTING);
        setDeviceName();
    }

    @Override
    public void onBatteryLevelChange(int batteryLevel) {
        setBatteryLevel(batteryLevel);
    }

    @Override
    public void onDeviceStatusChange(int status) {
        setDeviceStatus(status);
    }

    @Override
    public void onHeartRateChange(int heartRate) {
        setHeartRate(heartRate);
    }

    @Override
    public void onSensorLocationChange(int sensorLocation) {
        setSensorLocation(sensorLocation);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_search) {
            LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View customView = inflater.inflate(R.layout.devices, null);

            RecyclerView mDevicesRV = (RecyclerView) customView.findViewById(R.id.devices_rv);
            RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(MainActivity.this);
            mDevicesRV.setLayoutManager(layoutManager);
            mDevicesRV.setHasFixedSize(true);
            mDevicesAdapter = new DevicesAdapter(MainActivity.this);
            mDevicesRV.setAdapter(mDevicesAdapter);

            mProgressBar = (ProgressBar) customView.findViewById(R.id.progress_bar);
            devicesDialog = new MaterialStyledDialog.Builder(MainActivity.this)
                    .setTitle("Dispositivos BLE")
                    .setIcon(R.drawable.ic_bluetooth_white_24dp)
                    .setCustomView(customView, 16, 16, 16, 0)
                    .show();
            scanLeDevice(true);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
