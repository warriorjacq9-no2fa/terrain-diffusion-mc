package com.github.xandergos.terraindiffusionmc.mixin.client;

import net.minecraft.client.gui.screens.worldselection.PresetEditor;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.presets.WorldPreset;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(WorldCreationUiState.class)
public abstract class WorldCreationUiStateMixin {
    @Shadow
    public abstract WorldCreationUiState.WorldTypeEntry getWorldType();

    @Unique
    private static ResourceKey<WorldPreset> TD_PRESET = ResourceKey.create(
            Registries.WORLD_PRESET,
            Identifier.fromNamespaceAndPath("terrain_diffusion_mc", "terrain_diffusion")
    );

    @Inject(method = "getPresetEditor", at = @At("HEAD"))
    private void terrain_diffusion_mc$getPresetEditor(CallbackInfoReturnable<PresetEditor> cir) {
        Holder<WorldPreset> holder = this.getWorldType().preset();

        if(holder != null && holder.unwrapKey().orElseThrow().equals(TD_PRESET)) {

        }
    }
}
