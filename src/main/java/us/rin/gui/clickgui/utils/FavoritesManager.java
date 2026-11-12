package us.rin.gui.clickgui.utils;

import net.minecraft.client.Minecraft;
import us.rin.gui.hud.utils.NotificationManager;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;

public final class FavoritesManager {

    private static final Set<String> favorites = new HashSet<>();
    private static final File favoritesFile = new File(Minecraft.getInstance().gameDirectory, "favorites.txt");

    private FavoritesManager() { }

    public static boolean isFavorite(String name) {
        if (name == null) return false;
        return favorites.contains(name);
    }

    public static void addFavorite(String name) {
        if (name == null) return;
        favorites.add(name);
        save();
        NotificationManager.push(NotificationManager.NotificationType.SUCCESS, "Favorite Modules", "Module " + name + " added to favorites", 5000);
    }

    public static void removeFavorite(String name) {
        if (name == null) return;
        favorites.remove(name);
        save();
        NotificationManager.push(NotificationManager.NotificationType.SUCCESS, "Favorite Modules", "Module " + name + " removed from favorites", 5000);
    }

    public static void save() {
        try {
            Files.write(favoritesFile.toPath(), favorites);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void load() {
        if (!favoritesFile.exists()) return;

        try {
            favorites.clear();
            favorites.addAll(Files.readAllLines(favoritesFile.toPath()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
