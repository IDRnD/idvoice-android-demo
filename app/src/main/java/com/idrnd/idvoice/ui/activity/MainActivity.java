package com.idrnd.idvoice.ui.activity;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.idrnd.idvoice.BuildConfig;
import com.idrnd.idvoice.R;
import com.idrnd.idvoice.ui.fragments.StartFragment;
import com.idrnd.idvoice.utils.Prefs;
import com.idrnd.idvoice.utils.logs.FileUtils;
import com.idrnd.idvoice.utils.verification.EngineManager;

import net.idrnd.voicesdk.verify.QualityShortDescription;

import static com.idrnd.idvoice.IDVoiceApplication.TD_VERIFY_ACCURACY;
import static com.idrnd.idvoice.IDVoiceApplication.TI_VERIFY_ACCURACY;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        adjustFontScale(getApplicationContext());

        // Replace app theme for splash screen to simple app theme. Need call before onCreate
        setTheme(R.style.AppTheme);
        super.onCreate(savedInstanceState);

        getWindow().setStatusBarColor(Color.TRANSPARENT);
        setContentView(R.layout.main_activity);

        EngineManager.getInstance().init(getApplicationContext(), TD_VERIFY_ACCURACY, TI_VERIFY_ACCURACY);
        Prefs.getInstance().init(this, "Prefs", MODE_PRIVATE);
        FileUtils.getInstance().init(getApplicationContext());

        getSupportFragmentManager().beginTransaction()
                .add(R.id.fragmentContainer, new StartFragment())
                .commit();
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

    public static void showToastWithSpeechRecordQualityStatus(Context context, QualityShortDescription status) {
        int messageId = 0;
        switch (status) {
            case TOO_NOISY:
                messageId = R.string.message_speech_record_is_too_noisy;
                break;
            case TOO_LONG_REVERBERATION:
                messageId = R.string.message_speech_record_long_reverberation;
                break;
            case TOO_SMALL_SPEECH_TOTAL_LENGTH:
                messageId = R.string.message_speech_not_enough;
                break;
            default:
                if (BuildConfig.DEBUG) {
                    throw new AssertionError("Impossible the quality value:" + status);
                }
        }
        String message = context.getString(messageId);
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }
}