'use strict';
app.controller('HomeCtrl', function ($scope, $http, $interval) {
    $scope.imagePath = 'img/washedout.png';
    var devices = null;
    var indoorDevice = new Object();
    var outdoorDevices = [];
    $scope.indoorDevice = null;
    $scope.outdoorDevices = null;
    $scope.kuraDevices = null;
    $scope.deviceNumber = 0;
    var kuraRest = {
        baseURL: 'http://130.149.232.227/api/status?',
        get: function (callback, params) {
            $http({
                method: 'GET',
                url: this.baseURL + params
            }).then(function successCallback(response) {
                // this callback will be called asynchronously
                // when the response is available
                console.log(response);
                callback(response);
            }, function errorCallback(response) {
                // called asynchronously if an error occurs
                // or server returns response with an error status.
                callback(response);
            });
        },
        getDevices: function (callback) {
            var params = "action=getDevices";
            this.get(callback, params);
        },
        removeDevice: function (callback, deviceID) {
            var params = "action=Removal" + "&" + "deviceID=" + deviceID;
            this.get(callback, params);
        },
        keepDevice: function (callback, deviceID) {
            var params = "action=KeepAlive" + "&" + "deviceID=" + deviceID;
            this.get(callback, params);
        },
        deactDevice: function (callback, deviceID) {
            var params = "action=Deactivation" + "&" + "deviceID=" + deviceID;
            this.get(callback, params);
        }
    };
    $scope.removeDevice = function ($event, device) {
    	$event.stopPropagation();
        kuraRest.removeDevice(function (response) {
            console.debug('kura rest', response);
            $scope.kuraDevices.splice($scope.kuraDevices.indexOf(device), 1);
        }, device.ID);
    }
    
//    $scope.formatOutputs = function($event, device, values, units){
//    	$scope.measurementAndResults[device.ID] = [];
//    	if (values != undefined && units != undefined ){	    	
//	    	console.debug(values, units);
//	    	for (var key in values) {
//    		    if (values.hasOwnProperty(key)) {
//    		        console.log(key + " = " + values[key]);
//    		        $scope.measurementAndResults[device].push(key+":"+values[key] +" "+units[key]);
//    		    }
//    		}    
//	    	console.debug("measurements:",$scope.measurementAndResults[device.ID]);
//    	}
//    }
    $scope.getMeasurement = function(values, units){
    	var measurementAndResults = [];
    	if (values != undefined && units != undefined ){	    	
	    	console.debug(values, units);
	    	for (var key in values) {
    		    if (values.hasOwnProperty(key)) {
    		        console.log(key + " = " + values[key]);
    		        measurementAndResults.push(key+":"+values[key] +" "+units[key]);
    		    }
    		}    
	    	console.debug("measurements:",$scope.measurementAndResults);
	    	return measurementAndResults;
    	}
    }
    
    $scope.keepDevice = function ($event, device) {
    	$event.stopPropagation();
        kuraRest.keepDevice(function (response) {
            console.debug('kura rest', response);
        }, device.ID);
    }
    kuraRest.getDevices(function (response) {
        console.debug('kura rest', response);
        $scope.kuraDevices = response.data;
        //console.debug("kura Device", $scope.kuraDevices);
    });
//    
    $interval(function(){
        kuraRest.getDevices(function (response) {
            console.debug('kura rest', response);
            $scope.kuraDevices = response.data;
        //console.debug("kura Device", $scope.kuraDevices);
        });
    },20000);

    kuraRest.getDevices(function (response) {
            console.debug('kura rest', response);
            $scope.kuraDevices = response.data;
        //console.debug("kura Device", $scope.kuraDevices);
    });
    $scope.switchOnOff = function ($event, device){
    	  $event.stopPropagation();
         if(device.Status == "true") {
             kuraRest.deactDevice(function (response) {
                    console.debug('kura rest', response);
                }, device.ID);
         }else {
              kuraRest.keepDevice(function (response) {
                    console.debug('kura rest', response);
                }, device.ID);
         }
    }

    function getStationDate() {
        netatmoapi.getStationsData(function (err, devices) {
            console.log(devices);
            if (devices.length > 0) {
                // outdoorDevices = [];
                indoorDevice = devices[0].dashboard_data;
                indoorDevice.id = devices[0]._id;
                if (devices[0].modules.length > 0) {
                    var l = devices[0].modules.length;
                    var modules = devices[0].modules;
                    for (var i = 0; i < l; i++) {
                    	if (modules[i] != undefined){
                    		var device = modules[i];
                            device.id = modules[i]._id;
                            outdoorDevices.push(device);
                            console.log(JSON.stringify(device));	
                    	}
                    }
                }
            }
            $scope.$apply(function () {
                $scope.indoorDevice = indoorDevice;
                $scope.outdoorDevices = outdoorDevices;
            });
            console.debug("indoorDevice", $scope.indoorDevice);
            console.debug("outdoorDevice", $scope.outdoorDevices);
        });
    }
    getStationDate();
    // setInterval(function () {
    //     getStationDate();
    // },10000);
}).directive('slideable', function () {
    return {
        restrict: 'C',
        compile: function (element, attr) {
            // wrap tag
            var contents = element.html();
            element.html('<div class="slideable_content" style="margin:0 !important; padding:0 !important" >' + contents + '</div>');
            return function postLink(scope, element, attrs) {
                // default properties
                attrs.duration = (!attrs.duration) ? '0.1s' : attrs.duration;
                attrs.easing = (!attrs.easing) ? 'ease-in-out' : attrs.easing;
                element.css({
                    'overflow': 'hidden',
                    'height': '0px',
                    'transitionProperty': 'height',
                    'transitionDuration': attrs.duration,
                    'transitionTimingFunction': attrs.easing,
                    'z-index':'999'
                });
            };
        }
    };
}).directive('slideToggle', function () {
    return {
        restrict: 'A',
        link: function (scope, element, attrs) {
            var target = document.querySelector(attrs.slideToggle);
            attrs.expanded = false;
            element.bind('click', function () {
                var content = target.querySelector('.slideable_content');
                if (!attrs.expanded) {
                    content.style.border = '1px solid rgba(0,0,0,0)';
                    var y = content.clientHeight;
                    content.style.border = 0;
                    target.style.height = y + 'px';
                } else {
                    target.style.height = '0px';
                }
                attrs.expanded = !attrs.expanded;
            });
        }
    }
});
