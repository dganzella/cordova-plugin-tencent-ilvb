package cn.easecloud.cordova.tencent;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.util.Log;
import android.webkit.WebView;
import android.widget.Toast;

import org.apache.cordova.*;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;
import org.json.JSONTokener;

import com.google.gson.Gson;

import com.tencent.av.sdk.AVRoomMulti;
import com.tencent.ilivesdk.*;
import com.tencent.ilivesdk.core.*;
import com.tencent.livesdk.*;

public class TencentILVB extends CordovaPlugin {

    private Context context;
    private Activity activity;
    private CordovaInterface cordova;
    private CordovaWebView webView;

	public CallbackContext eventCallbackContext;

    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);
        this.webView = webView;
        this.cordova = cordova;
        this.activity = cordova.getActivity();
        this.context = this.activity.getApplicationContext();
    }

    @Override
    public boolean execute(String action, JSONArray data, final CallbackContext callbackContext) throws JSONException
	{
		if (action.equals("init"))
		{
            int appid = data.getInt(0);
			Log.i("ILVB","APP ID:");
			Log.i("ILVB",new Integer(appid).toString());

            int accountType = data.getInt(1);

			Log.i("ILVB","ACC TYPE:");
			Log.i("ILVB",new Integer(accountType).toString()) ;

            ILiveSDK.getInstance().initSdk(this.context, appid, accountType);
			
			Log.i("ILVB","FINISH INIT");

            String id = data.getString(2);
            String sig = data.getString(3);

			Log.i("ILVB","ID");
			Log.i("ILVB",id);

			Log.i("ILVB","SIG");
			Log.i("ILVB",sig);
			
            ILiveLoginManager.getInstance().iLiveLogin(id, sig, new ILiveCallBack()
			{
                @Override
                public void onSuccess(Object data)
				{
					Log.i("ILVB","LOGIN SUCCESS");

                    Gson gson = new Gson();
                    callbackContext.success(gson.toJson(data));
                }

                @Override
                public void onError(String module, int errCode, String errMsg)
				{
					Log.i("ILVB","LOGIN ERROR");
					Log.i("ILVB",new Integer(errCode).toString());
					Log.i("ILVB",errMsg);

                    JSONObject obj = new JSONObject();
                    try
					{
                        obj.put("module", module);
                        obj.put("errCode", errCode);
                        obj.put("errMsg", errMsg);
                    } catch (JSONException e) {
                        e.printStackTrace();
                        callbackContext.error("ERROR: " + errMsg);
                    }
                    callbackContext.error(obj);
                }
            });
			
			return true;
        }
		else if (action.equals("createOrJoinRoom"))
		{
			
			int roomId = data.getInt(0);
            String role = data.getString(1);
			String hostId = data.getString(2);
			
			if(role.equals("LiveMaster"))
			{
				 //Configuration options of creating room
				 ILVLiveRoomOption hostOption = new ILVLiveRoomOption(null).
						controlRole("Host")//Role configuration
						.authBits(AVRoomMulti.AUTH_BITS_DEFAULT)//Permission configuration
						.cameraId(ILiveConstants.FRONT_CAMERA)//Front/rear-facing camera
						.videoRecvMode(AVRoomMulti.VIDEO_RECV_MODE_SEMI_AUTO_RECV_CAMERA_VIDEO)//Whether to start semi-automatic receiving
						.autoMic(true);
				
				//Create room
				ILVLiveManager.getInstance().createRoom(roomId, hostOption, new ILiveCallBack() {
					@Override
					public void onSuccess(Object data) {
						Log.i("ILVB","CREATE ROOM SUCCESS");
						Gson gson = new Gson();
						callbackContext.success(gson.toJson(data));
					}

					@Override
					public void onError(String module, int errCode, String errMsg) {
						
						Log.i("ILVB","CREATE ROOM ERROR");
						Log.i("ILVB",new Integer(errCode).toString());
						Log.i("ILVB",errMsg);

						JSONObject obj = new JSONObject();
						try
						{
							obj.put("module", module);
							obj.put("errCode", errCode);
							obj.put("errMsg", errMsg);
						} catch (JSONException e) {
							e.printStackTrace();
							callbackContext.error("ERROR: " + errMsg);
						}
						
						callbackContext.error(obj);
					}
				});
			}
			else
			{
				//Configuration options of joining room
				ILVLiveRoomOption memberOption = new ILVLiveRoomOption(hostId)
						.autoCamera(false) //Whether to enable camera automatically
						.controlRole("NormalMember") //Role configuration
						.authBits(AVRoomMulti.AUTH_BITS_DEFAULT) //Permission configuration
						.videoRecvMode(AVRoomMulti.VIDEO_RECV_MODE_SEMI_AUTO_RECV_CAMERA_VIDEO) //Whether to start semi-automatic receiving
						.autoMic(true);//Whether to enable mic automatically
						
				//Join a room
				ILVLiveManager.getInstance().joinRoom(roomId, memberOption, new ILiveCallBack() {
					@Override
					public void onSuccess(Object data) {
						Log.i("ILVB","JOIN ROOM SUCCESS");
						Gson gson = new Gson();
						callbackContext.success(gson.toJson(data));
					}

					@Override
					public void onError(String module, int errCode, String errMsg) {
						Log.i("ILVB","JOIN ROOM ERROR");
						Log.i("ILVB",new Integer(errCode).toString());
						Log.i("ILVB",errMsg);

						JSONObject obj = new JSONObject();
						try
						{
							obj.put("module", module);
							obj.put("errCode", errCode);
							obj.put("errMsg", errMsg);
						} catch (JSONException e) {
							e.printStackTrace();
							callbackContext.error("ERROR: " + errMsg);
						}
						
						callbackContext.error(obj);
					}
				});
			}
			
			return true;
		}
		else if( action.equals( "addEvents" ))
		{
			eventCallbackContext = callbackContext;
			
			return true;
      	}
		else if (action.equals("quit"))
		{
			return true;
        }

        return false;
    }

	public void triggerJSEvent(String type, JSONObject data )
	{
		JSONObject message = new JSONObject();       

		try{
			message.put("eventType", type);
			message.put("data", data);
		}catch (JSONException e) {}
		
		PluginResult myResult = new PluginResult(PluginResult.Status.OK, message);
		myResult.setKeepCallback(true);
		eventCallbackContext.sendPluginResult(myResult);
	}
}