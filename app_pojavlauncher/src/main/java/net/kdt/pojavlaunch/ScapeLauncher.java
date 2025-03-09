package net.kdt.pojavlaunch;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.kdt.mcgui.ProgressLayout;

import net.kdt.pojavlaunch.progresskeeper.ProgressKeeper;
import net.kdt.pojavlaunch.services.ProgressServiceKeeper;

public class ScapeLauncher extends BaseActivity {

    private TextView settings;
    private Button playHD;
    private Button playSD;
    private static final int FILE_SELECT_CODE_JSON = 0;
    private static final int FILE_SELECT_CODE_ZIP = 1;
    private ProgressServiceKeeper mProgressServiceKeeper;
    private ProgressLayout mProgressLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dummy_launcher);
        playHD = findViewById(R.id.playHD);
        playSD = findViewById(R.id.playSD);
        mProgressLayout = findViewById(R.id.progress_layout);

        ProgressKeeper.addTaskCountListener((mProgressServiceKeeper = new ProgressServiceKeeper(this)));
        ProgressKeeper.addTaskCountListener(mProgressLayout);

        mProgressLayout.observe(ProgressLayout.UNPACK_RUNTIME);
        mProgressLayout.observe(ProgressLayout.INSTALL_MODPACK);

        playHD.setOnClickListener(view -> {
            // Double launch sd
            if(!runtimeReady()) return;
            Intent intent = new Intent(ScapeLauncher.this, JavaGUILauncherActivity.class);
            startActivity(intent);
        });

        playSD.setOnClickListener(view -> {
            if(!runtimeReady()) return;
            Intent intent = new Intent(ScapeLauncher.this, JavaGUILauncherActivity.class);
            startActivity(intent);
        });
        //ProgressKeeper.addTaskCountListener(this::launchOnFinished); // this should work but it doesn't...
    }
    private void launchOnFinished(int tc){
        if(!mProgressLayout.hasProcesses()){
            Intent intent = new Intent(ScapeLauncher.this, MainActivity.class);
            startActivity(intent);
        }
    }

    private boolean runtimeReady(){
        if(mProgressLayout.hasProcesses()){
            Toast.makeText(this, R.string.tasks_ongoing, Toast.LENGTH_LONG).show();
            return false;
        }
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ProgressKeeper.removeTaskCountListener(mProgressServiceKeeper);
    }
}