package org.bukkit.permissions;

import java.util.Map;

public class Permission {
    public Permission(String name) { }
    public Permission(String name, String description) { }
    public Permission(String name, String description, PermissionDefault defaultValue) { }
    public Permission(String name, String description, PermissionDefault defaultValue, Map<String, Boolean> children) { }
    public String getName() { throw new UnsupportedOperationException(); }
    public Map<String, Boolean> getChildren() { throw new UnsupportedOperationException(); }
    public PermissionDefault getDefault() { throw new UnsupportedOperationException(); }
    public void setDefault(PermissionDefault value) { }
    public void recalculatePermissibles() { }
}
