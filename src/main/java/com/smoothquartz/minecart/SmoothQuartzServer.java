package com.smoothquartz.minecart;

import com.smoothquartz.minecart.network.SmoothQuartzNetworking;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SmoothQuartzServer implements ModInitializer {

    private static final Logger LOGGER = LoggerFactory.getLogger("SmoothQuartz-Server");

    @Override
    public void onInitialize() {
        SmoothQuartzNetworking.initServerNetworking();
        LOGGER.info("SmoothQuartz server initialized");
    }
}
