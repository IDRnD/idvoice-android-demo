package com.idrnd.idvoice.utils.verification;

import net.idrnd.android.media.AssetsExtractor;

import java.io.File;

class TextDependentVerifyEngine extends VoiceVerifyEngine {
    public TextDependentVerifyEngine(File dataDir) {
        super(dataDir, AssetsExtractor.VERIFY_INIT_DATA_TD_SUBPATH);
    }
}
