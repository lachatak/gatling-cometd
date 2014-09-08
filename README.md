# gatling-cometd [![Build Status](https://travis-ci.org/lachatak/gatling-cometd.svg?branch=master)](https://travis-ci.org/lachatak/gatling-cometd)

This extension is build on top of [Gatling](http://gatling.io/) websocket functionality to support cometD testing via cometD DSL.
```scala
    import org.kaloz.gatling.http.cometd.Predef._

    .exec(cometd("Open").open("/beyaux").registerPubSubProcessor)
    .feed(idFeeder).exec(cometd("Handshake").handshake())
    .feed(idFeeder).exec(cometd("Connect").connect())

    .feed(idFeeder).exec(cometd("Subscribe Timer").subscribe("/timer/${userId}", Set("TriggeredTime")))
    .feed(idFeeder).exec(cometd("Subscribe Echo").subscribe("/echo/${userId}", subscribeToPubSubProcessor = false))

    .feed(uuidFeeder).exec(cometd("Shout Command").sendCommand("/shout/${userId}", Shout()).checkResponse(matchers = Set("${correlationId}", "EchoedMessage", "!!egassem ohcE")))

    .feed(idFeeder).exec(cometd("Unsubscribe Timer").unsubscribe("/timer/${userId}"))
    .feed(idFeeder).exec(cometd("Unsubscribe Echo").unsubscribe("/echo/${userId}"))
    .feed(idFeeder).exec(cometd("Disconnect").disconnect())
```

## Run ##
- Clone the project
- Download and install [nodejs](http://nodejs.org/) 
- Install required nodejs packages with the *npm install* command
- Run the server from the project directory with the *node server.js* command
- Run the example load test with the *sbt gatling:test* command

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
- Import the cometd package object which contains all the necessary language elements. 
```scala
    import org.kaloz.gatling.http.cometd.Predef._
```
### Open websocket connection ###
Open websocket connection to the server. The nature of cometD the extension required some modifications in the Gatling engine. Instead of modifying the Gatling itself I use implicit conversions to extend the functionality. The *registerPubSubProcessor* method forces Gatling to use my own WsActor implementation to be able to listen on published messages in the background.
```scala
    .exec(cometd("Open").open("/beyaux").registerPubSubProcessor)
```
### Handshake and connect ###
After successful websocket open there are two mandatory steps in cometD. The *handshake* and the *connect*. The *handshake* gives back the clientId which is going to be used in every other subsequent requests. It is mandatory for cometD.
```scala
    .feed(idFeeder).exec(cometd("Handshake").handshake())
    .feed(idFeeder).exec(cometd("Connect").connect())
```
Each and every cometD protocol related method can be called with extended content. It is a common use case that you have to authenticate the user during the handshake. All you have to do is to call the handshake with some extra information
```scala
    case class Authentication(name:String="user", password:String="password")
    .feed(idFeeder).exec(cometd("Handshake").handshake(Handshake(ext=Autentication())))
```
### Send command ###
After the *handshake* and *connect* you are ready to send and receive messages. The easiest scenario is to send
command to the server. Before I send the command I generate a new correlationId for my message. I use this id to be able to pair requests with responses at the client side. For this I have to instruct the extension to wait until a message arrives which has the same correlationId and contains all other required extra data I need. With this you could fine tune what do you regard as a successful response.
In my case I am waiting for a response message which has the same correlationId and I expect some extra content in the response. It could happen that the response arrives with the correct correlationId but instead of having *EchoedMessage* type it has *Exception* type. In this case the extension doesn't consider it a valid response. It is up to the implementor to define the extra criteria. Strings defined in the matchers field might be anywhere in the response but all of them should be there at once!!
```scala
    .feed(uuidFeeder).exec(cometd("Shout Command").sendCommand("/shout/${userId}", Shout()).checkResponse(matchers = Set("${correlationId}", "EchoedMessage", "!!egassem ohcE")))
```
### Subscription ###
It is easy to subscribe for some services with the *subscribe* method although it is bit more complicated compared to sending a command.
- The easiert situation is when you would like to subscribe to a channel to trigger some process at the backend which sends some kind of messages to the client but you don't want to process those messages. In this case use *subscribeToPubSubProcessor* attribute with false value. The result is that you subscribe for a channel but if the server publishes something it won't be processed at Gatling's side but still generates load.
- The second case is a bit trickier. If you use this option the incoming message will be sent to a processor Actor behind the scene. This processor actor validates that all the required content can be found in the message like for commands and after that using the provided extractor it converts the message to a predefined type. If the conversion was successful then the new object will be sent to an actor which was implemented by the user to process the message. 
It is a bit confusing for the first sight but I am going to show it later how you could implement your own processor. In your actor you could do whatever you want with the incoming message. In my example I am increasing a counter which drives how many command will be sent during the test. 
```scala
    .feed(idFeeder).exec(cometd("Subscribe Echo").subscribe("/echo/${userId}", subscribeToPubSubProcessor = false))
    .feed(idFeeder).exec(cometd("Subscribe Timer").subscribe("/timer/${userId}", Set("TriggeredTime")))
```
### Unsubscribe and disconnect ###
After the test you might want to unsubscribe and disconnect. The unsubscription removes all the previous subscriptions from the server and from Gatling as well, so it stops listening and processing that specific type of subscription.
```scala
    .feed(idFeeder).exec(cometd("Unsubscribe Timer").unsubscribe("/timer/${userId}"))
    .feed(idFeeder).exec(cometd("Unsubscribe Echo").unsubscribe("/echo/${userId}"))
    .feed(idFeeder).exec(cometd("Disconnect").disconnect())
```

## Processing pubished messages coming from the server ##
If you would like to process published messages from the server you have to extends the *PubSubProcessorActor* class and create a new instance in the Gatling's actor system. *PubSubProcessorActor* subscribes for events published by the *CometDActor* via *GatlingActorSystem*. Later on you could use this reference to obtain information about the background processes.
```scala
    val processor = GatlingActorSystem.instance.actorOf(Processor.props, name = "Processor")
```
You have to implement the *messageReceive* method and process the incoming typed messages I mentioned at the subscription phase. In my example whenever the server sends a timer message the processor actor checks if it has *TriggeredTime* in its content. If so it converts it to *PublishedMap* type and sends to itself. As the base class's receive method doesn't handle this type the *messageReceive* partial function will be called with the new object where I just increment a counter.
```scala
class Processor extends PubSubProcessorActor {

  val counter = new AtomicLong(0)

  def messageReceive: Actor.Receive = {
    case PublishedMap(channel, data) =>
      counter.getAndIncrement
    case GetCounter =>
      sender ! counter.get
  }
}
```
As I have the actor's reference I could ask it to give back some stored results. Based on those results the test could do different things. In my case I send commands to the server as long as the server doesn't publish 4 messages to the timer channel. But based on this technique you could implement more complex scenarios. Obviously the *Await* is not the best solution as it blocks but for my tests it worked pretty well. I am looking into it how I could provide a bit nicer solutionfor this.  
```scala
      .asLongAs(session => {
      implicit val timeout = Timeout(5 seconds)
      import akka.pattern.ask

      val counterFuture = (processor ? GetCounter).mapTo[Long]
      val counter = Await.result(counterFuture, timeout.duration)
      counter < 5
    }) {
      feed(uuidFeeder).exec(cometd("Shout Command").sendCommand("/shout/${userId}", Shout()).checkResponse(matchers = Set("${correlationId}", "EchoedMessage", "!!egassem ohcE")))
        .pause(2, 4)
    }
```
What if you have different content in your pubished messages? In this case you have to pass your own extractor function to the subscription method.
```scala
    case class Reponse(...)
    .feed(idFeeder).exec(cometd("Subscribe Timer").subscribe("/timer/${userId}", Set("TriggeredTime"), extractor: String => Published = { m => m.fromJson[List[Response]].get(0)}))
```    
    
