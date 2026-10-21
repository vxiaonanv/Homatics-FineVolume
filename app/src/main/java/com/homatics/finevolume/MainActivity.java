package com.homatics.finevolume;
import android.app.Activity;
import android.os.Bundle;
import android.media.AudioManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.provider.Settings;
import android.view.KeyEvent;
import android.view.View;
import android.widget.*;
public class MainActivity extends Activity {
 AudioManager audio; SharedPreferences prefs; LinearLayout root; TextView status;
 String[] durations={"1 秒","2 秒","3 秒","5 秒"}; String[] positions={"右侧居中","左侧居中","顶部居中"};
 @Override public void onCreate(Bundle state){super.onCreate(state);audio=(AudioManager)getSystemService(Context.AUDIO_SERVICE);prefs=getSharedPreferences("finevolume",MODE_PRIVATE);render();}
 TextView label(String s,int size){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(0xfff1f5f9);t.setPadding(10,12,10,12);return t;}
 void button(String name,Runnable r){Button b=new Button(this);b.setText(name);b.setAllCaps(false);root.addView(b,new LinearLayout.LayoutParams(-1,-2));b.setOnClickListener(v->r.run());}
 void refresh(){int min=android.os.Build.VERSION.SDK_INT>=28?audio.getStreamMinVolume(AudioManager.STREAM_MUSIC):0;int max=audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC);int cur=audio.getStreamVolume(AudioManager.STREAM_MUSIC);status.setText("媒体流音量："+cur+" / "+max+"（最小 "+min+"）\n当前设备系统提供 "+(max-min+1)+" 个整数音量值。\n注意：此处不是独立的 100 级 DSP 音量。");}
 void render(){ScrollView scroll=new ScrollView(this);root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(34,25,34,30);root.setBackgroundColor(0xff111827);scroll.addView(root);setContentView(scroll);
 root.addView(label("Homatics FineVolume 1.0",26));root.addView(label("兼容性检测版 · 不修改系统 · 不需要 Root",16));status=label("",17);root.addView(status);refresh();
 button("检测当前系统音量",this::refresh);
 button("音量 -（系统原生一级）",()->{audio.adjustStreamVolume(AudioManager.STREAM_MUSIC,AudioManager.ADJUST_LOWER,0);refresh();});
 button("音量 +（系统原生一级）",()->{audio.adjustStreamVolume(AudioManager.STREAM_MUSIC,AudioManager.ADJUST_RAISE,0);refresh();});
 root.addView(label("悬浮条规划参数（保存设置，不会假装已经接管系统音量键）",17));
 Spinner duration=new Spinner(this);duration.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,durations));duration.setSelection(prefs.getInt("duration",1));root.addView(duration);
 duration.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){public void onNothingSelected(AdapterView<?> p){} public void onItemSelected(AdapterView<?> p,View v,int pos,long id){prefs.edit().putInt("duration",pos).apply();}});
 Spinner position=new Spinner(this);position.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,positions));position.setSelection(prefs.getInt("position",0));root.addView(position);
 position.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){public void onNothingSelected(AdapterView<?> p){} public void onItemSelected(AdapterView<?> p,View v,int pos,long id){prefs.edit().putInt("position",pos).apply();}});
 Switch boot=new Switch(this);boot.setText("开机自启动（预留，尚未启用）");boot.setTextColor(0xfff1f5f9);boot.setChecked(false);boot.setEnabled(false);root.addView(boot);
 button("打开 HDMI-CEC / 显示声音设置",()->{try{startActivity(new Intent("android.settings.HDMI_CONTROL_SETTINGS"));}catch(Exception e){try{startActivity(new Intent(Settings.ACTION_SOUND_SETTINGS));}catch(Exception e2){Toast.makeText(this,"固件未开放该设置入口",Toast.LENGTH_LONG).show();}}});
 button("清除本应用保存的设置",()->{prefs.edit().clear().apply();render();Toast.makeText(this,"已清除应用设置；未修改系统配置",Toast.LENGTH_SHORT).show();});
 root.addView(label("检测版限制：无法保证全局拦截遥控器音量键、隐藏系统音量条或真正提供 100 级音频增益。必须在实体设备上验证后再开发这些能力。",14));
 }
 @Override public boolean onKeyDown(int keyCode,KeyEvent event){if(keyCode==KeyEvent.KEYCODE_VOLUME_UP||keyCode==KeyEvent.KEYCODE_VOLUME_DOWN){audio.adjustStreamVolume(AudioManager.STREAM_MUSIC,keyCode==KeyEvent.KEYCODE_VOLUME_UP?AudioManager.ADJUST_RAISE:AudioManager.ADJUST_LOWER,0);refresh();return true;}return super.onKeyDown(keyCode,event);}
}
