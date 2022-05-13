package com.biglucas.demos;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ContentActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_content);
        handleIntentOpen();
    }

    private void handleIntentOpen() {
        try {
            System.out.println(getIntent().getData());
            InputStream inputStream = getContentResolver().openInputStream(getIntent().getData());
//            if (inputStream == null) {
//                // TODO: mostrar algo mais palpável
//                return;
//            }
            //        int size = inputStream.available();
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
            BufferedReader dis = new BufferedReader(inputStreamReader);
            ArrayList<String> lines = new ArrayList<>();
            while (true) {
                String line = dis.readLine();
                System.out.println(line);
                if (line == null) {
                    break;
                }
                lines.add(line);
            }
            this.getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.browser_content, new PageContentFragment(lines, this.getIntent().getData()))
                    .commit();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}