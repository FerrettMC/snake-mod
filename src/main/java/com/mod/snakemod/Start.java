package com.mod.snakemod;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
public class Start {
    public static BlockPos platformStartPos = null;
    public static int platformLength = 0;
    private static boolean hasPlayed = false;
    @SubscribeEvent
    public static void onPlayerChat(ServerChatEvent event) {
        String message = event.getMessage().getString();
        ServerPlayer player = event.getPlayer();
        String[] words = message.split("\\s+");

        if (message.equalsIgnoreCase("snake")) {
            player.sendSystemMessage(Component.literal("Enter an integer to specify platform side length (e.g. platform 12)."));
            event.setCanceled(true);
            return;
        }

        if (message.equalsIgnoreCase("help")) {
            player.sendSystemMessage(Component.literal("""
                §a§lSnake Commands:
                §e• §f§lsnake <size> §7– Start a game (size 4–1000)
                §e• §f§lsnake <size> <speed> §7– Start with custom speed (1–6)
                §e• §f§lstop §7– Quit your current game
                §e• §f§lclear §7– Remove the board after a game ends
                §e• §f§lhelp §7– Show this help menu
                
                §aHow to Play:
                §7Move on the control platform to steer your snake.
                §7Eat red concrete apples to grow and score points.
                §cDon't hit the walls or yourself!
                """));
            event.setCanceled(true);
            return;
        }

        if (message.equalsIgnoreCase("clear") && hasPlayed && !Game.started && player.level() instanceof ServerLevel serverLevel) {
            cleanPlatform(serverLevel);
            player.sendSystemMessage(Component.literal("Platform cleared."));
            event.setCanceled(true);
            return;
        }

        if (message.equalsIgnoreCase("stop") && Game.started) {
            Game.started = false;
            Game.snake.clear();
            player.removeAllEffects();
            Game.outOfBounds.clear();
            Game.possibleApples.clear();
            Game.direction = "none";
            Game.playerStartedGame = false;
            player.sendSystemMessage(Component.literal("Snake game stopped."));
            event.setCanceled(true);
            return;
        }

        if (words[0].equalsIgnoreCase("snake") && (words.length == 2 || words.length == 3) && player.level() instanceof ServerLevel serverLevel) {
            int length;
            if (words[1].matches("\\d+")) {
                length = Integer.parseInt(words[1]);
            } else {
                player.sendSystemMessage(Component.literal("Not a valid length. (Invalid format)"));
                event.setCanceled(true);
                return;
            }
            if (length < 4 || length > 100) {
                player.sendSystemMessage(Component.literal("Not a valid length."));
                event.setCanceled(true);
                return;
            }

            if (words.length == 3) {
                if (words[2].matches("-?\\d+")) {
                    int speed = Integer.parseInt(words[2]);
                    if (speed < 1 || speed > 6) {
                        player.sendSystemMessage(Component.literal("Not a valid speed."));
                        event.setCanceled(true);
                        return;
                    }
                    switch (speed) {
                        case 6 -> Game.speed = 1;
                        case 5 -> Game.speed = 3;
                        case 4 -> Game.speed = 5;
                        case 2 -> Game.speed = 8;
                        case 1 -> Game.speed = 10; // 10
                        default -> Game.speed = 6;
                    }


                }
            }
            platformLength = length;
            int firstNum = (length / 2) * -1;
            int secondNum = ((length + 1) / 2);

            BlockPos pos = player.blockPosition();

            int x = pos.getX();
            int y = pos.getY();
            int z = pos.getZ();
            if (y > 270) {
                player.sendSystemMessage(Component.literal("Too high up! Go lower to play."));
                event.setCanceled(true);
                return;
            }
            platformStartPos = pos;
            Game.possibleApples.clear();
            for (int i = firstNum; i < secondNum; i++) {
                for (int q = firstNum; q < secondNum; q++) {
                    BlockPos pPos = new BlockPos(x + i, y, z + q);
                    serverLevel.setBlock(pPos, Blocks.AIR.defaultBlockState(), 3);
                    Game.possibleApples.add(pPos);
                    BlockPos pPos1 = new BlockPos(x + i, y - 1, z + q);
                    if (Math.abs(i % 2) == 1) {
                        if (Math.abs(q % 2) == 1) {
                            serverLevel.setBlock(pPos1, Blocks.LIME_CONCRETE.defaultBlockState(), 3);
                        } else {
                            serverLevel.setBlock(pPos1, Blocks.GREEN_CONCRETE.defaultBlockState(), 3);
                        }
                    } else {
                        if (Math.abs(q % 2) == 1) {
                            serverLevel.setBlock(pPos1, Blocks.GREEN_CONCRETE.defaultBlockState(), 3);
                        } else {
                            serverLevel.setBlock(pPos1, Blocks.LIME_CONCRETE.defaultBlockState(), 3);
                        }
                    }

                }
            }

            int edgeMin = firstNum - 1;

            for (int i = edgeMin; i <= secondNum; i++) {
                for (int q = edgeMin; q <= secondNum; q++) {
                    boolean onEdge = i == edgeMin || i == secondNum || q == edgeMin || q == secondNum;
                    if (onEdge) {
                        BlockPos blockPos = new BlockPos(x + i, y, z + q);
                        Game.outOfBounds.add(blockPos);
                        serverLevel.setBlock(blockPos, Blocks.GREEN_STAINED_GLASS.defaultBlockState(), 3);
                        serverLevel.setBlock(new BlockPos(x + i, y - 1, z + q), Blocks.GRAY_CONCRETE.defaultBlockState(), 3);
                    }
                }
            }
            hasPlayed = true;
            Game.snake.clear();
            BlockPos startBlock = new BlockPos((firstNum * -1) + x + 5, y + (firstNum * -1), z);
            player.teleportTo((firstNum * -1) + x + 5.5, y + (firstNum * -1) + 1, z + 0.5);
            serverLevel.setBlock(startBlock, Blocks.GLASS.defaultBlockState() ,3);
            Game.startBlock = startBlock;
            Game.startPos = new BlockPos((firstNum * -1) + x + 5, y + (firstNum * -1) + 1, z);
            Game.snake.addFirst(new Game.Body(pos.getX(), pos.getY(), pos.getZ(), "head"));
            Game.Body body = new Game.Body(pos.getX(), pos.getY(), pos.getZ() + 1, "body");
            Game.snake.add(body);
            serverLevel.setBlock(new BlockPos(body.x, body.y, body.z), Blocks.LIGHT_BLUE_WOOL.defaultBlockState(), 3);
            serverLevel.setBlock(pos, Blocks.RED_WOOL.defaultBlockState(), 3);
            serverLevel.setBlock(
                    new BlockPos(pos.getX(), pos.getY() + 1, pos.getZ()),
                    ModBlocks.EYES.get().defaultBlockState(),
                    3
            );

            player.getAbilities().flying = false;
            player.onUpdateAbilities(); // syncs to client
            player.connection.teleport(player.getX(), player.getY(), player.getZ(), 90f, 0f);
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 99999, 3, false, false));
            Game.fireworkPos = pos;
            if (words.length == 2) {
                Game.speed = 6;
            }



            player.sendSystemMessage(Component.literal("Platform set."));
            Game.startGame(player, serverLevel);
            Game.started = true;

            // Cancel the message (stops it from being sent to chat)
            event.setCanceled(true);

        }


    }
    public static void cleanPlatform(ServerLevel serverLevel) {

        int firstNum = (platformLength / 2) * -1;
        int secondNum = ((platformLength + 1) / 2);


        for (int i = firstNum; i < secondNum; i++) {
            for (int q = firstNum; q < secondNum; q++) {
                BlockPos pPos = new BlockPos(platformStartPos.getX() + i, platformStartPos.getY(), platformStartPos.getZ() + q);
                serverLevel.setBlock(pPos, Blocks.AIR.defaultBlockState(), 3);
                BlockPos pPos1 = new BlockPos(platformStartPos.getX() + i, platformStartPos.getY() - 1, platformStartPos.getZ() + q);
                if (Math.abs(i % 2) == 1) {
                    if (Math.abs(q % 2) == 1) {
                        serverLevel.setBlock(pPos1, Blocks.AIR.defaultBlockState(), 3);
                    } else {
                        serverLevel.setBlock(pPos1, Blocks.AIR.defaultBlockState(), 3);
                    }
                } else {
                    if (Math.abs(q % 2) == 1) {
                        serverLevel.setBlock(pPos1, Blocks.AIR.defaultBlockState(), 3);
                    } else {
                        serverLevel.setBlock(pPos1, Blocks.AIR.defaultBlockState(), 3);
                    }
                }

            }
        }

        int edgeMin = firstNum - 1;

        for (int i = edgeMin; i <= secondNum; i++) {
            for (int q = edgeMin; q <= secondNum; q++) {
                boolean onEdge = i == edgeMin || i == secondNum || q == edgeMin || q == secondNum;
                if (onEdge) {
                    BlockPos blockPos = new BlockPos(platformStartPos.getX() + i, platformStartPos.getY(), platformStartPos.getZ() + q);
                    Game.outOfBounds.add(blockPos);
                    serverLevel.setBlock(blockPos, Blocks.AIR.defaultBlockState(), 3);
                    serverLevel.setBlock(new BlockPos(platformStartPos.getX() + i, platformStartPos.getY() - 1, platformStartPos.getZ() + q), Blocks.AIR.defaultBlockState(), 3);
                }
            }
        }
        serverLevel.setBlock(Game.startBlock, Blocks.AIR.defaultBlockState() ,3);
    }

}
