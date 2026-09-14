package me.mattstudios.citizenscmd.utility;

import java.util.Locale;
import java.util.function.Function;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

/** Parses the existing sound [volume] [pitch] command without splitting resource keys. */
public record SoundCommand(String sound, float volume, float pitch) {
    public static SoundCommand parse(String input) {
        String[] parts = input.trim().split("\\s+");
        if (parts.length > 3 || parts[0].isEmpty()) {
            throw new IllegalArgumentException("Expected sound [volume] [pitch]");
        }
        if (!parts[0].matches("[A-Za-z0-9_.:/-]+")) {
            throw new IllegalArgumentException("Invalid sound name");
        }
        float volume = parts.length > 1 ? number(parts[1], "volume") : 1f;
        float pitch = parts.length > 2 ? number(parts[2], "pitch") : 1f;
        if (volume < 0 || pitch <= 0) {
            throw new IllegalArgumentException("Volume must be non-negative and pitch must be positive");
        }
        return new SoundCommand(parts[0], volume, pitch);
    }

    private static float number(String value, String name) {
        try {
            float number = Float.parseFloat(value);
            if (Float.isFinite(number)) {
                return number;
            }
        } catch (NumberFormatException ignored) {
        }
        throw new IllegalArgumentException("Invalid " + name + ": " + value);
    }

    public void play(Player player) {
        play(player, Sound::valueOf);
    }

    void play(Player player, Function<String, Sound> legacyLookup) {
        // Preserve Bukkit constant names; replacing underscores with dots is not equivalent.
        if (!sound.contains(":") && !sound.contains(".") && !sound.contains("/")) {
            Sound legacy;
            try {
                legacy = legacyLookup.apply(sound.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ignored) {
                legacy = null;
            }
            if (legacy != null) {
                player.playSound(player.getLocation(), legacy, volume, pitch);
                return;
            }
        }
        // String playback also supports client resource-pack sounds unknown to the server.
        if (NamespacedKey.fromString(sound) == null) {
            throw new IllegalArgumentException("Unknown Bukkit sound or invalid resource key: " + sound);
        }
        player.playSound(player.getLocation(), sound, volume, pitch);
    }
}
