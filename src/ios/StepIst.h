#import "Foundation/Foundation.h"
#import "Cordova/CDV.h"

@interface StepIst : CDVPlugin

- (void) isStepCountingAvailable:(CDVInvokedUrlCommand*)command;
- (void) isDistanceAvailable:(CDVInvokedUrlCommand*)command;
- (void) isFloorCountingAvailable:(CDVInvokedUrlCommand*)command;

- (void) startPedometerUpdates:(CDVInvokedUrlCommand*)command;
- (void) stopPedometerUpdates:(CDVInvokedUrlCommand*)command;

- (void) queryData:(CDVInvokedUrlCommand*)command;

@end
