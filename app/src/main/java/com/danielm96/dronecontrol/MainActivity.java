package com.danielm96.dronecontrol;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.URL;
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
 * Wymagane są uprawnienia "na oko". Co jest naprawdę potrzebne, okaże się podczas testów.
 */

public class MainActivity extends AppCompatActivity
{
    // Podstawowe pola
    // socketStatus - pozwala ustalić, czy socket jest w użyciu (=true)
    boolean socketStatus = false;

    // socket - socket HTTP
    Socket socket;

    // address - adres IP modułu Wi-Fi drona -- docelowo nadawany w ustawieniach
    String address = "192.168.43.40";

    // conTask - obiekt klasy ClientTask do wysyłania żądań HTTP
    ClientTask conTask;

    // port - numer portu -- niewykorzystywany
    // int port = 80;

    // isEnginePoweredOn - zmienna logiczna przechowująca stan silników
    boolean isEnginePoweredOn = false;

    // ENGINE_STATE - etykieta wykorzystywana przy zapisywaniu stanu
    private static final String ENGINE_STATE = "ENGINE_STATE";


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Jeśli poprzedni stan został zapisany, przywróć go
        if (savedInstanceState != null)
        {
            // Przywróc poprzednią wartość stanu silników
            isEnginePoweredOn = savedInstanceState.getBoolean(ENGINE_STATE, isEnginePoweredOn);
        }

        // Jeśli przyznano uprawnienia, to kontynuuj działanie
        if (checkPermissions())
        {
            startApp();
        }
    }

    // Sprawdzanie uprawnień
    private boolean checkPermissions()
    {
        int NeededPermissionsCount = 0;
        // Uprawnienia dotyczące Wi-Fi
        // Odczytywanie stanu Wi-Fi
        /*
        int WiFiStatePermission = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_WIFI_STATE);
        if (WiFiStatePermission != PackageManager.PERMISSION_GRANTED)
        {
            requestPermissions(new String[]{Manifest.permission.ACCESS_WIFI_STATE},1);
            NeededPermissionsCount++;
        }
        */

        // Zmiana stanu Wi-Fi
        int WiFiChangePermission = ContextCompat.checkSelfPermission(this, Manifest.permission.CHANGE_WIFI_STATE);
        if (WiFiChangePermission != PackageManager.PERMISSION_GRANTED)
        {
            requestPermissions(new String[]{Manifest.permission.CHANGE_WIFI_STATE},1);
            NeededPermissionsCount++;
        }

        // Uprawnienia dotyczące sieci
        // Odczytywanie stanu sieci
        /*
        int NetworkStatePermission = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_NETWORK_STATE);
        if (NetworkStatePermission != PackageManager.PERMISSION_GRANTED)
        {
            requestPermissions(new String[]{Manifest.permission.ACCESS_NETWORK_STATE},1);
            NeededPermissionsCount++;
        }
        */

        // Zmiana stanu sieci
        int NetworkChangePermission = ContextCompat.checkSelfPermission(this, Manifest.permission.CHANGE_NETWORK_STATE);
        if (NetworkChangePermission != PackageManager.PERMISSION_GRANTED)
        {
            requestPermissions(new String[]{Manifest.permission.CHANGE_NETWORK_STATE},1);
            NeededPermissionsCount++;
        }

        return (NeededPermissionsCount == 0);
    }

    // Przyznawanie uprawnień
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
    {
        if (requestCode == 1)
        {
            Map<String, Integer> perms = new HashMap<>();
            perms.put(Manifest.permission.CHANGE_WIFI_STATE, PackageManager.PERMISSION_GRANTED);
            perms.put(Manifest.permission.CHANGE_NETWORK_STATE, PackageManager.PERMISSION_GRANTED);

            if (grantResults.length > 0)
            {
                for (int i = 0; i < permissions.length; i++)
                {
                    perms.put(permissions[i], grantResults[i]);
                }
                // Przyznano oba uprawnienia
                if (perms.get(Manifest.permission.CHANGE_WIFI_STATE) == PackageManager.PERMISSION_GRANTED && perms.get(Manifest.permission.CHANGE_NETWORK_STATE) == PackageManager.PERMISSION_GRANTED)
                {
                    startApp();
                }
                else
                {
                    // Nie przyznano uprawnień, nie zaznaczono "Nie pytaj ponownie"
                    if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CHANGE_WIFI_STATE) || ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CHANGE_NETWORK_STATE))
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
                        // Nie przyznano uprawnień, zaznaczono "Nie pytaj ponownie"
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

    // Uruchamianie właściwej części aplikacji
    // Tworzenie przycisków, obrazu
    private void startApp()
    {
        // Wszystkie przyciski w MainActivity są typu ImageButton
        ImageButton arrowUp,
                    arrowDown,
                    arrowLeft,
                    arrowRight,
                    rotateLeft,
                    rotateRight,
                    heightUp,
                    heightDown,
                    engines,
                    settingsButton;

        // Obraz drona
        ImageView droneView = findViewById(R.id.droneIcon);
        droneView.setImageResource(R.drawable.dron_front_256x256);

        // Tworzenie przycisków
        arrowUp = findViewById(R.id.arrowUp);
        arrowDown = findViewById(R.id.arrowDown);
        arrowLeft = findViewById(R.id.arrowLeft);
        arrowRight = findViewById(R.id.arrowRight);
        rotateLeft = findViewById(R.id.rotateLeft);
        rotateRight = findViewById(R.id.rotateRight);
        heightUp = findViewById(R.id.heightUp);
        heightDown = findViewById(R.id.heightDown);
        engines = findViewById(R.id.engines);
        settingsButton = findViewById(R.id.settingsButton);

        // Nadanie początkowego zasobu przyciskowi engines w zależności od stanu silników
        if (isEnginePoweredOn)
        {
            // Silniki włączone - zielona ikona
            engines.setImageResource(R.drawable.power_on);
        }
        else
        {
            // Silniki wyłączone - biała ikona
            engines.setImageResource(R.drawable.power_off);
        }

        // Sprawdź, czy socket jest pusty
        if (socketStatus)
        {
            Toast.makeText(MainActivity.this, "Yyy synek!",Toast.LENGTH_SHORT).show();
        }
        else
        {
            socket = null;
            conTask = new ClientTask(address);
            conTask.execute("CON");
        }

        // Wątek wykonujący pewne zadania
        final Handler mHandler = new Handler();

        // Wykorzystanie wątku mHandler dla przycisków sterujących
        // _hold - akcja wykonywana podczas trzymania przycisku
        // _release - akcja wykonywana przy puszczeniu przycisku

        // Wariant I
        // Przy trzymaniu, wykonuj co 100 ms.

        // Wariant II
        // Wykonaj tylko raz (nie ma metody postDelayed).

        // stopMove
        // Akcja wykonywana przy puszczeniu przycisków sterujących
        final Runnable stopMove = new Runnable()
        {
            @Override
            public void run()
            {
                Log.i("DroneControl", "Stopped move");
                String msg = "STOP";
                ClientTask task = new ClientTask(address);
                task.execute(msg);
            }
        };

        // arrowUp
        // Akcja wykonywana podczas trzymania
        final Runnable arrowUp_hold = new Runnable()
        {
            @Override
            public void run()
            {
                Log.i("DroneControl","Holding arrowUp");
                String msg = "FORWARD";
                ClientTask task = new ClientTask(address);
                task.execute(msg);
                // Opóźnienie wykonania akcji [ms]
                // II
                // mHandler.postDelayed(this,100);
            }
        };

        // arrowDown
        // Akcja wykonywana podczas trzymania
        final Runnable arrowDown_hold = new Runnable()
        {
            @Override
            public void run()
            {
                Log.i("DroneControl","Holding arrowDown");
                String msg = "BACKWARD";
                ClientTask task = new ClientTask(address);
                task.execute(msg);
                // II
                //mHandler.postDelayed(this,100);
            }
        };

        // arrowLeft
        // Akcja wykonywana podczas trzymania
        final Runnable arrowLeft_hold = new Runnable()
        {
            @Override
            public void run()
            {
                Log.i("DroneControl","Holding arrowLeft");
                String msg = "LEFT";
                ClientTask task = new ClientTask(address);
                task.execute(msg);
                // II
                //mHandler.postDelayed(this,100);
            }
        };

        // arrowRight
        // Akcja wykonywana podczas trzymania
        final Runnable arrowRight_hold = new Runnable()
        {
            @Override
            public void run()
            {
                Log.i("DroneControl","Holding arrowRight");
                String msg = "RIGHT";
                ClientTask task = new ClientTask(address);
                task.execute(msg);
                // II
                //mHandler.postDelayed(this,100);
            }
        };

        // rotateLeft
        // Akcja wykonywana podczas trzymania
        final Runnable rotateLeft_hold = new Runnable()
        {
            @Override
            public void run()
            {
                Log.i("DroneControl","Holding rotateLeft");
                String msg = "R_LEFT";
                ClientTask task = new ClientTask(address);
                task.execute(msg);
                // II
                //mHandler.postDelayed(this,100);
            }
        };

        // rotateRight
        // Akcja wykonywana podczas trzymania
        final Runnable rotateRight_hold = new Runnable()
        {
            @Override
            public void run()
            {
                Log.i("DroneControl","Holding rotateRight");
                String msg = "R_RIGHT";
                ClientTask task = new ClientTask(address);
                task.execute(msg);
                // II
                //mHandler.postDelayed(this,100);
            }
        };

        // heightUp
        // Akcja wykonywana podczas trzymania
        final Runnable heightUp_hold = new Runnable()
        {
            @Override
            public void run()
            {
                Log.i("DroneControl","Holding heightUp");
                String msg = "UP";
                ClientTask task = new ClientTask(address);
                task.execute(msg);
                // II
                //mHandler.postDelayed(this,100);
            }
        };

        // heightDown
        // Akcja wykonywana podczas trzymania
        final Runnable heightDown_hold = new Runnable()
        {
            @Override
            public void run()
            {
                Log.i("DroneControl","Holding heightDown");
                String msg = "DOWN";
                ClientTask task = new ClientTask(address);
                task.execute(msg);
                // II
                //mHandler.postDelayed(this,100);
            }
        };

        // metody onTouchListener dla przycisków sterujących dronem

        // arrowUp - przechył do przodu
        arrowUp.setOnTouchListener(new View.OnTouchListener()
        {
            @Override
            public boolean onTouch(View v, MotionEvent event)
            {
                // Wysłanie komendy przez Wi-Fi
                switch (event.getAction())
                {
                    case MotionEvent.ACTION_DOWN:
                        mHandler.postDelayed(arrowUp_hold,10);
                        mHandler.removeCallbacks(stopMove);
                        return true;

                    case MotionEvent.ACTION_UP:
                        mHandler.postDelayed(stopMove,10);
                        mHandler.removeCallbacks(arrowUp_hold);
                        return true;

                    default:
                        break;
                }
                return true;
            }
        });

        // arrowDown - przechył do tyłu
        arrowDown.setOnTouchListener(new View.OnTouchListener()
        {
            @Override
            public boolean onTouch(View v, MotionEvent event)
            {
                // Wysłanie komendy przez Wi-Fi
                switch (event.getAction())
                {
                    case MotionEvent.ACTION_DOWN:
                        mHandler.postDelayed(arrowDown_hold,10);
                        mHandler.removeCallbacks(stopMove);
                        return true;

                    case MotionEvent.ACTION_UP:
                        mHandler.postDelayed(stopMove,10);
                        mHandler.removeCallbacks(arrowDown_hold);
                        return true;

                    default:
                        break;
                }
                return true;
            }
        });

        // arrowLeft - przechył w lewo
        arrowLeft.setOnTouchListener(new View.OnTouchListener()
        {
            @Override
            public boolean onTouch(View v, MotionEvent event)
            {
                // Wysłanie komendy przez Wi-Fi
                switch (event.getAction())
                {
                    case MotionEvent.ACTION_DOWN:
                        mHandler.postDelayed(arrowLeft_hold,10);
                        mHandler.removeCallbacks(stopMove);
                        return true;

                    case MotionEvent.ACTION_UP:
                        mHandler.postDelayed(stopMove,10);
                        mHandler.removeCallbacks(arrowLeft_hold);
                        return true;

                    default:
                        break;
                }
                return true;
            }
        });

        // arrowRight - przechył w prawo
        arrowRight.setOnTouchListener(new View.OnTouchListener()
        {
            @Override
            public boolean onTouch(View v, MotionEvent event)
            {
                // Wysłanie komendy przez Wi-Fi
                switch (event.getAction())
                {
                    case MotionEvent.ACTION_DOWN:
                        mHandler.postDelayed(arrowRight_hold,10);
                        mHandler.removeCallbacks(stopMove);
                        return true;

                    case MotionEvent.ACTION_UP:
                        mHandler.postDelayed(stopMove,10);
                        mHandler.removeCallbacks(arrowRight_hold);
                        return true;

                    default:
                        break;
                }
                return true;
            }
        });

        // rotateLeft - obrót w lewo wokół osi pionowej
        rotateLeft.setOnTouchListener(new View.OnTouchListener()
        {
            @Override
            public boolean onTouch(View v, MotionEvent event)
            {
                // Wysłanie komendy przez Wi-Fi
                switch (event.getAction())
                {
                    case MotionEvent.ACTION_DOWN:
                        mHandler.postDelayed(rotateLeft_hold,10);
                        mHandler.removeCallbacks(stopMove);
                        return true;

                    case MotionEvent.ACTION_UP:
                        mHandler.postDelayed(stopMove,10);
                        mHandler.removeCallbacks(rotateLeft_hold);
                        return true;

                    default:
                        break;
                }
                return true;
            }
        });

        // rotateRight - obrót w prawo wokół osi pionowej
        rotateRight.setOnTouchListener(new View.OnTouchListener()
        {
            @Override
            public boolean onTouch(View v, MotionEvent event)
            {
                // Wysłanie komendy przez Wi-Fi
                switch (event.getAction())
                {
                    case MotionEvent.ACTION_DOWN:
                        mHandler.postDelayed(rotateRight_hold,10);
                        mHandler.removeCallbacks(stopMove);
                        return true;

                    case MotionEvent.ACTION_UP:
                        mHandler.postDelayed(stopMove,10);
                        mHandler.removeCallbacks(rotateRight_hold);
                        return true;

                    default:
                        break;
                }
                return true;
            }
        });

        // heightUp - zwiększenie pułapu lotu
        heightUp.setOnTouchListener(new View.OnTouchListener()
        {
            @Override
            public boolean onTouch(View v, MotionEvent event)
            {
                // Wysłanie komendy przez Wi-Fi
                switch (event.getAction())
                {
                    case MotionEvent.ACTION_DOWN:
                        mHandler.postDelayed(heightUp_hold,10);
                        mHandler.removeCallbacks(stopMove);
                        return true;

                    case MotionEvent.ACTION_UP:
                        mHandler.postDelayed(stopMove,10);
                        mHandler.removeCallbacks(heightUp_hold);
                        return true;

                    default:
                        break;
                }
                return true;
            }
        });

        // heightDown - zmniejszenie pułapu lotu
        heightDown.setOnTouchListener(new View.OnTouchListener()
        {
            @Override
            public boolean onTouch(View v, MotionEvent event)
            {
                // Wysłanie komendy przez Wi-Fi
                switch (event.getAction())
                {
                    case MotionEvent.ACTION_DOWN:
                        mHandler.postDelayed(heightDown_hold,10);
                        mHandler.removeCallbacks(stopMove);
                        return true;

                    case MotionEvent.ACTION_UP:
                        mHandler.postDelayed(stopMove,10);
                        mHandler.removeCallbacks(heightDown_hold);
                        return true;

                    default:
                        break;
                }
                return true;
            }
        });

        // engines - sterowanie silnikami
        engines.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                // Silniki są wyłączone - naciśnięcie powoduje włączenie
                if (!isEnginePoweredOn)
                {
                    // Zmień wartość zmiennej logicznej
                    isEnginePoweredOn = true;

                    // Wyślij komendę uruchomienia silników
                    String msg = "POWER";
                    ClientTask task = new ClientTask(address);
                    task.execute(msg);

                    // Zmień wyświetlaną ikonę
                    ((ImageButton)v).setImageResource(R.drawable.power_on);

                    // Informacja o włączeniu silników
                    Toast.makeText(MainActivity.this, R.string.EnginesPoweredOn, Toast.LENGTH_SHORT).show();
                }
                else
                {
                    // Silniki są włączone - naciśnięcie powoduje wyłączenie
                    // Zmień wartość zmiennej logicznej
                    isEnginePoweredOn = false;

                    // Wyślij komendę wyłączenia silników
                    String msg = "POWER";
                    ClientTask task = new ClientTask(address);
                    task.execute(msg);

                    // Zmień wyświetlaną ikonę
                    ((ImageButton)v).setImageResource(R.drawable.power_off);

                    // Informacja o wyłączeniu silników
                    Toast.makeText(MainActivity.this, R.string.EnginesPoweredOff, Toast.LENGTH_SHORT).show();
                }
            }
        });

        // settingsButton - przejście do ustawień
        settingsButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                // TODO - AppSettings
            }
        });
    }

    /**
     * ClientTask
     *
     * Klasa do komunikacji HTTP
     */
    public static class ClientTask extends AsyncTask<String,Void,String>
    {

        String server;

        ClientTask(String server)
        {
            this.server = server;
        }

        @Override
        protected String doInBackground(String... params)
        {

            StringBuilder chaine = new StringBuilder("");

            final String val = params[0];
            final String p = "http://"+ server+"/"+val;

//            runOnUiThread(new Runnable()
//            {
//                @Override
//                public void run()
//                {
//                    estado.setText(p);
//                }
//            });

            String serverResponse = "";
            try
            {
                URL url = new URL(p);
                HttpURLConnection connection = (HttpURLConnection)url.openConnection();
                connection.setRequestMethod("GET");
                connection.connect();
                InputStream inputStream = connection.getInputStream();

                BufferedReader rd = new BufferedReader(new InputStreamReader(inputStream));
                String line;
                while ((line = rd.readLine()) != null)
                {
                    chaine.append(line);
                }
                inputStream.close();

                System.out.println("chaine: " + chaine.toString());

                connection.disconnect();

            }
            catch (IOException e)
            {
                e.printStackTrace();
                serverResponse = e.getMessage();
            }

            return serverResponse;
        }

        @Override
        protected void onPostExecute(String s)
        {

        }
    }

    // Zapisywanie bieżącego stanu silników w razie zmiany orientacji
    @Override
    protected void onSaveInstanceState(Bundle outState)
    {
        super.onSaveInstanceState(outState);

        // Zapisz bieżący stan silników
        outState.putBoolean(ENGINE_STATE, isEnginePoweredOn);
    }
}