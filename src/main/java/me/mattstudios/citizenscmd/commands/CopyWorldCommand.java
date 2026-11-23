package me.mattstudios.citizenscmd.commands;

import static me.mattstudios.citizenscmd.utility.Util.HEADER;
import static me.mattstudios.citizenscmd.utility.Util.LEGACY;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.event.player.PlayerTeleportEvent;

import dev.triumphteam.cmd.bukkit.annotation.Permission;
import dev.triumphteam.cmd.core.annotation.Optional;
import dev.triumphteam.cmd.core.annotation.SubCommand;
import dev.triumphteam.cmd.core.annotation.Suggestion;
import me.mattstudios.citizenscmd.CitizensCMD;
import me.mattstudios.citizenscmd.utility.WorldCopyPlan;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.npc.NPCRegistry;

/**
 * {@code /npcmd copyworld <from> <to> [confirm]}: copies every NPC in one world into another at the same
 * coordinates, with skins, traits and their CitizensCMD click commands. Without {@code confirm} it only
 * says what it would do. Running it again skips NPCs that already have a copy there.
 */
public class CopyWorldCommand extends Npcmd {

    /** Persistent Citizens metadata on each copy: the id of the NPC it was copied from. */
    public static final String COPIED_FROM = "cozy-copied-from";

    private final CitizensCMD plugin;

    public CopyWorldCommand(final CitizensCMD plugin) {
        this.plugin = plugin;
    }

    @SubCommand("copyworld")
    @Permission("citizenscmd.copyworld")
    public void copyWorld(final CommandSender sender, @Suggestion("worlds") final String from,
                          @Suggestion("worlds") final String to, @Optional final String confirm) {
        final World fromWorld = Bukkit.getWorld(from);
        final World toWorld = Bukkit.getWorld(to);
        if (fromWorld == null || toWorld == null) {
            send(sender, "&cNo loaded world called &f" + (fromWorld == null ? from : to) + "&c.");
            return;
        }
        if (fromWorld.equals(toWorld)) {
            send(sender, "&cPick two different worlds.");
            return;
        }

        final NPCRegistry registry = CitizensAPI.getNPCRegistry();
        final List<WorldCopyPlan.Npc> npcs = new ArrayList<>();
        for (final NPC npc : registry) {
            final Location at = npc.getStoredLocation();
            if (at == null || at.getWorld() == null) {
                continue;
            }
            npcs.add(new WorldCopyPlan.Npc(npc.getId(), at.getWorld().getName(),
                    npc.data().has(COPIED_FROM) ? WorldCopyPlan.parseId(npc.data().get(COPIED_FROM)) : null));
        }
        final WorldCopyPlan.Result plan = WorldCopyPlan.plan(npcs, fromWorld.getName(), toWorld.getName());

        if (!"confirm".equalsIgnoreCase(confirm)) {
            send(sender, "&7Found &f" + plan.toCopy().size() + " &7NPC(s) in &f" + fromWorld.getName() + " &7to copy"
                    + (plan.alreadyCopied() > 0 ? " &8(" + plan.alreadyCopied() + " already copied, skipped)" : "") + "&7.");
            if (plan.toCopy().isEmpty()) {
                return;
            }
            send(sender, "&7Each copy goes to the &fsame coordinates &7in &f" + toWorld.getName()
                    + " &7with its skin, settings and click commands. The originals stay where they are.");
            send(sender, "&7Run &f/npcmd copyworld " + fromWorld.getName() + " " + toWorld.getName() + " confirm &7to copy them.");
            return;
        }

        int copied = 0;
        final List<String> failed = new ArrayList<>();
        for (final int id : plan.toCopy()) {
            final NPC original = registry.getById(id);
            if (original == null) {
                continue;
            }
            try {
                copy(original, toWorld);
                copied++;
            } catch (RuntimeException e) {
                failed.add("#" + id + " " + original.getName());
                plugin.getLogger().warning("Could not copy NPC " + id + " to " + toWorld.getName() + ": " + e.getMessage());
            }
        }
        registry.saveToStore();
        send(sender, "&aCopied &f" + copied + " &aNPC(s) to &f" + toWorld.getName() + "&a.");
        if (!failed.isEmpty()) {
            send(sender, "&cCouldn't copy: &f" + String.join("&7, &f", failed) + " &7(see the console).");
        }
        if (plan.alreadyCopied() > 0) {
            send(sender, "&8Skipped " + plan.alreadyCopied() + " that already had a copy there.");
        }
    }

    private void copy(final NPC original, final World toWorld) {
        final Location source = original.getStoredLocation();
        final Location target = new Location(toWorld, source.getX(), source.getY(), source.getZ(), source.getYaw(), source.getPitch());
        target.getChunk().load();

        // copy() fires Citizens' clone event, which copies the CitizensCMD click commands to the new id.
        final NPC copy = original.copy();
        copy.data().setPersistent(COPIED_FROM, original.getId());
        if (plugin.getDataHandler().hasNPCData(original.getId()) && !plugin.getDataHandler().hasNPCData(copy.getId())) {
            plugin.getDataHandler().cloneData(original.getId(), copy.getId());
        }

        if (copy.isSpawned()) {
            copy.teleport(target, PlayerTeleportEvent.TeleportCause.PLUGIN);
        } else {
            copy.spawn(target);
        }
        if (!original.isSpawned() && copy.isSpawned()) {
            copy.despawn(); // the original was hidden, so the copy is too, but it remembers its new spot
        }
    }

    private static void send(final CommandSender sender, final String message) {
        sender.sendMessage(HEADER);
        sender.sendMessage(LEGACY.deserialize(message));
    }
}
