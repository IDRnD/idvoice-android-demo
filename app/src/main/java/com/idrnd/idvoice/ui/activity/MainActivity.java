package com.idrnd.idvoice.ui.activity;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;

import com.idrnd.idvoice.R;
import com.idrnd.idvoice.ui.fragments.StartFragment;
import com.idrnd.idvoice.utils.EngineManager;
import com.idrnd.idvoice.utils.Prefs;
import com.idrnd.idvoice.utils.logs.FileUtils;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        adjustFontScale(getApplicationContext());

        // Replace app theme for splash screen to simple app theme. Need call before onCreate
        setTheme(R.style.AppTheme);
        super.onCreate(savedInstanceState);

        getWindow().setStatusBarColor(getResources().getColor(R.color.whiteBackground));
        setContentView(R.layout.activity_main);

        initSingletons();

        getSupportFragmentManager().beginTransaction()
                .add(R.id.fragmentContainer, new StartFragment())
                .commit();
    }

    private void initSingletons() {
        EngineManager.getInstance().init(getApplicationContext());
        Prefs.getInstance().init(getPreferences(MODE_PRIVATE));
        FileUtils.getInstance().init(getApplicationContext());
    }

    /**
     * If font size is changed in Android Settings, UI may become broken.
     * This function sets strict scale size for all fonts in the app independently from global android Settings
     */
    private void adjustFontScale(Context context) {
        Configuration configuration = context.getResources().getConfiguration();
        configuration.fontScale = 1f;
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        wm.getDefaultDisplay().getMetrics(metrics);
        metrics.scaledDensity = configuration.fontScale * metrics.density;
        context.getResources().updateConfiguration(configuration, metrics);
    }
}
