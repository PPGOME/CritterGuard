package me.ppgome.critterGuard.database;

import com.j256.ormlite.field.DatabaseField;

import java.util.Date;
import java.util.HashMap;
import java.util.UUID;

/**
 * Represents a saved mount in the database.
 * This class is used to store information about mounts such as horses, camels, llamas, donkeys, mules, and happy ghasts.
 * It includes fields for the mount's UUID, name, owner UUID and name, entity type, lock date, color, style, and access list.
 */
public class SavedMount extends SavedAnimal {

    /**
     * The date when the mount is locked/initially saved
     */
    @DatabaseField
    private Date lockDate;

    /**
     * The style of the mount, applicable for horses (e.g., "BLACK_DOTS", "WHITE", etc.).
     */
    @DatabaseField
    private String style;

    /**
     * A list of UUIDs and their corresponding access levels for the mount.
     */
    HashMap<UUID, MountAccess> accessList = new HashMap<>();

    //------------------------------------------------------------------------------------------------------------------

    /**
     * Empty constructor required by ORMLite.
     */
    public SavedMount() {}

    /**
     * Constructor to create a new SavedMount horse instance.
     * @param mountUuid the UUID of the mount
     * @param mountName the name of the mount
     * @param mountOwnerUuid the UUID of the mount owner
     * @param mountOwnerName the name of the mount owner
     * @param entityType the type of the mount entity (e.g., "horse", "camel", etc.)
     * @param color the color of the mount (e.g., "white", "brown", etc.)
     * @param style the style of the mount (e.g., "chestnut", "black", etc.)
     */
    public SavedMount(String mountUuid, String mountName, String mountOwnerUuid, String mountOwnerName,
                      String entityType, String color, String style) {
        this.entityUuid = mountUuid;
        this.entityName = mountName;
        this.entityOwnerUuid = mountOwnerUuid;
        this.entityType = entityType;
        this.color = color;
        this.style = style;
        this.lockDate = new Date();
    }

    /**
     * Constructor to create a new SavedMount llama instance.
     * @param mountUuid the UUID of the mount
     * @param mountName the name of the mount
     * @param mountOwnerUuid the UUID of the mount owner
     * @param mountOwnerName the name of the mount owner
     * @param entityType the type of the mount entity (e.g., "horse", "camel", etc.)
     * @param color the color of the mount (e.g., "white", "brown", etc.)
     */
    public SavedMount(String mountUuid, String mountName, String mountOwnerUuid, String mountOwnerName,
                      String entityType, String color) {
        this.entityUuid = mountUuid;
        this.entityName = mountName;
        this.entityOwnerUuid = mountOwnerUuid;
        this.entityType = entityType;
        this.color = color;
        this.style = null; // Llamas do not have a style
        this.lockDate = new Date();
    }

    /**
     * Constructor to create a new SavedMount donkey, mule, camel, or happy ghast instance.
     * @param mountUuid the UUID of the mount
     * @param mountName the name of the mount
     * @param mountOwnerUuid the UUID of the mount owner
     * @param mountOwnerName the name of the mount owner
     * @param entityType the type of the mount entity (e.g., "horse", "camel", etc.)
     */
    public SavedMount(String mountUuid, String mountName, String mountOwnerUuid, String mountOwnerName,
                      String entityType) {
        this.entityUuid = mountUuid;
        this.entityName = mountName;
        this.entityOwnerUuid = mountOwnerUuid;
        this.entityType = entityType;
        this.color = null; // Donkeys, mules, camels, and ghasts do not have a color
        this.style = null; // Donkeys, mules, camels, and ghasts
        this.lockDate = new Date();
    }

    //------------------------------------------------------------------------------------------------------------------


    /**
     * Gets the lock date for the mount.
     * This is the date when the mount was last locked or saved.
     * @return the lock date of the mount
     */
    public Date getLockDate() {
        return lockDate;
    }

    /**
     * Sets the lock date for the mount.
     * This is the date when the mount was last locked or saved.
     * @param lockDate the date to set as the lock date
     */
    public void setLockDate(Date lockDate) {
        this.lockDate = lockDate;
    }

    /**
     * Gets the style of the mount.
     * This is applicable for horses.
     * @return the style of the mount (e.g., "BLACK_DOTS", "WHITE", etc.)
     */
    public String getStyle() {
        return style;
    }

    /**
     * Sets the style of the mount.
     * This is applicable for horses.
     * @param style the style of the mount (e.g., "BLACK_DOTS", "WHITE", etc.)
     */
    public void setStyle(String style) {
        this.style = style;
    }

    /**
     * Adds access for the given UUID to the mount.
     * @param uuid the UUID of the user to add access for
     * @param access the access level to grant to the user
     */
    public void addAccess(UUID uuid, MountAccess access) {
        this.accessList.put(uuid, access);
    }

    /**
     * Removes access for the given UUID from the mount.
     * @param uuid the UUID of the user to remove access for
     */
    public void removeAccess(UUID uuid) {
        this.accessList.remove(uuid);
    }

    /**
     * Checks if the given UUID has access to the mount.
     * @param uuid the UUID of the user to check
     * @return true if the user has access, false otherwise
     */
    public boolean hasAccess(UUID uuid) {
        return this.accessList.containsKey(uuid);
    }

    /**
     * Returns the object representing the specified player's access to this mount.
     * @param uuid The UUID of the player being fetched
     * @return the object representing the specified player's access to this mount
     */
    public MountAccess getMountAccess(UUID uuid) {
        return this.accessList.get(uuid);
    }

    /**
     * Gets the access list for the mount.
     * @return a HashMap of UUIDs and their corresponding access levels
     */
    public HashMap<UUID, MountAccess> getAccessList() {
        return accessList;
    }

    /**
     * Checks if the given UUID has full access to the mount.
     * Full access means the user can do everything with the mount, including riding, feeding, and interacting.
     * @param uuid the UUID of the user to check
     * @return true if the user has full access, false otherwise
     */
    public boolean hasFullAccess(UUID uuid) {
        MountAccess access = this.accessList.get(uuid);
        return access != null && access.isFullAccess();
    }

}
