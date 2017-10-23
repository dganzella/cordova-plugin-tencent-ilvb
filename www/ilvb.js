module.exports = {
    init: function (listenerList, config, successCallback, errorCallback) {
		
        cordova.exec(
            successCallback,
            errorCallback,
            "TencentILVB",
            "init",
            [config.sdkAppId, config.accountType, config.openid, config.userSig, listenerList.onLocalStreamAdd, listenerList.onUpdateRemoteStream]
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
	updateViews: function (openIdList, viewPositionRectList) {
		    cordova.exec(
            successCallback,
            errorCallback,
            "TencentILVB",
            "updateViews",
            [openIdList, viewPositionRectList]
        );
	}
};
