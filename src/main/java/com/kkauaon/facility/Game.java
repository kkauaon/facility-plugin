package com.kkauaon.facility;

import fr.mrmicky.fastboard.adventure.FastBoard;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.Openable;
import org.bukkit.block.data.type.Door;
import org.bukkit.entity.Axolotl;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Silverfish;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.w3c.dom.Text;

import java.util.*;

public class Game {
    private static int ABILITY_TIME = 4;

    private boolean started;
    private final Map<UUID, PlayerInGame> players;

    private final List<FreezerInGame> freezers;
    private final List<ComputerInGame> computers;

    private PlayerInGame beast;
    private int hackedComputers;
    private final int minComputersToExit;

    private final List<BukkitRunnable> runnables;

    private final List<Block> restore;
    private final List<BlockState> doorsOpened;

    private Axolotl rope;
    private PlayerInGame leashedPlayer;

    private GameLoop gameLoop;
    private BeastLoop beastLoop;

    private boolean canBeastAbility;

    public Game() {
        started = false;
        canBeastAbility = true;
        players = new HashMap<>();
        beast = null;
        hackedComputers = 0;
        minComputersToExit = 2;
        runnables = new ArrayList<>();
        freezers = new ArrayList<>();
        restore = new ArrayList<>();
        computers = new ArrayList<>();
        doorsOpened = new ArrayList<>();
    }

    public List<BukkitRunnable> getRunnables() {
        return runnables;
    }

    public void prepareRope() {
        Axolotl silverfish = (Axolotl) Bukkit.getWorld("world").spawnEntity(new Location(Bukkit.getWorld("world"), 0,0,0), EntityType.AXOLOTL);
        silverfish.setAI(true);
        silverfish.setCollidable(false);
        silverfish.setSilent(true);
        silverfish.setInvulnerable(true);
        silverfish.setInvisible(true);
        silverfish.setMetadata("leashEntity", new FixedMetadataValue(Facility.getInstance(), true));
        rope = silverfish;
        leashedPlayer = null;
        Facility.getInstance().getLogger().info("Axolotl spawned!");
    }

    public void chooseBeast() {
        // First will be always the beast.
        PlayerInGame beast = players.entrySet().stream().findFirst().get().getValue();
        setBeast(beast);

        beast.getPlayer().getInventory().setItem(0, new ItemStack(Material.MACE, 1));
        beast.getPlayer().getInventory().setHeldItemSlot(0);
    }

    public void chooseBeast(Player chosen) {
        // First will be always the beast.
        PlayerInGame beast = players.get(chosen.getUniqueId());
        setBeast(beast);

        beast.getPlayer().getInventory().setItem(0, new ItemStack(Material.MACE, 1));
        beast.getPlayer().getInventory().setHeldItemSlot(0);
    }

    public void stopGame() {
        String lobbyString = Facility.getInstance().getServerConfig().getString("lobby-location");
        Location loc = Util.stringToLocation(lobbyString);

        runnables.forEach(BukkitRunnable::cancel);
        restore.forEach(block -> block.setType(Material.AIR));

        doorsOpened.forEach(bs -> {
            Openable door = (Openable)bs.getBlockData();
            door.setOpen(false);
            bs.setBlockData(door);
            bs.update();
        });

        computers.forEach(computerInGame -> {
            if (computerInGame.getItemFrame() != null)
                computerInGame.getItemFrame().remove();
        });

        players.forEach((uuid, playerInGame) -> {
            Player p = playerInGame.getPlayer();
            p.teleport(loc);
            p.setCollidable(true);
            p.clearActivePotionEffects();
            p.setGameMode(GameMode.ADVENTURE);
            playerInGame.stopFreezing();
            p.getInventory().clear();

            Facility.getInstance().getTeam().removeEntry(p.getName());

            FastBoard board = Facility.getInstance().getBoards().remove(uuid);

            if (board != null) {
                board.delete();
            }
        });

        destroyRope();

        started = false;
        players.clear();
        freezers.clear();
        computers.clear();
        runnables.clear();
        restore.clear();
        doorsOpened.clear();
        beast = null;
        hackedComputers = 0;
    }

    public void destroyRope() {
        if (rope == null) {
            leashedPlayer = null;
            return;
        }

        rope.setLeashHolder(null);
        rope.remove();
        leashedPlayer = null;
    }

    public boolean checkBeast(Player p) {
        if (beast == null) return false;

        return p.getPlayer().getUniqueId() == beast.getPlayer().getUniqueId();
    }

    public boolean checkPlayer(Player p) {
        return players.containsKey(p.getUniqueId());
    }

    public void scoreboardSetup() {
        players.forEach((uuid, playerInGame) -> {
            FastBoard board = Facility.getInstance().getBoards().remove(uuid);

            if (board != null) {
                board.delete();
            }

            board = new FastBoard(playerInGame.getPlayer());
            board.updateTitle(Component.text("Facility").color(TextColor.color(0xE61C3E)).decorate(TextDecoration.BOLD));

            Facility.getInstance().getBoards().put(uuid, board);
        });
    }

    public void scoreboardUpdate() {
        Collection<Component> lines = new ArrayList<>();

        lines.add(Component.text(""));

        if (hackedComputers >= minComputersToExit) lines.add(Component.text("Encontre a saída"));
        else lines.add(Component.text((minComputersToExit - hackedComputers) + " PCs restantes"));

        lines.add(Component.text(""));

        players.forEach((uuid, playerInGame) -> {
            if (uuid != beast.getPlayer().getUniqueId()) {
                lines.add(
                        Component.text(playerInGame.getPlayer().getName()).color(TextColor.color(255, 255, 255))
                );

                Component lifeLine = Component.text("❤ " + (100 - playerInGame.getFrozenPercentage()) + "%");

                if (playerInGame.isFreezing()) lifeLine = lifeLine.append(Component.text(" ❄")).color(TextColor.color(0x3DA2C4));
                else lifeLine = lifeLine.color(TextColor.color(0xE61C3E));

                lines.add(lifeLine);

                lines.add(Component.text(""));
            }
        });

        players.forEach((uuid, playerInGame) -> {
            FastBoard board = Facility.getInstance().getBoards().get(uuid);

            if (board != null) {
                board.updateLines(
                        lines
                );
            } else {
                scoreboardSetup();
            }
        });
    }

    public void beastHit(Player p) {
        PlayerInGame player = players.get(p.getUniqueId());
        player.setKnocked(true);

        if (player.getComputerHacking() != null) {
            player.getComputerHacking().error();
        }

        p.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 28 * 20, 5));

        players.forEach((uuid, playerInGame) -> playerInGame.getPlayer().spawnParticle(Particle.TOTEM_OF_UNDYING, p.getLocation(), 1) );

        KnockedTimer timer = new KnockedTimer(player);
        runnables.add(timer);

        // De-rope other player (if roped)
        Facility.getInstance().getLogger().info("De-roped on beastHit().");
        destroyRope();

        timer.runTaskTimer(Facility.getInstance(), 0, 20L);
    }

    public void beastRope(Player p) {
        Facility.getInstance().getLogger().info("beastRope() called.");

        destroyRope();
        prepareRope();

        p.setCollidable(false);
        rope.teleport(p);
        rope.setMetadata("leashEntity", new FixedMetadataValue(Facility.getInstance(), true));
        rope.setLeashHolder(beast.getPlayer());
        leashedPlayer = players.get(p.getUniqueId());

        // Criar um loop para mover o Silverfish e puxar o jogador
        RopeLoop ropeLoop = new RopeLoop(p);
        runnables.add(ropeLoop);
        ropeLoop.runTaskTimer(Facility.getInstance(), 0, 5); // Executa a cada 5 ticks
    }

    public void freeze(FreezerInGame freezer, Player p) {
        destroyRope();
        p.setCollidable(true);
        p.teleport(freezer.getLocations().getPlayerLocation());

        players.get(p.getUniqueId()).startFreezing();

        FreezeTimer timer = new FreezeTimer(players.get(p.getUniqueId()));
        runnables.add(timer);

        timer.runTaskTimer(Facility.getInstance(), 0, 10L);

        players.forEach((uuid, playerInGame) -> playerInGame.getPlayer().sendTitle(p.getName(), "foi capturado"));
    }

    public void startGameLoop() {
        Facility.getInstance().getBoards().forEach((uuid, fastBoard) -> {
            fastBoard.updateLines(
                    Component.text(""),
                    Component.text("15 sec head start"),
                    Component.text("")
            );
        });

        Bukkit.getScheduler().runTaskLater(Facility.getInstance(), () -> {
            String playerSpawnString = Facility.getInstance().getServerConfig().getString("player-spawn");
            Location playerSpawn = Util.stringToLocation(playerSpawnString);

            beast.getPlayer().teleport(playerSpawn);

            gameLoop = new GameLoop();
            gameLoop.runTaskTimer(Facility.getInstance(), 0, 10L);

            beastLoop = new BeastLoop(beast);
            beastLoop.runTaskTimer(Facility.getInstance(), 0, 20L);

            runnables.add(gameLoop);
            runnables.add(beastLoop);
        }, 15 * 20L);
    }

    public List<ComputerInGame> getComputers() {
        return computers;
    }

    public void addHackedComputer() {
        hackedComputers += 1;

        if (hackedComputers >= minComputersToExit) {
            openExits();
        }
    }

    public void openExits() {
        players.forEach((uuid, playerInGame) -> {

            Player p = playerInGame.getPlayer();
            p.playSound(p.getLocation(), Sound.BLOCK_IRON_DOOR_OPEN, 1, 1);
            p.sendActionBar(Component.text("Encontre a saída.").color(TextColor.color(0xE8A701)));

            if (!checkBeast(p)) {
                ItemStack chave = new ItemStack(Material.TRIAL_KEY);
                chave.setAmount(1);

                ItemMeta im = chave.getItemMeta();
                im.displayName(Component.text("Chave da Saída"));

                chave.setItemMeta(im);

                p.getInventory().addItem(chave);
            }
        });
    }

    public void unlockDoor(PlayerInGame p, Block b) {
        BlockState doorState = b.getState();
        Openable openable = (Openable) doorState.getBlockData();

        if (openable.isOpen() || p.isKnocked() || p.isFreezing() || p.getComputerHacking() != null) return;

        UnlockLoop loop = new UnlockLoop(p, openable, doorState);
        runnables.add(loop);
        loop.runTaskTimer(Facility.getInstance(), 0, 10L);
    }

    public void tryActivateBeastAbility() {
        if (beastLoop.ability > 30) {
            beastLoop.ability = -1;
            beast.getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.SPEED, ABILITY_TIME * 20, 2));

            Bukkit.getScheduler().runTaskLater(Facility.getInstance(), () -> {
                beastLoop.ability = 0;
            }, ABILITY_TIME * 20L);
        }
    }

    private class GameLoop extends BukkitRunnable {
        @Override
        public void run() {
            scoreboardUpdate();
        }
    }

    private class BeastLoop extends BukkitRunnable {
        public int ability = 0;
        private PlayerInGame beast;

        public BeastLoop(PlayerInGame p) {
            beast = p;
        }

        @Override
        public void run() {
            if (ability != -1) {
                if (ability <= 30) {
                    String symbol = "▋";
                    int percentComplete = (int) Util.remap(ability, 0, 30, 0, 70);
                    int percentLeft = 70 - percentComplete;

                    Component actionBarText = Component
                            .text()
                            .append(
                                    Component.text(symbol.repeat(percentComplete)).color(TextColor.color(0xE61C3E))
                            )
                            .append(
                                    Component.text(symbol.repeat(percentLeft)).color(TextColor.color(0x2F3330))
                            )
                            .build();

                    beast.getPlayer().sendActionBar(
                            actionBarText
                    );

                    ability += 1;
                } else {
                    beast.getPlayer().sendActionBar(
                            Component.text("Aperte Q para utilizar habilidade Corredor.").color(TextColor.color(0x32A852))
                    );
                }
            } else {
                beast.getPlayer().sendActionBar(
                        Component.text("Habilidade em uso.").color(TextColor.color(0xE8A701))
                );
            }
        }
    }

    private class UnlockLoop extends BukkitRunnable {
        private PlayerInGame player;
        private Openable door;
        private BlockState block;
        private int complete = 0;

        public UnlockLoop(PlayerInGame p, Openable o, BlockState b) {
            player = p;
            door = o;
            block = b;
        }

        @Override
        public void run() {
            if (complete <= 30) {
                String symbol = "▋";
                int percentComplete = (int) Util.remap(complete, 0, 30, 0, 70);
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

                player.getPlayer().sendActionBar(
                        actionBarText
                );

                if (player.getPlayer().getLocation().distanceSquared(block.getLocation()) > 10 || player.isKnocked() || player.isFreezing() || player.getComputerHacking() != null) {
                    player.getPlayer().sendActionBar(
                            Component
                                    .text("Chegue mais perto para abrir a saída.")
                                    .color(TextColor.color(0xE61C3E))
                    );

                    Bukkit.getScheduler().runTaskLater(Facility.getInstance(), () -> {
                        player.getPlayer().sendActionBar(Component.text());
                    }, 20L);

                    runnables.remove(this);
                    cancel();
                }

                complete += 1;
            } else {
                door.setOpen(true);
                block.setBlockData(door);
                block.update();

                doorsOpened.add(block);

                players.forEach((uuid, playerInGame) -> playerInGame.getPlayer().playSound(block.getBlock().getLocation(), Sound.BLOCK_IRON_DOOR_OPEN, 1,1));

                Bukkit.getScheduler().runTaskLater(Facility.getInstance(), () -> {
                    player.getPlayer().sendActionBar(Component.text());
                }, 20L);

                runnables.remove(this);
                cancel();
            }
        }
    }

    private class RopeLoop extends BukkitRunnable {
        private final Player p;

        public RopeLoop(Player p) {
            this.p = p;
        }

        @Override
        public void run() {
            if (!rope.isValid() || !p.isOnline() || leashedPlayer == null) {
                destroyRope();
                cancel();
                return;
            }

            // Puxar o jogador para perto do Silverfish
            Location silverfishLoc = rope.getLocation();
            Location playerLoc = p.getLocation();

            if (silverfishLoc.distance(playerLoc) > 1) {
                p.teleport(silverfishLoc);
            }
        }
    }

    private class KnockedTimer extends BukkitRunnable {
        private PlayerInGame player;
        private int secondsLeft = 28;

        public KnockedTimer(PlayerInGame p) {
            player = p;
        }

        @Override
        public void run() {
            if (player.isFreezing()) {
                player.getPlayer().sendActionBar(Component.text());
                this.cancel();
                return;
            }

            if (secondsLeft <= 0) {
                player.getPlayer().sendActionBar(
                        Component.text()
                                .content("Liberado.")
                                .color(TextColor.color(0x69E801))
                );

                player.setKnocked(false);

                player.getPlayer().setCollidable(true);
                destroyRope();

                Bukkit.getScheduler().runTaskLater(Facility.getInstance(), () -> {
                    player.getPlayer().sendActionBar(Component.text());
                }, 20L);

                runnables.remove(this);
                this.cancel();
            } else {
                secondsLeft--;

                players.forEach((uuid, playerInGame) -> playerInGame.getPlayer().spawnParticle(Particle.BLOCK, player.getPlayer().getLocation().add(0,1,0), 10, Material.REDSTONE_BLOCK.createBlockData()));
                players.forEach((uuid, playerInGame) -> playerInGame.getPlayer().playSound(player.getPlayer().getLocation(), Sound.BLOCK_SPAWNER_PLACE, 1, 1));

                player.getPlayer().sendActionBar(
                        Component.text()
                                .content("" + secondsLeft + " segundos para ser liberado.")
                                .color(TextColor.color(0xE8A701))
                );
            }
        }
    }

    private class FreezeTimer extends BukkitRunnable {
        private PlayerInGame player;

        public FreezeTimer(PlayerInGame p) {
            player = p;
        }

        @Override
        public void run() {
            if (!player.isFreezing()) {
                player.getPlayer().sendActionBar(Component.text());
                this.cancel();
                return;
            }

            if (player.getFrozenPercentage() >= 100) {
                player.getPlayer().sendActionBar(
                        Component.text()
                                .content("Congelado")
                                .color(TextColor.color(0x3DA2C4))
                );

                players.forEach((uuid, playerInGame) -> playerInGame.getPlayer().sendTitle(player.getPlayer().getName(), "congelou"));

                Block block1 = player.getPlayer().getLocation().getBlock();
                block1.setType(Material.BLUE_ICE);
                Block block2 = player.getPlayer().getLocation().getBlock().getLocation().add(0,1,0).getBlock();
                block2.setType(Material.BLUE_ICE);
                restore.add(block1);
                restore.add(block2);

                player.getPlayer().setGameMode(GameMode.SPECTATOR);

                Bukkit.getScheduler().runTaskLater(Facility.getInstance(), () -> {
                    player.getPlayer().sendActionBar(Component.text());
                }, 20L);

                runnables.remove(this);
                this.cancel();
            } else {
                player.getPlayer().setFreezeTicks(player.getPlayer().getMaxFreezeTicks());
                player.setFrozenPercentage(player.getFrozenPercentage() + 1);
                player.getPlayer().sendActionBar(
                        Component.text()
                                .content("Estado de congelamento: " + player.getFrozenPercentage() + "%")
                                .color(TextColor.color(0x3DA2C4))
                );
            }
        }
    }

    public List<FreezerInGame> getFreezers() {
        return freezers;
    }

    public boolean isStarted() {
        return started;
    }

    public void setStarted(boolean started) {
        this.started = started;
    }

    public Axolotl getRope() {
        return rope;
    }

    public PlayerInGame getBeast() {
        return beast;
    }

    public void setBeast(PlayerInGame beast) {
        this.beast = beast;
    }

    public BeastLoop getBeastLoop() {
        return beastLoop;
    }

    public PlayerInGame getLeashedPlayer() {
        return leashedPlayer;
    }

    public void setLeashedPlayer(PlayerInGame leashedPlayer) {
        this.leashedPlayer = leashedPlayer;
    }

    public int getHackedComputers() {
        return hackedComputers;
    }

    public void setHackedComputers(int hackedComputers) {
        this.hackedComputers = hackedComputers;
    }

    public Map<UUID, PlayerInGame> getPlayers() {
        return players;
    }

    public int getMinComputersToExit() {
        return minComputersToExit;
    }

    public boolean isCanBeastAbility() {
        return canBeastAbility;
    }

    public void setCanBeastAbility(boolean canBeastAbility) {
        this.canBeastAbility = canBeastAbility;
    }
}
