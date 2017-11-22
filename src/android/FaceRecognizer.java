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

import android.hardware.SensorManager;
import android.hardware.SensorEventListener;
import android.hardware.Sensor;
import android.hardware.SensorEvent;

public class FaceRecognizer
{

}