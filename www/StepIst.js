var exec = require("cordova/exec");

var stepist = function() {
    this.name = "StepIst";
};

stepist.prototype.isStepCountingAvailable = function(onSuccess, onError) {
    exec(onSuccess, onError, "StepIst", "isStepCountingAvailable", []);
};

stepist.prototype.isDistanceAvailable = function(onSuccess, onError) {
    exec(onSuccess, onError, "StepIst", "isDistanceAvailable", []);
};

stepist.prototype.isFloorCountingAvailable = function(onSuccess, onError) {
    exec(onSuccess, onError, "StepIst", "isFloorCountingAvailable", []);
};

stepist.prototype.startStepIstUpdates = function(onSuccess, onError) {
    exec(onSuccess, onError, "StepIst", "startStepIstUpdates", []);
};

stepist.prototype.stopStepIstUpdates = function(onSuccess, onError) {
    exec(onSuccess, onError, "StepIst", "stopStepIstUpdates", []);
};

stepist.prototype.queryData = function(onSuccess, onError, options) {
    exec(onSuccess, onError, "StepIst", "queryData", [options]);
};

stepist.prototype.recordingAPI = function(onSuccess, onError, options) {
    exec(onSuccess, onError, "StepIst", "recordingAPI", [options]);
};

module.exports = new stepist();