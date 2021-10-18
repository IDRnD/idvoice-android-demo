package com.idrnd.idvoice.viewModels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.idrnd.idvoice.model.BiometricsType
import com.idrnd.idvoice.model.BiometricsType.*
import com.idrnd.idvoice.model.GlobalPrefs
import com.idrnd.idvoice.utils.TemplateFileCreator
import kotlinx.coroutines.*
import net.idrnd.android.media.AssetsExtractor
import net.idrnd.android.media.AssetsExtractor.*
import net.idrnd.voicesdk.antispoof2.AntispoofEngine
import net.idrnd.voicesdk.media.SpeechSummaryEngine
import net.idrnd.voicesdk.verify.VoiceTemplateFactory
import net.idrnd.voicesdk.verify.VoiceTemplateMatcher
import java.io.File

class SharedViewModel : ViewModel() {

    /**
     * Indicates whether this view model is initialized or not.
     */
    var isInitialized = false
        private set

    /**
     * Listeners for permissions request.
     */
    var onEnrollPermissionsAreGranted: ((Boolean) -> Unit)? = null
    var onVerifyPermissionsAreGranted: ((Boolean) -> Unit)? = null

    /**
     * Speech summary engine for speech analysis.
     */
    lateinit var speechSummaryEngine: SpeechSummaryEngine
        private set

    /**
     * Voice template file creator.
     */
    lateinit var templateFileCreator: TemplateFileCreator
        private set

    /*
    Antispoof and verify VoiceSDK engines are placed here because they spend a lot of time for initialization.
    Necessary to init them as soon as possible. Activity or application are convenient places for this therefore
    engines are placed in SharedViewModel of MainActivity.
    */

    private lateinit var deferredAntispoofEngine: Deferred<AntispoofEngine>
    /**
     * Antispoof engine.
     */
    val antispoofEngine: AntispoofEngine by lazy {
        runBlocking { deferredAntispoofEngine.await() }
    }

    private lateinit var deferredVoiceTemplateFactories: Deferred<Map<BiometricsType, VoiceTemplateFactory>>
    private val voiceTemplateFactories: Map<BiometricsType, VoiceTemplateFactory> by lazy {
        runBlocking { deferredVoiceTemplateFactories.await() }
    }
    /**
     * Returns a voice template factory by a current biometrics type.
     */
    val voiceTemplateFactory: VoiceTemplateFactory
        get() {
            return voiceTemplateFactories[GlobalPrefs.biometricsType]!!
        }

    private lateinit var deferredVoiceTemplateMatcher: Deferred<Map<BiometricsType, VoiceTemplateMatcher>>
    private val voiceTemplateMatchers: Map<BiometricsType, VoiceTemplateMatcher> by lazy {
        runBlocking { deferredVoiceTemplateMatcher.await() }
    }
    /**
     * Returns a voice template matcher by a current biometrics type.
     */
    val voiceTemplateMatcher: VoiceTemplateMatcher
        get() {
            return voiceTemplateMatchers[GlobalPrefs.biometricsType]!!
        }

    fun init(context: Context) {

        // Init shared preferences
        val sharedPreferencesName = "my_preferences"
        val sharedPreferences = context.getSharedPreferences(sharedPreferencesName, Context.MODE_PRIVATE)

        // Init singletons
        GlobalPrefs.init(sharedPreferences)

        // Init file creator
        val logFolder = File(context.filesDir, "userdata")
        if (!logFolder.exists()) { logFolder.mkdir() }

        templateFileCreator = TemplateFileCreator(logFolder)

        // Extract init data for antispoof and verify engines
        val initDataFolder = AssetsExtractor(context).extractAssets()

        // Make init data for TD and TI biometrics modes
        val textDependentInitData = File(initDataFolder, VERIFY_INIT_DATA_TD_ACCURATE_SUBPATH).absolutePath
        val textIndependentInitData = File(initDataFolder, VERIFY_INIT_DATA_TI_SUBPATH).absolutePath

        // Init verify engines
        deferredVoiceTemplateFactories = viewModelScope.async(Dispatchers.Default) {
            mapOf(
                TextDependent to VoiceTemplateFactory(textDependentInitData),
                TextIndependent to VoiceTemplateFactory(textIndependentInitData),
            )
        }

        deferredVoiceTemplateMatcher = viewModelScope.async(Dispatchers.Default) {
            mapOf(
                TextDependent to VoiceTemplateMatcher(textDependentInitData),
                TextIndependent to VoiceTemplateMatcher(textIndependentInitData),
            )
        }

        // Init a liveness engine
        deferredAntispoofEngine = viewModelScope.async(Dispatchers.Default) {
            AntispoofEngine(File(initDataFolder, ANTISPOOF_INIT_DATA_DEFAULT_SUBPATH).absolutePath)
        }

        // Init speech summary engine
        speechSummaryEngine = SpeechSummaryEngine(File(initDataFolder, SPEECH_SUMMARY_INIT_DATA_SUBPATH).absolutePath)

        // Set that view model is initialized
        isInitialized = true
    }

    override fun onCleared() {
        super.onCleared()
        // Clear resources
        antispoofEngine.close()
        speechSummaryEngine.close()
        voiceTemplateFactories.entries.forEach { it.value.close() }
        voiceTemplateMatchers.entries.forEach { it.value.close() }
    }
}
