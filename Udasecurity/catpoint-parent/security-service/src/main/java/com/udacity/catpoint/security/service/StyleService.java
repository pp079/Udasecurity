package com.udacity.catpoint.security.service;

import java.awt.*;

public final class StyleService {

    // Made field final and unmodifiable
    public static final Font HEADING_FONT = new Font("Sans Serif", Font.BOLD, 24);

    
    private StyleService() {
        throw new AssertionError("This is a utility class");
    }
}