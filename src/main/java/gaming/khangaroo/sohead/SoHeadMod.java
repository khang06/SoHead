package gaming.khangaroo.sohead;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SoHeadMod implements ModInitializer {
	public static final Logger LOGGER = LoggerFactory.getLogger("sohead");

	@Override
	public void onInitialize() {
		LOGGER.info("SoHead loaded!");
	}
}
