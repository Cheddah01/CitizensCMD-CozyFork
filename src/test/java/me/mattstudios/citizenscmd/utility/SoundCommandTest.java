package me.mattstudios.citizenscmd.utility;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SoundCommandTest {
    @Test
    void reportedVillagerCommandUsesLegacyLookupNotRawPlayback() {
        Player player = mock(Player.class);
        SoundCommand command = SoundCommand.parse("ENTITY_VILLAGER_YES 1 1");
        assertEquals(1f, command.volume());
        assertEquals(1f, command.pitch());
        var reached = new java.util.concurrent.atomic.AtomicBoolean();
        assertThrows(UnsupportedOperationException.class, () -> command.play(player, name -> {
            assertEquals("ENTITY_VILLAGER_YES", name);
            reached.set(true);
            throw new UnsupportedOperationException("Stop at server registry boundary");
        }));
        assertTrue(reached.get());
        verifyNoInteractions(player);
    }

    @Test
    void unknownUppercaseNameCannotReachRawPlayback() {
        Player player = mock(Player.class);
        assertThrows(IllegalArgumentException.class, () -> SoundCommand.parse("INVALID_SOUND 1 1").play(player,
                name -> { throw new IllegalArgumentException("Unknown sound"); }));
        verifyNoInteractions(player);
    }

    @Test
    void resourceKeysKeepTheirFullNameAndIndependentVolumePitch() {
        for (String name : new String[]{"minecraft:entity.experience_orb.pickup", "entity.villager.yes", "cozy:npc/greeting2"}) {
            SoundCommand sound = SoundCommand.parse(name + " 0.7 1.4");
            assertEquals(name, sound.sound());
            assertEquals(0.7f, sound.volume());
            assertEquals(1.4f, sound.pitch());
            Player player = mock(Player.class);
            Location location = mock(Location.class);
            when(player.getLocation()).thenReturn(location);
            sound.play(player);
            verify(player).playSound(location, name, 0.7f, 1.4f);
        }
    }

    @Test
    void defaultsAndWhitespaceAndDigitsArePreserved() {
        assertEquals(new SoundCommand("MUSIC_DISC_13", 1f, 1f), SoundCommand.parse(" MUSIC_DISC_13 "));
        assertEquals(new SoundCommand("entity.villager.yes", 2f, 1f), SoundCommand.parse("entity.villager.yes\t 2"));
        assertEquals(new SoundCommand("BLOCK_NOTE_BLOCK_PLING", 0.5f, 2f), SoundCommand.parse("BLOCK_NOTE_BLOCK_PLING  .5   2"));
    }

    @Test
    void malformedArgumentsAreRejectedInsteadOfPartiallyMatched() {
        for (String input : new String[]{"", "entity.villager.yes nope", "a 1 2 3", "a NaN", "a Infinity", "a -1", "a 1 0", "a 1 1.2.3"}) {
            assertThrows(IllegalArgumentException.class, () -> SoundCommand.parse(input), input);
        }
    }
}
