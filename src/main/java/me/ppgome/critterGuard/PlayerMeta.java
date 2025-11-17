package me.ppgome.critterGuard;

import me.ppgome.critterGuard.database.MountAccess;
import me.ppgome.critterGuard.database.SavedAnimal;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * PlayerMeta holds metadata for a player, including their UUID and a list of MountAccess objects
 * representing the mounts they have access to. This data can be expanded in the future to include
 * additional player-specific information as needed.
 */
public class PlayerMeta {

    /**
     * The UUID of the player.
     */
    private UUID uuid;

    /**
     * List of all critters owned by the player.
     */
    private ArrayList<SavedAnimal> ownedList;

    /**
     * The list of mounts the player has access to.
     */
    private Set<MountAccess> accessList;

    /**
     * The instance of the configuration class.
     */
    private CGConfig config;

    /**
     * The NamespacedKey used for interacting with the player's persistent data.
     */
    private NamespacedKey notificationKey;

    /**
     * Initializes PlayerMeta for a player.
     * @param uuid the UUID of the player.
     */
    public PlayerMeta(UUID uuid, CritterGuard plugin) {
        this.uuid = uuid;
        this.ownedList = new ArrayList<>();
        this.accessList = new java.util.HashSet<>();
        this.config = plugin.getCGConfig();
        notificationKey = new NamespacedKey(plugin, "cg_notif_toggle");
    }

    //------------------------------------------------------------------------------------------------------------------

    /**
     * Adds a critter to the player's list of owned SavedAnimals.
     *
     * @param savedAnimal the SavedAnimal instance of the critter being added
     */
    public void addOwnedAnimal(SavedAnimal savedAnimal) {
        this.ownedList.add(savedAnimal);
    }

    /**
     * Removes a critter from the player's list of owned SavedAnimals.
     *
     * @param savedAnimal the SavedAnimal instance of the critter being removed
     */
    public void removeOwnedAnimal(SavedAnimal savedAnimal) {
        this.ownedList.remove(savedAnimal);
        // Update indices
        for (int i = 0; i < ownedList.size(); i++) {
            ownedList.get(i).setIndex(i + 1);
        }
    }

    /**
     * Adds a MountAccess object to the player's access list, representing a player-mount relationship.
     *
     * @param access The access being added
     */
    public void addMountAccess(MountAccess access) {
        this.accessList.add(access);
    }

    /**
     * Removes a MountAccess object from the player's access list.
     *
     * @param access The access being removed
     */
    public void removeMountAccess(MountAccess access) {
        if(accessList.remove(access)) return;
        for(MountAccess mountAccess : accessList) {
            if(mountAccess.getMountUuid().equals(access.getMountUuid())) {
                accessList.remove(mountAccess);
                return;
            }
        }
    }

    /**
     * Fetches the state of the player's notification display option from their persistent data.
     *
     * @return True if they're enabled, false if not
     */
    public boolean showNotifications() {
        Player player = Bukkit.getPlayer(uuid);
        if(player == null) return false;
        PersistentDataContainer container = player.getPersistentDataContainer();
        if(container.has(notificationKey, PersistentDataType.BOOLEAN)) {
            return container.get(notificationKey, PersistentDataType.BOOLEAN);
        }
        container.set(notificationKey, PersistentDataType.BOOLEAN, true);
        return true;
    }

    /**
     * Toggles the state of the player's notifications.
     *
     * @param isEnabling True if the player is enabling notifications, false if disabling
     */
    public void toggleNotifications(boolean isEnabling) {
        Player player = Bukkit.getPlayer(uuid);
        if(player == null) return;
        PersistentDataContainer container = player.getPersistentDataContainer();
        if(container.has(notificationKey, PersistentDataType.BOOLEAN)) {
            if(isEnabling) {
                player.sendMessage(config.NOTIFICATION_TOGGLE_ON);
                container.set(notificationKey, PersistentDataType.BOOLEAN, true);
                return;
            }
        }
        player.sendMessage(config.NOTIFICATION_TOGGLE_OFF);
        container.set(notificationKey, PersistentDataType.BOOLEAN, false);
    }

    //------------------------------------------------------------------------------------------------------------------

    /**
     * Gets the UUID of the player.
     * @return the player's UUID.
     */
    public UUID getUuid() {
        return uuid;
    }

    /**
     * Gets the list of mounts owned by the player.
     * @return the list of owned mounts.
     */
    public List<SavedAnimal> getOwnedList() {
        return ownedList;
    }

    /**
     * Gets the list of mounts the player has access to.
     * @return the list of mount access objects.
     */
    public Set<MountAccess> getAccessList() {
        return accessList;
    }

    /**
     * Retrieves an owned critter by its UUID.
     * @param animalUuid the UUID of the mount to retrieve.
     * @return the SavedAnimal if found, null otherwise.
     */
    public SavedAnimal getOwnedAnimalByUuid(UUID animalUuid) {
        for(SavedAnimal savedAnimal : ownedList) {
            if(savedAnimal.getEntityUuid().equals(animalUuid)) return savedAnimal;
        }
        return null;
    }

}
