package com.idrnd.idvoice.utils.logs;

/**
 * Log with verification and liveness statistic
 */
public class StatisticLog {

    String audioFilename;
    float verificationProbability;
    float verificationScore;
    float livenessScore;

    public StatisticLog(String audioFilename, float verificationProbability, float verificationScore, float livenessScore) {
        this.audioFilename = audioFilename;
        this.verificationProbability = verificationProbability;
        this.verificationScore = verificationScore;
        this.livenessScore = livenessScore;
    }

    /**
     * Headers for CSV files
     */
    public static String[] getHeaderCsv() {
        return new String[] {
            "Audio file name", "Verification probability",
            "Verification Score", "Liveness Score"
        };
    }

    /**
     * Single log row
     */
    public String[] getCsvData() {
        return new String[] {
            audioFilename, String.valueOf(verificationProbability),
            String.valueOf(verificationScore), String.valueOf(livenessScore)
        };
    }
}