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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Comparator;
import javax.inject.Inject;
import javax.inject.Singleton;

import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Model;
import net.runelite.api.RuneLiteObject;
import net.runelite.api.Perspective;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;

@Singleton
public class RegionLockOverlay extends Overlay
{
    private final Client client;
    private final RegionLockEnforcerConfig config;
    private final ClientThread clientThread;
    RegionLockEnforcerPlugin plugin; // set by plugin.startUp()

    private static final Color DEFAULT_BORDER_COLOR = new Color(255, 255, 0, 220);
    private static final Color DEFAULT_EDIT_COLOR = new Color(255, 0, 0, 120);

    private final Map<WorldPoint, List<RuneLiteObject>> propObjects = new HashMap<>();
    private final Set<PlacementInstance> lastPlacements = new HashSet<>();
    private int lastPropHash = 0;

    @Inject
    public RegionLockOverlay(Client client, RegionLockEnforcerConfig config, ClientThread clientThread)
    {
        this.client = client;
        this.config = config;
        this.clientThread = clientThread;

        setPosition(OverlayPosition.DYNAMIC);
        setPriority(OverlayPriority.HIGHEST); // deprecation warning is OK
        setLayer(OverlayLayer.ABOVE_SCENE);
    }

    public void setPlugin(RegionLockEnforcerPlugin p) { this.plugin = p; }

    @Override
    public Dimension render(Graphics2D g)
    {
        if (config.disableBorder() || plugin == null)
        {
            clearPropObjects();
            return null;
        }
        if (client.getGameState() != GameState.LOGGED_IN || client.getLocalPlayer() == null) return null;

        com.regionlockenforcer.Region currentProfile = plugin.getCurrentRegion();
        Border activeBorder = plugin.getActiveBorder();
        boolean editing = plugin.isEditing();
        List<PropPlacement> placements = new ArrayList<>();

        if (currentProfile != null)
        {
            if (editing && activeBorder != null)
            {
                for (WorldPoint wp : activeBorder.getBoundaryTiles())
                {
                    LocalPoint lp = LocalPoint.fromWorld(client, wp);
                    if (lp == null) continue;

                    Polygon tilePoly = Perspective.getCanvasTilePoly(client, lp);
                    if (tilePoly != null)
                    {
                        Composite old = g.getComposite();
                        g.setComposite(AlphaComposite.SrcOver.derive(0.7f));
                        g.setColor(DEFAULT_EDIT_COLOR);
                        g.fill(tilePoly);
                        g.setComposite(old);
                    }
                }
            }

            // Draw finished borders (all of them) without copying into unions
            for (Border border : currentProfile.getBorders())
            {
                if (border.getInnerTiles().isEmpty())
                {
                    continue;
                }
                Border.RenderMode mode = border.getRenderMode();
                if (mode == Border.RenderMode.LINES)
                {
                    drawBorderLines(g, border.getBoundaryTiles(), border.getInnerTiles(), border.getLineColor());
                }
                if (mode == Border.RenderMode.PROPS)
                {
                    Set<WorldPoint> boundary = border.getBoundaryTiles();
                    Set<WorldPoint> inner = border.getInnerTiles();
                    PropDefinition def = getPropDefinition(resolvePropStyle(border));
                    for (WorldPoint wp : boundary)
                    {
                        addPropPlacementsForTile(placements, wp, boundary, inner, def);
                    }
                }
            }

            if (!placements.isEmpty())
            {
                ensurePropObjects(placements);
            }
            else
            {
                clearPropObjects();
            }
        }
        else
        {
            clearPropObjects();
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
     * Ensure RuneLite props exist for the provided placements.
     */
    private void ensurePropObjects(List<PropPlacement> placements)
    {
        if (placements == null || placements.isEmpty())
        {
            clearPropObjects();
            return;
        }

        List<PlacementInstance> instances = new ArrayList<>();
        for (PropPlacement p : placements)
        {
            PlacementInstance inst = new PlacementInstance(
                p.point,
                p.modelId,
                p.orientationOffset,
                p.orientation,
                p.offsetX,
                p.offsetY
            );
            instances.add(inst);
        }

        instances.sort(Comparator
            .comparingInt((PlacementInstance i) -> i.point.getPlane())
            .thenComparingInt(i -> i.point.getX())
            .thenComparingInt(i -> i.point.getY())
            .thenComparingInt(i -> i.orientationBase)
            .thenComparingInt(i -> i.offsetX)
            .thenComparingInt(i -> i.offsetY)
            .thenComparingInt(i -> i.modelId));

        Set<PlacementInstance> newSet = new HashSet<>(instances);

        int hash = 17;
        for (PlacementInstance inst : newSet)
        {
            hash = 31 * hash + inst.hashCode();
        }

        if (hash == lastPropHash && lastPlacements.equals(newSet))
        {
            return;
        }

        lastPropHash = hash;
        lastPlacements.clear();
        lastPlacements.addAll(newSet);

        clientThread.invokeLater(() -> rebuildPropObjectsInternal(instances));
    }

    private void addPropPlacementsForTile(List<PropPlacement> placements,
                                          WorldPoint wp,
                                          Set<WorldPoint> boundaryTiles,
                                          Set<WorldPoint> innerTiles,
                                          PropDefinition def)
    {
        if (boundaryTiles == null || def == null)
        {
            return;
        }

        Set<WorldPoint> inner = innerTiles != null ? innerTiles : Set.of();
        int x = wp.getX();
        int y = wp.getY();
        int plane = wp.getPlane();

        WorldPoint north = new WorldPoint(x, y + 1, plane);
        WorldPoint east = new WorldPoint(x + 1, y, plane);
        WorldPoint south = new WorldPoint(x, y - 1, plane);
        WorldPoint west = new WorldPoint(x - 1, y, plane);

        boolean outN = isOutsideTile(north, boundaryTiles, inner);
        boolean outE = isOutsideTile(east, boundaryTiles, inner);
        boolean outS = isOutsideTile(south, boundaryTiles, inner);
        boolean outW = isOutsideTile(west, boundaryTiles, inner);

        int rotateRepeat = 512; // rotate one edge clockwise for visual alignment (repeat props)
        int rotateSingle = 0;   // keep original orientation for single props
        int shift = 24; // local units (~128 per tile)
        boolean repeat = isRepeatPerEdge(def);
        boolean placeOutside = isPlaceOutside(def);
        int outsideShift = 96; // push into the outside tile for placeOutside props

        if (repeat || placeOutside) // treat placeOutside as repeat with outside offset
        {
            int off = placeOutside ? outsideShift : shift;
            if (outN) placements.add(new PropPlacement(wp, (0 + rotateRepeat) % 2048, def, 0, off));
            if (outE) placements.add(new PropPlacement(wp, (512 + rotateRepeat) % 2048, def, off, 0));
            if (outS) placements.add(new PropPlacement(wp, (1024 + rotateRepeat) % 2048, def, 0, -off));
            if (outW) placements.add(new PropPlacement(wp, (1536 + rotateRepeat) % 2048, def, -off, 0));
        }
        else
        {
            if (outN)
            {
                int off = placeOutside ? outsideShift : 0;
                placements.add(new PropPlacement(wp, (0 + rotateSingle) % 2048, def, 0, off));
                return;
            }
            if (outE)
            {
                int off = placeOutside ? outsideShift : 0;
                placements.add(new PropPlacement(wp, (512 + rotateSingle) % 2048, def, off, 0));
                return;
            }
            if (outS)
            {
                int off = placeOutside ? -outsideShift : 0;
                placements.add(new PropPlacement(wp, (1024 + rotateSingle) % 2048, def, 0, off));
                return;
            }
            if (outW)
            {
                int off = placeOutside ? -outsideShift : 0;
                placements.add(new PropPlacement(wp, (1536 + rotateSingle) % 2048, def, off, 0));
                return;
            }
            // fully enclosed, drop one default
            placements.add(new PropPlacement(wp, (0 + rotateSingle) % 2048, def, 0, 0));
        }
    }

    private boolean isRepeatPerEdge(PropDefinition def)
    {
        if (def == null)
        {
            return false;
        }
        return def.isRepeatPerEdge();
    }

    private boolean isPlaceOutside(PropDefinition def)
    {
        if (def == null)
        {
            return false;
        }
        return def.isPlaceOutside();
    }

    /**
     * Remove all active prop RuneLiteObjects.
     */
    public void clearPropObjects()
    {
        lastPropHash = 0;
        lastPlacements.clear();

        Runnable deactivate = () -> {
            for (List<RuneLiteObject> objs : propObjects.values())
            {
                for (RuneLiteObject obj : objs)
                {
                    obj.setActive(false);
                }
            }
            propObjects.clear();
        };

        if (client.isClientThread())
        {
            deactivate.run();
        }
        else
        {
            clientThread.invokeLater(deactivate);
        }
    }

    private void rebuildPropObjectsInternal(List<PlacementInstance> instances)
    {
        if (instances == null || instances.isEmpty())
        {
            clearPropObjects();
            return;
        }

        clearPropObjects();

        Map<Integer, Model> modelCache = new HashMap<>();

        for (PlacementInstance instKey : instances)
        {
            WorldPoint wp = instKey.point;

            Model model = modelCache.computeIfAbsent(instKey.modelId, id -> client.loadModel(id));
            if (model == null)
            {
                continue;
            }

            Collection<WorldPoint> localInstances = WorldPoint.toLocalInstance(client, wp);
            if (localInstances == null || localInstances.isEmpty())
            {
                continue;
            }

            for (WorldPoint inst : localInstances)
            {
                LocalPoint lp = LocalPoint.fromWorld(client, inst);
                if (lp == null)
                {
                    continue;
                }

                RuneLiteObject obj = client.createRuneLiteObject();
                obj.setModel(model);
                obj.setLocation(new LocalPoint(lp.getX() + instKey.offsetX, lp.getY() + instKey.offsetY), inst.getPlane());
                obj.setOrientation((instKey.orientationBase + instKey.orientationOffset) % 2048);
                obj.setActive(true);
                propObjects.computeIfAbsent(wp, k -> new ArrayList<>()).add(obj);
            }
        }
    }

    @SuppressWarnings("unused")
    private int computeOutwardOrientation(WorldPoint wp,
                                   Set<WorldPoint> boundaryTiles,
                                   Set<WorldPoint> innerTiles)
    {
        if (boundaryTiles == null)
        {
            return 0;
        }

        Set<WorldPoint> inner = innerTiles != null ? innerTiles : Set.of();

        int x = wp.getX();
        int y = wp.getY();
        int plane = wp.getPlane();

        WorldPoint north = new WorldPoint(x, y + 1, plane);
        WorldPoint east = new WorldPoint(x + 1, y, plane);
        WorldPoint south = new WorldPoint(x, y - 1, plane);
        WorldPoint west = new WorldPoint(x - 1, y, plane);

        if (isOutsideTile(north, boundaryTiles, inner))
        {
            return 0;
        }
        if (isOutsideTile(east, boundaryTiles, inner))
        {
            return 512;
        }
        if (isOutsideTile(south, boundaryTiles, inner))
        {
            return 1024;
        }
        if (isOutsideTile(west, boundaryTiles, inner))
        {
            return 1536;
        }

        // Fully surrounded (rare); default north
        return 0;
    }

    private RegionLockEnforcerConfig.PropStyle resolvePropStyle(Border border)
    {
        RegionLockEnforcerConfig.PropStyle style = border != null ? border.getPropStyle() : null;
        return style != null ? style : RegionLockEnforcerConfig.PropStyle.SEA_ROCK;
    }

    private PropDefinition getPropDefinition(RegionLockEnforcerConfig.PropStyle style)
    {
        switch (style)
        {
            case SEA_ROCK:
                return PropDefinition.randomized(
                    new int[]{58596, 58597, 58598, 58599, 58600, 58601},
                    /*repeatPerEdge=*/true,
                    /*placeOutside=*/true,
                    /*randomOrientation=*/true
                );
            case ROCK_WALL:
                return PropDefinition.fixed(17319, /*repeatPerEdge=*/true, /*placeOutside=*/true);
            case IRON_FENCE:
                return PropDefinition.fixed(6745, /*repeatPerEdge=*/true, /*placeOutside=*/false);
            case LOG_FENCE:
                return PropDefinition.fixed(42889, /*repeatPerEdge=*/true, /*placeOutside=*/false);
            default:
                return PropDefinition.fixed(58598, /*repeatPerEdge=*/true, /*placeOutside=*/true);
        }
    }

    private static class PropPlacement
    {
        private final WorldPoint point;
        private final int orientation;
        private final int offsetX;
        private final int offsetY;
        private final int modelId;
        private final int orientationOffset;

        PropPlacement(WorldPoint point, int orientation, PropDefinition definition, int offsetX, int offsetY)
        {
            this.point = point;
            this.orientation = orientation;
            this.offsetX = offsetX;
            this.offsetY = offsetY;
            this.modelId = definition.resolveModelId(point);
            this.orientationOffset = definition.resolveOrientationOffset(point);
        }
    }

    private static class PlacementInstance
    {
        private final WorldPoint point;
        private final int modelId;
        private final int orientationOffset;
        private final int orientationBase;
        private final int offsetX;
        private final int offsetY;

        PlacementInstance(WorldPoint point, int modelId, int orientationOffset, int orientationBase, int offsetX, int offsetY)
        {
            this.point = point;
            this.modelId = modelId;
            this.orientationOffset = orientationOffset;
            this.orientationBase = orientationBase;
            this.offsetX = offsetX;
            this.offsetY = offsetY;
        }

        @Override
        public boolean equals(Object o)
        {
            if (this == o) return true;
            if (!(o instanceof PlacementInstance)) return false;
            PlacementInstance other = (PlacementInstance) o;
            return modelId == other.modelId
                && orientationOffset == other.orientationOffset
                && orientationBase == other.orientationBase
                && offsetX == other.offsetX
                && offsetY == other.offsetY
                && point.equals(other.point);
        }

        @Override
        public int hashCode()
        {
            int result = point.hashCode();
            result = 31 * result + Integer.hashCode(modelId);
            result = 31 * result + Integer.hashCode(orientationOffset);
            result = 31 * result + Integer.hashCode(orientationBase);
            result = 31 * result + Integer.hashCode(offsetX);
            result = 31 * result + Integer.hashCode(offsetY);
            return result;
        }
    }

    private static class PropDefinition
    {
        private final int[] modelIds;
        private final boolean repeatPerEdge;
        private final boolean placeOutside;
        private final boolean randomOrientation;
        private final int fixedOrientationOffset;

        private PropDefinition(int[] modelIds, boolean repeatPerEdge, boolean placeOutside, boolean randomOrientation, int fixedOrientationOffset)
        {
            this.modelIds = modelIds;
            this.repeatPerEdge = repeatPerEdge;
            this.placeOutside = placeOutside;
            this.randomOrientation = randomOrientation;
            this.fixedOrientationOffset = fixedOrientationOffset;
        }

        static PropDefinition fixed(int modelId, boolean repeatPerEdge, boolean placeOutside)
        {
            return new PropDefinition(new int[]{modelId}, repeatPerEdge, placeOutside, false, 0);
        }

        static PropDefinition randomized(int[] modelIds, boolean repeatPerEdge, boolean placeOutside, boolean randomOrientation)
        {
            return new PropDefinition(modelIds, repeatPerEdge, placeOutside, randomOrientation, 0);
        }

        int resolveModelId(WorldPoint point)
        {
            if (modelIds.length == 1)
            {
                return modelIds[0];
            }
            long seed = computeSeed(point, 0x9E3779B97F4A7C15L);
            java.util.Random rng = new java.util.Random(seed);
            int idx = rng.nextInt(modelIds.length);
            return modelIds[idx];
        }

        int resolveOrientationOffset(WorldPoint point)
        {
            if (!randomOrientation)
            {
                return fixedOrientationOffset;
            }
            long seed = computeSeed(point, 0xC2B2AE3D27D4EB4FL);
            java.util.Random rng = new java.util.Random(seed);
            int spin = rng.nextInt(4); // 0,1,2,3 -> 0/512/1024/1536
            return (spin * 512) % 2048;
        }

        boolean isRepeatPerEdge()
        {
            return repeatPerEdge;
        }

        boolean isPlaceOutside()
        {
            return placeOutside;
        }

        private static long computeSeed(WorldPoint point, long salt)
        {
            return ((long) point.getX() * 73856093L)
                ^ ((long) point.getY() * 19349663L)
                ^ ((long) point.getPlane() * 83492791L)
                ^ salt;
        }
    }

    /**
     * Draw lines along the outer edges of boundary tiles.
     * An edge is "outer" if:
     * 1. It belongs to a boundary tile (in markedTiles)
     * 2. The neighbor tile across that edge is outside (not in boundaryTiles AND not in innerTiles)
     */
    private void drawBorderLines(Graphics2D g, Set<WorldPoint> markedTiles, Set<WorldPoint> innerTiles, Color overrideColor)
    {
        if (markedTiles.isEmpty()) return;
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
        g.setColor(overrideColor != null ? overrideColor : DEFAULT_BORDER_COLOR); // per-border or default color

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
