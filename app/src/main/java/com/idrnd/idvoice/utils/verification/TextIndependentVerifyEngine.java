package com.idrnd.idvoice.utils.verification;

import net.idrnd.android.media.AssetsExtractor;

import java.io.File;

class TextIndependentVerifyEngine extends VoiceVerifyEngine {
    public TextIndependentVerifyEngine(File dataDir) {
        super(dataDir, AssetsExtractor.VERIFY_INIT_DATA_TI_SUBPATH);
    }
}
