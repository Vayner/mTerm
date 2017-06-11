package com.enderwolf.mterm;

import com.enderwolf.mterm.reference.ModReference;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.*;


/**
 * Mod main entry point. Defines the mod "RealMapsWorldType".
 */
@Mod(
	modid = ModReference.MOD_ID,
	version = ModReference.VERSION,
//  certificateFingerprint = ModReference.FINGERPRINT, // TODO implement properly?
	acceptableRemoteVersions = "*",
	guiFactory = ModReference.GUI_FACTORY_CLASS
)
public class mTermMod {

    @Mod.Instance(ModReference.MOD_ID)
    static public mTermMod instance;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {

    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {

    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {

    }
}
