# gatling-cometd #

This extension is build on top of [Gatling](http://gatling.io/) websocket functionality to support cometD testing via cometD DSL.
```
    .exec(ws("Handshake").handshake)
    .exec(ws("Connect").connect)

    .exec(ws("Subscribe Timer").subscribe("/timer"))
    .exec(ws("Shout Command").sendCommand("/shout/${userId}", Shout()).checkResponse(m => if (m.contains("EchoedMessage")) m else ""))

    .exec(ws("Unsubscribe Timer").unsubscribe("/timer"))
    .exec(ws("Disconnect").disconnect)
```

## Run ##
- clone the project
- download and install nodejs 
- install the nodejs requirements (npm install)
- run the server (node server.js)
- run the example load test (sbt gatling:test)
