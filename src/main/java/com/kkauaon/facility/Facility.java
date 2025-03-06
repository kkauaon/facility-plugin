package com.kkauaon.facility;

import com.destroystokyo.paper.event.player.PlayerJumpEvent;
import fr.mrmicky.fastboard.adventure.FastBoard;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityUnleashEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.*;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.*;

public final class Facility extends JavaPlugin implements Listener {
    private FileConfiguration config = getConfig();
    private static Facility instance;
    private Game game;
    private final Map<UUID, FastBoard> boards = new HashMap<>();
    private Team hidenameTeam;

    @Override
    public void onEnable() {
        instance = this;
        game = new Game();
        saveDefaultConfig();

        getCommand("facility").setExecutor(new Iniciar());
        getServer().getPluginManager().registerEvents(this, this);

        Scoreboard score = Bukkit.getScoreboardManager().getMainScoreboard();
        hidenameTeam = score.getTeam("nhide");
        if(hidenameTeam == null) {
            hidenameTeam = score.registerNewTeam("nhide");
            hidenameTeam.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.NEVER);
        }
    }

    @Override
    public void onDisable() {
        game.stopGame();
    }

    public void StartGame(Player beastChosen) {
        Player[] onlinePlayers = Bukkit.getOnlinePlayers().toArray(Player[]::new);

        game.getFreezers().clear();

        List<FreezerLocation> locs = Facility.getInstance().getFreezerList();
        for (FreezerLocation loc : locs) {
            getLogger().info("Freezer verified: " + Util.locationToString(loc.getButtonLocation()));
            game.getFreezers().add(new FreezerInGame(loc));
        }

        for (Player p : onlinePlayers) {
            game.getPlayers().put(p.getUniqueId(), new PlayerInGame(p));
            p.getInventory().clear();
            hidenameTeam.addEntry(p.getName());
        }

        game.prepareRope();
        game.chooseBeast(beastChosen);
        game.scoreboardSetup();
        game.setStarted(true);

        String beastSpawnString = config.getString("beast-spawn");
        String playerSpawnString = config.getString("player-spawn");

        Location beastSpawn = Util.stringToLocation(beastSpawnString);
        Location playerSpawn = Util.stringToLocation(playerSpawnString);

        game.getPlayers().forEach((uuid, playerInGame) -> {
            Player p = playerInGame.getPlayer();

            p.setGameMode(GameMode.ADVENTURE);

            if (game.getBeast().getPlayer().getUniqueId() == uuid) {
                p.sendTitle("Besta", "");
                p.teleport(beastSpawn);
            } else {
                p.sendTitle("Sobrevivente", "");
                p.teleport(playerSpawn);
            }
        });

        game.startGameLoop();
    }

    @EventHandler
    public void onDropItem(PlayerDropItemEvent e) {
        if (game.isStarted() && game.checkBeast(e.getPlayer()) && e.getItemDrop().getItemStack().getType() == Material.MACE) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onSlotChange(PlayerItemHeldEvent e) {
        if (game.isStarted() && game.checkBeast(e.getPlayer())) {
            //e.getPlayer().getInventory().setHeldItemSlot(0);
            e.setCancelled(true);
        }
    }

    /*@EventHandler
    public void onSprint(PlayerToggleSprintEvent e) {
        if (game.isStarted() && game.getPlayers().get(e.getPlayer().getUniqueId()) != null && e.isSprinting()) {
            getLogger().info("Trying to cancel sprinting..");
            e.getPlayer().setSprinting(false);
            e.setCancelled(true);
        }
    }*/

    @EventHandler
    public void onJump(PlayerJumpEvent e) {
        if (game.isStarted() && game.checkBeast(e.getPlayer())) {
            e.getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 2));
        }
    }

    @EventHandler
    public void onHit(EntityDamageByEntityEvent e) {
        if (game.isStarted() && e.getEntity() instanceof Player && e.getDamager() instanceof Player) {
            Player whoWasHit = (Player) e.getEntity();
            Player whoHit = (Player) e.getDamager();

            if (game.checkBeast(whoHit) && game.checkPlayer(whoWasHit)) {
                e.setDamage(0);

                if (!game.getPlayers().get(whoWasHit.getUniqueId()).isKnocked() && !game.getPlayers().get(whoWasHit.getUniqueId()).isFreezing())
                    game.beastHit(whoWasHit);
                else {
                    if (game.getLeashedPlayer() != null) {
                        getLogger().info("Deroped at hit check.");
                        game.setLeashedPlayer(null);
                        game.getRope().setLeashHolder(null);
                    }

                    e.setCancelled(true);
                }
            } else if (game.checkPlayer(whoHit) && game.checkPlayer(whoWasHit)) {
                e.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onInteractOnPlayer(PlayerInteractAtEntityEvent e) {
        if (game.isStarted() && game.checkBeast(e.getPlayer()) && e.getRightClicked() instanceof Player) {
            Player whoWasClicked = (Player) e.getRightClicked();

            if (game.checkPlayer(whoWasClicked) && game.getPlayers().get(whoWasClicked.getUniqueId()).isKnocked()) {
                if (game.getLeashedPlayer() == null) {
                    getLogger().info("Now roping on " + whoWasClicked.getName());
                    game.beastRope(whoWasClicked);
                } else if (game.getLeashedPlayer().getPlayer().getUniqueId() != whoWasClicked.getUniqueId()) {
                    getLogger().info("Deroped");
                    game.destroyRope();
                }
            }
        }
    }

    @EventHandler
    public void onInteractOnBlock(PlayerInteractEvent e) {
        if (e.getAction() == Action.RIGHT_CLICK_BLOCK) {
            Block clicked = e.getClickedBlock();

            if (clicked != null && clicked.getType().name().endsWith("_BUTTON")) {
                for (FreezerInGame freezer : game.getFreezers()) {
                    if (Util.locationToString(freezer.getLocations().getButtonLocation()).equals(Util.locationToString(clicked.getLocation().toBlockLocation()))) {
                        if (game.checkBeast(e.getPlayer()) && game.getLeashedPlayer() != null && freezer.getPlayer() == null) {
                            // Caso: a besta captura um jogador e coloca-o para congelar.
                            game.freeze(freezer, game.getLeashedPlayer().getPlayer());
                        } else if (game.checkPlayer(e.getPlayer()) && freezer.getPlayer() != null && freezer.getPlayer().isFreezing() && freezer.getPlayer().getFrozenPercentage() < 100 && !game.getPlayers().get(e.getPlayer().getUniqueId()).isKnocked()) {
                            // Caso: um jogador quer resgatar alguém que está congelando.
                            Player playerInFreezer = freezer.getPlayer().getPlayer();
                            playerInFreezer.getPlayer().setCollidable(true);
                            playerInFreezer.teleport(freezer.getLocations().getButtonLocation());

                            freezer.setPlayer(null);
                            freezer.getPlayer().stopFreezing();
                        }
                    }
                }
            }
        }
    }

    @EventHandler
    public void onUnleash(EntityUnleashEvent e) {
        if (e.getReason() == EntityUnleashEvent.UnleashReason.DISTANCE) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        Player player = e.getPlayer();

        FastBoard board = this.boards.remove(player.getUniqueId());

        if (board != null) {
            board.delete();
        }
    }

    @EventHandler
    public void onHunger(FoodLevelChangeEvent e) {
        e.setCancelled(true);
    }

    public Game getGame() {
        return game;
    }

    public FileConfiguration getServerConfig() {
        return config;
    }

    public Team getTeam() {
        return hidenameTeam;
    }

    public static Facility getInstance() {
        return instance;
    }

    public Map<UUID, FastBoard> getBoards() {
        return boards;
    }

    public List<FreezerLocation> getFreezerList() {
        List<FreezerLocation> freezers = new ArrayList<>();
        if (config.contains("freezers")) {
            for (String key : config.getConfigurationSection("freezers").getKeys(false)) {
                String buttonLocation = config.getString("freezers." + key + ".button-location");
                String playerLocation = config.getString("freezers." + key + ".player-location");
                freezers.add(new FreezerLocation(Integer.parseInt(key), buttonLocation, playerLocation));
            }
        }
        return freezers;
    }
}
