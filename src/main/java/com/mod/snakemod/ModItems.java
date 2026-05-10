package com.mod.snakemod;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, SnakeMod.MOD_ID);

    public static final RegistryObject<Item> EYES_ITEM = ITEMS.register("eyes",
            () -> new BlockItem(ModBlocks.EYES.get(), new Item.Properties()));
}
