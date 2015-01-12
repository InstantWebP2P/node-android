node-android
===============

Node.js rewrite for Android with the compatible API.

third-party: libuvpp, libuv-java JNI code by Oracle.


### Build

  Clone the code, open Android Studio (1.*) and import the project.

### Javascript code injection

```bash
> adb shell am start -a android.intent.action.VIEW -n com.iwebpp.nodeandroid/.MainActivity -e js "var run = function () { return 'hello world'; } run();"
```
  
### Features

* Node.js 0.10.x compatible API
* libUV native support
* Timer, set/clear Timeout/Interval
* EventEmitter
* Stream
* HttpParser(rewrite http-parser.c in java)
* HTTP
* HTTPP(run http over udp)
* TCP
* UDT(udp transport)
* DNS
* URL
* IPv6
* for API usage, check https://github.com/InstantWebP2P/node-android/tree/master/src/com/iwebpp/node/tests
* WebSocket/WebSocketServer supported, check https://github.com/InstantWebP2P/node-android/tree/master/src/com/iwebpp/wspp/tests
* Connect middleware
* NACL support, public box,secret box,signature/verify
* SecureWebSocket over NACL


### JS runtime

* Rhino supported
* Exposed node-android packages: com.iwebpp.node.http, com.iwebpp.node.stream, com.iwebpp.node.net, etc
* Exposed node-android classes: com.iwebpp.node.EventEmitter2, com.iwebpp.node.Dns, com.iwebpp.node.Url, etc
* Exposed node-android native context in JS standard scope as NodeCurrentContext alias NCC
* Exposed Android API: android.util.Log
* NodeJS compatible internal modules are available in JS standard scope
* Exposed WebSocket classes: com.iwebpp.wspp.WebSocket, com.iwebpp.wspp.WebSocketServer

### JS usage

* In case Rhino, create class 'MyScript' extends from com.iwebpp.node.js.rhino.Host
* Implement 'public String content()' in 'MyScript' to return user script
* Execute JS engine in a separate Java Thread with 'MyScript.execute()'
* When authoring script, please use NodeCurrentContext(alias NCC) in node-android API
* For details, check https://github.com/InstantWebP2P/node-android/tree/master/src/com/iwebpp/node/js/tests


### TODO

* Crypto
* TLS, HTTPS
* UDTS, HTTPPS
* API doc, more demos
* JS runtime CommonJS compliance


<br/>
### License

(The MIT License)

Copyright (c) 2015 Tom Zhou(iwebpp@gmail.com)
