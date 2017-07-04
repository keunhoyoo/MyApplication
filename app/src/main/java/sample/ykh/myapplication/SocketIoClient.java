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
