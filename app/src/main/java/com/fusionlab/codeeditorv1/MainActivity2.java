package com.fusionlab.codeeditorv1;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.text.Layout;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

public class MainActivity2 extends AppCompatActivity {



    EditText mEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);

        mEditText = findViewById(R.id.editTextTextMultiLine);
        SpannableString string = new SpannableString("Text with clickable text Text with clickable text Text with clickable text Text with clickable text");
        mEditText.setText(string);
        mEditText.getEditableText().setSpan(new CustomClickableSpan(), 5, 19, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        mEditText.setMovementMethod(CustomMovementMethod.getInstance());
    }

    class CustomClickableSpan extends ClickableSpan {
        final String TAG = "CustomClickableSpan";


        @Override
        public void updateDrawState(@NonNull TextPaint ds) {
            super.updateDrawState(ds);
        }

        @Override
        public void onClick(@NonNull View widget) {
            Log.d(TAG, "onClick: text clicked");

            if (widget instanceof EditText) {

               /* widget.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        //Selection.setSelection(((EditText) widget).getText(),5,19);
                        //((EditText) widget).setSelection(5, 19);
                    }
                }, 50);*/

                //((EditText) widget).performLongClick();

            }
        }
    }

    class CustomMovementMethod extends LinkMovementMethod {

        @Override
        public void onTakeFocus(TextView view, Spannable text, int dir) {
            super.onTakeFocus(view, text, dir);
        }

        @Override
        public boolean onTouchEvent(TextView widget, Spannable buffer, MotionEvent event) {
            int action = event.getAction();

            if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_DOWN) {
                int x = (int) event.getX();
                int y = (int) event.getY();

                x -= widget.getTotalPaddingLeft();
                y -= widget.getTotalPaddingTop();

                x += widget.getScrollX();
                y += widget.getScrollY();

                Layout layout = widget.getLayout();
                int line = layout.getLineForVertical(y);
                int off = layout.getOffsetForHorizontal(line, x);

                int lineLength = line;
                String[] lines = widget.getText().toString().split("\n");
                for (int i = 0; i <= line; i++) {
                    lineLength += lines[i].length();
                }

                if (off >= lineLength) {
                    // Return true so click won't be triggered in the leftover empty space
                    return true;
                }
            }

            return super.onTouchEvent(widget, buffer, event);
        }
    }
}