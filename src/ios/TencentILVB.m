#import "TencentILVB.h"

@implementation TencentILVB

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
        
        
        [[TILLiveManager getInstance] createRoom:roomId option:option succ:^{
            
            NSLog(@"createRoom succ");
            
            
        } failed:^(NSString *module, int errId, NSString *errMsg) {
            
            NSString *errinfo = [NSString stringWithFormat:@"module=%@,errid=%d,errmsg=%@",module,errId,errMsg];
            NSLog(@"createRoom fail.%@",errinfo);
        }];
    }
}


- (void)updateViews:(CDVInvokedUrlCommand*)command
{
    
}

- (void)quit:(CDVInvokedUrlCommand*)command
{
    
}


@end
