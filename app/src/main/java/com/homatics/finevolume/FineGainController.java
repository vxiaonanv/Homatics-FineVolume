package com.homatics.finevolume;
import android.content.Context;
import android.media.audiofx.DynamicsProcessing;
/**
 * Experimental session-0 DSP control. Not all Android TV firmware allows global
 * effects; caller must test isAvailable() before claiming fine volume support.
 */
public final class FineGainController {
 private DynamicsProcessing effect;
 private boolean available=false;
 private int percent=100;
 public FineGainController(Context context) {
   try {
     DynamicsProcessing.Config config=new DynamicsProcessing.Config.Builder(
       DynamicsProcessing.VARIANT_FAVOR_FREQUENCY_RESOLUTION,
       2,false,0,false,0,false,0,true).build();
     effect=new DynamicsProcessing(0,0,config);
     effect.setEnabled(true);
     available=effect.getEnabled();
   } catch(Exception | LinkageError e){release();}
 }
 public boolean isAvailable(){return available;}
 public int getPercent(){return percent;}
 public boolean setPercent(int value){
   if(!available||effect==null)return false;
   int v=Math.max(0,Math.min(100,value));
   // Gain is in dB. 0% is silence-equivalent attenuation (-96dB),
   // 100% is unity gain. Intermediate values use logarithmic mapping.
   float db=v==0?-96f:(float)(20.0*Math.log10(v/100.0));
   try{
     int channels=effect.getChannelCount();
     for(int ch=0;ch<channels;ch++)effect.setInputGainByChannelIndex(ch,db);
     percent=v;return true;
   }catch(Exception | LinkageError e){available=false;release();return false;}
 }
 public void release(){available=false;if(effect!=null){try{effect.release();}catch(Exception ignored){}effect=null;}}
}