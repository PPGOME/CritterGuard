package me.ppgome.critterGuard.commands;

import io.papermc.paper.entity.LookAnchor;
import io.papermc.paper.math.Position;
import me.ppgome.critterGuard.*;
import me.ppgome.critterGuard.database.SavedAnimal;
import me.ppgome.critterGuard.utility.MessageUtils;
import me.ppgome.critterGuard.utility.PlaceholderParser;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;

import java.util.List;
import java.util.UUID;

/**
 * This class represents the command used to give the player the coordinates of a specific one of their critters.
 */
public class GPSSubCommand implements SubCommandHandler {

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
     * Constructor for GPSSubCommand.
     * Initializes the command with the plugin instance.
     *
     * @param plugin The instance of the CritterGuard plugin.
     */
    public GPSSubCommand(CritterGuard plugin) {
        this.plugin = plugin;
        this.config = plugin.getCGConfig();
        this.critterCache = plugin.getCritterCache();
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if(!(sender instanceof org.bukkit.entity.Player player)) return;
        String critterIdentifier = args[0].toLowerCase();
        PlayerMeta playerMeta = critterCache.getPlayerMeta(player.getUniqueId());

        // Validate player metadata
        if(playerMeta == null) {
            player.sendMessage(config.GPS_NO_PLAYERMETA);
            return;
        }

        // Search for the critter by name, UUID, or index
        SavedAnimal matchedSavedAnimal = CommandUtils.searchByIdentifier(critterIdentifier, playerMeta);
        if(matchedSavedAnimal == null) {
            player.sendMessage(PlaceholderParser.of(config.GPS_NO_MATCH).identifier(critterIdentifier).parse());
            return;
        }
        Entity matchedEntity = Bukkit.getEntity(matchedSavedAnimal.getEntityUuid());

        // If match found, notify the player
        Location location;
        if(matchedEntity != null) {
            location = matchedEntity.getLocation();
            player.sendMessage(MessageUtils.locationBuilder(matchedEntity.getLocation(), NamedTextColor.GREEN));
            if(config.ENABLE_MINIMAP_MOD_OUTPUT) player.sendMessage(MessageUtils.minimapOutput(location));
            player.lookAt(matchedEntity, LookAnchor.EYES, LookAnchor.FEET);
        } else if(matchedSavedAnimal != null) {
            location = matchedSavedAnimal.getLastLocation();
            player.sendMessage(MessageUtils.locationBuilder(location, NamedTextColor.GREEN));
            if(config.ENABLE_MINIMAP_MOD_OUTPUT) player.sendMessage(MessageUtils.minimapOutput(location));
            player.lookAt(Position.block(matchedSavedAnimal.getLastLocation()), LookAnchor.EYES);
        } else {
            player.sendMessage(PlaceholderParser.of(config.GPS_NO_MATCH).identifier(critterIdentifier).parse());
        }
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return List.of();
    }

    @Override
    public String getCommandName() {
        return "gps";
    }

    @Override
    public String getDescription() {
        return "Get the location of one of your critters.";
    }

    @Override
    public Component getUsage() {
        return MessageUtils.miniMessageDeserialize(config.PREFIX + " " + getStringUsage());
    }

    @Override
    public String getStringUsage() {
        return "<red>Usage: /critter gps <critterName OR uuid OR number>";
    }

    @Override
    public String getPermission() {
        return "critterguard.gps";
    }

    @Override
    public int getMinArgs() {
        return 1;
    }
}
