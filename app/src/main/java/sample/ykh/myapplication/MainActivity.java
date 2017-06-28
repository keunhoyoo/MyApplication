package sample.ykh.myapplication;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

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

    private class PCObserver implements PeerConnection.Observer {

        @Override
        public void onSignalingChange(PeerConnection.SignalingState signalingState) {
            Log.d(TAG, "onSignalingChange");
        }

        @Override
        public void onIceConnectionChange(PeerConnection.IceConnectionState iceConnectionState) {
            Log.d(TAG, "onIceConnectionChange");
        }

        @Override
        public void onIceConnectionReceivingChange(boolean b) {
            Log.d(TAG, "onIceConnectionReceivingChange");
        }

        @Override
        public void onIceGatheringChange(PeerConnection.IceGatheringState iceGatheringState) {
            Log.d(TAG, "onIceGatheringChange");
        }

        @Override
        public void onIceCandidate(IceCandidate iceCandidate) {
            Log.d(TAG, "onIceCandidate");
        }

        @Override
        public void onIceCandidatesRemoved(IceCandidate[] iceCandidates) {
            Log.d(TAG, "onIceCandidatesRemoved");
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

    private class WebSocketConnectorCallback implements WebSocketConnector.Callback {

        @Override
        public void onWebSocketMessage(String message) {
            Log.d(TAG, "onWebSocketMessage");
        }

        @Override
        public void onWebSocketClose() {
            Log.d(TAG, "onWebSocketClose");
        }

        @Override
        public void onWebSocketError(String desc) {
            Log.d(TAG, "onWebSocketError");
        }
    }

    PeerConnection peerConnection;
    PCObserver pcObserver = new PCObserver();
    SurfaceViewRenderer localView = null;
    SurfaceViewRenderer remoteView = null;
    VideoCapturer videoCapturer;
    EglBase eglBase;
    Executor executor = Executors.newSingleThreadScheduledExecutor();

    WebSocketConnectorCallback wsCallback = new WebSocketConnectorCallback();
    WebSocketConnector wsConnector = new WebSocketConnector(wsCallback);

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

        PeerConnectionFactory.initializeAndroidGlobals(this, true, true, true);

        PeerConnectionFactory factory = new PeerConnectionFactory(new PeerConnectionFactory.Options());

        List<PeerConnection.IceServer> iceServers = new ArrayList<PeerConnection.IceServer>();
        iceServers.add(new PeerConnection.IceServer("stun:stun.l.google.com:19302"));

        MediaConstraints pcConst = new MediaConstraints();
        pcConst.optional.add(new MediaConstraints.KeyValuePair("DtlsSrtpKeyAgreement", "false"));

        peerConnection = factory.createPeerConnection(iceServers, pcConst, pcObserver);
        Log.d(TAG, "factory.createPeerConnection");


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


        final MediaConstraints sdpConst = new MediaConstraints();
        //sdpConst.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"));
        sdpConst.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"));

        peerConnection.createOffer(new SdpObserver() {
            @Override
            public void onCreateSuccess(SessionDescription sessionDescription) {
                Logging.d(TAG, "onCreateSuccess: " + sessionDescription.description);

                peerConnection.setLocalDescription(new SdpObserver() {
                    @Override
                    public void onCreateSuccess(SessionDescription sessionDescription) {

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
                }, sessionDescription);

            }

            @Override
            public void onSetSuccess() {

            }

            @Override
            public void onCreateFailure(String s) {
                Logging.d(TAG, "onCreateFailure: " + s);
            }

            @Override
            public void onSetFailure(String s) {
                Logging.d(TAG, "onSetFailure: " + s);
            }
        }, sdpConst);



        wsConnector.connect("wss://10.0.2.2:8443");


    }

    private VideoCapturer createCameraCapturer(CameraEnumerator enumerator) {
        final String[] deviceNames = enumerator.getDeviceNames();

        // First, try to find front facing camera
        Logging.d(TAG, "Looking for front facing cameras.");
        for (String deviceName : deviceNames) {
            if (enumerator.isFrontFacing(deviceName)) {
                Logging.d(TAG, "Creating front facing camera capturer. " + deviceName);
                VideoCapturer videoCapturer = enumerator.createCapturer(deviceName, null);

                if (videoCapturer != null) {
                    return videoCapturer;
                }
            }
        }

        // Front facing camera not found, try something else
        Logging.d(TAG, "Looking for other cameras.");
        for (String deviceName : deviceNames) {
            if (!enumerator.isFrontFacing(deviceName)) {
                Logging.d(TAG, "Creating other camera capturer.");
                VideoCapturer videoCapturer = enumerator.createCapturer(deviceName, null);

                if (videoCapturer != null) {
                    return videoCapturer;
                }
            }
        }

        return null;
    }

}
