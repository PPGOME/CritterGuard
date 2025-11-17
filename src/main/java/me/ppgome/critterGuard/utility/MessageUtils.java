package me.ppgome.critterGuard.utility;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Location;

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
                " Y: " + (int)location.getY() +
                " Z: " + (int)location.getZ() +
                "]", NamedTextColor.RED);
        Component xaerosMapText = Component.text("xaero-waypoint:CritterLoc:C:" +
                (int)location.getX() + ":" +
                (int)location.getY() + ":" +
                (int)location.getZ() + ":1:true:0:Internal-overworld-waypoints", NamedTextColor.WHITE);
        return voxelMapText.appendNewline().append(xaerosMapText);
    }

    public static Component miniMessageDeserialize(String message) {
        return mm.deserialize(message);
    }

}
