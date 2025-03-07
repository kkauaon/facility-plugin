package com.kkauaon.facility;

import org.bukkit.Location;

public class ComputerLocation {
    private final int number;
    private final Location buttonLocation;
    private final Location screenLocation;

    public ComputerLocation(int number, String buttonLocation, String screenLocation) {
        this.number = number;
        this.buttonLocation = Util.stringToLocation(buttonLocation);
        this.screenLocation = Util.stringToLocation(screenLocation);
    }

    public int getNumber() {
        return number;
    }

    public Location getButtonLocation() {
        return buttonLocation;
    }

    public Location getScreenLocation() {
        return screenLocation;
    }
}
