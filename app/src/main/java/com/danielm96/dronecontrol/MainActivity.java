package com.danielm96.dronecontrol;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import java.util.HashMap;
import java.util.Map;

/**
 * MainActivity.java
 *
 * Tu będzie znajdować się główny panel sterowania dronem.
 * Być może będzie to Fullscreen activity...
 *
 * Na początku wyświetlana jest informacja o wersji beta.
 * Następnie aplikacja prosi o przyznanie uprawnień.
 * Wymagane są uprawnienia "na oko". Co jest naprawdę potrzebne, okaże się później...
 */

public class MainActivity extends AppCompatActivity
{
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // informacja o wersji beta
        showBetaDialog();

        // jeśli przyznano uprawnienia, to kontynuuj działanie
        if (checkPermissions())
        {
            // ...
        }
    }

    // BETA
    private void showBetaDialog()
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.BetaHeader);
        builder.setMessage(R.string.BetaMessage);
        builder.setPositiveButton(R.string.Button_OK,null);
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    // sprawdzanie uprawnień
    private boolean checkPermissions()
    {
        int NeededPermissionsCount = 0;
        // uprawnienia dotyczące Wi-Fi
        int WiFiStatePermission = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_WIFI_STATE);
        if (WiFiStatePermission != PackageManager.PERMISSION_GRANTED)
        {
            requestPermissions(new String[]{Manifest.permission.ACCESS_WIFI_STATE},1);
            NeededPermissionsCount++;
        }

        int WiFiChangePermission = ContextCompat.checkSelfPermission(this, Manifest.permission.CHANGE_WIFI_STATE);
        if (WiFiChangePermission != PackageManager.PERMISSION_GRANTED)
        {
            requestPermissions(new String[]{Manifest.permission.CHANGE_WIFI_STATE},1);
            NeededPermissionsCount++;
        }

        return (NeededPermissionsCount == 0);
    }

    // przyznawanie uprawnień
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
    {
        if (requestCode == 1)
        {
            Map<String, Integer> perms = new HashMap<>();
            perms.put(Manifest.permission.ACCESS_WIFI_STATE, PackageManager.PERMISSION_GRANTED);
            perms.put(Manifest.permission.CHANGE_WIFI_STATE, PackageManager.PERMISSION_GRANTED);

            if (grantResults.length > 0)
            {
                for (int i = 0; i < permissions.length; i++)
                {
                    perms.put(permissions[i], grantResults[i]);
                }
                // przyznano oba uprawnienia
                if (perms.get(Manifest.permission.ACCESS_WIFI_STATE) == PackageManager.PERMISSION_GRANTED && perms.get(Manifest.permission.CHANGE_WIFI_STATE) == PackageManager.PERMISSION_GRANTED)
                {
                    // ...
                }
                else
                {
                    // nie przyznano uprawnień, zaznaczono "Nie pytaj ponownie"
                    if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_WIFI_STATE) || ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CHANGE_WIFI_STATE))
                    {
                        showDialogOK(getString(R.string.PermissionsInfo), new DialogInterface.OnClickListener()
                        {
                            @Override
                            public void onClick(DialogInterface dialog, int button)
                            {
                                switch (button)
                                {
                                    case DialogInterface.BUTTON_POSITIVE:
                                        checkPermissions();
                                        break;

                                    case DialogInterface.BUTTON_NEGATIVE:
                                        Thread close_thread = new Thread()
                                        {
                                            @Override
                                            public void run()
                                            {
                                                try
                                                {
                                                    sleep(1000);
                                                    System.exit(0);
                                                }
                                                catch (InterruptedException e)
                                                {
                                                    Log.e("MainActivity", "Error while closing application.");
                                                }
                                            }
                                        };
                                        close_thread.start();
                                }
                            }
                        });
                    }
                    else
                    {
                        // nie przyznano uprawnień, zaznaczono "Nie pytaj ponownie"
                        Toast.makeText(this, R.string.PermissionsInfo,Toast.LENGTH_SHORT).show();
                        Thread close_thread = new Thread()
                        {
                            @Override
                            public void run()
                            {
                                try
                                {
                                    sleep(1000);
                                    System.exit(0);
                                }
                                catch (InterruptedException e)
                                {
                                    Log.e("MainActivity", "Error while closing application.");
                                }
                            }
                        };
                        close_thread.start();
                    }
                }
            }
        }
    }

    // Okno dialogowe z przyciskami OK i Anuluj, dla procedury przyznawania uprawnień
    private void showDialogOK(String message, DialogInterface.OnClickListener okListener)
    {
        new AlertDialog.Builder(this)
                .setMessage(message)
                .setPositiveButton(R.string.Button_OK, okListener)
                .setNegativeButton(R.string.Button_Cancel, okListener)
                .create()
                .show();
    }
}
