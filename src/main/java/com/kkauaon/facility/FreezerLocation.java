package com.kkauaon.facility;

import org.bukkit.Location;

public class FreezerLocation {
    private final int number;
    private final Location buttonLocation;
    private final Location playerLocation;

    public FreezerLocation(int number, String buttonLocation, String playerLocation) {
        this.number = number;
        this.buttonLocation = Util.stringToLocation(buttonLocation);
        this.playerLocation = Util.stringToLocation(playerLocation);
    }

    public int getNumber() {
        return number;
    }

    public Location getButtonLocation() {
        return buttonLocation;
    }

    public Location getPlayerLocation() {
        return playerLocation;
    }
}