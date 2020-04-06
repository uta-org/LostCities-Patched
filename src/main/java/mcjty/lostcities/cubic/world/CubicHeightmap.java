package mcjty.lostcities.cubic.world;

import io.github.opencubicchunks.cubicchunks.api.util.CubePos;
import io.github.opencubicchunks.cubicchunks.api.world.ICubicWorld;
import mcjty.lostcities.config.LandscapeType;
import mcjty.lostcities.cubic.world.driver.ICubeDriver;
import mcjty.lostcities.dimensions.world.terraingen.LostCitiesTerrainGenerator;
import net.minecraft.util.math.BlockPos;

import java.util.Objects;

public class CubicHeightmap implements ICommonHeightmap {
    private int heightmap[];
    private int chunkY;

    private static ICubicWorld world;

    public CubicHeightmap(ICubeDriver driver, LandscapeType type, int groundLevel, char baseChar) {
        world = (ICubicWorld)CubicCityWorldProcessor.worldObj;

        // TODO:  Cavern & space

        /*
        heightmap = new int[16*16];
        char air = LostCitiesTerrainGenerator.airChar;
        if (type == LandscapeType.CAVERN) {
            // Here we try to find the height inside the cavern itself. Ignoring the top layer
            int base = Math.max(groundLevel - 20, 1);
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    int y = base;
                    driver.current(x, y, z);
                    while (y < 100 && driver.getBlock() != air) {
                        y++;
                        driver.incY();
                    }

                    // TODO
                    if (y >= 100) {
                        y = 128;
                    } else {
                        while (y > 0 && driver.getBlock() == air) {
                            y--;
                            driver.decY();
                        }
                    }
                    heightmap[z * 16 + x] = (byte) y;
                }
            }
        } else if (type == LandscapeType.SPACE) {
            // Here we ignore the glass from the spheres
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    int y = 15;
                    driver.current(x, y, z);
                    while (y > 0 && driver.getBlock() != baseChar) {
                        y--;
                        driver.decY();
                    }
                    heightmap[z * 16 + x] = y;
                }
            }
        } else {
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    int y = 15;
                    driver.current(x, y, z);
                    while (y > 0 && driver.getBlock() == air) {
                        y--;
                        driver.decY();
                    }
                    heightmap[z * 16 + x] = y;
                }
            }
        }
        */
    }

    public void setChunkY(int chunkY) { this.chunkY = chunkY; }

    public int getHeight(int x, int z) {

        int y = chunkY * 16;
        BlockPos topBlock = world.findTopBlock(new BlockPos(x, y, z), y, y + 16, ICubicWorld.SurfaceType.SOLID);
        if(topBlock == null) return y;
        return topBlock.getY();

        // new CubePos(x, chunkY, z), 0, 0, 0, ICubicWorld.SurfaceType.SOLID
        // return (chunkY * 16 + 8) + heightmap[z*16+x] & 0xff;
    }

    public int getAverageHeight() {
        int cnt = 0;
        int y = 0;
        int yy;
        yy = getHeight(2, 2);
        if (yy > 5) {
            y += yy;
            cnt++;
        }
        yy = getHeight(13, 2);
        if (yy > 5) {
            y += yy;
            cnt++;
        }
        yy = getHeight(2, 13);
        if (yy > 5) {
            y += yy;
            cnt++;
        }
        yy = getHeight(13, 13);
        if (yy > 5) {
            y += yy;
            cnt++;
        }
        yy = getHeight(8, 8);
        if (yy > 5) {
            y += yy;
            cnt++;
        }
        if (cnt > 0) {
            return y / cnt;
        } else {
            return 0;
        }
    }

    public int getMinimumHeight() {
        int y = 255;
        int yy;
        yy = getHeight(2, 2);
        if (yy < y) {
            y = yy;
        }
        yy = getHeight(13, 2);
        if (yy < y) {
            y = yy;
        }
        yy = getHeight(2, 13);
        if (yy < y) {
            y = yy;
        }
        yy = getHeight(13, 13);
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
