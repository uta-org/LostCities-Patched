package mcjty.lostcities.cubic;

import io.github.opencubicchunks.cubicchunks.api.util.CubePos;
import io.github.opencubicchunks.cubicchunks.api.world.ICube;
import io.github.opencubicchunks.cubicchunks.api.worldgen.populator.CubePopulatorEvent;
import io.github.terra121.TerraMod;
import io.github.terra121.events.CubeHeightmapEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import static mcjty.lostcities.cubic.world.CubicCityWorldProcessor.*;

@Mod.EventBusSubscriber
public class CubicHandler {
    @SubscribeEvent
    public static void onCubeHeightmapGenerate(CubeHeightmapEvent e) {
        // HeightmapModel.add(e.getCubePos(), new HeightmapModel(e.isSurface(), e.getHeightmaps()));

        CubePos pos = e.getCubePos();
        populator.spawnInChunk(pos.getX(), pos.getY(), pos.getZ(), new HeightmapModel(e.isSurface(), e.getHeightmaps()));
    }

    @SubscribeEvent
    public static void onTerraPopulate(CubePopulatorEvent e) {
        ICube cube = e.getCube();
        try {
            spawnersGenerator.populate(worldObj.rand, cube.getX(), cube.getZ(), worldObj);
        } catch(Exception ex) {
            TerraMod.LOGGER.error(ex);
        }
    }
}
