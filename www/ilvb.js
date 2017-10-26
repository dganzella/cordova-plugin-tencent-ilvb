module.exports = {
	
	eventList: null,
	
    eventReceived: function (response)
	{
        window.WebRTCAPI.eventList[response.eventType](response.data);
    },
	
    init: function (listenerList, config) {
		
		window.WebRTCAPI.eventList = listenerList;
		
		cordova.exec(window.WebRTCAPI.eventReceived,
					function(){},
					"TencentILVB",
					'addEvents',
					[]);
		
        cordova.exec(
            window.WebRTCAPI.eventList['onInitResult'],
            window.WebRTCAPI.eventList['onInitResult'],
            "TencentILVB",
            "init",
            [config.sdkAppId, config.accountType, config.openid, config.userSig]
        );
    },
    createRoom: function (configs, resultCallback) {
        cordova.exec(
            resultCallback,
            resultCallback,
            "TencentILVB",
            "createOrJoinRoom",
            [configs.roomId, configs.role, configs.hostId]
        );
    },
	updateViews: function (openIdList, viewPositionRectList) {
		    cordova.exec(
            function(data){console.log("Views updated Successfully.")},
            function(error){console.log("Error in updating views.")},
            "TencentILVB",
            "updateViews",
            [openIdList, viewPositionRectList]
        );
	}
};
