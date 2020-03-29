package mcjty.lostcities.cubic;

import io.github.opencubicchunks.cubicchunks.api.world.ICube;
import io.github.opencubicchunks.cubicchunks.api.worldgen.ICubeGenerator;
import io.github.opencubicchunks.cubicchunks.api.util.Box;
import io.github.opencubicchunks.cubicchunks.api.util.Coords;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.Biome.SpawnListEntry;
import net.minecraft.world.chunk.Chunk;

import java.util.List;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

/**
 * A partial implementation of {@link ICubeGenerator} that handles biome assignment.
 * <p>
 * Structure recreation and lookup are not supported by default.
 */
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public abstract class CubeCityGenerator implements ICubeGenerator {


    protected World world;
    private Biome[] columnBiomes;

    public CubeCityGenerator(World world) {
        this.world = world;
    }

    @Override
    public void generateColumn(Chunk column) {
        this.columnBiomes = this.world.getBiomeProvider()
                .getBiomes(this.columnBiomes,
                        Coords.cubeToMinBlock(column.x),
                        Coords.cubeToMinBlock(column.z),
                        ICube.SIZE, ICube.SIZE);

        // Copy ids to column internal biome array
        byte[] columnBiomeArray = column.getBiomeArray();
        for (int i = 0; i < columnBiomeArray.length; ++i) {
            columnBiomeArray[i] = (byte) Biome.getIdForBiome(this.columnBiomes[i]);
        }
    }

    @Override
    public void recreateStructures(ICube cube) {
    }

    @Override
    public void recreateStructures(Chunk column) {
    }

    @Override
    public List<SpawnListEntry> getPossibleCreatures(EnumCreatureType type, BlockPos pos) {
        return world.getBiome(pos).getSpawnableList(type);
    }

    @Nullable
    @Override
    public BlockPos getClosestStructure(String name, BlockPos pos, boolean findUnexplored) {
        return null;
    }

    @Override
    public Box getFullPopulationRequirements(ICube cube) {
        return RECOMMENDED_FULL_POPULATOR_REQUIREMENT;
    }

    @Override
    public Box getPopulationPregenerationRequirements(ICube cube) {
        return RECOMMENDED_GENERATE_POPULATOR_REQUIREMENT;
    }

}