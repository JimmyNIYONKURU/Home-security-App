module Security
{
    requires Image;
    requires miglayout.swing;
    requires java.desktop;
    requires guava;
    requires com.google.gson;
    requires java.prefs;
    opens com.udacity.catpoint2.data to com.google.gson, org.mockito;

}