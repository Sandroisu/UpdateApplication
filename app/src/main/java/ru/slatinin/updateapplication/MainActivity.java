package ru.slatinin.updateapplication;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Objects;
import java.util.Scanner;

import javax.net.ssl.HttpsURLConnection;

public class MainActivity extends AppCompatActivity {
    private TextView tvStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Intent intent = getIntent();
        if (intent != null) {
            if (intent.getDataString() != null) {
                String x = intent.getDataString();
                String y = x.split("#")[1];
                String z = y.split("&")[0];
                final String token = z.split("=")[1];
            }
        }
        tvStatus = findViewById(R.id.text_of_status);
        Button btnUpdate = findViewById(R.id.button_update_apk);
        btnUpdate.setVisibility(View.VISIBLE);
        btnUpdate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateAPK();
            }
        });
        Button button = findViewById(R.id.button_get_permission);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://oauth.yandex.ru/authorize?response_type=token&client_id=eebf53967ac5404db7c70967b375a252"));
                startActivity(browserIntent);
            }
        });
    }

    private void updateAPK() {
        final Handler handler = new Handler(Looper.getMainLooper());
        Thread thread = new Thread(new Runnable() {
            @SuppressWarnings("ResultOfMethodCallIgnored")
            @Override
            public void run() {
                String link = null;
                InputStream input = null;
                HttpsURLConnection connection = null;
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        tvStatus.setText("Проверяем наличие новой версии...");
                    }
                });
                try {
                    URL sUrl = new URL("https://cloud-api.yandex.net/v1/disk/public/resources/download?public_key=https://yadi.sk/d/74qNbPRS4RptGA");
                    connection = (HttpsURLConnection) sUrl.openConnection();
                    connection.connect();
                    input = connection.getInputStream();
                    InputStream in = new BufferedInputStream(input);
                    Scanner s = new Scanner(in).useDelimiter("\\A");
                    String d = s.hasNext() ? s.next() : "";
                    if (!d.isEmpty()) {
                        JSONObject object = new JSONObject(d);
                        link = object.getString("href");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            tvStatus.setTextColor(Color.RED);
                            tvStatus.setText("У вас установлена последняя версия приложения");
                        }
                    });
                } finally {
                    try {
                        if (input != null)
                            input.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    if (connection != null)
                        connection.disconnect();
                }
                if (link != null) {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            tvStatus.setText("Есть новая версия, скачиваем...");
                        }
                    });
                    File folder = new File(Objects.requireNonNull(MainActivity.this.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)).toString());
                    File file = new File(folder.getAbsolutePath(), "app-debug.apk");
                    final Uri uri = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) ?
                            FileProvider.getUriForFile(MainActivity.this, BuildConfig.APPLICATION_ID + ".provider", file) : Uri.fromFile(file);
                    if (file.exists()) {
                        file.delete();
                    }
                    getApk(link, file);
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            tvStatus.setText("Новая версия скачана");
                        }
                    });
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            Intent install = new Intent(Intent.ACTION_INSTALL_PACKAGE)
                                    .setData(uri)
                                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                            MainActivity.this.startActivity(install);
                        }
                    });
                }
            }
        });
        thread.start();
    }

    private void getApk(String link, File file) {
        InputStream input = null;
        OutputStream output = null;
        HttpURLConnection connection = null;
        try {
            URL sUrl = new URL(link);
            connection = (HttpURLConnection) sUrl.openConnection();
            connection.connect();

            input = connection.getInputStream();
            output = new FileOutputStream(file);

            byte[] data = new byte[4096];
            int count;
            while ((count = input.read(data)) != -1) {
                output.write(data, 0, count);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (output != null)
                    output.close();
                if (input != null)
                    input.close();
            } catch (IOException ignored) {
            }

            if (connection != null)
                connection.disconnect();
        }
    }


}