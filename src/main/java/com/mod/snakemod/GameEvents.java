package com.mod.snakemod;

import com.mod.snakemod.Game;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraft.server.level.ServerPlayer;


@Mod.EventBusSubscriber
public class GameEvents {

    @SubscribeEvent
    public static void onPlayerTick(LivingEvent.LivingTickEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!player.level().isClientSide()) {
            Game.onPlayerMoved(player);
        }
    }
}