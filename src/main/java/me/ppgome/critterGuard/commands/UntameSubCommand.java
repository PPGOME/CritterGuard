package me.ppgome.critterGuard.commands;

import me.ppgome.critterGuard.CGConfig;
import me.ppgome.critterGuard.CritterCache;
import me.ppgome.critterGuard.CritterGuard;
import me.ppgome.critterGuard.actions.UntameAction;
import me.ppgome.critterGuard.utility.MessageUtils;
import me.ppgome.critterGuard.utility.PlaceholderParser;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.List;
import java.util.UUID;

/**
 * This class represents the command used to untame a critter.
 */
public class UntameSubCommand implements SubCommandHandler {

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
     * Constructor for UntameSubCommand.
     * Initializes the command with the plugin instance.
     *
     * @param plugin The instance of the CritterGuard plugin.
     */
    public UntameSubCommand(CritterGuard plugin) {
        this.plugin = plugin;
        this.config = plugin.getCGConfig();
        this.critterCache = plugin.getCritterCache();
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if(!(sender instanceof Player player)) return;
        UUID senderUuid = player.getUniqueId();
        UntameAction action = new UntameAction(player, plugin);
        player.sendMessage(PlaceholderParser
                .of(config.CLICK_UNTAME)
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
        critterCache.addAwaitingClick(senderUuid, action, task);
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return List.of();
    }

    @Override
    public String getCommandName() {
        return "untame";
    }

    @Override
    public String getDescription() {
        return "Untame a critter.";
    }

    @Override
    public Component getUsage() {
        return MessageUtils.miniMessageDeserialize(config.PREFIX + " " + getStringUsage());
    }

    @Override
    public String getStringUsage() {
        return "<red>Usage: /critter untame</red>";
    }

    @Override
    public String getPermission() {
        return "critterguard.untame";
    }

    @Override
    public int getMinArgs() {
        return 0;
    }
}