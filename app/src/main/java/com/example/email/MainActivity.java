package com.example.email;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;

//========================================================
import android.util.Log;
import com.sun.mail.pop3.POP3Store;

import java.io.IOException;
import java.util.Properties;

import javax.mail.FetchProfile;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.NoSuchProviderException;
import javax.mail.Session;
import javax.mail.Store;

import javax.mail.BodyPart;
import javax.mail.Multipart;
import javax.mail.internet.MimeBodyPart;
import javax.mail.Part;

import java.io.IOException;
import android.database.SQLException;
import android.database.sqlite.SQLiteException;
import java.io.File;

import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;
import android.content.Context;
import javax.xml.transform.Result;


public class MainActivity extends AppCompatActivity {



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //================================================================================================

        while (!checkAllPermissons()) //установка нескольких разрешений
        {
            requestAllPermissions();
        }


    }


//==============================================методы для добавления нескольких разрешений============================================

    //если хоть одного из нижеперечисленных разрешений нет
    public boolean checkAllPermissons() {
        if ( ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
            return false;//то возвращаем false
        else
            return true;//если уже установлены все разрешения
    }

    //получение (запрос) нижеперечисленных разрешений
    public void requestAllPermissions() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                111);
    }


    //показ табло с клавишей для получения  нужного разрешения
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        // Checking whether user granted the permission or not.
        for (int i = 0; i < grantResults.length; i++) {
            if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                showAppSettings();
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    public void showAppSettings() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + getPackageName()));
        startActivity(intent);
    }
//================================================================================================================

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) //колл-бэк функция, срабатывает при вызове метода  startActivityForResult(intent, REQUEST_CODE); по коду  REQUEST_CODE можно определить кто вызвал метод
    {
        super.onActivityResult(requestCode, resultCode, data);

    }

}