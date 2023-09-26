package com.example.email;

import android.content.Context;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.util.Properties;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

//активность для направления сообщения (это может быть просто направление сообщение и как ответ как какое либо сообщение,
// отличие лишь в том что в последнем случае уже определен адресат
public class Send_messageActivity extends AppCompatActivity {

    Context context=null;

    Button button_send_message = null;
    Button button_attach_file = null;
    EditText editText_enter_email =null;
    EditText editText_message_text =null;

    ListView list_view_files_to_attach = null;
    String [] string_list_files_to_attach=null;

    Message message = null;
    MimeMultipart multipart =null;
    boolean isAnswer=false;//флаг того что отправляется ответ на сообщение false значит обычное сообщение если true - значит ответ
    String answer_to="";//адрес того кому отправляется ответ если флаг answer=true

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.send_message);

        button_send_message = findViewById(R.id.button_send_message);
        button_attach_file = findViewById(R.id.button_attach_file);
        editText_enter_email = findViewById(R.id.enter_email);
        editText_message_text = findViewById(R.id.message_text);
        list_view_files_to_attach = findViewById(R.id.list_files_to_attach);

        context=this;//контекст для воспроизведения для звука

        button_send_message.setTextSize(9);//установка размера шрифта на кнопку 10sp
        button_attach_file.setTextSize(9);//установка размера шрифта на кнопку 10sp

        isAnswer=getIntent().getBooleanExtra("isAnswer",false);

        if (isAnswer)//если это ответ на сообщение
        {
            answer_to=getIntent().getStringExtra("answer_to");
            editText_enter_email.setText(answer_to);
            editText_enter_email.setEnabled(false);
        }



        button_send_message.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Обработка нажатия

//                System.out.println("editText_enter_email.getText().toString() " + editText_enter_email.getText().toString()+
//                                "editText_message_text.getText().toString() " + editText_message_text.getText().toString());

                if (!isAnswer)
                {
                    String email_address=editText_enter_email.getText().toString();
                    String message_text=editText_message_text.getText().toString();
                    send_message_to_mail(email_address, message_text);
                }
                else if (isAnswer)
                {
                    String message_text=editText_message_text.getText().toString();
                    send_message_to_mail(answer_to, message_text);
                }


            }
        });





        button_attach_file.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Обработка нажатия

                show_list_files_to_attach();

            }
        });



    }

//первый параметр почтовый адрес, второй параметр текст сообщения
    public  void send_message_to_mail(String email_address, String message_text) {

        //создаем дочерний поток
        Thread thread = new Thread(new Runnable() {

            @Override
            public void run() //в теле переопределенного метода run() выполняется работа в отдельном потоке
            {
                //======================================работа в дочернем потоке========================================================
                //используем mail.ru
                String to = email_address;//почта адресат
                String from = "~";//почта отправитель
                final String username = "~";//имя пользователя почты отправителя (то что было при регистрации почты)
                final String password = "~";//специальный пароль для внешнего приложения

                String host = "smtp.mail.ru";//настройки javamail для mail.ru
                Properties props = new Properties();
                props.put("mail.smtp.auth", "true");
                props.put("mail.smtp.starttls.enable", "true");
                props.put("mail.smtp.host", host);

                props.put("mail.smtp.port", 587);//mail.ru работает с этим портом
                // props.put("mail.smtp.port", 25);//это тоже работает при установки настройки в gmail разрешения на допуск небезопасного приложения

                Session session = Session.getInstance(props,
                        new javax.mail.Authenticator() {
                            protected PasswordAuthentication getPasswordAuthentication() {
                                return new PasswordAuthentication(username, password);
                            }
                        });

                try {

                    message = new MimeMessage(session);
                    message.setFrom(new InternetAddress(from));
                    message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
                    message.setSubject("От Базылева Дмитрия Александровича");//тема сообщения - короткое название файла (тип (входящий, исходящий), номер телефона, дата, время)

                    // Create the message part - отправка файла
                    MimeBodyPart messageBodyPart = new MimeBodyPart();
                    messageBodyPart.setText(message_text);//содержание письма

                    // Create a multipart message
                    if (multipart==null)
                    {
                        multipart = new MimeMultipart();
                    }


                    // Set text message part
                    multipart.addBodyPart(messageBodyPart);

                    // Send the complete message parts
                    message.setContent(multipart);

                    Transport.send(message);//отправка сообщения


                } catch (MessagingException e) {
                    System.out.println(e);
                    //отлов исключения при ошибке при отправке почты

                }


                //=========================================остаток кода дочернего потока=====================================================

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


    public void show_list_files_to_attach()
    {
        File directory = new File("/storage/emulated/0/Download");
        File[] files = directory.listFiles();

        int len=files.length;

        if (len==0)
        {
            return;
        }

        string_list_files_to_attach= new String[len];

        for (int r=0;r<string_list_files_to_attach.length;++r)
        {
            string_list_files_to_attach[r]=files[r].getName().toString();
        }



        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, string_list_files_to_attach);
        // ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, string_list_messages);

        System.out.println("устанавливаем адаптер списку");
        // устанавливаем адаптер списку
        list_view_files_to_attach.setAdapter(adapter);

        //обработчик нажатия на лист - при нажатии файл прикрепляется к сообщению
        list_view_files_to_attach.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                //  System.out.println("public void onItemClick(AdapterView<?> parent, View view, int position, long id)");

                if (multipart==null)
                {
                    multipart = new MimeMultipart();
                }


                String file_name = "/storage/emulated/0/Download/" + string_list_files_to_attach[position];

                // подключаем (прикрепляем) файл к сообщению
                MimeBodyPart messageBodyPart_with_file = new MimeBodyPart();

                DataSource source = new FileDataSource(file_name);

                try {
                    messageBodyPart_with_file.setDataHandler(new DataHandler(source));
                    messageBodyPart_with_file.setFileName(file_name);
                    multipart.addBodyPart(messageBodyPart_with_file);

                    System.out.println("прикрепляем файл: "+file_name);

                } catch (MessagingException e) {
                    e.printStackTrace();
                }

            }
        });

    }

}