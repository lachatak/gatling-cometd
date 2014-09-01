# gatling-cometd #

This extension is build on top of [Gatling](http://gatling.io/) websocket functionality to support cometD testing via cometD DSL.
```
    .exec(cometd("Handshake").handshake)
    .exec(cometd("Connect").connect)

    .exec(cometd("Subscribe Timer").subscribe("/timer"))
    .exec(cometd("Shout Command").sendCommand("/echo", Echo()).checkResponse(m => if (m.contains("EchoedMessage")) m else ""))

    .exec(cometd("Unsubscribe Timer").unsubscribe("/timer"))
    .exec(cometd("Disconnect").disconnect)
```

## Run ##
- clone the project
- download and install nodejs (this is the cometd test server to be able to test gatling cometD DSL) 
- install the nodejs requirements (npm install)
- run the server (node server.js)
- run the example load test (sbt gatling:test)

**DSL is still under development !!**
