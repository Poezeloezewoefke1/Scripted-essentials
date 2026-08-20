package org.bukkit.profile;

import java.net.URL;

public interface PlayerTextures {
    URL getSkin();
    void setSkin(URL skinUrl);
    boolean isEmpty();
}
