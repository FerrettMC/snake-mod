package com.mod.snakemod;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.PressurePlateBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.material.PushReaction;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlocks {

    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, SnakeMod.MOD_ID);

    public static final RegistryObject<Block> EYES = BLOCKS.register("eyes",
            () -> new EyesPressurePlateBlock(
                    BlockSetType.OAK,
                    BlockBehaviour.Properties.of()
                            .strength(0.5f)
                            .noOcclusion()
            ));

}
