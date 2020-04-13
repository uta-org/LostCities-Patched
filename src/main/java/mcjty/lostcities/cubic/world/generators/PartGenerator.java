package mcjty.lostcities.cubic.world.generators;

import mcjty.lostcities.dimensions.world.lost.BuildingInfo;
import mcjty.lostcities.dimensions.world.lost.Transform;
import mcjty.lostcities.dimensions.world.lost.cityassets.*;
import net.minecraft.block.*;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;

import java.util.Map;

import static mcjty.lostcities.cubic.world.generators.Utils.clearRange;

import static mcjty.lostcities.cubic.world.CubicCityWorldProcessor.driver;

import static mcjty.lostcities.cubic.world.CubicCityUtils.*;
import static mcjty.lostcities.cubic.world.generators.Utils.isSide;

public class PartGenerator {
    /**
     * Generate a part. If 'airWaterLevel' is true then 'hard air' blocks are replaced with water below the waterLevel.
     * Otherwise they are replaced with air.
     */
    public static int generatePart(BuildingInfo info, IBuildingPart part,
                                   Transform transform,
                                   int ox, int oy, int oz, boolean airWaterLevel) {
        CompiledPalette compiledPalette = info.getCompiledPalette();
        // Cache the combined palette?
        Palette localPalette = part.getLocalPalette();
        if (localPalette != null) {
            compiledPalette = new CompiledPalette(compiledPalette, localPalette);
        }

        boolean nowater = part.getMetaBoolean("nowater");

        for (int x = 0; x < part.getXSize(); x++) {
            for (int z = 0; z < part.getZSize(); z++) {
                char[] vs = part.getVSlice(x, z);
                if (vs != null) {
                    if(isSide(x, z) && info.isFloor() && info.getCurrentFloor() < 0) continue;
                    int rx = ox + transform.rotateX(x, z);
                    int rz = oz + transform.rotateZ(x, z);
                    driver.current(rx, oy, rz);
                    int len = vs.length;
                    for (int y = 0; y < len; y++) {
                        char c = vs[y];
                        Character b = compiledPalette.get(c);
                        if (b == null) {
                            throw new RuntimeException("Could not find entry '" + c + "' in the palette for part '" + part.getName() + "'!");
                        }

                        IBlockState bs = Block.BLOCK_STATE_IDS.getByValue(b);
                        if (bs == null) {
                            System.out.println("Null block state!");
                            continue;
                        }

                        CompiledPalette.Info inf = compiledPalette.getInfo(c);

                        if (transform != Transform.ROTATE_NONE) {
                            if (BuildingGenerator.getRotatableChars().contains(b)) {
                                bs = bs.withRotation(transform.getMcRotation());
                                b = (char) Block.BLOCK_STATE_IDS.get(bs);
                            } else if (BuildingGenerator.getRailChars().contains(b)) {
                                PropertyEnum<BlockRailBase.EnumRailDirection> shapeProperty;
                                if (bs.getBlock() == Blocks.RAIL) {
                                    shapeProperty = BlockRail.SHAPE;
                                } else if (bs.getBlock() == Blocks.GOLDEN_RAIL) {
                                    shapeProperty = BlockRailPowered.SHAPE;
                                } else {
                                    throw new RuntimeException("Error with rail!");
                                }
                                BlockRailBase.EnumRailDirection shape = bs.getValue(shapeProperty);
                                bs = bs.withProperty(shapeProperty, transform.transform(shape));
                                b = (char) Block.BLOCK_STATE_IDS.get(bs);
                            }
                        }
                        // We don't replace the world where the part is empty (air)
                        if (b != airChar) {
                            if (b == liquidChar) {
                                if (info.profile.AVOID_WATER) {
                                    b = airChar;
                                }
                            } else if (b == hardAirChar) {
                                if (airWaterLevel && !info.profile.AVOID_WATER && !nowater) {
                                    b = (oy + y) < info.waterLevel ? liquidChar : airChar;
                                } else {
                                    b = airChar;
                                }
                            } else if (inf != null) {
                                Map<String, Integer> orientations = inf.getTorchOrientations();
                                if (orientations != null) {
                                    if (info.profile.GENERATE_LIGHTING) {
                                        info.addTorchTodo(driver.getCurrent(), orientations);
                                    } else {
                                        b = airChar;        // No torches
                                    }
                                } else if (inf.getLoot() != null && !inf.getLoot().isEmpty()) {
                                    if (!info.noLoot) {
                                        info.getTodoChunk(rx, rz).addLootTodo(new BlockPos(info.chunkX * 16 + rx, oy + y, info.chunkZ * 16 + rz),
                                                new BuildingInfo.ConditionTodo(inf.getLoot(), part.getName(), info));
                                    }
                                } else if (inf.getMobId() != null && !inf.getMobId().isEmpty()) {
                                    if (info.profile.GENERATE_SPAWNERS && !info.noLoot) {
                                        String mobid = inf.getMobId();
                                        info.getTodoChunk(rx, rz).addSpawnerTodo(new BlockPos(info.chunkX * 16 + rx, oy + y, info.chunkZ * 16 + rz),
                                                new BuildingInfo.ConditionTodo(mobid, part.getName(), info));
                                    } else {
                                        b = airChar;
                                    }
                                }
                            } else if (BuildingGenerator.getCharactersNeedingLightingUpdate().contains(b)) {
                                info.getTodoChunk(rx, rz).addLightingUpdateTodo(new BlockPos(info.chunkX * 16 + rx, oy + y, info.chunkZ * 16 + rz));
                            } else if (BuildingGenerator.getCharactersNeedingTodo().contains(b)) {
                                Block block = bs.getBlock();
                                if (block instanceof BlockSapling || block instanceof BlockFlower) {
                                    if (info.profile.AVOID_FOLIAGE) {
                                        b = airChar;
                                    } else {
                                        info.getTodoChunk(rx, rz).addSaplingTodo(new BlockPos(info.chunkX * 16 + rx, oy + y, info.chunkZ * 16 + rz));
                                    }
                                }
                            }

                            String bName = bs.getBlock().getUnlocalizedName();
                            if(bName.contains("ladder")) {
                                info.addLadderTodo(driver.current());
                            }
                            else
                                driver.add(b);
                        } else {
                            // Set air blocks also (to avoid grass inside buildings)
                            driver.add(airChar);
                        }
                    }
                }
            }
        }
        return oy + part.getSliceCount();
    }


    public static void generateFrontPart(BuildingInfo info, int height, BuildingInfo adj, Transform rot) {
        if (info.hasFrontPartFrom(adj)) {
            generatePart(adj, adj.frontType, rot, 0, height, 0, false);
        }
    }

    public static void generateHighwayPart(BuildingInfo info, int level, Transform transform, BuildingInfo adjacent1, BuildingInfo adjacent2, String suffix) {
        int highwayGroundLevel = info.groundLevel + level * 6;

        BuildingPart part;
        if (info.isTunnel(level)) {
            // We know we need a tunnel
            part = AssetRegistries.PARTS.get("highway_tunnel" + suffix);
            generatePart(info, part, transform, 0, highwayGroundLevel, 0, true);
        } else if (info.isCity && level <= adjacent1.cityLevel && level <= adjacent2.cityLevel && adjacent1.isCity && adjacent2.isCity) {
            // Simple highway in the city
            part = AssetRegistries.PARTS.get("highway_open" + suffix);
            int height = generatePart(info, part, transform, 0, highwayGroundLevel, 0, true);
            // Clear a bit more above the highway
            if (!info.profile.isCavern()) {
                int clearheight = 15;
                for (int x = 0; x < 16; x++) {
                    for (int z = 0; z < 16; z++) {
                        clearRange(info, x, z, height, height + clearheight, info.waterLevel > info.groundLevel);
                    }
                }
            }
        } else {
            part = AssetRegistries.PARTS.get("highway_bridge" + suffix);
            int height = generatePart(info, part, transform, 0, highwayGroundLevel, 0, true);
            // Clear a bit more above the highway
            if (!info.profile.isCavern()) {
                int clearheight = 15;
                for (int x = 0; x < 16; x++) {
                    for (int z = 0; z < 16; z++) {
                        clearRange(info, x, z, height, height + clearheight, info.waterLevel > info.groundLevel);
                    }
                }
            }
        }

        Character support = part.getMetaChar("support");
        if (info.profile.HIGHWAY_SUPPORTS && support != null) {
            char sup = info.getCompiledPalette().get(support);
            int x1 = transform.rotateX(0, 15);
            int z1 = transform.rotateZ(0, 15);
            driver.current(x1, highwayGroundLevel - 1, z1);
            for (int y = 0; y < 40; y++) {
                if (driver.getBlock() == airChar || driver.getBlock() == liquidChar) {
                    driver.block(sup);
                } else {
                    break;
                }
                driver.decY();
            }

            int x2 = transform.rotateX(0, 0);
            int z2 = transform.rotateZ(0, 0);
            driver.current(x2, highwayGroundLevel - 1, z2);
            for (int y = 0; y < 40; y++) {
                if (driver.getBlock() == airChar || driver.getBlock() == liquidChar) {
                    driver.block(sup);
                } else {
                    break;
                }
                driver.decY();
            }
        }
    }

}
