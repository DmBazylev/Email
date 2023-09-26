package com.example.email;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.IOException;
import java.util.Properties;

import javax.mail.FetchProfile;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.internet.MimeBodyPart;

//класс для почты Akuznecov2023@mail.ru - только получает список входящих писем - подпапка письма самому себе
public class Akuznecov2023Activity extends AppCompatActivity {

    String user = "~";//почта (не логин) с которой получаем письмо
    String password = "~";//новый специальный пароль для внешнего приложения
    String host = "~";//host для mail.ru для imap

    ListView list_view = null;
    String [] string_list_messages=null;
    Message[] messages_toMyself =null;

    Context context=null;

    Button button_get_list_messages = null;
    Button buttonExit = null;
    TextView txtView =null;

    Folder inbox =null;
    Folder toMyself =null;
    Store store =null;

//    Порты для IMAP:
//    порт 143 — без шифрования,
//    порт 993 — SSL IMAP-порт, или IMAPS.

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.akuznecov2023);

        button_get_list_messages = findViewById(R.id.my_button_get_list_messages);
        buttonExit = findViewById(R.id.my_button_exit);

        list_view = (ListView) findViewById(R.id.list_messages_on_mail);


        context=this;//для звука

        button_get_list_messages.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Обработка нажатия
                get_mail();
            }
        });

        buttonExit.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                //создаем дочерний поток
                Thread thread = new Thread(new Runnable() {

                    @Override
                    public void run() //в теле переопределенного метода run() выполняется работа в отдельном потоке
                    {
                        //======================================работа в дочернем потоке========================================================

                        try {
                            if (inbox!=null)
                            {
                                inbox.close(false);//закрываем папку входящих сообщений с флагом false
                            }
                            if (store!=null)
                            {
                                store.close();//закрываем Store
                            }


                        } catch (MessagingException e) {
                            e.printStackTrace();
                        }


                        // Обработка нажатия
                        finishAffinity();//выход из приложения - то есть из всех активностей
                        //=========================================остаток кода дочернего потока=====================================================
                    }
                });

                thread.start();//запуск потока (который отдельный)
            }
        });

    }

    public void get_mail() {

        //создаем дочерний поток
        Thread thread = new Thread(new Runnable() {

            @Override
            public void run() //в теле переопределенного метода run() выполняется работа в отдельном потоке
            {
                //======================================работа в дочернем потоке========================================================

                try {

                    Properties properties = new Properties();//создаем свойства
                    //для использования обычного протокола imap надо на самом почтовом сервере mail.ru разрешить доступ по этому протоколу,
                    // иначе можно будет только по зашифрованному протоколу imaps
                    properties.put("mail.imap.host", host);//используем протокол imap для mail.ru
                    properties.put("mail.imap.port", "143");//для протокола imap используется порт 143
//                  properties.put("mail.imap.port", "993");//для протокола зашифрованного imaps используется порт 993
//                  properties.put("mail.imap.starttls.enable", "true");//для зашифрованного протокола  imaps
//                  properties.put("mail.imap.ssl.enable", "true");//для зашифрованного протокола  imaps

                    Session emailSession = Session.getDefaultInstance(properties);//открываем сессию на основе этих свойств
                    //Store store = emailSession.getStore("imap");//на основе сессии создаем Store - так дает ошибку - не входит на почтовый сервер
                    store = emailSession.getStore("imaps");//на основе сессии создаем Store
                    store.connect(host, user, password);//связываем Store  с почтой где user - почта (но не логин!) а password пароль к почте

                    // Папка входящих сообщений, "INBOX" - это папка входящих сообщений
                    inbox = store.getFolder("INBOX");//из этого Store берем папку входящих сообщений Inbox -папка входящих сообщений
                    // Открываем папку в режиме только для чтения
                    inbox.open(Folder.READ_ONLY);//открываем ее для чтения

 //=========================================папки внутри папки входящие Inbox==================================================
//                    I/System.out: >> Social//социальные сети
//                            >> Newsletters//рассылки
//                            >> ToMyself//письма себе
//                            >> News//новости
//                    I/System.out: >> Receipts//чеки

                    toMyself =   inbox.getFolder("ToMyself");//берем папку Письма себе

                    toMyself.open(Folder.READ_ONLY);//открываем ее для чтения

                     messages_toMyself = toMyself.getMessages();//берем все сообщения из папки входящие

                    Log.d("TAG", "количество сообщений в папке toMyself: "+messages_toMyself.length);//+String.valueOf(messages.length)

                    FetchProfile fetchProfile = new FetchProfile();//профиль извлечения
              //      fetchProfile.add(FetchProfile.Item.ENVELOPE);//добавляем в профиль извлечения информацию о конверте
                    fetchProfile.add(FetchProfile.Item.CONTENT_INFO);//добавляем в профиль извлечения информацию о содержании

                    toMyself.fetch(messages_toMyself, fetchProfile);//выбираем из писем из папки информацию о содержании

                    String strToCutOff = "/storage/emulated/0/Android/data/com.example.bird/files/";//строка которую отрезают от начала названия файла - в разных телефонах может быть разная строка

                    int len=messages_toMyself.length;
                    string_list_messages=new String[len];

                    for (int s=len-1;s>=0;--s)
                    {
                        Multipart multiPart = (Multipart) messages_toMyself[s].getContent();
                        MimeBodyPart part = (MimeBodyPart) multiPart.getBodyPart(1);
                        string_list_messages[s]= part.getFileName().substring(strToCutOff.length());
                    }

//                    inbox.close(false);//закрываем папку входящих сообщений с флагом false
//                    store.close();//закрываем Store
                } catch (MessagingException exp) {
                    exp.printStackTrace();
                } catch (Exception exp) {
                    exp.printStackTrace();
                }


                //=========================================остаток кода дочернего потока=====================================================
                Log.d("", "конец метода получения почты");

            }
        });

        thread.start();//запуск потока (который отдельный)

        //ждем пока данные скачаются с сервера
        try {
            thread.join();//метод join - осуществляет ожидание пока этот поток (thread) умрет (закончится)
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.out.println("работа потока закончена выводим полученный массив:");

       int reverse_len=string_list_messages.length;
        String [] reverse= new String[reverse_len];

        for (int r=reverse_len-1,s=0;r>=0;--r,++s)
        {
           // System.out.println(r+" "+string_list_messages[r]);
            reverse[r]=string_list_messages[s];
        }

        System.out.println("создаем адаптер");
        // создаем адаптер- android.R.layout.simple_list_item_1 – это системный layout-файл, который представляет собой TextView.

               ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, reverse);
        // ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, string_list_messages);

        System.out.println("устанавливаем адаптер списку");
        // устанавливаем адаптер списку
        list_view.setAdapter(adapter);

        String str_cut = "/storage/emulated/0/Android/data/com.example.bird/files/";//строка которую отрезают от начала названия файла - в разных телефонах может быть разная строка

        System.out.println("устанавливаем обработчик нажатия на лист");
        //обработчик нажатия на лист - при нажатии файл в этом письме скачивается на телефон
        list_view.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                //создаем дочерний поток
                Thread thread = new Thread(new Runnable() {

                    @Override
                    public void run() //в теле переопределенного метода run() выполняется работа в отдельном потоке
                    {
                        //======================================работа в дочернем потоке========================================================
//                        System.out.println("position " + position);
//                        System.out.println("id " + id);
//                        System.out.println("messages_toMyself[position] " + messages_toMyself[position]);
//                        System.out.println("messages_toMyself[reverse.length-1- position] " + messages_toMyself[reverse.length - 1 - position]);

                        Multipart multiPart = null;
                        try {
                            multiPart = (Multipart) messages_toMyself[reverse.length - 1 - position].getContent();
                            MimeBodyPart part = (MimeBodyPart) multiPart.getBodyPart(1);
                            System.out.println("part.getFileName().substring(str_cut.length()) " + part.getFileName().substring(str_cut.length()));
                            part.saveFile("/storage/emulated/0/Download/" + part.getFileName().substring(str_cut.length()));//сохраняем полученную часть вложения в виде файла в файл по определенному пути

                            Log.d("", "файл сохранен во внешнее хранилище");
                            //==============воспроизведение звука после скачивания файла с почты=============================================================
                            try {
                                //  TYPE_ALARM -длинная мелодия будильника
                                //TYPE_NOTIFICATION - СМС уведомление
                                Uri notify = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                                //  Ringtone r = RingtoneManager.getRingtone(getApplicationContext(), notify);
                                Ringtone r = RingtoneManager.getRingtone(context, notify);
                                r.play();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }

                        } catch (IOException e) {
                            e.printStackTrace();
                        } catch (MessagingException e) {
                            e.printStackTrace();
                        }
                        //=========================================остаток кода дочернего потока=====================================================
                        Log.d("", "конец метода получения почты");

                    }
                });

                thread.start();//запуск потока (который отдельный)

            }
        });
    }


}