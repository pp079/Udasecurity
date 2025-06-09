module com.udacity.catpoint.security {
    requires com.google.gson;
    requires com.miglayout.swing;
    requires com.google.common;
    requires com.udacity.catpoint.image;
    requires java.desktop;
    requires java.prefs;

    opens com.udacity.catpoint.security.data to com.google.gson;
}