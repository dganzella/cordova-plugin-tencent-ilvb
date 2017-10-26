module.exports = {
	
	eventList: null,
	
    eventReceived: function (response)
	{
        window.WebRTCAPI.eventList[response.eventType](response.data);
    },
	
    init: function (listenerList, config, successCallback, errorCallback) {
		
		window.WebRTCAPI.eventList = listenerList;
		
		cordova.exec(window.WebRTCAPI.eventReceived,
					function(){},
					"TencentILVB",
					'addEvents',
					[]);
		
        cordova.exec(
            successCallback,
            errorCallback,
            "TencentILVB",
            "init",
            [config.sdkAppId, config.accountType, config.openid, config.userSig]
        );
    },
    createRoom: function (configs, successCallback, errorCallback) {
        cordova.exec(
            successCallback,
            errorCallback,
            "TencentILVB",
            "createOrJoinRoom",
            [configs.roomId, configs.role, configs.hostId]
        );
    },
	updateViews: function (openIdList, viewPositionRectList, successCallback, errorCallback) {
		    cordova.exec(
            successCallback,
            errorCallback,
            "TencentILVB",
            "updateViews",
            [openIdList, viewPositionRectList]
        );
	}
};
