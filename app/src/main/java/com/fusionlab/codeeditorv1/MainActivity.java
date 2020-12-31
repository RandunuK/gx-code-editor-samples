package com.fusionlab.codeeditorv1;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.Layout;
import android.text.Selection;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.EasyEditSpan;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button type1Button = findViewById(R.id.button1);
        Button type2Button = findViewById(R.id.button2);
        Button type3Button = findViewById(R.id.button3);
        type1Button.setOnClickListener(this);
        type2Button.setOnClickListener(this);
        type3Button.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {

        switch (v.getId()) {
            case R.id.button1:
                startActivity(new Intent(MainActivity.this, MainActivity2.class));
                break;
            case R.id.button2:
                startActivity(new Intent(MainActivity.this, MainActivity3.class));
                break;
            case R.id.button3:
                startActivity(new Intent(MainActivity.this, BottomSheetActivity.class));
                break;
        }
    }
}