package mcjty.lostcities.cubic;

import io.github.opencubicchunks.cubicchunks.api.util.CubePos;

import java.util.HashMap;
import java.util.Map;

/**
 * A class that stores the heightmap in order to be used by other mods.
 * @author z3nth10n
 */
public class HeightmapModel {
    private static final Map<CubePos, HeightmapModel> cachedHeightmaps = new HashMap<>();

    private boolean surface;
    private double[][] heightmap;

    @SuppressWarnings("unused")
    private HeightmapModel() {}

    public HeightmapModel(boolean surface, double[][] heightmap) {
        this.surface = surface;
        this.heightmap = heightmap;
    }

    public double[][] getHeightmap() {
        return heightmap;
    }

    public boolean isSurface() {
        return surface;
    }

    public static HeightmapModel getModel(int chunkX, int chunkY, int chunkZ) {
        return getModel(new CubePos(chunkX, chunkY, chunkZ), true);
    }

    public static HeightmapModel getModel(CubePos pos) {
        return getModel(pos, true);
    }

    public static HeightmapModel getModel(CubePos pos, boolean remove) {
        if(!cachedHeightmaps.containsKey(pos))
            return null;

        // Once check remove if flag checked
        if(remove) cachedHeightmaps.remove(pos);
        return cachedHeightmaps.get(pos);
    }

    public static void add(CubePos pos, HeightmapModel model) {
        cachedHeightmaps.put(pos, model);
    }
}