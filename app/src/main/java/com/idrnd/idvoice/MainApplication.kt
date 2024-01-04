package com.idrnd.idvoice

import android.app.Application
import android.content.Context
import com.idrnd.idvoice.preferences.GlobalPrefs
import com.idrnd.idvoice.utils.TemplateFileCreator
import com.idrnd.idvoice.utils.license.IdrndLicense
import com.idrnd.idvoice.utils.license.LicenseStatus
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import net.idrnd.voicesdk.android.media.AssetsExtractor
import net.idrnd.voicesdk.liveness.LivenessEngine
import net.idrnd.voicesdk.media.QualityCheckEngine
import net.idrnd.voicesdk.verify.VoiceTemplateFactory
import net.idrnd.voicesdk.verify.VoiceTemplateMatcher
import java.io.File

class MainApplication : Application() {

    lateinit var templateFileCreator: TemplateFileCreator
        private set

    /*
    Liveness and verify VoiceSDK engines are placed here because they spend a lot of time for initialization.
    Necessary to init them as soon as possible.
     */
    private lateinit var deferredLivenessEngine: Deferred<LivenessEngine>

    val livenessEngine: LivenessEngine by lazy {
        runBlocking { deferredLivenessEngine.await() }
    }

    private lateinit var deferredVoiceTemplateFactory: Deferred<VoiceTemplateFactory>

    val voiceTemplateFactory: VoiceTemplateFactory by lazy {
        runBlocking { deferredVoiceTemplateFactory.await() }
    }

    private lateinit var deferredVoiceTemplateMatcher: Deferred<VoiceTemplateMatcher>

    private lateinit var deferredQualityCheckEngine: Deferred<QualityCheckEngine>

    val qualityCheckEngine: QualityCheckEngine by lazy {
        runBlocking { deferredQualityCheckEngine.await() }
    }

    val voiceTemplateMatcher: VoiceTemplateMatcher by lazy {
        runBlocking { deferredVoiceTemplateMatcher.await() }
    }

    lateinit var voiceSdkLicense: IdrndLicense
        private set

    override fun onCreate() {
        super.onCreate()

        // Set license (the contents of VoiceSDK <release bundle>/license/license-*.txt file)
        voiceSdkLicense = IdrndLicense("PUT_HERE_CONTENT_OF_LICENSE_FILE")

        if (voiceSdkLicense.licenseStatus != LicenseStatus.Valid) {
            // We prevent user to use the application if the license is not valid,
            // so we don't need to initialize anything here.
            return
        }

        val sharedPreferencesName = "my_preferences"
        val sharedPreferences = getSharedPreferences(sharedPreferencesName, Context.MODE_PRIVATE)

        GlobalPrefs.init(sharedPreferences)

        val logFolder = File(filesDir, "userdata")
        if (!logFolder.exists()) logFolder.mkdir()

        templateFileCreator = TemplateFileCreator(logFolder)

        // Extract init data for liveness and verify engines
        val initDataFolder = AssetsExtractor(this).extractAssets()

        // Make init data for TD and TI biometrics modes
        val voiceInitData = File(initDataFolder, AssetsExtractor.VERIFY_INIT_DATA_MIC_V1_SUBPATH).absolutePath

        // Init verify engines
        deferredVoiceTemplateFactory = GlobalScope.async(Dispatchers.Default) {
            VoiceTemplateFactory(voiceInitData)
        }

        deferredVoiceTemplateMatcher = GlobalScope.async(Dispatchers.Default) {
            VoiceTemplateMatcher(voiceInitData)
        }

        deferredQualityCheckEngine = GlobalScope.async(Dispatchers.Default) {
            QualityCheckEngine(File(initDataFolder, AssetsExtractor.QUALITY_CHECK_WITH_MULT_SPEAKERS_DETECTOR_INIT_DATA_SUBPATH).absolutePath)
        }

        // Init a liveness engine
        deferredLivenessEngine = GlobalScope.async(Dispatchers.Default) {
            LivenessEngine(File(initDataFolder, AssetsExtractor.LIVENESS_INIT_DATA_SUBPATH).absolutePath)
        }
    }

    override fun onTerminate() {
        super.onTerminate()
        // onTerminate() will never be called never on production Android devices.
        // We have added this code to show you that every VoiceSDK class should be closed when it's no longer needed.
        livenessEngine.close()
        voiceTemplateFactory.close()
        voiceTemplateMatcher.close()
        qualityCheckEngine.close()
    }
}
