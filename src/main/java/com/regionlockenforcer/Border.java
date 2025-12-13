package com.regionlockenforcer;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import lombok.Data;
import net.runelite.api.coords.WorldPoint;
import java.awt.Color;

/**
 * Represents a single border within a region.
 * Holds its own boundary and inner tiles and can notify listeners when it changes.
 */
@Data
public class Border
{
    private String name;
    private Set<WorldPoint> boundaryTiles = ConcurrentHashMap.newKeySet();
    private Set<WorldPoint> innerTiles = ConcurrentHashMap.newKeySet();

    // Cache of boundary + inner tiles for quick click checks
    private transient Set<WorldPoint> cachedClickableTiles = null;

    // Optional per-border styling
    private RenderMode renderMode = RenderMode.LINES;
    private RegionLockEnforcerConfig.PropStyle propStyle = null;
    private Color lineColor = null;

    // Optional callback to signal parent region that cached aggregates should be invalidated
    private transient Runnable onChange;

    public Border()
    {
        this("Border 1");
    }

    public Border(String name)
    {
        this.name = name != null ? name : "Border 1";
    }

    public void setOnChange(Runnable onChange)
    {
        this.onChange = onChange;
    }

    public Set<WorldPoint> getBoundaryTiles()
    {
        if (boundaryTiles == null)
        {
            boundaryTiles = ConcurrentHashMap.newKeySet();
        }
        return boundaryTiles;
    }

    public Set<WorldPoint> getInnerTiles()
    {
        if (innerTiles == null)
        {
            innerTiles = ConcurrentHashMap.newKeySet();
        }
        return innerTiles;
    }

    public boolean addTile(WorldPoint tile)
    {
        boolean added = getBoundaryTiles().add(tile);
        if (added)
        {
            invalidate();
        }
        return added;
    }

    public boolean removeTile(WorldPoint tile)
    {
        boolean removed = getBoundaryTiles().remove(tile);
        if (removed)
        {
            invalidate();
        }
        return removed;
    }

    public boolean toggleTile(WorldPoint tile)
    {
        if (getBoundaryTiles().contains(tile))
        {
            getBoundaryTiles().remove(tile);
            invalidate();
            return false;
        }
        else
        {
            getBoundaryTiles().add(tile);
            invalidate();
            return true;
        }
    }

    public Set<WorldPoint> getAllClickableTiles()
    {
        if (cachedClickableTiles != null)
        {
            return cachedClickableTiles;
        }

        Set<WorldPoint> all = new HashSet<>(getBoundaryTiles());
        all.addAll(getInnerTiles());
        cachedClickableTiles = all;
        return cachedClickableTiles;
    }

    public void ensureCacheComputed()
    {
        if (cachedClickableTiles == null)
        {
            getAllClickableTiles();
        }
    }

    public void setBoundaryTiles(Set<WorldPoint> boundaryTiles)
    {
        Set<WorldPoint> newSet = ConcurrentHashMap.newKeySet();
        if (boundaryTiles != null)
        {
            newSet.addAll(boundaryTiles);
        }
        this.boundaryTiles = newSet;
        invalidate();
    }

    public void setInnerTiles(Set<WorldPoint> innerTiles)
    {
        Set<WorldPoint> newSet = ConcurrentHashMap.newKeySet();
        if (innerTiles != null)
        {
            newSet.addAll(innerTiles);
        }
        this.innerTiles = newSet;
        invalidate();
    }

    public RegionLockEnforcerConfig.PropStyle getPropStyle()
    {
        return propStyle;
    }

    public void setPropStyle(RegionLockEnforcerConfig.PropStyle propStyle)
    {
        this.propStyle = propStyle;
        invalidate();
    }

    public RenderMode getRenderMode()
    {
        return renderMode == null ? RenderMode.LINES : renderMode;
    }

    public void setRenderMode(RenderMode renderMode)
    {
        this.renderMode = renderMode;
        invalidate();
    }

    public Color getLineColor()
    {
        return lineColor;
    }

    public void setLineColor(Color lineColor)
    {
        this.lineColor = lineColor;
        invalidate();
    }

    public enum RenderMode
    {
        LINES,
        PROPS
    }

    public int addArea(int startX, int startY, int plane, int size)
    {
        if (size <= 0)
        {
            return 0;
        }

        int added = 0;

        for (int x = 0; x < size; x++)
        {
            for (int y = 0; y < size; y++)
            {
                WorldPoint tile = new WorldPoint(startX + x, startY + y, plane);
                if (getBoundaryTiles().add(tile))
                {
                    added++;
                }
            }
        }

        getInnerTiles().clear();
        invalidate();
        return added;
    }

    public int removeArea(int startX, int startY, int plane, int size)
    {
        if (size <= 0)
        {
            return 0;
        }

        int removed = 0;

        for (int x = 0; x < size; x++)
        {
            for (int y = 0; y < size; y++)
            {
                WorldPoint tile = new WorldPoint(startX + x, startY + y, plane);
                if (getBoundaryTiles().remove(tile))
                {
                    removed++;
                }
            }
        }

        getInnerTiles().clear();
        invalidate();
        return removed;
    }

    public int addChunk(int chunkX, int chunkY, int plane)
    {
        return addArea(chunkX * 64, chunkY * 64, plane, 64);
    }

    public int removeChunk(int chunkX, int chunkY, int plane)
    {
        return removeArea(chunkX * 64, chunkY * 64, plane, 64);
    }

    public boolean hasAnyTileInArea(int startX, int startY, int plane, int size)
    {
        if (size <= 0)
        {
            return false;
        }

        for (int x = 0; x < size; x++)
        {
            for (int y = 0; y < size; y++)
            {
                WorldPoint tile = new WorldPoint(startX + x, startY + y, plane);
                if (getBoundaryTiles().contains(tile))
                {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean isAreaFullyContained(int startX, int startY, int plane, int size)
    {
        if (size <= 0)
        {
            return false;
        }

        for (int x = 0; x < size; x++)
        {
            for (int y = 0; y < size; y++)
            {
                WorldPoint tile = new WorldPoint(startX + x, startY + y, plane);
                if (!getBoundaryTiles().contains(tile))
                {
                    return false;
                }
            }
        }

        return true;
    }

    public boolean isChunkFullyContained(int chunkX, int chunkY, int plane)
    {
        return isAreaFullyContained(chunkX * 64, chunkY * 64, plane, 64);
    }

    private void invalidate()
    {
        cachedClickableTiles = null;
        if (onChange != null)
        {
            onChange.run();
        }
    }
}

