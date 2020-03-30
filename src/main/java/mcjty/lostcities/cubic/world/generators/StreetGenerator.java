package mcjty.lostcities.cubic.world.generators;

import mcjty.lostcities.api.RailChunkType;
import mcjty.lostcities.cubic.world.CubicHeightmap;
import mcjty.lostcities.cubic.world.ICommonHeightmap;
import mcjty.lostcities.dimensions.world.lost.*;
import mcjty.lostcities.dimensions.world.lost.cityassets.BuildingPart;
import mcjty.lostcities.dimensions.world.lost.cityassets.CompiledPalette;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRail;
import net.minecraft.block.BlockRailBase;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;

import java.util.Random;

import static mcjty.lostcities.cubic.world.LostCityCubicGenerator.*;
import static mcjty.lostcities.cubic.world.generators.PartGenerator.*;
import static mcjty.lostcities.cubic.world.generators.Utils.*;

public class StreetGenerator {

    public void generateStreet(BuildingInfo info, CubicHeightmap heightmap, Random rand) {
        boolean xRail = info.hasXCorridor();
        boolean zRail = info.hasZCorridor();
        if (xRail || zRail) {
            generateCorridors(info, xRail, zRail);
        }

        Railway.RailChunkInfo railInfo = info.getRailInfo();
        boolean canDoParks = info.getHighwayXLevel() != info.cityLevel && info.getHighwayZLevel() != info.cityLevel
                && railInfo.getType() != RailChunkType.STATION_SURFACE
                && (railInfo.getType() != RailChunkType.STATION_EXTENSION_SURFACE || railInfo.getLevel() < info.cityLevel);

        if (canDoParks) {
            int height = info.getCityGroundLevel();

            BuildingInfo.StreetType streetType = info.streetType;
            boolean elevated = info.isElevatedParkSection();
            if (elevated) {
                Character elevationBlock = info.getCityStyle().getParkElevationBlock();
                char elevation = info.getCompiledPalette().get(elevationBlock);
                streetType = BuildingInfo.StreetType.PARK;
                for (int x = 0; x < 16; ++x) {
                    driver.current(x, height, 0);
                    for (int z = 0; z < 16; ++z) {
                        driver.block(elevation).incZ();
                    }
                }
                height++;
            }

            switch (streetType) {
                case NORMAL:
                    generateNormalStreetSection(info, height);
                    break;
                case FULL:
                    generateFullStreetSection(height);
                    break;
                case PARK:
                    generateParkSection(info, height, elevated);
            }
            height++;

            if (streetType == BuildingInfo.StreetType.PARK || info.fountainType != null) {
                BuildingPart part;
                if (streetType == BuildingInfo.StreetType.PARK) {
                    part = info.parkType;
                } else {
                    part = info.fountainType;
                }
                generatePart(info, part, Transform.ROTATE_NONE, 0, height, 0, false);
            }

            generateRandomVegetation(info, rand, height);

            generateFrontPart(info, height, info.getXmin(), Transform.ROTATE_NONE);
            generateFrontPart(info, height, info.getZmin(), Transform.ROTATE_90);
            generateFrontPart(info, height, info.getXmax(), Transform.ROTATE_180);
            generateFrontPart(info, height, info.getZmax(), Transform.ROTATE_270);
        }

        generateBorders(info, canDoParks);
    }

    public void generateStreetDecorations(BuildingInfo info) {
        Direction stairDirection = info.getActualStairDirection();
        if (stairDirection != null) {
            BuildingPart stairs = info.stairType;
            Transform transform;
            int oy = info.getCityGroundLevel() + 1;
            switch (stairDirection) {
                case XMIN:
                    transform = Transform.ROTATE_NONE;
                    break;
                case XMAX:
                    transform = Transform.ROTATE_180;
                    break;
                case ZMIN:
                    transform = Transform.ROTATE_90;
                    break;
                case ZMAX:
                    transform = Transform.ROTATE_270;
                    break;
                default:
                    throw new RuntimeException("Cannot happen!");
            }

            generatePart(info, stairs, transform, 0, oy, 0, false);
        }
    }

    private void generateCorridors(BuildingInfo info, boolean xRail, boolean zRail) {
        IBlockState railx = Blocks.RAIL.getDefaultState().withProperty(BlockRail.SHAPE, BlockRailBase.EnumRailDirection.EAST_WEST);
        char railxC = (char) Block.BLOCK_STATE_IDS.get(railx);
        IBlockState railz = Blocks.RAIL.getDefaultState();
        char railzC = (char) Block.BLOCK_STATE_IDS.get(railz);

        Character corridorRoofBlock = info.getCityStyle().getCorridorRoofBlock();
        Character corridorGlassBlock = info.getCityStyle().getCorridorGlassBlock();
        CompiledPalette palette = info.getCompiledPalette();

        for (int x = 0; x < 16; ++x) {
            for (int z = 0; z < 16; ++z) {
                char b;
                if ((xRail && z >= 7 && z <= 10) || (zRail && x >= 7 && x <= 10)) {
                    int height = info.groundLevel - 5;
                    if (xRail && z == 10) {
                        b = railxC;
                    } else if (zRail && x == 10) {
                        b = railzC;
                    } else {
                        b = airChar;
                    }
                    driver.current(x, height, z).add(b).add(airChar).add(airChar);

                    if ((xRail && x == 7 && (z == 8 || z == 9)) || (zRail && z == 7 && (x == 8 || x == 9))) {
                        char glass = palette.get(corridorGlassBlock);
                        info.addLightingUpdateTodo(new BlockPos(x, height, z));
                        driver.add(glass).add(glowstoneChar);
                    } else {
                        char roof = palette.get(corridorRoofBlock);
                        driver.add(roof).add(roof);
                    }
                } else {
                    driver.setBlockRange(x, info.groundLevel - 5, z, info.getCityGroundLevel(), baseChar);
                }
            }
        }
    }

    private void generateRandomVegetation(BuildingInfo info, Random rand, int height) {
        if (height == 0) return; // Leaf blocks need to sit on something, and the void isn't something
        if (info.getXmin().hasBuilding) {
            for (int x = 0; x < info.profile.THICKNESS_OF_RANDOM_LEAFBLOCKS; x++) {
                zLoop: for (int z = 0; z < 16; z++) {
                    driver.current(x, height, z);
                    // @todo can be more optimal? Only go down to non air in case random succeeds?
                    while (driver.getBlockDown() == airChar) {
                        driver.decY();
                        if (driver.getY() == 0) {
                            // whoops, it's air all the way down. No leaf blocks here
                            continue zLoop;
                        }
                    }
                    float v = Math.min(.8f, info.profile.CHANCE_OF_RANDOM_LEAFBLOCKS * (info.profile.THICKNESS_OF_RANDOM_LEAFBLOCKS + 1 - x));
                    int cnt = 0;
                    while (rand.nextFloat() < v && cnt < 30) {
                        driver.add(getRandomLeaf());
                        cnt++;
                    }
                }
            }
        }
        if (info.getXmax().hasBuilding) {
            for (int x = 15 - info.profile.THICKNESS_OF_RANDOM_LEAFBLOCKS; x < 15; x++) {
                zLoop: for (int z = 0; z < 16; z++) {
                    driver.current(x, height, z);
                    // @todo can be more optimal? Only go down to non air in case random succeeds?
                    while (driver.getBlockDown() == airChar) {
                        driver.decY();
                        if (driver.getY() == 0) {
                            // whoops, it's air all the way down. No leaf blocks here
                            continue zLoop;
                        }
                    }
                    float v = Math.min(.8f, info.profile.CHANCE_OF_RANDOM_LEAFBLOCKS * (x - 14 + info.profile.THICKNESS_OF_RANDOM_LEAFBLOCKS));
                    int cnt = 0;
                    while (rand.nextFloat() < v && cnt < 30) {
                        driver.add(getRandomLeaf());
                        cnt++;
                    }
                }
            }
        }
        if (info.getZmin().hasBuilding) {
            for (int z = 0; z < info.profile.THICKNESS_OF_RANDOM_LEAFBLOCKS; z++) {
                xLoop: for (int x = 0; x < 16; x++) {
                    driver.current(x, height, z);
                    // @todo can be more optimal? Only go down to non air in case random succeeds?
                    while (driver.getBlockDown() == airChar) {
                        driver.decY();
                        if (driver.getY() == 0) {
                            // whoops, it's air all the way down. No leaf blocks here
                            continue xLoop;
                        }
                    }
                    float v = Math.min(.8f, info.profile.CHANCE_OF_RANDOM_LEAFBLOCKS * (info.profile.THICKNESS_OF_RANDOM_LEAFBLOCKS + 1 - z));
                    int cnt = 0;
                    while (rand.nextFloat() < v && cnt < 30) {
                        driver.add(getRandomLeaf());
                        cnt++;
                    }
                }
            }
        }
        if (info.getZmax().hasBuilding) {
            for (int z = 15 - info.profile.THICKNESS_OF_RANDOM_LEAFBLOCKS; z < 15; z++) {
                xLoop: for (int x = 0; x < 16; x++) {
                    driver.current(x, height, z);
                    // @todo can be more optimal? Only go down to non air in case random succeeds?
                    while (driver.getBlockDown() == airChar) {
                        driver.decY();
                        if (driver.getY() == 0) {
                            // whoops, it's air all the way down. No leaf blocks here
                            continue xLoop;
                        }
                    }
                    float v = info.profile.CHANCE_OF_RANDOM_LEAFBLOCKS * (z - 14 + info.profile.THICKNESS_OF_RANDOM_LEAFBLOCKS);
                    int cnt = 0;
                    while (rand.nextFloat() < v && cnt < 30) {
                        driver.add(getRandomLeaf());
                        cnt++;
                    }
                }
            }
        }
    }

    private void generateParkSection(BuildingInfo info, int height, boolean elevated) {
        char b;
        boolean el00 = info.getXmin().getZmin().isElevatedParkSection();
        boolean el10 = info.getZmin().isElevatedParkSection();
        boolean el20 = info.getXmax().getZmin().isElevatedParkSection();
        boolean el01 = info.getXmin().isElevatedParkSection();
        boolean el21 = info.getXmax().isElevatedParkSection();
        boolean el02 = info.getXmin().getZmax().isElevatedParkSection();
        boolean el12 = info.getZmax().isElevatedParkSection();
        boolean el22 = info.getXmax().getZmax().isElevatedParkSection();
        for (int x = 0; x < 16; ++x) {
            for (int z = 0; z < 16; ++z) {
                if (x == 0 || x == 15 || z == 0 || z == 15) {
                    b = street;
                    if (elevated) {
                        if (x == 0 && z == 0) {
                            if (el01 && el00 && el10) {
                                b = grassChar;
                            }
                        } else if (x == 15 && z == 0) {
                            if (el21 && el20 && el10) {
                                b = grassChar;
                            }
                        } else if (x == 0 && z == 15) {
                            if (el01 && el02 && el12) {
                                b = grassChar;
                            }
                        } else if (x == 15 && z == 15) {
                            if (el12 && el22 && el21) {
                                b = grassChar;
                            }
                        } else if (x == 0) {
                            if (el01) {
                                b = grassChar;
                            }
                        } else if (x == 15) {
                            if (el21) {
                                b = grassChar;
                            }
                        } else if (z == 0) {
                            if (el10) {
                                b = grassChar;
                            }
                        } else if (z == 15) {
                            if (el12) {
                                b = grassChar;
                            }
                        }
                    }
                } else {
                    b = grassChar;
                }
                driver.current(x, height, z).block(b);
            }
        }
    }

    private void generateFullStreetSection(int height) {
        char b;
        for (int x = 0; x < 16; ++x) {
            for (int z = 0; z < 16; ++z) {
                if (isSide(x, z)) {
                    b = street;
                } else {
                    b = street2;
                }
                driver.current(x, height, z).block(b);
            }
        }
    }

    private void generateNormalStreetSection(BuildingInfo info, int height) {
//        char defaultStreet = info.profile.isFloating() ? street2 : streetBase;
        char defaultStreet = streetBase;
        char b;
        for (int x = 0; x < 16; ++x) {
            for (int z = 0; z < 16; ++z) {
                b = defaultStreet;
                if (isStreetBorder(x, z)) {
                    if (x <= streetBorder && z > streetBorder && z < (15 - streetBorder)
                            && (BuildingInfo.hasRoadConnection(info, info.getXmin()) || (info.getXmin().hasXBridge(provider) != null))) {
                        b = street;
                    } else if (x >= (15 - streetBorder) && z > streetBorder && z < (15 - streetBorder)
                            && (BuildingInfo.hasRoadConnection(info, info.getXmax()) || (info.getXmax().hasXBridge(provider) != null))) {
                        b = street;
                    } else if (z <= streetBorder && x > streetBorder && x < (15 - streetBorder)
                            && (BuildingInfo.hasRoadConnection(info, info.getZmin()) || (info.getZmin().hasZBridge(provider) != null))) {
                        b = street;
                    } else if (z >= (15 - streetBorder) && x > streetBorder && x < (15 - streetBorder)
                            && (BuildingInfo.hasRoadConnection(info, info.getZmax()) || (info.getZmax().hasZBridge(provider) != null))) {
                        b = street;
                    }
                } else {
                    b = street;
                }
                driver.current(x, height, z).block(b);
            }
        }
    }

    public void generateHighways(int chunkX, int chunkZ, BuildingInfo info) {
        int levelX = Highway.getXHighwayLevel(chunkX, chunkZ, provider, info.profile);
        int levelZ = Highway.getZHighwayLevel(chunkX, chunkZ, provider, info.profile);
        if (levelX == levelZ && levelX >= 0) {
            // Crossing
            generateHighwayPart(info, levelX, Transform.ROTATE_NONE, info.getXmax(), info.getZmax(), "_bi");
        } else if (levelX >= 0 && levelZ >= 0) {
            // There are two highways on different level. Make sure the lowest one is done first because it
            // will clear out what is above it
            if (levelX == 0) {
                generateHighwayPart(info, levelX, Transform.ROTATE_NONE, info.getZmin(), info.getZmax(), "");
                generateHighwayPart(info, levelZ, Transform.ROTATE_90, info.getXmax(), info.getXmax(), "");
            } else {
                generateHighwayPart(info, levelZ, Transform.ROTATE_90, info.getXmax(), info.getXmax(), "");
                generateHighwayPart(info, levelX, Transform.ROTATE_NONE, info.getZmin(), info.getZmax(), "");
            }
        } else {
            if (levelX >= 0) {
                generateHighwayPart(info, levelX, Transform.ROTATE_NONE, info.getZmin(), info.getZmax(), "");
            } else if (levelZ >= 0) {
                generateHighwayPart(info, levelZ, Transform.ROTATE_90, info.getXmax(), info.getXmax(), "");
            }
        }
    }
}
