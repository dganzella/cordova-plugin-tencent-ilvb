#import <Cordova/CDV.h>
#import <ImSDK/ImSDK.h>
#import <QAVSDK/QAVSDK.h>
#import <QALSDK/QalSDKProxy.h>
#import <TLSSDK/TLSHelper.h>
#import <IMSDKBugly/IMSDKBugly.h>

#import <ILiveSDK/ILiveCoreHeader.h>
#import <ILiveSDK/ILiveQualityData.h>
#import <ILiveSDK/ILiveSpeedTestManager.h>
#import <TILLiveSDK/TILLiveSDK.h>

@interface TencentILVB : CDVPlugin

- (void)init:(CDVInvokedUrlCommand*)command;
- (void)createRoom:(CDVInvokedUrlCommand*)command;
- (void)updateViews:(CDVInvokedUrlCommand*)command;
- (void)quit:(CDVInvokedUrlCommand*)command;

@end

