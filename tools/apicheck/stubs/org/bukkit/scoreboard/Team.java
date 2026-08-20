package org.bukkit.scoreboard;

import net.kyori.adventure.text.Component;

import java.util.Set;

public interface Team {
    String getName();
    void displayName(Component displayName);
    void prefix(Component prefix);
    void suffix(Component suffix);
    void color(net.kyori.adventure.text.format.NamedTextColor color);
    void addEntry(String entry);
    void removeEntry(String entry);
    Set<String> getEntries();
    boolean hasEntry(String entry);
    void unregister();
    void setOption(Option option, OptionStatus status);
    void setAllowFriendlyFire(boolean enabled);
    void setCanSeeFriendlyInvisibles(boolean enabled);

    enum Option { NAME_TAG_VISIBILITY, DEATH_MESSAGE_VISIBILITY, COLLISION_RULE }
    enum OptionStatus { ALWAYS, NEVER, FOR_OTHER_TEAMS, FOR_OWN_TEAM }
}
