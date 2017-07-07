const fs = require('fs');
const path = require('path');
const url = require('url');


var options = {
    key: fs.readFileSync('key.pem'),
    cert: fs.readFileSync('cert.pem')
};

var app = require('https').createServer(options, handleRequest);
var io = require('socket.io').listen(app);


app.listen(443);

// ----------------------------------------------------------------------------------------

// Create a server for the client html page
function handleRequest(request, response) {

	const parsedUrl = url.parse(request.url);
  // extract URL path
  var pathname = `.${parsedUrl.pathname}`;

  console.log('pathname=' + pathname);

   fs.exists(pathname, function (exist) {
    if(!exist) {
      // if the file is not found, return 404
      response.statusCode = 404;
      response.end(`File ${pathname} not found!`);
      return;
    }

  if (fs.statSync(pathname).isDirectory()) {
      pathname += '/index.html';
    }

  fs.readFile(pathname, function (err,data) {
    if (err) {
      response.writeHead(404);
      response.end(JSON.stringify(err));
      return;
    }
    response.writeHead(200);
    response.end(data);
  });
   });
}


var created = false;
var createId = "";
var offerSdp = "";
var offerIce = [];

io.sockets.on('connection', function(socket) {
  var socketId = socket.id;
  var clientIp = socket.request.connection.remoteAddress;
  console.log(socketId + '/ New connection from ' + clientIp);

  socket.on('create', function(message) {
    console.log('create');
    if (created) {
      console.log('already created');
    } else {
      created = true;
      createId = socketId;
      socket.emit('createdRoom', socketId, 'success');
    }
  });

  socket.on('join', function(message) {
    if (created) {
      console.log('join / ' + offerIce.length);
      socket.emit('offerSdp', offerSdp);
      for (var i = 0; i < offerIce.length; i++) {
        socket.emit('offerIce', offerIce[i]);
      }
    } else {
      console.log('not created room');
    }
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
    offerSdp = message;
    //socket.broadcast.emit('offerSdp', message);
  });

  socket.on('offerIce', function(message) {
    console.log('offerIce: %s', message);
    offerIce.push(message);
    console.log('offerIce count=' + offerIce.length);
    // socket.broadcast.emit('offerIce', message);
  });

  socket.on('disconnect', function() {
    console.log('disconnect ');
    io.sockets.emit('disconnect');

    if (createId == socketId) {
      created = false;
      offerIce = [];
      console.log('destroyed');
    }
  });
});
