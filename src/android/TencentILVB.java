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
import com.tencent.ilivesdk.view.*;
import com.tencent.livesdk.ILVLiveConfig;

public class TencentILVB extends CordovaPlugin
{
	AVRootView avRootView;

    private Context context;
    private Activity activity;
    private CordovaInterface cordova;
    private CordovaWebView webView;

	public CallbackContext eventCallbackContext;

    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView)
	{
        super.initialize(cordova, webView);
        this.webView = webView;
        this.cordova = cordova;
        this.activity = cordova.getActivity();
        this.context = this.activity.getApplicationContext();

		ViewGroup parent = (ViewGroup) cordova.getActivity().findViewById(android.R.id.content);
			
		LayoutInflater inflater = (LayoutInflater)getBaseContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		
		View ilvbFrameView = inflater.inflate(R.layout.ilvbview, null);
		
		parent.addView(ilvbFrameView);
		
		this.avRootView = (AVRootView) ilvbFrameView.findViewById(
			this.context.getResources().
			getIdentifier("av_root_view", "id", this.activity.getPackageName())
		);

		Log.i("ILVB","ROOT VIEW");
		Log.i("ILVB",this.avRootView.toString());
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
			ILiveRoomManager.getInstance().init(new ILiveRoomConfig());

			ILVLiveManager.getInstance().init(new ILVLiveConfig());
        	ILVLiveManager.getInstance().setAvVideoView(this.avRootView);
			
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
			
			Log.i("ILVB","CREATE OR JOIN ROOM");

			int roomId = data.getInt(0);
			Log.i("ILVB","ROOM ID");
			Log.i("ILVB",new Integer(roomId).toString());

            String role = data.getString(1);
			Log.i("ILVB","ROLE");
			Log.i("ILVB",role);

			String hostId = data.getString(2);
			Log.i("ILVB","HOST ID");
			Log.i("ILVB", hostId);

			class CreateOrJoinRunnable implements Runnable
			{
				String role, hostId;
				int roomId;

				CreateOrJoinRunnable(String role, int roomId, String hostId)
				{ 
					this.role = role;
					this.roomId = roomId;
					this.hostId = hostId;
				}

				public void run()
				{
					if(role.equals("LiveMaster"))
					{
						Log.i("ILVB","BUILD HOST OPTIONS");

						//Configuration options of creating room
						ILVLiveRoomOption hostOption = new ILVLiveRoomOption(null).
								controlRole("Host")//Role configuration
								.authBits(AVRoomMulti.AUTH_BITS_DEFAULT)//Permission configuration
								.cameraId(ILiveConstants.FRONT_CAMERA)//Front/rear-facing camera
								.videoRecvMode(AVRoomMulti.VIDEO_RECV_MODE_SEMI_AUTO_RECV_CAMERA_VIDEO)//Whether to start semi-automatic receiving
								.autoMic(true);
						
						Log.i("ILVB","CREATE ROOM NOW");
						Log.i("ILVB",hostOption.toString());

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

						Log.i("ILVB","FINISH CREATE ROOM");
					}
					else
					{
						Log.i("ILVB","BUILD VIEWER OPTIONS");

						//Configuration options of joining room
						ILVLiveRoomOption memberOption = new ILVLiveRoomOption(hostId)
								.autoCamera(false) //Whether to enable camera automatically
								.controlRole("NormalMember") //Role configuration
								.authBits(AVRoomMulti.AUTH_BITS_DEFAULT) //Permission configuration
								.videoRecvMode(AVRoomMulti.VIDEO_RECV_MODE_SEMI_AUTO_RECV_CAMERA_VIDEO) //Whether to start semi-automatic receiving
								.autoMic(true);//Whether to enable mic automatically
								
						Log.i("ILVB","JOIN ROOM NOW");

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

						Log.i("ILVB","FINISH JOIN ROOM");
					}
				}
			}	

			cordova.getActivity().runOnUiThread(new CreateOrJoinRunnable(role, roomId, hostId));
			
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