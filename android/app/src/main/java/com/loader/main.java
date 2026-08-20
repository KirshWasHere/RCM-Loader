package com.loader;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

public class main extends Activity {

    UsbManager manager;
    TextView status;
    Button select;
    byte[] target;
    PendingIntent permission;

    @Override
    protected void onCreate(Bundle saved) {
        super.onCreate(saved);
        setContentView(R.layout.main);

        manager = (UsbManager) getSystemService(Context.USB_SERVICE);
        status = findViewById(R.id.status);
        select = findViewById(R.id.select);

        permission = PendingIntent.getBroadcast(this, 0, new Intent("com.loader.permission"), PendingIntent.FLAG_IMMUTABLE);
        IntentFilter filter = new IntentFilter("com.loader.permission");
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        
        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(receiver, filter);
        }

        select.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("*/*");
                startActivityForResult(intent, 1);
            }
        });
        
        check();
    }

    @Override
    protected void onActivityResult(int request, int result, Intent data) {
        if (request == 1 && result == RESULT_OK && data != null) {
            Uri uri = data.getData();
            try {
                InputStream input = getContentResolver().openInputStream(uri);
                ByteArrayOutputStream output = new ByteArrayOutputStream();
                byte[] chunk = new byte[1024];
                int read;
                while ((read = input.read(chunk)) != -1) {
                    output.write(chunk, 0, read);
                }
                input.close();
                target = output.toByteArray();
                status.setText("loaded");
                check();
            } catch (Exception error) {
                status.setText("fail");
            }
        }
    }

    void check() {
        if (target == null) return;
        for (UsbDevice device : manager.getDeviceList().values()) {
            if (device.getVendorId() == 2389 && device.getProductId() == 29473) {
                if (manager.hasPermission(device)) {
                    inject(device);
                } else {
                    manager.requestPermission(device, permission);
                }
                return;
            }
        }
    }

    BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if ("com.loader.permission".equals(action)) {
                synchronized (this) {
                    UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        if (device != null) {
                            inject(device);
                        }
                    }
                }
            } else if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
                check();
            }
        }
    };

    void inject(UsbDevice device) {
        try {
            UsbDeviceConnection connection = manager.openDevice(device);
            if (connection == null) {
                status.setText("missing");
                return;
            }
            
            UsbInterface layer = device.getInterface(0);
            connection.claimInterface(layer, true);
            
            UsbEndpoint in = null;
            UsbEndpoint out = null;
            for (int i = 0; i < layer.getEndpointCount(); i++) {
                UsbEndpoint endpoint = layer.getEndpoint(i);
                if (endpoint.getDirection() == android.hardware.usb.UsbConstants.USB_DIR_IN) {
                    in = endpoint;
                } else {
                    out = endpoint;
                }
            }
            
            if (in == null || out == null) {
                status.setText("endpoints");
                return;
            }

            byte[] read = new byte[16];
            connection.bulkTransfer(in, read, read.length, 1000);

            byte[] buffer = payload.build(target);
            int sent = 0;
            int chunks = buffer.length / 4096;
            
            for (int i = 0; i < chunks; i++) {
                connection.bulkTransfer(out, buffer, i * 4096, 4096, 1000);
            }
            
            if (chunks % 2 == 0) {
                byte[] dummy = new byte[4096];
                connection.bulkTransfer(out, dummy, dummy.length, 1000);
            }

            int fd = connection.getFileDescriptor();
            int result = payload.smash(fd, 28672);
            
            if (result == 0) {
                status.setText("smashed");
            } else {
                status.setText("error: " + result);
            }
            
            connection.close();
        } catch (Exception error) {
            status.setText("error");
        }
    }
}
