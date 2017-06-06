package com.apray.myoled;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.things.pio.PeripheralManagerService;
import com.google.android.things.pio.SpiDevice;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;

import static java.lang.Thread.sleep;


public class MainActivity extends Activity {
    String TAG = "MainActivity";
    private HandlerThread mDisplayThread;
    edOLED oled;
    private Handler mDisplayHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Attempt to access the SPI
        //LayoutInflater inflater = LayoutInflater.from(this);
        //LinearLayout mylayout = (LinearLayout) inflater.inflate(R.layout.layout, null, false);
        oled = new edOLED();

        mDisplayThread = new HandlerThread("Display Thread");
        mDisplayThread.start();
        mDisplayHandler = new Handler(mDisplayThread.getLooper());
        mDisplayHandler.post(mDisplayLoop);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Display Loop Destroyed");

        if (mDisplayThread != null) {
            mDisplayThread.quitSafely();
        }
        oled.onDestroy();
    }
    int i = 2;

    private int xPos=0;
    private int yPos=0;

    private  Runnable mAnamation = new Runnable() {
        @Override
        public void run() {
            mAnamationRuner();
            try {
                sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            oled.clear(oled.PAGE);
            oled.display();
            mDisplayrun.run();
            disaplyLock = false;
        }
    };

    void mAnamationRuner(){
        for(int i = 2; i<20; i++){
            oled.clear(oled.PAGE);
            oled.circle(xPos,yPos,i++);
            if(i >4) {
                oled.circle(xPos, yPos, i - 4);
            }
            if(i>8) {
                oled.circle(xPos, yPos, i - 8);
                oled.circle(xPos, yPos, 3);
            }
            oled.display();

        }
    }

    private int rad;
    private Runnable mDisplayrun =new Runnable(){
        @Override
        public void run() {
            if(xPos < rad) {
                xPos += rad;
                mAnamationRuner();
            }
            if(yPos < rad+1){
                yPos+=rad;
                mAnamationRuner();
            }
            if(xPos > oled.getLCDWidth()-rad-1){
                xPos-=rad;
                mAnamationRuner();
            }
            if(yPos > oled.getLCDHeight()-rad-1){
                yPos-=rad;
                mAnamationRuner();
            }
            oled.circle(xPos,yPos,rad);

            oled.display();
            disaplyLock = false;
        }
    };
    volatile boolean disaplyLock =false;




    private Runnable mDisplayLoop =new Runnable(){

        @Override
        public void run() {
            if(!disaplyLock){
                disaplyLock =true;
            }else{
                Log.d(TAG,"disaplyLock"+disaplyLock);
                return;
            }
            if (oled != null) {
                try {
                    oled.begin();
                    xPos=oled.getLCDHeight()/2;
                    yPos=oled.getLCDWidth()/2;
                    rad = 1;
                    oled.display();
                    sleep(2000);
                    oled.clear(oled.PAGE);
                    oled.drawLayout();
                    oled.display();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally{
                    //mDisplayThread.quitSafely();
                    disaplyLock = false;
                    new Thread(mDisplayrun).start();
                }

            }
        }
    };


    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(disaplyLock) {
            return super.onKeyDown(keyCode, event);
        }else {
            disaplyLock = true;
        }

        //if (keyCode == KeyEvent.KEYCODE_B) {
        //    new Thread(mAnamation).start();
        //    return true;
        //}

        if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
            xPos ++;
            if( oled.rdPixal(xPos+rad,yPos)){
                new Thread(mAnamation).start();
            }else {
                new Thread(mDisplayrun).start();
            }
            return true;
        }
        if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
            xPos --;
            if( oled.rdPixal(xPos-rad,yPos)){
                new Thread(mAnamation).start();
            }else {
                new Thread(mDisplayrun).start();
            }            return true;
        }
        if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
            yPos --;
            if( oled.rdPixal(xPos,yPos-rad)){
                new Thread(mAnamation).start();
            }else {
                new Thread(mDisplayrun).start();
            }
            return true;
        }
        if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
            yPos++;
            if( oled.rdPixal(xPos,yPos+rad)){
                new Thread(mAnamation).start();
            }else {
                new Thread(mDisplayrun).start();
            }            return true;
        }

        if (keyCode == KeyEvent.KEYCODE_A) {
            if(rad < 10){
                rad++;
                new Thread(mDisplayrun).start();
            }else {
                disaplyLock = false;
            }
            return true;
        }

        if (keyCode == KeyEvent.KEYCODE_B) {
            if(rad > 1){
                rad--;
                new Thread(mAnamation).start();
            }else {
                disaplyLock = false;
            }
            return true;
        }
        disaplyLock = false;
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        return super.onKeyUp(keyCode, event);
    }

}
