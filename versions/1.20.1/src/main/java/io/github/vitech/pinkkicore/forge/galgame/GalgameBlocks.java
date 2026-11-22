package io.github.vitech.pinkkicore.forge.galgame;

import io.github.vitech.pinkkicore.core.ModInfo;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class GalgameBlocks {
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, ModInfo.ID);
    public static final RegistryObject<Block> SWORD_IN_STONE = BLOCKS.register("sword_in_stone",
        () -> new SwordInStoneBlock(BlockBehaviour.Properties.copy(Blocks.COBBLESTONE).noOcclusion()));
    public static final RegistryObject<Block> GALGAME_ARCADE = BLOCKS.register("galgame_arcade",
        () -> new GalgameArcadeBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).strength(2.5F).noOcclusion()));

    private GalgameBlocks() {}

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
    }
}
