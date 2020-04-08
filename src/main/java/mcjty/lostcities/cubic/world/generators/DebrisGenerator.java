package mcjty.lostcities.cubic.world.generators;

import mcjty.lostcities.cubic.world.CubicCityUtils;
import mcjty.lostcities.cubic.world.CubicCityWorldProcessor;
import mcjty.lostcities.cubic.world.ICommonGeneratorProvider;
import mcjty.lostcities.cubic.world.driver.ICubeDriver;
import mcjty.lostcities.dimensions.world.LostCityChunkGenerator;
import mcjty.lostcities.dimensions.world.driver.IIndex;
import mcjty.lostcities.dimensions.world.driver.IPrimerDriver;
import mcjty.lostcities.dimensions.world.lost.BuildingInfo;
import mcjty.lostcities.dimensions.world.lost.DamageArea;
import mcjty.lostcities.dimensions.world.terraingen.LostCitiesTerrainGenerator;
import net.minecraft.util.math.BlockPos;

import java.util.*;
import java.util.function.BiFunction;

import static mcjty.lostcities.cubic.world.CubicCityUtils.*;
import static mcjty.lostcities.cubic.world.CubicCityWorldProcessor.*;
import static mcjty.lostcities.cubic.world.CubicCityWorldPopulator.*;

public class DebrisGenerator {
    public void breakBlocksForDamage(int chunkX, int chunkZ, BuildingInfo info) {
        int cx = chunkX * 16;
        int cz = chunkZ * 16;

        DamageArea damageArea = info.getDamageArea();

        boolean clear = false;
        float damageFactor = 1.0f;

        for (int yy = 0; yy < 16; yy++) {
            if (clear || damageArea.hasExplosions(yy)) {
                if (clear || damageArea.isCompletelyDestroyed(yy)) {
                    for (int x = 0; x < 16; x++) {
                        for (int z = 0; z < 16; z++) {
                            int height = yy * 16;
                            driver.current(x, height, z);
                            for (int y = 0; y < 16; y++) {
                                driver.add(((height + y) <= info.waterLevel) ? liquidChar : airChar);
                            }
                        }
                    }
                    // All further subchunks will also be totally cleared
                    clear = true;
                } else {
                    for (int y = 0; y < 16; y++) {
                        int cntDamaged = 0;
                        int cntAir = 0;
                        int cury = yy * 16 + y;
                        for (int x = 0; x < 16; x++) {
                            driver.current(x, cury, 0);
                            for (int z = 0; z < 16; z++) {
                                char d = driver.getBlock();
                                if (d != airChar || cury <= info.waterLevel) {
                                    float damage = damageArea.getDamage(cx + x, cury, cz + z) * damageFactor;
                                    if (damage >= 0.001) {
                                        Character newd = damageArea.damageBlock(d, provider, cury, damage, info.getCompiledPalette(), liquidChar);
                                        if (newd != d) {
                                            driver.block(newd);
                                            cntDamaged++;
                                        }
                                    }
                                } else {
                                    cntAir++;
                                }
                                driver.incZ();
                            }
                        }

                        int tot = cntDamaged + cntAir;
                        if (tot > 250) {
                            damageFactor = 200;
                            clear = true;
                        } else if (tot > 220) {
                            damageFactor = damageFactor * 1.4f;
                        } else if (tot > 180) {
                            damageFactor = damageFactor * 1.2f;
                        }

                    }

//                    for (int x = 0; x < 16; x++) {
//                        for (int z = 0; z < 16; z++) {
//                            int index = (x << 12) | (z << 8) + yy * 16;
//                            for (int y = 0; y < 16; y++) {
//                                char d = primer.data[index];
//                                if (d != airChar || (index & 0xff) < waterLevel) {
//                                    int cury = yy * 16 + y;
//                                    float damage = damageArea.getDamage(cx + x, cury, cz + z);
//                                    if (damage >= 0.001) {
//                                        Character newd = damageArea.damageBlock(d, provider, cury, damage, info.getCompiledPalette());
//                                        if (newd != d) {
//                                            primer.data[index] = newd;
//                                        }
//                                    }
//                                }
//                                index++;
//                            }
//                        }
//                    }
                }
            }
        }
    }

    /// Fix floating blocks after an explosion
    public void fixAfterExplosionNew(BuildingInfo info, Random rand) {
        int start = info.getDamageArea().getLowestExplosionHeight();
        if (start == -1) {
            // Nothing is affected
            return;
        }
        int end = info.getDamageArea().getHighestExplosionHeight();

        List<DebrisGenerator.Blob> blobs = new ArrayList<>();

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                driver.current(x, start, z);
                for (int y = start; y < end; y++) {
                    char p = driver.getBlock();
                    if (p != airChar && p != liquidChar) {
                        DebrisGenerator.Blob blob = findBlob(blobs, driver.getCurrent());
                        if (blob == null) {
                            blob = new DebrisGenerator.Blob(start, end + 6);
                            // We must make a copy of the driver here so that we can safely modify it
                            blob.scan(info, driver.copy(), airChar, liquidChar, new BlockPos(x, y, z));
                            blobs.add(blob);
                        }
                    }
                    driver.incY();
                }
            }
        }

//        // Split large blobs that have very thin connections in Y direction
//        for (Blob blob : blobs) {
//            if (blob.getAvgdamage() > .3f && blob.getCntMindamage() < 10) { // @todo configurable?
//                int y = blob.needsSplitting();
//                if (y != -1) {
//                    Set<Integer> toRemove = blob.cut(y);
//                    for (Integer index : toRemove) {
//                        primer.data[index] = ((index & 0xff) < waterLevel) ? liquidChar : airChar;
//                    }
//                }
//            }
//        }

        DebrisGenerator.Blob blocksToMove = new DebrisGenerator.Blob(0, 256);

        // Sort all blobs we delete with lowest first
        blobs.stream().filter(blob -> blob.destroyOrMoveThis(provider, info)).sorted().forEachOrdered(blob -> {
            if (rand.nextFloat() < info.profile.DESTROY_OR_MOVE_CHANCE || blob.connectedBlocks.size() < info.profile.DESTROY_SMALL_SECTIONS_SIZE
                    || blob.connections < 5) {
                for (IIndex index : blob.connectedBlocks) {
                    driver.current(index);
                    driver.block(((driver.getY()) < info.waterLevel) ? liquidChar : airChar);
                }
            } else {
                blocksToMove.connectedBlocks.addAll(blob.connectedBlocks);
            }
        });
        for (IIndex index : blocksToMove.connectedBlocks) {
            driver.current(index);
            char c = driver.getBlock();
            driver.block(((driver.getY()) < info.waterLevel) ? liquidChar : airChar);
            driver.decY();
            int y = driver.getY();
            while (y > 2 && (blocksToMove.contains(driver.getCurrent()) || driver.getBlock() == airChar || driver.getBlock() == liquidChar)) {
                driver.decY();
                y--;
            }
            driver.incY();
            driver.block(c);
        }
    }

    public void generateDebris(Random rand, BuildingInfo info) {
        generateDebrisFromChunk(rand, info, info.getXmin(), (xx, zz) -> (15.0f - xx) / 16.0f);
        generateDebrisFromChunk(rand, info, info.getXmax(), (xx, zz) -> xx / 16.0f);
        generateDebrisFromChunk(rand, info, info.getZmin(), (xx, zz) -> (15.0f - zz) / 16.0f);
        generateDebrisFromChunk(rand, info, info.getZmax(), (xx, zz) -> zz / 16.0f);
        generateDebrisFromChunk(rand, info, info.getXmin().getZmin(), (xx, zz) -> ((15.0f - xx) * (15.0f - zz)) / 256.0f);
        generateDebrisFromChunk(rand, info, info.getXmax().getZmax(), (xx, zz) -> (xx * zz) / 256.0f);
        generateDebrisFromChunk(rand, info, info.getXmin().getZmax(), (xx, zz) -> ((15.0f - xx) * zz) / 256.0f);
        generateDebrisFromChunk(rand, info, info.getXmax().getZmin(), (xx, zz) -> (xx * (15.0f - zz)) / 256.0f);
    }

    private void generateDebrisFromChunk(Random rand, BuildingInfo info, BuildingInfo adjacentInfo, BiFunction<Integer, Integer, Float> locationFactor) {
        if (adjacentInfo.hasBuilding) {
            char filler = adjacentInfo.getCompiledPalette().get(adjacentInfo.getBuilding().getFillerBlock());
            float damageFactor = adjacentInfo.getDamageArea().getDamageFactor();
            if (damageFactor > .5f) {
                // An estimate of the amount of blocks
                int blocks = (1 + adjacentInfo.getNumFloors()) * 1000;
                float damage = Math.max(1.0f, damageFactor * DamageArea.BLOCK_DAMAGE_CHANCE);
                int destroyedBlocks = (int) (blocks * damage);
                // How many go this direction (approx, based on cardinal directions from building as well as number that simply fall down)
                destroyedBlocks /= info.profile.DEBRIS_TO_NEARBYCHUNK_FACTOR;
                int h = adjacentInfo.getMaxHeight() + 10;
                for (int i = 0; i < destroyedBlocks; i++) {
                    int x = rand.nextInt(16);
                    int z = rand.nextInt(16);
                    if (rand.nextFloat() < locationFactor.apply(x, z)) {
                        driver.current(x, h, z);
                        while (h > 0 && (driver.getBlock() == airChar || driver.getBlock() == liquidChar)) {
                            h--;
                            driver.decY();
                        }
                        // Fix for FLOATING // @todo!
                        char b;
                        switch (rand.nextInt(5)) {
                            case 0:
                                b = ironbarsChar;
                                break;
                            default:
                                b = filler;     // Filler from adjacent building
                                break;
                        }
                        driver.current(x, h+1, z).block(b);
                    }
                }
            }
        }
    }

    private static class Blob implements Comparable<DebrisGenerator.Blob> {
        private final int starty;
        private final int endy;
        private final Set<IIndex> connectedBlocks = new HashSet<>();
        private final Map<Integer, Integer> blocksPerY = new HashMap<>();
        private int connections = 0;
        private int lowestY;
        private int highestY;
        private float avgdamage;
        private int cntMindamage;  // Number of blocks that receive almost no damage

        public Blob(int starty, int endy) {
            this.starty = starty;
            this.endy = endy;
            lowestY = 256;
            highestY = 0;
        }

        public float getAvgdamage() {
            return avgdamage;
        }

        public int getCntMindamage() {
            return cntMindamage;
        }

        public boolean contains(IIndex index) {
            return connectedBlocks.contains(index);
        }

        public int getLowestY() {
            return lowestY;
        }

        public int getHighestY() {
            return highestY;
        }

        //        public Set<IIndex> cut(int y) {
//            Set<IIndex> toRemove = new HashSet<>();
//            for (IIndex block : connectedBlocks) {
//                if ((block.getY()) >= y) {
//                    toRemove.add(block);
//                }
//            }
//            connectedBlocks.removeAll(toRemove);
//            return toRemove;
//        }
//
        public int needsSplitting() {
            float averageBlocksPerLevel = (float) connectedBlocks.size() / (highestY - lowestY + 1);
            int connectionThresshold = (int) (averageBlocksPerLevel / 10);
            if (connectionThresshold <= 0) {
                // Too small to split
                return -1;
            }
            int cuttingY = -1;      // Where we will cut
            int cuttingCount = 1000000;
            for (int y = lowestY; y <= highestY; y++) {
                if (y >= 3 && blocksPerY.get(y) <= connectionThresshold) {
                    if (blocksPerY.get(y) < cuttingCount) {
                        cuttingCount = blocksPerY.get(y);
                        cuttingY = y;
                    } else if (blocksPerY.get(y) > cuttingCount * 4) {
                        return cuttingY;
                    }
                }
            }
            return -1;
        }

        public boolean destroyOrMoveThis(ICommonGeneratorProvider provider, BuildingInfo info) {
            return connections < 5 || (((float) connections / connectedBlocks.size()) < info.profile.DESTROY_LONE_BLOCKS_FACTOR);
        }

        private boolean isOutside(BuildingInfo info, int x, int y, int z) {
            if (x < 0) {
                if (y <= info.getXmin().getMaxHeight() + 3) {
                    connections++;
                }
                return true;
            }
            if (x > 15) {
                if (y <= info.getXmax().getMaxHeight() + 3) {
                    connections++;
                }
                return true;
            }
            if (z < 0) {
                if (y <= info.getZmin().getMaxHeight() + 3) {
                    connections++;
                }
                return true;
            }
            if (z > 15) {
                if (y <= info.getZmax().getMaxHeight() + 3) {
                    connections++;
                }
                return true;
            }
            if (y < starty) {
                connections += 5;
                return true;
            }
            return false;
        }

        public void scan(BuildingInfo info, ICubeDriver driver, char air, char liquid, BlockPos pos) {
            DamageArea damageArea = info.getDamageArea();
            avgdamage = 0;
            cntMindamage = 0;
            Queue<BlockPos> todo = new ArrayDeque<>();
            todo.add(pos);

            while (!todo.isEmpty()) {
                pos = todo.poll();
                int x = pos.getX();
                int y = pos.getY();
                int z = pos.getZ();
                IIndex index = driver.getIndex(x, y, z);
                if (connectedBlocks.contains(index)) {
                    continue;
                }
                if (isOutside(info, x, y, z)) {
                    continue;
                }
                driver.current(x, y, z);
                if (driver.getBlock() == air || driver.getBlock() == liquid) {
                    continue;
                }
                connectedBlocks.add(index);
                float damage = damageArea.getDamage(x, y, z);
                if (damage < 0.01f) {
                    cntMindamage++;
                }
                avgdamage += damage;
                if (!blocksPerY.containsKey(y)) {
                    blocksPerY.put(y, 1);
                } else {
                    blocksPerY.put(y, blocksPerY.get(y) + 1);
                }
                if (y < lowestY) {
                    lowestY = y;
                }
                if (y > highestY) {
                    highestY = y;
                }
                todo.add(pos.up());
                todo.add(pos.down());
                todo.add(pos.east());
                todo.add(pos.west());
                todo.add(pos.south());
                todo.add(pos.north());
            }

            avgdamage /= (float) connectedBlocks.size();
        }

        @Override
        public int compareTo(DebrisGenerator.Blob o) {
            return this.lowestY - o.lowestY;
        }
    }

    private DebrisGenerator.Blob findBlob(List<DebrisGenerator.Blob> blobs, IIndex index) {
        for (DebrisGenerator.Blob blob : blobs) {
            if (blob.contains(index)) {
                return blob;
            }
        }
        return null;
    }
}
