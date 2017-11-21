#import "TencentILVB.h"

@implementation TencentILVB
{
    UIView * videosview;
    NSString * callbackId;
    bool ILVBinitialized, quitting;
}

-(void) pluginInitialize
{
    NSLog(@"ILVB initialized");
    
    self->ILVBinitialized = false;
    self->quitting = false;
    
    videosview = [[UIView alloc] initWithFrame:CGRectMake(0, 0, [[UIScreen mainScreen] bounds].size.width, [[UIScreen mainScreen] bounds].size.height)];
    [videosview setBackgroundColor:[UIColor clearColor]];
}
- (void)addEvents:(CDVInvokedUrlCommand*)command
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

- (void)onFirstFrameRecved:(int)width height:(int)height identifier:(NSString *)identifier srcType:(avVideoSrcType)srcType
{
    
}

- (void)onUserUpdateInfo:(ILVLiveAVEvent)event users:(NSArray *)users
{
    NSLog(@"ENDPOINTS UPDATE INFO RECEIVED");
    
    for (NSString* openid in users)
    {
        NSMutableDictionary* eventData = [[NSMutableDictionary alloc] init];
        [eventData setObject:openid forKey:@"openid"];
        
        NSLog(@"EVENT: %d", (long)event);
        
        if(event == ILVLIVE_AVEVENT_CAMERA_ON)
        {
            if([openid isEqualToString: [ILiveLoginManager getInstance].getLoginId])
            {
                NSLog(@"LOCAL STREAM ADD");
                [self triggerJSEvent: @"onLocalStreamAdd" withData: eventData];
            }
            else
            {
                NSLog(@"REMOTE STREAM ADD");
                [self triggerJSEvent: @"onUpdateRemoteStream" withData: eventData];
            }
        }
        else if(event == ILVLIVE_AVEVENT_CAMERA_OFF)
        {
            if(![openid isEqualToString: [ILiveLoginManager getInstance].getLoginId])
            {
                NSLog(@"REMOTE STREAM REMOVE");
                [self triggerJSEvent: @"onRemoteStreamRemove" withData: eventData];
                
                [[TILLiveManager getInstance] removeAVRenderView: openid srcType: QAVVIDEO_SRC_TYPE_CAMERA];
            }
        }
    }
}

- (void)init:(CDVInvokedUrlCommand*)command
{
    NSString* sdkppid = [[command arguments] objectAtIndex:0];
    NSLog(@"SDK APP ID: %@", sdkppid);
    
    NSString* accounttype = [[command arguments] objectAtIndex:1];
    NSLog(@"ACCOUNT TYPE: %@", accounttype);
    
    
    if(!self->ILVBinitialized){
        [[ILiveSDK getInstance] setLogLevel:ILive_LOG_NONE];
        [[ILiveSDK getInstance] initSdk:[sdkppid intValue] accountType:[accounttype intValue]];
        self->ILVBinitialized = true;
    }
    
    [[ILiveSDK getInstance] setConsoleLogPrint:NO];
    
    NSString* openid = [[command arguments] objectAtIndex:2];
    NSLog(@"OPEN ID: %@", openid);
    
    NSString* usersig = [[command arguments] objectAtIndex:3];
    NSLog(@"USER SIG: %@", usersig);
    
    [[ILiveLoginManager getInstance] iLiveLogin:openid sig:usersig succ:^{
        
        NSLog(@"Logged in successfully");
        
        [[TILLiveManager getInstance]  setAVRootView:videosview];
        [[TILLiveManager getInstance]  setAVListener:self];
        
        CDVPluginResult* result = [CDVPluginResult
                                   resultWithStatus:CDVCommandStatus_OK
                                   messageAsInt:0];
        
        [self.commandDelegate sendPluginResult:result callbackId:command.callbackId];
        
    } failed:^(NSString *module, int errId, NSString *errMsg) {
        NSLog(@"Login failed");
        
        NSLog(@"login fail.module=%@,errid=%d,errmsg=%@",module,errId,errMsg);
        
        CDVPluginResult* result = [CDVPluginResult
                                   resultWithStatus:CDVCommandStatus_ERROR
                                   messageAsInt:errId];
        
        [self.commandDelegate sendPluginResult:result callbackId:command.callbackId];
    }];
}


- (void)createOrJoinRoom:(CDVInvokedUrlCommand*)command
{
    NSLog(@"CREATE OR JOIN ROOM");
    
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
        option.controlRole = role;
        option.imOption.imSupport = NO;
        option.avOption.avSupport = true;
        option.avOption.autoCamera = true;
        option.avOption.autoMic = true;
        option.avOption.autoSpeaker = true;
        
        [[TILLiveManager getInstance] createRoom:roomId option:option succ:^
         {
             NSLog(@"createRoom succ");
             
             CDVPluginResult* result = [CDVPluginResult
                                        resultWithStatus:CDVCommandStatus_OK
                                        messageAsInt:0];
             
             [self.commandDelegate sendPluginResult:result callbackId:command.callbackId];
             
         } failed:^(NSString *module, int errId, NSString *errMsg) {
             
             NSString *errinfo = [NSString stringWithFormat:@"module=%@,errid=%d,errmsg=%@",module,errId,errMsg];
             NSLog(@"createRoom fail.%@",errinfo);
             
             CDVPluginResult* result = [CDVPluginResult
                                        resultWithStatus:CDVCommandStatus_ERROR
                                        messageAsInt:errId];
             
             [self.commandDelegate sendPluginResult:result callbackId:command.callbackId];
         }];
    }
    else
    {
        NSLog(@"IS VIEWER, WILL JOIN ROOM");
        
        TILLiveRoomOption *option = [TILLiveRoomOption defaultInteractUserLiveOption];
        option.controlRole = role;
        option.imOption.imSupport = NO;
        option.avOption.avSupport = true;
        option.avOption.autoCamera = true;
        option.avOption.autoMic = true;
        option.avOption.autoSpeaker = true;
        
        [[TILLiveManager getInstance] joinRoom:roomId option:option succ:^
         {
             NSLog(@"JOIN ROOM SUCCESS");
             
             CDVPluginResult* result = [CDVPluginResult
                                        resultWithStatus:CDVCommandStatus_OK
                                        messageAsInt:0];
             
             [self.commandDelegate sendPluginResult:result callbackId:command.callbackId];
             
         } failed:^(NSString *module, int errId, NSString *errMsg) {
             
             NSLog(@"JOIN ROOM FAIL");
             
             CDVPluginResult* result = [CDVPluginResult
                                        resultWithStatus:CDVCommandStatus_ERROR
                                        messageAsInt:errId];
             
             [self.commandDelegate sendPluginResult:result callbackId:command.callbackId];
         }];
    }
}

- (void)deviceRotated:(CDVInvokedUrlCommand*)command
{
    UIInterfaceOrientation orientation = [UIApplication sharedApplication].statusBarOrientation;
    
    NSLog(@"ORIENTATION: %ld", (long)orientation);
    
    NSLog(@"LEFT IS: %ld, RIGHT IS: %ld",(long)UIInterfaceOrientationLandscapeLeft, (long)UIInterfaceOrientationLandscapeRight);
    
    [self updateRotation:[ILiveLoginManager getInstance].getLoginId];
}

- (void)enableDisableOutput:(CDVInvokedUrlCommand*)command
{
    NSString*  output = [[command arguments] objectAtIndex:0];
    
    NSLog(@"OUTPUT: %@", output);
    
    NSString*  onOff = [[command arguments] objectAtIndex:1];
    
    NSLog(@"ON OFF: %@", onOff);
    
    if([output isEqualToString:@"mic"]){
        
        [[ILiveRoomManager getInstance] enableMic:[onOff isEqualToString:@"on"] succ:^{
            NSLog(@"enable mic succ");
        } failed:^(NSString *module, int errId, NSString *errMsg) {
            NSLog(@"enable mic fail.module=%@,errid=%d,errmsg=%@",module,errId,errMsg);
        }];
    }
    else if ([output isEqualToString:@"video"]){
        [[ILiveRoomManager getInstance] enableCamera: CameraPosFront enable:[onOff isEqualToString:@"on"] succ:^{
            NSLog(@"enable video succ");
        } failed:^(NSString *module, int errId, NSString *errMsg) {
            NSLog(@"enable camera fail.module=%@,errid=%d,errmsg=%@",module,errId,errMsg);
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
    double ratio = [[[command arguments] objectAtIndex:5] doubleValue];
    
    NSLog(@"TOP: %d", top);
    NSLog(@"LEFT: %d", left);
    NSLog(@"WIDTH: %d", width);
    NSLog(@"HEIGHT: %d", height);
    NSLog(@"RATIO: %f", ratio);
    
    ILiveRenderView * renderview = [[TILLiveManager getInstance] getAVRenderView:openid srcType:QAVVIDEO_SRC_TYPE_CAMERA];
    
    if(renderview)
    {
        [renderview setFrame:CGRectMake(left, top, width, height)];
        renderview.rotateAngle = ILIVEROTATION_0;
        renderview.autoRotate = NO;
    }
    else
    {
        renderview = [[TILLiveManager getInstance] addAVRenderView: CGRectMake((int)(left), (int)(top), (int)(width), (int)(height)) forIdentifier:openid srcType: QAVVIDEO_SRC_TYPE_CAMERA];
        
        [self.webView.superview insertSubview:renderview atIndex:0];
        self.webView.layer.zPosition = 999;
        renderview.layer.zPosition = 1;
        
        renderview.autoRotate = NO;
    }
    
    [self updateRotation:openid];
}

- (void) updateRotation: (NSString*) openid
{
    ILiveRenderView * renderview = [[TILLiveManager getInstance] getAVRenderView:openid srcType:QAVVIDEO_SRC_TYPE_CAMERA];
    
    UIInterfaceOrientation orientation = [UIApplication sharedApplication].statusBarOrientation;
    
    if([openid isEqualToString: [ILiveLoginManager getInstance].getLoginId]){
        
        if(orientation == UIInterfaceOrientationLandscapeLeft){
            renderview.rotateAngle = ILIVEROTATION_0;
            [[[ILiveSDK getInstance] getAVContext].videoCtrl setRotation:OrientationLandscapeRight]; //its inverted
        }
        else if(orientation != UIInterfaceOrientationLandscapeLeft){
            renderview.rotateAngle = ILIVEROTATION_180;
            [[[ILiveSDK getInstance] getAVContext].videoCtrl setRotation:OrientationLandscapeLeft]; //its inverted
        }
    }
    else{
        renderview.rotateAngle = ILIVEROTATION_0;
    }
}

- (void)quit:(CDVInvokedUrlCommand*)command
{
    NSLog(@"QUIT");
    
    if(!quitting)
    {
        self->quitting = true;
        
        [[TILLiveManager getInstance] removeAllAVRenderViews];
        
        TILLiveManager *manager = [TILLiveManager getInstance];
        
        [manager quitRoom:^{
            NSLog(@"quit Room succ");
            [[ILiveLoginManager getInstance] iLiveLogout:^{
                NSLog(@"logout succ");
                self->quitting = false;
            } failed:^(NSString *module, int errId, NSString *errMsg) {
                NSLog(@"logout room fail.module=%@,errid=%d,errmsg=%@",module,errId,errMsg);
                self->quitting = false;
            }];
            
        } failed:^(NSString *module, int errId, NSString *errMsg) {
            NSLog(@"exit room fail.module=%@,errid=%d,errmsg=%@",module,errId,errMsg);
            self->quitting = false;
        }];
    }
}


@end

