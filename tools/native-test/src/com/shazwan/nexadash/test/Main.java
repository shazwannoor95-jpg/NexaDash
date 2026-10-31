package com.shazwan.nexadash.test;
import android.app.Activity;import android.os.Bundle;import android.widget.*;import android.graphics.Color;import java.lang.ref.WeakReference;import java.util.concurrent.atomic.AtomicInteger;
/** Real independent app UI, used only to verify display/input isolation; not a navigation/player mock. */
public class Main extends Activity {
 static WeakReference<Main> current=new WeakReference<>(null);
 static final AtomicInteger count=new AtomicInteger();static volatile String lastInput="";
 @Override public void onCreate(Bundle state){super.onCreate(state);getWindow().setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);current=new WeakReference<>(this);
  boolean a=getPackageName().endsWith(".a");LinearLayout column=new LinearLayout(this);column.setOrientation(1);column.setPadding(12,12,12,12);column.setBackgroundColor(a?0xff103b55:0xff3c204c);
  TextView title=new TextView(this);title.setTextColor(Color.WHITE);title.setTextSize(18);title.setText("Native test "+(a?"A":"B")+" · display "+getWindowManager().getDefaultDisplay().getDisplayId());column.addView(title,new LinearLayout.LayoutParams(-1,48));
  TextView counter=new TextView(this);counter.setTextColor(Color.WHITE);counter.setTextSize(22);counter.setBackgroundColor(a?0xff076f88:0xff7e3277);counter.setText("COUNTER: "+count.get());counter.setOnClickListener(v->counter.setText("COUNTER: "+count.incrementAndGet()));column.addView(counter,new LinearLayout.LayoutParams(-1,64));
  EditText input=new EditText(this);input.setSingleLine(true);input.setTextColor(Color.WHITE);input.setHintTextColor(0xffcccccc);input.setHint("Native test text field");input.addTextChangedListener(new android.text.TextWatcher(){public void beforeTextChanged(CharSequence s,int start,int count,int after){}public void onTextChanged(CharSequence s,int start,int before,int count){lastInput=s.toString();}public void afterTextChanged(android.text.Editable e){}});column.addView(input,new LinearLayout.LayoutParams(-1,52));
  TextView note=new TextView(this);note.setTextColor(Color.WHITE);note.setText("This is a separate test app. It does not prove Maps or TizenTube playback.");column.addView(note);column.setFocusableInTouchMode(true);setContentView(column);column.requestFocus();
 }
 @Override protected void onDestroy(){if(current.get()==this)current.clear();super.onDestroy();}
}
