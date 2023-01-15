package com.frozenbloo;

import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.Component;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.GameMode;
import net.minestom.server.event.player.PlayerChatEvent;
import net.minestom.server.event.player.PlayerLoginEvent;
import net.minestom.server.event.player.PlayerSpawnEvent;
import net.minestom.server.extras.MojangAuth;
import net.minestom.server.extras.bungee.BungeeCordProxy;
import net.minestom.server.extras.velocity.VelocityProxy;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.packet.client.play.ClientSetRecipeBookStatePacket;
import net.minestom.server.network.packet.server.CachedPacket;
import net.minestom.server.network.packet.server.play.CameraPacket;
import net.minestom.server.network.packet.server.play.SpawnEntityPacket;
import net.minestom.server.utils.NamespaceID;
import net.minestom.server.world.DimensionType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.UUID;

public class Main {

    private static final Path propertiesPath = Path.of("./server.properties");
    private static final Logger logger = LoggerFactory.getLogger("FrozenHorizon");

    public static void main(String... args) {

        System.setProperty("minestom.tps", "1");
        System.getProperty("minestom.chunk-view-distance", "2");
        System.getProperty("minestom.entity-view-distance", "0");

        final var properties = loadProperties();
        final var server = MinecraftServer.init();

        final var dimension = DimensionType.builder(NamespaceID.from("minecraft:oblivion"))
                .skylightEnabled(false)
                .ceilingEnabled(false)
                .fixedTime(null)
                .effects(properties.getProperty("end-dimension").equals("true") ? "the_end" : "")
                .ambientLight(1.0f)
                .height(16)
                .minY(0)
                .logicalHeight(16)
                .build();

        MinecraftServer.getDimensionTypeManager().addDimension(dimension);

        final var instance = new LightInstance(UUID.randomUUID(), dimension);
        MinecraftServer.getInstanceManager().registerInstance(instance);
        instance.setTimeRate(0);
        instance.setTimeUpdate(null);
        instance.enableAutoChunkLoad(false);

        for (int i = -1; i < 1; i++) {
            for (int j = -1; j < 1; j++) {
                instance.loadChunk(i, j);
            }
        }

        final var entityId = Entity.generateId();
        final var entityPacket = new SpawnEntityPacket(entityId, UUID.randomUUID(), EntityType.ARMOR_STAND.id() , Pos.ZERO, 0f, 0, (short) 0, (short) 0, (short) 0);
        final var cameraPacket = new CameraPacket(entityId);

        final var globalEventHandler = MinecraftServer.getGlobalEventHandler();

        MinecraftServer.getConnectionManager().setPlayerProvider(PlayerLight::new);

        MinecraftServer.getPacketListenerManager().setListener(ClientSetRecipeBookStatePacket.class, (a, b) -> {});

        MinecraftServer.setBrandName("Frozen Horizon");

        globalEventHandler.addListener(PlayerLoginEvent.class, playerLoginEvent -> {
            playerLoginEvent.setSpawningInstance(instance);
        });

        globalEventHandler.addListener(PlayerSpawnEvent.class, playerSpawnEvent -> {
            playerSpawnEvent.getPlayer().setGameMode(GameMode.SPECTATOR);
            playerSpawnEvent.getPlayer().sendPacket(entityPacket);
            playerSpawnEvent.getPlayer().sendPacket(cameraPacket);
            playerSpawnEvent.getPlayer().sendMessage(limboWelcome());
        });

        globalEventHandler.addListener(PlayerChatEvent.class, playerChatEvent -> {
            playerChatEvent.setCancelled(true);
        });

        //region art
        System.out.println("\n");
        System.out.println("\n" +
                "\n" +
                "______                        _   _            _                \n" +
                "|  ___|                      | | | |          (_)               \n" +
                "| |_ _ __ ___ _______ _ __   | |_| | ___  _ __ _ _______  _ __  \n" +
                "|  _| '__/ _ \\_  / _ \\ '_ \\  |  _  |/ _ \\| '__| |_  / _ \\| '_ \\ \n" +
                "| | | | | (_) / /  __/ | | | | | | | (_) | |  | |/ / (_) | | | |\n" +
                "\\_| |_|  \\___/___\\___|_| |_| \\_| |_/\\___/|_|  |_/___\\___/|_| |_|\n" +
                "                                                                \n" +
                "                                                                \n" +
                "\n");
        System.out.println("\n");
        System.out.println("The Frozen Horizon was a place of beauty and mystery, a place of fantasy and imagination.");
        System.out.println("It was a place where one could explore and discover new wonders, and where dreams could come true");
        System.out.println("\n");
        System.out.println("====== VERSIONS ======");
        System.out.println("Java: " + Runtime.version());
        System.out.println("Name: " + MinecraftServer.getBrandName());
        System.out.println("Version: " + MinecraftServer.VERSION_NAME);
        System.out.println("======================");
        System.out.println("\n");
        //endregion

        final var onlineMode = Boolean.parseBoolean(properties.getProperty("online-mode"));
        final var address = properties.getProperty("address");
        final var port = Integer.parseInt(properties.getProperty("port"));
        final var compressionThreshold = Integer.parseInt(properties.getProperty("compression-threshold"));

        final var proxy = properties.getProperty("proxy").toLowerCase();
        final var proxySecret = properties.getProperty("proxy-secret");

        switch (proxy) {
            case "velocity" -> {
                VelocityProxy.enable(proxySecret);
                logger.info("Enabling velocity forwarding");
            }
            case "bungee" -> {
                BungeeCordProxy.enable();
                logger.info("Enabling bungee forwarding");
            }
        }

        if (onlineMode) {
            logger.info("Starting server with online mode enabled!");
            MojangAuth.init();
        }

        MinecraftServer.setCompressionThreshold(compressionThreshold);

        server.start(address, port);
    }

    public static Properties loadProperties() {
        var properties = new Properties();
        try {
            if (Files.exists(propertiesPath)) {
                properties.load(Files.newInputStream(propertiesPath));
            } else {
                var inputStream = Main.class.getClassLoader().getResourceAsStream("server.properties");
                properties.load(inputStream);
                properties.store(Files.newOutputStream(propertiesPath), "Frozen Horizon " + MinecraftServer.VERSION_NAME);
            }
        } catch (IOException e) {
            MinecraftServer.getExceptionManager().handleException(e);
        }
        return properties;
    }

    private static net.kyori.adventure.text.TextComponent limboWelcome() {
        final TextComponent textComponent2 = Component.text()
                .color(TextColor.color(0xF9F2ED))
                .append(Component.text().content("The server is currently full and you have been placed in a queue. "))
                .append(Component.text("\nPurchase ").color(TextColor.color(0xFFB562)))
                .append(Component.text("FAN ").color(TextColor.color(0x3AB0FF)).decoration(TextDecoration.BOLD, true))
                .append(Component.text("to skip the queue at ").color(TextColor.color(0xFFB562)))
                .append(Component.text("store.survivalquest.net")
                        .color(TextColor.color(0x3AB0FF))
                        .decoration(TextDecoration.UNDERLINED, true)
                        .clickEvent(ClickEvent.openUrl("https://store.survivalquest.net")))
                .build();
        return textComponent2;
    }
}