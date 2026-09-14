package me.mattstudios.citizenscmd;

import ch.jalu.configme.SettingsManager;
import ch.jalu.configme.SettingsManagerBuilder;
import me.mattstudios.citizenscmd.files.CooldownHandler;
import me.mattstudios.citizenscmd.files.DataHandler;
import me.mattstudios.citizenscmd.utility.EnumTypes.ClickType;
import me.mattstudios.citizenscmd.utility.Util;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Server;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.messaging.Messenger;
import org.bukkit.scheduler.BukkitScheduler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.lang.reflect.Field;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CompatibilityTest {
    @TempDir Path folder;

    @Test
    void existingCommandsAndCooldownsSurviveDisableAndReload() throws Exception {
        Path dataFolder = Files.createDirectories(folder.resolve("data"));
        String saves = """
                npc-data:
                  npc-7:
                    cooldown: -1
                    price: 12.5
                    permission: server.guide
                    right-click-commands:
                    - 'console:say Welcome'
                    left-click-commands:
                    - 'message:&aHello'
                  npc-8:
                    cooldown: 3600
                """;
        Files.writeString(dataFolder.resolve("saves.yml"), saves);
        long now = System.currentTimeMillis();
        Files.writeString(dataFolder.resolve("cooldowns.yml"), """
                cooldown-data:
                  npc-7:
                    existing-player: 1
                  npc-8:
                    recent-player: %d
                """.formatted(now));
        CitizensCMD plugin = mock(CitizensCMD.class);
        when(plugin.getDataFolder()).thenReturn(folder.toFile());
        Server server = mock(Server.class);
        when(plugin.getServer()).thenReturn(server);
        when(server.getScheduler()).thenReturn(mock(BukkitScheduler.class));
        when(server.getMessenger()).thenReturn(mock(Messenger.class));
        DataHandler data = new DataHandler(plugin);
        when(plugin.getDataHandler()).thenReturn(data);
        data.initialize();
        CooldownHandler cooldowns = new CooldownHandler(plugin);
        cooldowns.initialize();
        assertTrue(cooldowns.onCooldown(7, "existing-player"));
        assertTrue(cooldowns.onCooldown(8, "recent-player"));
        assertEquals(12.5, data.getPrice(7));
        assertEquals(List.of("console:say Welcome"), data.getClickCommandsData(7, ClickType.RIGHT));
        assertEquals(List.of("message:&aHello"), data.getClickCommandsData(7, ClickType.LEFT));
        cooldowns.addInteraction(7, "new-player", now);
        Field handler = CitizensCMD.class.getDeclaredField("cooldownHandler");
        handler.setAccessible(true);
        handler.set(plugin, cooldowns);
        when(plugin.getWaitingList()).thenCallRealMethod();
        Field waiting = CitizensCMD.class.getDeclaredField("waitingList");
        waiting.setAccessible(true);
        waiting.set(plugin, new java.util.HashMap<>());
        doCallRealMethod().when(plugin).onDisable();
        plugin.onDisable();
        CooldownHandler reloaded = new CooldownHandler(plugin);
        reloaded.initialize();
        assertTrue(reloaded.onCooldown(7, "existing-player"));
        assertTrue(reloaded.onCooldown(7, "new-player"));
        assertTrue(reloaded.onCooldown(8, "recent-player"));
        assertEquals(saves, Files.readString(dataFolder.resolve("saves.yml")));
        verify(server.getScheduler()).cancelTasks(plugin);
    }

    @Test
    void freshInstallAndExistingConfigurationRemainValid() throws Exception {
        Path config = folder.resolve("config.yml");
        Files.writeString(config, "lang: FR\ncheck-updates: true\nshift-confirm: false\n");
        SettingsManager settings = SettingsManagerBuilder.withYamlFile(config)
                .configurationData(Settings.class).useDefaultMigrationService().create();
        assertEquals("FR", settings.getProperty(Settings.LANG));
        assertFalse(settings.getProperty(Settings.SHIFT_CONFIRM));
        assertFalse(settings.getProperty(Settings.MINIMESSAGE));
        CitizensCMD plugin = mock(CitizensCMD.class);
        when(plugin.getDataFolder()).thenReturn(folder.toFile());
        DataHandler data = new DataHandler(plugin);
        when(plugin.getDataHandler()).thenReturn(data);
        data.initialize();
        CooldownHandler cooldowns = new CooldownHandler(plugin);
        cooldowns.initialize();
        cooldowns.saveToFile();
        assertFalse(cooldowns.onCooldown(1, "new-player"));
        assertTrue(Files.exists(folder.resolve("data/saves.yml")));
        assertTrue(Files.exists(folder.resolve("data/cooldowns.yml")));
    }

    @Test
    void bundledLanguagesAndNativeAdventureMessagesLoad() throws Exception {
        try (var languages = Files.list(Path.of("src/main/resources/lang"))) {
            for (Path language : languages.toList()) {
                YamlConfiguration yaml = new YamlConfiguration();
                yaml.load(language.toFile());
                assertNotNull(yaml.getConfigurationSection("messages"), language.toString());
            }
        }
        var plain = PlainTextComponentSerializer.plainText();
        assertEquals("Welcome", plain.serialize(Util.LEGACY.deserialize("&aWelcome")));
        var message = Util.MINIMESSAGE.deserialize("<green><click:run_command:/spawn>Spawn</click></green>");
        assertEquals("Spawn", plain.serialize(message));
        assertEquals(ClickEvent.runCommand("/spawn"), message.clickEvent());
    }
}
