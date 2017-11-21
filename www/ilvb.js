cordova.define("cordova-plugin-tencent-ilvb.TencentILVB", function(require, exports, module) {
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

        window.addEventListener("orientationchange", function(){
            cordova.exec(
                function(data){console.log("Orientation update success.")},
                function(error){console.log("Orientation update fail.")},
                "TencentILVB",
                "deviceRotated",
                []
            );    
        });
    },
    createRoom: function (configs, resultCallback) {
        cordova.exec(
            resultCallback,
            resultCallback,
            "TencentILVB",
            "createOrJoinRoom",
            [configs.roomid, configs.role, configs.hostid]
        );
    },
	updateViews: function (openIdList, domList) {

        for(let i = 0; i < domList.length; i++){
            let rect = domList[i].getBoundingClientRect();

            cordova.exec(
                function(data){console.log("View updated Successfully." + data)},
                function(error){console.log("Error in updating view." + error)},
                "TencentILVB",
                "updateView",
                [openIdList[i], rect.top, rect.left, rect.width, rect.height, window.devicePixelRatio]);
        }
	},
	quit: function () {

		cordova.exec(
			function(data){console.log("Quited Successfully." + data)},
			function(error){console.log("Error in quiting." + error)},
			"TencentILVB",
			"quit",
			[]);
	}
};

});
