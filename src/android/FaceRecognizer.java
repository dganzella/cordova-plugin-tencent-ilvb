package cn.easecloud.cordova.tencent;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.util.Log;
import android.webkit.WebView;
import android.widget.Toast;
import android.view.View;
import android.view.ViewGroup;
import android.view.LayoutInflater;
import android.view.WindowManager;
import android.view.Display;

import org.apache.cordova.*;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;
import org.json.JSONTokener;

import com.google.gson.Gson;


import com.tencent.*;
import com.tencent.av.sdk.AVRoomMulti;
import com.tencent.av.sdk.AVView;
import com.tencent.ilivesdk.*;
import com.tencent.ilivesdk.core.*;
import com.tencent.livesdk.*;
import com.tencent.ilivesdk.view.*;
import com.tencent.livesdk.ILVLiveConfig;
import com.tencent.ilivesdk.view.AVVideoView;
import com.tencent.ilivesdk.ILiveMemStatusLisenter;
import com.tencent.ilivesdk.data.ILiveMessage;
import com.tencent.ilivesdk.data.msg.ILiveOtherMessage;
import com.tencent.ilivesdk.listener.*;
import com.tencent.ilivesdk.core.impl.*;

import android.graphics.Rect;
import java.util.HashMap;
import java.util.ArrayList;
import java.lang.reflect.*;

import android.content.res.Configuration;
import android.os.Bundle;

import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;
import com.google.android.gms.vision.face.Landmark;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.concurrent.locks.ReentrantLock;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.util.SparseArray;
import android.graphics.PointF;
import android.graphics.drawable.Drawable;

import com.tencent.av.sdk.AVVideoCtrl;
import com.tencent.av.sdk.AVVideoCtrl.RemoteVideoPreviewCallback;
import com.tencent.av.sdk.AVVideoCtrl.LocalVideoPreviewCallback;

public class FaceRecognizer
{
    static private FaceDetector fdetector;
    AVVideoCtrl videoCtrl;
    Context mContext;
    CallbackContext mCallbackContext;
    String mSid;
    boolean mShouldRecognizeFaceOnNextFrame = false;

    public FaceRecognizer(Context context, String sid, String openid)
    {
        Log.i("ILVB","I 1");

        this.mContext = context;
        this.mSid = sid;

        if(fdetector == null)
        {
            fdetector = new FaceDetector.Builder(context)
            .setProminentFaceOnly(true)
            .setTrackingEnabled(false)
            .setLandmarkType(FaceDetector.ALL_LANDMARKS)
            .build();
        }
    }

    public void verifyNeedRecognizeFace(AVVideoCtrl.VideoFrame frame)
    {
        if(this.mShouldRecognizeFaceOnNextFrame)
        {
            this.mShouldRecognizeFaceOnNextFrame = false;  
            try{
                this.DoRecognizeFace(frame);
            } catch (JSONException e){
                Log.e("ILVB", "JSONException"+e.getMessage());
            }
        }
    }

	public void setToRecognizeFace(CallbackContext callbackContext)
    {
        this.mShouldRecognizeFaceOnNextFrame = true;
		this.mCallbackContext = callbackContext;
    }

    static public void decodeYUV420(int[] rgba, byte[] yuv420, int width, int height) {
        int half_width = (width + 1) >> 1;
        int half_height = (height +1) >> 1;
        int y_size = width * height;
        int uv_size = half_width * half_height;

        for (int j = 0; j < height; j++) {
            for (int i = 0; i < width; i++) {

                double y = (yuv420[j * width + i]) & 0xff;
                double v = (yuv420[y_size + (j >> 1) * half_width + (i>>1)]) & 0xff;
                double u = (yuv420[y_size + uv_size + (j >> 1) * half_width + (i>>1)]) & 0xff;

                double r;
                double g;
                double b;

                r = y + 1.402 * (u-128);
                g = y - 0.34414*(v-128) - 0.71414*(u-128);
                b = y + 1.772*(v-128);

                if (r < 0) r = 0;
                else if (r > 255) r = 255;
                if (g < 0) g = 0;
                else if (g > 255) g = 255;
                if (b < 0) b = 0;
                else if (b > 255) b = 255;

                int ir = (int)r;
                int ig = (int)g;
                int ib = (int)b;
                rgba[j * width + i] = 0xff000000 | (ir << 16) | (ig << 8) | ib;
            }
        }
    }

    public void DoRecognizeFace(AVVideoCtrl.VideoFrame frame) throws JSONException
    {
        Log.i("ILVB","START RECOGNIZER");

        synchronized(fdetector)
        {
            int width = frame.width;
            int height = frame.height;
            int half_width = (width + 1) >> 1;
            int half_height = (height +1) >> 1;
            int y_size = width * height;
            int uv_size = half_width * half_height;

            byte []yuv = frame.data;
            int[] intArray = new int[width*height];

            // Decode Yuv data to integer array
            decodeYUV420(intArray, yuv, width, height);

            // Initialize the bitmap, with the replaced color
            Bitmap bmp = Bitmap.createBitmap(intArray, width, height, Bitmap.Config.ARGB_8888);

            if(fdetector.isOperational())
            {
                com.google.android.gms.vision.Frame visionframe = new com.google.android.gms.vision.Frame.Builder().setBitmap(bmp).build();

                SparseArray<Face> faces = fdetector.detect(visionframe);
            
                if( faces.size() > 0 )
                {
                    Face face = faces.get(0);

                    if(face != null)
                    {
                        boolean found_left_eye = false, found_right_eye = false;
                        PointF left_eye = new PointF(0,0), right_eye = new PointF(0,0);

                        for ( Landmark landmark : face.getLandmarks() )
                        {
                            if(landmark.getType() == Landmark.LEFT_EYE)
                            {
                                left_eye = landmark.getPosition();
                                found_left_eye = true;
                            }
                            else if(landmark.getType() == Landmark.RIGHT_EYE)
                            {
                                right_eye = landmark.getPosition();
                                found_right_eye = true;
                            }
                        }

                        if(found_left_eye && found_right_eye)
                        {
                            //    left_eye = new PointF(width-left_eye.x, left_eye.y*0.975f);
                            //    right_eye = new PointF(width-right_eye.x, right_eye.y*0.975f);
                         
                            PointF diff = new PointF(left_eye.x - right_eye.x, left_eye.y - right_eye.y);
                            
                            double rotation = Math.atan2(diff.y, Math.abs(diff.x)) * 180.0 / Math.PI;

                            //rotation = -rotation;

                            double distance = Math.sqrt( diff.x*diff.x + diff.y*diff.y )/((double)width) * 100.0;
                            
                            PointF midPoint = new PointF ( 
                                            (float) ( ((left_eye.x + right_eye.x)/2.0)/((double)width)  * 100.0 ),
                                            (float) ( ((left_eye.y + right_eye.y)/2.0)/((double)height) * 100.0 )
                                            );
                            
                            JSONObject resultdict = new JSONObject();
                            
                            resultdict.put("eyesDistance", distance);
                            resultdict.put("midPointX", midPoint.x);
                            resultdict.put("midPointY", midPoint.y);
                            resultdict.put("rotation", rotation);
                            resultdict.put("streamId", this.mSid);

                            this.mCallbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK, resultdict));
                        }
                        else
                        {
                            JSONObject resultdict = new JSONObject();
                            
                            resultdict.put("streamId", this.mSid);
                            resultdict.put("error", "EYES NOT FOUND");
                            this.mCallbackContext.sendPluginResult(new PluginResult(PluginResult.Status.ERROR, resultdict));					
                        }
                    }
                    else
                    {
                        JSONObject resultdict = new JSONObject();
                        
                        resultdict.put("streamId", this.mSid);
                        resultdict.put("error", "FACE IS NULL");
                        this.mCallbackContext.sendPluginResult(new PluginResult(PluginResult.Status.ERROR, resultdict));
                    }
                }
                else
                {
                    JSONObject resultdict = new JSONObject();
                    
                    resultdict.put("streamId", this.mSid);
                    resultdict.put("error", "NO FACE DETECTED");
                    this.mCallbackContext.sendPluginResult(new PluginResult(PluginResult.Status.ERROR, resultdict));
                }
            }
            else
            {
                JSONObject resultdict = new JSONObject();
                
                resultdict.put("streamId", this.mSid);
                resultdict.put("error", "FACE DETECTOR NOT OPERATIONAL");
                this.mCallbackContext.sendPluginResult(new PluginResult(PluginResult.Status.ERROR, resultdict)); 
            }
        }
    }
}