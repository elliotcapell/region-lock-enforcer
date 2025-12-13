package com.regionlockenforcer;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.event.MouseEvent;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.widgets.ComponentID;
import net.runelite.api.widgets.Widget;
import net.runelite.api.worldmap.WorldMap;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;

/**
 * World map overlay for drawing chunk grid and handling chunk selection.
 * Based on approach used in region-locker plugin.
 * Uses WorldMap API directly for proper coordinate conversion.
 */
@Singleton
public class RegionLockWorldMapOverlay extends Overlay
{
    private static final int DEFAULT_GRID_SIZE = 64;

    private final Client client;
    private final ClientThread clientThread;
    private final RegionLockEnforcerConfig config;
    private RegionLockEnforcerPlugin plugin;
    private static final Color DEFAULT_BORDER_COLOR = new Color(255, 255, 0, 220);
    private static final Color DEFAULT_EDIT_COLOR = new Color(255, 0, 0, 120);

    @Inject
    private RegionLockWorldMapOverlay(Client client, ClientThread clientThread, RegionLockEnforcerConfig config)
    {
        this.client = client;
        this.clientThread = clientThread;
        this.config = config;
        setPosition(OverlayPosition.DYNAMIC);
        setPriority(OverlayPriority.HIGHEST);
        setLayer(OverlayLayer.ALWAYS_ON_TOP);
    }

    public void setPlugin(RegionLockEnforcerPlugin plugin)
    {
        this.plugin = plugin;
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        if (plugin == null)
        {
            return null;
        }

        Region currentProfile = plugin.getCurrentRegion();
        if (currentProfile == null)
        {
            return null;
        }

        if (config.disableBorder())
        {
            return null;
        }

        Border activeBorder = plugin.getActiveBorder();
        boolean editing = plugin.isEditing();
        int gridSize = getGridSize();
        boolean hasInnerTiles = currentProfile.getBorders().stream().anyMatch(b -> !b.getInnerTiles().isEmpty());

        if (editing)
        {
            drawChunkGrid(graphics, gridSize, activeBorder);
            drawEditingTiles(graphics, currentProfile, activeBorder);
            return null;
        }

        if (hasInnerTiles && config.displayBorderOnWorldMap())
        {
            drawFinishedTiles(graphics, currentProfile);
        }

        return null;
    }

    /**
     * Draw a grid showing 64x64 chunks on the world map.
     * Based on region-locker plugin implementation.
     * Uses WorldMap API directly for proper coordinate conversion.
     */
    private void drawChunkGrid(Graphics2D graphics, int gridSize, Border activeBorder)
    {
        WorldMapRenderContext context = buildWorldMapRenderContext();
        if (context == null)
        {
            return;
        }

        Rectangle worldMapRect = context.getMapBounds();
        float pixelsPerTile = context.getPixelsPerTile();
        net.runelite.api.Point worldMapPosition = context.getWorldMapPosition();

        graphics.setClip(worldMapRect);

        int widthInTiles = (int) Math.ceil(worldMapRect.getWidth() / pixelsPerTile);
        int heightInTiles = (int) Math.ceil(worldMapRect.getHeight() / pixelsPerTile);

        // Offset in tiles from anchor sides
        int yTileMin = worldMapPosition.getY() - heightInTiles / 2;
        int xRegionMin = floorToMultiple((int)worldMapPosition.getX() - widthInTiles / 2, gridSize);
        int xRegionMax = floorToMultiple((int)worldMapPosition.getX() + widthInTiles / 2, gridSize) + gridSize;
        int yRegionMin = floorToMultiple(yTileMin, gridSize);
        int yRegionMax = floorToMultiple((int)worldMapPosition.getY() + heightInTiles / 2, gridSize) + gridSize;
        int regionPixelSize = (int) Math.ceil(gridSize * pixelsPerTile);

        Stroke oldStroke = graphics.getStroke();
        graphics.setStroke(new BasicStroke(1.0f));
        graphics.setColor(Color.BLACK);

        boolean hasSelectedAreas = activeBorder != null && !activeBorder.getBoundaryTiles().isEmpty();

        // Draw chunk grid lines and fill selected chunks
        for (int x = xRegionMin; x < xRegionMax; x += gridSize)
        {
            for (int y = yRegionMin; y < yRegionMax; y += gridSize)
            {
                int yTileOffset = -(yTileMin - y);
                int xTileOffset = x + widthInTiles / 2 - (int)worldMapPosition.getX();

                int xPos = ((int) (xTileOffset * pixelsPerTile)) + (int) worldMapRect.getX();
                int yPos = (worldMapRect.height - (int) (yTileOffset * pixelsPerTile)) + (int) worldMapRect.getY();
                // Offset y-position by a single region to correct for drawRect starting from the top
                yPos -= regionPixelSize;

                // Check if this chunk is fully selected
                if (hasSelectedAreas && activeBorder != null)
                {
                    if (activeBorder.isAreaFullyContained(x, y, 0, gridSize))
                    {
                        // Fill with semi-transparent dark grey/black
                        graphics.setColor(new Color(20, 20, 20, 140));
                        graphics.fillRect(xPos, yPos, regionPixelSize, regionPixelSize);
                    }
                }

                // Draw chunk border
                graphics.setColor(Color.BLACK);
                graphics.drawRect(xPos, yPos, regionPixelSize, regionPixelSize);
            }
        }
        graphics.setStroke(oldStroke);
    }

    /**
     * Draw individual border tiles on the world map.
     * Shows finished border (innerTiles) or marked tiles when in edit mode.
     */
    private void drawEditingTiles(Graphics2D graphics, Region currentProfile, Border activeBorder)
    {
        WorldMapRenderContext context = buildWorldMapRenderContext();
        if (context == null)
        {
            return;
        }

        if (activeBorder == null)
        {
            return;
        }

            drawTiles(graphics, context, activeBorder.getBoundaryTiles(), DEFAULT_EDIT_COLOR);
    }

    private void drawFinishedTiles(Graphics2D graphics, Region currentProfile)
    {
        WorldMapRenderContext context = buildWorldMapRenderContext();
        if (context == null)
        {
            return;
        }

        for (Border border : currentProfile.getBorders())
        {
            if (border.getInnerTiles().isEmpty())
            {
                continue;
            }
                drawTiles(graphics, context, border.getBoundaryTiles(), DEFAULT_BORDER_COLOR);
        }
    }

    private void drawTiles(Graphics2D graphics, WorldMapRenderContext context, Set<WorldPoint> tilesToDraw, Color tileColor)
    {
        if (tilesToDraw == null || tilesToDraw.isEmpty())
        {
            return;
        }

        Rectangle worldMapRect = context.getMapBounds();
        float pixelsPerTile = context.getPixelsPerTile();
        net.runelite.api.Point worldMapPosition = context.getWorldMapPosition();

        graphics.setClip(worldMapRect);

        int widthInTiles = (int) Math.ceil(worldMapRect.getWidth() / pixelsPerTile);
        int heightInTiles = (int) Math.ceil(worldMapRect.getHeight() / pixelsPerTile);

        int yTileMin = worldMapPosition.getY() - heightInTiles / 2;
        int xTileMin = worldMapPosition.getX() - widthInTiles / 2;
        int xTileMax = worldMapPosition.getX() + widthInTiles / 2;
        int yTileMax = worldMapPosition.getY() + heightInTiles / 2;

        graphics.setColor(tileColor);
        int tilePixelSize = Math.max(1, (int) pixelsPerTile);

        for (WorldPoint tile : tilesToDraw)
        {
            if (tile.getPlane() != 0) continue;

            int tileX = tile.getX();
            int tileY = tile.getY();

            if (tileX < xTileMin || tileX > xTileMax || tileY < yTileMin || tileY > yTileMax)
            {
                continue;
            }

            int yTileOffset = -(yTileMin - tileY);
            int xTileOffset = tileX + widthInTiles / 2 - (int)worldMapPosition.getX();

            int xPos = ((int) (xTileOffset * pixelsPerTile)) + (int) worldMapRect.getX();
            int yPos = (worldMapRect.height - (int) (yTileOffset * pixelsPerTile)) + (int) worldMapRect.getY();
            yPos -= (int) pixelsPerTile;

            graphics.fillRect(xPos, yPos, tilePixelSize, tilePixelSize);
        }
    }

    /**
     * Handle mouse clicks on the world map.
     * Called from the plugin when a click is detected.
     * Must handle widget access on client thread.
     */
    public boolean handleWorldMapClick(MouseEvent e)
    {
        // Only handle left clicks with Shift key when in editing mode
        if (e.getButton() != MouseEvent.BUTTON1)
        {
            return false;
        }
        
        // Require Shift key to be pressed
        if ((e.getModifiersEx() & MouseEvent.SHIFT_DOWN_MASK) == 0)
        {
            return false;
        }

        Border activeBorder = plugin.getActiveBorder();
        if (plugin == null || !plugin.isEditing() || plugin.getCurrentRegion() == null || activeBorder == null)
        {
            return false;
        }

        final int tileGroupSize = getGridSize();

        // All widget access must be on client thread
        // Use a wrapper to capture the result from invoke()
        final boolean[] shouldHandle = {false};

        clientThread.invoke(() ->
        {
            Point clickPoint = getMouseCanvasPoint(e);
            if (clickPoint == null)
            {
                return;
            }

            WorldMapRenderContext context = buildWorldMapRenderContext();
            if (context == null)
            {
                return;
            }

            Rectangle mapBounds = context.getMapBounds();
            if (mapBounds == null || !mapBounds.contains(clickPoint))
            {
                return;
            }

            WorldPoint worldPoint = screenToWorldPoint(clickPoint, context);
            if (worldPoint == null)
            {
                return;
            }

            // Convert to chunk coordinates
            int baseX = floorToMultiple(worldPoint.getX(), tileGroupSize);
            int baseY = floorToMultiple(worldPoint.getY(), tileGroupSize);
            int plane = worldPoint.getPlane();

            Region currentProfile = plugin.getCurrentRegion();
            Border border = plugin.getActiveBorder();
            if (currentProfile == null || border == null)
            {
                return;
            }

            boolean areaHasTiles = border.hasAnyTileInArea(baseX, baseY, plane, tileGroupSize);

            if (areaHasTiles)
            {
                border.removeArea(baseX, baseY, plane, tileGroupSize);
            }
            else
            {
                border.addArea(baseX, baseY, plane, tileGroupSize);
            }

            currentProfile.invalidateClickableTilesCache();
            plugin.markUnsavedEdits();

            shouldHandle[0] = true;
        });

        return shouldHandle[0];
    }

    /**
     * Use the client's mouse canvas position when available so Stretched Mode
     * scaling is taken into account. Falls back to the raw event point.
     */
    private Point getMouseCanvasPoint(MouseEvent e)
    {
        net.runelite.api.Point mouseCanvasPos = client.getMouseCanvasPosition();
        if (mouseCanvasPos != null)
        {
            return new Point(mouseCanvasPos.getX(), mouseCanvasPos.getY());
        }
        return e != null ? e.getPoint() : null;
    }

    private WorldMapRenderContext buildWorldMapRenderContext()
    {
        Widget map = client.getWidget(ComponentID.WORLD_MAP_MAPVIEW);
        if (map == null)
        {
            return null;
        }

        if (map.isHidden())
        {
            return null;
        }

        Rectangle mapBounds = map.getBounds();
        if (mapBounds == null)
        {
            return null;
        }

        WorldMap worldMap = client.getWorldMap();
        if (worldMap == null)
        {
            return null;
        }

        net.runelite.api.Point worldMapPosition = worldMap.getWorldMapPosition();
        if (worldMapPosition == null)
        {
            return null;
        }

        float pixelsPerTile = worldMap.getWorldMapZoom();
        if (pixelsPerTile <= 0)
        {
            return null;
        }

        return new WorldMapRenderContext(mapBounds, pixelsPerTile, worldMapPosition);
    }

    private WorldPoint screenToWorldPoint(Point screenPoint, WorldMapRenderContext context)
    {
        Rectangle mapBounds = context.getMapBounds();
        float pixelsPerTile = context.getPixelsPerTile();
        net.runelite.api.Point worldMapPosition = context.getWorldMapPosition();

        if (pixelsPerTile <= 0 || mapBounds == null || worldMapPosition == null)
        {
            return null;
        }

        int widthInTiles = (int)Math.ceil(mapBounds.getWidth() / pixelsPerTile);
        int heightInTiles = (int)Math.ceil(mapBounds.getHeight() / pixelsPerTile);

        double xTileOffset = (screenPoint.x - mapBounds.getX()) / pixelsPerTile;
        double yTileOffsetFromTop = (mapBounds.getHeight() - 1 - (screenPoint.y - mapBounds.getY())) / pixelsPerTile;

        int xIndex = clamp((int)Math.floor(xTileOffset), 0, Math.max(0, widthInTiles - 1));
        int yIndex = clamp((int)Math.floor(yTileOffsetFromTop), 0, Math.max(0, heightInTiles - 1));

        int xTileMin = (int)worldMapPosition.getX() - widthInTiles / 2;
        int yTileMin = worldMapPosition.getY() - heightInTiles / 2;

        int worldX = xTileMin + xIndex;
        int worldY = yTileMin + yIndex;

        return new WorldPoint(worldX, worldY, 0);
    }

    private int clamp(int value, int min, int max)
    {
        if (value < min)
        {
            return min;
        }
        if (value > max)
        {
            return max;
        }
        return value;
    }

    private int getGridSize()
    {
        int size = config.worldMapGridSize();
        if (size < 1)
        {
            return 1;
        }
        if (size > 64)
        {
            return DEFAULT_GRID_SIZE;
        }
        return size;
    }

    private int floorToMultiple(int value, int multiple)
    {
        if (multiple <= 0)
        {
            return value;
        }

        return Math.floorDiv(value, multiple) * multiple;
    }

    private static final class WorldMapRenderContext
    {
        private final Rectangle mapBounds;
        private final float pixelsPerTile;
        private final net.runelite.api.Point worldMapPosition;

        private WorldMapRenderContext(Rectangle mapBounds, float pixelsPerTile, net.runelite.api.Point worldMapPosition)
        {
            this.mapBounds = mapBounds;
            this.pixelsPerTile = pixelsPerTile;
            this.worldMapPosition = worldMapPosition;
        }

        private Rectangle getMapBounds()
        {
            return mapBounds;
        }

        private float getPixelsPerTile()
        {
            return pixelsPerTile;
        }

        private net.runelite.api.Point getWorldMapPosition()
        {
            return worldMapPosition;
        }
    }
}

