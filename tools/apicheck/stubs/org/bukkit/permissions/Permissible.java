package org.bukkit.permissions;

import org.bukkit.plugin.Plugin;

import java.util.Set;

public interface Permissible {
    boolean hasPermission(String name);
    boolean isPermissionSet(String name);
    boolean isOp();
    void setOp(boolean value);
    PermissionAttachment addAttachment(Plugin plugin);
    void removeAttachment(PermissionAttachment attachment);
    void recalculatePermissions();
    Set<PermissionAttachmentInfo> getEffectivePermissions();
}
