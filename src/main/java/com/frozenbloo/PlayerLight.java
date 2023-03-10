package com.frozenbloo;

import net.minestom.server.entity.Player;
import net.minestom.server.network.player.PlayerConnection;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class PlayerLight extends Player {

    public PlayerLight(@NotNull UUID uuid , @NotNull String username , @NotNull PlayerConnection playerConnection) {
        super(uuid , username , playerConnection);

        setAutoViewable(false);
    }

    public void Update(long time) {
        // For keep alive
        interpretPacketQueue();
    }

    @Override
    public void handleVoid() {

    }
}
