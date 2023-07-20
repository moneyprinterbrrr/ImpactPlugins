package com.impact.Firemaker;

import com.example.EthanApiPlugin.Collections.Bank;
import com.example.EthanApiPlugin.Collections.Inventory;
import com.example.EthanApiPlugin.Collections.TileObjects;
import com.example.EthanApiPlugin.Collections.query.TileObjectQuery;
import com.example.EthanApiPlugin.EthanApiPlugin;
import com.example.InteractionApi.BankInteraction;
import com.example.InteractionApi.TileObjectInteraction;
import com.example.PacketUtils.PacketUtilsPlugin;
import com.example.Packets.MousePackets;
import com.example.Packets.MovementPackets;
import com.example.Packets.WidgetPackets;
import com.google.inject.Provides;
import lombok.SneakyThrows;
import net.runelite.api.*;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameTick;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.input.KeyManager;
import net.runelite.client.plugins.*;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.HotkeyListener;

import javax.inject.Inject;
import javax.swing.*;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

@PluginDependency(EthanApiPlugin.class)
@PluginDependency(PacketUtilsPlugin.class)
@PluginDescriptor(
        name = "<html><font color=\"#fa5555\">Firemaker</font></html>",
        description = "Burns logs at Varrock for efficiency",
        tags = {"ethan", "skilling"}
)
public class FiremakerPlugin extends Plugin {
    @Inject
    private Client client;

    @Inject
    private FiremakerConfig config;

    @Inject
    private KeyManager keyManager;

    @Inject
    private OverlayManager overlayManager;

    @Inject
    private FiremakerOverlay overlay;

    @Inject
    PluginManager pluginManager;

    State state;
    boolean started;
    int tickDelay;
    private WorldPoint burnStartPoint;
    private int fireLane;

    private enum State {
        WALK_VARROCK,
        TELEPORT_VARROCK, // not implemented
        ANIMATING,
        BURN,
        BANK
    }

    @Override
    @SneakyThrows
    protected void startUp() throws Exception {
        if (client.getRevision() != PacketUtilsPlugin.CLIENT_REV) {
            SwingUtilities.invokeLater(() ->
            {
                try {
                    pluginManager.setPluginEnabled(this, false);
                    pluginManager.stopPlugin(this);
                } catch (PluginInstantiationException ignored) {
                }
            });
            return;
        }
        keyManager.registerKeyListener(toggle);
        overlayManager.add(overlay);
    }

    @Override
    protected void shutDown() throws Exception {
        keyManager.unregisterKeyListener(toggle);
        overlayManager.remove(overlay);
    }

    @Provides
    private FiremakerConfig getConfig(ConfigManager configManager) {
        return configManager.getConfig(FiremakerConfig.class);
    }

    @Subscribe
    private void onGameTick(GameTick event) {
        if (!EthanApiPlugin.loggedIn() || !started) {
            // We do an early return if the user isn't logged in
            return;
        }

        State nextState = getNextState();
        if (nextState == state) {
            handleState();
            return;
        }

        if (tickDelay > 0) {
            tickDelay--;
            return;
        }

        state = nextState;
        // TODO: add utils with gaussian delay w/ values config
        tickDelay = ThreadLocalRandom.current().nextInt(0, 3); // reset delay
    }

    private void handleState() {
        switch (state) {
            case BANK:
                restockLogs();
                break;
            case WALK_VARROCK:
                walkVarrock();
                break;
            case BURN:
                burnLogs();
                break;
        }
    }

    private State getNextState() { // Get current state, `state` field is previous state really
        if (EthanApiPlugin.isMoving() || client.getLocalPlayer().getAnimation() != -1) {
            // this is to prevent clicks while animating/moving.
            return State.ANIMATING;
        }

        // TODO: breaks on last remaining logs inventory, inventory is not full
        if (Inventory.full() && !atBurnLocation()) {
            // TODO: return config.useTeleport() ? State.TELEPORT_VARROCK : State.WALK_VARROCK;
            return State.WALK_VARROCK;
        }

        if (!outOfLogs()) {
            return State.BURN;
        }

        // always resets/defaults to bank
        // to avoid areas we can't firemake or against a wall
        return State.BANK;
    }

    private void teleportVarrock() {
        // TODO: handle teleport
    }

    private void walkVarrock() {
        int numLanes = 3;
        int nextFireLane = ThreadLocalRandom.current().nextInt(1, numLanes); // start at 1, find new lane
        int xNoise =  ThreadLocalRandom.current().nextInt(0, 4);
        fireLane = (fireLane + nextFireLane) % numLanes;
        // subtract lanes: to go south, add noise: to go east
        burnStartPoint = new WorldPoint(3203 + xNoise,3430 - fireLane,0);

        // TODO: check pathing? PathingTesting.walkTo(burnStartPoint)
        MousePackets.queueClickPacket();
        MovementPackets.queueMovement(burnStartPoint);
    }

    private void burnLogs() {
        Widget tinderbox = Inventory.search().nameContains("Tinderbox").first().get();
        Widget logs = Inventory.search().nameContains(config.itemToBurn()).first().get();

        MousePackets.queueClickPacket();
        MousePackets.queueClickPacket();
        WidgetPackets.queueWidgetOnWidget(tinderbox, logs);
    }

    private void restockLogs() {
        if (Bank.isOpen()) {
            if (!hasTinderbox()) {
                BankInteraction.useItem(x -> x.getName().contains("Tinderbox"), "Withdraw-1");
            }

            Optional<Widget> logsInBank = Bank.search().nameContains(config.itemToBurn()).first();
             if (outOfLogs() && logsInBank.isPresent()) {

                // BankInteraction.withdrawX kept enter amount interface up...
                BankInteraction.useItem(x -> x.getName().contains(config.itemToBurn()), "Withdraw-All");
                return;
            }
        } else {
            // Open bank
            Optional<TileObject> bankBooth = findBank();
            if (bankBooth.isPresent()) {
                MousePackets.queueClickPacket();
                TileObjectInteraction.interact(bankBooth.get(), "Bank");
                // TODO: add utils with gaussian delay w/ values config
                tickDelay = ThreadLocalRandom.current().nextInt(2, 6);
            }
        }
    }

    private Optional<TileObject> findBank() {
        // TODO: EthanAPI.findObject() instead ?
        Optional<TileObject> bankBooth = TileObjects.search().filter(tileObject -> {
            ObjectComposition objectComposition = TileObjectQuery.getObjectComposition(tileObject);
            return getName().contains("Bank") ||
                    Arrays.stream(objectComposition.getActions()).anyMatch(action -> action != null && action.contains("Bank"));
        }).nearestToPlayer();
        return bankBooth;
    }

    private boolean outOfLogs() {
        String itemName = config.itemToBurn();
        return Inventory.search()
                .filter(item -> item.getName().contains(itemName))
                .empty();
    }

    private boolean hasTinderbox() {
        // TODO: can have full bag of tinderboxes... not going to handle specifics
        return Inventory.search()
                .filter(item -> item.getName().contains("Tinderbox"))
                .result().size() > 0;
    }

    private boolean atBurnLocation() {
        return client.getLocalPlayer().getWorldLocation().equals(burnStartPoint);
    }

    private final HotkeyListener toggle = new HotkeyListener(() -> config.toggle()) {
        @Override
        public void hotkeyPressed() {
            toggle();
        }
    };

    public void toggle() {
        if (client.getGameState() != GameState.LOGGED_IN) {
            return;
        }
        started = !started;
        tickDelay = 0;
        state = State.BANK;
        fireLane = 0;
    }
}