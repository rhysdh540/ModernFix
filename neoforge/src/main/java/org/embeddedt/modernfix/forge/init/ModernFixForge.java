package org.embeddedt.modernfix.forge.init;

import com.google.common.collect.ImmutableList;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.*;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.javafmlmod.FMLJavaModLoadingContext;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.registries.RegisterEvent;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.apache.commons.lang3.tuple.Pair;
import org.embeddedt.modernfix.ModernFix;
import org.embeddedt.modernfix.core.ModernFixMixinPlugin;
import org.embeddedt.modernfix.entity.EntityDataIDSyncHandler;
import org.embeddedt.modernfix.forge.ModernFixConfig;
import org.embeddedt.modernfix.forge.classloading.ClassLoadHack;
import org.embeddedt.modernfix.forge.classloading.ModFileScanDataDeduplicator;
import org.embeddedt.modernfix.forge.config.ConfigFixer;
import org.embeddedt.modernfix.forge.config.NightConfigFixer;
import org.embeddedt.modernfix.forge.packet.PacketHandler;

import java.util.List;

@Mod(ModernFix.MODID)
public class ModernFixForge {
    private static ModernFix commonMod;
    public static boolean launchDone = false;

    public ModernFixForge() {
        commonMod = new ModernFix();
        // Register ourselves for server and other game events we are interested in
        NeoForge.EVENT_BUS.register(this);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::commonSetup);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::registerItems);
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> NeoForge.EVENT_BUS.register(new ModernFixClientForge()));
        ModLoadingContext.get().registerExtensionPoint(IExtensionPoint.DisplayTest.class, () -> new IExtensionPoint.DisplayTest(() -> IExtensionPoint.DisplayTest.IGNORESERVERONLY, (a, b) -> true));
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, ModernFixConfig.COMMON_CONFIG);
        PacketHandler.register();
        ModFileScanDataDeduplicator.deduplicate();
        ClassLoadHack.loadModClasses();
        ConfigFixer.replaceConfigHandlers();
    }

    @SubscribeEvent
    public void onCommandRegister(RegisterCommandsEvent event) {
        for(String name : new String[] { "mfsrc"}) {
            event.getDispatcher().register(LiteralArgumentBuilder.<CommandSourceStack>literal(name)
                    .requires(source -> source.hasPermission(3))
                    .executes(context -> {
                        NightConfigFixer.runReloads();
                        return 1;
                    }));
        }
    }

    @SubscribeEvent
    public void onDatapackSync(OnDatapackSyncEvent event) {
        if(event.getPlayer() != null) {
            if(!ServerLifecycleHooks.getCurrentServer().isDedicatedServer() && event.getPlayerList().getPlayerCount() == 0)
                return;
            EntityDataIDSyncHandler.onDatapackSyncEvent(event.getPlayer());
        }
    }

    private void registerItems(RegisterEvent event) {
        if(Boolean.getBoolean("modernfix.largeRegistryTest")) {
            event.register(Registries.ITEM, helper -> {
                Item.Properties props = new Item.Properties();
                for(int i = 0; i < 1000000; i++) {
                    helper.register(new ResourceLocation("modernfix", "item_" + i), new Item(props));
                }
            });
        }
    }

    private static final List<Pair<List<String>, String>> MOD_WARNINGS = ImmutableList.of(
            Pair.of(ImmutableList.of("ferritecore"), "modernfix.no_ferritecore")
    );

    @SubscribeEvent
    public void commonSetup(FMLCommonSetupEvent event) {
        if(ModernFixMixinPlugin.instance.isOptionEnabled("feature.warn_missing_perf_mods.Warnings")) {
            event.enqueueWork(() -> {
                boolean atLeastOneWarning = false;
                for(Pair<List<String>, String> warning : MOD_WARNINGS) {
                    boolean isPresent = !FMLLoader.isProduction() || warning.getLeft().stream().anyMatch(name -> ModList.get().isLoaded(name));
                    if(!isPresent) {
                        atLeastOneWarning = true;
                        ModLoader.get().addWarning(new ModLoadingWarning(ModLoadingContext.get().getActiveContainer().getModInfo(), ModLoadingStage.COMMON_SETUP, warning.getRight()));
                    }
                }
                if(atLeastOneWarning)
                    ModLoader.get().addWarning(new ModLoadingWarning(ModLoadingContext.get().getActiveContainer().getModInfo(), ModLoadingStage.COMMON_SETUP, "modernfix.perf_mod_warning"));
            });
        }
    }
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onServerDead(ServerStoppedEvent event) {
        commonMod.onServerDead(event.getServer());
    }
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onServerStarted(ServerStartedEvent event) {
        commonMod.onServerStarted();
    }
}
