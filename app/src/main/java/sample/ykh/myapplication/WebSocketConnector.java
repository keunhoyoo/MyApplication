package sample.ykh.myapplication;

import android.util.Log;

import java.net.URI;
import java.net.URISyntaxException;

import de.tavendo.autobahn.WebSocket;
import de.tavendo.autobahn.WebSocketConnection;
import de.tavendo.autobahn.WebSocketException;

/**
 * Created by ykh on 2017-06-28.
 */

public class WebSocketConnector {
    final static String TAG = "WebSocketConnector";

    public interface Callback {
        void onWebSocketMessage(final String message);
        void onWebSocketClose();
        void onWebSocketError(final String desc);
    }

    WebSocketConnection ws;
    WebSocket.WebSocketConnectionObserver wsObserver;


    public WebSocketConnector(Callback callback) {

    }

    public void connect(String wssUrl) {
        Log.d(TAG, "connecting to " + wssUrl);

        ws = new WebSocketConnection();
        wsObserver = new WebSocket.WebSocketConnectionObserver() {
            @Override
            public void onOpen() {

            }

            @Override
            public void onClose(WebSocketCloseNotification webSocketCloseNotification, String s) {

            }

            @Override
            public void onTextMessage(String s) {

            }

            @Override
            public void onRawTextMessage(byte[] bytes) {

            }

            @Override
            public void onBinaryMessage(byte[] bytes) {

            }
        };

        try {
            ws.connect(new URI(wssUrl), wsObserver);
        } catch (WebSocketException e) {
            e.printStackTrace();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }
}
