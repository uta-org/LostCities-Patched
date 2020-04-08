package mcjty.lostcities.cubic.world;

import io.github.opencubicchunks.cubicchunks.api.util.CubePos;
import io.github.opencubicchunks.cubicchunks.api.world.ICubicWorld;
import io.github.terra121.EarthTerrainProcessor;
import io.github.terra121.dataset.HeightmapModel;
import mcjty.lostcities.LostCitiesDebug;
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

    public CubicHeightmap(ICubeDriver driver, LandscapeType type, int groundLevel, char baseChar) {

    }

    public int getHeight(int x, int z)
    {
        return (int)getInternalHeight(x, z);
    }

    private double getInternalHeight(int x, int z)
    {
        return model.heightmap[x][z];
    }

    public static boolean hasValidSteepness(double[][] arr) {
        return getMaxValue(arr) - getMinValue(arr) <= 8;
    }

    public static boolean hasValidSteepness_Debug(double[][] arr, int chunkX, int chunkY, int chunkZ) {
        double min = getMinValue(arr);
        double max = getMaxValue(arr);
        boolean result = max - min <= 8;

        if(!result) {
            System.out.println("("+chunkX+", "+chunkY+", "+chunkZ+") Steepness not valid: "+max+" - "+min+" <= 8 ("+(max - min)+")!");
        }

        return result;
    }

    private static double getMaxValue(double[][] numbers) {
        double maxValue = numbers[0][0];
        for (int j = 0; j < numbers.length; j++) {
            for (int i = 0; i < numbers[j].length; i++) {
                if (numbers[j][i] > maxValue) {
                    maxValue = numbers[j][i];
                }
            }
        }
        return maxValue;
    }

    private static double getMinValue(double[][] numbers) {
        double minValue = numbers[0][0];
        for (int j = 0; j < numbers.length; j++) {
            for (int i = 0; i < numbers[j].length; i++) {
                if (numbers[j][i] < minValue ) {
                    minValue = numbers[j][i];
                }
            }
        }
        return minValue;
    }

    private static double getAverageValue(double[][] array){
        int counter=0;
        double sum = 0;
        for(int i=0;i<array.length;i++){
            for(int j=0;j<array[i].length;j++){
                sum = sum+array[i][j];
                counter++;
            }
        }

        return sum / counter;
    }

    public int getFullMinHeight() {
        return (int)getMinValue(model.heightmap);
    }

    public int getFullMaxHeight() {
        return (int)getMaxValue(model.heightmap);
    }

    public int getFullAverageHeight() {
        return (int)getAverageValue(model.heightmap);
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
