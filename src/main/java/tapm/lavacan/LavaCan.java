package tapm.lavacan;

import net.fabricmc.api.ModInitializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LavaCan implements ModInitializer {
	public static final String MOD_ID = "lavacan";

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {

		LOGGER.info("Loading Config...");

		LavaCanConfig.get();

		LOGGER.info("Lava Can initialized");
	}
}