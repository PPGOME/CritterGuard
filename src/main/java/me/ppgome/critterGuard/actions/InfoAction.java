package me.ppgome.critterGuard.actions;

import me.ppgome.critterGuard.CritterCache;
import me.ppgome.critterGuard.CritterGuard;
import me.ppgome.critterGuard.database.MountAccess;
import me.ppgome.critterGuard.database.SavedMount;
import me.ppgome.critterGuard.utility.CritterUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.*;
import org.bukkit.scheduler.BukkitScheduler;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.*;

public class InfoAction implements ActionHandler {

    private Player player;
    private CritterGuard plugin;
    private CritterCache cache;

    //------------------------------------------------------------------------------------------------------------------

    public InfoAction(Player player, CritterGuard plugin) {
        this.player = player;
        this.plugin = plugin;
        this.cache = plugin.getCritterCache();
    }

    //------------------------------------------------------------------------------------------------------------------

    @Override
    public void execute(Entity clickedEntity) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String ownerName;
            if(clickedEntity instanceof Tameable tameable && tameable.isTamed()) {
                ownerName = tameable.getOwner().getName();
                buildInfo(clickedEntity, player, ownerName);
            } else {
                SavedMount mount = cache.getSavedMount(clickedEntity.getUniqueId());
                if(mount != null) {
                    ownerName = Bukkit.getOfflinePlayer(mount.getEntityOwnerUuid()).getName();
                    buildInfo(clickedEntity, player, ownerName);
                } else {
                    buildInfo(clickedEntity, player, "Nobody");
                }
            }
        });
    }

    /**
     * Puts together the message to send the player who's finishing up a "/cg info" query.
     *
     * @param entity the entity being checked
     * @param player the player checking the entity
     * @param ownerName the name of the owner of the entity
     */
    private void buildInfo(Entity entity, Player player, String ownerName) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            Component message = Component.text(ownerName, NamedTextColor.YELLOW)
                    .append(Component.text(" owns this critter!", NamedTextColor.GREEN));
            if(entity instanceof AbstractHorse abstractHorse) {
                DecimalFormat df = new DecimalFormat("#.###");
                df.setRoundingMode(RoundingMode.UP);
                message = message.appendNewline().append(Component.text("Speed: ", NamedTextColor.RED))
                        .append(Component.text(df.format(abstractHorse.getAttribute(Attribute.MOVEMENT_SPEED).getValue()),
                                NamedTextColor.YELLOW)).appendNewline()
                        .append(Component.text("Jump: ", NamedTextColor.BLUE))
                        .append(Component.text(df.format(abstractHorse.getAttribute(Attribute.JUMP_STRENGTH).getValue()),
                                NamedTextColor.YELLOW)).appendNewline()
                        .append(Component.text("Health: ", NamedTextColor.GREEN))
                        .append(Component.text(df.format(abstractHorse.getAttribute(Attribute.MAX_HEALTH).getValue()),
                                NamedTextColor.YELLOW));

                if(entity instanceof Llama llama) {
                    message = message.appendNewline().appendNewline().append(Component.text("Strength: ",
                                    NamedTextColor.LIGHT_PURPLE))
                            .append(Component.text(llama.getStrength(),
                                    NamedTextColor.YELLOW));
                }
            }

            SavedMount savedMount = cache.getSavedMount(entity.getUniqueId());

            if(CritterUtils.isMountableEntity(entity) && savedMount != null) {
                buildAccessList(message, entity, savedMount.getAccessList());
                return;
            }

            player.sendMessage(message);
            ActionUtils.clickDing(player, entity);
        });
    }

    private void buildAccessList(Component message, Entity entity, HashMap<UUID, MountAccess> mountAccessMap) {
        BukkitScheduler scheduler = Bukkit.getScheduler();
        scheduler.runTaskAsynchronously(plugin, () -> {
            Component listsMessage = Component.text("Full access:", NamedTextColor.GOLD);
            List<String> fullNamesList = new ArrayList<>();
            List<String> passengerNamesList = new ArrayList<>();

            for(MountAccess mountAccess : mountAccessMap.values()) {
                String name = Bukkit.getOfflinePlayer(UUID.fromString(mountAccess.getPlayerUuid())).getName();
                if(mountAccess.isFullAccess()) fullNamesList.add(name);
                else passengerNamesList.add(name);
            }

            if(fullNamesList.isEmpty()) {
                listsMessage = listsMessage.append(Component.text(" None", NamedTextColor.RED));
            } else {
                listsMessage = listColourBuilder(listsMessage, fullNamesList);
            }

            listsMessage = listsMessage.appendNewline().append(Component.text("Passenger access:", NamedTextColor.GOLD));

            if(passengerNamesList.isEmpty()) {
                listsMessage = listsMessage.append(Component.text(" None", NamedTextColor.RED));
            } else {
                listsMessage = listColourBuilder(listsMessage, passengerNamesList);
            }
            Component finalListsMessage = listsMessage;
            scheduler.runTask(plugin, () -> sendInfo(player, entity, message, finalListsMessage));
        });
    }

    private Component listColourBuilder(Component listsMessage, List<String> namesList) {
        NamedTextColor colour;
        for(int i = 0; i < namesList.size(); i++) {
            colour = (i % 2) != 1 ? NamedTextColor.GRAY : NamedTextColor.DARK_GRAY;
            listsMessage = listsMessage.append(Component.text(" " + namesList.get(i), colour));
        }
        return listsMessage;
    }

    private void sendInfo(Player player, Entity entity, Component message, Component listsMesasge) {

        if(listsMesasge != null) message = message.appendNewline().appendNewline().append(listsMesasge);

        player.sendMessage(message);
        ActionUtils.clickDing(player, entity);
    }

}
