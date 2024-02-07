package com.udacity.catpoint.service;

import java.awt.image.BufferedImage;

public interface ImageService
{
    /**
     * Method to determine if an image contains a cat.
     * @param image The image to analyze.
     * @param confidenceLevel The confidence level for cat detection.
     * @return true if a cat is detected, false otherwise.
     */
    boolean imageContainsCat(BufferedImage image, float confidenceLevel);
}
