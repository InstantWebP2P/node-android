node-android
===============

Node.js rewrite for Android with the compatible API.



third-party: libuvpp, libuv-java JNI code by Oracle.


### Usage:

  clone the code, open Android eclipse ADT and import the project.
  
  notes: to run node-android API from Rhino JS engine, add libs/js.jar in build path manually


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
* for API usage, check https://github.com/InstantWebP2P/node-android/tree/httpp/src/com/iwebpp/node/tests
* WebSocket/WebSocketServer supported, check https://github.com/InstantWebP2P/node-android/tree/httpp/src/com/iwebpp/wspp/tests
* Connect middleware


### TODO

* Crypto
* TLS, HTTPS
* UDTS, HTTPPS
* API doc, more demos


<br/>
### License

(The MIT License)

Copyright (c) 2014 Tom Zhou(iwebpp@gmail.com)
