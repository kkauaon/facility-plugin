package com.kkauaon.facility;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapPalette;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public class ComputerInGame {
    private final static int MAX_HACK_NUMBER = 150;

    private final Map<UUID, PlayerInGame> playersHacking;
    private int hackingPercentage;
    private final ComputerLocation location;

    private BukkitRunnable runnable;

    private ItemFrame itemFrame;

    private double distanceToError = 10d;

    public ComputerInGame(ComputerLocation location) {
        this.playersHacking = new HashMap<>();
        this.location = location;
        this.hackingPercentage = 0;
    }

    public void generateFrame() {
        World world = location.getScreenLocation().getWorld();
        Block block = world.getBlockAt(location.getScreenLocation());
        Block button = world.getBlockAt(location.getButtonLocation());

        itemFrame = world.spawn(location.getButtonLocation(), ItemFrame.class);
        itemFrame.setFacingDirection(block.getFace(button));

        mapColor(MapPalette.BLUE);
    }

    private void mapColor(byte color) {
        if (Facility.getInstance().getServerConfig().contains("map")) {
            ItemStack map = new ItemStack(Material.FILLED_MAP);

            MapView mapView = Bukkit.getMap(Facility.getInstance().getServerConfig().getInt("map.blue"));
            if (color == MapPalette.RED) mapView = Bukkit.getMap(Facility.getInstance().getServerConfig().getInt("map.red"));
            else if (color == MapPalette.LIGHT_GREEN) mapView = Bukkit.getMap(Facility.getInstance().getServerConfig().getInt("map.green"));

            mapView.getRenderers().clear();

            mapView.addRenderer(new MapRenderer() {
                @Override
                public void render(MapView view, MapCanvas canvas, Player player) {
                    for (int x = 0; x < 128; x++) {
                        for (int y = 0; y < 128; y++) {
                            canvas.setPixel(x, y, color);
                        }
                    }
                }
            });

            MapMeta mapMeta = (MapMeta) map.getItemMeta();
            mapMeta.setMapView(mapView);
            mapView.setTrackingPosition(false);
            mapView.setUnlimitedTracking(false);
            map.setItemMeta(mapMeta);

            itemFrame.setItem(map);
        }
    }

    public void startHacking(PlayerInGame p) {
        if (Facility.getInstance().getGame().checkBeast(p.getPlayer()) || hackingPercentage == ComputerInGame.MAX_HACK_NUMBER) return;
        if (playersHacking.containsKey(p.getPlayer().getUniqueId())) return;

        if (runnable != null) {
            runnable.cancel();
            runnable = null;
        }

        p.setComputerHacking(this);
        playersHacking.put(p.getPlayer().getUniqueId(), p);

        distanceToError = Facility.getInstance().getServerConfig().getDouble("pc-hack-distance");

        HackLoop loop = new HackLoop();
        runnable = loop;
        Facility.getInstance().getGame().getRunnables().add(runnable);
        loop.runTaskTimer(Facility.getInstance(), 0, 10L);
    }

    public void stopHacking(PlayerInGame p) {
        p.setComputerHacking(null);
        playersHacking.remove(p.getPlayer().getUniqueId());

        if (playersHacking.isEmpty() && runnable != null && !runnable.isCancelled()) {
            runnable.cancel();
            runnable = null;
        }
    }

    public void error() {
        mapColor(MapPalette.RED);

        for (Iterator<PlayerInGame> iterator = playersHacking.values().iterator(); iterator.hasNext();) {
            PlayerInGame player = iterator.next();
            stopHacking(player);
        }
        playersHacking.forEach((uuid, playerInGame) -> stopHacking(playerInGame));

        Facility.getInstance().getGame().getPlayers().forEach((uuid, playerInGame) -> {
            Player p = playerInGame.getPlayer();
            p.playSound(p.getLocation(), Sound.BLOCK_ANVIL_LAND, 1, 0.6f);
        });
    }

    public ComputerLocation getLocation() {
        return location;
    }

    public ItemFrame getItemFrame() {
        return itemFrame;
    }

    private class HackLoop extends BukkitRunnable {
        @Override
        public void run() {
            if (playersHacking.isEmpty()) {
                runnable = null;
                cancel();
            } else {
                if (hackingPercentage >= ComputerInGame.MAX_HACK_NUMBER) {
                    runnable = null;

                    playersHacking.forEach((uuid, playerInGame) -> {
                        mapColor(MapPalette.LIGHT_GREEN);

                        playerInGame.getPlayer().sendActionBar(
                                Component
                                        .text("Hack concluído.")
                                        .color(TextColor.color(0x32A852))
                        );

                        Bukkit.getScheduler().runTaskLater(Facility.getInstance(), () -> {
                            playerInGame.getPlayer().sendActionBar(Component.text());
                        }, 20L);

                        playerInGame.setComputerHacking(null);
                    });

                    playersHacking.clear();

                    Facility.getInstance().getGame().addHackedComputer();

                    cancel();
                } else {
                    String symbol = "▋";
                    int percentComplete = (int) Util.remap(hackingPercentage, 0, ComputerInGame.MAX_HACK_NUMBER, 0, 70);
                    int percentLeft = 70 - percentComplete;

                    Component actionBarText = Component
                            .text()
                            .append(
                                    Component.text(symbol.repeat(percentComplete)).color(TextColor.color(0x32A852))
                            )
                            .append(
                                    Component.text(symbol.repeat(percentLeft)).color(TextColor.color(0x2F3330))
                            )
                            .build();

                    for (Iterator<PlayerInGame> iterator = playersHacking.values().iterator(); iterator.hasNext();) {
                        PlayerInGame playerInGame = iterator.next();
                        playerInGame.getPlayer().sendActionBar(
                                actionBarText
                        );

                        if (playerInGame.getPlayer().getLocation().distanceSquared(location.getButtonLocation()) > distanceToError) {
                            error();

                            playerInGame.getPlayer().sendActionBar(
                                    Component
                                            .text("Chegue mais perto para hackear.")
                                            .color(TextColor.color(0xE61C3E))
                            );

                            Bukkit.getScheduler().runTaskLater(Facility.getInstance(), () -> {
                                playerInGame.getPlayer().sendActionBar(Component.text());
                            }, 20L);
                        }
                    }

                    hackingPercentage += 1 * playersHacking.size();
                }
            }
        }
    }
}
