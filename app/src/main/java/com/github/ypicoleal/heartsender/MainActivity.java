package com.github.ypicoleal.heartsender;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.androidplot.util.Redrawer;
import com.androidplot.xy.AdvancedLineAndPointRenderer;
import com.androidplot.xy.BoundaryMode;
import com.androidplot.xy.XYPlot;
import com.androidplot.xy.XYSeries;
import com.github.javiersantos.materialstyleddialogs.MaterialStyledDialog;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class MainActivity extends AppCompatActivity implements OnChartValueSelectedListener, DevicesAdapter.ListItemClickListener {

    private static final int REQUEST_ENABLE_BT = 1;
    // Stops scanning after 10 seconds.
    private static final long SCAN_PERIOD = 10000;
    private BluetoothAdapter mBluetoothAdapter;
    private ArrayList<BluetoothDevice> devices;
    private ArrayList<String> devicesName;
    private DevicesAdapter mDevicesAdapter;
    private ProgressBar mProgressBar;
    private MaterialStyledDialog devicesDialog;
    //private LineChart mChart;
    private TextView mCurrentValue;
    private XYPlot plot;
    private Redrawer redrawer;
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
    private BluetoothGattCallback gattCallBack = new BluetoothGattCallback() {
        final UUID HEART_RATE = UUID.fromString("00002A37-0000-1000-8000-00805f9b34fb");
        final UUID BODY_SENSOR = UUID.fromString("00002A38-0000-1000-8000-00805f9b34fb");

        void enableNotifications(BluetoothGatt gatt) {
            Log.i("gatt", "enableNotifications");

            gatt.readRemoteRssi();
            List<BluetoothGattService> services = gatt.getServices();
            for (BluetoothGattService service : services) {
                Log.i("gatt", "Service UUID: " + service.getUuid());
                List<BluetoothGattCharacteristic> characteristics = service.getCharacteristics();
                for (BluetoothGattCharacteristic characteristic : characteristics) {
                    Log.i("gatt", "Characteristic UUID: " + characteristic.getUuid());
                    if (characteristic.getUuid().compareTo(HEART_RATE) == 0) {
                        Log.i("gatt", "you are the chosen one");
                        gatt.setCharacteristicNotification(characteristic, true);
                        gatt.readCharacteristic(characteristic);
                    } else if (characteristic.getUuid().compareTo(BODY_SENSOR) == 0) {
                        gatt.readCharacteristic(characteristic);
                    }
                    //
                }
            }
        }

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            Log.i("gatt", "Status change: " + status + " " + newState);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                gatt.discoverServices();
            } else if (status == BluetoothGatt.GATT_CONNECTION_CONGESTED) {
                Log.i("gatt", "Error: " + status + " " + newState);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            Log.w("Gatt", "onServicesDiscovered received: " + status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                enableNotifications(gatt);
            } else {
                Log.w("Gatt", "error onServicesDiscovered received: " + status);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                byte[] characteristicValue = characteristic.getValue();
                for (byte singleByte : characteristicValue) {
                    Log.i("gatt", "onCharacteristicWrite: " + singleByte + " length: " + characteristicValue.length);
                }
                if (characteristic.getUuid().compareTo(HEART_RATE) == 0) {
                    addEntry(characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_SINT8, 0));
                } else {
                    Log.i("gatt", "sensor location: " + characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_SINT8, 0) + " length: " + characteristicValue.length);
                    setSensorIcon(characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_SINT8, 0));
                }

            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
            Log.i("gatt", "onCharacteristicWrite status: " + status);
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            byte[] characteristicValue = characteristic.getValue();
            for (byte singleByte : characteristicValue) {
                Log.i("gatt", "single byte: " + singleByte + " length: " + characteristicValue.length);
            }
            addEntry(characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_SINT8, 1));
        }

        @Override
        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorRead(gatt, descriptor, status);
            Log.i("gatt", "onDescriptorRead: ");
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorWrite(gatt, descriptor, status);
            Log.i("gatt", "onDescriptorWrite: ");
        }

        @Override
        public void onReliableWriteCompleted(BluetoothGatt gatt, int status) {
            super.onReliableWriteCompleted(gatt, status);
            Log.i("gatt", "onDescriptorWrite: ");
        }

        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
            super.onReadRemoteRssi(gatt, rssi, status);
            Log.i("gatt", "onReadRemoteRssi: ");
        }

        @Override
        public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
            super.onMtuChanged(gatt, mtu, status);
            Log.i("gatt", "onMtuChanged: ");
        }
    };

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

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

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
            }
        });
    }

    void setChart() {
        mCurrentValue = (TextView) findViewById(R.id.current_value);
        plot = (XYPlot) findViewById(R.id.plot);

        ECGModel ecgSeries = new ECGModel(2000, 200);

        // add a new series' to the xyplot:
        MyFadeFormatter formatter = new MyFadeFormatter(500);
        formatter.setLegendIconEnabled(false);
        plot.addSeries(ecgSeries, formatter);
        plot.setRangeBoundaries(0, 10, BoundaryMode.FIXED);
        plot.setDomainBoundaries(0, 2000, BoundaryMode.FIXED);

        // reduce the number of range labels
        plot.setLinesPerRangeLabel(3);

        // start generating ecg data in the background:
        ecgSeries.start(new WeakReference<>(plot.getRenderer(AdvancedLineAndPointRenderer.class)));

        // set a redraw rate of 30hz and start immediately:
        redrawer = new Redrawer(plot, 30, true);
        /*mChart = (LineChart) findViewById(R.id.chart1);
        mChart.setOnChartValueSelectedListener(this);
        mChart.setDrawGridBackground(false);
        mChart.getDescription().setEnabled(false);

        // add an empty data object
        mChart.setData(new LineData());
//        mChart.getXAxis().setDrawLabels(false);
//        mChart.getXAxis().setDrawGridLines(false);

        mChart.invalidate();*/
    }

    private void addEntry(final float yValue) {

        /*final LineData data = mChart.getData();

        ILineDataSet set = data.getDataSetByIndex(0);
        // set.addEntry(...); // can be called as well

        if (set == null) {
            set = createSet();
            data.addDataSet(set);
        }

        MainActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mCurrentValue.setText(String.format(Locale.US, "%.1f ppm", yValue));

                data.addEntry(new Entry(data.getDataSetByIndex(0).getEntryCount(), yValue), 0);
                data.notifyDataChanged();

                // let the chart know it's data has changed
                mChart.notifyDataSetChanged();

                mChart.setVisibleXRangeMaximum(6);
                //mChart.setVisibleYRangeMaximum(15, AxisDependency.LEFT);
//
//            // this automatically refreshes the chart (calls invalidate())
                mChart.moveViewTo(data.getEntryCount() - 7, 50f, YAxis.AxisDependency.LEFT);
            }
        });*/
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

        return set;
    }

    void setSensorIcon(final int sensor_location) {
        MainActivity.this.runOnUiThread(new Runnable() {

            @Override
            public void run() {
                ImageView sensor_icon = (ImageView) findViewById(R.id.sensor_icon);
                if (sensor_location == 0) {
                    sensor_icon.setImageResource(R.drawable.ic_favorite);
                } else if (sensor_location == 1) {
                    sensor_icon.setImageResource(R.drawable.ic_chest);
                } else if (sensor_location == 2) {
                    sensor_icon.setImageResource(R.drawable.ic_wrist);
                } else if (sensor_location == 3) {
                    sensor_icon.setImageResource(R.drawable.ic_finger);
                } else if (sensor_location == 4) {
                    sensor_icon.setImageResource(R.drawable.ic_hand);
                } else if (sensor_location == 5) {
                    sensor_icon.setImageResource(R.drawable.ic_ear);
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

    @Override
    public void onValueSelected(Entry e, Highlight h) {

    }

    @Override
    public void onNothingSelected() {

    }

    @Override
    public void onListItemClick(int position) {
        BluetoothDevice device = devices.get(position);
        device.connectGatt(this, false, gattCallBack);
        devicesDialog.dismiss();
    }

    public static class MyFadeFormatter extends AdvancedLineAndPointRenderer.Formatter {

        private int trailSize;

        public MyFadeFormatter(int trailSize) {
            this.trailSize = trailSize;
        }

        @Override
        public Paint getLinePaint(int thisIndex, int latestIndex, int seriesSize) {
            // offset from the latest index:
            int offset;
            if (thisIndex > latestIndex) {
                offset = latestIndex + (seriesSize - thisIndex);
            } else {
                offset = latestIndex - thisIndex;
            }

            float scale = 255f / trailSize;
            int alpha = (int) (255 - (offset * scale));
            getLinePaint().setAlpha(alpha > 0 ? alpha : 0);
            return getLinePaint();
        }
    }

    /**
     * Primitive simulation of some kind of signal.  For this example,
     * we'll pretend its an ecg.  This class represents the data as a circular buffer;
     * data is added sequentially from left to right.  When the end of the buffer is reached,
     * i is reset back to 0 and simulated sampling continues.
     */
    public static class ECGModel implements XYSeries {

        private final Number[] data;
        private final long delayMs;
        private final int blipInteral;
        private final Thread thread;
        private boolean keepRunning;
        private int latestIndex;

        private WeakReference<AdvancedLineAndPointRenderer> rendererRef;

        /**
         * @param size         Sample size contained within this model
         * @param updateFreqHz Frequency at which new samples are added to the model
         */
        public ECGModel(int size, int updateFreqHz) {
            data = new Number[size];
            for (int i = 0; i < data.length; i++) {
                data[i] = 0;
            }

            // translate hz into delay (ms):
            delayMs = 1000 / updateFreqHz;

            // add 7 "blips" into the signal:
            blipInteral = size / 7;

            thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        while (keepRunning) {
                            if (latestIndex >= data.length) {
                                latestIndex = 0;
                            }

                            // generate some random data:
                            if (latestIndex % blipInteral == 0) {
                                // insert a "blip" to simulate a heartbeat:
                                data[latestIndex] = (Math.random() * 10) + 3;
                            } else {
                                // insert a random sample:
                                data[latestIndex] = Math.random() * 2;
                            }

                            if (latestIndex < data.length - 1) {
                                // null out the point immediately following i, to disable
                                // connecting i and i+1 with a line:
                                data[latestIndex + 1] = null;
                            }

                            if (rendererRef.get() != null) {
                                rendererRef.get().setLatestIndex(latestIndex);
                                Thread.sleep(delayMs);
                            } else {
                                keepRunning = false;
                            }
                            latestIndex++;
                        }
                    } catch (InterruptedException e) {
                        keepRunning = false;
                    }
                }
            });
        }

        public void start(final WeakReference<AdvancedLineAndPointRenderer> rendererRef) {
            this.rendererRef = rendererRef;
            keepRunning = true;
            thread.start();
        }

        @Override
        public int size() {
            return data.length;
        }

        @Override
        public Number getX(int index) {
            return index;
        }

        @Override
        public Number getY(int index) {
            return data[index];
        }

        @Override
        public String getTitle() {
            return "Signal";
        }
    }
}
