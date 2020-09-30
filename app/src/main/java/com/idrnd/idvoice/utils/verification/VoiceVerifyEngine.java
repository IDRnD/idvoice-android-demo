package com.idrnd.idvoice.utils.verification;

import net.idrnd.voicesdk.core.common.VoiceTemplate;
import net.idrnd.voicesdk.verify.VerifyResult;
import net.idrnd.voicesdk.verify.VoiceTemplateFactory;
import net.idrnd.voicesdk.verify.VoiceTemplateMatcher;
import net.idrnd.voicesdk.verify.VoiceVerifyStream;

import java.io.File;

/**
 * Class wrapper for [VoiceTemplateFactory] and [VoiceTemplateMatcher] from VoiceSDK
 */
public class VoiceVerifyEngine {

    private VoiceTemplateFactory voiceTemplateFactory;
    private VoiceTemplateMatcher voiceTemplateMatcher;

    public VoiceVerifyEngine(File dataDir, String initDataSubPath) {
        String initDataPath = new File(dataDir, initDataSubPath).getAbsolutePath();
        voiceTemplateFactory = new VoiceTemplateFactory(initDataPath);
        voiceTemplateMatcher = new VoiceTemplateMatcher(initDataPath);
    }

    public VoiceTemplate createVoiceTemplate(byte[] byteArray, int sampleRate) {
        return voiceTemplateFactory.createVoiceTemplate(byteArray, sampleRate);
    }

    VerifyResult matchVoiceTemplates(VoiceTemplate firstVoiceTemplate, VoiceTemplate secondVoiceTemplate) {
        return voiceTemplateMatcher.matchVoiceTemplates(
                firstVoiceTemplate,
                secondVoiceTemplate
        );
    }


    public VoiceTemplate mergeVoiceTemplates(VoiceTemplate[] arrayVoiceTemplates) {
        return voiceTemplateFactory.mergeVoiceTemplates(arrayVoiceTemplates);
    }

    VoiceVerifyStream getVoiceVerifyStream(VoiceTemplate enrolledVoiceTemplate, int sampleRate) {
        return new VoiceVerifyStream(
                voiceTemplateFactory,
                voiceTemplateMatcher,
                enrolledVoiceTemplate,
                sampleRate
        );
    }

    public VoiceVerifyStream getVoiceVerifyStream(VoiceTemplate enrolledVoiceTemplate, int sampleRate, int windowLength) {
        return new VoiceVerifyStream(
                voiceTemplateFactory,
                voiceTemplateMatcher,
                enrolledVoiceTemplate,
                sampleRate,
                windowLength
        );
    }
}
