package com.idrnd.idvoice.utils.logs;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.util.Log;

import com.idrnd.idvoice.models.AudioRecord;
import com.idrnd.idvoice.utils.runners.SingleTaskRunner;
import com.opencsv.CSVWriter;

import net.lingala.zip4j.ZipFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import static com.idrnd.idvoice.IDVoiceApplication.singleTaskRunner;

/**
 * Singleton helper used to file operations
 */
public class FileUtils {

    private final static String NAME_CSV_LOGS = "logs.csv";
    private final static String NAME_ZIP_LOGS = "logs.zip";
    private final String TAG = FileUtils.class.getSimpleName();
    private File baseDir;
    private static FileUtils instance;

    private FileUtils() { }

    public static FileUtils getInstance() {
        if (instance == null) {
            instance = new FileUtils();
        }
        return instance;
    }

    public void init(Context context) {
        String USER_DATA_DIR = "userdata";
        this.baseDir = context.getExternalFilesDir(USER_DATA_DIR);
        if (!baseDir.exists()) {
            baseDir.mkdirs();
        }
    }

    /**
     * Make name audio file from current time
     * @return name audio file
     */
    public static String makeNameAudioFile() {
        return new SimpleDateFormat("yyyy-MM-dd-'T'-HH24:mm:ss", Locale.US).format(new Date()) + ".wav";
    }

    /**
     * Save audio record with log to the filesystem
     * @param audioRecord audio record to save
     * @param verificationProbability verification probability from 0 to 1
     * @param verificationScore raw verification score
     * @param livenessScore liveness score from 0 to 1
     */
    public void saveAudioRecordWithLog(AudioRecord audioRecord, float verificationProbability, float verificationScore, float livenessScore) {
        singleTaskRunner.execute(() -> {
            String nameAudioFile = FileUtils.makeNameAudioFile();
            try {
                saveAudioRecord(nameAudioFile, audioRecord);
            } catch (IOException e) {
                Log.e(TAG, "Something wrong when save audio record", e);
            }
            try {
                saveLog(new StatisticLog(nameAudioFile, verificationProbability, verificationScore, livenessScore));
            } catch (IOException e) {
                Log.e(TAG, "Something wrong when save log", e);
            }
        });
    }

    public void saveAudioRecordWithLog(AudioRecord audioRecord) {
        saveAudioRecordWithLog(audioRecord, -1f, -1f, -1f);
    }

    /**
     * Zip logs and execute callback with result
     * @param callback for work with zipped logs in main thread
     */
    public void zipLogs(SingleTaskRunner.Callback<File> callback) {
        singleTaskRunner.execute(() -> {
            getZipFileLogs().delete();

            ZipFile zipFile = new ZipFile(getZipFileLogs());

            zipFile.addFolder(baseDir);

            return getZipFileLogs();
        }, callback);
    }

    /**
     * Send a file by email/send message application
     * @param file for send
     * @param subject of message
     * @throws PackageManager.NameNotFoundException
     */
    public void sendFileByEmail(Activity activity, Uri file, String subject) throws PackageManager.NameNotFoundException {
        Intent emailIntent = new Intent(Intent.ACTION_SEND);
        emailIntent.setType("vnd.android.cursor.item/email");
        emailIntent.putExtra(Intent.EXTRA_STREAM, file);
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, subject);
        Intent chooserIntent = Intent.createChooser(emailIntent, "Select the application to send logs");

        List<ResolveInfo> resInfoList = activity.getPackageManager()
            .queryIntentActivities(chooserIntent, PackageManager.MATCH_DEFAULT_ONLY);

        for(ResolveInfo resolveInfo : resInfoList) {
            String packageName = resolveInfo.activityInfo.packageName;
            activity.grantUriPermission(
                packageName,
                file,
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION
            );
        }

        if (emailIntent.resolveActivity(activity.getPackageManager()) != null) {
            activity.startActivity(chooserIntent);
        } else {
            throw new PackageManager.NameNotFoundException("Application used to send files not found");
        }
    }


    /**
     * Save audio record to the filesystem
     * @param nameAudioFile audio file name
     * @param audioRecord audio record for save
     * @throws IOException
     */
    private void saveAudioRecord(String nameAudioFile, AudioRecord audioRecord) throws IOException {
        int LENGTH_HEADER = 44;
        byte[] header = new byte[LENGTH_HEADER];
        int audioLen = audioRecord.samples.length;
        int dataLen = audioLen + 36;
        long recordByteRate = (2 * audioRecord.sampleRate);
        File fileAudioRecord = getFileFromBaseDir(nameAudioFile);

        if(!fileAudioRecord.exists()) {
            Log.d(TAG, "File with path: ${fileAudioRecord.absolutePath} don't exist.");
            try {
                if(fileAudioRecord.createNewFile()) {
                    Log.d(TAG, "File with path: ${fileAudioRecord.absolutePath} success create.");
                } else {
                    Log.d(TAG, "File with path: ${fileAudioRecord.absolutePath} don't create.");
                }
            } catch(Exception exc) {
                Log.e(TAG, "When file with path: ${fileAudioRecord.absolutePath} was creating appeared exception:", exc);
                throw exc;
            }
        }

        FileOutputStream os = new FileOutputStream(fileAudioRecord);

        header[0] = 'R';
        header[1] = 'I';
        header[2] = 'F';
        header[3] = 'F';
        header[4] = (byte)(dataLen & 0xFF);
        header[5] = (byte)(dataLen >> 8 & 0xFF);
        header[6] = (byte)(dataLen >> 16 & 0xFF);
        header[7] = (byte)(dataLen >> 24 & 0xFF);
        header[8] = 'W';
        header[9] = 'A';
        header[10] = 'V';
        header[11] = 'E';
        header[12] = 'f';
        header[13] = 'm';
        header[14] = 't';
        header[15] = ' ';
        header[16] = 16;
        header[17] = 0;
        header[18] = 0;
        header[19] = 0;
        header[20] = 1;
        header[21] = 0;
        header[22] = (1 & 0xFF);
        header[23] = (1 >> 8 & 0xFF);
        header[24] = (byte)(audioRecord.sampleRate & 0xFF);
        header[25] = (byte)(audioRecord.sampleRate >> 8 & 0xFF);
        header[26] = (byte)(audioRecord.sampleRate >> 16 & 0xFF);
        header[27] = (byte)(audioRecord.sampleRate >> 24 & 0xFF);
        header[28] = (byte)(recordByteRate & 0xFF);
        header[29] = (byte)(recordByteRate >> 8 & 0xFF);
        header[30] = (byte)(recordByteRate >> 16 & 0xFF);
        header[31] = (byte)(recordByteRate >> 24 & 0xFF);
        header[32] =  2;
        header[33] =  0;
        header[34] =  16;
        header[35] =  0;
        header[36] = 'd';
        header[37] = 'a';
        header[38] = 't';
        header[39] = 'a';
        header[40] = (byte)(audioLen & 0xFF);
        header[41] = (byte)(audioLen >> 8 & 0xFF);
        header[42] = (byte)(audioLen >> 16 & 0xFF);
        header[43] = (byte)(audioLen >> 24 & 0xFF);

        os.write(header, 0, LENGTH_HEADER);
        os.write(audioRecord.samples);
        os.close();
    }

    /**
     * Add log to saved logs in file system
     * @param log log with information about some audio record
     * @throws IOException
     */
    private void saveLog(StatisticLog log) throws IOException {
        File logs = getFileFromBaseDir(NAME_CSV_LOGS);
        final CSVWriter writer;
        if(!logs.exists()) {
            logs.createNewFile();
            writer = new CSVWriter(new FileWriter(logs, true));
            writer.writeNext(StatisticLog.getHeaderCsv());
        } else
            writer = new CSVWriter(new FileWriter(logs, true));

        String[] data = log.getCsvData();

        writer.writeNext(data);
        writer.close();
    }

    private File getFileFromBaseDir(String nameFile) { return new File(baseDir, nameFile); }

    private File getZipFileLogs() { return new File(baseDir, NAME_ZIP_LOGS); }
}