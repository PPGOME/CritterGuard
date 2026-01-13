package me.ppgome.critterGuard.commands;

import me.ppgome.critterGuard.CGConfig;
import me.ppgome.critterGuard.CritterCache;
import me.ppgome.critterGuard.CritterGuard;
import me.ppgome.critterGuard.actions.TameAction;
import me.ppgome.critterGuard.utility.MessageUtils;
import me.ppgome.critterGuard.utility.PlaceholderParser;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.scheduler.BukkitTask;

import java.util.List;
import java.util.UUID;

/**
 * This class represents the command used to tame an entity to a player.
 */
public class TameSubCommand implements SubCommandHandler {

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
     * Constructor for TameSubCommand.
     * Initializes the command with the plugin instance.
     *
     * @param plugin The instance of the CritterGuard plugin.
     */
    public TameSubCommand(CritterGuard plugin) {
        this.plugin = plugin;
        this.config = plugin.getCGConfig();
        this.critterCache = plugin.getCritterCache();
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if(!(sender instanceof org.bukkit.entity.Player player)) return;

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String targetPlayerName = args[0];
            OfflinePlayer targetPlayer = Bukkit.getOfflinePlayer(targetPlayerName);

            if(targetPlayer.hasPlayedBefore()) {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    UUID senderUuid = player.getUniqueId();
                    TameAction tameAction = new TameAction(player, targetPlayer, plugin);
                    player.sendMessage(PlaceholderParser
                            .of(config.CLICK_TAME)
                            .player(targetPlayer.getName())
                            .click()
                            .parse());

                    // Set timeout
                    BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        if (critterCache.isAwaitingClick(senderUuid)) {
                            critterCache.removeAwaitingClick(senderUuid);
                            if (player.isOnline()) {
                                player.sendMessage(config.CLICK_TIMEOUT);
                            }
                        }
                    }, 20L * 15L); // 15 seconds timeout

                    critterCache.addAwaitingClick(senderUuid, tameAction, task);

                });
            } else {
                player.sendMessage(config.ACCESS_NO_PLAYER);
            }
        });

    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if(args.length != 1) return List.of();
        return null; // Let Bukkit handle player name completions
    }

    @Override
    public String getCommandName() {
        return "tame";
    }

    @Override
    public String getDescription() {
        return "Forces an entity to be tamed to the specified player.";
    }

    @Override
    public Component getUsage() {
        return MessageUtils.miniMessageDeserialize(config.PREFIX + " " + getStringUsage());
    }

    @Override
    public String getStringUsage() {
        return "<red>Usage: /critter tame <playerName></red>";
    }

    @Override
    public String getPermission() {
        return "critterguard.tame";
    }

    @Override
    public int getMinArgs() {
        return 1;
    }
}
