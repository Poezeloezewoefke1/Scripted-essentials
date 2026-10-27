package dev.scripted.forgotten.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.scripted.forgotten.Reality;
import dev.scripted.forgotten.item.CodexItem;
import dev.scripted.forgotten.logic.integrity.IntegrityStage;
import dev.scripted.forgotten.world.WorldMemory;
import dev.scripted.forgotten.system.AnomalyCommands;
import dev.scripted.forgotten.system.FirstSigns;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

/**
 * /forgotten - the Codex for anyone, and operator controls for testing and running a server:
 * set how awake the world is, how intact a chunk is, force an anomaly or the first cube.
 */
public final class ForgottenCommand {
    private ForgottenCommand() {
    }

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        LiteralArgumentBuilder<ServerCommandSource> root = CommandManager.literal("forgotten");
        root.then(CommandManager.literal("codex").executes(ctx -> {
            ServerPlayerEntity player = ctx.getSource().getPlayerOrThrow();
            CodexItem.report(player, Reality.world(player));
            return 1;
        }));
        LiteralArgumentBuilder<ServerCommandSource> admin = CommandManager.literal("admin")
                .requires(CommandManager.requirePermissionLevel(CommandManager.GAMEMASTERS_CHECK));
        admin.then(CommandManager.literal("lucidity").then(CommandManager.argument("value", DoubleArgumentType.doubleArg(0, 100))
                .executes(ctx -> {
                    double v = DoubleArgumentType.getDouble(ctx, "value");
                    WorldMemory memory = Reality.memory(ctx.getSource().getServer());
                    memory.lucidity.setLucidity(v);
                    memory.markDirty();
                    ctx.getSource().sendFeedback(() -> Text.literal("Lucidity is now " + String.format("%.1f", memory.lucidity.lucidity())), true);
                    return 1;
                })));
        admin.then(CommandManager.literal("integrity").then(CommandManager.argument("value", IntegerArgumentType.integer(0, 100))
                .executes(ctx -> {
                    ServerPlayerEntity player = ctx.getSource().getPlayerOrThrow();
                    int v = IntegerArgumentType.getInteger(ctx, "value");
                    Reality.setIntegrity(Reality.world(player), player.getBlockPos(), v);
                    ctx.getSource().sendFeedback(() -> Text.literal("This chunk's integrity is now " + v + " (" + IntegrityStage.of(v).title() + ")"), true);
                    return 1;
                })));
        admin.then(CommandManager.literal("anomaly").then(CommandManager.argument("stage", IntegerArgumentType.integer(1, 5))
                .executes(ctx -> {
                    ServerPlayerEntity player = ctx.getSource().getPlayerOrThrow();
                    int stage = IntegerArgumentType.getInteger(ctx, "stage");
                    boolean ok = AnomalyCommands.force(player, stage);
                    ctx.getSource().sendFeedback(() -> Text.literal(ok ? "The world made a mistake." : "Nothing here could go wrong that way. Try again."), false);
                    return ok ? 1 : 0;
                })));
        admin.then(CommandManager.literal("cube").executes(ctx -> {
            ServerPlayerEntity player = ctx.getSource().getPlayerOrThrow();
            FirstSigns.spawn(Reality.world(player), player, Reality.now(ctx.getSource().getServer()));
            return 1;
        }));
        admin.then(CommandManager.literal("insight").then(CommandManager.argument("amount", IntegerArgumentType.integer(0))
                .executes(ctx -> {
                    ServerPlayerEntity player = ctx.getSource().getPlayerOrThrow();
                    Reality.player(player).insight += IntegerArgumentType.getInteger(ctx, "amount");
                    return 1;
                })));
        admin.then(CommandManager.literal("debt").then(CommandManager.argument("amount", DoubleArgumentType.doubleArg(0))
                .executes(ctx -> {
                    ServerPlayerEntity player = ctx.getSource().getPlayerOrThrow();
                    Reality.player(player).debt = DoubleArgumentType.getDouble(ctx, "amount");
                    return 1;
                })));
        AdminExtensions.register(admin);
        dev.scripted.forgotten.system.Guilds.register(root);
        root.then(admin);
        dispatcher.register(root);
    }
}
