package xiaoyuan.debugger;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

/**
 * 模块应用页（说明页）。控制面板由 hook 叠加在小猿当前 Activity 上（InAppOverlay），
 * 配置读写小猿自身 SharedPreferences，实时生效，无需本页任何开关。
 */
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        TextView status = findViewById(R.id.server_status);
        status.setText(getString(R.string.settings_conn_hint));
    }
}
