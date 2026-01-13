package me.ppgome.critterGuard.commands;

import me.ppgome.critterGuard.CGConfig;
import me.ppgome.critterGuard.CritterGuard;
import me.ppgome.critterGuard.disguisesaddles.DisguiseSaddleHandler;
import me.ppgome.critterGuard.disguisesaddles.LibsDisguiseProvider;
import me.ppgome.critterGuard.utility.CritterTamingHandler;
import me.ppgome.critterGuard.utility.CritterUtils;
import me.ppgome.critterGuard.utility.MessageUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * This class represents the command used to toggle the status of a mount controller seeing their own mount's disguise.
 */
public class ShowDisguiseSubCommand implements SubCommandHandler {

    /**
     * The instance of the plugin.
     */
    private final CritterGuard plugin;
    /**
     * The instance of the configuration class.
     */
    private CGConfig config;
    /**
     * The instance of the plugin's DisguiseSaddleHandler
     */
    private DisguiseSaddleHandler disguiseHandler;
    /**
     * The instance of the plugin's LibsDisguiseProvider
     */
    private LibsDisguiseProvider disguiseProvider;

    /**
     * Constructor for ShowDisguiseSubCommand.
     * Initializes the command with the plugin instance.
     *
     * @param plugin The instance of the CritterGuard plugin.
     */
    public ShowDisguiseSubCommand(CritterGuard plugin) {
        this.plugin = plugin;
        config = plugin.getCGConfig();
        this.disguiseHandler = plugin.getDisguiseSaddleHandler();
        this.disguiseProvider = plugin.getDisguiseProvider();
    }

    @Override
    public void execute(CommandSender sender, String[] args) {

        if(!(sender instanceof Player player)) return;
        Entity mount = player.getVehicle();
        if(!CritterUtils.isMountableEntity(mount)) return;

        boolean isDisguised = disguiseProvider.isDisguisedToPlayer(player, mount);

        // Simple toggle
        if(args.length == 0) {
            if(isDisguised) {
                toggleOff(mount, player);
            } else {
                toggleOn(mount, player);
            }
        }
        // Player specifies a toggle state
        else if(args.length == 1) {
            String arg = args[0];
            // Specified state is on
            if(arg.equals("on")) {
                // Is the player already seeing it disguised?
                if(isDisguised) {
                    player.sendMessage(config.DISGUISE_ALREADY_ENABLED);
                } else {
                    toggleOn(mount, player);
                }
            }
            // Specified state is off
            else if(arg.equals("off")) {
                // Is the player already seeing it disguised?
                if(isDisguised) {
                    toggleOff(mount, player);
                } else {
                    player.sendMessage(config.DISGUISE_ALREADY_DISABLED);
                }
            } else {
                player.sendMessage(getUsage());
            }
        }
    }

    /**
     * Toggles the visibility of the player's own mount's disguise on.
     *
     * @param mount The mount whose disguise is becoming visible
     * @param player The player who is having their state toggled on
     */
    private void toggleOn(Entity mount, Player player) {
        if(disguiseHandler.toggleSaddleDisguise(mount, player, true)) {
            player.sendMessage(config.DISGUISE_VIEW_SELF_ENABLED);
        }
    }

    /**
     * Toggles the visibility of the player's own mount's disguise off.
     *
     * @param mount The mount whose disguise is being removed for the player
     * @param player The player who is having their state toggled off
     */
    private void toggleOff(Entity mount, Player player) {
        if(disguiseHandler.toggleSaddleDisguise(mount, player, false)) {
            player.sendMessage(config.DISGUISE_VIEW_SELF_DISABLED);
        }
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if(args.length == 1) return List.of("on", "off");
        return List.of();
    }

    @Override
    public String getCommandName() {
        return "showdisguise";
    }

    @Override
    public String getDescription() {
        return "Toggles if you can see the disguise of the mount you're controlling.";
    }

    @Override
    public Component getUsage() {
        return MessageUtils.miniMessageDeserialize(config.PREFIX + " " + getStringUsage());
    }

    @Override
    public String getStringUsage() {
        return "<red>Usage: /critter showdisguise [on/off]</red>";
    }

    @Override
    public String getPermission() {
        return "critterguard.showdisguise";
    }

    @Override
    public int getMinArgs() {
        return 0;
    }
}
