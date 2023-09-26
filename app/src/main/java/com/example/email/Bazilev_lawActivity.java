package com.example.email;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import android.util.Base64;
import java.util.Properties;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.Address;
import javax.mail.BodyPart;
import javax.mail.FetchProfile;
import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.internet.MimeUtility;

import org.jsoup.Jsoup;

//класс для почты bazilev_law@mail.ru - получает входящие может их читать скачивать файлы может отвечать перенаправлять, удалять
public class Bazilev_lawActivity extends AppCompatActivity {

    String user = "~";//почта (не логин) с которой получаем письмо
    String password = "~";//специальный пароль для внешнего приложения
    String host = "~";//host для mail.ru для imap

    ListView list_view = null;
    String [] string_list_messages=null;
    Message[] messages_inbox =null;
    static Message[] messages_inbox_last_100 =null;//последние 100 входящих писем - используем статический массив для использования в тч в статических методах


   static Context context=null;//статический контекст для звука в статическом методе скачивания файла

    Button button_get_list_messages = null;
    Button button_send_mail = null;
    Button buttonExit = null;
    TextView txtView =null;

    Folder inbox =null;
    Store store =null;

//    Порты для IMAP:
//    порт 143 — без шифрования,
//    порт 993 — SSL IMAP-порт, или IMAPS.

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.akuznecov2023);

        button_get_list_messages = findViewById(R.id.my_button_get_list_messages);
        button_send_mail= findViewById(R.id.my_button_send_mail);
        buttonExit = findViewById(R.id.my_button_exit);

        list_view = (ListView) findViewById(R.id.list_messages_on_mail);


        button_get_list_messages.setTextSize(12);//размер шрифта для кнопки в sp - так гораздо лучше (шрифт 12sp)
        button_send_mail.setTextSize(12);//размер шрифта для кнопки в sp
        buttonExit.setTextSize(12);//размер шрифта для кнопки в sp


        context=this;//для звука

        button_get_list_messages.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Обработка нажатия
                try {
                    get_mail();
                } catch (MessagingException | IOException e) {
                    e.printStackTrace();
                }
            }
        });

        button_send_mail.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Обработка нажатия
                // Выполняем переход на другой экран:
                Intent intent = new Intent(Bazilev_lawActivity.this, Send_messageActivity.class);
                startActivity(intent);
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

                        //перед выходов закрывает папку и Store (если  они были инициализированы) - поскольку закрытие обращается к почтовому серверу это нужно делать в отдельном потоке
                        try {
                            if (inbox!=null)
                            {
                                inbox.close(true);//закрываем папку входящих сообщений с флагом true - флаг true нужен чтоб есть при удалении какое лио сообщение пометили флагом для удаления оно при закрытии папки удалилось бы
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

    public void get_mail() throws MessagingException, IOException {

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
                    // Store store = emailSession.getStore("imap");//на основе сессии создаем Store - так дает ошибку - не входит на почтовый сервер
                    store = emailSession.getStore("imaps");//на основе сессии создаем Store, "imaps" - это зашифрованный протокол
                    store.connect(host, user, password);//связываем Store  с почтой где user - почта (но не логин!) а password пароль к почте

                    // Папка входящих сообщений, "INBOX" - это папка входящих сообщений
                    inbox = store.getFolder("INBOX");//из этого Store берем папку входящих сообщений Inbox -папка входящих сообщений
                    // Открываем папку в режиме только для чтения
                    inbox.open(Folder.READ_WRITE);//открываем ее для чтения и записи - запись нужно для возможности удаления сообщения
                     messages_inbox = inbox.getMessages();//берем все сообщения из папки входящие

                  //  messages_inbox_last_100 = inbox.getMessages(messages_inbox.length-101, messages_inbox.length-1);//берем сообщения начиная с messages_inbox.length-101 до messages_inbox.length-1 из папки входящие
                    messages_inbox_last_100 = inbox.getMessages(messages_inbox.length-100, messages_inbox.length);//ТАК  ПРАВИЛЬНО (как я понял сообщения идут с 1 до количества сообщений, а не с 0 до кол-во -1) берем сообщения начиная с messages_inbox.length-100 до messages_inbox.length из папки входящие

               //     Log.d("TAG", "количество сообщений в папке messages_inbox_last_100: "+messages_inbox_last_100.length);//+String.valueOf(messages.length)

                    FetchProfile fetchProfile = new FetchProfile();//профиль извлечения
                    fetchProfile.add(FetchProfile.Item.ENVELOPE);//добавляем в профиль извлечения информацию о конверте (кому, куда и тд)
                    fetchProfile.add(FetchProfile.Item.CONTENT_INFO);//добавляем в профиль извлечения информацию о содержании (названия файлов, субъект и тд)

                    inbox.fetch(messages_inbox_last_100, fetchProfile);//выбираем из писем из папки информацию о конверте и информацю о содержании

                 //   String strToCutOff = "/storage/emulated/0/Android/data/com.example.bird/files/";//строка которую отрезают от начала названия файла - в разных телефонах может быть разная строка

                    int len=messages_inbox_last_100.length;
                    string_list_messages=new String[len];
                    String str="";

                //    int len_inbox=messages_inbox.length;

//заполняем лист названий сообщений (последние 100 входящих сообщений) - откуда+субъект
                    for (int s = len - 1; s >= (len - 101); --s) {
                        string_list_messages[s] = string_handler(messages_inbox_last_100[s].getFrom()[0].toString()) + " " + messages_inbox_last_100[s].getSubject();
                    //    System.out.println(s+" for (int s = len - 1; s >= (len - 101); --s) "+string_list_messages[s]);
                    }

//здест папку не закрываем - закроем ее только при выходе из приложения
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

        //заполняем массив -обратный массиву листа названий сообдений только заом наперед сначало сообщение номер 100 и тд так до 1 чтоб самые верхние были самые свежие
        for (int r=reverse_len-1,s=0;r>=0;--r,++s)
        {
           // System.out.println(r+" "+string_list_messages[r]);
            reverse[r]=string_list_messages[s];
        }

        System.out.println("создаем адаптер");
        // создаем адаптер- android.R.layout.simple_list_item_1 – это системный layout-файл, который представляет собой TextView (шаблон простейшего вида TextView -звено для  ListView) .

        //стандартный шаблон элемента листа
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, reverse);//android.R.layout.simple_list_item_1 –стандарный элемент списка из системы

        //кастомный шаблон элемента листа
        //  ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, R.layout.listview_item, reverse);//устанавливаем в адаптер кастомный элемент списка

        System.out.println("устанавливаем адаптер списку");
        // устанавливаем адаптер списку
        list_view.setAdapter(adapter);

        System.out.println("устанавливаем обработчик нажатия на лист");
        //обработчик нажатия на лист
        list_view.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                Thread thread = new Thread(new Runnable() {

                    @RequiresApi(api = Build.VERSION_CODES.O)
                    @Override
                    public void run() //в теле переопределенного метода run() выполняется работа в отдельном потоке
                    {
                        //======================================работа в дочернем потоке========================================================

//                        System.out.println("reverse.length " + reverse.length);
//                        System.out.println("position " + position);

                        int message_number = reverse.length - 1 - position;//чтобы при нажатии получить номер сообщения на который нажал нужно от длины списка отнять 1 и номер позиии листа
                        String text_inline = "";
                        boolean isList_of_Files = false;//флаг если в сообщении есть файлы
                        String[] list_of_files = null;
                        ArrayList<String> arraylist_files = new ArrayList<String>();//

                        System.out.println("message_number " + message_number);

                        String contentType = null;//определяем тип контента этого сообщения
                        try {
                            contentType = messages_inbox_last_100[message_number].getContentType();
                        } catch (MessagingException e) {
                            e.printStackTrace();
                        }

                        System.out.println("contentType " + contentType);

                        // если содержание письма состоит из нескольких частей - содержание может состоять из приложений
                        if (contentType.contains("multipart")) {

                            // содержание может состоять из приложений
                            Multipart multiPart = null;
                            try {
                                multiPart = (Multipart) messages_inbox_last_100[message_number].getContent();

                                System.out.println("multiPart.getContentType() " + multiPart.getContentType());//тип мультипата
                                System.out.println("multiPart.getCount() " + multiPart.getCount());//количнство частей мультипата

                    //    MimeBodyPart temp;
                                for (int r = 0; r < multiPart.getCount(); ++r)//прокручиваем все части мультипата в цикле
                                {

                                    System.out.println("contentType.contains(\"multipart\") итерация номер " + r);

                                    // temp=(MimeBodyPart) multiPart.getBodyPart(r);
                                    BodyPart bodyPart = multiPart.getBodyPart(r);

                                    //встроеная часть мультипата
                                    if (Part.INLINE.equalsIgnoreCase(bodyPart.getDisposition())) {
                                        System.out.println("Part.INLINE.equalsIgnoreCase(temp.getDisposition())");

                                        // BodyPart part = multipart.getBodyPart(i);
                                        //Для html-сообщений создается две части, "text/plain" и "text/html" (для клиентов без возможности чтения html сообщений), так что если нам не важна разметка:

                                        if (bodyPart.isMimeType("text/plain")) {
                                             System.out.println("bodyPart.isMimeType(text/plain)");

                                            text_inline = bodyPart.getContent().toString();
                                              System.out.println("multipart Part.INLINE text/plain part.getContent().toString() " + bodyPart.getContent().toString());
                                        } else if (bodyPart.isMimeType("text/html")) {
                                            System.out.println("multipart Part.INLINE text/html part.getContent().toString() " + bodyPart.getContent().toString());

                                            text_inline = "\n" + Jsoup.parse(bodyPart.getContent().toString()).text();
                                        }
                                        else
                                        {
                                            System.out.println("multipart Part.INLINE другой тип part.getContent().toString() " + bodyPart.getContent().toString());

                                        }

//                                        arraylist_files.add(MimeUtility.decodeText(bodyPart.getFileName()));
//                                        System.out.println("Part.INLINE.equalsIgnoreCase(temp.getDisposition()) " + MimeUtility.decodeText(bodyPart.getFileName()));

                                    //приложение мультипата
                                    } else if (Part.ATTACHMENT.equalsIgnoreCase(bodyPart.getDisposition())) {
                                        System.out.println("Part.ATTACHMENT.equalsIgnoreCase(temp.getDisposition())");
                                        isList_of_Files = true;

                                        arraylist_files.add(MimeUtility.decodeText(bodyPart.getFileName()));//вставляем имя прикрепленного файла в приложении

                                        System.out.println("Part.ATTACHMENT.equalsIgnoreCase(temp.getDisposition()) " + MimeUtility.decodeText(bodyPart.getFileName()));

                                    }
                                    else//если ни встроенаая часть ни приложение то это значит мультипат - т.е. мультипат вложенный в мультипат (таких мультипататов в мультипате может быть много)
                                    {

                                        System.out.println(" НИ Part.INLINE НИ Part.ATTACHMENT bodyPart.getDisposition() "+bodyPart.getDisposition());//здесть  скорее всего bodyPart.getDisposition() покажет мультипат

                                        try{
                                            System.out.println("text_inline=getTextFromMimeMultipart((MimeMultipart)bodyPart.getContent());");
                                            String result=getTextFromMimeMultipart((MimeMultipart)bodyPart.getContent());
                                            text_inline+= result;
                                            System.out.println("text_inline "+text_inline);
                                        }catch(ClassCastException ex)//если ОШИБКА java.lang.String cannot be cast to javax.mail.internet.MimeMultipart - те. нельзя преобразовать MimeMultipart то bodyPart скорее всего строка String
                                        {
                                            System.out.println("ОШИБКА java.lang.String cannot be cast to javax.mail.internet.MimeMultipart");
                                            String contentType_1=bodyPart.getContentType();
                                            System.out.println("bodyPart.getContentType() "+contentType_1);

                                            Object content = bodyPart.getContent();
                                            if (content instanceof String)//проверка является ли объект строкой, если то расшифровываем строку
                                            {
                                                String body = (String)content;
                                                System.out.println(" if (content instanceof String)  body"+body);
                                               if (contentType_1.contains("text/plain"))
                                               {
                                                   System.out.println(" TEXT/PLAIN");
                                                   text_inline +=body;
                                               }else if (contentType_1.contains("text/html"))
                                               {
                                                   System.out.println(" TEXT/HTML");
                                                   text_inline +=  "\n" + Jsoup.parse(body).text();
                                               }

                                            }
                                            else if (content instanceof Multipart)//если объект мультипат (но тут это как я понял редко)
                                            {
                                                Multipart mp = (Multipart)content;
                                                System.out.println(" else if (content instanceof Multipart) ");
                                            }

                                        }



                                    }


                                }

                            } catch (IOException e) {
                                e.printStackTrace();
                            } catch (MessagingException e) {
                                e.printStackTrace();
                            }

                        } else if (contentType.contains("text/plain")) {//если содержание простой текст

                            BodyPart bodyPart = null;
                            MimeBodyPart mimeBodyPart= null;
                            try {
                          //      System.out.println("не multipart contentType.contains(\"text/plain\")" );
                             String result = messages_inbox_last_100[message_number].getContent().toString();

                                System.out.println("result "+result);
                                text_inline+=result;
                                System.out.println("text_inline "+text_inline);

                            } catch (IOException e) {
                                e.printStackTrace();
                            } catch (MessagingException e) {
                                e.printStackTrace();
                            }


                        }else if (contentType.contains("text/html"))//если содержание  текст html
                        {

                            try {
                                String result =  "\n" + Jsoup.parse(messages_inbox_last_100[message_number].getContent().toString()).text();
                                System.out.println("result "+result);
                                text_inline+=result;
                                System.out.println("text_inline "+text_inline);

                            } catch (IOException e) {
                                e.printStackTrace();
                            } catch (MessagingException e) {
                                e.printStackTrace();
                            }

                            System.out.println("не multipart contentType.contains(\"text/html\")" );
                        }
                        else
                        {
                            System.out.println("не multipart другой тип contentType "+contentType);
                        }

//+++++++++++++++++++++++++++++++++++++++++++++++после получения текста переходим на активность самого сообщения++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++

                        // Выполняем переход на другой экран:
                        Intent intent = new Intent(Bazilev_lawActivity.this, MessageActivity.class);
                        //передаем
                        //     System.out.println("messages_inbox_last_100[reverse.length - 1 - position].getMessageNumber() " + messages_inbox_last_100[reverse.length - 1 - position].getMessageNumber());
                        intent.putExtra("message_number", message_number);
                        intent.putExtra("text_inline", text_inline);

                        intent.putExtra("isList_of_Files", isList_of_Files);

                        try {
                            intent.putExtra("answer_to_from_Bazilev_lawActivity",  string_handler_1(messages_inbox_last_100[message_number].getFrom()[0].toString()));
                        } catch (MessagingException e) {
                            e.printStackTrace();
                        }


                        if (isList_of_Files) {//если есть в сообщении файлы то получаем список этих файлов
                            // arraylist_files
                            list_of_files = new String[arraylist_files.size()];

                            //заполняем список файлов
                            for (int d = 0; d < arraylist_files.size(); ++d) {
                                System.out.println("arraylist_files.get(d) " + arraylist_files.get(d));
                                list_of_files[d] = arraylist_files.get(d);
                            }

                            //    list_of_files = (String[]) arraylist_files.toArray();

                            //передаем список файлов в другую активность
                            intent.putExtra("list_of_files", list_of_files);
                        }

                        startActivity(intent);

//+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++

                        //=========================================остаток кода дочернего потока=====================================================
                        Log.d("", "конец метода получения почты");

                    }
                });

                thread.start();//запуск потока (который отдельный)
            }
        });

    }

    //чтение текста из встроенного мультитипата
    private String getTextFromMimeMultipart(MimeMultipart mimeMultipart)  throws MessagingException, IOException{

        String result = "";
        int count = mimeMultipart.getCount();
        for (int i = 0; i < count; i++) {
            BodyPart bodyPart = mimeMultipart.getBodyPart(i);
            if (bodyPart.isMimeType("text/plain")) {//если часть мультипата обычный текст
                System.out.println("if (bodyPart.isMimeType(\"text/plain\"))");
                result = result + "\n" + bodyPart.getContent();
                System.out.println("result "+result);
                break; // without break same text appears twice in my tests
            } else if (bodyPart.isMimeType("text/html")) {//если часть мультипата  текст html
                System.out.println("else if (bodyPart.isMimeType(\"text/html\"))");
                String html = (String) bodyPart.getContent();
                result = result + "\n" + org.jsoup.Jsoup.parse(html).text();
                System.out.println("result "+result);
            } else if (bodyPart.getContent() instanceof MimeMultipart){//если часть мультипата  опять мультапат то
                System.out.println("else if (bodyPart.getContent() instanceof MimeMultipart)");
                result = result + getTextFromMimeMultipart((MimeMultipart)bodyPart.getContent());//рекурсивно вызываем этот же метод
                System.out.println("result "+result);
            }
        }
        return result;
    }




    //метод обрезает часть строки начинающуюся с "=?" и заканчивающуюся на "?="
    public String string_handler(String str_source)
    {
        String target ="";

        if (str_source.indexOf("=?")!=-1&&str_source.indexOf("?=")!=-1)
        {
            // начальный индекс включительно.
            int beginIndex = str_source.indexOf("=?");

            // конечный индекс, исключительный.
            int endIndex = str_source.lastIndexOf("?=");

            // заменяем цель пустой строкой
            target = str_source.replace(str_source.substring(beginIndex, endIndex+2), "");

            return target;
        }

        return str_source;


    }

    //метод возвращает часть строки между символами "<" и  ">" - для получения
    public String string_handler_1(String str_source)
    {
        String target ="";

        if (str_source.indexOf("<")!=-1&&str_source.indexOf(">")!=-1)
        {
            // начальный индекс включительно.
            int beginIndex = str_source.indexOf("<");

            // конечный индекс, исключительный.
            int endIndex = str_source.lastIndexOf(">");

            // заменяем цель пустой строкой
            target = str_source.substring(beginIndex+1,endIndex);

            return target;
        }

        return target;


    }




//статический метод по скачиванию файла с почты (используется из другой активности - для того и статический)
    //в статическом методе используются только статические поля класса
    public static void save_file_from_mail(int message_number, String file_name) {

        try {
            Multipart multiPart = (Multipart) messages_inbox_last_100[message_number].getContent();

            for (int r = 0; r < multiPart.getCount(); ++r)//прокручиваем все части мультипата в цикле
            {

                System.out.println("r " + r);


                BodyPart bodyPart = multiPart.getBodyPart(r);

                if (Part.ATTACHMENT.equalsIgnoreCase(bodyPart.getDisposition())) {

                    if (MimeUtility.decodeText(bodyPart.getFileName()).equals(file_name)) {

                        final int num = r;//создаем пременную специально для другого потока, для использования переменных в другом потоке нужно использовать ключевое слово final

                        Thread thread = new Thread(new Runnable() {

                            @Override
                            public void run() //в теле переопределенного метода run() выполняется работа в отдельном потоке
                            {
                                //======================================работа в дочернем потоке========================================================
                                //в отдельном потоке отлов исключения созданный в другом потоке не работает, нужно заново делать отлов исключения уже в этом новом потоке
                                try {
                                    System.out.println("MimeUtility.decodeText(bodyPart.getFileName()).equals(file_name)");
                                    MimeBodyPart mimeBodyPart = (MimeBodyPart) multiPart.getBodyPart(num);
                                    mimeBodyPart.saveFile("/storage/emulated/0/Download/" + file_name);//сохраняем полученную часть вложения в виде файла в файл по определенному пути
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

                        try {
                            thread.join();//метод join - осуществляет ожидание пока этот поток (thread) умрет (закончится)
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                        //==============воспроизведение звука после загрузки файла с почты=============================================================
                        try {//после загрузки файла воспроизводим звук

                            //TYPE_ALARM -длинная мелодия будильника
                            //TYPE_NOTIFICATION - СМС уведомление

                            Uri notify = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                            //  Ringtone r = RingtoneManager.getRingtone(getApplicationContext(), notify);
                            Ringtone rington = RingtoneManager.getRingtone(context, notify);

                            rington.play();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                    }

                    //      System.out.println("Part.ATTACHMENT.equalsIgnoreCase(temp.getDisposition()) " + MimeUtility.decodeText(bodyPart.getFileName()));

                }


            }


        } catch (IOException e) {
            e.printStackTrace();
        } catch (MessagingException e) {
            e.printStackTrace();
        }
    }

    //статический метод по удалению сообщения - статический для вызова из другой активности
    public static void delete_message(int message_number_to_delete) {

        Thread thread = new Thread(new Runnable() {

            @Override
            public void run() //в теле переопределенного метода run() выполняется работа в отдельном потоке
            {
                //======================================работа в дочернем потоке========================================================

                try {
                    messages_inbox_last_100[message_number_to_delete].setFlag(Flags.Flag.DELETED, true);//ставим соответствующий флаг, сообщения удалиться после закрытия папки
                } catch (MessagingException e) {
                    e.printStackTrace();
                }
                //=========================================остаток кода дочернего потока=====================================================
                Log.d("", "конец метода получения почты");

            }
        });

        thread.start();//запуск потока (который отдельный)

    }

    //статический метод по перенаправлению сообщения - вызывается из другого сообщения - в параметрах передается номер сообщения, почта адресат, текст сообщения, файлы при перенапрвлении не прикрепляю для этого лучше использовать просто отправку сообщения)
    public static void redirect_message(int message_number_to_redirect, String email_address_to_redirect,String text) {

        Thread thread = new Thread(new Runnable() {

            @Override
            public void run() //в теле переопределенного метода run() выполняется работа в отдельном потоке
            {
                //======================================работа в дочернем потоке========================================================
//                System.out.println("public static void redirect_message(int message_number_to_redirect, String email_address_to_redirect)");
//                System.out.println("message_number_to_redirect "+message_number_to_redirect);
//                System.out.println("email_address_to_redirect "+email_address_to_redirect);

                //используем mail.ru
                String to = email_address_to_redirect;//почта адресат (Кому)
                String from = "~";//почта отправитель
                final String username = "~";//имя пользователя почты отправителя (то что было при регистрации почты)
                final String password = "~";//специальный пароль для внешнего приложения

                String host = "smtp.mail.ru";//настройки javamail для mail.ru
                Properties props = new Properties();
                props.put("mail.smtp.auth", "true");
                props.put("mail.smtp.starttls.enable", "true");
                props.put("mail.smtp.host", host);

                props.put("mail.smtp.port", 587);//mail.ru работает с этим портом
                // props.put("mail.smtp.port", 25);//25 стандартный порт

                Session session = Session.getInstance(props,
                        new javax.mail.Authenticator() {
                            protected PasswordAuthentication getPasswordAuthentication() {
                                return new PasswordAuthentication(username, password);
                            }
                        });

                try {

                    Message message = new MimeMessage(session);
                    message.setFrom(new InternetAddress(from));
                    message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
                    message.setSubject(messages_inbox_last_100[message_number_to_redirect].getSubject());//тема сообщения - короткое название файла (тип (входящий, исходящий), номер телефона, дата, время)
                    System.out.println("messages_inbox_last_100[message_number_to_redirect].getSubject() "+messages_inbox_last_100[message_number_to_redirect].getSubject());

                    message.setText(text);

                    Transport.send(message);//отправка сообщения

                } catch (MessagingException e) {
                    System.out.println(e);
                    //отлов исключения при ошибке при отправке почты

                }


                //=========================================остаток кода дочернего потока=====================================================
                //  Log.d("", "конец метода получения почты");

            }
        });

        thread.start();//запуск потока (который отдельный)
        try {
            thread.join();//метод join - осуществляет ожидание пока этот поток (thread) умрет (закончится)
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        //==============воспроизведение звука после отправки почты=============================================================
        try {//после загрузки файла воспроизводим звук

            //TYPE_ALARM -длинная мелодия будильника
            //TYPE_NOTIFICATION - СМС уведомление

            Uri notify = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            //  Ringtone r = RingtoneManager.getRingtone(getApplicationContext(), notify);
            Ringtone rington = RingtoneManager.getRingtone(context, notify);

            rington.play();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

}