package me.ppgome.critterGuard.utility;

import me.ppgome.critterGuard.CritterCache;
import me.ppgome.critterGuard.database.SavedMount;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class MessageUtils {

    private static MiniMessage mm = MiniMessage.miniMessage();

    //------------------------------------------------------------------------------------------------------------------

    public static String capitalizeFirstLetter(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    public static Component locationBuilder(Location location, NamedTextColor color) {
        return Component.text("Location: " + location.getBlockX()
                + ", " + location.getBlockY()
                + ", " + location.getBlockZ()
                + " in world: " + location.getWorld().getName(), color);
    }

    public static Component minimapOutput(Location location) {
        Component voxelMapText = Component.text("[Name: CritterLocation, X: " + (int)location.getX() +
                ", Y: " + (int)location.getY() +
                ", Z: " + (int)location.getZ() +
                "]", NamedTextColor.RED);
        Component xaerosMapText = Component.text("xaero-waypoint:CritterLoc:C:" +
                (int)location.getX() + ":" +
                (int)location.getY() + ":" +
                (int)location.getZ() + ":1:true:0:Internal-overworld-waypoints", NamedTextColor.WHITE);
        return voxelMapText.appendNewline().append(xaerosMapText);
    }

    /**
     * Send a notification to the owner of the mount regarding other users riding them.
     * Used for mounting, dismounting, and deaths.
     *
     * @param player The player triggering the notification
     * @param savedMount The mount the notification is about
     * @param notificationType The type of notification
     */
    public static void notifyPlayer(Player player, SavedMount savedMount, String notificationType,
                                    CritterCache critterCache) {
        Player owner = Bukkit.getPlayer(savedMount.getEntityOwnerUuid());
        // Send message to owner
        if(owner != null && owner.isOnline() &&
                critterCache.getPlayerMeta(owner.getUniqueId()).showNotifications()) {
            String mountString;
            if(savedMount.getEntityName() == null) mountString = String.valueOf(savedMount.getEntityUuid());
            else mountString = savedMount.getEntityName();

            owner.sendMessage(PlaceholderParser
                    .of(notificationType)
                    .player(player.getName())
                    .mount(mountString)
                    .parse());
        }
    }

    public static Component miniMessageDeserialize(String message) {
        return mm.deserialize(message);
    }

}
