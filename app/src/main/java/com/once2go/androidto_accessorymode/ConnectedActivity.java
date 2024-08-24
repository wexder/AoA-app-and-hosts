package com.once2go.androidto_accessorymode;

import static android.app.PendingIntent.FLAG_IMMUTABLE;
import static android.app.PendingIntent.FLAG_MUTABLE;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbAccessory;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.once2go.androidto_accessorymode.webusb.WebUsbChatActivity;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;

public abstract class ConnectedActivity extends AppCompatActivity {

    private static final String ACTION_USB_PERMISSION = WebUsbChatActivity.class.getCanonicalName() + ".usb_permission";

    /**
     * -Keep links to parcel descriptor and file descriptor in memory to avoid
     * "BAD FILE DESCRIPTOR ISSUE DURING COMMUNICATION"
     */
    private ParcelFileDescriptor fileDescriptor;
    private FileDescriptor fd;
    private UsbAccessory usbAccessory;
    /**
     * ----------------------------------
     */

    protected FileOutputStream outputStream;
    protected FileInputStream inputStream;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getIntent() == null) finish();
        UsbAccessory accessory = (UsbAccessory) getIntent().getParcelableExtra(UsbManager.EXTRA_ACCESSORY);
        if (accessory == null) {
            Toast.makeText(this, "Something wen wrong! Try to reconnect.", Toast.LENGTH_SHORT).show();
            finish();
        }
        this.usbAccessory = accessory;
        prepareToConnect(getIntent());
    }

    private final BroadcastReceiver mAccessoryPermissionReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null && intent.getAction().equals(UsbManager.ACTION_USB_ACCESSORY_DETACHED)) {
                finish();
                return;
            }

            prepareToConnect(intent);
        }
    };

    private void prepareToConnect(Intent intent) {
        Log.i("m", "Acc "+ usbAccessory + "  " + intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false));
        if (usbAccessory == null){
            finish();
            return;
        }
        UsbManager usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        if (!intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
            PendingIntent permissionIntent = PendingIntent.getBroadcast(this, 0,
                    new Intent(ACTION_USB_PERMISSION), FLAG_MUTABLE);
            IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
            filter.addAction(UsbManager.ACTION_USB_ACCESSORY_DETACHED);
            registerReceiver(mAccessoryPermissionReceiver, filter);
            usbManager.requestPermission(usbAccessory, permissionIntent);
        } else {
            fileDescriptor = usbManager.openAccessory(usbAccessory);
            Log.i("FUCK", "FCUK "+ fileDescriptor);
            if (fileDescriptor == null) {
                finish();
                return;
            }
            fd = fileDescriptor.getFileDescriptor();
            inputStream = new FileInputStream(fd);
            outputStream = new FileOutputStream(fd);
            onConnected();
        }
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(mAccessoryPermissionReceiver);
        super.onDestroy();
    }

    protected abstract void onConnected();
}
