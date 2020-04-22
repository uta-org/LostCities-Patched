package mcjty.lostcities.cubic;

import io.github.terra121.events.CubeHeightmapEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Mod.EventBusSubscriber
public class CubicHandler {
    @SubscribeEvent
    public static void onCubeHeightmapGenerate(CubeHeightmapEvent e) {
        HeightmapModel.add(e.getCubePos(), new HeightmapModel(e.isSurface(), e.getHeightmaps()));
    }
}
