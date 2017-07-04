var localVideo;
var remoteVideo;
var peerConnection;
var uuid;
var socket;


var peerConnectionConfig = {
    'iceServers': [
        {'urls': 'stun:stun.services.mozilla.com'},
        {'urls': 'stun:stun.l.google.com:19302'},
    ]
};

function pageReady() {

    localVideo = document.getElementById('localVideo');
    remoteVideo = document.getElementById('remoteVideo');
	
	serverConnection = null;

    //serverConnection = new WebSocket('wss://' + window.location.hostname + ':8443');
	//serverConnection = new WebSocket('wss://' + '127.0.0.1' + ':8443');
    //serverConnection.onmessage = gotMessageFromServer;
	
	socket = io('http://13.124.155.2:8888');
	//socket = io();

    var constraints = {
        video: true,
        audio: true,
    };

    if(navigator.mediaDevices.getUserMedia) {
        navigator.mediaDevices.getUserMedia(constraints).then(getUserMediaSuccess).catch(errorHandler);
    } else {
        alert('Your browser does not support getUserMedia API');
    }
	
	
socket.on('offerIce', function(message) {
	console.log('addIce ->' + message);
	if(!peerConnection) 
		start(false);
	
	var ice = JSON.parse(message)
	peerConnection.addIceCandidate(new RTCIceCandidate(ice)).catch(errorHandler);
});

socket.on('hi', function(message) {
	console.log('hi ->' + message);
});

	
socket.on('offerSdp', function(message) {
	console.log('offerSdp ->' + message);
	if(!peerConnection)
		start(false);

    var sdp = JSON.parse(message);
	peerConnection.setRemoteDescription(new RTCSessionDescription(sdp)).then(function() {
		// Only create answers in response to offers
		if(sdp.type == 'offer') {
			console.log('createAnswer');
			peerConnection.createAnswer().then(createdDescription).catch(errorHandler);
		}
	}).catch(errorHandler);
    
});
}


function getUserMediaSuccess(stream) {
    localStream = stream;
    localVideo.src = window.URL.createObjectURL(stream);
}

function start(isCaller) {
    peerConnection = new RTCPeerConnection(peerConnectionConfig);
    peerConnection.onicecandidate = gotIceCandidate;
    peerConnection.onaddstream = gotRemoteStream;
    peerConnection.addStream(localStream);

    if(isCaller) {
        peerConnection.createOffer().then(createdDescription).catch(errorHandler);
    }
}

function createdDescription(description) {
    console.log('got description');

    peerConnection.setLocalDescription(description).then(function() {
		socket.emit('answerSdp', JSON.stringify({'remoteSdp': peerConnection.localDescription, 'uuid': uuid}));
       // serverConnection.send(JSON.stringify({'sdp': peerConnection.localDescription, 'uuid': uuid}));
    }).catch(errorHandler);
}

function gotIceCandidate(event) {
	console.log('gotIceCandidate');
    if(event.candidate != null) {
        //serverConnection.send(JSON.stringify({'ice': event.candidate, 'uuid': uuid}));
		socket.emit('answerIce', JSON.stringify({'ice': event.candidate, 'uuid': uuid}));
    }
}

function gotRemoteStream(event) {
    console.log('got remote stream');
    remoteVideo.src = window.URL.createObjectURL(event.stream);
}

function errorHandler(error) {
    console.log(error);
}