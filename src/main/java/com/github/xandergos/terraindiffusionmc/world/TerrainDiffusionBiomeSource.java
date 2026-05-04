package com.github.xandergos.terraindiffusionmc.world;

import com.github.xandergos.terraindiffusionmc.config.TerrainDiffusionConfig;
import com.github.xandergos.terraindiffusionmc.pipeline.LocalTerrainProvider;
import com.github.xandergos.terraindiffusionmc.pipeline.LocalTerrainProvider.HeightmapData;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;
import org.jspecify.annotations.NonNull;

import java.util.Map;
import java.util.stream.Stream;

import static java.util.Map.entry;

public class TerrainDiffusionBiomeSource extends BiomeSource {
    private static final ResourceKey<Biome> FOREST_SPARSE = ResourceKey.create(Registries.BIOME,
            Identifier.fromNamespaceAndPath("terrain_diffusion_mc", "forest_sparse")
    );
    private static final ResourceKey<Biome> TAIGA_SPARSE = ResourceKey.create(Registries.BIOME,
            Identifier.fromNamespaceAndPath("terrain_diffusion_mc", "taiga_sparse")
    );
    private static final ResourceKey<Biome> SNOWY_TAIGA_SPARSE = ResourceKey.create(Registries.BIOME,
            Identifier.fromNamespaceAndPath("terrain_diffusion_mc", "snowy_taiga_sparse")
    );
    public static final MapCodec<TerrainDiffusionBiomeSource> CODEC = RecordCodecBuilder.mapCodec((instance) ->
            instance.group(
                    RegistryOps.retrieveGetter(Registries.BIOME)
            ).apply(instance, instance.stable(TerrainDiffusionBiomeSource::new)));


    private final HolderGetter<Biome> biomeLookup;
    private Map<Short, Holder<Biome>> biomeIdMap = null;

    public TerrainDiffusionBiomeSource(HolderGetter<Biome> biomeLookup) {
        this.biomeLookup = biomeLookup;
    }

    @Override
    protected @NonNull MapCodec<? extends BiomeSource> codec() {
        return CODEC;
    }

    private void requireBiomeIdMap() {
        if (biomeIdMap == null) {
            biomeIdMap = Map.ofEntries(
                    entry((short) 1, biomeLookup.getOrThrow(Biomes.PLAINS)),
                    entry((short) 3, biomeLookup.getOrThrow(Biomes.SNOWY_PLAINS)),
                    entry((short) 5, biomeLookup.getOrThrow(Biomes.DESERT)),
                    entry((short) 6, biomeLookup.getOrThrow(Biomes.SWAMP)),
                    entry((short) 8, biomeLookup.getOrThrow(Biomes.FOREST)),
                    entry((short) 15, biomeLookup.getOrThrow(Biomes.TAIGA)),
                    entry((short) 16, biomeLookup.getOrThrow(Biomes.SNOWY_TAIGA)),
                    entry((short) 17, biomeLookup.getOrThrow(Biomes.SAVANNA)),
                    entry((short) 19, biomeLookup.getOrThrow(Biomes.WINDSWEPT_HILLS)),
                    entry((short) 23, biomeLookup.getOrThrow(Biomes.JUNGLE)),
                    entry((short) 26, biomeLookup.getOrThrow(Biomes.BADLANDS)),
                    entry((short) 29, biomeLookup.getOrThrow(Biomes.MEADOW)),
                    entry((short) 31, biomeLookup.getOrThrow(Biomes.GROVE)),
                    entry((short) 32, biomeLookup.getOrThrow(Biomes.SNOWY_SLOPES)),
                    entry((short) 33, biomeLookup.getOrThrow(Biomes.FROZEN_PEAKS)),
                    entry((short) 35, biomeLookup.getOrThrow(Biomes.STONY_PEAKS)),
                    entry((short) 41, biomeLookup.getOrThrow(Biomes.WARM_OCEAN)),
                    entry((short) 44, biomeLookup.getOrThrow(Biomes.OCEAN)),
                    entry((short) 46, biomeLookup.getOrThrow(Biomes.COLD_OCEAN)),
                    entry((short) 48, biomeLookup.getOrThrow(Biomes.FROZEN_OCEAN)),
                    entry((short) 108, biomeLookup.getOrThrow(FOREST_SPARSE)),
                    entry((short) 115, biomeLookup.getOrThrow(TAIGA_SPARSE)),
                    entry((short) 116, biomeLookup.getOrThrow(SNOWY_TAIGA_SPARSE))
            );
        }
    }

    @Override
    public @NonNull Stream<Holder<Biome>> collectPossibleBiomes() {
        requireBiomeIdMap();
        return biomeIdMap.values().stream();
    }

    @Override
    public @NonNull Holder<Biome> getNoiseBiome(int x, int y, int z, Climate.Sampler noise) {
        requireBiomeIdMap();
        Holder<Biome> defaultEntry = biomeIdMap.get((short) 1);

        // x, y, z are in quart coordinates (block / 4)
        int blockX = x << 2;
        int blockZ = z << 2;

        int tileSize = TerrainDiffusionConfig.tileSize();
        int tileShift = Integer.numberOfTrailingZeros(tileSize);

        int tileX = blockX >> tileShift;
        int tileZ = blockZ >> tileShift;

        int blockStartX = tileX << tileShift;
        int blockStartZ = tileZ << tileShift;
        int blockEndX = blockStartX + tileSize;
        int blockEndZ = blockStartZ + tileSize;

        HeightmapData data = LocalTerrainProvider.getInstance().fetchHeightmap(blockStartZ, blockStartX, blockEndZ, blockEndX);
        if (data != null && data.biomeIds != null) {
            int localX = Math.clamp(blockX - blockStartX, 0, data.width - 1);
            int localZ = Math.clamp(blockZ - blockStartZ, 0, data.height - 1);
            Holder<Biome> entry = biomeIdMap.get(data.biomeIds[localZ][localX]);
            if (entry != null) return entry;
        }

        return defaultEntry;
    }
}

