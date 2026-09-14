/*
  CitizensCMD - Add-on for Citizens
  Copyright (C) 2018 Mateus Moreira
  <p>
  This program is free software: you can redistribute it and/or modify
  it under the terms of the GNU General Public License as published by
  the Free Software Foundation, either version 3 of the License, or
  (at your option) any later version.
  <p>
  This program is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU General Public License for more details.
  <p>
  You should have received a copy of the GNU General Public License
  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package me.mattstudios.citizenscmd.permissions;


import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachment;
import me.mattstudios.citizenscmd.CitizensCMD;

/** Main-thread temporary permissions; every attachment is removed before unload. */
public class PermissionsManager {
    private final List<Grant> grants = new ArrayList<>();
    private final CitizensCMD plugin;
    private boolean closed;

    private record Grant(Player player, String permission, PermissionAttachment attachment) {}

    public PermissionsManager(CitizensCMD plugin) {
        this.plugin = plugin;
    }

    private Grant grant(Player player, String permission) {
        if (closed) {
            throw new IllegalStateException("CitizensCMD permissions are closed");
        }
        PermissionAttachment attachment = player.addAttachment(plugin);
        Grant grant = new Grant(player, permission, attachment);
        grants.add(grant);
        attachment.setRemovalCallback(removed -> grants.remove(grant));
        attachment.setPermission(permission, true);
        return grant;
    }

    public void setPermission(Player player, String permission) {
        grant(player, permission);
    }

    public void unsetPermission(Player player, String permission) {
        for (int i = grants.size() - 1; i >= 0; i--) {
            Grant grant = grants.get(i);
            if (grant.player().getUniqueId().equals(player.getUniqueId()) && grant.permission().equals(permission)) {
                remove(grant);
                return;
            }
        }
    }

    public void withPermission(Player player, String permission, Runnable command) {
        Grant grant = grant(player, permission);
        try {
            command.run();
        } finally {
            remove(grant);
        }
    }

    private void remove(Grant grant) {
        if (grants.remove(grant)) {
            grant.attachment().setRemovalCallback(null);
            grant.player().removeAttachment(grant.attachment());
        }
    }

    public void close() {
        closed = true;
        for (Grant grant : new ArrayList<>(grants)) {
            try {
                remove(grant);
            } catch (RuntimeException exception) {
                plugin.getLogger().log(Level.SEVERE, "Could not remove temporary NPC permission", exception);
            }
        }
    }
}
