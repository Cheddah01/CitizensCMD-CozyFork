package me.mattstudios.citizenscmd.files;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class DataKeyMatchTest {
    @Test
    void npcIdsMatchExactly() {
        assertTrue(DataHandler.belongsTo("npc-data.npc-1.right-click-commands", 1));
        assertFalse(DataHandler.belongsTo("npc-data.npc-12.right-click-commands", 1), "NPC 1 must not pick up NPC 12's commands");
        assertFalse(DataHandler.belongsTo("npc-data.npc-21.cooldown", 1));
        assertTrue(DataHandler.belongsTo("npc-data.npc-12.cooldown", 12));
    }
}
