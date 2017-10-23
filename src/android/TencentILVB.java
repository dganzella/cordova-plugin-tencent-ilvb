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

import com.tencent.ilivesdk.*;
import com.tencent.ilivesdk.core.*;

public class TencentILVB extends CordovaPlugin {

    private Context context;
    private Activity activity;
    private CordovaInterface cordova;
    private CordovaWebView webView;

    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);
        this.webView = webView;
        this.cordova = cordova;
        this.activity = cordova.getActivity();
        this.context = this.activity.getApplicationContext();
    }

    @Override
    public boolean execute(String action, JSONArray data, final CallbackContext callbackContext) throws JSONException {

		if (action.equals("init")) {

            int appid = data.getInt(0);
            int accountType = data.getInt(1);
            ILiveSDK.getInstance().initSdk(this.context, appid, accountType);
			
            String id = data.getString(2);
            String sig = data.getString(3);
			
            ILiveLoginManager.getInstance().iLiveLogin(id, sig, new ILiveCallBack() {
                @Override
                public void onSuccess(Object data) {
                    Gson gson = new Gson();
                    callbackContext.success(gson.toJson(data));
                }

                @Override
                public void onError(String module, int errCode, String errMsg) {
                    JSONObject obj = new JSONObject();
                    try {
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
			
			String localStreamAdd = data.getString(4);
			String remoteStreamAdd = data.getString(5);
			
			System.out.println(localStreamAdd);
			System.out.println(remoteStreamAdd);
			
        } else if (action.equals("createOrJoinRoom")) {
			
			int roomId = data.getInt(0);
            String role = data.getString(1);
			String hostId = data.getString(2);
			
			if(role.equals('LiveMaster'))
			{
				 //Configuration options of creating room
				 ILiveRoomOption hostOption = new ILiveRoomOption(null).
						controlRole("Host")//Role configuration
						.authBits(AVRoomMulti.AUTH_BITS_DEFAULT)//Permission configuration
						.cameraId(ILiveConstants.FRONT_CAMERA)//Front/rear-facing camera
						.videoRecvMode(AVRoomMulti.VIDEO_RECV_MODE_SEMI_AUTO_RECV_CAMERA_VIDEO)//Whether to start semi-automatic receiving
						.autoMic(true);
				
				//Create room
				ILVLiveManager.getInstance().createRoom(room, hostOption, new ILiveCallBack() {
					@Override
					public void onSuccess(Object data) {
						Toast.makeText(LiveActivity.this, "create room ok", Toast.LENGTH_SHORT).show();
						System.out.println("Create room Data");
						System.out.println(data);
					}

					@Override
					public void onError(String module, int errCode, String errMsg) {
						Toast.makeText(LiveActivity.this, module + "|create fail " + errMsg + " " + errMsg, Toast.LENGTH_SHORT).show();
					}
				});
			}
			else{
				//Configuration options of joining room
				ILiveRoomOption memberOption = new ILiveRoomOption(hostId)
						.autoCamera(false) //Whether to enable camera automatically
						.controlRole("NormalMember") //Role configuration
						.authBits(AVRoomMulti.AUTH_BITS_DEFAULT) //Permission configuration
						.videoRecvMode(AVRoomMulti.VIDEO_RECV_MODE_SEMI_AUTO_RECV_CAMERA_VIDEO) //Whether to start semi-automatic receiving
						.autoMic(true);//Whether to enable mic automatically
						
				//Join a room
				ILVLiveManager.getInstance().joinRoom(room, memberOption, new ILiveCallBack() {
					@Override
					public void onSuccess(Object data) {
						Toast.makeText(LiveActivity.this, "join room ok ", Toast.LENGTH_SHORT).show();
						System.out.println("Join room Data");
						System.out.println(data);	
					}

					@Override
					public void onError(String module, int errCode, String errMsg) {
						Toast.makeText(LiveActivity.this, module + "|join fail " + errMsg + " " + errMsg, Toast.LENGTH_SHORT).show();
					}
				});
			
			}
		} else if (action.equals("quit")) {
        } else {
            return false;
        }
    }

}
