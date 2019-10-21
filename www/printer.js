var BrotherPrinter = function () {}
BrotherPrinter.prototype = {
    findNetworkPrinters: function (callback, scope) {
        var callbackFn = function () {
            var args = typeof arguments[0] == 'boolean' ? arguments : arguments[0]
            callback.apply(scope || window, args)
        }
        cordova.exec(callbackFn, null, 'BrotherPrinter', 'findNetworkPrinters', [])
    },
    printTemplate: function ( data, printInfo, callback, callbackError) {
        if (!data) {
            console.log('No data passed in. Expects a object template.')
            return
        }
        cordova.exec(callback, callbackError, 'BrotherPrinter', 'printTemplate', [data, printInfo])
    },
    printPDF: function ( data, printInfo, callback, callbackError) {
        if (!data) {
            console.log('No data passed in. Expects a object PDF.')
            return
        }
        cordova.exec(callback, callbackError, 'BrotherPrinter', 'printPDF', [data, printInfo])
    },
    printImage: function ( data, printInfo, callback, callbackError ) {
        if (!data) {
            console.log('No data passed in. Expects a object Image.')
            return
        }
        cordova.exec(callback, callbackError, 'BrotherPrinter', 'printImage', [data, printInfo])
    },
    getTemplates: function ( printInfo, callback, callbackError) {
        cordova.exec(callback, callbackError, 'BrotherPrinter', 'getTemplates', [printInfo])
    },
    addTemplate: function ( data, printInfo, callback, callbackError ) {
        if (!data) {
            console.log('No data passed in. Expects a object template.')
            return
        }
        cordova.exec(callback, callbackError, 'BrotherPrinter', 'addTemplate', [data, printInfo])
    },
    removeTemplates: function ( data, printInfo, callback, callbackError ) {
        if (!data) {
            console.log('No data passed in. Expects a object template.')
            return
        }
        cordova.exec(callback, callbackError, 'BrotherPrinter', 'removeTemplates', [data, printInfo])
    },
    getPrinters: function ( callback, callbackError) {
        cordova.exec(callback, callbackError, 'BrotherPrinter', 'getPrinters', [])
    },
    sendUSBConfig: function (data, callback, callbackError) {
        if (!data || !data.length) {
            console.log('No data passed in. Expects print payload string.')
            return
        }
        cordova.exec(callback, callbackError, 'BrotherPrinter', 'sendUSBConfig', [data])
    },
    getRTCInfo: function ( printInfo, callback, callbackError) {
        cordova.exec(callback, callbackError, 'BrotherPrinter', 'getRTCInfo', [printInfo])
    },
}
var plugin = new BrotherPrinter()
module.exports = plugin
