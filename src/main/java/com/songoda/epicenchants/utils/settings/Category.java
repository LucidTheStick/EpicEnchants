package com.songoda.ultimateclaims.utils.settings;

public enum Category {

    MAIN("General settings and options."),

    INTERFACES("These settings allow you to alter the way interfaces look.",
            "They are used in GUI's to make paterns, change them up then open up a",
            "GUI to see how it works."),

    ENTITIES("Stacked Entity Settings."),

    ITEMS("Stacked Item Settings."),

    SPAWNERS("Stacked Spawner Settings."),

    DATABASE("Settings regarding the Database."),

    SYSTEM("System related settings.");

    private String[] comments;


    Category(String... comments) {
        this.comments = comments;
    }

    public String[] getComments() {
        return comments;
    }
}