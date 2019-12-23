node-android
===============

Run Node.js on Android by rewrite Node.js in  Java with the compatible API.


third-party: libuvpp, libuv-java JNI code by Oracle.


### Build

  Clone the code, open Android Studio (1.*) and import the project.
  
  For Eclipse ADT user, refer to [ADT branch](https://github.com/InstantWebP2P/node-android/tree/adt)


### Javascript code injection

```bash
> adb shell am start -a android.intent.action.VIEW -n com.iwebpp.nodeandroid/.MainActivity -e js "var run = function () { return 'hello world'; } run();"
```
  
### Features

* Node.js 0.10.x compatible API by rewrite NodeJS in Java
* Multi-threading: run separate node context in Java thread
* [libUV native support](https://github.com/InstantWebP2P/node-android/tree/master/app/src/main/java/com/iwebpp/libuvpp)
* Timer, set/clear Timeout/Interval
* EventEmitter
* Stream
* [HttpParser - rewrite http-parser.c in java](https://github.com/InstantWebP2P/node-android/blob/master/app/src/main/java/com/iwebpp/node/HttpParser.java)
* HTTP
* [HTTPP - run http over udp](https://github.com/InstantWebP2P/node-android/blob/master/app/src/main/java/com/iwebpp/node/http/httpp.java)
* TCP
* [UDT - udp transport](https://github.com/InstantWebP2P/node-android/blob/master/app/src/main/java/com/iwebpp/node/net/UDT.java)
* DNS
* URL
* IPv6
* [NodeJS alike API](https://github.com/InstantWebP2P/node-android/tree/master/app/src/main/java/com/iwebpp/node)
* [WebSocket, WebSocketServer](https://github.com/InstantWebP2P/node-android/tree/master/app/src/main/java/com/iwebpp/wspp)
* Connect middleware
* [Crypto: NaCL support, public box,secret box,signature/verify](https://github.com/InstantWebP2P/node-android/blob/master/app/src/main/java/com/iwebpp/crypto/TweetNaclFast.java)
* [SecureWebSocket over NaCL](https://github.com/InstantWebP2P/node-android/blob/master/app/src/main/java/com/iwebpp/wspp/SecureWebSocket.java)
* [NaCL Cert](https://github.com/InstantWebP2P/node-android/blob/master/app/src/main/java/com/iwebpp/crypto/NaclCert.java)


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
* [JS API usages details](https://github.com/InstantWebP2P/node-android/tree/master/app/src/main/java/com/iwebpp/node/js)


### TODO

* API doc, more demos
* JS runtime CommonJS/AMD compliance


### Support us

* Welcome contributing on document, codes, tests and issues

<br/>
### License

(see LICENSE file)

Copyright (c) 2014-present Tom Zhou(iwebpp@gmail.com)
