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

import com.tencent.av.sdk.AVVideoCtrl;
import com.tencent.av.sdk.AVVideoCtrl.LocalVideoPreProcessCallback;
import com.tencent.av.sdk.AVVideoCtrl.RemoteVideoPreviewCallback;

import android.support.annotation.RequiresApi;
import android.os.Build;

import com.tencent.ilivefilter.TILFilter;
import com.tencent.liteav.basic.listener.TXINotifyListener;

public class TencentILVB extends CordovaPlugin implements ILiveMemStatusLisenter
{
	AVRootView avRootView = null;

    private Context context;
    private Activity activity;
    private CordovaInterface cordova;
    private CordovaWebView webView;
	private HashMap<String, ILVBVideoConfigs> viewConfigs;
	private TILFilter videoFilter;

	public CallbackContext eventCallbackContext;
	public TencentILVB selfRef;

	private boolean quitting = false, inited = false, insideRoom = false;

	class ILVBVideoConfigs
	{
		Rect r;
		FaceRecognizer f;
	}

	class UpdateViewRunnable implements Runnable
	{
		String openid;

		UpdateViewRunnable(String openid)
		{ 
			this.openid = openid;
		}

		public void run()
		{
			AVVideoView videoview = avRootView.getUserAvVideoView(openid, AVView.VIDEO_SRC_TYPE_CAMERA);

			if(videoview != null)
			{
				if(viewConfigs.containsKey(this.openid))
				{
					Rect r = viewConfigs.get(this.openid).r;

					videoview.setPosTop(r.top);
					videoview.setPosLeft(r.left);
					videoview.setPosWidth(r.width());
					videoview.setPosHeight(r.height());
					videoview.setBackgroundColor(0xFFFFFFFF);

					Display display = ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
					int rotation = display.getRotation();

					if(!this.openid.equals(ILiveLoginManager.getInstance().getMyUserId()))
					{
						videoview.setRotation(rotation == 1 ? 270 : 90);
					}

					videoview.autoLayout();
				}

				/*if(this.openid.equals(ILiveLoginManager.getInstance().getMyUserId()))
				{
					Log.i("ILVB","PUBLISHER UPDATED, REUPDATING EVERYBODY ELSE");

					//On manual orientation mode, when the publisher is updated, everybody else needs to be updated too :/
					for (String userId : viewConfigs.keySet())
					{
						if(!userId.equals(this.openid))
						{
							new android.os.Handler().postDelayed(
								new DelayedUpdate(userId), 
							1000);
						}
					}
				}*/
			}
		}
	}

	class DelayedUpdate implements Runnable
	{
		public void run() {

			for (String userId : viewConfigs.keySet())
			{
				doUpdateView(userId);
			}
			
			new android.os.Handler().postDelayed(
				new DelayedUpdate(), 
			1000);
		}
	};
	
    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView)
	{
        super.initialize(cordova, webView);
        this.webView = webView;
        this.cordova = cordova;
        this.activity = cordova.getActivity();
        this.context = this.activity.getApplicationContext();
		this.selfRef = this;
		viewConfigs = new HashMap<String, ILVBVideoConfigs>();

		new android.os.Handler().postDelayed(
			new DelayedUpdate(), 
		1000);
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
						ILVBVideoConfigs ivc = viewConfigs.get(id);
						ivc.r = new Rect(99000,99000,99999,99999);
						viewConfigs.put(id, ivc);
			
						//avRootView.closeUserView(id, AVView.VIDEO_SRC_TYPE_CAMERA, true);
						//viewConfigs.remove(id);
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

			if(!inited)
			{
				ILiveLog.setLogLevel(ILiveLog.TILVBLogLevel.OFF);
				ILiveSDK.getInstance().initSdk(this.context, appid, accountType);
				inited = true;
			}

			this.cordova.getActivity().runOnUiThread(new Runnable()
			{
				@Override
				public void run()
				{
					if(avRootView == null)
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

						avRootView.getVideoGroup().setBackgroundColor(0xFFFFFFFF);

						avRootView.setAutoOrientation(false);

						avRootView.setSubCreatedListener(new AVRootView.onSubViewCreatedListener()
						{
							@Override
							public void onSubViewCreated()
							{
								Log.i("ILVB","ON SUBVIEW CREATED");

								for (int i = 0; i < ILiveConstants.MAX_AV_VIDEO_NUM; i++)
								{
									AVVideoView avVideoView = avRootView.getViewByIndex(i);

									avVideoView.setRotate(false);

									if( avVideoView.getIdentifier() == null)
									{
										avVideoView.setPosTop(99999);
										avVideoView.setPosLeft(99999);
										avVideoView.autoLayout();
									}

									/*avVideoView.setRecvFirstFrameListener(new AVVideoView.RecvFirstFrameListener()
									{
										@Override
										public void onFirstFrameRecved(int width, int height, int angle, String identifier)
										{
											class UnlockRend implements Runnable
											{
												String identifier;

												UnlockRend(String identifier)
												{
													this.identifier = identifier;
												}

												@Override
												public void run()
												{
													AVVideoView videoview = avRootView.getUserAvVideoView(identifier, AVView.VIDEO_SRC_TYPE_CAMERA);

													if(videoview != null)
													{
														videoview.unlockRendering();
													}
												}
											}

											cordova.getActivity().runOnUiThread(new UnlockRend(identifier));
										}
									});*/
								}
							}
						});

						
						Log.i("ILVB","ROOT VIEW");
						Log.i("ILVB",avRootView.toString());

						ILVLiveManager.getInstance().init(new ILVLiveConfig());
						ILVLiveManager.getInstance().setAvVideoView(avRootView);
					}

					Display display = ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
					int rotation = display.getRotation();
					avRootView.setRemoteRotationFix(rotation == 1 ? 270 : 90);
				}
			});

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
								controlRole(role)//Role configuration
								.authBits(AVRoomMulti.AUTH_BITS_DEFAULT)
								.cameraId(ILiveConstants.FRONT_CAMERA)
								.videoRecvMode(AVRoomMulti.VIDEO_RECV_MODE_SEMI_AUTO_RECV_CAMERA_VIDEO)
								.autoMic(true)
								.autoSpeaker(true)
								.autoCamera(true)
								.imsupport(false)
								.setRoomMemberStatusLisenter(selfRef);
						
						Log.i("ILVB","CREATE ROOM NOW");
						Log.i("ILVB",hostOption.toString());

						//Create room
						ILVLiveManager.getInstance().createRoom(roomId, hostOption, new ILiveCallBack() {
							@Override
							public void onSuccess(Object data) {
								Log.i("ILVB","CREATE ROOM SUCCESS");
								insideRoom = true;

								RegisterPreviews();

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
								.autoSpeaker(true)
								.imsupport(false)
								.setRoomMemberStatusLisenter(selfRef);
								
						Log.i("ILVB","JOIN ROOM NOW");

						//Join a room
						ILVLiveManager.getInstance().joinRoom(roomId, memberOption, new ILiveCallBack() {
							@Override
							public void onSuccess(Object data) {
								Log.i("ILVB","JOIN ROOM SUCCESS");
								insideRoom = true;

								RegisterPreviews();

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
		else if(action.equals( "enableDisableOutput" )){

			String output = data.getString(0);
			Log.i("ILVB","OUTPUT TO CHANGE");
			Log.i("ILVB",output);

            String onOff = data.getString(1);
			boolean turningOn = onOff.equals("on");

			Log.i("ILVB","ON OR OFF");
			Log.i("ILVB", turningOn ? "ON_" : "OFF_");

			class UpdatePublishAudioVideoState implements Runnable
			{
				String output;
				boolean turningOn;

				UpdatePublishAudioVideoState(String output, boolean turningOn)
				{ 
					this.output = output;
					this.turningOn = turningOn;
				}

				public void run()
				{
					if(this.output.equals("video")){
						ILiveRoomManager.getInstance().enableCamera(ILiveConstants.FRONT_CAMERA,this.turningOn);
					}
					else if(this.output.equals("mic")){
						ILiveRoomManager.getInstance().enableMic(this.turningOn);
					}
				}
			}

			cordova.getActivity().runOnUiThread(new UpdatePublishAudioVideoState(output, turningOn));

			return true;
		}
		else if(action.equals("deviceRotated")){

			if(avRootView != null)
			{
				this.cordova.getActivity().runOnUiThread(new Runnable()
				{
					@Override
					public void run()
					{
						Display display = ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
						int rotation = display.getRotation();
						Log.i("ILVB","ROT ON UPDATE: " + new Integer(rotation).toString());
						avRootView.setRemoteRotationFix(rotation == 1 ? 270 : 90);

						/*for (String userId : viewConfigs.keySet())
						{
							doUpdateView(userId);
						}*/
					}
				});
			}
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

			ILVBVideoConfigs ivc = new ILVBVideoConfigs();
			ivc.r = new Rect((int)(left * ratio), (int)(top * ratio), (int)(left * ratio) + (int)(width * ratio),  (int)(top * ratio) + (int)(height * ratio)  );

			viewConfigs.put(openid, ivc);

			//doUpdateView(openid);

			return true;
		}
		else if (action.equals("quit"))
		{
			doQuitRoom();

			return true;
        }
		else if (action.equals("recognizeFace"))
		{
			String streamid = data.getString(0);

			//Log.i("ILVB","REGISTER RECOGNIZE FACE: ");
			//Log.i("ILVB",streamid);

			String openid = data.getString(1);

			//Log.i("ILVB","REGISTER RECOGNIZE FACE, OPENID: ");
			//Log.i("ILVB",openid);

			if(viewConfigs.containsKey(openid))
			{
				ILVBVideoConfigs ivc = viewConfigs.get(openid);

				if(ivc.f == null)
				{
					ivc.f = new FaceRecognizer(this.context, streamid, openid, openid.equals(ILiveLoginManager.getInstance().getMyUserId() ) );
				}
				
				ivc.f.setToRecognizeFace(callbackContext);
			}
			else
			{
				JSONObject resultdict = new JSONObject();
				
				try{
					resultdict.put("streamId", streamid);
					resultdict.put("error", "VIDEO NOT FOUND IN HASHMAP, NEEDS TO UPDATE VIEW ONCE AT LEAST");
				}catch (JSONException e) {}

				callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.ERROR, resultdict)); 
			}

			return true;
        }

        return false;
    }

	@Override
    public void onPause(boolean multitasking) {
        super.onPause(multitasking);
		doQuitRoom();
    }

	@Override
    public void onDestroy()
	{
		super.onDestroy();
		doQuitRoom();

        ILVLiveManager.getInstance().shutdown();
		ILiveRoomManager.getInstance().shutdown();
    }

	public void doQuitRoom()
	{
		if(quitting || !insideRoom)
			return;

		quitting = true;

		ILiveSDK.getInstance().getAvVideoCtrl().setLocalVideoPreProcessCallback(null);
		//ILiveSDK.getInstance().getAvVideoCtrl().setRemoteVideoPreProcessCallback(null);

		this.cordova.getActivity().runOnUiThread(new Runnable()
		{
			@Override
			public void run()
			{

				//for (String userId : viewConfigs.keySet())
				//{
				//	avRootView.closeUserView(userId, AVView.VIDEO_SRC_TYPE_CAMERA, true);
				//}

				for (int i = 0; i < ILiveConstants.MAX_AV_VIDEO_NUM; i++)
				{
					AVVideoView avVideoView = avRootView.getViewByIndex(i);

					avVideoView.setPosTop(99999);
					avVideoView.setPosLeft(99999);
					avVideoView.autoLayout();
				}

				viewConfigs.clear();

				ILVLiveManager.getInstance().quitRoom(new ILiveCallBack()
				{
					@Override
					public void onSuccess(Object data)
					{
						Log.i("ILVB","QUIT ROOM SUCCESS");

						if (videoFilter != null) {
							videoFilter.setFilter(-1);
							videoFilter.destroyFilter();
							videoFilter = null;
						}

						insideRoom = false;
						quitting = false;

						//this.context.stopService(new Intent("com.tencent.qalsdk.service.QalService"));

						

						//Android is Crashing on logout.

						/*ILiveLoginManager.getInstance().iLiveLogout(new ILiveCallBack()
						{
							@Override
							public void onSuccess(Object data)
							{
								Log.i("ILVB","LOGOUT SUCCESS");
								quitting = false;
							}

							@Override
							public void onError(String module, int errCode, String errMsg)
							{
								Log.i("ILVB","LOGOUT ERROR");
								Log.i("ILVB",module);
								Log.i("ILVB",new Integer(errCode).toString());
								Log.i("ILVB",errMsg);

								quitting = false;
							}
						});*/
					}

					@Override
					public void onError(String module, int errCode, String errMsg)
					{
						Log.i("ILVB","QUIT ERROR");
						Log.i("ILVB",module);
						Log.i("ILVB",new Integer(errCode).toString());
						Log.i("ILVB",errMsg);

						quitting = false;
					}
				});
			}
		});
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
		cordova.getActivity().runOnUiThread(new UpdateViewRunnable(openid));
	}

	public void RegisterPreviews()
	{
		
		/*videoFilter = new TILFilter(this.context);
		videoFilter.setFilter(5);

		videoFilter.setNotifyListener(new TXINotifyListener(){
			@Override
			public void onNotifyEvent (final int event, final Bundle param) {
				Log.i("ILVB", "recv event id " + event);
				if (TILFilter.EventVideoProcess.EVENT_VIDEOPROCESS_FACERECOGNISE_SUCESS == event){
					Log.i("ILVB", "Face Recognise sucess");
				}else if (TILFilter.EventVideoProcess.EVENT_VIDEOPROCESS_FACERECOGNISE_FAILED == event){
					Log.i("ILVB", "Face Recognise failed");
				}
			}
		});*/

		cordova.getActivity().runOnUiThread(new Runnable()
		{
			@Override
			public void run()
			{
				AVVideoCtrl videoCtrl = ILiveSDK.getInstance().getAvVideoCtrl();

				boolean localSuccess, remoteSuccess;

				localSuccess = videoCtrl.setLocalVideoPreProcessCallback(new AVVideoCtrl.LocalVideoPreProcessCallback()
				{
					@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
					@Override
					public void onFrameReceive(AVVideoCtrl.VideoFrame frame) {

						ILVBVideoConfigs ivc = viewConfigs.get(ILiveLoginManager.getInstance().getMyUserId());

						if(ivc != null)
						{
							if(ivc.f != null)
							{
								//Bitmap b = FaceRecognizer.GenerateBitmap(frame);
								ivc.f.verifyNeedRecognizeFace(frame);
							}
						}

						//videoFilter.processData(frame.data, frame.dataLen, frame.width, frame.height, frame.srcType);
					}
				});

				/*remoteSuccess = videoCtrl.setRemoteVideoPreviewCallback(new AVVideoCtrl.RemoteVideoPreviewCallback()
				{
					@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
					@Override
					public void onFrameReceive(AVVideoCtrl.VideoFrame frame)
					{
						ILVBVideoConfigs ivc = viewConfigs.get(frame.identifier);

						AVVideoView videoview = avRootView.getUserAvVideoView(openid, AVView.VIDEO_SRC_TYPE_CAMERA);

						Bitmap b = FaceRecognizer.GenerateBitmap(frame);
						
						videoview.setBackground(b);

						if(ivc != null)
						{
							if(ivc.f != null)
							{
								ivc.f.verifyNeedRecognizeFace(b);
							}
						}
					}
				});*/

				Log.i("ILVB","LOCAL PRE PROCESS: ");
				Log.i("ILVB", localSuccess ? "SUCCESS" : "FAIL");

				//Log.i("ILVB","REMOTE PREVIEW: ");
				//Log.i("ILVB", remoteSuccess ? "SUCCESS" : "FAIL");
			}
		});
	}
}