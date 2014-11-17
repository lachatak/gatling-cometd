# gatling-cometD [![Build Status](https://travis-ci.org/lachatak/gatling-cometd.svg?branch=master)](https://travis-ci.org/lachatak/gatling-cometd)

This extension is build on top of [Gatling](http://gatling.io/) websocket functionality to support cometD testing via cometD DSL.
```scala
    import org.kaloz.gatling.http.cometd.Predef._

    .execCometD(cometD("Open").open("/bayeux").pushProcessor[TimerCounterProcessor])
    .execCometD(cometD("Handshake").handshake())
    .execCometD(cometD("Connect").connect())

    .execCometD(cometD("Subscribe Timer").subscribe("/timer/${userId}").acceptPushContains(Set("TriggeredTime")))
    .execCometD(cometD("Subscribe Echo").subscribe("/echo/${userId}"))

    .feed(uuidFeeder).exec(cometD("Shout Command").sendCommand("/shout/${userId}", Shout()).acceptResponseContains(Set("${correlationId}", "EchoedMessage", "!!egassem ohcE")))

    .execCometD(cometD("Unsubscribe Timer").unsubscribe("/timer/${userId}"))
    .execCometD(cometD("Unsubscribe Echo").unsubscribe("/echo/${userId}"))
    .execCometD(cometD("Disconnect").disconnect())
```
### Version constraint ###
Current version supports scala 2.11 and gatling 2.1.0-SNAPSHOT

## Run ##
- Clone the project
- Download and install [nodejs](http://nodejs.org/) 
- Switch to the http-cometd-example project and install required nodejs packages with the *npm install* command
- Run the server from the http-cometd-example project directory with the *node server.js* command
- Run the example load test with the *sbt gatling:test* command from the root project

If you would like to use the cometD extension in your own project you should add the following sbt dependency:
```scala
    "org.kaloz.gatling" % "http-cometd" % "1.2.0-SNAPSHOT" % "test"
```

## Server side ##
In order to be able to run and test the DSL we need a simple and easy to use cometD server. I picked [Faye](http://faye.jcoglan.com/) which is a publish-subscribe messaging system based on the Bayeux protocol running under nodejs.
 
## DSL in details##

**DSL is still under development !!**

### Configuration ###
- Configuration requires elements from the default Gatling API. The *wsBaseURl* points to my Faye local server.
```scala
   val httpConf = http
     .wsBaseURL("ws://localhost:8000")
     .wsReconnect
     .wsMaxReconnects(3)
     .disableFollowRedirect
     .disableWarmUp
 ```
- Import the cometD Predef which contains all the necessary language elements. 
```scala
    import org.kaloz.gatling.http.cometd.Predef._
```
### Using CometD DSL elements ###
As there are couple of things going behind the scene you have to use *execCometD* DSL element with the *cometD* protocol. Otherwise some mondatory elements won't be resolved in the sent messages.
### Open cometD connection ###
Open cometD connection to the server using websocket as a transport layer.
```scala
    .execCometD(cometD("Open").open("/bayeux").pushProcessor[TimerCounterProcessor])
```
If you need an async processor you have to specify the processor type using the *pushProcessor* method. I am going to introduce it later.
### Handshake and connect ###
After successful open there are two mandatory steps in cometD. The *handshake* and the *connect*. The *handshake* gives back the clientId which is going to be used in every other subsequent requests since it is mandatory for cometD messages after open. All these things happens behind the scenes inside the extention.
```scala
    .execCometD(cometD("Handshake").handshake())
    .execCometD(cometD("Connect").connect())
```
Each and every cometD protocol related method can be called with extended content. It is a common use case that you have to authenticate the user during the handshake. All you have to do is to call the handshake with some extra information
```scala
    case class Authentication(name:String="user", password:String="password")
    .execCometD(cometD("Handshake").handshake(Handshake(ext=Autentication())))
```
### Send command ###
After the *handshake* and *connect* you are ready to send and receive messages. The easiest scenario is to send
command to the server. Before I send the command I generate a new correlationId for my messages. I use this id to be able to pair requests with responses at the client side. For this I have to instruct the extension to wait until a message arrives which has the same correlationId and contains all other required extra data I need. With this you could fine tune what do you regard as a successful response.
In my case I am waiting for a response message which has the same correlationId and I expect some extra content in the response. It could happen that the response arrives with the correct correlationId but instead of having *EchoedMessage* type it has *Exception* type. In this case the extension doesn't consider it a valid response. It is up to the implementor to define the extra criteria. Strings defined in the matchers field might be anywhere in the response but all of them should be there at once!!
```scala
    .feed(uuidFeeder).exec(cometD("Shout Command").sendCommand("/shout/${userId}", Shout()).acceptResponseContains(Set("${correlationId}", "EchoedMessage", "!!egassem ohcE")))
```
In this case the following request/response will be accepted as a valid pair:
```json
{"channel":"/shout/1","data":{"message":"Echo message!!","userId":"1","correlationId":"b4eab88f-241c-4101-8a3d-6f8a5a49dbbb"},"clientId":"tn4f4mulyxmyg5h29u8shbim0spnbz9"}

{"channel":"/echo/1","data":{"message":"!!egassem ohcE","userId":"1","correlationId":"b4eab88f-241c-4101-8a3d-6f8a5a49dbbb","type":"EchoedMessage"},"id":"n"}
```
The response contains the proper correlationId and "EchoedMessage", "!!egassem ohcE" stings in any order.
### Subscription ###
It is easy to subscribe for some services with the *subscribe* method although it is bit more complicated compared to sending a command.
- The easiert situation is when you would like to subscribe to a channel to trigger some process at the backend which sends some kind of messages to the client but you don't want to process those messages. In this case just don't call the *acceptPushContains* method. The result is that you subscribe for a channel but if the server publishes something it won't be processed at Gatling's side but still generates load.
- The second case is a bit trickier. If you use this option the incoming message will be sent to the push processor Actor behind the scene. Every session has its push processor. This processor actor validates that all the required content can be found in the message like for commands and after that using the provided extractor it converts the message to a predefined type. If the conversion was successful then the new object will be sent to an actor which was implemented by the user to process the message. 
It is a bit confusing for the first sight but I am going to show it later how you could implement your own processor. In your actor you could do whatever you want with the incoming message. In my example I am increasing a counter which drives how many command will be sent during the test. 
```scala
    .execCometD(cometD("Subscribe Timer").subscribe("/timer/${userId}").acceptPushContains(Set("TriggeredTime")))
    .execCometD(cometD("Subscribe Echo").subscribe("/echo/${userId}"))
```
### Unsubscribe and disconnect ###
After the test you might want to unsubscribe and disconnect. The unsubscription removes all the previous subscriptions from the server and from Gatling as well, so it stops listening and processing that specific type of subscription.
```scala
    .execCometD(cometD("Unsubscribe Timer").unsubscribe("/timer/${userId}"))
    .execCometD(cometD("Unsubscribe Echo").unsubscribe("/echo/${userId}"))
    .execCometD(cometD("Disconnect").disconnect())
```

## Check response ##
It is common problem that you would like to accept cometD responses which has specific content. To support this demad there is a *acceptResponseContains* method what you can use after sending a command. Only a message will be accepted which contains all the listed string items in any order. Using this DSL elemen provides some extra methods to fine tune the response procession.
```scala
    .feed(uuidFeeder).exec(cometD("Shout Command").sendCommand("/shout/${userId}", Shout()).acceptResponseContains(Set("${correlationId}", "EchoedMessage", "!!egassem ohcE")).transformer(message => message.fromJson[List[Ack]].head.clientId.get).saveAs("clientId"))
```
- using the *transformer* DSL element if you would like to transform the incoming message to something more meaningful for you. In this case I transform the message to *Ack* object and get the clientId from it. It could be any complex object what is required by your test.
- If you would like to save the extraced value you jast pass the *savaAs* parameter and the resoult will be saved to the session using the given name. In this case I save the extracted clientId to the session with the name *clientId*.
 
## Processing pubished messages coming from the server ##
If you would like to process published messages from the server you have to extends the *PushProcessorActor* class and pass this type to the *pushProcessor* method when you open the cometD channel. For every session there will be created a new actor to process subscriptions. 
```scala
    .execCometD(cometD("Open").open("/bayeux").pushProcessor[TimerCounterProcessor])
```
You have to implement the *messageReceive* method by definig a partial function for every different incoming message types I mentioned at the subscription phase. In my example whenever the server sends a timer message the processor actor checks if it has *TriggeredTime* in its content. If so it converts it to *PublishedMap* type and call the *messageReceive* method. The return value should be a Map containing all the new variables you would like to set to the session.
```scala
class TimerCounterProcessor(sessionHandler: ActorRef) extends PushProcessorActor(sessionHandler) {

  val counter = new AtomicLong(0)

  override def messageReceive = {
    case PublishedMap(channel, data) =>
      Map("counter" -> counter.getAndIncrement)
  }
}
```
As the Map will be used to update the session behind the scene in your test you could use any gatling DSL element to react on those new session changes.  
```scala
      .asLongAs(session => session("counter").asOption[Long].getOrElse(0l) < 4) {
      pause(1, 2).feed(uuidFeeder).exec(cometD("Shout Command").sendCommand("/shout/${userId}", Shout()).acceptResponseContains(Set("${correlationId}", "EchoedMessage", "!!egassem ohcE")))
    }
```
Tee above example is going to send *shout command* as long as the server doesn't push 3 messages to the UI. As you may noticed I subscribed to the */timer/${userId}* pused messages. So If the server push a message the *TimerCounterProcessor* handles it and provides a new value for the *counter* variable.
It works perfectly if you have any activity in your loop. If this is not the case like you would like to wait for some data to be present in session before sending any new message to the server you have to do it a bit different way. The reason is that a send/publish message forces the synchronization between the main flow and the asynch processor. You have to force it using a special DSL element called *waitFor*.
```scala
    .waitFor(session => session("counter").asOption[Long].getOrElse(0l) < 8) 
    //execute after the condition holds
    .pause(1, 2).feed(uuidFeeder).exec(cometD("Shout Command").sendCommand("/shout/${userId}", Shout()).acceptResponseContains(Set("${correlationId}", "EchoedMessage", "!!egassem ohcE")))
```

What if you have different content in your pubished messages? In this case you have to pass your own extractor function to the subscription method.
```scala
    case class Reponse(...)
  .execCometD(cometD("Subscribe Timer").subscribe("/timer/${userId}").acceptPushContains(Set("TriggeredTime")).extractor message => message.fromJson[List[Response]].head))
```
