package com.idrnd.idvoice.utils.verification;

import java.io.File;

import static net.idrnd.android.media.AssetsExtractor.VERIFY_INIT_DATA_TD_ACCURATE_SUBPATH;
import static net.idrnd.android.media.AssetsExtractor.VERIFY_INIT_DATA_TD_MEDIUM_SUBPATH;

class TextDependentVerifyEngine extends VoiceVerifyEngine {

    /**
     * TD verify engine
     * @param dataDir directory where store init data.
     * @param accuracy verification accuracy. Than it's lower than engine works faster.
     */
    public TextDependentVerifyEngine(File dataDir, Accuracy accuracy) {
        super(dataDir, accuracy);
    }

    @Override
    String accuracyToInitDataSubPath(Accuracy accuracy) {
        switch (accuracy) {
            case HIGH:
                return VERIFY_INIT_DATA_TD_ACCURATE_SUBPATH;
            case MEDIUM:
            case LOW:
                return VERIFY_INIT_DATA_TD_MEDIUM_SUBPATH;
            default:
                throw new IllegalStateException("Can't convert " + accuracy + " to sub path!");
        }
    }
}