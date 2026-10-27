package dev.scripted.forgotten.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.Block;

/**
 * Unwritten Stone. It does not exist firmly enough to be broken, except while a Reality Storm
 * has every rule loosened. The block-break hook enforces that.
 */
public class ParadoxStoneBlock extends Block {
    public static final MapCodec<ParadoxStoneBlock> CODEC = createCodec(ParadoxStoneBlock::new);

    public ParadoxStoneBlock(Settings settings) {
        super(settings);
    }

    @Override
    protected MapCodec<? extends Block> getCodec() {
        return CODEC;
    }
}
