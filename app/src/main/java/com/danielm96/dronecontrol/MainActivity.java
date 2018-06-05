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
    static TcpClient mTcpClient;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // Informacja o wersji beta
        // showBetaDialog();

        // Jeśli przyznano uprawnienia, to kontynuuj działanie
        if (checkPermissions())
        {
            startApp();
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
        settingsButton = findViewById(R.id.settingsButton);

        // TODO - Komunikacja przez Wi-Fi
        new ConnectTask().execute();

        // Wątek wykonujący pewne zadania
        final Handler mHandler = new Handler();

        // Wykorzystanie wątku mHandler dla przycisków sterujących
        // _hold - akcja wykonywana podczas trzymania przycisku
        // _release - akcja wykonywana przy puszczeniu przycisku

        // Wariant I
        // Przy trzymaniu, wykonuj co 100 ms.

        // Wariant II
        // Wykonaj tylko raz (nie ma metody postDelayed).

        // arrowUp
        // Akcja wykonywana podczas trzymania
        final Runnable arrowUp_hold = new Runnable()
        {
            @Override
            public void run()
            {
                Log.i("DroneControl","Holding arrowUp");
                if (mTcpClient != null)
                {
                    new SendMessageTask().execute("MOVE_FORWARD");
                }
                // Opóźnienie wykonania akcji [ms]
                // II
                // mHandler.postDelayed(this,100);
            }
        };

        // Akcja wykonywana przy puszczeniu
        final Runnable arrowUp_release = new Runnable()
        {
            @Override
            public void run()
            {
                Log.i("DroneControl","Released arrowUp");
                new SendMessageTask().execute("STOP");
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
                // II
                //mHandler.postDelayed(this,100);
            }
        };

        // Akcja wykonywana przy puszczeniu
        final Runnable arrowDown_release = new Runnable()
        {
            @Override
            public void run()
            {
                Log.i("DroneControl","Released arrowDown");
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
                // II
                //mHandler.postDelayed(this,100);
            }
        };

        // Akcja wykonywana przy puszczeniu
        final Runnable arrowLeft_release = new Runnable()
        {
            @Override
            public void run()
            {
                Log.i("DroneControl","Released arrowLeft");
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
                // II
                //mHandler.postDelayed(this,100);
            }
        };

        // Akcja wykonywana przy puszczeniu
        final Runnable arrowRight_release = new Runnable()
        {
            @Override
            public void run()
            {
                Log.i("DroneControl","Released arrowRight");
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
                // II
                //mHandler.postDelayed(this,100);
            }
        };

        // Akcja wykonywana przy puszczeniu
        final Runnable rotateLeft_release = new Runnable()
        {
            @Override
            public void run()
            {
                Log.i("DroneControl","Released rotateLeft");
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
                // II
                //mHandler.postDelayed(this,100);
            }
        };

        // Akcja wykonywana przy puszczeniu
        final Runnable rotateRight_release = new Runnable()
        {
            @Override
            public void run()
            {
                Log.i("DroneControl","Released rotateRight");
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
                // II
                //mHandler.postDelayed(this,100);
            }
        };

        // Akcja wykonywana przy puszczeniu
        final Runnable heightUp_release = new Runnable()
        {
            @Override
            public void run()
            {
                Log.i("DroneControl","Released heightUp");
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
                // II
                //mHandler.postDelayed(this,100);
            }
        };

        // Akcja wykonywana przy puszczeniu
        final Runnable heightDown_release = new Runnable()
        {
            @Override
            public void run()
            {
                Log.i("DroneControl","Released heightDown");
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
                        mHandler.removeCallbacks(arrowUp_release);
                        return true;

                    case MotionEvent.ACTION_UP:
                        mHandler.postDelayed(arrowUp_release,10);
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
                        mHandler.removeCallbacks(arrowDown_release);
                        return true;

                    case MotionEvent.ACTION_UP:
                        mHandler.postDelayed(arrowDown_release,10);
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
                        mHandler.removeCallbacks(arrowLeft_release);
                        return true;

                    case MotionEvent.ACTION_UP:
                        mHandler.postDelayed(arrowLeft_release,10);
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
                        mHandler.removeCallbacks(arrowRight_release);
                        return true;

                    case MotionEvent.ACTION_UP:
                        mHandler.postDelayed(arrowRight_release,10);
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
                        mHandler.removeCallbacks(rotateLeft_release);
                        return true;

                    case MotionEvent.ACTION_UP:
                        mHandler.postDelayed(rotateLeft_release,10);
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
                        mHandler.removeCallbacks(rotateRight_release);
                        return true;

                    case MotionEvent.ACTION_UP:
                        mHandler.postDelayed(rotateRight_release,10);
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
                        mHandler.removeCallbacks(heightUp_release);
                        return true;

                    case MotionEvent.ACTION_UP:
                        mHandler.postDelayed(heightUp_release,10);
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
                        mHandler.removeCallbacks(heightDown_release);
                        return true;

                    case MotionEvent.ACTION_UP:
                        mHandler.postDelayed(heightDown_release,10);
                        mHandler.removeCallbacks(heightDown_hold);
                        return true;

                    default:
                        break;
                }
                return true;
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
     * ConnectTask
     *
     * Klasa do łączenia z serwerem w tle.
     */

    public static class ConnectTask extends AsyncTask<String, String, TcpClient>
    {
        @Override
        protected TcpClient doInBackground(String... message)
        {
            // Tworzenie obiektu TcpClient
            mTcpClient = new TcpClient(new TcpClient.OnMessageReceived()
            {
                @Override
                public void messageReceived(String message)
                {
                    publishProgress(message);
                }
            });
            Log.d("TCP", "3,2,1,...");
            mTcpClient.run();
            if (mTcpClient != null)
            {
                Log.d("TCP", "Go!");
                mTcpClient.sendMessage("Started");
            }

            return null;
        }

        @Override
        protected void onProgressUpdate(String... values)
        {
            super.onProgressUpdate(values);
            Log.d("test", "Response: " + values[0]);
        }
    }

    /**
     * DisconnectTask
     *
     * Klasa do rozłączenia z serwerem w tle.
     */

    public static class DisconnectTask extends AsyncTask<Void, Void, Void>
    {
        @Override
        protected Void doInBackground(Void... voids)
        {
            mTcpClient.stopClient();
            mTcpClient = null;

            return null;
        }

        @Override
        protected void onPostExecute(Void nothing)
        {
            super.onPostExecute(nothing);
        }
    }

    /**
     * SendMessageTask
     *
     * Klasa do wysyłania wiadomości w tle.
     */

    public static class SendMessageTask extends AsyncTask<String, Void, Void>
    {
        @Override
        protected Void doInBackground(String... params)
        {
            mTcpClient.sendMessage(params[0]);
            return null;
        }

        @Override
        protected void onPostExecute(Void nothing)
        {
            super.onPostExecute(nothing);
        }
    }
}