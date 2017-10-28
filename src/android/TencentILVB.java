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

public class TencentILVB extends CordovaPlugin implements ILiveMemStatusLisenter
{
	AVRootView avRootView;

    private Context context;
    private Activity activity;
    private CordovaInterface cordova;
    private CordovaWebView webView;
	private HashMap<String, Rect> viewPositions;

	public CallbackContext eventCallbackContext;
	public TencentILVB selfRef;

    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView)
	{
        super.initialize(cordova, webView);
        this.webView = webView;
        this.cordova = cordova;
        this.activity = cordova.getActivity();
        this.context = this.activity.getApplicationContext();
		this.selfRef = this;
		viewPositions = new HashMap<String, Rect>();
    }

    @Override
    public boolean onEndpointsUpdateInfo(int eventid, String[] updateList)
	{
        Log.i("ILVB","ENDPOINTS UPDATED");

		for (String id : updateList)
		{
			Log.i("ILVB","LIST");
			Log.i("ILVB",id);
			Log.i("ILVB",new Integer(eventid).toString());

			JSONObject eventData = new JSONObject();

			try
			{
				eventData.put("openid", id);
			} catch (JSONException e) {}

			switch (eventid)
			{
				case ILiveConstants.TYPE_MEMBER_CHANGE_HAS_CAMERA_VIDEO:

					if(id.equals(ILiveLoginManager.getInstance().getMyUserId()))
					{
						triggerJSEvent("onLocalStreamAdd", eventData);
					}
					else
					{
						triggerJSEvent("onUpdateRemoteStream", eventData);
					}
					
				break;

				case ILiveConstants.TYPE_MEMBER_CHANGE_NO_CAMERA_VIDEO:

					if(!id.equals(ILiveLoginManager.getInstance().getMyUserId()))
					{
						triggerJSEvent("onRemoteStreamRemove", eventData);
					}
					
				break;
			}
		}

		return false;
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

			this.cordova.getActivity().runOnUiThread(new Runnable()
			{
				@Override
				public void run()
				{
					ViewGroup parent = (ViewGroup) cordova.getActivity().findViewById(android.R.id.content);
						
					LayoutInflater inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
					
					View ilvbFrameView = inflater.inflate(context.getResources().
						getIdentifier("ilvbview", "layout", activity.getPackageName()), null);

					Log.i("ILVB","FRAME OF ROOT VIEW");
					Log.i("ILVB",ilvbFrameView.toString());

					parent.addView(ilvbFrameView, 0);

					ilvbFrameView.setBackgroundColor(0xFFFFFFFF);
					
					avRootView = (AVRootView) ilvbFrameView.findViewById(
						context.getResources().
						getIdentifier("av_root_view", "id", activity.getPackageName())
					);

					Log.i("ILVB","ROOT VIEW");
					Log.i("ILVB",avRootView.toString());

					ILVLiveManager.getInstance().init(new ILVLiveConfig());

        			ILVLiveManager.getInstance().setAvVideoView(avRootView);
					avRootView.getVideoGroup().setBackgroundColor(0xFFFFFFFF);

					avRootView.setSubCreatedListener(new AVRootView.onSubViewCreatedListener()
					{
						@Override
						public void onSubViewCreated()
						{
							for (int i = 0; i < ILiveConstants.MAX_AV_VIDEO_NUM; i++)
							{
								AVVideoView avVideoView = avRootView.getViewByIndex(i);

								if( avVideoView.getIdentifier() == null)
								{
									avVideoView.setPosTop(99999);
									avVideoView.setPosLeft(99999);
								}

								avVideoView.setRecvFirstFrameListener(new AVVideoView.RecvFirstFrameListener()
								{
									@Override
									public void onFirstFrameRecved(int width, int height, int angle, String identifier)
									{
										Log.i("ILVB","FIRST FRAME LISTENER");
										doUpdateView(identifier);
									}
								});
							}
						}
					});
				}
			});
	
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
								controlRole(role)//Role configuration
								.authBits(AVRoomMulti.AUTH_BITS_DEFAULT)
								.cameraId(ILiveConstants.FRONT_CAMERA)
								.videoRecvMode(AVRoomMulti.VIDEO_RECV_MODE_SEMI_AUTO_RECV_CAMERA_VIDEO)
								.autoMic(true)
								.autoCamera(true)
								.setRoomMemberStatusLisenter(selfRef);
						
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
								.controlRole(role)
								.authBits(AVRoomMulti.AUTH_BITS_DEFAULT)
								.videoRecvMode(AVRoomMulti.VIDEO_RECV_MODE_SEMI_AUTO_RECV_CAMERA_VIDEO)
								.autoMic(true)
								.autoCamera(true)
								.setRoomMemberStatusLisenter(selfRef);
								
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
		else if(action.equals("updateView"))
		{
			String openid = data.getString(0);

			Log.i("ILVB","VIEW TO UPDATE: ");
			Log.i("ILVB",openid);

			int top = data.getInt(1);
			int left = data.getInt(2);
			int width = data.getInt(3);
			int height = data.getInt(4);
			double ratio = data.getDouble(5);

			Log.i("ILVB","TOP");
			Log.i("ILVB",new Integer(top).toString());

			Log.i("ILVB","LEFT");
			Log.i("ILVB",new Integer(left).toString());

			Log.i("ILVB","WIDTH");
			Log.i("ILVB",new Integer(width).toString());

			Log.i("ILVB","HEIGHT");
			Log.i("ILVB",new Integer(height).toString());

			Log.i("ILVB","RATIO");
			Log.i("ILVB",new Double(ratio).toString());

			Rect r = new Rect((int)(left * ratio), (int)(top * ratio), (int)(left * ratio) + (int)(width * ratio),  (int)(top * ratio) + (int)(height * ratio)  );

			viewPositions.put(openid, r);

			doUpdateView(openid);

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

	public void doUpdateView(String openid)
	{
		class UpdateViewRunnable implements Runnable
		{
			String openid;

			UpdateViewRunnable(String openid)
			{ 
				this.openid = openid;
			}

			public void run()
			{
				for (int i = 0; i < ILiveConstants.MAX_AV_VIDEO_NUM; i++)
				{
					AVVideoView avVideoView = avRootView.getViewByIndex(i);
					String openid = avVideoView.getIdentifier();

					if(openid != null)
					{
						Log.i("ILVB","FOUND VIEW WITH ID");
						Log.i("ILVB",openid);
					}
				}

				AVVideoView videoview = avRootView.getUserAvVideoView(openid, AVView.VIDEO_SRC_TYPE_CAMERA);

				if(videoview != null)
				{
					Log.i("ILVB","FOUND CORRECT VIEW TO UPDATE");

					if(viewPositions.containsKey(this.openid))
					{
						Rect r = viewPositions.get(this.openid);

						videoview.setPosTop(r.top);
						videoview.setPosLeft(r.left);
						videoview.setPosWidth(r.width());
						videoview.setPosHeight(r.height());
						videoview.setBackgroundColor(0xFFFFFFFF);
						videoview.autoLayout();
					}

					Log.i("ILVB","FINISHED UPDATE");
				}
			}
		}

		cordova.getActivity().runOnUiThread(new UpdateViewRunnable(openid));
	}
}