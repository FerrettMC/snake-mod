package com.mod.snakemod;

import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.FireworkExplosion;
import net.minecraft.world.item.component.Fireworks;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.*;

public class Game {
    public static BlockPos fireworkPos = null;
    public static boolean started = false;
    public static BlockPos startBlock = null;
    public static BlockPos startPos = null;
    private static BlockPos lastPos = null;
    public static String direction = "none";
    public static ServerPlayer serverPlayer = null;
    public static List<BlockPos> outOfBounds = new ArrayList<>();
    public static List<BlockPos> possibleApples = new ArrayList<>();
    private static List<BlockPos> apples = new ArrayList<>();
    private static int maxApples = 1;
    private static int tickCounter = 0;
    public static boolean playerStartedGame = false;
    public static int speed = 6;
    private static int score = 0;
    private static final Random random = new Random();

    public static class Body {
        public int x, y, z;
        public String type;

        public Body(int x, int y, int z, String type) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.type = type;
        }
    }

    public static LinkedList<Body> snake = new LinkedList<>();

    // Start game func
    public static void startGame(ServerPlayer player, ServerLevel serverLevel) {
        apples.clear();
        maxApples = 1;
        lastPos = player.blockPosition();
        serverPlayer = player;
        direction = "none";
        playerStartedGame = false;
        tickCounter = 0;
        score = 0;
        if (apples.size() < maxApples) {
            BlockPos applePos;
            do {

                // pick a random pos from possibleApples
                applePos = possibleApples.get(random.nextInt(possibleApples.size()));
            } while (isPosInSnake(applePos, true));

            serverLevel.setBlock(applePos, Blocks.RED_CONCRETE.defaultBlockState(), 3);
            serverLevel.setBlock(new BlockPos(applePos.getX(), applePos.getY() + 1, applePos.getZ()), Blocks.MANGROVE_PROPAGULE.defaultBlockState(), 3);
            apples.add(applePos);
        }
    }

    public static boolean isPosInSnake(BlockPos pos, boolean appleCheck) {
        for (Body body : snake) {
            if (!appleCheck && Objects.equals(body.type, "head")) {
                continue;
            }
            if (body.x == pos.getX() && body.y == pos.getY() && body.z == pos.getZ()) {
                return true;
            }
        }
        if (apples.isEmpty() || !appleCheck) return false;
        for (BlockPos apple : apples) {
            if (apple.getX() == pos.getX() && apple.getY() == pos.getY() && apple.getZ() == pos.getZ()) {
                return true;
            }
        }
        return false;
    }


    private static void fireworks(ServerLevel serverLevel, int x, int y, int z) {
        FireworkExplosion explosion = new FireworkExplosion(
                FireworkExplosion.Shape.LARGE_BALL,
                IntList.of(0xFF69B4),  // pink colors
                IntList.of(0xFFFFFF),  // fade to white
                true,  // trail
                true   // flicker
        );

        ItemStack fireworkItem = new ItemStack(Items.FIREWORK_ROCKET);
        fireworkItem.set(DataComponents.FIREWORKS, new Fireworks(1, List.of(explosion)));

        FireworkRocketEntity firework = new FireworkRocketEntity(serverLevel, x, y, z, fireworkItem);
        serverLevel.addFreshEntity(firework);
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (serverPlayer == null || snake.isEmpty() || !started || direction == null || !playerStartedGame) return;
        if (serverPlayer.level() instanceof ServerLevel serverLevel) {
            serverLevel.setBlock(startBlock, Blocks.GLASS.defaultBlockState(), 3);
        }

        tickCounter++;
        if (tickCounter % speed == 0) {
            if (serverPlayer.level() instanceof ServerLevel serverLevel) {
                serverPlayer.displayClientMessage(Component.literal("Score: §c" + score), true);

                // 1. Compute new head position
                Body head = snake.getFirst();
                int addX;
                int addZ;
                switch (direction) {
                    case "down" -> { addX = 1; addZ = 0; }
                    case "up" -> { addX = -1; addZ = 0; }
                    case "right" -> { addX = 0; addZ = -1; }
                    case "left" -> { addX = 0; addZ = 1; }
                    default -> { return; } // don't move if direction is unrecognized
                }
                Body newHead = new Body(head.x + addX, head.y, head.z + addZ, "head");

                // 2. Place new head block
                BlockPos newHeadPos = new BlockPos(newHead.x, newHead.y, newHead.z);
                serverLevel.setBlock(newHeadPos, Blocks.RED_WOOL.defaultBlockState(), 3);

                // 3. Add new head to front
                snake.addFirst(newHead);

                Body oldHead = snake.get(1);
                snake.set(1, new Body(oldHead.x, oldHead.y, oldHead.z, "body"));

                // 4. Remove tail block
                Body tail = snake.removeLast();
                BlockPos tailPos = new BlockPos(tail.x, tail.y, tail.z);
                serverLevel.setBlock(tailPos, Blocks.AIR.defaultBlockState(), 3);
                int i = 0;
                for (Body snake_block : snake) {
                    i++;
                    if (snake_block.type.equals("head"))
                        continue;
                    BlockPos blockPos = new BlockPos(snake_block.x, snake_block.y, snake_block.z);
                    if (i % 2 == 0) {
                        serverLevel.setBlock(blockPos, Blocks.LIGHT_BLUE_WOOL.defaultBlockState(), 3);
                    } else {
                        serverLevel.setBlock(blockPos, Blocks.CYAN_WOOL.defaultBlockState(), 3);
                    }
                }
                if (isPosInSnake(new BlockPos(newHead.x, newHead.y, newHead.z), false)) {
                    started = false;
                    serverPlayer.removeAllEffects();
                    serverPlayer.connection.send(new ClientboundSetTitleTextPacket(Component.literal("Game over! Score: §6" + score)));
                    fireworks(serverLevel, fireworkPos.getX(), fireworkPos.getY(), fireworkPos.getZ());
                    playerStartedGame = false;
                    return;
                }
                if (outOfBounds.contains(new BlockPos(newHead.x, newHead.y, newHead.z))) {
                    started = false;
                    serverPlayer.removeAllEffects();
                    serverPlayer.connection.send(new ClientboundSetTitleTextPacket(Component.literal("Game over! Score: §6" + score)));
                    playerStartedGame = false;
                    fireworks(serverLevel, fireworkPos.getX(), fireworkPos.getY(), fireworkPos.getZ());
                    return;
                }
                if (apples.contains(new BlockPos(newHead.x, newHead.y, newHead.z))) {
                    score++;
                    if (score % 5 == 0 && maxApples < 5) {
                        maxApples++;
                    }
                    snake.add(tail);
                    apples.remove(new BlockPos(newHead.x, newHead.y, newHead.z));

                }
                if (apples.size() < maxApples) {
                    BlockPos applePos;
                    do {

                        // pick a random pos from possibleApples
                        applePos = possibleApples.get(random.nextInt(possibleApples.size()));
                    } while (isPosInSnake(applePos, true));

                    serverLevel.setBlock(applePos, Blocks.RED_CONCRETE.defaultBlockState(), 3);
                    serverLevel.setBlock(new BlockPos(applePos.getX(), applePos.getY() + 1, applePos.getZ()), Blocks.MANGROVE_PROPAGULE.defaultBlockState(), 3);
                    apples.add(applePos);
                }
            }
        }

    }




    // Player block movement logic
    public static void onPlayerMoved(ServerPlayer player) {  // must be public static
        if (player != serverPlayer) {
            return;
        }
        BlockPos currentPos = player.blockPosition();


        if (lastPos == null || currentPos.equals(lastPos) || !started) return;
        if (lastPos == startPos) return;

        lastPos = currentPos;
        if (player.blockPosition().getX() == startPos.getX()) {
            if (player.blockPosition().getZ() - 1 == startPos.getZ()) {
                if (direction.equals("right") || !playerStartedGame) {
                    player.teleportTo(startPos.getX() + 0.5, startPos.getY(), startPos.getZ() + 0.5);
                    return;
                }
                direction = "left";
            } else if (player.blockPosition().getZ() + 1 == startPos.getZ()) {
                if (direction.equals("left")) {
                    player.teleportTo(startPos.getX() + 0.5, startPos.getY(), startPos.getZ() + 0.5);
                    return;
                }
                if (!started) serverPlayer.displayClientMessage(Component.literal("Game started!"), true);
                playerStartedGame = true;
                direction = "right";
            }
            else {
                player.teleportTo(startPos.getX() + 0.5, startPos.getY(), startPos.getZ() + 0.5);
                return;
            }
        }
        else if (player.blockPosition().getZ() == startPos.getZ()) {
            if (player.blockPosition().getX() - 1 == startPos.getX()) {
                if (direction.equals("up")) {
                    player.teleportTo(startPos.getX() + 0.5, startPos.getY(), startPos.getZ() + 0.5);
                    return;
                }
                if (!started) serverPlayer.displayClientMessage(Component.literal("Game started!"), true);
                playerStartedGame = true;
                direction = "down";
            } else if (player.blockPosition().getX() + 1 == startPos.getX()) {
                if (direction.equals("down")) {
                    player.teleportTo(startPos.getX() + 0.5, startPos.getY(), startPos.getZ() + 0.5);
                    return;
                }
                if (!started) serverPlayer.displayClientMessage(Component.literal("Game started!"), true);
                playerStartedGame = true;
                direction = "up";
            }
            else {
                player.teleportTo(startPos.getX() + 0.5, startPos.getY(), startPos.getZ() + 0.5);
                return;
            }
        }
        else {
            player.teleportTo(startPos.getX() + 0.5, startPos.getY(), startPos.getZ() + 0.5);
            return;
        }
        player.teleportTo(startPos.getX() + 0.5, startPos.getY(), startPos.getZ() + 0.5);



    }
}
