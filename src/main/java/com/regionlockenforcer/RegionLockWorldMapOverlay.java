package com.regionlockenforcer;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.event.MouseEvent;
import java.util.HashSet;
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

        boolean editing = plugin.isEditing();
        int gridSize = getGridSize();
        boolean hasInnerTiles = !currentProfile.getInnerTiles().isEmpty();

        if (editing)
        {
            drawChunkGrid(graphics, gridSize);
            drawBorderTiles(graphics);
            return null;
        }

        if (hasInnerTiles && config.displayBorderOnWorldMap())
        {
            drawBorderTiles(graphics);
        }

        return null;
    }

    /**
     * Draw a grid showing 64x64 chunks on the world map.
     * Based on region-locker plugin implementation.
     * Uses WorldMap API directly for proper coordinate conversion.
     */
    private void drawChunkGrid(Graphics2D graphics, int gridSize)
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

        // Get current border profile to check for selected chunks
        Region currentProfile = plugin.getCurrentRegion();
        boolean hasSelectedAreas = currentProfile != null && !currentProfile.getBoundaryTiles().isEmpty();

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
                if (hasSelectedAreas)
                {
                    if (currentProfile.isAreaFullyContained(x, y, 0, gridSize))
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
    private void drawBorderTiles(Graphics2D graphics)
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

        Region currentProfile = plugin.getCurrentRegion();
        if (currentProfile == null) return;

        // Determine which tiles to show and what color to use
        Set<WorldPoint> tilesToShow;
        Color tileColor;
        boolean isEditing = plugin.isEditing();
        boolean hasInnerTiles = !currentProfile.getInnerTiles().isEmpty();
        
        if (isEditing)
        {
            // In edit mode: show marked boundary tiles in editing color
            tilesToShow = currentProfile.getBoundaryTiles();
            tileColor = config.editingColor();
        }
        else if (hasInnerTiles)
        {
            // Finished border: show boundary tiles in config color
            tilesToShow = currentProfile.getBoundaryTiles();
            tileColor = config.borderColor();
        }
        else
        {
            // No tiles to show
            return;
        }

        Set<WorldPoint> tilesToDraw = new HashSet<>(tilesToShow);
        if (tilesToDraw.isEmpty()) return;

        int widthInTiles = (int) Math.ceil(worldMapRect.getWidth() / pixelsPerTile);
        int heightInTiles = (int) Math.ceil(worldMapRect.getHeight() / pixelsPerTile);
        
        int yTileMin = worldMapPosition.getY() - heightInTiles / 2;
        int xTileMin = worldMapPosition.getX() - widthInTiles / 2;
        int xTileMax = worldMapPosition.getX() + widthInTiles / 2;
        int yTileMax = worldMapPosition.getY() + heightInTiles / 2;

        // Draw each tile
        graphics.setColor(tileColor);
        int tilePixelSize = Math.max(1, (int) pixelsPerTile); // At least 1 pixel per tile
        
        for (WorldPoint tile : tilesToDraw)
        {
            // Only draw tiles on plane 0 (surface) for now
            if (tile.getPlane() != 0) continue;
            
            int tileX = tile.getX();
            int tileY = tile.getY();
            
            // Check if tile is visible in current view
            if (tileX < xTileMin || tileX > xTileMax || tileY < yTileMin || tileY > yTileMax)
            {
                continue;
            }
            
            // Calculate position on map using same approach as chunk grid
            int yTileOffset = -(yTileMin - tileY);
            int xTileOffset = tileX + widthInTiles / 2 - (int)worldMapPosition.getX();
            
            int xPos = ((int) (xTileOffset * pixelsPerTile)) + (int) worldMapRect.getX();
            int yPos = (worldMapRect.height - (int) (yTileOffset * pixelsPerTile)) + (int) worldMapRect.getY();
            // Adjust Y position to match chunk grid coordinate system
            yPos -= (int) pixelsPerTile;
            
            // Draw tile as a small rectangle
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

        if (plugin == null || !plugin.isEditing() || plugin.getCurrentRegion() == null)
        {
            return false;
        }

        final int tileGroupSize = getGridSize();

        // Store mouse point for use on client thread
        Point mousePoint = e.getPoint();
        final Point finalMousePoint = new Point(mousePoint);

        // All widget access must be on client thread
        // Use a wrapper to capture the result from invoke()
        final boolean[] shouldHandle = {false};

        clientThread.invoke(() ->
        {
            // Check if world map is open
            Widget map = client.getWidget(ComponentID.WORLD_MAP_MAPVIEW);
            if (map == null || map.isHidden())
            {
                return;
            }

            Rectangle mapBounds = map.getBounds();
            if (mapBounds == null)
            {
                return;
            }

            // Check if click is within map bounds
            if (!mapBounds.contains(finalMousePoint))
            {
                return;
            }

            // Convert mouse coordinates to world coordinates using WorldMap API
            WorldMap worldMap = client.getWorldMap();
            if (worldMap == null)
            {
                return;
            }

            float pixelsPerTile = worldMap.getWorldMapZoom();
            net.runelite.api.Point worldMapPosition = worldMap.getWorldMapPosition();
            
            // Get relative point within map bounds
            Point relativePoint = new Point(finalMousePoint.x - mapBounds.x, finalMousePoint.y - mapBounds.y);
            
            // Calculate world coordinates from mouse position
            int widthInTiles = (int) Math.ceil(mapBounds.getWidth() / pixelsPerTile);
            int heightInTiles = (int) Math.ceil(mapBounds.getHeight() / pixelsPerTile);
            
            int xTileOffset = (int) (relativePoint.x / pixelsPerTile) - widthInTiles / 2;
            int yTileOffset = heightInTiles / 2 - (int) (relativePoint.y / pixelsPerTile);
            
            int worldX = (int)worldMapPosition.getX() + xTileOffset;
            int worldY = (int)worldMapPosition.getY() + yTileOffset;
            
            WorldPoint worldPoint = new WorldPoint(worldX, worldY, 0);

            // Convert to chunk coordinates
            int baseX = floorToMultiple(worldPoint.getX(), tileGroupSize);
            int baseY = floorToMultiple(worldPoint.getY(), tileGroupSize);
            int plane = worldPoint.getPlane();

            Region currentProfile = plugin.getCurrentRegion();
            if (currentProfile == null)
            {
                return;
            }

            // Toggle chunk: add if not fully contained, remove if fully contained
            if (currentProfile.isAreaFullyContained(baseX, baseY, plane, tileGroupSize))
            {
                currentProfile.removeArea(baseX, baseY, plane, tileGroupSize);
            }
            else
            {
                currentProfile.addArea(baseX, baseY, plane, tileGroupSize);
            }

            plugin.saveRegions();
            plugin.notifyRegionsChanged();

            shouldHandle[0] = true;
        });

        return shouldHandle[0];
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

