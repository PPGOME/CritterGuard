package me.ppgome.critterGuard;

import me.ppgome.critterGuard.commands.actions.CommandAction;
import me.ppgome.critterGuard.database.MountAccess;
import me.ppgome.critterGuard.database.SavedMount;
import me.ppgome.critterGuard.database.SavedPet;
import org.bukkit.OfflinePlayer;
import org.bukkit.scheduler.BukkitScheduler;

import java.util.*;

/**
 * This class handles the storage of critters in-memory and provides the necessary
 * methods to accomplish this.
 */
public class CritterCache {

    /**
     * The instance of the CritterGuard plugin.
     */
    CritterGuard plugin;

    /**
     * An in-memory cache of SavedMount objects.
     * This cache is used to store all SavedMounts for quick retrieval.
     */
    private HashMap<UUID, SavedMount> savedMountsCache = new HashMap<>();

    /**
     * An in-memory cache of SavedPet UUIDs.
     * This cache is used to store all SavedPet UUIDs for quick checking.
     */
    private Set<UUID> savedPetsCache = new HashSet<>();

    /**s
     * An in-memory cache of PlayerMeta objects.
     * This cache is used to store all PlayerMetas for quick retrieval.
     */
    private HashMap<UUID, PlayerMeta> playerMetaCache = new HashMap<>();

    private HashMap<UUID, BukkitScheduler> clickCache = new HashMap<>();

    private HashMap<UUID, CommandAction> actionCache = new HashMap<>();

    // TODO: REMOVE BELOW!

    /**
     * An in-memory cache of awaiting clicks for access requests.
     * This cache is used to store player UUIDs that are waiting for access clicks.
     */
    private HashMap<UUID, MountAccess> accessClickCache = new HashMap<>();

    /**
     * An in-memory cache of awaiting clicks for tame requests.
     * This cache is used to store player UUIDs that are waiting for tame clicks.
     */
    private HashMap<UUID, OfflinePlayer> tameClickCache = new HashMap<>();

    /**
     * An in-memory cache of awaiting clicks for untame requests.
     * This cache is used to store player UUIDs that are waiting for untame clicks.
     */
    private Set<UUID> untameClickCache = new HashSet<>();

    /**
     * An in-memory cache of awaiting clicks for info requests.
     * This cache is used to store player UUIDs that are waiting for info clicks.
     */
    private Set<UUID> infoClickCache = new HashSet<>();

    //------------------------------------------------------------------------------------------------------------------

    /**
     * Initializes the plugin's cache.
     * @param plugin The instance of the plugin
     */
    public CritterCache(CritterGuard plugin) {
        this.plugin = plugin;
    }

    //------------------------------------------------------------------------------------------------------------------

    // -------- Saved Mount Cache --------------------------------------------------------------------------------------

    /**
     * Adds a new saved mount to the cache.
     *
     * @param savedMount The mount being added to the cache
     */
    public void addSavedMount(SavedMount savedMount) {
        savedMountsCache.put(savedMount.getEntityUuid(), savedMount);
    }

    /**
     * Fetches the saved mount whom the specified UUID belongs to.
     *
     * @param mountUuid The UUID of the mount
     * @return The mount
     */
    public SavedMount getSavedMount(UUID mountUuid) {
        return savedMountsCache.get(mountUuid);
    }

    /**
     * Removes a saved mount from the cache.
     *
     * @param savedMount The saved mount to be removed
     */
    public void removeSavedMount(SavedMount savedMount) {
        savedMountsCache.remove(savedMount.getEntityUuid());
    }

    // -------- Saved Pet Cache ----------------------------------------------------------------------------------------

    /**
     * Adds a new saved pet's UUID to the cache.
     *
     * @param savedPet The pet's UUID being added to the cache
     */
    public void addSavedPet(SavedPet savedPet) {
        savedPetsCache.add(savedPet.getEntityUuid());
    }

    /**
     * Checks if an entity's UUID exists in the saved pet cache.
     *
     * @param uuid the UUID being checked
     * @return true if it does, false if not
     */
    public boolean isSavedPet(UUID uuid) {
        return savedPetsCache.contains(uuid);
    }

    /**
     * Removes a saved pet's UUID from the cache.
     *
     * @param savedPet The saved pet's UUID to be removed
     */
    public void removeSavedPet(SavedPet savedPet) {
        savedPetsCache.remove(savedPet.getEntityUuid());
    }

    // -------- Player Meta Cache --------------------------------------------------------------------------------------

    /**
     * Adds a player's playermeta to the cache.
     *
     * @param playerMeta The player's playermeta
     */
    public void addPlayerMeta(PlayerMeta playerMeta) {
        playerMetaCache.put(playerMeta.getUuid(), playerMeta);
    }

    /**
     * Fetches the player's playermeta whom the specified UUID belongs to.
     *
     * @param playerUuid The UUID of the player whose playermeta is being fetched
     * @return The playermeta
     */
    public PlayerMeta getPlayerMeta(UUID playerUuid) {
        return playerMetaCache.get(playerUuid);
    }

    /**
     * Removes a player's playermeta from the cache.
     *
     * @param playerUuid The UUID of the player whose playermeta is being removed
     */
    public void removePlayerMeta(UUID playerUuid) {
        playerMetaCache.remove(playerUuid);
    }

    // -------- Click Cache --------------------------------------------------------------------------------------------

    /**
     * Adds a player who is going to be clicking an entity to grant access to another player.
     *
     * @param playerUuid The UUID of the player who will be clicking
     * @param scheduler The scheduler instance representing the timeout of a click action
     */
    public void addAwaitingClick(UUID playerUuid, BukkitScheduler scheduler) {
        clickCache.put(playerUuid, scheduler);
    }

    /**
     * Checks if there's a record of the specified player who is in the process of granting access to another player.
     *
     * @param playerUuid The UUID of the player being checked for
     * @return True if they are in the cache, false if not.
     */
    public boolean isAwaitingClick(UUID playerUuid) {
        return clickCache.containsKey(playerUuid);
    }

    /**
     * Returns the value in the map associated to the UUID of the player as specified.
     *
     * @param playerUuid The UUID of the player
     * @return The value in the map
     */
    public BukkitScheduler getAwaitingClick(UUID playerUuid) {
        BukkitScheduler scheduler = clickCache.get(playerUuid);
        removeAwaitingClick(playerUuid);
        return scheduler;
    }

    /**
     * Removes a record from the cache that matches the specified UUID.
     *
     * @param playerUuid the UUID of the player whose record is being removed from the cache
     */
    public void removeAwaitingClick(UUID playerUuid) {
        clickCache.remove(playerUuid);
    }

    // -------- Action Cache -------------------------------------------------------------------------------------------

    /**
     * Adds a player who is going to be clicking an entity to grant access to another player.
     *
     * @param playerUuid The UUID of the player who will be clicking
     * @param commandAction The scheduler instance representing the timeout of a click action
     */
    public void addAwaitingAction(UUID playerUuid, CommandAction commandAction) {
        actionCache.put(playerUuid, commandAction);
    }

    /**
     * Checks if there's a record of the specified player who is in the process of granting access to another player.
     *
     * @param playerUuid The UUID of the player being checked for
     * @return True if they are in the cache, false if not.
     */
    public boolean isAwaitingAction(UUID playerUuid) {
        return actionCache.containsKey(playerUuid);
    }

    /**
     * Returns the value in the map associated to the UUID of the player as specified.
     *
     * @param playerUuid The UUID of the player
     * @return The value in the map
     */
    public CommandAction getAwaitingAction(UUID playerUuid) {
        CommandAction commandAction = actionCache.get(playerUuid);
        removeAwaitingClick(playerUuid);
        return commandAction;
    }

    /**
     * Removes a record from the cache that matches the specified UUID.
     *
     * @param playerUuid the UUID of the player whose record is being removed from the cache
     */
    public void removeAwaitingAction(UUID playerUuid) {
        actionCache.remove(playerUuid);
    }

}
