package com.example.email;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;


public class PasswordActivity extends AppCompatActivity {

    // Объявляем об использовании следующих объектов:
    private EditText username;
    private EditText password;
    private Button login;
    private TextView loginLocked;
    private TextView attempts;
    private TextView numberOfAttempts;

    // Число для подсчета попыток залогиниться:
    int numberOfRemainingLoginAttempts = 3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
      //  setContentView(R.layout.password_activity);
        setContentView(R.layout.password_activity_with_my_button);

        username = (EditText) findViewById(R.id.my_edit_user);
        password = (EditText) findViewById(R.id.my_edit_password);
        login = (Button) findViewById(R.id.my_button_login);
        loginLocked = (TextView) findViewById(R.id.my_login_locked);
        attempts = (TextView) findViewById(R.id.my_attempts);
        numberOfAttempts = (TextView) findViewById(R.id.my_number_of_attempts);

        numberOfAttempts.setText(Integer.toString(numberOfRemainingLoginAttempts));

    }

    // Обрабатываем нажатие кнопки "Войти":
    public void Login(View view) {

        // Если введенные логин и пароль будут словом "admin", "4321",
        // показываем Toast сообщение об успешном входе:
        //вход на почту akuznecov2023@mail.ru
        if (username.getText().toString().equals("~") &&password.getText().toString().equals("~"))
        {
            Toast.makeText(getApplicationContext(), "Вход выполнен!", Toast.LENGTH_SHORT).show();

            // Выполняем переход на другой экран:
            Intent intent = new Intent(PasswordActivity.this, Akuznecov2023Activity.class);
            startActivity(intent);
        }
        //вход на почту bazilev_law@mail.ru
        else if (username.getText().toString().equals("~") &&password.getText().toString().equals("~"))
        {
            Toast.makeText(getApplicationContext(), "Вход выполнен!", Toast.LENGTH_SHORT).show();

            // Выполняем переход на другой экран:
            Intent intent = new Intent(PasswordActivity.this, Bazilev_lawActivity.class);
            startActivity(intent);
        }
        else {// В другом случае выдаем сообщение с ошибкой:
            Toast.makeText(getApplicationContext(), "Неправильные данные!", Toast.LENGTH_SHORT).show();
            numberOfRemainingLoginAttempts--;//минус попытка

            // Делаем видимыми текстовые поля, указывающие на количество оставшихся попыток:
            attempts.setVisibility(View.VISIBLE);
            numberOfAttempts.setVisibility(View.VISIBLE);
            numberOfAttempts.setText(Integer.toString(numberOfRemainingLoginAttempts));

            // Когда выполнено 3 безуспешных попытки залогиниться,
            // делаем видимым текстовое поле с надписью, что все пропало и выставляем
            // кнопке настройку невозможности нажатия setEnabled(false):
            if (numberOfRemainingLoginAttempts == 0) {
                login.setEnabled(false);
                loginLocked.setVisibility(View.VISIBLE);
                loginLocked.setBackgroundColor(Color.RED);
                loginLocked.setText("Вход заблокирован!!!");
            }
        }
    }


}