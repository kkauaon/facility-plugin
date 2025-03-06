package com.kkauaon.facility;

public class FreezerInGame {
    private final FreezerLocation locations;
    private PlayerInGame player;

    public FreezerInGame(FreezerLocation loc) {
        locations = loc;
    }

    public FreezerLocation getLocations() {
        return locations;
    }

    public PlayerInGame getPlayer() {
        return player;
    }

    public void setPlayer(PlayerInGame player) {
        this.player = player;
    }
}
