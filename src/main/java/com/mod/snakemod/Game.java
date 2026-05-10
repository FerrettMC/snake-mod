package com.mod.snakemod;

import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.BlockEvent;
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
            serverLevel.setBlock(new BlockPos(applePos.getX(), applePos.getY() + 1, applePos.getZ()), Blocks.MANGROVE_PROPAGULE.defaultBlockState(), 2);
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

            tickCounter++;
            if (tickCounter % speed != 0) return;

            serverPlayer.displayClientMessage(Component.literal("Score: §c" + score), true);

            // 1. Compute new head position
            Body head = snake.getFirst();
            int addX = 0, addZ = 0;
            switch (direction) {
                case "down"  -> addX =  1;
                case "up"    -> addX = -1;
                case "right" -> addZ = -1;
                case "left"  -> addZ =  1;
                default -> { return; }
            }

            Body newHead = new Body(head.x + addX, head.y, head.z + addZ, "head");
            BlockPos newHeadPos = new BlockPos(newHead.x, newHead.y, newHead.z);

            // 2. Check apple BEFORE mutating snake or checking collisions
            boolean ateApple = apples.contains(newHeadPos);

            // 3. Add new head, demote old head to body
            snake.addFirst(newHead);
            Body oldHead = snake.get(1);
            snake.set(1, new Body(oldHead.x, oldHead.y, oldHead.z, "body"));
            serverLevel.setBlock(new BlockPos(oldHead.x, oldHead.y + 1, oldHead.z), Blocks.AIR.defaultBlockState(), 3);

            // 4. Remove tail (but keep it around in case we ate an apple)
            Body tail = snake.removeLast();
            BlockPos tailPos = new BlockPos(tail.x, tail.y, tail.z);

            if (ateApple) {
                // Grow the snake by putting the tail back
                snake.addLast(tail);
            } else {
                serverLevel.setBlock(tailPos, Blocks.AIR.defaultBlockState(), 3);
            }

            // 5. Place new head block
            serverLevel.setBlock(newHeadPos, Blocks.RED_WOOL.defaultBlockState(), 3);

            // Place eyes
            Direction facing = Direction.NORTH;

            // 270 degrees
            facing = switch (direction) {
                case "right" -> Direction.NORTH; // 180 degrees

                case "left" -> Direction.SOUTH; // 0 degrees

                case "up" -> Direction.WEST; // 90 degrees

                case "down" -> Direction.EAST;
                default -> facing;
            };

            serverLevel.setBlock(
                    new BlockPos(newHeadPos.getX(), newHeadPos.getY() + 1, newHeadPos.getZ()),
                    ModBlocks.EYES.get().defaultBlockState().setValue(EyesPressurePlateBlock.FACING, facing),
                    3
            );



            // 6. Recolor body blocks
            int i = 0;
            for (Body segment : snake) {
                i++;
                if (segment.type.equals("head")) continue;
                BlockPos blockPos = new BlockPos(segment.x, segment.y, segment.z);
                serverLevel.setBlock(blockPos, i % 2 == 0
                        ? Blocks.LIGHT_BLUE_WOOL.defaultBlockState()
                        : Blocks.CYAN_WOOL.defaultBlockState(), 3);
            }

            // 7. Self-collision check (safe to do now, apple position is not a body segment)
            if (!ateApple && isPosInSnake(newHeadPos, false)) {
                endGame(serverLevel, false);
                return;
            }

            // 8. Out-of-bounds check
            if (outOfBounds.contains(newHeadPos)) {
                endGame(serverLevel, false);
                return;
            }

            // 9. Handle apple effects
            if (ateApple) {
                score++;
                apples.remove(newHeadPos);
                if (score % 5 == 0 && maxApples < 5) maxApples++;

                // Remove the apple blocks

                serverLevel.setBlock(newHeadPos, Blocks.RED_WOOL.defaultBlockState(), 3);


                if (snake.size() >= Start.platformLength * Start.platformLength) {
                    endGame(serverLevel, true);
                    return;
                }
            }

            // 10. Spawn new apples if needed
            int attempt = 0;
            while (apples.size() < maxApples) {
                BlockPos applePos;
                do {
                    applePos = possibleApples.get(random.nextInt(possibleApples.size()));
                    attempt++;
                } while (isPosInSnake(applePos, true) && attempt < 30);
                if (attempt >= 30) break;

                serverLevel.setBlock(applePos, Blocks.RED_CONCRETE.defaultBlockState(), 3);
                serverLevel.setBlock(new BlockPos(applePos.getX(), applePos.getY() + 1, applePos.getZ()), Blocks.MANGROVE_PROPAGULE.defaultBlockState(), 2);
                apples.add(applePos);

            }
            for (BlockPos apple : apples) {
                serverLevel.setBlock(new BlockPos(apple.getX(), apple.getY() + 1, apple.getZ()), Blocks.MANGROVE_PROPAGULE.defaultBlockState(), 2);
            }

        }
    }

    private static void endGame(ServerLevel serverLevel, boolean won) {
        started = false;
        playerStartedGame = false;
        serverPlayer.removeAllEffects();
        String message = won
                ? "You win! Score: §6" + score
                : "Game over! Score: §6" + score;
        serverPlayer.connection.send(new ClientboundSetTitleTextPacket(Component.literal(message)));
        serverPlayer.sendSystemMessage(Component.literal("Game over. §2" + score + "§f out of §5" + (Start.platformLength * Start.platformLength - 2) + "§f possible."));
        fireworks(serverLevel, fireworkPos.getX(), fireworkPos.getY(), fireworkPos.getZ());
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
    @SubscribeEvent
    public void onNeighborNotify(BlockEvent.NeighborNotifyEvent event) {
        // Only cancel neighbor updates for propagules
        if (event.getState().is(Blocks.MANGROVE_PROPAGULE)) {
            event.setCanceled(true);
        }
    }

}
