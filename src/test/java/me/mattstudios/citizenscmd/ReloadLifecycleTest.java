package me.mattstudios.citizenscmd;

import me.mattstudios.citizenscmd.files.CooldownHandler;
import me.mattstudios.citizenscmd.files.DataHandler;
import me.mattstudios.citizenscmd.permissions.PermissionsManager;
import org.bukkit.Server;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.plugin.messaging.Messenger;
import org.bukkit.scheduler.BukkitScheduler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.nio.file.Files;
import java.util.*;
import java.util.logging.Logger;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ReloadLifecycleTest {
    @TempDir Path folder;

    private CitizensCMD plugin() {
        CitizensCMD plugin = mock(CitizensCMD.class);
        when(plugin.getLogger()).thenReturn(mock(Logger.class));
        when(plugin.getDataFolder()).thenReturn(folder.toFile());
        return plugin;
    }

    private Player player(CitizensCMD plugin, PermissionAttachment... attachments) {
        Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        when(player.addAttachment(plugin)).thenReturn(attachments[0], Arrays.copyOfRange(attachments, 1, attachments.length));
        return player;
    }

    @Test
    void commandExceptionStillRemovesAttachment() {
        CitizensCMD plugin = plugin();
        PermissionAttachment attachment = mock(PermissionAttachment.class);
        Player player = player(plugin, attachment);
        PermissionsManager manager = new PermissionsManager(plugin);
        assertThrows(IllegalStateException.class, () -> manager.withPermission(player, "test", () -> {
            throw new IllegalStateException("Command failed");
        }));
        verify(player).removeAttachment(attachment);
        manager.close();
        verify(player, times(1)).removeAttachment(attachment);
    }

    @Test
    void unloadInsideNestedCommandRemovesAllGrantsExactlyOnce() {
        CitizensCMD plugin = plugin();
        PermissionAttachment first = mock(PermissionAttachment.class), second = mock(PermissionAttachment.class);
        Player player = player(plugin, first, second);
        PermissionsManager manager = new PermissionsManager(plugin);
        manager.withPermission(player, "test", () ->
                manager.withPermission(player, "test", manager::close));
        manager.close();
        verify(player).removeAttachment(first);
        verify(player).removeAttachment(second);
        assertThrows(IllegalStateException.class, () -> manager.setPermission(player, "test"));
    }

    @Test
    void closePreventsOldSaverOverwritingNewInstanceData() throws Exception {
        CitizensCMD plugin = plugin();
        DataHandler data = new DataHandler(plugin);
        when(plugin.getDataHandler()).thenReturn(data);
        data.initialize();
        CooldownHandler old = new CooldownHandler(plugin);
        old.initialize();
        old.addInteraction(7, "old", 123);
        old.close();
        Path file = folder.resolve("data/cooldowns.yml");
        String newData = "cooldown-data:\n  npc-7:\n    new: 456\n";
        Files.writeString(file, newData);
        old.addInteraction(7, "late", 789);
        old.saveToFile();
        old.close();
        assertEquals(newData, Files.readString(file));
    }

    @Test
    void invalidNpcDataAbortsBeforeCooldownsAreLoaded() throws Exception {
        CitizensCMD plugin = plugin();
        Path data = Files.createDirectories(folder.resolve("data"));
        Files.writeString(data.resolve("saves.yml"), "npc-data: [broken\n");
        String cooldowns = "cooldown-data:\n  npc-7:\n    existing: 123\n";
        Files.writeString(data.resolve("cooldowns.yml"), cooldowns);
        assertThrows(IllegalStateException.class, () -> new DataHandler(plugin).initialize());
        assertEquals(cooldowns, Files.readString(data.resolve("cooldowns.yml")));
    }

    @Test
    void failedInitializationNeverOverwritesCooldownFile() throws Exception {
        CitizensCMD plugin = plugin();
        Path file = Files.createDirectories(folder.resolve("data")).resolve("cooldowns.yml");
        Files.writeString(file, "cooldown-data: [broken\n");
        CooldownHandler handler = new CooldownHandler(plugin);
        assertThrows(IllegalStateException.class, handler::initialize);
        String repaired = "cooldown-data:\n  npc-7:\n    restored: 123\n";
        Files.writeString(file, repaired);
        handler.close();
        assertEquals(repaired, Files.readString(file));
    }

    @Test
    void disableContinuesAfterFailureRemovesOnlyOwnedCommandsAndCanRepeat() throws Exception {
        CitizensCMD plugin = plugin();
        Server server = mock(Server.class);
        when(plugin.getServer()).thenReturn(server);
        BukkitScheduler scheduler = mock(BukkitScheduler.class);
        when(server.getScheduler()).thenReturn(scheduler);
        Messenger messenger = mock(Messenger.class);
        when(server.getMessenger()).thenReturn(messenger);
        CommandMap commandMap = mock(CommandMap.class);
        when(server.getCommandMap()).thenReturn(commandMap);
        Command ours = mock(Command.class), other = mock(Command.class);
        Map<String, Command> commands = new HashMap<>();
        commands.put("npcmd", ours);
        commands.put("citizenscmd:npcmd", ours);
        commands.put("other", other);
        when(commandMap.getKnownCommands()).thenReturn(commands);
        set(plugin, "registeredCommands", new HashSet<>(Set.of(ours)));
        Map<String, Boolean> confirmations = new HashMap<>(Map.of("player.7", true));
        set(plugin, "waitingList", confirmations);
        set(plugin, "usePAPI", true);
        CooldownHandler cooldowns = mock(CooldownHandler.class);
        set(plugin, "cooldownHandler", cooldowns);
        PermissionsManager permissions = mock(PermissionsManager.class);
        set(plugin, "permissionsManager", permissions);
        doThrow(new IllegalStateException("simulated save failure")).when(cooldowns).close();
        doCallRealMethod().when(plugin).onDisable();
        plugin.onDisable();
        plugin.onDisable();
        verify(cooldowns).close();
        verify(permissions).close();
        verify(ours).unregister(commandMap);
        verify(other, never()).unregister(any());
        assertEquals(Map.of("other", other), commands);
        assertTrue(confirmations.isEmpty());
        verify(scheduler, times(2)).cancelTasks(plugin);
        verify(messenger, times(2)).unregisterOutgoingPluginChannel(plugin);
        verify(messenger, times(2)).unregisterIncomingPluginChannel(plugin);
        assertNull(CitizensCMD.getApi());
    }

    private static void set(CitizensCMD plugin, String name, Object value) throws Exception {
        var field = CitizensCMD.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(plugin, value);
    }
}
