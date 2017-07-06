const fs = require('fs');

var app = require('http').createServer(handleRequest);
var io = require('socket.io').listen(app);


app.listen(8888);

// ----------------------------------------------------------------------------------------

// Create a server for the client html page
function handleRequest(request, response) {
}

io.sockets.on('connection', function(socket) {
   var socketId = socket.id;
  var clientIp = socket.request.connection.remoteAddress;
  console.log(socketId + '/ New connection from ' + clientIp);

  socket.on('create', function(message) {

  });

    socket.on('answerSdp', function(message) {
        console.log('answerSdp: %s', message);
        socket.broadcast.emit('answerSdp', message);
    });

	socket.on('answerIce', function(message) {
        console.log('answerIce: %s', message);
        socket.broadcast.emit('answerIce', message);
    });

	 socket.on('offerSdp', function(message) {
        console.log('offerSdp: %s', message);
        socket.broadcast.emit('offerSdp', message);
    });

	 socket.on('offerIce', function(message) {
        console.log('offerIce: %s', message);
        socket.broadcast.emit('offerIce', message);
    });

	socket.on('disconnect', function() {
		console.log('disconnect ');
		io.sockets.emit('disconnect');
	});
});

io.sockets.on('disconnect', function() {
	console.log('disconnect ');
	io.sockets.emit('disconnect');
});
