var http = require('http'),
    faye = require('faye');

faye.logger = function (message){
    console.log('log', message);
}

var server = http.createServer(),
    bayeux = new faye.NodeAdapter({mount: '/beyaux', timeout: 45});

var bayeuxClient = bayeux.getClient();

Logger = {
    incoming: function(message, callback) {
        console.log('incoming', message);
        callback(message);
    },
    outgoing: function(message, callback) {
        console.log('outgoing', message);
        callback(message);
    }
};

bayeuxClient.addExtension(Logger);

bayeux.attach(server);
server.listen(8000);

console.log("Server listening on port %d", server.address().port);


