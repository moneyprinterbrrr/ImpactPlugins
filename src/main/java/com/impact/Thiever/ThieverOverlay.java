package com.impact.Thiever;

import net.runelite.api.Client;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayMenuEntry;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;
import net.runelite.client.ui.overlay.components.PanelComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.awt.*;

import static net.runelite.api.MenuAction.RUNELITE_OVERLAY_CONFIG;
import static net.runelite.client.ui.overlay.OverlayManager.OPTION_CONFIGURE;

@Singleton
public class ThieverOverlay extends Overlay {
    private Client client;
    private ThieverPlugin plugin;
    private ThieverConfig config;
    private PanelComponent panelComponent = new PanelComponent();

    @Inject
    private ThieverOverlay(Client client, ThieverPlugin plugin, ThieverConfig config)
    {
        this.client = client;
        this.plugin = plugin;
        this.config = config;

        this.setPriority(OverlayPriority.HIGHEST);
        this.setPosition(OverlayPosition.BOTTOM_LEFT);
        this.getMenuEntries().add(new OverlayMenuEntry(RUNELITE_OVERLAY_CONFIG, OPTION_CONFIGURE, "Lava Crafter Overlay"));
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        if (config.disablePaint()) {
            return null;
        }

        if (plugin == null)
            return null;

        if (!plugin.started)
            return null;

        panelComponent.getChildren().clear();

        panelComponent.getChildren().add(TitleComponent.builder()
                .text("Impact Thiever")
                .color(Color.decode("#fa5555"))
                .build());

        if (plugin.state != null)
        {
            panelComponent.getChildren().add(TitleComponent.builder()
                    .text("State: " + plugin.state)
                    .color(Color.ORANGE)
                    .build());
        }

        panelComponent.getChildren().add(TitleComponent.builder()
                .text("Delay: " + plugin.tickDelay)
                .color(Color.ORANGE)
                .build());

        panelComponent.getChildren().add(TitleComponent.builder()
                .text("Pouch open at: " + plugin.numPouchesToOpen)
                .color(Color.ORANGE)
                .build());

        panelComponent.setPreferredSize(new Dimension(175, 130));
        panelComponent.setBackgroundColor(Color.BLACK);

        return panelComponent.render(graphics);
    }
}