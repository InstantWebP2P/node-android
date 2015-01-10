# Secure WebSocket(alias SWS) intends to setup Nacl public key exchange over plain WebSocket.


### SWS' handshake process consists of six steps

* 1. WebSocket client connect to WebSocketServer,  

* 2. WebSocket client send ClientHello message to WebSocketServer with their public key/certification information and a Random number,

* 3. WebSocket server verify ClientHello message and decide to continue or reject handshake request,
*  3.a If WebSocket server grant client's handshake request, then send it's public key/certification information, 
       another encrypted Random number and Share-Key as ServerHello message back
*  3.b If WebSocket server reject client's handshake request, then [send invalid certification as ServerHello message back and] close WebSocket client,

* 4. WebSocket client got Server's ServerHello message, then verify Server's certification and decide to continue or reject handshake process,
*  4.a If WebSocket client grant server's handshake request, then send encrypted Share-Key as ClientReady message to Server, then switch to secure-context,
*  4.b If WebSocket client reject server's handshake request, then close WebSocket client,

* 5. WebSocket server got Client's ClientReady message, extract their Share-Key, then switch to secure-context,

* 6. Both WebSocket client and server run on secure-context, done.

### All exchanged data format is JSON.

### Copyright tom zhou<iwebpp@gmail.com>

