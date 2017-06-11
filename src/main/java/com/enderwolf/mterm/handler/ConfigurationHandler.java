package com.enderwolf.mterm.handler;

import com.enderwolf.mterm.reference.ModReference;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.io.File;

/**
 * Handler for all things configuration.
 */
public class ConfigurationHandler {

    public static Configuration configuration;

    public static Property terrainDataFolder = null;
    public static Property defaultTerrainData = null;
    public static Property forceGenerate = null;
    public static Property forceGenerateQuit = null;

    public static void init(File configurationFile) {

        if(configuration == null) {
            configuration = new Configuration(configurationFile);
            loadConfiguration();
        }
    }

    @SubscribeEvent
    public void onConfigurationChangedEvent(ConfigChangedEvent.OnConfigChangedEvent event) {
        if(event.getModID().equalsIgnoreCase(ModReference.MOD_ID)) {
            loadConfiguration();
        }
    }

    private static void loadConfiguration () {

        terrainDataFolder = configuration.get(Configuration.CATEGORY_GENERAL, "data_folder", "terraindata", "The folder to search after valid data sets in");
        defaultTerrainData = configuration.get(Configuration.CATEGORY_GENERAL, "default_data_set", "default", "Name of the data set that should be selected as default");
        forceGenerate = configuration.get(Configuration.CATEGORY_GENERAL, "force_generate", false, "Force all areas in data set to be generated");
        forceGenerateQuit = configuration.get(Configuration.CATEGORY_GENERAL, "force_generate_quit", false, "Shut down Minecraft after force generating the areas");

        if(configuration.hasChanged()) {
            configuration.save();
        }
    }
}
