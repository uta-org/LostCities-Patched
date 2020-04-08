package mcjty.lostcities.cubic.world;

import io.github.opencubicchunks.cubicchunks.api.util.CubePos;
import io.github.opencubicchunks.cubicchunks.api.world.ICubicWorld;
import io.github.terra121.EarthTerrainProcessor;
import io.github.terra121.dataset.HeightmapModel;
import mcjty.lostcities.config.LandscapeType;
import mcjty.lostcities.cubic.world.driver.ICubeDriver;

import java.util.Objects;

public class CubicHeightmap implements ICommonHeightmap {
    private int chunkX, chunkY, chunkZ;
    private HeightmapModel model;

    public void setLocalChunk(CubePos localPos)
    {
        setChunkX(localPos.getX());
        setChunkY(localPos.getY());
        setChunkZ(localPos.getZ());
    }

    public void setLocalChunk(int chunkX, int chunkY, int chunkZ)
    {
        this.chunkX = chunkX;
        this.chunkY = chunkY;
        this.chunkZ = chunkZ;
    }

    public int getChunkX() { return chunkX; }

    public int getChunkY() { return chunkY; }

    public int getChunkZ() { return chunkZ; }

    public void setChunkX(int chunkX) { this.chunkX = chunkX; }

    public void setChunkY(int chunkY) { this.chunkY = chunkY; }

    public void setChunkZ(int chunkZ) { this.chunkZ = chunkZ; }

    public HeightmapModel getModel() { return model; }

    public void setModel(HeightmapModel model) { this.model = model; }

    private static ICubicWorld world;

    public CubicHeightmap(ICubeDriver driver, LandscapeType type, int groundLevel, char baseChar) {
        world = (ICubicWorld)CubicCityWorldProcessor.worldObj;
    }

    public int getHeight(int x, int z)
    {
        /*
        int y = chunkY * 16;
        BlockPos topBlock = world.findTopBlock(new BlockPos(chunkX + x, y, chunkZ + z), y, y + 16, ICubicWorld.SurfaceType.SOLID);
        if(topBlock == null) return y;
        return topBlock.getY();
        */

        return (int)getInternalHeight(x, z);
    }

    private double getInternalHeight(int x, int z)
    {
        return model.heightmap[x][z];
    }

    public boolean hasValidSteepness() {
        return false;
    }

    public int getAverageHeight() {
        int cnt = 0;
        double y = 0;
        double yy;
        yy = getInternalHeight(2, 2);
        if (yy > 5) {
            y += yy;
            cnt++;
        }
        yy = getInternalHeight(13, 2);
        if (yy > 5) {
            y += yy;
            cnt++;
        }
        yy = getInternalHeight(2, 13);
        if (yy > 5) {
            y += yy;
            cnt++;
        }
        yy = getInternalHeight(13, 13);
        if (yy > 5) {
            y += yy;
            cnt++;
        }
        yy = getInternalHeight(8, 8);
        if (yy > 5) {
            y += yy;
            cnt++;
        }
        if (cnt > 0) {
            return (int)(y / cnt);
        } else {
            return 0;
        }
    }

    public int getMinimumHeight() {
        int y = Integer.MAX_VALUE;
        int yy;
        yy = getHeight(0, 0);
        if (yy < y) {
            y = yy;
        }
        yy = getHeight(0, 15);
        if (yy < y) {
            y = yy;
        }
        yy = getHeight(15, 15);
        if (yy < y) {
            y = yy;
        }
        yy = getHeight(15, 0);
        if (yy < y) {
            y = yy;
        }
        yy = getHeight(8, 8);
        if (yy < y) {
            y = yy;
        }
        return y;
    }
}
