package mcjty.lostcities.cubic.world.generators;

import mcjty.lostcities.cubic.world.ICommonHeightmap;
import mcjty.lostcities.dimensions.world.driver.IIndex;
import mcjty.lostcities.dimensions.world.lost.BuildingInfo;
import mcjty.lostcities.dimensions.world.lost.Direction;
import mcjty.lostcities.dimensions.world.lost.Transform;
import mcjty.lostcities.dimensions.world.lost.cityassets.BuildingPart;
import mcjty.lostcities.dimensions.world.lost.cityassets.CompiledPalette;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;
import java.util.Map;

import static mcjty.lostcities.cubic.world.CubicCityUtils.*;
import static mcjty.lostcities.cubic.world.CubicCityWorldPopulator.*;

import static mcjty.lostcities.cubic.world.CubicCityWorldProcessor.driver;

public class Utils {

    private static int g_seed = 123456789;
    private static char[] randomLeafs = null;

    public static void clearRange(BuildingInfo info, int x, int z, int height1, int height2, boolean dowater) {
        if (dowater) {
            // Special case for drowned city
            driver.setBlockRangeSafe(x, height1, z, info.waterLevel, liquidChar);
            driver.setBlockRangeSafe(x, info.waterLevel+1, z, height2, airChar);
        } else {
            driver.setBlockRange(x, height1, z, height2, airChar);
        }
    }

    // Used for space type worlds: fill underside the building/street until a block is encountered
    public static void fillToGround(BuildingInfo info, int lowestLevel, Character borderBlock) {
        for (int x = 0; x < 16; ++x) {
            for (int z = 0; z < 16; ++z) {
                int y = lowestLevel - 1;
                driver.current(x, y, z);
                if (isSide(x, z)) {
                    while (y > 1 && driver.getBlock() == airChar) {
                        driver.block(info.getCompiledPalette().get(borderBlock)).decY();
                    }
                } else {
                    while (y > 1 && driver.getBlock() == airChar) {
                        driver.block(baseChar).decY();
                    }
                }
            }
        }
    }

    public static int fastrand() {
        g_seed = (214013 * g_seed + 2531011);
        return (g_seed >> 16) & 0x7FFF;
    }

    public static int fastrand128() {
        g_seed = (214013 * g_seed + 2531011);
        return (g_seed >> 16) & 0x7F;
    }

    public static char getRandomLeaf() {
        if (randomLeafs == null) {
            randomLeafs = new char[128];
            int i = 0;
            for (; i < 20; i++) {
                randomLeafs[i] = leaves2Char;
            }
            for (; i < 40; i++) {
                randomLeafs[i] = leaves3Char;
            }
            for (; i < randomLeafs.length; i++) {
                randomLeafs[i] = leavesChar;
            }
        }
        return randomLeafs[fastrand128()];
    }


    public static boolean doBorder(BuildingInfo info, Direction direction) {
        BuildingInfo adjacent = direction.get(info);
        if (isHigherThenNearbyStreetChunk(info, adjacent)) {
            return true;
        } else if (!adjacent.isCity) {
            if (adjacent.cityLevel <= info.cityLevel) {
                return true;
            }
            if (info.profile.isSpace()) {
                // Base it on ground level
                ICommonHeightmap adjacentHeightmap = provider.getHeightmap(info.chunkX, info.chunkZ);
                int adjacentHeight = adjacentHeightmap.getAverageHeight();
                if (adjacentHeight > 5) {
                    if ((adjacentHeight-4) < info.getCityGroundLevel()) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public static boolean isHigherThenNearbyStreetChunk(BuildingInfo info, BuildingInfo adjacent) {
        if (!adjacent.isCity) {
            return false;
        }
        if (adjacent.hasBuilding) {
            return adjacent.cityLevel + adjacent.getNumFloors() < info.cityLevel;
        } else {
            return adjacent.cityLevel < info.cityLevel;
        }
    }

    public static void setBlocksFromPalette(int x, int y, int z, int y2, CompiledPalette palette, char character) {
        if (palette.isSimple(character)) {
            char b = palette.get(character);
            driver.setBlockRangeSafe(x, y, z, y2, b);
        } else {
            driver.current(x, y, z);
            while (y < y2) {
                driver.add(palette.get(character));
                y++;
            }
        }
    }

    public static boolean borderNeedsConnectionToAdjacentChunk(BuildingInfo info, int x, int z) {
        for (Direction direction : Direction.VALUES) {
            if (direction.atSide(x, z)) {
                BuildingInfo adjacent = direction.get(info);
                if (adjacent.getActualStairDirection() == direction.getOpposite()) {
                    BuildingPart stairType = adjacent.stairType;
                    Integer z1 = stairType.getMetaInteger("z1");
                    Integer z2 = stairType.getMetaInteger("z2");
                    Transform transform = direction.getOpposite().getRotation();
                    int xx1 = transform.rotateX(15, z1);
                    int zz1 = transform.rotateZ(15, z1);
                    int xx2 = transform.rotateX(15, z2);
                    int zz2 = transform.rotateZ(15, z2);
                    if (x >= Math.min(xx1, xx2) && x <= Math.max(xx1, xx2) && z >= Math.min(zz1, zz2) && z <= Math.max(zz1, zz2)) {
                        return true;
                    }
                }
                if (adjacent.hasBridge(provider, direction.getOrientation()) != null) {
                    return true;
                }
            }
        }
        return false;
    }


    public static int getInterpolatedHeight(BuildingInfo info, int x, int z) {
        if (x < 8 && z < 8) {
            // First quadrant
            float h00 = info.getXmin().getZmin().getCityGroundLevelOutsideLower();
            float h10 = info.getZmin().getCityGroundLevelOutsideLower();
            float h01 = info.getXmin().getCityGroundLevelOutsideLower();
            float h11 = info.getCityGroundLevelOutsideLower();
            return bipolate(h00, h10, h01, h11, x + 8, z + 8);
        } else if (x >= 8 && z < 8) {
            // Second quadrant
            float h00 = info.getZmin().getCityGroundLevelOutsideLower();
            float h10 = info.getXmax().getZmin().getCityGroundLevelOutsideLower();
            float h01 = info.getCityGroundLevelOutsideLower();
            float h11 = info.getXmax().getCityGroundLevelOutsideLower();
            return bipolate(h00, h10, h01, h11, x - 8, z + 8);
        } else if (x < 8 && z >= 8) {
            // Third quadrant
            float h00 = info.getXmin().getCityGroundLevelOutsideLower();
            float h10 = info.getCityGroundLevelOutsideLower();
            float h01 = info.getXmin().getZmax().getCityGroundLevelOutsideLower();
            float h11 = info.getZmax().getCityGroundLevelOutsideLower();
            return bipolate(h00, h10, h01, h11, x + 8, z - 8);
        } else {
            // Fourth quadrant
            float h00 = info.getCityGroundLevelOutsideLower();
            float h10 = info.getXmax().getCityGroundLevelOutsideLower();
            float h01 = info.getZmax().getCityGroundLevelOutsideLower();
            float h11 = info.getXmax().getZmax().getCityGroundLevelOutsideLower();
            return bipolate(h00, h10, h01, h11, x - 8, z - 8);
        }
    }

    private static int bipolate(float h00, float h10, float h01, float h11, int dx, int dz) {
        float factor = (15.0f - dx) / 15.0f;
        float h0 = h00 + (h10 - h00) * factor;
        float h1 = h01 + (h11 - h01) * factor;
        float h = h0 + (h1 - h0) * (15.0f - dz) / 15.0f;
        return (int) h;
    }


    public static void fixTorches(BuildingInfo info) {
        List<Pair<IIndex, Map<String, Integer>>> torches = info.getTorchTodo();
        if (torches.isEmpty()) {
            return;
        }

        for (Pair<IIndex, Map<String, Integer>> pair : torches) {
            IIndex idx = pair.getLeft();
            driver.current(idx);
            Map<String, Integer> map = pair.getRight();

            char torch = driver.getBlock();
            IBlockState torchState = Block.BLOCK_STATE_IDS.getByValue(torch);
            if (map != null) {
                int x = driver.getX();
                int z = driver.getZ();
                if (driver.getY() != 0 && driver.getBlockDown() != airChar) {
                    driver.block(torchState.getBlock().getStateFromMeta(map.get("up")));
                } else if (x > 0 && driver.getBlockWest() != airChar) {
                    driver.block(torchState.getBlock().getStateFromMeta(map.get("east")));
                } else if (x < 15 && driver.getBlockEast() != airChar) {
                    driver.block(torchState.getBlock().getStateFromMeta(map.get("west")));
                } else if (z > 0 && driver.getBlockNorth() != airChar) {
                    driver.block(torchState.getBlock().getStateFromMeta(map.get("south")));
                } else if (z < 15 && driver.getBlockSouth() != airChar) {
                    driver.block(torchState.getBlock().getStateFromMeta(map.get("north")));
                }
            }
        }
        info.clearTorchTodo();
    }

    public static boolean isSide(int x, int z) {
        return x == 0 || x == 15 || z == 0 || z == 15;
    }

    public static boolean isCorner(int x, int z) {
        return (x == 0 || x == 15) && (z == 0 || z == 15);
    }

    public static boolean isStreetBorder(int x, int z) {
        return x <= streetBorder || x >= (15 - streetBorder) || z <= streetBorder || z >= (15 - streetBorder);
    }
}
