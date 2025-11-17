package com.regionlockenforcer;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Stroke;
import java.util.HashSet;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;

import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Perspective;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;

@Singleton
public class RegionLockOverlay extends Overlay
{
    private final Client client;
    private final RegionLockEnforcerConfig config;
    RegionLockEnforcerPlugin plugin; // set by plugin.startUp()

    @Inject
    public RegionLockOverlay(Client client, RegionLockEnforcerConfig config)
    {
        this.client = client;
        this.config = config;

        setPosition(OverlayPosition.DYNAMIC);
        setPriority(OverlayPriority.HIGHEST); // deprecation warning is OK
        setLayer(OverlayLayer.ABOVE_SCENE);
    }

    public void setPlugin(RegionLockEnforcerPlugin p) { this.plugin = p; }

    @Override
    public Dimension render(Graphics2D g)
    {
        if (config.disableBorder() || plugin == null) return null;
        if (client.getGameState() != GameState.LOGGED_IN || client.getLocalPlayer() == null) return null;

        Set<WorldPoint> markedTiles = new HashSet<>(plugin.getCurrentBorderTiles());
        com.regionlockenforcer.BorderProfile currentProfile = plugin.getCurrentBorderProfile();
        boolean hasInnerTiles = currentProfile != null && !currentProfile.getInnerTiles().isEmpty();
        
        if (hasInnerTiles)
        {
            // Draw border lines along inner edges when inner tiles are computed
            if (!markedTiles.isEmpty())
            {
                drawBorderLines(g, markedTiles);
            }
        }
        else
        {
            // Show marked tiles with editing color when inner tiles are not computed (edit mode)
            for (WorldPoint wp : markedTiles)
            {
                // Convert the world tile to local/canvas
                LocalPoint lp = LocalPoint.fromWorld(client, wp);
                if (lp == null) continue;

                // Fill the tile with editing color
                Polygon tilePoly = Perspective.getCanvasTilePoly(client, lp);
                if (tilePoly != null)
                {
                    Composite old = g.getComposite();
                    g.setComposite(AlphaComposite.SrcOver.derive(0.7f));
                    g.setColor(config.editingColor());
                    g.fill(tilePoly);
                    g.setComposite(old);
                }
            }
        }

        // Show editing mode indicator - above player's head
        if (plugin.isEditing())
        {
            net.runelite.api.Player localPlayer = client.getLocalPlayer();
            if (localPlayer != null)
            {
                LocalPoint playerLocalPoint = localPlayer.getLocalLocation();
                if (playerLocalPoint != null)
                {
                    // Get the plane from the player's world point
                    net.runelite.api.coords.WorldPoint playerWorldPoint = localPlayer.getWorldLocation();
                    int plane = playerWorldPoint != null ? playerWorldPoint.getPlane() : 0;
                    
                    // Get the canvas position of the player
                    net.runelite.api.Point playerPoint = Perspective.localToCanvas(client, playerLocalPoint, 
                        plane, localPlayer.getLogicalHeight() + 20); // Offset above head
                    
                    if (playerPoint != null)
                    {
                        // Use larger font
                        java.awt.Font oldFont = g.getFont();
                        g.setFont(oldFont);
                        
                        FontMetrics fm = g.getFontMetrics();
                        String toggleKey = plugin.getToggleEditorKeybindString();
                        String text = "Press " + toggleKey + " to Toggle Editing";
                        int textWidth = fm.stringWidth(text);
                        
                        // Center text horizontally above player's head
                        int textX = playerPoint.getX() - (textWidth / 2);
                        int textY = playerPoint.getY() - 10; // Offset above head
                        
                        // Draw text with shadow for better visibility
                        g.setColor(Color.BLACK);
                        g.drawString(text, textX + 1, textY + 1); // Shadow
                        g.setColor(Color.WHITE);
                        g.drawString(text, textX, textY); // Main text
                        
                        g.setFont(oldFont);
                    }
                }
            }
        }

        return null;
    }

    /**
     * Draw lines along the outer edges of boundary tiles.
     * An edge is "outer" if:
     * 1. It belongs to a boundary tile (in markedTiles)
     * 2. The neighbor tile across that edge is outside (not in boundaryTiles AND not in innerTiles)
     */
    private void drawBorderLines(Graphics2D g, Set<WorldPoint> markedTiles)
    {
        if (markedTiles.isEmpty()) return;

        // Get the inner tiles from the current profile
        com.regionlockenforcer.BorderProfile currentProfile = plugin.getCurrentBorderProfile();
        if (currentProfile == null) return;
        
        Set<WorldPoint> innerTiles = currentProfile.getInnerTiles();
        if (innerTiles == null || innerTiles.isEmpty()) 
        {
            // If inner tiles are empty, we can't determine which edges are outer
            // This shouldn't happen if computeInnerTiles was called, but handle gracefully
            return;
        }

        // Get plane from first marked tile
        int plane = -1;
        for (WorldPoint wp : markedTiles)
        {
            plane = wp.getPlane();
            break;
        }
        if (plane == -1) return;

        Stroke oldStroke = g.getStroke();
        g.setStroke(new BasicStroke(2.0f));
        g.setColor(config.borderColor()); // Configurable border color

        for (WorldPoint wp : markedTiles)
        {
            LocalPoint lp = LocalPoint.fromWorld(client, wp);
            if (lp == null) continue;

            int x = wp.getX();
            int y = wp.getY();

            // Check each of the 4 directions (N, S, E, W)
            // An edge is "outer" if the neighbor across that edge is outside
            // (not in boundaryTiles AND not in innerTiles)
            
            // North edge
            WorldPoint northNeighbor = new WorldPoint(x, y + 1, plane);
            if (isOutsideTile(northNeighbor, markedTiles, innerTiles))
            {
                drawEdgeLine(g, wp, "north");
            }

            // South edge
            WorldPoint southNeighbor = new WorldPoint(x, y - 1, plane);
            if (isOutsideTile(southNeighbor, markedTiles, innerTiles))
            {
                drawEdgeLine(g, wp, "south");
            }

            // East edge
            WorldPoint eastNeighbor = new WorldPoint(x + 1, y, plane);
            if (isOutsideTile(eastNeighbor, markedTiles, innerTiles))
            {
                drawEdgeLine(g, wp, "east");
            }

            // West edge
            WorldPoint westNeighbor = new WorldPoint(x - 1, y, plane);
            if (isOutsideTile(westNeighbor, markedTiles, innerTiles))
            {
                drawEdgeLine(g, wp, "west");
            }
        }

        g.setStroke(oldStroke);
    }

    /**
     * Check if a tile is outside the region.
     * A tile is outside if it's neither a boundary tile nor an inner tile.
     */
    private boolean isOutsideTile(WorldPoint tile, Set<WorldPoint> boundaryTiles, Set<WorldPoint> innerTiles)
    {
        return !boundaryTiles.contains(tile) && !innerTiles.contains(tile);
    }

    /**
     * Draw a line along a specific edge of a tile.
     * Uses world coordinates to prevent rotation with camera.
     * Similar to the west edge drawing approach that doesn't rotate.
     */
    private void drawEdgeLine(Graphics2D g, WorldPoint wp, String direction)
    {
        LocalPoint lp = LocalPoint.fromWorld(client, wp);
        if (lp == null) return;

        final int half = 64; // half tile in local units (tile size = 128)
        int plane = wp.getPlane();
        
        // Calculate corner LocalPoints using offsets from center (same approach as west edge)
        // These offsets are in world space and won't rotate with camera
        LocalPoint swLocal = new LocalPoint((int)(lp.getX() - half), (int)(lp.getY() - half));
        LocalPoint seLocal = new LocalPoint((int)(lp.getX() + half), (int)(lp.getY() - half));
        LocalPoint neLocal = new LocalPoint((int)(lp.getX() + half), (int)(lp.getY() + half));
        LocalPoint nwLocal = new LocalPoint((int)(lp.getX() - half), (int)(lp.getY() + half));

        net.runelite.api.Point p1 = null;
        net.runelite.api.Point p2 = null;

        switch (direction.toLowerCase())
        {
            case "north":
                // North edge: from NE corner to NW corner
                p1 = Perspective.localToCanvas(client, neLocal, plane);
                p2 = Perspective.localToCanvas(client, nwLocal, plane);
                break;
            case "south":
                // South edge: from SE corner to SW corner
                p1 = Perspective.localToCanvas(client, seLocal, plane);
                p2 = Perspective.localToCanvas(client, swLocal, plane);
                break;
            case "east":
                // East edge: from NE corner to SE corner
                p1 = Perspective.localToCanvas(client, neLocal, plane);
                p2 = Perspective.localToCanvas(client, seLocal, plane);
                break;
            case "west":
                // West edge: from NW corner to SW corner
                p1 = Perspective.localToCanvas(client, nwLocal, plane);
                p2 = Perspective.localToCanvas(client, swLocal, plane);
                break;
        }

        if (p1 != null && p2 != null)
        {
            // Only draw if both points are valid (on-screen)
            // If either point is null, the edge is off-screen and we skip it
            g.drawLine(p1.getX(), p1.getY(), p2.getX(), p2.getY());
        }
        // Note: If p1 or p2 is null, the edge is off-screen and we skip drawing it
        // This is expected behavior - we only draw visible edges
    }
}
