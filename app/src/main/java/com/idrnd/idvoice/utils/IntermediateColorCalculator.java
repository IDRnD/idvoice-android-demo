package com.idrnd.idvoice.utils;

import androidx.core.graphics.ColorUtils;

/**
 * Class for calculating intermediate color by ratio between 0 to 1 from two colors
 * Example: first color is red, second color is blue.
 * User passes percent 0 - get red color
 * User passes percent 0.5 - get green color (average between red and green)
 * User passes percent 1 - get blue color
 */
public class IntermediateColorCalculator {

    private float lowThreshold;
    private float coefficientRecalculation;

    private int colorNegativeResult;
    private int colorPositiveResult;

    /**
     * Constructor
     * @param threshold from 0 to 1 ratio of colors to be mixed.
     * @param colorNegativeResult color for threshold equals to 0
     * @param colorPositiveResult color for threshold equals to 1
     */
    public IntermediateColorCalculator(float threshold, int colorNegativeResult, int colorPositiveResult) {
        this.colorNegativeResult = colorNegativeResult;
        this.colorPositiveResult = colorPositiveResult;

        float range = 1.0f - threshold;

        lowThreshold = Math.max(threshold - range, 0f);

        if (lowThreshold != 0f) {
            coefficientRecalculation = 1f / (1f - lowThreshold);
        } else {
            coefficientRecalculation = 1f / threshold;
        }
    }

    /**
     * Returns intermediate color
     * @param progress from 0 to 1 ratio for colors mixing
     * @return intermediate color
     */
    public int calculateIntermediateColor(float progress) {
        float ratioColor;

        if (progress <= lowThreshold) {
            ratioColor = 0f;
        } else {
            ratioColor = Math.min(((progress - lowThreshold) * coefficientRecalculation), 1f);
        }

        return ColorUtils.blendARGB(
            colorNegativeResult,
            colorPositiveResult,
            ratioColor);
    }
}