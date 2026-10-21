package com.homatics.finevolume;
import android.accessibilityservice.AccessibilityService;import android.view.KeyEvent;import android.view.accessibility.AccessibilityEvent;import android.media.AudioManager;
public class VolumeKeyService extends AccessibilityService{
 public void onAccessibilityEvent(AccessibilityEvent e){}
 public void onInterrupt(){}
 @Override protected boolean onKeyEvent(KeyEvent e){int k=e.getKeyCode();if(k!=KeyEvent.KEYCODE_VOLUME_UP&&k!=KeyEvent.KEYCODE_VOLUME_DOWN)return false;if(!getSharedPreferences("finevolume",0).getBoolean("enabled",false))return false;if(e.getAction()==KeyEvent.ACTION_DOWN&&e.getRepeatCount()==0){AudioManager a=(AudioManager)getSystemService(AUDIO_SERVICE);a.adjustStreamVolume(AudioManager.STREAM_MUSIC,k==KeyEvent.KEYCODE_VOLUME_UP?AudioManager.ADJUST_RAISE:AudioManager.ADJUST_LOWER,0);}return true;}
}