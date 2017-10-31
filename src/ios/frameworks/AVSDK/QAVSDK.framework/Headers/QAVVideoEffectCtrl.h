//
//  QAVVideoEffectCtrl.h
//  QAVVideoEffectCtrl
//
//  Created by roclan on 2016/12/27.
//  Copyright © 2016年 roclan. All rights reserved.
//
#ifndef QAVEFFECT_QAVVideoEffectCtrl_h
#define QAVEFFECT_QAVVideoEffectCtrl_h
#import "QAVSDK/QAVError.h"


@interface QAVVideoEffectCtrl : NSObject

///---------------------------------------------------------------------------------------
/// for应用层调用
///---------------------------------------------------------------------------------------
/*!
 @abstract      创建QAVVideoEffect对象。
 
 @return        成功则返回QAVVideoEffect的实例指针；否则返回nil。
 */
+ (QAVVideoEffectCtrl *)shareContext;

/*!
 @abstract      设置滤镜
 @param         path           当前设置滤镜的本地路径
 @discussion    所设置滤镜的资源文件路径。路径内包含滤镜的配置文件及资源文件。
 */
- (QAVResult)setFilter:(NSString*)path;

/*!
 @abstract      设置挂件
 @param         path           当前设置挂件的本地路径
 @discussion    所设置挂件的资源文件路径。路径内包含挂件的配置文件及资源文件。。
 */
- (QAVResult)setPendant:(NSString*)path;

///---------------------------------------------------------------------------------------
/// end
///---------------------------------------------------------------------------------------

///---------------------------------------------------------------------------------------
/// for OPENSDK内部调用。外部调用会引发错误。
///---------------------------------------------------------------------------------------
/*!
 @abstract      开始工作
 @discussion    需要在开启摄像头之后之前调用。
 */
- (QAVResult)start;

/*!
 @abstract      停止工作
 @discussion    需要在每次关闭摄像头之后调用。
 */
- (QAVResult)stop;

/*!
 @abstract      输入视频帧
 @discussion    向SDK内输入视频帧，输入格式为NV12，输出格式为I420。
 */
- (QAVResult)onInputFrame:(unsigned char *)pBuf toBuf:(unsigned char *)dstBuf BufferSize:(size_t)nBufferSize Width:(size_t)width HeightY:(size_t)heightOfYPlane colorFormat:(int)colorFormat rotate:(int)rotate;

/*!
 @abstract      获取滤镜接口调用次数
 */
- (int)getFilterTime;

/*!
 @abstract      获取挂件接口调用次数
 */
-(int)getPendantTime;

/*!
 @abstract      重置接口调用次数
 */
- (void)resetTime;
/*!
 @abstract      销毁QAVVideoEffect对象。
 */
- (void)destroy;
///---------------------------------------------------------------------------------------
/// end
///---------------------------------------------------------------------------------------

@end
#endif
