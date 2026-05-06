package tapm.lavacan.client;

import net.fabricmc.api.ClientModInitializer;
import tapm.lavacan.LavaCanFx;


public class LavaCanClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        LavaCanFx.set(new LavaCanClientFX());
    }
}
