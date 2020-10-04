package software.engineering2.mockandroid;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;

import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "Info";
    private NavController navController;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.d(TAG, "MainActivity: In the onCreate() event");

    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    protected void onStart() {
        super.onStart();
        Log.d(TAG, "MainActivity: In the onStart() event");
    }

    protected void onRestart() {
        super.onRestart();
        Log.d(TAG, "MainActivity: In the onRestart() event");
    }

    protected void onResume() {
        super.onResume();
        Log.d(TAG, "MainActivity: In the onResume() event");
        AppManager.getInstance().createTCPConnection();
    }

    protected void onPause() {
        super.onPause();
        Log.d(TAG, "MainActivity: In the onPause() event");
        AppManager.getInstance().closeTCPConnection();
    }

    protected void onStop() {
        super.onStop();
        Log.d(TAG, "MainActivity: In the onStop() event");
    }

    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "MainActivity: In the onDestroy() event");
    }
}