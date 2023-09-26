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

import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.util.Properties;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

//активность для перенаправления сообщения
public class Redirect_messageActivity extends AppCompatActivity {

    Context context=null;

    Button button_redirect_message = null;
    EditText editText_enter_email_to_redirect =null;
    int message_number_to_redirect=0;
    String text_inline="";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.redirect_message);

        button_redirect_message = findViewById(R.id.button_send_message_to_redirect);
        editText_enter_email_to_redirect =findViewById(R.id.enter_email_to_redirect);


        context=this;//контекст для воспроизведения для звука

        message_number_to_redirect=getIntent().getIntExtra("message_number",0);
        text_inline=getIntent().getStringExtra("text_inline");

        button_redirect_message.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Обработка нажатия

                System.out.println(" button_redirect_message.setOnClickListener(new View.OnClickListener()");

                String email_address_to_redirect=editText_enter_email_to_redirect.getText().toString();//берем текст для перенаправления - содержание письма
                System.out.println(" email_address_to_redirect "+email_address_to_redirect);

                Bazilev_lawActivity.redirect_message(message_number_to_redirect,email_address_to_redirect,text_inline);


            }
        });

    }



}