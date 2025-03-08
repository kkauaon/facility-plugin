package com.kkauaon.facility;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;

public class Iniciar implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player) {
            Player player =  (Player) sender;

            if (args[0].equalsIgnoreCase("start")) {
                if (args.length == 1) {
                    sender.sendMessage("Informe um jogador para ser a besta.");
                    return true;
                }

                Player selected = Bukkit.getPlayer(args[1]);

                if (Facility.getInstance().getGame().isStarted()) {
                    sender.sendMessage("Uma partida já está acontecendo");
                    return true;
                }

                if (selected == null) {
                    sender.sendMessage("Informe um jogador para ser a besta.");
                    return true;
                }

                Facility.getInstance().StartGame(selected);
            } else if (args[0].equalsIgnoreCase("setlobby")) {
                Location loc = player.getLocation();
                String locString = Util.locationToString(loc);

                Facility.getInstance().getServerConfig().set("lobby-location", locString);
                Facility.getInstance().saveConfig();
                player.sendMessage("Localização do Lobby setada com sucesso!");
                return true;
            } else if (args[0].equalsIgnoreCase("setbeastspawn")) {
                Location loc = player.getLocation();
                String locString = Util.locationToString(loc);

                Facility.getInstance().getServerConfig().set("beast-spawn", locString);
                Facility.getInstance().saveConfig();
                player.sendMessage("Spawn da Besta foi setado com sucesso!");
                return true;
            } else if (args[0].equalsIgnoreCase("setplayerspawn")) {
                Location loc = player.getLocation();
                String locString = Util.locationToString(loc);

                Facility.getInstance().getServerConfig().set("player-spawn", locString);
                Facility.getInstance().saveConfig();
                player.sendMessage("Spawn de jogador foi setado com sucesso!");
                return true;
            } else if (args[0].equalsIgnoreCase("stop")) {
                player.sendMessage("Parando jogo...");
                Facility.getInstance().getGame().stopGame();
                return true;
            } else if (args[0].equalsIgnoreCase("setfreezerbutton")) {
                int spawnNumber = Integer.parseInt(args[1]);
                Block targetBlock = player.getTargetBlockExact(5);

                if (targetBlock == null || !targetBlock.getType().name().endsWith("_BUTTON")) {
                    player.sendMessage("Olhe para um botão para definir o freezer.");
                    return true;
                }

                Location loc = targetBlock.getLocation();
                Facility.getInstance().getServerConfig().set("freezers."+spawnNumber+".button-location", Util.locationToString(loc));
                Facility.getInstance().saveConfig();
                player.sendMessage("Freezer - Botão foi setado com sucesso!");

                return true;
            } else if (args[0].equalsIgnoreCase("setfreezerplayer")) {
                int spawnNumber = Integer.parseInt(args[1]);
                Location loc = player.getLocation();

                Facility.getInstance().getServerConfig().set("freezers."+spawnNumber+".player-location", Util.locationToString(loc));
                Facility.getInstance().saveConfig();
                player.sendMessage("Freezer - player foi setado com sucesso!");
                return true;
            } else if (args[0].equalsIgnoreCase("setpcbutton")) {
                int spawnNumber = Integer.parseInt(args[1]);
                Block targetBlock = player.getTargetBlockExact(5);

                if (targetBlock == null || !targetBlock.getType().name().endsWith("_BUTTON")) {
                    player.sendMessage("Olhe para um botão para definir o PC.");
                    return true;
                }

                Location loc = targetBlock.getLocation();
                Facility.getInstance().getServerConfig().set("computers."+spawnNumber+".button-location", Util.locationToString(loc));
                Facility.getInstance().saveConfig();
                player.sendMessage("Computador - Botão foi setado com sucesso!");

                return true;
            } else if (args[0].equalsIgnoreCase("setpcscreen")) {
                int spawnNumber = Integer.parseInt(args[1]);
                Block targetBlock = player.getTargetBlockExact(5);

                if (targetBlock == null) {
                    player.sendMessage("Olhe para um bloco para definir o PC.");
                    return true;
                }

                Location loc = targetBlock.getLocation();
                Facility.getInstance().getServerConfig().set("computers."+spawnNumber+".screen-location", Util.locationToString(loc));
                Facility.getInstance().saveConfig();
                player.sendMessage("Computador - Tela foi setada com sucesso!");

                return true;
            } else if (args[0].equalsIgnoreCase("actionbar") && args.length > 1) {
                player.sendActionBar(Component.text(String.join(" ", Arrays.copyOfRange(args, 1, args.length))));

                Bukkit.getScheduler().runTaskLater(Facility.getInstance(), () -> {
                    player.sendActionBar(Component.text());
                }, 40L);
            } else if (args[0].equalsIgnoreCase("setpchackdistance")) {
                if (args.length > 1) {
                    double distance = Double.parseDouble(args[1]);

                    Facility.getInstance().getServerConfig().set("pc-hack-distance", distance);
                    Facility.getInstance().saveConfig();

                    player.sendMessage("Ok.");
                }
            } else if (args[0].equalsIgnoreCase("setpctohack")) {
                if (args.length > 1) {
                    int number = Integer.parseInt(args[1]);

                    Facility.getInstance().getServerConfig().set("pc-to-hack", number);
                    Facility.getInstance().saveConfig();

                    player.sendMessage("Ok.");
                }
            }
        }

        return true;
    }

}