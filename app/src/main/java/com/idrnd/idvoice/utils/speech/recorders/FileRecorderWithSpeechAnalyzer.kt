package com.idrnd.idvoice.utils.speech.recorders

import com.idrnd.idvoice.utils.audioRecorder.FileAudioRecorder
import com.idrnd.idvoice.utils.speech.analysers.InputSpeechStreamAnalyser
import com.idrnd.idvoice.utils.speech.model.AnalysisResult
import java.io.File
import java.io.InputStream

/**
 * File audio recorder with speech counter and speech endpoint detector.
 *
 * @param audioRecorder for audio recording in file.
 * @param inputSpeechStreamAnalyser for speech analysing.
 */
class FileRecorderWithSpeechAnalyzer(
    private val audioRecorder: FileAudioRecorder,
    private val inputSpeechStreamAnalyser: InputSpeechStreamAnalyser
) : Iterator<AnalysisResult> {

    val outputFile: File?
        get() = audioRecorder.outputFile

    val sampleRate: Int
        get() = audioRecorder.sampleRate

    val bufferSizeForRecord: Int
        get() = audioRecorder.bufferSize

    val bufferSizeForAnalysis: Int
        get() = inputSpeechStreamAnalyser.bufferSize

    /**
     * Returns true if audio recorder is paused and false otherwise.
     */
    val isPaused: Boolean
        get() = audioRecorder.isPaused

    /**
     * Returns true if audio recorder is stopped and false otherwise.
     */
    val isStopped: Boolean
        get() = audioRecorder.isStopped

    private var inputSpeechStream: InputStream? = null

    /**
     * Start record and check that speech is ended.
     * @return pair of speech length in ms and whether speech has ended. After moment of speech has
     * ended a detector start again analysis audio on has speech ended.
     */
    fun startRecordAndSpeechAnalysis(outputFile: File) {

        // Stop record if that has been start
        stopRecordAndSpeechAnalysis()

        audioRecorder.startRecord(outputFile)

        // Get input stream output file
        inputSpeechStream = audioRecorder.outputFile!!.inputStream()

        // Start analysis
        inputSpeechStreamAnalyser.startAnalysis(inputSpeechStream!!)
    }

    fun pauseRecordAndSpeechAnalysis() {
        audioRecorder.pauseRecord()
    }

    fun resumeRecordAndSpeechAnalysis() {
        audioRecorder.resumeRecord()
    }

    fun stopRecordAndSpeechAnalysis() {
        // Stop audio record
        audioRecorder.stopRecord()

        // Stop analysis
        inputSpeechStreamAnalyser.stopAnalysis()

        // Close input stream
        inputSpeechStream?.close()
    }

    override fun hasNext(): Boolean {
        return inputSpeechStreamAnalyser.hasNext()
    }

    override fun next(): AnalysisResult {
        return inputSpeechStreamAnalyser.next()
    }

    companion object {
        private val TAG = FileRecorderWithSpeechAnalyzer::class.simpleName
    }
}
