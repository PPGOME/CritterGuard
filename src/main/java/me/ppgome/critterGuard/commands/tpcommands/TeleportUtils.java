package me.ppgome.critterGuard.commands.tpcommands;

import me.ppgome.critterGuard.CritterGuard;
import me.ppgome.critterGuard.PlayerMeta;
import me.ppgome.critterGuard.commands.CommandUtils;
import me.ppgome.critterGuard.database.SavedAnimal;
import me.ppgome.critterGuard.utility.PlaceholderParser;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * A utility class containing static methods and a functional interface for the teleportation commands.
 */
public class TeleportUtils {

    /**
     * Validates player existence and retrieves PlayerMeta asynchronously
     */
    public static void validateAndExecuteTeleport(
            Player sender,
            String targetPlayerName,
            String critterIdentifier,
            CritterGuard plugin,
            TeleportAction action) {

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            OfflinePlayer targetPlayer = Bukkit.getOfflinePlayer(targetPlayerName);
            if (!targetPlayer.hasPlayedBefore() && !targetPlayer.isOnline()) {
                sender.sendMessage(PlaceholderParser
                        .of(plugin.getCGConfig().TELEPORT_NO_PLAYER)
                        .player(targetPlayerName)
                        .parse());
                return;
            }

            PlayerMeta playerMeta = plugin.getCritterCache().getPlayerMeta(targetPlayer.getUniqueId());
            if (playerMeta == null) {
                sender.sendMessage(plugin.getCGConfig().TELEPORT_NO_PLAYERMETA);
                return;
            }

            Bukkit.getScheduler().runTask(plugin, () -> {
                SavedAnimal critter = CommandUtils.searchByIdentifier(critterIdentifier, playerMeta);
                Entity critterEntity = Bukkit.getEntity(critter.getEntityUuid());

                if (critter == null) {
                    sender.sendMessage(PlaceholderParser
                            .of(plugin.getCGConfig().TELEPORT_NO_MATCH)
                            .player(targetPlayer.getName())
                            .identifier(critterIdentifier)
                            .parse());
                    return;
                } else if(critterEntity == null) {
                    // Load the chunk the plugin last saw the entity in and try to grab it that way
                    Chunk lastChunk = critter.getLastLocation().getChunk();
                    lastChunk.load();
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        Entity chunkEntity = Bukkit.getEntity(critter.getEntityUuid());
                        if(chunkEntity != null) {
                            action.execute(sender, chunkEntity, targetPlayer.getName(), plugin);
                        } else {
                            sender.sendMessage(PlaceholderParser
                                    .of(plugin.getCGConfig().TELEPORT_NO_MATCH)
                                    .player(targetPlayer.getName())
                                    .identifier(critterIdentifier)
                                    .parse());
                        }
                    });
                    return;
                }

                action.execute(sender, critterEntity, targetPlayer.getName(), plugin);
            });
        });
    }

    /**
     * Functional interface for teleport actions.
     */
    public interface TeleportAction {
        void execute(Player sender, Entity critterEntity, String targetPlayerName, CritterGuard plugin);
    }
}
