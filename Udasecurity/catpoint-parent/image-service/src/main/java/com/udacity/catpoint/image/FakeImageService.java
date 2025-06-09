package com.udacity.catpoint.image;

import java.awt.image.BufferedImage;
import java.util.Random;

public class FakeImageService implements ImageService {
    private final Random random;
    private boolean deterministicMode = false;
    private boolean deterministicResult = false;
    private float lastUsedThreshold = 0.5f;

    public FakeImageService() {
        this.random = new Random();
    }

    public FakeImageService(long seed) {
        this.random = new Random(seed);
    }

    @Override
    public boolean imageContainsCat(BufferedImage image, float confidenceThreshold) {
        this.lastUsedThreshold = confidenceThreshold;

        if (deterministicMode) {
            return deterministicResult;
        }
        return random.nextBoolean();
    }

    public FakeImageService withDeterministicResult(boolean alwaysReturnCat) {
        this.deterministicMode = true;
        this.deterministicResult = alwaysReturnCat;
        return this;
    }

    public FakeImageService withRandomResults() {
        this.deterministicMode = false;
        return this;
    }

    public float getLastUsedThreshold() {
        return lastUsedThreshold;
    }
}
