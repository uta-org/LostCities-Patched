package mcjty.lostcities.cubic.world.driver;

import io.github.opencubicchunks.cubicchunks.api.worldgen.CubePrimer;
import mcjty.lostcities.dimensions.world.driver.IIndex;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;

import java.util.Objects;

public class CubeDriver implements ICubeDriver {
    private CubePrimer primer;
    private int currentX;
    private int currentY;
    private int currentZ;

    @Override
    public void setPrimer(CubePrimer primer) {
        this.primer = primer;
    }

    @Override
    public CubePrimer getPrimer() {
        return primer;
    }

    @Override
    public ICubeDriver current(int x, int y, int z) {
        currentX = x;
        currentY = y;
        currentZ = z;
        return this;
    }

    @Override
    public ICubeDriver current(IIndex index) {
        CubeDriver.Index i = (CubeDriver.Index) index;
        currentX = i.x;
        currentY = i.y;
        currentZ = i.z;
        return this;
    }

    @Override
    public IIndex getCurrent() {
        return new CubeDriver.Index(currentX, currentY, currentZ);
    }

    @Override
    public void incY() {
        currentY++;
    }

    @Override
    public void incY(int amount) {
        currentY += amount;
    }

    @Override
    public void decY() {
        currentY--;
    }

    @Override
    public void incX() {
        currentX++;
    }

    @Override
    public void incZ() {
        currentZ++;
    }

    @Override
    public int getX() {
        return currentX;
    }

    @Override
    public int getY() {
        return currentY;
    }

    @Override
    public int getZ() {
        return currentZ;
    }

    @Override
    public void setBlockRange(int x, int y, int z, int y2, char c) {
        IBlockState state = Block.BLOCK_STATE_IDS.getByValue(c);
        while (y < y2) {
            primer.setBlockState(x, y, z, state);
            y++;
        }
    }

    @Override
    public void setBlockRangeSafe(int x, int y, int z, int y2, char c) {
        IBlockState state = Block.BLOCK_STATE_IDS.getByValue(c);
        while (y < y2) {
            primer.setBlockState(x, y, z, state);
            y++;
        }
    }

    @Override
    public ICubeDriver block(char c) {
        IBlockState state = Block.BLOCK_STATE_IDS.getByValue(c);
        primer.setBlockState(currentX, currentY, currentZ, state);
        return this;
    }

    @Override
    public ICubeDriver block(IBlockState c) {
        primer.setBlockState(currentX, currentY, currentZ, c);
        return this;
    }

    @Override
    public ICubeDriver add(char c) {
        IBlockState state = Block.BLOCK_STATE_IDS.getByValue(c);
        primer.setBlockState(currentX, currentY++, currentZ, state);
        return this;
    }

    @Override
    public char getBlock() {
        return (char) Block.BLOCK_STATE_IDS.get(primer.getBlockState(currentX, currentY, currentZ));
    }

    @Override
    public char getBlockDown() {
        return (char) Block.BLOCK_STATE_IDS.get(primer.getBlockState(currentX, currentY-1, currentZ));
    }

    @Override
    public char getBlockEast() {
        return (char) Block.BLOCK_STATE_IDS.get(primer.getBlockState(currentX+1, currentY, currentZ));
    }

    @Override
    public char getBlockWest() {
        return (char) Block.BLOCK_STATE_IDS.get(primer.getBlockState(currentX-1, currentY, currentZ));
    }

    @Override
    public char getBlockSouth() {
        return (char) Block.BLOCK_STATE_IDS.get(primer.getBlockState(currentX, currentY, currentZ+1));
    }

    @Override
    public char getBlockNorth() {
        return (char) Block.BLOCK_STATE_IDS.get(primer.getBlockState(currentX, currentY, currentZ-1));
    }


    @Override
    public char getBlock(int x, int y, int z) {
        return (char) Block.BLOCK_STATE_IDS.get(primer.getBlockState(x, y, z));
    }

    @Override
    public IIndex getIndex(int x, int y, int z) {
        return new CubeDriver.Index(x, y, z);
    }


    private class Index implements IIndex {
        private final int x;
        private final int y;
        private final int z;

        Index(int x, int y, int z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            CubeDriver.Index index = (CubeDriver.Index) o;
            return x == index.x &&
                    y == index.y &&
                    z == index.z;
        }

        @Override
        public int hashCode() {
            return Objects.hash(x, y, z);
        }
    }

    @Override
    public ICubeDriver copy() {
        CubeDriver driver = new CubeDriver();
        driver.currentX = currentX;
        driver.currentY = currentY;
        driver.currentZ = currentZ;
        driver.primer = primer;
        return driver;
    }
}