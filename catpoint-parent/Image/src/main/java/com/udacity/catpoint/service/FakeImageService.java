package com.udacity.catpoint.service;

import java.util.Random;

/**
 * Service that tries to guess if an image displays a cat.
 */
public class FakeImageService implements ImageService
{
    private final Random r = new Random();

    public boolean imageContainsCat() {
        return r.nextBoolean();
    }
}
