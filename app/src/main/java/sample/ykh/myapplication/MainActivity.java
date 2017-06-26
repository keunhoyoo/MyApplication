package sample.ykh.myapplication;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import org.webrtc.PeerConnectionFactory;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        PeerConnectionFactory factory = new PeerConnectionFactory(new PeerConnectionFactory.Options());
    }
}
