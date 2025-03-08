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
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Pose;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityUnleashEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapPalette;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;
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

        if (!getConfig().contains("map")) {
            MapView blueMap = Bukkit.createMap(Bukkit.getWorld("world"));
            MapView redMap = Bukkit.createMap(Bukkit.getWorld("world"));
            MapView greenMap = Bukkit.createMap(Bukkit.getWorld("world"));

            config.set("map.blue", blueMap.getId());
            config.set("map.red", redMap.getId());
            config.set("map.green", greenMap.getId());
            saveConfig();
        }
    }

    @Override
    public void onDisable() {
        game.stopGame();
    }

    public void StartGame(Player beastChosen) {
        Player[] onlinePlayers = Bukkit.getOnlinePlayers().toArray(Player[]::new);

        game.getFreezers().clear();

        List<FreezerLocation> locs = getFreezerList();
        for (FreezerLocation loc : locs) {
            game.getFreezers().add(new FreezerInGame(loc));
        }

        List<ComputerLocation> pclocs = getComputerList();
        for (ComputerLocation loc : pclocs) {
            ComputerInGame pc = new ComputerInGame(loc);
            pc.generateFrame();
            game.getComputers().add(pc);
        }

        for (Player p : onlinePlayers) {
            game.getPlayers().put(p.getUniqueId(), new PlayerInGame(p));
            p.getInventory().clear();
            hidenameTeam.addEntry(p.getName());
        }

        game.setMinComputersToExit(getConfig().getInt("pc-to-hack"));
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
            game.tryActivateBeastAbility();

            e.setCancelled(true);
        }

        if (game.isStarted() && game.checkPlayer(e.getPlayer()) &&
                (e.getItemDrop().getItemStack().getType() == Material.TRIAL_KEY || e.getItemDrop().getItemStack().getType() == Material.COMPASS)) {
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
    public void onSneak(PlayerToggleSneakEvent e) {
        if (e.isSneaking()) {
            if (game.isStarted() && !game.checkBeast(e.getPlayer())) {
                e.getPlayer().setSneaking(false);
                e.getPlayer().setPose(Pose.SWIMMING, true);
            } else if (!game.isStarted()) {
                e.getPlayer().setSneaking(false);
                e.getPlayer().setPose(Pose.SWIMMING, true);
            }
        }else{
            e.getPlayer().setSneaking(false);
            e.getPlayer().setPose(Pose.STANDING);
        }
        e.setCancelled(true);
    }

    @EventHandler
    public void onJump(PlayerJumpEvent e) {
        if (game.isStarted() && game.checkBeast(e.getPlayer())) {
            e.getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 2));
        }
    }

    @EventHandler
    public void onDamage(EntityDamageEvent e) {
        if (e.getCause() == EntityDamageEvent.DamageCause.FALL || e.getCause() == EntityDamageEvent.DamageCause.FREEZE) e.setCancelled(true);
    }

    @EventHandler
    public void onHit(EntityDamageByEntityEvent e) {
        if (!game.isStarted()) return;

        if (e.getEntity() instanceof Player && e.getDamager() instanceof Player) {
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

        if (e.getEntity().getType() == EntityType.ITEM_FRAME || e.getEntity().getType() == EntityType.PAINTING) {
            e.setCancelled(true);
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
        if (!game.isStarted()) return;

        PlayerInGame p = game.getPlayers().get(e.getPlayer().getUniqueId());

        if (e.getAction() == Action.RIGHT_CLICK_BLOCK && p != null) {
            Block clicked = e.getClickedBlock();

            if (clicked != null && clicked.getType().name().endsWith("_BUTTON")) {
                for (FreezerInGame freezer : game.getFreezers()) {
                    if (Util.locationToString(freezer.getLocations().getButtonLocation()).equals(Util.locationToString(clicked.getLocation().toBlockLocation()))) {
                        if (game.checkBeast(e.getPlayer()) && game.getLeashedPlayer() != null && freezer.getPlayer() == null) {
                            // Caso: a besta captura um jogador e coloca-o para congelar.
                            game.freeze(freezer, game.getLeashedPlayer().getPlayer());

                            return;
                        } else if (game.checkPlayer(e.getPlayer()) && freezer.getPlayer() != null && freezer.getPlayer().isFreezing() && freezer.getPlayer().getFrozenPercentage() < 100 && !game.getPlayers().get(e.getPlayer().getUniqueId()).isKnocked()) {
                            // Caso: um jogador quer resgatar alguém que está congelando.
                            Player playerInFreezer = freezer.getPlayer().getPlayer();
                            playerInFreezer.getPlayer().setCollidable(true);
                            playerInFreezer.teleport(freezer.getLocations().getButtonLocation());

                            ItemStack tracker = game.getFreezerTrackers().get(playerInFreezer.getUniqueId());

                            if (tracker != null) {
                                game.getPlayers().forEach((uuid, playerInGame) -> {
                                    playerInGame.getPlayer().getInventory().removeItem(tracker);
                                });

                                game.getFreezerTrackers().remove(playerInFreezer.getUniqueId());
                            }

                            freezer.getPlayer().stopFreezing();
                            freezer.setPlayer(null);

                            return;
                        }
                    }
                }

                // Caso: apertou um botão de computador
                if (game.checkBeast(e.getPlayer())) return;
                if (p.getComputerHacking() != null) return;
                if (p.isKnocked() || p.isFreezing() || p.isDead() || p.isEscaped()) return;

                for (ComputerInGame computer : game.getComputers()) {
                    if (Util.locationToString(computer.getLocation().getButtonLocation()).equals(Util.locationToString(clicked.getLocation().toBlockLocation()))) {
                        computer.startHacking(p);
                    }
                }
            }

            if (clicked != null && clicked.getType() == Material.IRON_DOOR && e.getPlayer().getInventory().getItemInMainHand().getType() == Material.TRIAL_KEY) {
                game.unlockDoor(p, clicked);
            }
        } else if (e.getAction() == Action.PHYSICAL &&
                e.getClickedBlock().getType() == Material.TRIPWIRE &&
                p != null &&
                !p.isEscaped() &&
                !p.isDead() &&
                p.getComputerHacking() == null &&
                !p.isFreezing() &&
                !game.checkBeast(p.getPlayer()) &&
                game.checkPlayer(p.getPlayer()) &&
                game.getHackedComputers() >= game.getMinComputersToExit()
        ) {
            // Caso: alguém tocou no gancho de saída.
            p.setEscaped(true);
            p.getPlayer().setGameMode(GameMode.SPECTATOR);

            game.getPlayers().forEach((uuid, playerInGame) -> playerInGame.getPlayer().sendTitle(p.getPlayer().getName(), "escapou"));

            e.setCancelled(true);
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

    public List<ComputerLocation> getComputerList() {
        List<ComputerLocation> freezers = new ArrayList<>();
        if (config.contains("computers")) {
            for (String key : config.getConfigurationSection("computers").getKeys(false)) {
                String buttonLocation = config.getString("computers." + key + ".button-location");
                String playerLocation = config.getString("computers." + key + ".screen-location");
                freezers.add(new ComputerLocation(Integer.parseInt(key), buttonLocation, playerLocation));
            }
        }
        return freezers;
    }
}
