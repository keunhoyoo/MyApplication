package sample.ykh.myapplication;

import android.util.Log;

import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Socket;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * Created by ykh on 2017-06-29.
 */

public class SocketIoClient {
    private final String TAG = "SocketIoClient";

    private Socket socket;


    public interface EventHandler {
        void onCreatedRoom(final String token, final String msg);
        void onOfferSdp(final String offerSdp);
        void onOfferIce(final String offerIce);
        void onAnswerSdp(final String remoteSdp);
        void onAnswerIce(final String remoteIce);
    }

    private EventHandler eventHandler;

    public SocketIoClient(EventHandler eventHandler) {
        this.eventHandler = eventHandler;
    }

    public void connect(final String url) {
        try {
            socket = IO.socket(new URI(url));

            socket.on("hi", new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    Log.d(TAG, "socket.io - received message");
                }
            });

            socket.on("createdRoom", new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    Log.d(TAG, "socket.io - createdRoom");
                    eventHandler.onCreatedRoom((String)args[0], (String)args[1]);
                }
            });

            socket.on("answerSdp", new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    Log.d(TAG, "socket.io - answerSdp");
                    eventHandler.onAnswerSdp((String)args[0]);
                }
            });

            socket.on("answerIce", new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    Log.d(TAG, "socket.io - answerIce");
                    eventHandler.onAnswerIce((String)args[0]);
                }
            });

            socket.on("offerSdp", new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    Log.d(TAG, "socket.io - offerSdp");
                    eventHandler.onOfferSdp((String)args[0]);
                }
            });

            socket.on("offerIce", new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    Log.d(TAG, "socket.io - offerIce");
                    eventHandler.onOfferIce((String)args[0]);
                }
            });

            socket.connect();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    public void send(String key, String value) {
        socket.emit(key, value);
    }

    @Override
    protected void finalize() throws Throwable {
        if (socket != null) {
            socket.disconnect();
        }
        super.finalize();
    }
}
