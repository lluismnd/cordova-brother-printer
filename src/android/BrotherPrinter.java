package com.lluismnd.cordova.plugin.brotherprinter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.Set;

import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.apache.cordova.PermissionHelper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Picture;
import android.os.Environment;
import android.os.Handler;
import android.telecom.Call;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.app.PendingIntent;
import android.content.IntentFilter;

import com.brother.ptouch.sdk.LabelInfo;
import com.brother.ptouch.sdk.NetPrinter;
import com.brother.ptouch.sdk.Printer;
import com.brother.ptouch.sdk.PrinterInfo;
import com.brother.ptouch.sdk.PrinterStatus;
import com.brother.ptouch.sdk.connection.BluetoothConnectionSetting;
import com.brother.ptouch.sdk.TemplateInfo;

import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;

public class BrotherPrinter extends CordovaPlugin {

    String modelName = "QL-720NW";
    private NetPrinter[] netPrinters;

    private String ipAddress   = null;
    private String macAddress  = null;
    private Boolean searched   = false;
    private Boolean found      = false;

    public BluetoothAdapter bluetoothAdapter = null;

    public static final int READ_EXTERNAL_PDF = 1;
    public static final int READ_EXTERNAL_IMAGE = 2;
    public static final int READ_EXTERNAL_TEMPLATE = 3;
    public static final int PERMISSION_DENIED_ERROR = 20;
    private static final String ACTION_USB_PERMISSION = "com.lluismnd.cordova.plugin.brotherprinter.USB_PERMISSION";

    private JSONArray args = null;
    CallbackContext callbackContext = null;

    //token to make it easy to grep logcat
    private static final String TAG = "print";

    private CallbackContext callbackctx;

    @Override
    public boolean execute (String action, JSONArray args, CallbackContext callbackContext) throws JSONException {

        if ("findNetworkPrinters".equals(action)) {
            findNetworkPrinters(callbackContext);
            return true;
        }

        if ("printPDF".equals(action)) {
            if(!PermissionHelper.hasPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)) {
                this.args = args;
                this.callbackContext = callbackContext;
                PermissionHelper.requestPermission(this, READ_EXTERNAL_PDF, Manifest.permission.READ_EXTERNAL_STORAGE);
            } else {
                printPDF(args, callbackContext);
            }

            return true;
        }

        if ("printImage".equals(action)) {
            if(!PermissionHelper.hasPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)) {
                this.args = args;
                this.callbackContext = callbackContext;
                PermissionHelper.requestPermission(this, READ_EXTERNAL_IMAGE, Manifest.permission.READ_EXTERNAL_STORAGE);
            } else {
                printImage(args, callbackContext);
            }
            return true;
        }

        if ("printTemplate".equals(action)) {
            Log.d(TAG, "---- printTemplate! ----");
            if(!PermissionHelper.hasPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)) {
                this.args = args;
                this.callbackContext = callbackContext;
                PermissionHelper.requestPermissions( this, READ_EXTERNAL_TEMPLATE, new String[] {Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE});
            } else {
                printTemplate(args, callbackContext);
            }

            return true;
        }

        if ("getTemplates".equals(action)) {
            getTemplates(args, callbackContext);
            return true;
        }

        if ("addTemplate".equals(action)) {
            addTemplate(args, callbackContext);
            return true;
        }

        if ("removeTemplates".equals(action)) {
            removeTemplates(args, callbackContext);
            return true;
        }

        if ("getPrinters".equals(action)) {
            Log.d(TAG, "---- getPrinters! ----");
            getPrinters(callbackContext);
            return true;
        }


        if ("sendUSBConfig".equals(action)) {
            sendUSBConfig(args, callbackContext);
            return true;
        }

        return false;
    }

    private NetPrinter[] enumerateNetPrinters() {
        Printer myPrinter = new Printer();
        PrinterInfo myPrinterInfo = new PrinterInfo();
        netPrinters = myPrinter.getNetPrinters(modelName);
        return netPrinters;
    }

    private void findNetworkPrinters(final CallbackContext callbackctx) {

        cordova.getThreadPool().execute(new Runnable() {
            public void run() {
                try{

                    searched = true;

                    NetPrinter[] netPrinters = enumerateNetPrinters();
                    int netPrinterCount = netPrinters.length;

                    ArrayList<Map> netPrintersList = null;
                    if(netPrintersList != null) netPrintersList.clear();
                    netPrintersList = new ArrayList<Map>();

                    if (netPrinterCount > 0) {
                        found = true;
                        Log.d(TAG, "---- network printers found! ----");

                        for (int i = 0; i < netPrinterCount; i++) {
                            Map<String, String> netPrinter = new HashMap<String, String>();

                            ipAddress = netPrinters[i].ipAddress;
                            macAddress = netPrinters[i].macAddress;

                            netPrinter.put("ipAddress", netPrinters[i].ipAddress);
                            netPrinter.put("macAddress", netPrinters[i].macAddress);
                            netPrinter.put("serNo", netPrinters[i].serNo);
                            netPrinter.put("nodeName", netPrinters[i].nodeName);

                            netPrintersList.add(netPrinter);

                            Log.d(TAG,
                                    " idx:    " + Integer.toString(i)
                                            + "\n model:  " + netPrinters[i].modelName
                                            + "\n ip:     " + netPrinters[i].ipAddress
                                            + "\n mac:    " + netPrinters[i].macAddress
                                            + "\n serial: " + netPrinters[i].serNo
                                            + "\n name:   " + netPrinters[i].nodeName
                            );
                        }

                        Log.d(TAG, "---- /network printers found! ----");

                    }else if (netPrinterCount == 0 ) {
                        found = false;
                        Log.d(TAG, "!!!! No network printers found !!!!");
                    }

                    JSONArray args = new JSONArray();
                    PluginResult result;

                    Boolean available = netPrinterCount > 0;

                    args.put(available);
                    args.put(netPrintersList);

                    result = new PluginResult(PluginResult.Status.OK, args);

                    callbackctx.sendPluginResult(result);

                }catch(Exception e){
                    e.printStackTrace();
                }

            }

        });

    }

    private Printer initPrinter(JSONObject printerInfo, CallbackContext callbackctx ){
        Printer myPrinter = new Printer();
        PrinterInfo myPrinterInfo = new PrinterInfo();

        try {
            myPrinterInfo = myPrinter.getPrinterInfo();

            myPrinterInfo.printerModel = PrinterInfo.Model.valueOf(printerInfo.getString("model"));
            myPrinterInfo.port = PrinterInfo.Port.valueOf(printerInfo.getString("port"));
            myPrinterInfo.printMode = PrinterInfo.PrintMode.ORIGINAL;
            myPrinterInfo.orientation = PrinterInfo.Orientation.valueOf(printerInfo.getString("orientation"));
            myPrinterInfo.paperSize = PrinterInfo.PaperSize.CUSTOM;
            myPrinterInfo.ipAddress = ipAddress;
            myPrinterInfo.macAddress = printerInfo.getString("macAddress");
            myPrinterInfo.numberOfCopies = ( printerInfo.has( "numberOfCopies" ) )? printerInfo.getInt("numberOfCopies") : 1;
            myPrinterInfo.customPaper = printerInfo.getString( "customPaper" ); //Environment.getExternalStorageDirectory().toString() + "/ALEX/TD2120_57mm.bin" = /storage/emulated/0/ALEX/...

            myPrinter.setPrinterInfo(myPrinterInfo);


            if( printerInfo.getString("port").equals( "BLUETOOTH" ) ){
                this.bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                BluetoothConnectionSetting.setBluetoothAdapter( this.bluetoothAdapter );
            }
            else{
                UsbManager usbManager = (UsbManager) cordova.getActivity().getSystemService(Context.USB_SERVICE);
                UsbDevice usbDevice = myPrinter.getUsbDevice(usbManager);

                if (usbDevice == null) {
                    callbackctx.error( "No USB Found" );
                }

                PendingIntent.getBroadcast(cordova.getActivity().getApplicationContext(), 0, new Intent(ACTION_USB_PERMISSION), 0);
                cordova.getContext().registerReceiver(usbReceiver, new IntentFilter(ACTION_USB_PERMISSION) );
                while (true) {
                    if (!usbManager.hasPermission(usbDevice) ) {
                        usbManager.requestPermission(usbDevice, PendingIntent.getBroadcast(cordova.getContext(), 0, new Intent(ACTION_USB_PERMISSION), 0));
                    } else {
                        Log.d( "BrotherSDK", "TENEMOS PERMISO" );
                        break;
                    }
                    try {
                        Thread.sleep(1000);
                    }
                    catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

            }
        }
        catch( Exception e ){}

        Log.d( "BrotherSDK", "DEVOLVEMOS la printer" );
        return myPrinter;
    }

    private void printPDF( JSONArray args, final CallbackContext callbackctx) {
        try{
            String filePath = args.getString(0);
            JSONObject printerInfo = args.getJSONObject(1);

            cordova.getThreadPool().execute(new Runnable() {
                public void run() {
                    try{
                        Printer myPrinter = initPrinter( printerInfo, callbackctx );

                        int pages = myPrinter.getPDFFilePages( filePath );
                        PrinterStatus status = null;
                        for( int i = 1; i <= pages; i++ ){
                            Log.d( TAG, "Print page " + i);
                            status = myPrinter.printPdfFile( filePath, i );
                        }
                        callbackctx.success(String.valueOf(status.errorCode));

                    }catch(Exception e){
                        callbackctx.error("FAILED to print: " + e.toString() );
                    }
                }
            });
        }
        catch( Exception e ){
            callbackctx.error("FAILED to print: " + e.toString() );
        }
    }


    public static Bitmap bmpFromBase64(String base64, final CallbackContext callbackctx){
        try{
            byte[] bytes = Base64.decode(base64, Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        }catch(Exception e){
            e.printStackTrace();
            return null;
        }
    }

    private void printImage( JSONArray args, final CallbackContext callbackctx) {
        try{
            final Bitmap bitmap = bmpFromBase64(args.optString(0, null), callbackctx);
            JSONObject printerInfo = args.getJSONObject(1);

            cordova.getThreadPool().execute(new Runnable() {
                public void run() {
                    try{
                        Printer myPrinter = initPrinter( printerInfo, callbackctx );

                        PrinterStatus status = myPrinter.printImage(bitmap);

                        callbackctx.success(String.valueOf(status.errorCode));

                    }catch(Exception e){
                        callbackctx.error("FAILED to print: " + e.toString() );
                    }
                }
            });
        }
        catch( Exception e ){
            callbackctx.error("FAILED to print: " + e.toString() );
        }
    }

    private void getTemplates( JSONArray args, final CallbackContext callbackctx) {
        try{
            JSONObject printerInfo = args.getJSONObject(0);

            cordova.getThreadPool().execute(new Runnable() {
                public void run() {
                    try{
                        Printer myPrinter = initPrinter( printerInfo, callbackctx );

                        myPrinter.startCommunication();
                        List<TemplateInfo> templates = new ArrayList();
                        PrinterStatus status = myPrinter.getTemplateList( templates );

                        JSONArray result = new JSONArray();
                        for( TemplateInfo t : templates ){
                            JSONObject templInfo = new JSONObject();
                            templInfo.put( "key", t.key );
                            templInfo.put( "fileName", t.fileName );
                            result.put( templInfo );
                            Log.d( "BrotherSDKPlugin", t.key + " " + t.fileName );
                        }
                        myPrinter.endCommunication();
                        JSONObject oResult = new JSONObject();
                        oResult.put( "data", result );
                        callbackctx.success( oResult );

                    }catch(Exception e){
                        callbackctx.error("FAILED to get templates: " + e.toString() );
                    }
                }
            });
        }
        catch( Exception e ){
            callbackctx.error("FAILED to get templates: " + e.toString() );
        }
    }

    private void addTemplate( JSONArray args, final CallbackContext callbackctx) {
        try{
            JSONObject template = args.getJSONObject(0);
            JSONObject printerInfo = args.getJSONObject(1);

            cordova.getThreadPool().execute(new Runnable() {
                public void run() {
                    try{
                        Printer myPrinter = initPrinter( printerInfo, callbackctx );

                        myPrinter.startCommunication();
                        PrinterStatus status = myPrinter.transfer( template.getString( "path" ) );
                        myPrinter.endCommunication();

                        String tmplName = template.getString( "path" );
                        String[] aTmplName = tmplName.split("/" );
                        tmplName = aTmplName[ aTmplName.length - 1 ];
                        //Log.d( "BrotherSDKPlugin", tmplName );
                        aTmplName = tmplName.split( "\\." );
                        //Log.d( "BrotherSDKPlugin", Arrays.toString( aTmplName ) );
                        tmplName = aTmplName[ 0 ];

                        myPrinter.startCommunication();
                        List<TemplateInfo> templates = new ArrayList();
                        status = myPrinter.getTemplateList( templates );

                        JSONObject template = new JSONObject();
                        for( TemplateInfo t : templates ){
                            if( t.fileName == tmplName ) {
                                JSONObject templInfo = new JSONObject();
                                templInfo.put("key", t.key);
                                templInfo.put("fileName", t.fileName);

                                template = templInfo;
                            }
                            //Log.d( "BrotherSDKPlugin", t.key + " " + t.fileName + " " + tmplName );
                        }
                        myPrinter.endCommunication();
                        callbackctx.success( template );

                    }catch(Exception e){
                        callbackctx.error("FAILED to add template: " + e.toString() );
                    }
                }
            });
        }
        catch( Exception e ){
            callbackctx.error("FAILED to print: " + e.toString() );
        }
    }

    private void removeTemplates( JSONArray args, final CallbackContext callbackctx) {
        try{
            JSONArray templates = args.getJSONArray(0);
            JSONObject printerInfo = args.getJSONObject(1);

            ArrayList<Integer> list_templates = new ArrayList<Integer>();
            if (templates != null) {
                int len = templates.length();
                for (int i=0;i<len;i++){
                    list_templates.add(templates.getInt(i));
                }
            }

            Log.d( "BrotherSDK", list_templates.toString() );

            cordova.getThreadPool().execute(new Runnable() {
                public void run() {
                    try{
                        Printer myPrinter = initPrinter( printerInfo, callbackctx );

                        myPrinter.startCommunication();
                        PrinterStatus status = myPrinter.removeTemplate( list_templates );
                        myPrinter.endCommunication();
                        callbackctx.success( String.valueOf(status.errorCode) );

                    }catch(Exception e){
                        callbackctx.error("FAILED to add template: " + e.toString() );
                    }
                }
            });
        }
        catch( Exception e ){
            callbackctx.error("FAILED to print: " + e.toString() );
        }
    }

    private void printTemplate( JSONArray args, final CallbackContext callbackctx) {
        try{
            JSONObject template = args.getJSONObject(0);
            JSONObject printerInfo = args.getJSONObject(1);

            cordova.getThreadPool().execute(new Runnable() {
                public void run() {
                    try{
                        Printer myPrinter = initPrinter( printerInfo, callbackctx );

                        myPrinter.startCommunication();
                        if( myPrinter.startPTTPrint( template.getInt("id"), null ) ){
                            String sVariables = "";
                            if( template.has( "data" ) ){
                                for (int i=0; i < template.getJSONArray("data").length(); i++) {
                                    JSONObject key = template.getJSONArray("data").getJSONObject(i);
                                    Boolean b = myPrinter.replaceTextName( key.getString( "v" ), key.getString( "k" ) );
                                    Log.d( "BrotherPrinter", "CAMBIAMOS EL TEMPLATE: " + key.getString( "v" ) + " - " + key.getString( "k" ) + " - " + b );
                                    if( !sVariables.equals( "" ) ) sVariables += ",";
                                    sVariables += key.getString( "v" );
                                }
                            }

                            Integer iTemplate = template.getInt("id");
                            String sTemplate = "";
                            if( iTemplate < 10 ) sTemplate+= "0";
                            if( iTemplate < 100 ) sTemplate+= "0";
                            sTemplate = sTemplate + iTemplate.toString();

                            String sNumCopies = "";
                            if( myPrinter.getUserPrinterInfo().numberOfCopies < 10 ) sNumCopies+= "0";
                            if( myPrinter.getUserPrinterInfo().numberOfCopies < 100 ) sNumCopies+= "0";
                            sNumCopies = sNumCopies + myPrinter.getUserPrinterInfo().numberOfCopies;
                            String s = "^TS" + sTemplate + sVariables + "^CN" + sNumCopies + "^FF";
                            Log.d( "BrotherPrinter", "FICHERO: " + s);
                            byte[] bytes = s.getBytes();
                            FileOutputStream fout=new FileOutputStream(Environment.getExternalStorageDirectory().toString() + "/ALEX/template.txt");
                            fout.write(bytes, 0, bytes.length);
                            fout.flush();
                            fout.close();

                            PrinterStatus status = myPrinter.sendBinaryFile( Environment.getExternalStorageDirectory().toString() + "/ALEX/template.txt" );
                            myPrinter.endCommunication();
                            callbackctx.success(String.valueOf(status.errorCode));
                        }
                        else{
                            callbackctx.error("FAILED to print: ");
                        }

                    }catch(Exception e){
                        callbackctx.error("FAILED to print: " + e.toString() );
                    }
                }
            });
        }
        catch( Exception e ){
            callbackctx.error("FAILED to print: " + e.toString() );
        }
    }

    private void getPrinters( final CallbackContext callbackctx) {
        try{

            cordova.getThreadPool().execute(new Runnable() {
                public void run() {
                    try{
                        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

                        if (bluetoothAdapter == null) {
                            Log.e("BrotherSDKPlugin", "No Bluetooth Adapter was found");
                            JSONObject error = new JSONObject();
                            error.put( "code", "1" );
                            error.put( "message", "No bluetooth adapter was found" );
                            callbackctx.error( error );
                        }

                        if (!bluetoothAdapter.isEnabled()) {

                            Intent enableBtIntent = new Intent(
                                    BluetoothAdapter.ACTION_REQUEST_ENABLE);
                            enableBtIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            /** startActivity(enableBtIntent);**/
                            Log.e("BrotherSDKPlugin", "Bluetooth Adapter not enabled. Please enable it and try again");

                            JSONObject error = new JSONObject();
                            error.put( "code", "2" );
                            error.put( "message", "Bluetooth Adapter not enabled. Please enable it and try again" );
                            callbackctx.error( error );
                        }


                        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();

                        if (pairedDevices == null || pairedDevices.isEmpty()) {
                            Log.d("BrotherSDKPlugin", "---- /NO bluetooth printers found! ----");

                            JSONObject error = new JSONObject();
                            error.put( "code", "3" );
                            error.put( "message", "No bluetooth printers found" );
                            callbackctx.success( new JSONArray() );
                        }

                        JSONArray devices = new JSONArray();
                        for (BluetoothDevice device : pairedDevices) {
                            JSONObject deviceInfo = new JSONObject();
                            deviceInfo.put( "name", device.getName() );
                            deviceInfo.put( "macAddress", device.getAddress() );
                            devices.put( deviceInfo );
                            Log.d("BrotherSDKPlugin", device.getName() + " " + device.getAddress());
                        }
                        JSONObject result = new JSONObject();
                        result.put( "data", devices );
                        callbackctx.success( result );

                    }catch(Exception e){
                        callbackctx.error("FAILED to add template: " + e.toString() );
                    }
                }
            });
        }
        catch( Exception e ){
            callbackctx.error("FAILED to print: " + e.toString() );
        }
    }


    private void sendUSBConfig(final JSONArray args, final CallbackContext callbackctx){

        cordova.getThreadPool().execute(new Runnable() {
            public void run() {

                Printer myPrinter = new Printer();

                Context context = cordova.getActivity().getApplicationContext();

                UsbManager usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
                UsbDevice usbDevice = myPrinter.getUsbDevice(usbManager);
                if (usbDevice == null) {
                    Log.d(TAG, "USB device not found");
                    return;
                }

                PendingIntent permissionIntent = PendingIntent.getBroadcast(context, 0, new Intent(ACTION_USB_PERMISSION), 0);
                usbManager.requestPermission(usbDevice, permissionIntent);

                final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        String action = intent.getAction();
                        if (ACTION_USB_PERMISSION.equals(action)) {
                            synchronized (this) {
                                if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false))
                                    Log.d(TAG, "USB permission granted");
                                else
                                    Log.d(TAG, "USB permission rejected");
                            }
                        }
                    }
                };

                context.registerReceiver(mUsbReceiver, new IntentFilter(ACTION_USB_PERMISSION));

                while (true) {
                    if (!usbManager.hasPermission(usbDevice)) {
                        usbManager.requestPermission(usbDevice, permissionIntent);
                    } else {
                        break;
                    }

                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                PrinterInfo myPrinterInfo = new PrinterInfo();

                myPrinterInfo = myPrinter.getPrinterInfo();

                myPrinterInfo.printerModel  = PrinterInfo.Model.QL_720NW;
                myPrinterInfo.port          = PrinterInfo.Port.USB;
                myPrinterInfo.paperSize     = PrinterInfo.PaperSize.CUSTOM;

                myPrinter.setPrinterInfo(myPrinterInfo);

                LabelInfo myLabelInfo = new LabelInfo();

                myLabelInfo.labelNameIndex  = myPrinter.checkLabelInPrinter();
                myLabelInfo.isAutoCut       = true;
                myLabelInfo.isEndCut        = true;
                myLabelInfo.isHalfCut       = false;
                myLabelInfo.isSpecialTape   = false;

                //label info must be set after setPrinterInfo, it's not in the docs
                myPrinter.setLabelInfo(myLabelInfo);


                try {
                    File outputDir = context.getCacheDir();
                    File outputFile = new File(outputDir.getPath() + "configure.prn");

                    FileWriter writer = new FileWriter(outputFile);
                    writer.write(args.optString(0, null));
                    writer.close();

                    PrinterStatus status = myPrinter.printFile(outputFile.toString());
                    outputFile.delete();

                    String status_code = ""+status.errorCode;

                    Log.d(TAG, "PrinterStatus: "+status_code);

                    PluginResult result;
                    result = new PluginResult(PluginResult.Status.OK, status_code);
                    callbackctx.sendPluginResult(result);

                } catch (IOException e) {
                    Log.d(TAG, "Temp file action failed: " + e.toString());
                }

            }
        });
    }

    public void onRequestPermissionResult(int requestCode, String[] permissions,
                                          int[] grantResults) throws JSONException {
        for (int r : grantResults) {
            if (r == PackageManager.PERMISSION_DENIED) {
                this.callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.ERROR, PERMISSION_DENIED_ERROR));
                return;
            }
        }
        switch (requestCode) {
            case READ_EXTERNAL_PDF:
                printPDF( this.args, this.callbackContext);
                break;
            case READ_EXTERNAL_IMAGE:
                printImage( this.args, this.callbackContext);
                break;
            case READ_EXTERNAL_TEMPLATE:
                printTemplate( this.args, this.callbackContext);
                break;
        }
    }

    private final BroadcastReceiver usbReceiver = new BroadcastReceiver() {

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (ACTION_USB_PERMISSION.equals(action)) {
                synchronized (this) {
                    UsbDevice device = (UsbDevice)intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);

                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        if(device != null){
                            //call method to set up device communication
                        }
                    }
                    else {
                        Log.d(TAG, "permission denied for device " + device);
                    }
                }
            }
        }
    };

}
