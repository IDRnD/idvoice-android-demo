package com.idrnd.idvoice.utils.verification;

import java.io.File;
import static net.idrnd.android.media.AssetsExtractor.VERIFY_INIT_DATA_TI_ACCURATE_SUBPATH;
import static net.idrnd.android.media.AssetsExtractor.VERIFY_INIT_DATA_TI_LITE_SUBPATH;
import static net.idrnd.android.media.AssetsExtractor.VERIFY_INIT_DATA_TI_SUBPATH;

class TextIndependentVerifyEngine extends VoiceVerifyEngine {

    /**
     * TI verify engine
     * @param dataDir directory where store init data.
     * @param accuracy verification accuracy. Than it's lower than engine works faster.
     */
    public TextIndependentVerifyEngine(File dataDir, Accuracy accuracy) {
        super(dataDir, accuracy);
    }

    @Override
    String accuracyToInitDataSubPath(Accuracy accuracy) {
        switch (accuracy) {
            case HIGH:
                return VERIFY_INIT_DATA_TI_ACCURATE_SUBPATH;
            case MEDIUM:
                return VERIFY_INIT_DATA_TI_SUBPATH;
            case LOW:
                return VERIFY_INIT_DATA_TI_LITE_SUBPATH;
            default:
                throw new IllegalStateException("Can't convert " + accuracy + " to sub path!");
        }
    }
}