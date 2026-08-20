package dev.scripted.essentials.core;

import dev.scripted.essentials.ScriptedEssentials;
import dev.scripted.essentials.features.chat.ChatFilterFeature;
import dev.scripted.essentials.features.chat.ChatMuteFeature;
import dev.scripted.essentials.features.chat.ClearChatFeature;
import dev.scripted.essentials.features.chat.CommandFeedbackFeature;
import dev.scripted.essentials.features.chat.FeedbackSoundsFeature;
import dev.scripted.essentials.features.chat.JoinLeaveMessagesFeature;
import dev.scripted.essentials.features.chat.PrivateMessagesFeature;
import dev.scripted.essentials.features.inventory.AutoClearFeature;
import dev.scripted.essentials.features.inventory.EnderChestViewFeature;
import dev.scripted.essentials.features.inventory.InventoryRollbackFeature;
import dev.scripted.essentials.features.inventory.InvseeFeature;
import dev.scripted.essentials.features.inventory.ItemMakerFeature;
import dev.scripted.essentials.features.inventory.KeepInventoryFeature;
import dev.scripted.essentials.features.inventory.KitFeature;
import dev.scripted.essentials.features.moderation.CommandBlockerFeature;
import dev.scripted.essentials.features.moderation.FreezeFeature;
import dev.scripted.essentials.features.moderation.HalfHeartFeature;
import dev.scripted.essentials.features.moderation.RecordingModeFeature;
import dev.scripted.essentials.features.moderation.ServerLockFeature;
import dev.scripted.essentials.features.moderation.VanishFeature;
import dev.scripted.essentials.features.player.DurabilityFeature;
import dev.scripted.essentials.features.player.FeedFeature;
import dev.scripted.essentials.features.player.FlyFeature;
import dev.scripted.essentials.features.player.FullToolsFeature;
import dev.scripted.essentials.features.player.GameModeFeature;
import dev.scripted.essentials.features.player.HealFeature;
import dev.scripted.essentials.features.player.HungerFeature;
import dev.scripted.essentials.features.player.InvisibilityFeature;
import dev.scripted.essentials.features.player.NoEffectsFeature;
import dev.scripted.essentials.features.player.PauseEffectsFeature;
import dev.scripted.essentials.features.player.PotionPresetsFeature;
import dev.scripted.essentials.features.player.RandomHeadFeature;
import dev.scripted.essentials.features.player.RepairFeature;
import dev.scripted.essentials.features.player.SetHealthFeature;
import dev.scripted.essentials.features.player.TotemFeature;
import dev.scripted.essentials.features.teleport.OfflineTeleportFeature;
import dev.scripted.essentials.features.teleport.SpawnFeature;
import dev.scripted.essentials.features.teleport.TopFeature;
import dev.scripted.essentials.features.teleport.WarpFeature;
import dev.scripted.essentials.features.npc.NpcFeature;
import dev.scripted.essentials.features.npc.VillagerMakerFeature;
import dev.scripted.essentials.features.social.NicknameFeature;
import dev.scripted.essentials.features.social.TeamChatFeature;
import dev.scripted.essentials.features.social.TeamFeature;
import dev.scripted.essentials.features.social.VoiceChatMuteFeature;
import dev.scripted.essentials.features.systems.DeathActionsFeature;
import dev.scripted.essentials.features.systems.OrbitalWeaponsFeature;
import dev.scripted.essentials.features.systems.PermissionsFeature;
import dev.scripted.essentials.features.systems.SkinFeature;
import dev.scripted.essentials.features.systems.StasisFeature;
import dev.scripted.essentials.features.world.ChunkToolsFeature;
import dev.scripted.essentials.features.world.DimensionLockFeature;
import dev.scripted.essentials.features.world.FakeWorldBorderFeature;

/**
 * The single list of every feature the plugin ships.
 *
 * <p>Adding a feature means writing the class and adding one line here; nothing else in the
 * plugin needs to know about it. Registration order is the order icons appear in {@code /se}.
 */
public final class FeatureCatalog {

    private FeatureCatalog() {
    }

    public static void populate(ScriptedEssentials plugin, FeatureManager manager) {
        // --- player ---
        manager.register(new HealFeature(plugin));
        manager.register(new FeedFeature(plugin));
        manager.register(new HungerFeature(plugin));
        manager.register(new SetHealthFeature(plugin));
        manager.register(new FlyFeature(plugin));
        manager.register(new GameModeFeature(plugin));
        manager.register(new RepairFeature(plugin));
        manager.register(new DurabilityFeature(plugin));
        manager.register(new FullToolsFeature(plugin));
        manager.register(new InvisibilityFeature(plugin));
        manager.register(new NoEffectsFeature(plugin));
        manager.register(new PauseEffectsFeature(plugin));
        manager.register(new PotionPresetsFeature(plugin));
        manager.register(new TotemFeature(plugin));
        manager.register(new RandomHeadFeature(plugin));

        // --- teleportation ---
        manager.register(new TopFeature(plugin));
        manager.register(new OfflineTeleportFeature(plugin));
        manager.register(new SpawnFeature(plugin));
        manager.register(new WarpFeature(plugin));

        // --- inventory ---
        manager.register(new InvseeFeature(plugin));
        manager.register(new EnderChestViewFeature(plugin));
        manager.register(new KeepInventoryFeature(plugin));
        manager.register(new InventoryRollbackFeature(plugin));
        manager.register(new KitFeature(plugin));
        manager.register(new ItemMakerFeature(plugin));
        manager.register(new AutoClearFeature(plugin));

        // --- moderation ---
        manager.register(new HalfHeartFeature(plugin));
        manager.register(new VanishFeature(plugin));
        manager.register(new FreezeFeature(plugin));
        manager.register(new ServerLockFeature(plugin));
        manager.register(new CommandBlockerFeature(plugin));
        manager.register(new RecordingModeFeature(plugin));

        // --- world ---
        manager.register(new DimensionLockFeature(plugin));
        manager.register(new FakeWorldBorderFeature(plugin));
        manager.register(new ChunkToolsFeature(plugin));

        // --- chat ---
        manager.register(new ChatMuteFeature(plugin));
        manager.register(new ClearChatFeature(plugin));
        manager.register(new ChatFilterFeature(plugin));
        manager.register(new JoinLeaveMessagesFeature(plugin));
        manager.register(new PrivateMessagesFeature(plugin));
        manager.register(new CommandFeedbackFeature(plugin));
        manager.register(new FeedbackSoundsFeature(plugin));
        manager.register(new VoiceChatMuteFeature(plugin));

        // --- social ---
        // Team chat reads its membership from the team system, so that one is built first.
        TeamFeature teams = new TeamFeature(plugin);
        manager.register(teams);
        manager.register(new TeamChatFeature(plugin, teams));
        manager.register(new NicknameFeature(plugin));

        // --- headline systems ---
        manager.register(new NpcFeature(plugin));
        manager.register(new VillagerMakerFeature(plugin));
        manager.register(new SkinFeature(plugin));
        manager.register(new DeathActionsFeature(plugin));
        manager.register(new OrbitalWeaponsFeature(plugin));
        manager.register(new StasisFeature(plugin));
        manager.register(new PermissionsFeature(plugin));
    }
}
