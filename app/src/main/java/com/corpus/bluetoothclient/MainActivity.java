package com.corpus.bluetoothclient;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatEditText;
import android.support.v7.widget.AppCompatImageButton;
import android.support.v7.widget.AppCompatTextView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SwitchCompat;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Set;

public class MainActivity extends AppCompatActivity implements ConnectionListener{


    private static final int BLUETOOTH_REQUEST = 1001;
    private static final int LOCATION_REQUEST = 1002;
    DevicesAdapter devicesAdapter;
    private BluetoothAdapter bluetoothAdapter;
    private ArrayList<Object> devices = new ArrayList<>();
    private AppCompatTextView status;
    private ConnectThread thread = null;
    private AppCompatEditText message;
    private AppCompatImageButton send;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        message = findViewById(R.id.message);
        send = findViewById(R.id.buttonSend);
        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(thread!=null && !TextUtils.isEmpty(message.getText().toString())){
                    thread.write(message.getText().toString());
                }

                message.setText("");
            }
        });
        status = findViewById(R.id.status);

        registerBroadCastReceiver();

        initBluetoothAdapter();


        SwitchCompat bluetoothButton = findViewById(R.id.button);
        bluetoothButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    enableBlueTooth();
                }else{
                    disableBluetooth();
                }

            }
        });
        bluetoothButton.setChecked(bluetoothAdapter.isEnabled());
        status.setText(String.format(getString(R.string.bluetooth_on_off), bluetoothAdapter.isEnabled() ? "On" : "Off"));

        setUpRecyclerView();

        getLocationPermission();



    }

    @Override
    protected void onStart() {
        super.onStart();
        getDevices();
    }

    private void disableBluetooth() {
        if(bluetoothAdapter!=null && bluetoothAdapter.isEnabled()){
            bluetoothAdapter.disable();
        }
    }

    private void registerBroadCastReceiver() {
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(mReceiver, filter);

    }

    private void getLocationPermission() {
        int status = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION);
        if(status != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, LOCATION_REQUEST);
        }

    }



    public void initBluetoothAdapter(){
        if(Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN_MR1) {
            BluetoothManager manager = (BluetoothManager) getApplicationContext().getSystemService(BLUETOOTH_SERVICE);
            if(manager !=null) {
                bluetoothAdapter = manager.getAdapter();
            }
        }else{
            bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        }


    }

    @Override
    protected void onPause() {
        super.onPause();
        cancelDisCovery();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Don't forget to unregister the ACTION_FOUND receiver.
        unregisterReceiver(mReceiver);
    }

    private void getDevices() {

        if(bluetoothAdapter.isEnabled()){
            getPairedDevices();
            startDiscovery();
        }else{
           enableBlueTooth();
           getPairedDevices();
        }
    }


    private void cancelDisCovery() {
        if(bluetoothAdapter!=null){
            bluetoothAdapter.cancelDiscovery();
        }
    }

    private void startDiscovery() {
        if(bluetoothAdapter!=null){
            bluetoothAdapter.cancelDiscovery();
            bluetoothAdapter.startDiscovery();
        }
    }

    private void getPairedDevices() {
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();

        if (!pairedDevices.isEmpty()) {
            devices.addAll(pairedDevices);
            devicesAdapter.notifyDataSetChanged();
        }
    }

    private void enableBlueTooth() {

        if(!bluetoothAdapter.isEnabled()){
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(intent, BLUETOOTH_REQUEST);
        }

    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == BLUETOOTH_REQUEST){
            getDevices();
        }else if(requestCode == LOCATION_REQUEST){
            getLocationPermission();
        }
    }

    private void setUpRecyclerView() {
        RecyclerView recyclerView = findViewById(R.id.devicesRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this,LinearLayoutManager.VERTICAL, false));
        devicesAdapter = new DevicesAdapter(devices);
        recyclerView.setAdapter(devicesAdapter);
    }


    // Create a BroadcastReceiver for ACTION_FOUND.
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {

        public void onReceive(Context context, Intent intent) {
            Log.e("Onreceiev","");
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Discovery has found a device. Get the BluetoothDevice
                // object and its info from the Intent.
                if(devices.contains(getString(R.string.no_devices_available))){
                    devices.remove(getString(R.string.no_devices_available));
                }
                if(devices.contains(getString(R.string.discovering))){
                    devices.remove(getString(R.string.discovering));
                }
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if(device.getBondState() != BluetoothDevice.BOND_BONDED) {
                    devices.add(device);
                }
                devicesAdapter.notifyDataSetChanged();
            }else if(BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)){
                if(devices.contains(getString(R.string.discovering))){
                    devices.remove(getString(R.string.discovering));
                }
                if(devices.isEmpty()){
                    devices.add(getString(R.string.no_devices_available));
                }
                devicesAdapter.notifyDataSetChanged();
            }else if(BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)){
                devices.clear();
                devices.add(getString(R.string.discovering));
                devicesAdapter.notifyDataSetChanged();
            } else if(BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)){
                int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_OFF);
                if(state == BluetoothAdapter.STATE_ON) {
                    status.setText(String.format(getString(R.string.bluetooth_on_off), "On"));
                }else if(state == BluetoothAdapter.STATE_OFF){
                    status.setText(String.format(getString(R.string.bluetooth_on_off), "Off"));
                    devices.clear();
                    devicesAdapter.notifyDataSetChanged();
                }else if(state == BluetoothAdapter.STATE_TURNING_OFF){
                    status.setText(String.format(getString(R.string.bluetooth_on_off), "Turning Off"));
                }else if(state == BluetoothAdapter.STATE_TURNING_ON){
                    status.setText(String.format(getString(R.string.bluetooth_on_off), "Turning On"));
                }
            }
        }
    };

    @Override
    public void onConnected() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                send.setVisibility(View.VISIBLE);
            }
        });

    }

    @Override
    public void onDisConnected() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                message.setText("");
                send.setVisibility(View.GONE);
            }
        });

    }

    private class DevicesAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>{
        private static final int VIEW_PROGRESS = 1;
        private static final int VIEW_NO_DEVICES = 2;
        private static final int VIEW_DEVICE = 3;
        ArrayList<Object> devicesList = new ArrayList<>();

        DevicesAdapter(ArrayList<Object> devices){
            devicesList = devices;
        }


        @Override
        public int getItemViewType(int position) {
            if(devicesList.isEmpty()){
               return super.getItemViewType(position);
            }
            if(devicesList.get(position) instanceof String){
                if(devicesList.get(position).equals(getString(R.string.no_devices_available))){
                    return VIEW_NO_DEVICES;
                }else if(devicesList.get(position).equals(getString(R.string.discovering))){
                    return VIEW_PROGRESS;
                }
            }else if(devicesList.get(position) instanceof BluetoothDevice){
                return VIEW_DEVICE;
            }
            return super.getItemViewType(position);
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            if(viewType == VIEW_DEVICE) {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.device_layout, parent, false);
                return new DeviceViewHolder(view);
            }else if(viewType == VIEW_NO_DEVICES){
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.no_devices_layout, parent, false);
                return new NoDevicesViewHolder(view);
            }else if(viewType == VIEW_PROGRESS){
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.progress_layout, parent, false);
                return new ProgressViewHolder(view);
            }
            return null;
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            if(holder instanceof DeviceViewHolder) {
                final BluetoothDevice device = (BluetoothDevice) devicesList.get(holder.getAdapterPosition());
                TextView name = holder.itemView.findViewById(R.id.deviceName);
                name.setText(device.getName());

                holder.itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        cancelDisCovery();
                        if(thread!=null){
                            thread.cancel();
                            thread = null;
                        }
                        thread = new ConnectThread(bluetoothAdapter.getRemoteDevice(device.getAddress()), MainActivity.this);
                        thread.start();
                    }
                });
            }
        }

        @Override
        public int getItemCount() {
            return devicesList.size();
        }

        private class DeviceViewHolder extends RecyclerView.ViewHolder{
            AppCompatTextView deviceName;
            DeviceViewHolder(View view) {
                super(view);
                deviceName = view.findViewById(R.id.deviceName);
            }
        }

        private class NoDevicesViewHolder extends RecyclerView.ViewHolder{
            NoDevicesViewHolder(View view) {
                super(view);
            }
        }

        private class ProgressViewHolder extends RecyclerView.ViewHolder{
            ProgressViewHolder(View view) {
                super(view);
            }
        }
    }


}
