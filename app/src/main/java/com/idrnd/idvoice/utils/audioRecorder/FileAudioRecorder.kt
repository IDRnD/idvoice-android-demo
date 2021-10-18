package com.idrnd.idvoice.utils.audioRecorder

import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import kotlin.concurrent.thread

/**
 * Audio recorder that records into file
 *
 */
class FileAudioRecorder(private val micAudioRecorder: MicAudioRecorder) {

    /**
     * Returns true if audio recorder is stopped and false otherwise.
     */
    val isStopped: Boolean
        get() = micAudioRecorder.isStopped()

    /**
     * Returns true if audio recorder is paused and false otherwise.
     */
    val isPaused: Boolean
        get() = micAudioRecorder.isPaused

    val sampleRate
        get() = micAudioRecorder.sampleRate

    val encoding
        get() = micAudioRecorder.encoding

    val bufferSize
        get() = micAudioRecorder.bufferSize

    var outputFile: File? = null
        private set

    private var outputStream: FileOutputStream? = null

    // Coroutine stuff
    private var writerThread: Thread? = null

    /**
     * Start record.
     *
     * @exception IllegalStateException  can't start audio recording cause an unknown reason.
     *
     * @exception FileNotFoundException  if the file exists but is a directory
     *                   rather than a regular file, does not exist but cannot
     *                   be created, or cannot be opened for any other reason
     * @exception SecurityException  if a security manager exists and its
     *               <code>checkWrite</code> method denies write access
     *               to the file.
     * @exception IOException  if an I/O error occurs.
     *
     * @see java.io.File#getPath()
     * @see java.lang.SecurityException
     * @see java.lang.SecurityManager#checkWrite(java.lang.String)
     */
    @Throws(FileNotFoundException::class, SecurityException::class, IOException::class)
    fun startRecord(outputFile: File) {
        // Stop a record
        stopRecord()

        // Set a new output file
        this.outputFile = outputFile

        // Open output stream
        outputStream = FileOutputStream(outputFile)

        // Start audio recording
        micAudioRecorder.startAudioRecording()

        // Start audio writing
        writerThread = thread(true) {
            // * Start to write data in file
            while (!micAudioRecorder.isStopped()) {
                if (micAudioRecorder.hasNext()) {
                    outputStream!!.write(micAudioRecorder.next())
                }
            }
        }
    }

    fun pauseRecord() {
        micAudioRecorder.pauseAudioRecording()
    }

    fun resumeRecord() {
        micAudioRecorder.resumeAudioRecording()
    }

    /**
     * Stop record.
     *
     * @exception IOException  if an I/O error occurs.
     */
    fun stopRecord() {
        micAudioRecorder.stopAudioRecording()
        writerThread?.join()
        writerThread = null
        outputStream?.close()
    }
}
