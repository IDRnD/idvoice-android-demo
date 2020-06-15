package com.idrnd.idvoice.utils.verification;

import android.provider.MediaStore;

import com.idrnd.idvoice.models.AudioRecord;

import net.idrnd.voicesdk.core.common.VoiceTemplate;
import net.idrnd.voicesdk.verify.QualityShortDescription;
import net.idrnd.voicesdk.verify.VerifyResult;
import net.idrnd.voicesdk.verify.VoiceTemplateFactory;
import net.idrnd.voicesdk.verify.VoiceTemplateMatcher;
import net.idrnd.voicesdk.verify.VoiceVerifyStream;

import java.io.File;
import static com.idrnd.idvoice.utils.verification.VoiceVerifyEngine.Accuracy.MEDIUM;

/**
 * Class wrapper for [VoiceTemplateFactory] and [VoiceTemplateMatcher] from VoiceSDK
 */
public abstract class VoiceVerifyEngine {

    public enum Accuracy {
        HIGH,
        MEDIUM,
        LOW
    }

    /**
     * Verification accuracy. Than it's lower than engine works faster.
     */
    private final Accuracy accuracy;
    private final VoiceTemplateFactory voiceTemplateFactory;
    private final VoiceTemplateMatcher voiceTemplateMatcher;

    public VoiceVerifyEngine(File dataDir, Accuracy accuracy) {
        this.accuracy = accuracy;
        String initDataSubPath = accuracyToInitDataSubPath(accuracy);
        String initDataPath = new File(dataDir, initDataSubPath).getAbsolutePath();
        voiceTemplateFactory = new VoiceTemplateFactory(initDataPath);
        voiceTemplateMatcher = new VoiceTemplateMatcher(initDataPath);
    }

    public VoiceTemplate createVoiceTemplate(byte[] byteArray, int sampleRate) {
        return voiceTemplateFactory.createVoiceTemplate(byteArray, sampleRate);
    }

    public VoiceTemplate createVoiceTemplate(AudioRecord audioRecord) {
        return createVoiceTemplate(audioRecord.samples, audioRecord.sampleRate);
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

    public QualityShortDescription checkQuality(AudioRecord audioRecord) {
        return checkQuality(audioRecord.samples, audioRecord.sampleRate);
    }

    public QualityShortDescription checkQuality(byte[] data, int sampleRate) {
        return voiceTemplateFactory.checkQuality(data, sampleRate).getQualityShortDescription();
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

    /**
     * Verification accuracy. Than it's lower than engine works faster.
     */
    public Accuracy getAccuracy() {
        return accuracy;
    }

    abstract String accuracyToInitDataSubPath(Accuracy accuracy);
}