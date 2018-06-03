package com.danielm96.dronecontrol;

import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;

/**
 * TcpClient.java
 *
 * Klasa definiująca klienta TCP/IP do komunikacji z modułem ESP8266.
 */

public class TcpClient
{
    // Stałe
    // Nazwa klasy
    private static final String TAG = TcpClient.class.getSimpleName();
    // Adres IP serwera - może się zmienić
    private static final String SERVER_IP = "192.168.4.1";
    // Numer portu
    private static final int SERVER_PORT = 28994;

    // Wiadomość wysyłana do serwera
    private String mServerMessage;
    // Wysyłanie potwierdzenie odczytu
    private OnMessageReceived mMessageListener;
    // Działanie serwera - true = działa
    private boolean mRun = false;
    // Do wysyłania wiadomości
    private PrintWriter mBufferOut;
    // Do odczytywania wiadomości
    private BufferedReader mBufferIn;

    // Konstruktor
    TcpClient(OnMessageReceived listener)
    {
        mMessageListener = listener;
    }

    // Wysłanie wiadomości do serwera
    public void sendMessage(final String message)
    {
        Runnable runnable = new Runnable()
        {
            @Override
            public void run()
            {
                if (mBufferOut != null)
                {
                    Log.d(TAG, "Sending: " + message);
                    mBufferOut.println(message);
                    mBufferOut.flush();
                }
            }
        };
        Thread thread = new Thread(runnable);
        thread.start();
    }

    // Zamykanie połączenia
    public void stopClient()
    {
        // Serwer już nie musi działać
        mRun = false;

        // Zamykanie bufora
        if (mBufferOut != null)
        {
            mBufferOut.flush();
            mBufferOut.close();
        }

        // Zerowanie listenera, bufora i wiadomości
        mMessageListener = null;
        mBufferIn = null;
        mBufferOut = null;
        mServerMessage = null;
    }

    // Uruchamianie klienta TCP/IP
    public void run()
    {
        // Serwer zaczyna działać
        mRun = true;

        // Nawiązywanie komunikacji z serwerem
        try
        {
            // Uzyskiwanie adresu IP
            InetAddress serverAddr = InetAddress.getByName(SERVER_IP);
            Log.d("TCP", "Connecting...");

            // Tworzenie socketa do kommunikacji z serwerem
            Socket socket = new Socket(serverAddr, SERVER_PORT);

            // Właściwa komunikacja
            try
            {
                // Wysyłanie wiadomości do serwera
                mBufferOut = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);

                // Odbieranie wiadomości z serwera
                mBufferIn = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                // Nasłuchiwanie wiadomości wysyłanych przez serwer
                while (mRun)
                {
                    mServerMessage = mBufferIn.readLine();

                    if (mServerMessage != null && mMessageListener != null)
                    {
                        // Wiadomość nie jest pusta, wywołaj listener
                        mMessageListener.messageReceived(mServerMessage);
                    }
                }
                Log.d("TCP", "Received from server: " + mServerMessage);
            }
            catch (Exception e)
            {
                Log.e("TCP", "Error during communication.");
            }
            finally
            {
                // Socket należy zamknąć. Ponowne użycie nie jest możliwe.
                socket.close();
            }
        }
        catch (Exception e)
        {
            Log.e("TCP", "Error while establishing connection.");
        }
    }

    /**
     * OnMessageReceived
     *
     * Interfejs konieczny do nasłuchiwania otrzymania wiadomości przez serwer.
     */
    public interface OnMessageReceived
    {
        void messageReceived(String message);
    }
}