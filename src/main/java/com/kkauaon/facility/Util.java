package com.kkauaon.facility;

import org.bukkit.Bukkit;
import org.bukkit.Location;

public class Util {
    public static String locationToString(final Location loc) {
        return loc.getWorld().getName() + ", " + loc.getX() + ", " + loc.getY() + ", " + loc.getZ();
    }

    public static Location stringToLocation(final String string) {
        final String[] split = string.split(",");
        return new Location(Bukkit.getWorld(split[0]), Double.parseDouble(split[1]), Double.parseDouble(split[2]), Double.parseDouble(split[3]));
    }

    public static double remap(double value, double inputMin, double inputMax, double outputMin, double outputMax) {
        // Verifica se o intervalo de entrada é válido
        if (inputMax - inputMin == 0) {
            throw new IllegalArgumentException("Intervalo de entrada inválido (inputMax - inputMin deve ser diferente de zero).");
        }

        // Calcula a proporção do valor no intervalo de entrada
        double proportion = (value - inputMin) / (inputMax - inputMin);

        // Mapeia a proporção para o intervalo de saída
        return outputMin + proportion * (outputMax - outputMin);
    }
}
