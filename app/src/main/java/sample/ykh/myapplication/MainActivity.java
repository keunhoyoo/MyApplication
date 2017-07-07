package sample.ykh.myapplication;

import android.app.Dialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.Camera2Enumerator;
import org.webrtc.CameraEnumerator;
import org.webrtc.DataChannel;
import org.webrtc.EglBase;
import org.webrtc.IceCandidate;
import org.webrtc.Logging;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.RendererCommon;
import org.webrtc.RtpReceiver;
import org.webrtc.SdpObserver;
import org.webrtc.SessionDescription;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoCapturer;
import org.webrtc.VideoRenderer;
import org.webrtc.VideoSource;
import org.webrtc.VideoTrack;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;



public class MainActivity extends AppCompatActivity {
    static final String TAG = "MainActivity";
    private Toast logToast;

    private class PCObserver implements PeerConnection.Observer {

        @Override
        public void onSignalingChange(PeerConnection.SignalingState signalingState) {
            Log.d(TAG, "onSignalingChange");
        }

        @Override
        public void onIceConnectionChange(PeerConnection.IceConnectionState iceConnectionState) {
            Log.d(TAG, "onIceConnectionChange -> " + iceConnectionState);
        }

        @Override
        public void onIceConnectionReceivingChange(boolean b) {
            Log.d(TAG, "onIceConnectionReceivingChange -> " + b);
        }

        @Override
        public void onIceGatheringChange(PeerConnection.IceGatheringState iceGatheringState) {
            Log.d(TAG, "onIceGatheringChange -> " + iceGatheringState);
        }

        @Override
        public void onIceCandidate(IceCandidate iceCandidate) {
            Log.d(TAG, "onIceCandidate -> " + iceCandidate);

            // {"ice":{"candidate":"candidate:2682507131 1 udp 2122255103 2001::9d38:90d7:201f:9e11:2c11:9387 63363 typ host generation 0 ufrag PszP network-id 4 network-cost 50","sdpMid":"audio","sdpMLineIndex":0}}
            try {
                JSONObject jsonIce = new JSONObject();

                jsonIce.put("candidate", iceCandidate.sdp);
                jsonIce.put("sdpMid", iceCandidate.sdpMid);
                jsonIce.put("sdpMLineIndex", iceCandidate.sdpMLineIndex);

                if (isOffer) {
                    socketIo.send("offerIce", jsonIce.toString());
                } else {
                    socketIo.send("answerIce", jsonIce.toString());
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }


            peerConnection.addIceCandidate(iceCandidate);
        }

        @Override
        public void onIceCandidatesRemoved(IceCandidate[] iceCandidates) {
            Log.d(TAG, "onIceCandidatesRemoved -> " + iceCandidates);
        }

        @Override
        public void onAddStream(MediaStream mediaStream) {
            Log.d(TAG, "onAddStream");

            VideoTrack videoTrack = mediaStream.videoTracks.get(0);
            videoTrack.setEnabled(true);
            videoTrack.addRenderer(new VideoRenderer(remoteView));
        }

        @Override
        public void onRemoveStream(MediaStream mediaStream) {
            Log.d(TAG, "onRemoveStream");
        }

        @Override
        public void onDataChannel(DataChannel dataChannel) {
            Log.d(TAG, "onDataChannel");
        }

        @Override
        public void onRenegotiationNeeded() {
            Log.d(TAG, "onRenegotiationNeeded");
        }

        @Override
        public void onAddTrack(RtpReceiver rtpReceiver, MediaStream[] mediaStreams) {
            Log.d(TAG, "onAddTrack");
        }
    }

    private class SignalEventHandler implements SocketIoClient.EventHandler {

        @Override
        public void onCreatedRoom(String token, String msg) {
            Log.d(TAG, "onCreatedRoom -> " + token + "," + msg);

            final MediaConstraints sdpConst = new MediaConstraints();
            //sdpConst.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"));
            sdpConst.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"));


            peerConnection.createOffer(new SdpObserver() {
                @Override
                public void onCreateSuccess(final SessionDescription sessionDescription) {
                    Log.d(TAG, "createOffer / onCreateSuccess -> " + sessionDescription.description);

                    peerConnection.setLocalDescription(new SdpObserver() {
                        @Override
                        public void onCreateSuccess(SessionDescription sessionDescription) {
                            Log.d(TAG, "setLocalDescription / onCreateSuccess -> " + sessionDescription);
                        }

                        @Override
                        public void onSetSuccess() {
                            Log.d(TAG, "setLocalDescription / onSetSuccess");


                            try {
                                JSONObject jsonSdp = new JSONObject();
                                jsonSdp.put("type", sessionDescription.type.canonicalForm());
                                jsonSdp.put("sdp", sessionDescription.description);

                                socketIo.send("offerSdp", jsonSdp.toString());
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }

                        }

                        @Override
                        public void onCreateFailure(String s) {
                            Log.d(TAG, "setLocalDescription / onCreateFailure -> " + s);
                        }

                        @Override
                        public void onSetFailure(String s) {
                            Log.d(TAG, "setLocalDescription / onSetFailure -> " + s);
                        }
                    }, sessionDescription);

                }

                @Override
                public void onSetSuccess() {
                    Log.d(TAG, "createOffer / onSetSuccess");
                }

                @Override
                public void onCreateFailure(String s) {
                    Log.d(TAG, "createOffer / onCreateFailure -> " + s);
                }

                @Override
                public void onSetFailure(String s) {
                    Log.d(TAG, "createOffer / onSetFailure" + s);
                }
            }, sdpConst);

        }

        @Override
        public void onOfferSdp(String offerSdp) {
            Log.d(TAG, "onOfferSdp -> " + offerSdp);

            JSONObject jsonRemoteSdp = null;
            try {
                jsonRemoteSdp = new JSONObject(offerSdp);

            JSONObject jsonSdp = jsonRemoteSdp.getJSONObject("offerSdp");

            peerConnection.setRemoteDescription(new SdpObserver() {
                @Override
                public void onCreateSuccess(SessionDescription sessionDescription) {
                    Log.d(TAG, "setRemoteDescription / onCreateSuccess -> " + sessionDescription);
                }

                @Override
                public void onSetSuccess() {
                    Log.d(TAG, "setRemoteDescription / onSetSuccess");

                    final MediaConstraints sdpConst = new MediaConstraints();
                    //sdpConst.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"));
                    sdpConst.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"));

                    peerConnection.createAnswer(new SdpObserver() {
                        @Override
                        public void onCreateSuccess(final SessionDescription sessionDescription2) {
                            Log.d(TAG, "setRemoteDescription / onSetSuccess / createAnswer / onCreateSuccess -> " + sessionDescription2);
                            peerConnection.setLocalDescription(new SdpObserver() {
                                @Override
                                public void onCreateSuccess(SessionDescription sessionDescription) {

                                }
                                @Override
                                public void onSetSuccess() {
                                    Log.d(TAG, "setLocalDescription / onSetSuccess");


                                    try {
                                        JSONObject jsonSdp = new JSONObject();
                                        jsonSdp.put("type", sessionDescription2.type.canonicalForm());
                                        jsonSdp.put("sdp", sessionDescription2.description);

                                        socketIo.send("answerSdp", jsonSdp.toString());
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }

                                }

                                @Override
                                public void onCreateFailure(String s) {

                                }

                                @Override
                                public void onSetFailure(String s) {

                                }
                            }, sessionDescription2);
                        }

                        @Override
                        public void onSetSuccess() {

                        }

                        @Override
                        public void onCreateFailure(String s) {

                        }

                        @Override
                        public void onSetFailure(String s) {

                        }
                    }, sdpConst);

                }

                @Override
                public void onCreateFailure(String s) {
                    Log.d(TAG, "setRemoteDescription / onCreateFailure -> " + s);
                }

                @Override
                public void onSetFailure(String s) {
                    Log.d(TAG, "setRemoteDescription / onSetFailure -> " + s);
                }
            }, new SessionDescription(SessionDescription.Type.OFFER, jsonSdp.getString("sdp")));

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onOfferIce(String offerIce) {
            Log.d(TAG, "onOfferIce -> " + offerIce);


            try {
                JSONObject jsonIce = new JSONObject(offerIce);

                peerConnection.addIceCandidate(new IceCandidate(
                        jsonIce.getString("sdpMid"), jsonIce.getInt("sdpMLineIndex"), jsonIce.getString("candidate")));

            } catch (JSONException e) {
                e.printStackTrace();
            }

        }

        @Override
        public void onAnswerSdp(String remoteSdp) {
            Log.d(TAG, "onAnswerSdp -> " + remoteSdp);

            // {"remoteSdp":{"type":"answer","sdp":"v=0\r\no=- 3971489185360764760 2 IN IP4 127.0.0.1\
            // r\ns=-\r\nt=0 0\r\na=group:BUNDLE video\r\na=msid-semantic: WMS T7qHCpV2Wt7W2rU1AqiBV5N1ihW794quAWAm\r\nm=video 9 UDP/T
            // LS/RTP/SAVPF 96 98 100 127 97 99 101\r\nc=IN IP4 0.0.0.0\r\na=rtcp:9 IN IP4 0.0.0.0\r\na=ice-ufrag:lu6n\r\na=ice-pwd:aIb
            // 8RoQUpa8uHqYqxu2GpZI/\r\na=fingerprint:sha-256 89:01:8C:15:E2:58:C9:B5:54:83:29:2F:B0:0A:75:E8:CD:72:1D:5D:EE:C5:5D:76:A5:27
            // :4A:44:1E:8A:4A:6F\r\na=setup:active\r\na=mid:video\r\na=extmap:2 urn:ietf:params:rtp-hdrext:toffset\r\na=extmap:3 http://www.
            // webrtc.org/experiments/rtp-hdrext/abs-send-time\r\na=extmap:4 urn:3gpp:video-orientation\r\na=extmap:5 http://www.ietf.org/id/d
            // raft-holmer-rmcat-transport-wide-cc-extensions-01\r\na=extmap:6 http://www.webrtc.org/experiments/rtp-hdrext/playout-delay\r\n
            // a=sendrecv\r\na=rtcp-mux\r\na=rtcp-rsize\r\na=rtpmap:96 VP8/90000\r\na=rtcp-fb:96 ccm fir\r\na=rtcp-fb:96 nack\r\na=rtcp-fb:96
            // nack pli\r\na=rtcp-fb:96 goog-remb\r\na=rtcp-fb:96 transport-cc\r\na=rtpmap:98 VP9/90000\r\na=rtcp-fb:98 ccm fir\r\na=rtcp-fb:98 nack\r\
            // na=rtcp-fb:98 nack pli\r\na=rtcp-fb:98 goog-remb\r\na=rtcp-fb:98 transport-cc\r\na=rtpmap:100 red/90000\r\na=rtpmap:127 ulpfec/90000\r\n
            // a=rtpmap:97 rtx/90000\r\na=fmtp:97 apt=96\r\na=rtpmap:99 rtx/90000\r\na=fmtp:99 apt=98\r\na=rtpmap:101 rtx/90000\r\na=fmtp:101 apt=100\r\na=ssrc-group:FID 660055680 3903518476\r\na=ssrc:660055680 cname:2mp6gPjZLb1RwDQT\r\na=ssrc:660055680 msid:T7qHCpV2Wt7W2rU1AqiBV5N1ihW794quAWAm 99c0bb20-b45e-4e10-9fc8-ffdfe6cf758a\r\na=ssrc:660055680 mslabel:T7qHCpV2Wt7W2rU1AqiBV5N1ihW794quAWAm\r\na=ssrc:660055680 label:99c0bb20-b45e-4e10-9fc8-ffdfe6cf758a\r\na=ssrc:3903518476 cname:2mp6gPjZLb1RwDQT\r\na=ssrc:3903518476 msid:T7qHCpV2Wt7W2rU1AqiBV5N1ihW794quAWAm 99c0bb20-b45e-4e10-9fc8-ffdfe6cf758a\r\na=ssrc:3903518476 mslabel:T7qHCpV2Wt7W2rU1AqiBV5N1ihW794quAWAm\r\na=ssrc:3903518476 label:99c0bb20-b45e-4e10-9fc8-ffdfe6cf758a\r\n"}}


            try {
                JSONObject jsonRemoteSdp = new JSONObject(remoteSdp);
                JSONObject jsonSdp = jsonRemoteSdp.getJSONObject("remoteSdp");

                peerConnection.setRemoteDescription(new SdpObserver() {
                    @Override
                    public void onCreateSuccess(SessionDescription sessionDescription) {
                        Log.d(TAG, "setRemoteDescription / onCreateSuccess -> " + sessionDescription);
                    }

                    @Override
                    public void onSetSuccess() {
                        Log.d(TAG, "setRemoteDescription / onSetSuccess");
                    }

                    @Override
                    public void onCreateFailure(String s) {
                        Log.d(TAG, "setRemoteDescription / onCreateFailure ->" + s);
                    }

                    @Override
                    public void onSetFailure(String s) {
                        Log.d(TAG, "setRemoteDescription / onSetFailure -> " + s);
                    }
                }, new SessionDescription(SessionDescription.Type.ANSWER, jsonSdp.getString("sdp")));

            } catch (JSONException e) {
                e.printStackTrace();
            }


        }

        @Override
        public void onAnswerIce(String remoteIce) {
            Log.d(TAG, "onAnswerIce -> " + remoteIce);

            // {"ice":{"candidate":"candidate:463174799 1 udp 2122129151 192.168.180.1 54506 typ host generation 0 ufrag lu6n network-id
            // 2 network-cost 10","sdpMid":"video","sdpMLineIndex":0}}

            try {
                JSONObject jsonIce = new JSONObject(remoteIce);

                peerConnection.addIceCandidate(new IceCandidate(
                        jsonIce.getString("sdpMid"), jsonIce.getInt("sdpMLineIndex"), jsonIce.getString("candidate")));

            } catch (JSONException e) {
                e.printStackTrace();
            }


        }
    }

    PeerConnection peerConnection;
    PCObserver pcObserver = new PCObserver();
    SurfaceViewRenderer localView = null;
    SurfaceViewRenderer remoteView = null;
    VideoCapturer videoCapturer;
    EglBase eglBase;
    Executor executor = Executors.newSingleThreadScheduledExecutor();

    boolean isOffer = false;
    String token = "";

    SocketIoClient socketIo;
    SignalEventHandler signalEventHandler = new SignalEventHandler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

//        requestWindowFeature(Window.FEATURE_NO_TITLE);
//        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
//                | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
//                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
//        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
//                | View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);


        setContentView(R.layout.activity_main);


        Button buttonCreate = (Button)findViewById(R.id.button_create);
        buttonCreate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isOffer = true;
                socketIo.send("create", "");
            }
        });

        Button buttonJoin = (Button)findViewById(R.id.button_join);
        buttonJoin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isOffer = false;
                socketIo.send("join", "");
            }
        });


        socketIo = new SocketIoClient(signalEventHandler);
        //socketIo.connect("http://13.124.155.2:8888/");
        //socketIo.connect("http://192.168.0.4:8888/");
        //socketIo.connect("https://192.168.0.4:443/");
        socketIo.connect("https://52.79.44.127:443/");


        PeerConnectionFactory.initializeAndroidGlobals(this, true, true, true);

        PeerConnectionFactory factory = new PeerConnectionFactory(new PeerConnectionFactory.Options());

        List<PeerConnection.IceServer> iceServers = new ArrayList<PeerConnection.IceServer>();
        iceServers.add(new PeerConnection.IceServer("stun:stun.l.google.com:19302"));

        MediaConstraints pcConst = new MediaConstraints();
        pcConst.optional.add(new MediaConstraints.KeyValuePair("DtlsSrtpKeyAgreement", "true"));

        peerConnection = factory.createPeerConnection(iceServers, pcConst, pcObserver);
        logAndToast("factory.createPeerConnection");


        localView = (SurfaceViewRenderer)findViewById(R.id.local_video_view);
        remoteView = (SurfaceViewRenderer)findViewById(R.id.remote_video_view);

        eglBase = EglBase.create();
        factory.setVideoHwAccelerationOptions(eglBase.getEglBaseContext(), eglBase.getEglBaseContext());

        localView.init(eglBase.getEglBaseContext(), null);
        remoteView.init(eglBase.getEglBaseContext(), null);

        localView.setEnableHardwareScaler(true);
        remoteView.setEnableHardwareScaler(true);

        localView.setZOrderMediaOverlay(true);
        localView.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL);
        localView.setBottom(720);

        remoteView.setZOrderMediaOverlay(true);
        remoteView.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL);


        Camera2Enumerator camEnum = new Camera2Enumerator(this);
        videoCapturer = createCameraCapturer(camEnum);


        VideoSource videoSource = factory.createVideoSource(videoCapturer);



        VideoTrack videoTrack = factory.createVideoTrack("ARDAMSv0", videoSource);

        videoTrack.addRenderer(new VideoRenderer(localView));



        localView.setMirror(true);
        localView.requestLayout();

        remoteView.setMirror(false);
        remoteView.requestLayout();



        MediaStream localMediaStream = factory.createLocalMediaStream("ARDAMS");

        localMediaStream.addTrack(videoTrack);
        peerConnection.addStream(localMediaStream);

        videoTrack.setEnabled(true);
        videoCapturer.startCapture(720, 480, 20);




    }

    private VideoCapturer createCameraCapturer(CameraEnumerator enumerator) {
        final String[] deviceNames = enumerator.getDeviceNames();

        // First, try to find front facing camera
        logAndToast("Looking for front facing cameras.");
        for (String deviceName : deviceNames) {
            if (enumerator.isFrontFacing(deviceName)) {
                logAndToast("Creating front facing camera capturer. " + deviceName);
                VideoCapturer videoCapturer = enumerator.createCapturer(deviceName, null);

                if (videoCapturer != null) {
                    return videoCapturer;
                }
            }
        }

        // Front facing camera not found, try something else
        logAndToast("Looking for other cameras.");
        for (String deviceName : deviceNames) {
            if (!enumerator.isFrontFacing(deviceName)) {
                logAndToast("Creating other camera capturer.");
                VideoCapturer videoCapturer = enumerator.createCapturer(deviceName, null);

                if (videoCapturer != null) {
                    return videoCapturer;
                }
            }
        }

        return null;
    }

    @Override
    protected void onStart() {
        logAndToast("onStart");
        // The activity is about to become visible.
        super.onStart();
    }

    @Override
    protected void onResume() {
        logAndToast("onResume");
        // The activity has become visible (it is now "resumed").
        super.onResume();
    }

    @Override
    protected void onPause() {
        logAndToast("onPause");
        // Another activity is taking focus (this activity is about to be "paused").
        super.onPause();
    }

    @Override
    protected void onStop() {
        logAndToast("onStop");
        // The activity is no longer visible (it is now "stopped")
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        logAndToast("onDestroy");
        // The activity is about to be destroyed.
        super.onDestroy();
    }

    private void logAndToast(String msg) {
        Log.d(TAG, msg);
        if (logToast != null) {
            logToast.cancel();
        }
        logToast = Toast.makeText(this, msg, Toast.LENGTH_SHORT);
        logToast.show();
    }
}
