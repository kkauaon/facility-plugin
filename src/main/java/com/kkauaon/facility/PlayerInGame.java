package com.kkauaon.facility;

import org.bukkit.entity.Player;

public class PlayerInGame {
    private Player player;
    private boolean isKnocked;
    private boolean isFreezing;
    private boolean dead;
    private boolean escaped;
    private int frozenPercentage;
    private ComputerInGame computerHacking;

    public PlayerInGame(Player p) {
        player = p;
        isKnocked = false;
        isFreezing = false;
        frozenPercentage = 0;
    }

    public void startFreezing() {
        isKnocked = false;
        isFreezing = true;
        player.setFreezeTicks(player.getMaxFreezeTicks());
    }

    public void stopFreezing() {
        player.setFreezeTicks(0);
        player.clearActivePotionEffects();
        isFreezing = false;
    }

    public boolean isFreezing() {
        return isFreezing;
    }

    public int getFrozenPercentage() {
        return frozenPercentage;
    }

    public void setFrozenPercentage(int frozenPercentage) {
        this.frozenPercentage = frozenPercentage;
    }

    public Player getPlayer() {
        return player;
    }

    public boolean isKnocked() {
        return isKnocked;
    }

    public void setKnocked(boolean knocked) {
        isKnocked = knocked;
    }

    public ComputerInGame getComputerHacking() {
        return computerHacking;
    }

    public void setComputerHacking(ComputerInGame computerHacking) {
        this.computerHacking = computerHacking;
    }

    public boolean isDead() {
        return dead;
    }

    public void setDead(boolean dead) {
        this.dead = dead;
    }

    public boolean isEscaped() {
        return escaped;
    }

    public void setEscaped(boolean escaped) {
        this.escaped = escaped;
    }
}
