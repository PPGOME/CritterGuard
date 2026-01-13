package me.ppgome.critterGuard.utility;

import me.ppgome.critterGuard.CGConfig;
import me.ppgome.critterGuard.CritterCache;
import me.ppgome.critterGuard.CritterGuard;
import me.ppgome.critterGuard.database.SavedMount;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Camel;
import org.bukkit.entity.Entity;
import org.bukkit.entity.HappyGhast;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

import static me.ppgome.critterGuard.utility.CritterUtils.isCamel;
import static me.ppgome.critterGuard.utility.CritterUtils.isHappyGhast;
import static me.ppgome.critterGuard.utility.MessageUtils.notifyPlayer;

/**
 * This class contains methods that handle multi-seated mounts.
 *
 * Some of these methods apply to disguised mounts while the rest apply to undisguised mounts.
 * Disguised mounts will stack players vertically to prevent the game from putting players inside each other.
 */
public class MountSeatHandler {

    /**
     * The instance of the plugin.
     */
    private CritterGuard plugin;
    /**
     * The instance of the configuration class.
     */
    private CGConfig config;

    /**
     * The instance of the plugin's cache.
     */
    private CritterCache critterCache;

    //------------------------------------------------------------------------------------------------------------------

    /**
     * Initializes the MountSeatHandler instance.
     * @param plugin the instance of the plugin
     */
    public MountSeatHandler(CritterGuard plugin) {
        this.plugin = plugin;
        this.config = plugin.getCGConfig();
        this.critterCache = plugin.getCritterCache();
    }

    //------------------------------------------------------------------------------------------------------------------

    /**
     * Checks if an entity is a player.
     *
     * @param entity The entity being checked.
     * @return True if it is, false if not.
     */
    public boolean isPlayer(Entity entity) {
        return entity instanceof Player;
    }

    /**
     * Returns the maximum number of players allowed to sit on a mount.
     *
     * @param mount The mount being checked.
     * @return The maximum number of players allowed to sit on a mount.
     */
    public int getMaxSeats(Entity mount) {
        if(isCamel(mount)) return 2;
        else if(isHappyGhast(mount)) return 4;
        return 1;
    }

    /**
     * Checks if the given player is the driver of the mount.
     *
     * @param player     The player to check.
     * @param passengers The list of current passengers on the mount.
     * @return true if the player is the driver, false otherwise.
     */
    public boolean isDriver(Player player, List<Entity> passengers) {
        return passengers.size() <= 1 || passengers.getFirst().getUniqueId().equals(player.getUniqueId());
    }

    /**
     * Checks if player is at the bottom of the stack (the driver) when using a disguised saddle.
     *
     * @param player The player being checked.
     * @return true if the player is the driver, false otherwise.
     */
    public boolean isDriverDisguised(Player player) {
        return !(isPlayer(player.getVehicle()));
    }

    /**
     * Gets a list of all players in a disguised mount's player stack.
     *
     * @param mount the mount whose stack is being fetched
     * @return the list of players in the stack
     */
    public List<Entity> getPlayerStack(Entity mount) {
        List<Entity> stack = new ArrayList<>();

        int maxPassengers;
        if (isCamel(mount)) {
            maxPassengers = 2;
        } else if (isHappyGhast(mount)) {
            maxPassengers = 4;
        } else {
            return stack; // Return empty list for other entity types
        }

        Entity current = mount;

        // Get the first passenger in each level (excluding the bottom entity)
        for (int i = 0; i < maxPassengers; i++) {
            List<Entity> passengers = current.getPassengers();

            if (passengers.isEmpty()) {
                break; // No more passengers in the chain
            }

            // Get the first passenger only
            Entity firstPassenger = passengers.getFirst();
            stack.add(firstPassenger);
            current = firstPassenger;
        }
        return stack;
    }

    /**
     * Builds a new player stack on a multi-seat mount.
     *
     * @param mount The mount the player stack is on.
     * @param players The players in the stack.
     */
    private void buildPlayerStack(Entity mount, List<Entity> players) {
        if (players.isEmpty()) return;

        // Stack players on top of each other from bottom to top
        for (int i = 1; i < players.size(); i++) {
            Entity below = players.get(i - 1);
            Entity above = players.get(i);
            Bukkit.getScheduler().runTaskLater(plugin, () -> below.addPassenger(above), 3L);
        }

        // Finally, mount the bottom player to the actual mount
        Entity bottomPlayer = players.getFirst();
        Bukkit.getScheduler().runTaskLater(plugin, () -> mount.addPassenger(bottomPlayer), 3L);
    }

    /**
     * Fixes the player tower on a disguised mount when someone dismounts.
     *
     * @param mount The entity having its position fixed.
     */
    public void fixPlayerStack(Entity mount, Entity entity) {
        Entity below = entity.getVehicle();
        Entity above = entity.getPassengers().getFirst();

        if(isPlayer(below) && above != null) below.addPassenger(above);
        else if(above != null) mount.addPassenger(above);
    }

    /**
     * Finds a new driver among the passengers who have control access to the mount.
     *
     * @param passengers The list of current passengers on the mount.
     * @param savedMount The SavedMount object associated with the mount.
     * @return The new driver entity if found, null otherwise.
     */
    public Entity findNewDriver(List<Entity> passengers, SavedMount savedMount) {
        for (Entity passenger : passengers) {
            if (hasControlAccess(passenger, savedMount)) return passenger;
        }
        return null;
    }

    /**
     * Checks if the given entity has control access to the mount.
     *
     * @param entity     The entity to check.
     * @param savedMount The SavedMount object associated with the mount.
     * @return true if the entity has control access, false otherwise.
     */
    public boolean hasControlAccess(Entity entity, SavedMount savedMount) {
        return savedMount.isOwner(entity.getUniqueId()) || savedMount.hasFullAccess(entity.getUniqueId());
    }

    /**
     * Transfers control of the undisguised mount to a new driver and reorders the passengers accordingly.
     *
     * @param mount      The undisguised mount entity.
     * @param passengers The list of current passengers on the mount.
     * @param newDriver  The entity that will become the new driver.
     */
    public void transferControl(Entity mount, List<Entity> passengers, Entity newDriver) {
        List<Entity> reorderedPassengers = new ArrayList<>();

        reorderedPassengers.add(newDriver);
        passengers.remove(newDriver);
        reorderedPassengers.addAll(passengers);

        Bukkit.getScheduler().runTaskLater(plugin, mount::eject, 3L);
        if (!(newDriver instanceof Player newDriverPlayer)) return;
        Component message = PlaceholderParser.of(config.SEAT_SWAP_SUCCESS).player(newDriverPlayer.getName()).parse();
        for (Entity passenger : reorderedPassengers) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> mount.addPassenger(passenger), 3L);
            passenger.sendMessage(message);
        }
    }

    /**
     * Transfers control of the disguised mount to a new driver and reorders the passengers accordingly.
     *
     * @param mount      The disguised mount entity.
     * @param passengers The list of current passengers on the mount.
     * @param newDriver  The entity that will become the new driver.
     */
    public void transferControlDisguised(Entity mount, List<Entity> passengers, Entity newDriver) {
        List<Entity> reorderedPassengers = new ArrayList<>();

        reorderedPassengers.add(newDriver);
        passengers.remove(newDriver);
        reorderedPassengers.addAll(passengers);

        for(Entity passenger : reorderedPassengers) {
            Bukkit.getScheduler().runTaskLater(plugin, passenger::eject, 3L);
        }
        if (!(newDriver instanceof Player newDriverPlayer)) return;
        Component message = PlaceholderParser.of(config.SEAT_SWAP_SUCCESS).player(newDriverPlayer.getName()).parse();

        for(Entity passenger : reorderedPassengers) {
            passenger.sendMessage(message);
        }

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            buildPlayerStack(mount, reorderedPassengers);
        }, 3L);
    }

    /**
     * Dismounts all passengers from the mount and notifies them.
     *
     * @param mount      The mount entity.
     * @param passengers The list of current passengers on the mount.
     */
    public void dismountAllPassengers(Entity mount, List<Entity> passengers) {
        for (Entity passenger : passengers) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                mount.removePassenger(passenger);
                passenger.sendMessage(config.SEAT_SWAP_FAILURE);
            }, 3L);
        }
    }

    /**
     * Breaks apart a player stack formed from a disguised saddle.
     *
     * @param mount The mount being broken apart.
     */
    public void disbandPlayerStack(Entity mount) {
        boolean isHappyGhast = isHappyGhast(mount);
        Location getBlockBelow;
        if(isHappyGhast) getBlockBelow = getClosestBlockBelow(mount);
        else getBlockBelow = null;

        for(Entity entity : getPlayerStack(mount)) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                entity.eject();
                if(isHappyGhast) entity.teleport(getBlockBelow);
                entity.sendMessage(config.SEAT_SWAP_FAILURE);
            }, 3L);
        }
    }

    /**
     * Used mostly for handling disguised happy ghasts as their hitboxes can be a mess and tend to get players stuck.
     * This will place the player at the lowest block relative to the ghast.
     *
     * @param entity The entity having its location checked.
     * @return The location of the safe place to put the player.
     */
    public Location getClosestBlockBelow(Entity entity) {
        Location location = entity.getLocation().clone();
        for(int y = location.getBlockY(); y > -50; y--) {
            if(location.subtract(0, 1 ,0).getBlock().getType() == Material.AIR) continue;
            return location.add(0, 1 ,0);
        }
        // Put them above the happy ghast
        return location.add(0, 20, 0);
    }

    /**
     * Teleports entity to the nearest block below them when dismounting a disguised happy ghast.
     *
     * @param entity The entity being teleported.
     */
    public void teleportDown(Entity mount, Entity entity) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            mount.removePassenger(entity);
            entity.teleport(getClosestBlockBelow(mount));
        }, 3L);
    }
}
