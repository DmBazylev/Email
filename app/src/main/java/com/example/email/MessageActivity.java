package com.example.email;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Properties;

import javax.mail.FetchProfile;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Store;

//активность Отдельного сообщения
public class MessageActivity extends AppCompatActivity {

    Context context=null;

    Button button_answer = null;
    Button button_redirect = null;
    Button button_delete_message = null;
    TextView txtView =null;
    ListView list_view = null;

    //для отображения письма
    int message_number=0;
    String text_inline="";
    boolean isList_of_Files=false;
    String [] list_of_files= null;

    //для ответа на сообщение
    boolean isAnswer=false;//флаг того что отправляется ответ на сообщение false значит обычное сообщение true - значит ответ
    String answer_to="";//адрес того кому отправляется ответ если флаг answer=true

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.bazilev_law_message_content);

        button_answer = findViewById(R.id.button_answer);
        button_redirect = findViewById(R.id.button_redirect);
        button_delete_message = findViewById(R.id.button_delete_message);
       txtView = (TextView) findViewById(R.id.textview_message_text);
        // txtView = (TextView) findViewById(R.id.textview_message_text_scrollview);
        list_view = (ListView) findViewById(R.id.list_files);

        context=this;//контекст для воспроизведения для звука

        button_answer.setTextSize(9);//установка размера шрифта на кнопку 12sp
        button_redirect.setTextSize(9);//установка размера шрифта на кнопку 12sp
        button_delete_message.setTextSize(9);//установка размера шрифта на кнопку 12sp

      //  txtView.setTextSize(8);
        txtView.setMovementMethod(new ScrollingMovementMethod());//чтобы сделать TextView прокручивающимся
        txtView.setMaxLines(Integer.MAX_VALUE);

        //получение данных из другой активности
        message_number=getIntent().getIntExtra("message_number",0);
        text_inline=getIntent().getStringExtra("text_inline");
        isList_of_Files=getIntent().getBooleanExtra("isList_of_Files",false);
        answer_to=getIntent().getStringExtra("answer_to_from_Bazilev_lawActivity");

//        System.out.println("MessageActivity message_number "+message_number);

        System.out.println("MessageActivity answer_to "+answer_to);


        txtView.setText(text_inline);

        if (isList_of_Files)//еси в сообщении есть файлы
        {
            list_of_files= getIntent().getStringArrayExtra("list_of_files");//то получаем список файлов

            for (String str:list_of_files)
            {
                System.out.println(str);
            }

            System.out.println("создаем адаптер");
            // создаем адаптер- android.R.layout.simple_list_item_1 – это системный layout-файл, который представляет собой TextView.

            ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, list_of_files);
            // ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, string_list_messages);

            System.out.println("устанавливаем адаптер списку");
            // устанавливаем адаптер списку
            list_view.setAdapter(adapter);

            //обработчик нажатия на лист - при нажатии с почтового сервера скачивается файл - вложение сообщения
            list_view.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                    System.out.println("MessageActivity message_number "+message_number+" list_of_files[position] "+list_of_files[position]);

                    Bazilev_lawActivity.save_file_from_mail(message_number,list_of_files[position]);//вызов стаического метода другой активности (активность -Bazilev_lawActivity)
                }
            });

        }


        button_answer.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Обработка нажатия

                // Выполняем переход на другой экран: - активность отправления сообщения
                Intent intent = new Intent(MessageActivity.this, Send_messageActivity.class);

                isAnswer=true;

                intent.putExtra("isAnswer", isAnswer);
                intent.putExtra("answer_to", answer_to);

                startActivity(intent);

            }
        });

        button_redirect.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Обработка нажатия
                // Выполняем переход на другой экран: активность перенаправления сообщения
                Intent intent = new Intent(MessageActivity.this, Redirect_messageActivity.class);

                intent.putExtra("message_number", message_number);
                intent.putExtra("text_inline", text_inline);

                startActivity(intent);
            }
        });

        button_delete_message.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Обработка нажатия
                Bazilev_lawActivity.delete_message(message_number);//вызов статического метода удаления сообщения

            }
        });

    }



}