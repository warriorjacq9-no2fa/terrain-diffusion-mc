package com.github.xandergos.terraindiffusionmc;

import com.github.xandergos.terraindiffusionmc.explorer.ExplorerServer;
import com.github.xandergos.terraindiffusionmc.pipeline.LocalTerrainProvider;
import com.github.xandergos.terraindiffusionmc.pipeline.ModelAssetManager;
import com.github.xandergos.terraindiffusionmc.pipeline.PipelineModels;
import com.github.xandergos.terraindiffusionmc.world.TerrainDiffusionBiomeSource;
import com.github.xandergos.terraindiffusionmc.world.TerrainDiffusionDensityFunction;
import com.github.xandergos.terraindiffusionmc.world.WorldScaleManager;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.serialization.MapCodec;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;

@Mod("terrain_diffusion_mc")
public class TerrainDiffusionMc {
    public static final String MOD_ID = "terrain_diffusion_mc";
    private static final Logger LOG = LoggerFactory.getLogger(TerrainDiffusionMc.class);
    private static final DeferredRegister<MapCodec<? extends BiomeSource>> BIOME_SOURCES =
            DeferredRegister.create(Registries.BIOME_SOURCE, MOD_ID);
    private static final DeferredRegister<MapCodec<? extends DensityFunction>> DENSITY_FUNCTIONS =
            DeferredRegister.create(Registries.DENSITY_FUNCTION_TYPE, MOD_ID);

    public static final RegistryObject<MapCodec<TerrainDiffusionBiomeSource>> BIOME_SOURCE =
            BIOME_SOURCES.register("terrain_diffusion", () -> TerrainDiffusionBiomeSource.CODEC);
    public static final RegistryObject<MapCodec<? extends DensityFunction>> DENSITY_FUNCTION =
            DENSITY_FUNCTIONS.register("terrain_diffusion", () -> TerrainDiffusionDensityFunction.CODEC);


    public TerrainDiffusionMc(FMLJavaModLoadingContext ctx) {
        LOG.info("Initializing terrain_diffusion_mc");
        MinecraftForge.EVENT_BUS.register(this);
        BIOME_SOURCES.register(ctx.getModBusGroup());
        DENSITY_FUNCTIONS.register(ctx.getModBusGroup());

        ModelAssetManager.ensureAssetsReady();
        PipelineModels.load();
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent evt) {
        LocalTerrainProvider.clearCache();
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent evt) {
        ExplorerServer.stop();
    }

    @SubscribeEvent
    public void onLevelLoad(LevelEvent.Load evt) {
        if (!(evt.getLevel() instanceof ServerLevel level)) return;

        if(level.dimension() == Level.OVERWORLD) {
            WorldScaleManager.initializeForWorld(level);
            LocalTerrainProvider.init(level.getSeed());
        }
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent evt) {
        evt.getDispatcher().register(
                Commands.literal("td-explore").executes(TerrainDiffusionMc::executeExplore)
        );
    }

    private static int executeExplore(CommandContext<CommandSourceStack> ctx) {
        try {
            int port = ExplorerServer.startIfNotRunning();
            String url = "http://localhost:" + port;
            MutableComponent link = Component.literal(url).withStyle(s -> s
                    .withClickEvent(new ClickEvent.OpenUrl(URI.create(url)))
                    .withUnderlined(true)
            );
            ctx.getSource().sendSuccess(
                    () -> Component.literal("Terrain Explorer: ").append(link),
                    false
            );
        } catch (Exception e) {
            LOG.error("Failed to start terrain explorer", e);
            ctx.getSource().sendFailure(
                    Component.literal("Failed to start terrain explorer: " + e.getMessage())
            );
        }
        return 1;
    }
}
