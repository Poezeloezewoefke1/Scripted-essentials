package dev.scripted.forgotten.client;

import dev.scripted.forgotten.registry.ModBlocks;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.BlockRenderLayerMap;
import net.minecraft.client.render.BlockRenderLayer;

/**
 * Client side. Almost everything in the mod is decided on the server and drawn with vanilla
 * renderers, so the client only needs to know which blocks are see-through and how to draw
 * the few creatures that are not reskins of vanilla ones.
 */
public final class ForgottenClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        BlockRenderLayerMap.putBlock(ModBlocks.MARROWGLASS, BlockRenderLayer.TRANSLUCENT);
    }
}
