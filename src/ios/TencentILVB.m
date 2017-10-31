#import "TencentILVB.h"

@implementation TencentILVB
{
    UIView * videosview;
    NSString * callbackId;
    NSMutableDictionary *viewsPositions;

}

-(void) pluginInitialize
{
    viewsPositions = [[NSMutableDictionary alloc] init];
    
    videosview = [[UIView alloc] initWithFrame:CGRectMake(0, 0, [[UIScreen mainScreen] bounds].size.width, [[UIScreen mainScreen] bounds].size.height)];
    [videosview setBackgroundColor:[UIColor clearColor]];
}
- (void)addEvent:(CDVInvokedUrlCommand*)command
{
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

    return false;
}

- (void)init:(CDVInvokedUrlCommand*)command
{
    NSString* sdkppid = [[command arguments] objectAtIndex:0];
    NSString* accounttype = [[command arguments] objectAtIndex:1];
    
    [[ILiveSDK getInstance] initSdk:[sdkppid intValue] accountType:[accounttype intValue]];
    [[ILiveSDK getInstance] setConsoleLogPrint:YES];
    
    NSString* openid = [[command arguments] objectAtIndex:2];
    NSString* usersig = [[command arguments] objectAtIndex:3];
    
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
    
    int roomId = [[[command arguments] objectAtIndex:0] intValue];
    NSString*  role = [[command arguments] objectAtIndex:1];
    NSString*  hostId = [[command arguments] objectAtIndex:2];
    
    if([role isEqualToString:@"LiveMaster"])
    {
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
    NSString* openid = [[command arguments] objectAtIndex:0];
    
    int top = [[[command arguments] objectAtIndex:1] intValue];
    int left = [[[command arguments] objectAtIndex:2] intValue];
    int width = [[[command arguments] objectAtIndex:3] intValue];
    int height = [[[command arguments] objectAtIndex:4] intValue];
    double ratio = [[[command arguments] objectAtIndex:0] doubleValue];
    
    [[TILLiveManager getInstance] addAVRenderView: CGRectMake((int)(left*ratio), (int)(top*ratio), (int)(width*ratio), (int)(height*ratio)) forIdentifier:openid srcType: QAVVIDEO_SRC_TYPE_CAMERA];
}

- (void)quit:(CDVInvokedUrlCommand*)command
{
    TILLiveManager *manager = [TILLiveManager getInstance];

    [manager quitRoom:^{
        NSLog(@"quit Room succ");
    } failed:^(NSString *module, int errId, NSString *errMsg) {
        NSLog(@"exit room fail.module=%@,errid=%d,errmsg=%@",module,errId,errMsg);
    }];
}


@end