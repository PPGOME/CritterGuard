package me.ppgome.critterGuard.commands;

import me.ppgome.critterGuard.CGConfig;
import me.ppgome.critterGuard.CritterCache;
import me.ppgome.critterGuard.CritterGuard;
import me.ppgome.critterGuard.actions.AccessAction;
import me.ppgome.critterGuard.utility.MessageUtils;
import me.ppgome.critterGuard.database.MountAccess;
import me.ppgome.critterGuard.utility.PlaceholderParser;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.List;
import java.util.UUID;

/**
 * This class represents the command used to give a player access to mounts.
 */
public class AccessSubCommand implements SubCommandHandler {

    /**
     * The instance of the plugin.
     */
    private final CritterGuard plugin;
    /**
     * The instance of the configuration class.
     */
    private CGConfig config;
    /**
     * The instance of the plugin's cache.
     */
    private CritterCache critterCache;

    /**
     * Constructor for AccessSubCommand.
     * Initializes the command with the plugin instance.
     *
     * @param plugin The instance of the CritterGuard plugin.
     */
    public AccessSubCommand(CritterGuard plugin) {
        this.plugin = plugin;
        this.config = plugin.getCGConfig();
        this.critterCache = plugin.getCritterCache();
    }

    // /critter access <add/remove> <full/passenger> <playername>
    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) return;

        // Parse command arguments
        boolean isAdd = args[0].equalsIgnoreCase("add");
        boolean isRemove = args[0].equalsIgnoreCase("remove");
        boolean isFullAccess = args[1].equalsIgnoreCase("full");
        boolean isPassengerAccess = args[1].equalsIgnoreCase("passenger");

        // Validate arguments
        if (!((isAdd && (isFullAccess || isPassengerAccess)) || isRemove)) {
            Bukkit.getScheduler().runTask(plugin, () -> player.sendMessage(getUsage()));
            return;
        }

        String playerName = isAdd ? args[2] : args[1];

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            OfflinePlayer playerBeingAdded = Bukkit.getOfflinePlayer(playerName);

            // Check if player exists (has played before)
            if (!playerBeingAdded.hasPlayedBefore() && !playerBeingAdded.isOnline()) {
                Bukkit.getScheduler().runTask(plugin, () -> player.sendMessage(PlaceholderParser
                        .of(config.ACCESS_NO_PLAYER).player(playerName).parse()));
                return;
            }

            UUID senderUuid = player.getUniqueId();
            AccessAction action = new AccessAction(player, playerBeingAdded, isAdd, isFullAccess, plugin);

            if(player.isInsideVehicle()) {
                action.execute(player.getVehicle());
                return;
            }

            Bukkit.getScheduler().runTask(plugin, () -> {
                sendClickMessage(player, playerBeingAdded, isAdd, isFullAccess);

                // Set timeout
                BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (critterCache.isAwaitingClick(senderUuid)) {
                        critterCache.removeAwaitingClick(senderUuid);
                        if (player.isOnline()) {
                            player.sendMessage(config.CLICK_TIMEOUT);
                        }
                    }
                }, 20L * 15L); // 15 seconds timeout
                critterCache.addAwaitingClick(senderUuid, action, task);
            });
        });
    }

    /**
     * Determines which message is sent to the player based on 2 criteria.
     *
     * @param player The player who the message is being sent to
     * @param playerBeingAdded The player who is being added
     * @param isAdd True if the player's access is being added, false if not
     * @param isFullAccess True if the access being granted/removed is full, false if it's passenger
     */
    public void sendClickMessage(Player player, OfflinePlayer playerBeingAdded, boolean isAdd, boolean isFullAccess) {
        String message;
        if(isAdd) {
            if(isFullAccess) {
                message = config.CLICK_GRANT_FULL_ACCESS;
            } else {
                message = config.CLICK_GRANT_PASSENGER_ACCESS;
            }
        } else {
            if(isFullAccess) {
                message = config.CLICK_REVOKE_FULL_ACCESS;
            } else {
                message = config.CLICK_REVOKE_PASSENGER_ACCESS;
            }
        }
        player.sendMessage(PlaceholderParser
                .of(message)
                .player(playerBeingAdded.getName())
                .click()
                .parse());
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if(args.length == 1) return List.of("add", "remove");
        else if(args.length == 2 && args[0].equalsIgnoreCase("add")) return List.of("full", "passenger");
        return null;
    }

    @Override
    public String getCommandName() {
        return "access";
    }

    @Override
    public String getDescription() {
        return "Manage access permissions for critters. Use 'add' to grant access or 'remove' to revoke access.";
    }

    @Override
    public Component getUsage() {
        return MessageUtils.miniMessageDeserialize(config.PREFIX + " " + getStringUsage());
    }

    @Override
    public String getStringUsage() {
        return "<red>Usage: /critter access <add/remove> <full/passenger> <playername></red>";
    }

    @Override
    public String getPermission() {
        return "critterguard.access";
    }

    @Override
    public int getMinArgs() {
        return 2;
    }
}
