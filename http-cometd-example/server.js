var http = require('http'),
    faye = require('faye');

faye.logger = function (message) {
    console.log('log', message);
}

var server = http.createServer(),
    bayeux = new faye.NodeAdapter({mount: '/bayeux', timeout: 45});

var bayeuxClient = bayeux.getClient();
var timers = {};

var isTimer = function (channel) {
    return channel.split('/').slice(1)[0] === 'timer';
}

var setTimer = function (channel) {
        timers[channel] = setTimeout(function () {
            var date = new Date();
            bayeuxClient.publish(channel, {datetime: date.getTime(), type: 'TriggeredTime'});
            setTimer(channel);
        }, 5000);
}

bayeux.bind('subscribe', function (clientId, channel) {
    if (isTimer(channel))
        setTimer(channel);
});

bayeux.bind('unsubscribe', function (clientId, channel) {
    if (isTimer(channel)) {
        clearTimeout(timers[channel]);
    }
});

bayeuxClient.subscribe('/shout/*', function (data) {
    data.message = data.message.split("").reverse().join("");
    data.type = "EchoedMessage";
    bayeuxClient.publish('/echo/' + data.userId, data);
});

Logger = {
    incoming: function (message, callback) {
        console.log('incoming', message);
        callback(message);
    },
    outgoing: function (message, callback) {
        console.log('outgoing', message);
        callback(message);
    }
};

bayeuxClient.addExtension(Logger);

bayeux.attach(server);
server.listen(8000);

console.log("Server listening on port %d", server.address().port);


