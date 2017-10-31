#import "TencentILVB.h"

@implementation TencentILVB
{
    UIView * videosview;
    NSString * callbackId;
}

-(void) pluginInitialize
{
    NSLog(@"ILVB initialized");

    videosview = [[UIView alloc] initWithFrame:CGRectMake(0, 0, [[UIScreen mainScreen] bounds].size.width, [[UIScreen mainScreen] bounds].size.height)];
    [videosview setBackgroundColor:[UIColor clearColor]];
}
- (void)addEvent:(CDVInvokedUrlCommand*)command
{
    NSLog(@"Add event with success");
    callbackId = command.callbackId;
}

- (void)triggerJSEvent:(NSString*)type withData:(NSMutableDictionary*) data
{
    NSMutableDictionary* message = [[NSMutableDictionary alloc] init];
    [message setObject:type forKey:@"eventType"];
    [message setObject:data forKey:@"data"];
    
    CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:message];
    [pluginResult setKeepCallbackAsBool:YES];
    
    [self.commandDelegate sendPluginResult:pluginResult callbackId:callbackId];
}


- (BOOL)onEndpointsUpdateInfo:(QAVUpdateEvent)event updateList:(NSArray *)endpoints
{
    NSLog(@"ENDPOINTS UPDATE INFO RECEIVED");
    
    for (NSString* openid in endpoints)
    {
        NSMutableDictionary* eventData = [[NSMutableDictionary alloc] init];
        [eventData setObject:openid forKey:@"openid"];
        
        if(event == QAV_EVENT_ID_ENDPOINT_HAS_CAMERA_VIDEO)
        {
            if([openid isEqualToString: [ILiveLoginManager getInstance].getLoginId])
            {
                [self triggerJSEvent: @"onLocalStreamAdd" withData: eventData];
            }
            else
            {
                [self triggerJSEvent: @"onUpdateRemoteStream" withData: eventData];
            }
        }
        else if(event == QAV_EVENT_ID_ENDPOINT_NO_CAMERA_VIDEO)
        {
            if(![openid isEqualToString: [ILiveLoginManager getInstance].getLoginId])
            {
                [self triggerJSEvent: @"onRemoteStreamRemove" withData: eventData];
            }
        }
    }
    
    return false;
}

- (void)init:(CDVInvokedUrlCommand*)command
{
    NSString* sdkppid = [[command arguments] objectAtIndex:0];
    NSLog(@"SDK APP ID: %@", sdkppid);
    
    NSString* accounttype = [[command arguments] objectAtIndex:1];
    NSLog(@"ACCOUNT TYPE: %@", accounttype);
    
    [[ILiveSDK getInstance] initSdk:[sdkppid intValue] accountType:[accounttype intValue]];
    [[ILiveSDK getInstance] setConsoleLogPrint:YES];
    
    NSString* openid = [[command arguments] objectAtIndex:2];
    NSLog(@"OPEN ID: %@", openid);
    
    NSString* usersig = [[command arguments] objectAtIndex:3];
    NSLog(@"USER SIG: %@", usersig);
    
    [[ILiveLoginManager getInstance] iLiveLogin:openid sig:usersig succ:^{
        
        NSLog(@"Logged in successfully");

        [[TILLiveManager getInstance]  setAVRootView:videosview];
        
        CDVPluginResult* result = [CDVPluginResult
                                   resultWithStatus:CDVCommandStatus_OK
                                   messageAsString:@"Logged Successfully"];
        
        [self.commandDelegate sendPluginResult:result callbackId:command.callbackId];
        
    } failed:^(NSString *moudle, int errId, NSString *errMsg) {
        NSLog(@"Login failed");
    }];
}


- (void)createRoom:(CDVInvokedUrlCommand*)command
{
    NSLog(@"CREATE ROOM");
    
    int roomId = [[[command arguments] objectAtIndex:0] intValue];
    
    NSLog(@"ROOM ID: %d", roomId);
    
    NSString*  role = [[command arguments] objectAtIndex:1];
    
    NSLog(@"ROLE: %@", role);
    
    NSString*  hostId = [[command arguments] objectAtIndex:2];
    
    NSLog(@"HOST ID: %@", hostId);
    
    if([role isEqualToString:@"LiveMaster"])
    {
        NSLog(@"IS HOST, WILL CREATE ROOM");
        
        TILLiveRoomOption *option = [TILLiveRoomOption defaultHostLiveOption];
        option.imOption.imSupport = NO;
        option.avOption.autoCamera = true;
        option.avOption.autoMic = true;
        option.memberStatusListener = self;
        
        [[TILLiveManager getInstance] createRoom:roomId option:option succ:^
        {
            NSLog(@"createRoom succ");
        } failed:^(NSString *module, int errId, NSString *errMsg) {
            
            NSString *errinfo = [NSString stringWithFormat:@"module=%@,errid=%d,errmsg=%@",module,errId,errMsg];
            NSLog(@"createRoom fail.%@",errinfo);
        }];
    }
    else
    {
        NSLog(@"IS VIEWER, WILL JOIN ROOM");
        
        TILLiveRoomOption *option = [TILLiveRoomOption defaultGuestLiveOption];
        option.imOption.imSupport = NO;
        option.avOption.autoCamera = true;
        option.avOption.autoMic = true;
        option.memberStatusListener = self;
        
        [[TILLiveManager getInstance] joinRoom:roomId option:option succ:^
         {
             NSLog(@"joinRoom succ");
         } failed:^(NSString *module, int errId, NSString *errMsg) {
             
             NSString *errinfo = [NSString stringWithFormat:@"module=%@,errid=%d,errmsg=%@",module,errId,errMsg];
             NSLog(@"joinRoom fail.%@",errinfo);
         }];
    }
}


- (void)updateView:(CDVInvokedUrlCommand*)command
{
    NSLog(@"UPDATE VIEW");
    
    NSString* openid = [[command arguments] objectAtIndex:0];
    
    NSLog(@"OPEN ID: %@", openid);
    
    int top = [[[command arguments] objectAtIndex:1] intValue];
    int left = [[[command arguments] objectAtIndex:2] intValue];
    int width = [[[command arguments] objectAtIndex:3] intValue];
    int height = [[[command arguments] objectAtIndex:4] intValue];
    double ratio = [[[command arguments] objectAtIndex:0] doubleValue];
    
    NSLog(@"TOP: %d", top);
    NSLog(@"LEFT: %d", left);
    NSLog(@"WIDTH: %d", width);
    NSLog(@"HEIGHT: %d", height);
    NSLog(@"RATIO: %f", ratio);
    
    [[TILLiveManager getInstance] addAVRenderView: CGRectMake((int)(left*ratio), (int)(top*ratio), (int)(width*ratio), (int)(height*ratio)) forIdentifier:openid srcType: QAVVIDEO_SRC_TYPE_CAMERA];
}

- (void)quit:(CDVInvokedUrlCommand*)command
{
    NSLog(@"QUIT");
    
    TILLiveManager *manager = [TILLiveManager getInstance];

    [manager quitRoom:^{
        NSLog(@"quit Room succ");
    } failed:^(NSString *module, int errId, NSString *errMsg) {
        NSLog(@"exit room fail.module=%@,errid=%d,errmsg=%@",module,errId,errMsg);
    }];
}


@end
