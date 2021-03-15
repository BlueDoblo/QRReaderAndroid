package com.example.myapplication;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Vibrator;
import android.text.Html;
import android.util.Size;
import android.widget.Toast;
import com.google.common.util.concurrent.ListenableFuture;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DecimalFormat;
import java.util.Base64;
import java.util.concurrent.ExecutionException;

public class MainActivity extends AppCompatActivity {
    private static final int PERMISSION_REQUEST_CAMERA = 0;
    private PreviewView previewView;
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;

    private String qrCode;

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        previewView = findViewById(R.id.activity_main_previewView);
        cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        requestCamera();
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void bindCameraPreview(@NonNull ProcessCameraProvider cameraProvider) {
        previewView.setPreferredImplementationMode(PreviewView.ImplementationMode.SURFACE_VIEW);
        Preview preview = new Preview.Builder()
                .build();
        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();
        preview.setSurfaceProvider(previewView.createSurfaceProvider());
        ImageAnalysis imageAnalysis =
                new ImageAnalysis.Builder()
                        .setTargetResolution(new Size(1280, 720))
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();
        imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(this), new QRCodeImageAnalyzer(new QRCodeFoundListener() {

            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void onQRCodeFound(String _qrCode) throws InterruptedException {
                qrCode = _qrCode;
                Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                vibrator.vibrate(300);
                stopCamera(cameraProvider);
                alertDialog(qrCode.toString());
            }

            @Override
            public void qrCodeNotFound() {

            }
        }));
        Camera camera = cameraProvider.bindToLifecycle((LifecycleOwner)this, cameraSelector, imageAnalysis, preview);

    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void requestCamera() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.CAMERA}, PERMISSION_REQUEST_CAMERA);
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, PERMISSION_REQUEST_CAMERA);
            }

    }
    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PERMISSION_REQUEST_CAMERA) {
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera();
            } else {
                Toast.makeText(this, "Camera Permission Denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void startCamera() {
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                bindCameraPreview(cameraProvider);
            } catch (ExecutionException | InterruptedException e) {
                Toast.makeText(this, "Error starting camera " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }, ContextCompat.getMainExecutor(this));



    }

    public interface QRCodeFoundListener {
        void onQRCodeFound(String qrCode) throws InterruptedException;
        void qrCodeNotFound();
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void alertDialog(String Msg) {
        AlertDialog.Builder dialog=new AlertDialog.Builder(this);
        String Salida = " ";
        if (Msg.length()>= 33 && Msg.startsWith("https://www.afip.gob.ar/fe/qr/?p=")){
        try {
            Base64.Decoder decoder = Base64.getDecoder();
            // Decoding string desde la posicion 33 que es donde Afip manda Datos
            String NewStr = Msg.toString().substring(33, Msg.toString().length());
            String dStr = new String(decoder.decode(NewStr.toString()));
            try {
                JSONObject jsonObject = new JSONObject(dStr);
                Salida = "Centro Emisor: " + jsonObject.getInt("ptoVta")+"\n";
                Salida = Salida + "Tipo: " + jsonObject.getInt("tipoCmp")+"\n";
                Salida = Salida + "Nro Comprobante: " + jsonObject.getInt("nroCmp")+"\n";
                Salida = Salida + "Cuit: " + jsonObject.getLong("cuit")+"\n";
                Salida = Salida + "Fecha: " + jsonObject.getString("fecha").toString()+"\n";
                Salida = Salida + "Importe " +jsonObject.getLong("importe")+"\n";
                Salida = Salida + "Moneda: " + jsonObject.getString("moneda").toString() +"\n";
                Salida = Salida + "Tipo Cambio: " + jsonObject.getInt("ctz") +"\n";
                Salida = Salida + "\n "+ "Desea Cargar el Comprobante ?";
            }catch (JSONException err){
                Salida = "Error en formato : \n";
                Salida = Salida + dStr.trim() + "\n";
                Salida = Salida + err.toString();
            }
        }catch (IllegalArgumentException e)
        {
            Salida = "Codigo QR no Pertenece a una Factura \n";
            Salida = Salida + "Codigo Leido: \n\n";
            Salida = Salida + Msg.trim().toString();
        }}
        else{
            Salida = "Codigo QR no Pertenece a una Factura \n";
            Salida = Salida + "Codigo Leido: \n\n";
            Salida = Salida + Msg.trim().toString();
        }

        // pruebas Naza
        dialog.setMessage(Salida.toString());
        dialog.setTitle("Comprobante Leido");
        dialog.setPositiveButton("Si",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog,
                                        int which) {
                        // Aca se deben realizar los inserts
                        startCamera();
                    }
                });
        dialog.setNegativeButton("No",new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // no hace falta hacer nada solo encender la camara de nuevo
                startCamera();
            }
        });
        AlertDialog alertDialog=dialog.create();
        alertDialog.show();

    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void stopCamera(@NonNull ProcessCameraProvider cameraProvider){
        cameraProvider.unbindAll();
    }

}

