package mcjty.lostcities.cubic.world.generators;

import mcjty.lostcities.api.RailChunkType;
import mcjty.lostcities.dimensions.world.lost.BuildingInfo;
import mcjty.lostcities.dimensions.world.lost.CitySphere;
import mcjty.lostcities.dimensions.world.lost.Railway;
import mcjty.lostcities.dimensions.world.lost.Transform;
import mcjty.lostcities.dimensions.world.lost.cityassets.AssetRegistries;
import mcjty.lostcities.dimensions.world.lost.cityassets.BuildingPart;
import net.minecraft.init.Blocks;

import java.util.HashSet;
import java.util.Set;

import static mcjty.lostcities.cubic.world.LostCityCubicGenerator.*;
import static mcjty.lostcities.cubic.world.generators.PartGenerator.generatePart;
import static mcjty.lostcities.cubic.world.generators.States.addStates;
import static mcjty.lostcities.cubic.world.generators.Utils.fillToGround;

import static mcjty.lostcities.cubic.CubicCityWorldProcessor.driver;

public class RailsGenerator {
    private static Set<Character> railChars = null;

    public static Set<Character> getRailChars() {
        if (railChars == null) {
            railChars = new HashSet<>();
            addStates(Blocks.RAIL, railChars);
            addStates(Blocks.GOLDEN_RAIL, railChars);
        }
        return railChars;
    }

    public void generateRailwayDungeons(BuildingInfo info) {
        if (info.railDungeon == null) {
            return;
        }
        if (info.getZmin().getRailInfo().getType() == RailChunkType.HORIZONTAL ||
                info.getZmax().getRailInfo().getType() == RailChunkType.HORIZONTAL) {
            int height = info.groundLevel + Railway.RAILWAY_LEVEL_OFFSET * 6;
            generatePart(info, info.railDungeon, Transform.ROTATE_NONE, 0, height, 0, false);
        }
    }

    public void generateRailways(BuildingInfo info, Railway.RailChunkInfo railInfo) {
        int height = info.groundLevel + railInfo.getLevel() * 6;
        RailChunkType type = railInfo.getType();
        BuildingPart part;
        Transform transform = Transform.ROTATE_NONE;
        boolean needsStaircase = false;
        switch (type) {
            case NONE:
                return;
            case STATION_SURFACE:
            case STATION_EXTENSION_SURFACE:
                if (railInfo.getLevel() < info.cityLevel) {
                    // Even for a surface station extension we switch to underground if we are an extension
                    // that is at a spot where the city is higher then where the station is
                    part = AssetRegistries.PARTS.get("station_underground");
                } else {
                    if (railInfo.getPart() != null) {
                        part = AssetRegistries.PARTS.get(railInfo.getPart());
                    } else {
                        part = AssetRegistries.PARTS.get("station_open");
                    }
                }
                break;
            case STATION_UNDERGROUND:
                part = AssetRegistries.PARTS.get("station_underground_stairs");
                needsStaircase = true;
                break;
            case STATION_EXTENSION_UNDERGROUND:
                part = AssetRegistries.PARTS.get("station_underground");
                break;
            case RAILS_END_HERE:
                part = AssetRegistries.PARTS.get("rails_horizontal_end");
                if (railInfo.getDirection() == Railway.RailDirection.EAST) {
                    transform = Transform.MIRROR_X;
                }
                break;
            case HORIZONTAL:
                part = AssetRegistries.PARTS.get("rails_horizontal");

                // If the adjacent chunks are also horizontal we take a sample of the blocks around us to see if we are in water
                RailChunkType type1 = info.getXmin().getRailInfo().getType();
                RailChunkType type2 = info.getXmax().getRailInfo().getType();
                if (!type1.isStation() && !type2.isStation()) {
                    if (driver.getBlock(3, height + 2, 3) == liquidChar &&
                            driver.getBlock(12, height + 2, 3) == liquidChar &&
                            driver.getBlock(3, height + 2, 12) == liquidChar &&
                            driver.getBlock(12, height + 2, 12) == liquidChar &&
                            driver.getBlock(3, height + 4, 7) == liquidChar &&
                            driver.getBlock(12, height + 4, 8) == liquidChar) {
                        part = AssetRegistries.PARTS.get("rails_horizontal_water");
                    }
                }
                break;
            case VERTICAL:
                part = AssetRegistries.PARTS.get("rails_vertical");
                if (driver.getBlock(3, height + 2, 3) == liquidChar &&
                        driver.getBlock(12, height + 2, 3) == liquidChar &&
                        driver.getBlock(3, height + 2, 12) == liquidChar &&
                        driver.getBlock(12, height + 2, 12) == liquidChar &&
                        driver.getBlock(3, height + 4, 7) == liquidChar &&
                        driver.getBlock(12, height + 4, 8) == liquidChar) {
                    part = AssetRegistries.PARTS.get("rails_vertical_water");
                }
                if (railInfo.getDirection() == Railway.RailDirection.EAST) {
                    transform = Transform.MIRROR_X;
                }
                break;
            case THREE_SPLIT:
                part = AssetRegistries.PARTS.get("rails_3split");
                if (railInfo.getDirection() == Railway.RailDirection.EAST) {
                    transform = Transform.MIRROR_X;
                }
                break;
            case GOING_DOWN_TWO_FROM_SURFACE:
            case GOING_DOWN_FURTHER:
                part = AssetRegistries.PARTS.get("rails_down2");
                if (railInfo.getDirection() == Railway.RailDirection.EAST) {
                    transform = Transform.MIRROR_X;
                }
                break;
            case GOING_DOWN_ONE_FROM_SURFACE:
                part = AssetRegistries.PARTS.get("rails_down1");
                if (railInfo.getDirection() == Railway.RailDirection.EAST) {
                    transform = Transform.MIRROR_X;
                }
                break;
            case DOUBLE_BEND:
                part = AssetRegistries.PARTS.get("rails_bend");
                if (railInfo.getDirection() == Railway.RailDirection.EAST) {
                    transform = Transform.MIRROR_X;
                }
                break;
            default:
                part = AssetRegistries.PARTS.get("rails_flat");
                break;
        }
        generatePart(info, part, transform, 0, height, 0, false);

        Character railMainBlock = info.getCityStyle().getRailMainBlock();
        char rail = info.getCompiledPalette().get(railMainBlock);

        if (type == RailChunkType.HORIZONTAL) {
            // If there is a rail dungeon north or south we must make a connection here
            if (info.getZmin().railDungeon != null) {
                for (int z = 0; z < 4; z++) {
                    driver.current(6, height + 1, z).add(rail).add(airChar).add(airChar);
                    driver.current(7, height + 1, z).add(rail).add(airChar).add(airChar);
                }
                for (int z = 0; z < 3; z++) {
                    driver.current(5, height+2, z).add(rail).add(rail).add(rail);

                    driver.current(6, height+4, z).block(rail);
                    driver.current(7, height+4, z).block(rail);

                    driver.current(8, height+2, z).add(rail).add(rail).add(rail);
                }
            }

            if (info.getZmax().railDungeon != null) {
                for (int z = 0; z < 5; z++) {
                    driver.current(6, height+1, 15-z).add(rail).add(airChar).add(airChar);
                    driver.current(7, height+1, 15-z).add(rail).add(airChar).add(airChar);
                }
                for (int z = 0; z < 4; z++) {
                    driver.current(5, height+2, 15-z).add(rail).add(rail).add(rail);

                    driver.current(6, height+4, 15-z).block(rail);

                    driver.current(7, height+4, 15-z).block(rail);

                    driver.current(8, height+2, 15-z).add(rail).add(rail).add(rail);
                }
            }
        }

        if (railInfo.getRails() < 3) {
            // We may have to reduce number of rails
            int index;
            switch (railInfo.getType()) {
                case NONE:
                    break;
                case STATION_SURFACE:
                case STATION_UNDERGROUND:
                case STATION_EXTENSION_SURFACE:
                case STATION_EXTENSION_UNDERGROUND:
                case HORIZONTAL: {
                    if (railInfo.getRails() == 1) {
                        driver.current(0, height+1, 5);
                        for (int x = 0; x < 16; x++) {
                            driver.block(rail).incX();
                        }
                        driver.current(0, height+1, 9);
                        for (int x = 0; x < 16; x++) {
                            driver.block(rail).incX();
                        }
                    } else {
                        driver.current(0, height+1, 7);
                        for (int x = 0; x < 16; x++) {
                            driver.block(rail).incX();
                        }
                    }
                    break;
                }
                case GOING_DOWN_TWO_FROM_SURFACE:
                case GOING_DOWN_ONE_FROM_SURFACE:
                case GOING_DOWN_FURTHER:
                    if (railInfo.getRails() == 1) {
                        for (int x = 0; x < 16; x++) {
                            for (int y = height + 1; y < height + part.getSliceCount(); y++) {
                                driver.current(x, y, 5);
                                if (getRailChars().contains(driver.getBlock())) {
                                    driver.block(rail);
                                }
                                driver.current(x, y, 9);;
                                if (getRailChars().contains(driver.getBlock())) {
                                    driver.block(rail);
                                }
                            }
                        }
                    } else {
                        for (int x = 0; x < 16; x++) {
                            for (int y = height + 1; y < height + part.getSliceCount(); y++) {
                                driver.current(x, y, 7);
                                if (getRailChars().contains(driver.getBlock())) {
                                    driver.block(rail);
                                }
                            }
                        }
                    }
                    break;
                case THREE_SPLIT:
                    break;
                case VERTICAL:
                    break;
                case DOUBLE_BEND:
                    break;
            }
        }

        if (needsStaircase) {
            part = AssetRegistries.PARTS.get("station_staircase");
            for (int i = railInfo.getLevel() + 1; i < info.cityLevel; i++) {
                height = info.groundLevel + i * 6;
                generatePart(info, part, transform, 0, height, 0, false);
            }
            height = info.groundLevel + info.cityLevel * 6;
            part = AssetRegistries.PARTS.get("station_staircase_surface");
            generatePart(info, part, transform, 0, height, 0, false);
        }
    }

    public void generateMonorails(BuildingInfo info) {
        Transform transform;
        boolean horiz = info.hasHorizontalMonorail();
        boolean vert = info.hasVerticalMonorail();
        if (horiz && vert) {
            if (!CitySphere.intersectsWithCitySphere(info.chunkX, info.chunkZ, provider)) {
                BuildingPart part = AssetRegistries.PARTS.get("monorails_both");
                generatePart(info, part, Transform.ROTATE_NONE, 0, getMainGroundLevel() + info.profile.CITYSPHERE_MONORAIL_HEIGHT_OFFSET, 0, true);
            }
            return;
        } else if (horiz) {
            transform = Transform.ROTATE_90;
        } else if (vert) {
            transform = Transform.ROTATE_NONE;
        } else {
            return;
        }
        BuildingPart part;

        if (CitySphere.fullyInsideCitySpere(info.chunkX, info.chunkZ, provider)) {
            // If there is a non enclosed monorail nearby we generate a station
            if (hasNonStationMonoRail(info.getXmin())) {
                part = AssetRegistries.PARTS.get("monorails_station");
                Character borderBlock = info.getCityStyle().getBorderBlock();
                transform = Transform.MIRROR_90_X; // flip
                fillToGround(info, getMainGroundLevel() + info.profile.CITYSPHERE_MONORAIL_HEIGHT_OFFSET, borderBlock);
            } else if (hasNonStationMonoRail(info.getXmax())) {
                part = AssetRegistries.PARTS.get("monorails_station");
                Character borderBlock = info.getCityStyle().getBorderBlock();
                transform = Transform.ROTATE_90;
                fillToGround(info, getMainGroundLevel() + info.profile.CITYSPHERE_MONORAIL_HEIGHT_OFFSET, borderBlock);
            } else if (hasNonStationMonoRail(info.getZmin())) {
                part = AssetRegistries.PARTS.get("monorails_station");
                Character borderBlock = info.getCityStyle().getBorderBlock();
                transform = Transform.ROTATE_NONE;
                fillToGround(info, getMainGroundLevel() + info.profile.CITYSPHERE_MONORAIL_HEIGHT_OFFSET, borderBlock);
            } else if (hasNonStationMonoRail(info.getZmax())) {
                part = AssetRegistries.PARTS.get("monorails_station");
                Character borderBlock = info.getCityStyle().getBorderBlock();
                transform = Transform.MIRROR_Z; // flip
                fillToGround(info, getMainGroundLevel() + info.profile.CITYSPHERE_MONORAIL_HEIGHT_OFFSET, borderBlock);
            } else {
                return;
            }
        } else {
            part = AssetRegistries.PARTS.get("monorails_vertical");
        }

        generatePart(info, part, transform, 0, getMainGroundLevel() + info.profile.CITYSPHERE_MONORAIL_HEIGHT_OFFSET, 0, true);
    }

    private boolean hasNonStationMonoRail(BuildingInfo info) {
        return info.hasMonorail() && !CitySphere.fullyInsideCitySpere(info.chunkX, info.chunkZ, provider);
    }
    
    private static int getMainGroundLevel() {
        return profile.GROUNDLEVEL;
    }
}
