package me.ppgome.critterGuard.utility;

import me.ppgome.critterGuard.CGConfig;
import me.ppgome.critterGuard.CritterCache;
import me.ppgome.critterGuard.CritterGuard;
import me.ppgome.critterGuard.PlayerMeta;
import me.ppgome.critterGuard.database.*;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.*;
import org.bukkit.inventory.AbstractHorseInventory;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

import static me.ppgome.critterGuard.utility.CritterUtils.isMountableEntity;
import static me.ppgome.critterGuard.utility.CritterUtils.isPetEntity;

/**
 * This class provides methods for taming and registering mounts and pets with the plugin.
 */
public class CritterTamingHandler {

    /**
     * The instance of the plugin.
     */
    private final CritterGuard plugin;
    /**
     * The instance of the configuration class.
     */
    private final CGConfig config;
    /**
     * The instance of the SavedMountTable used for interacting with the database.
     */
    private final SavedMountTable savedMountTable;
    /**
     * The instance of the mountAccessTable used for interacting with the database.
     */
    private final MountAccessTable mountAccessTable;
    /**
     * The instance of the SavedPetTable used for interacting with the database.
     */
    private final SavedPetTable savedPetTable;
    /**
     * The instance of the CritterCache for interacting with the data stored in-memory.
     */
    private final CritterCache critterCache;

    //------------------------------------------------------------------------------------------------------------------

    /**
     * Initializes the methods needed for taming critters with the plugin.
     *
     * @param plugin The instance of the plugin.
     */
    public CritterTamingHandler(CritterGuard plugin) {
        this.plugin = plugin;
        this.config = plugin.getCGConfig();
        this.savedMountTable = plugin.getSavedMountTable();
        this.mountAccessTable = plugin.getMountAccessTable();
        this.savedPetTable = plugin.getSavedPetTable();
        this.critterCache = plugin.getCritterCache();
    }

    //------------------------------------------------------------------------------------------------------------------

    /**
     * Gathers the base necessary information for any kind of taming then sends it off to other methods
     * depending on what group the entity is a member of.
     *
     * @param player The player taming the entity
     * @param entity The entity being tamed
     */
    public void handleTaming(OfflinePlayer player, Entity entity) {

        System.out.println("Taming 2");

        String entityId = entity.getUniqueId().toString();
        String customName = entity.customName() != null ? entity.customName().toString() : null;
        String tamerId = player.getUniqueId().toString();
        String tamerName = player.getName();
        String entityType = entity.getType().toString();

        if (isMountableEntity(entity)) {
            handleMountTaming(entity, entityId, customName, tamerId, tamerName, entityType, player);
        } else if (isPetEntity(entity)) {
            handlePetTaming(entity, entityId, customName, tamerId, tamerName, entityType, player);
        }
    }

    /**
     * Handles the taming of mounts.
     *
     * @param entity The entity being tamed
     * @param entityId The UUID of the entity being tamed
     * @param customName The custom name (applied by nametag) of the entity being tamed
     * @param tamerId The UUID of the player taming the entity
     * @param tamerName The name of the player taming the entity
     * @param entityType The entity's type
     * @param player The player taming the entity
     */
    private void handleMountTaming(Entity entity, String entityId, String customName,
                                   String tamerId, String tamerName, String entityType, OfflinePlayer player) {
        SavedMount newMount;

        switch (entity) {
            case Horse horse:
                newMount = new SavedMount(entityId, customName, tamerId, tamerName,
                        entityType, horse.getColor().toString(), horse.getStyle().toString());
                newMount.setLastLocation(entity.getLocation());
                break;

            case Llama llama:
                newMount = new SavedMount(entityId, customName, tamerId, tamerName,
                        entityType, llama.getColor().toString());
                newMount.setLastLocation(entity.getLocation());
                break;

            default:
                newMount = new SavedMount(entityId, customName, tamerId, tamerName, entityType);
                newMount.setLastLocation(entity.getLocation());
        }

        registerNewSavedAnimal(newMount);
        if(entity instanceof Tameable tameable) tameable.setOwner(player);

        if(player.isOnline()) {
            Bukkit.getPlayer(player.getUniqueId()).sendMessage(config.TAMING_TO_THEMSELVES);
        }
    }

    /**
     * Handles the taming of normal pets.
     *
     * @param entity The entity being tamed
     * @param entityId The UUID of the entity being tamed
     * @param customName The custom name (applied by nametag) of the entity being tamed
     * @param tamerId The UUID of the player taming the entity
     * @param tamerName The name of the player taming the entity
     * @param entityType The entity's type
     * @param player The player taming the entity
     */
    private void handlePetTaming(Entity entity, String entityId, String customName,
                                 String tamerId, String tamerName, String entityType, OfflinePlayer player) {
        SavedPet savedPet;

        System.out.println("Taming 3");

        switch (entity) {
            case Wolf wolf:
                savedPet = new SavedPet(entityId, customName, tamerId, tamerName, entityType,
                        wolf.getVariant().getKey().getKey(), wolf.getSoundVariant().getKey().getKey());
                completePetRegistration(savedPet, entity);
                break;
            case Cat cat:
                System.out.println("Taming 4");
                savedPet = new SavedPet(entityId, customName, tamerId, tamerName,
                        entityType, cat.getCatType().getKey().getKey());
                completePetRegistration(savedPet, entity);
                break;
            case Parrot parrot:
                savedPet = new SavedPet(entityId, customName, tamerId, tamerName,
                        entityType, parrot.getVariant().toString());
                completePetRegistration(savedPet, entity);
                break;
            default:
                return; // Unsupported pet type
        }
        registerNewSavedAnimal(savedPet);
        ((Tameable) entity).setOwner(player);
        if(player.isOnline()) {
            System.out.println("Taming 5");
            Bukkit.getPlayer(player.getUniqueId()).sendMessage(config.TAMING_TO_THEMSELVES);
        }
    }

    public void completePetRegistration(SavedPet savedPet, Entity entity) {
        System.out.println("Taming 6");
        savedPet.setLastLocation(entity.getLocation());
        critterCache.addSavedPet(savedPet);
    }

    /**
     * Adds a new SavedMount to the in-memory cache and persists it to the database.
     * @param savedAnimal the SavedAnimal to register
     */
    public void registerNewSavedAnimal(SavedAnimal savedAnimal) {
        UUID playerUuid = savedAnimal.getEntityOwnerUuid();
        plugin.registerNewPlayer(playerUuid);
        PlayerMeta playerMeta = critterCache.getPlayerMeta(playerUuid);
        savedAnimal.setIndex(playerMeta.getOwnedList().size() + 1);
        playerMeta.addOwnedAnimal(savedAnimal);
        if(savedAnimal instanceof SavedMount savedMount) {
            critterCache.addSavedMount(savedMount);
            savedMountTable.save(savedMount);
        } else {
            savedPetTable.save((SavedPet) savedAnimal);
        }
    }

    /**
     * Removes a SavedMount from the in-memory cache and the database.
     * @param savedAnimal the SavedAnimal to remove from the cache
     */
    public void unregisterSavedMount(SavedAnimal savedAnimal) {
        System.out.println("We get here 2.1");
        critterCache.getPlayerMeta(savedAnimal.getEntityOwnerUuid()).removeOwnedAnimal(savedAnimal);
        System.out.println("We get here 2.2");
        if(savedAnimal instanceof SavedMount savedMount) {
            critterCache.removeSavedMount(savedMount);
            savedMountTable.delete(savedMount);
            for(MountAccess mountAccess : savedMount.getAccessList().values()) {
                mountAccessTable.delete(mountAccess);
            }
        } else {
            System.out.println("We get here 2.3");
            SavedPet savedPet = (SavedPet) savedAnimal;
            critterCache.removeSavedPet(savedPet);
            savedPetTable.delete(savedPet);
        }
    }

    /**
     * Processes the death of an animal by removing its saved data from the cache and database.
     * This method checks if the entity is a saved mount or pet and removes it accordingly.
     * @param entityUuid the UUID of the entity that died
     */
    public void processAnimalDeath(UUID entityUuid) {
        SavedMount savedMount = critterCache.getSavedMount(entityUuid);
        if(savedMount != null) {
            unregisterSavedMount(savedMount);
            plugin.logInfo("Removed saved mount " + savedMount.getEntityUuid() + " due to death.");
        } else {
            savedPetTable.getSavedPet(entityUuid.toString()).thenAccept(savedPet -> {
                if(savedPet != null) {
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        PlayerMeta playerMeta = critterCache.getPlayerMeta(savedPet.getEntityOwnerUuid());
                        if(playerMeta != null) {
                            SavedAnimal realAnimal = playerMeta.getOwnedAnimalByUuid(entityUuid);
                            if(realAnimal != null) {
                                playerMeta.removeOwnedAnimal(realAnimal);
                            }
                        }
                        savedPetTable.delete(savedPet);
                        plugin.logInfo("Removed saved pet " + savedPet.getEntityUuid() + " due to death.");
                    });
                }
            });
        }
    }

    /**
     * Handles the untaming of critters that are registered with the plugin.
     *
     * @param playerUuid The UUID of the player who is untaming the entity
     * @param player The player who is untaming the entity
     * @param savedMount The SavedMount instance of the entity. May be null if the entity is a pet instead of a mount
     * @param entityUuid The UUID of the entity being untamed
     * @param entity The entity being untamed
     */
    public void untame(UUID playerUuid, Player player, SavedMount savedMount, UUID entityUuid, Entity entity) {
        boolean canUntameOwn = player.hasPermission("critterguard.untame.own");
        boolean canUntameOthers = player.hasPermission("critterguard.untame.others");
        // is it a mount?
        if(savedMount != null) {
            if((canUntameOwn && savedMount.isOwner(playerUuid)) || canUntameOthers) {
                if(entity instanceof Tameable tameable) tameable.setTamed(false);
                removeSaddle(entity);
                unregisterSavedMount(savedMount);
                player.sendMessage(config.UNTAME);

            } else player.sendMessage(config.TAMED_NOT_YOURS);
        }
        // Is it a pet?
        else {
            PlayerMeta playerMeta = critterCache.getPlayerMeta(playerUuid);
            if (playerMeta != null) {
                SavedAnimal savedPet = playerMeta.getOwnedAnimalByUuid(entityUuid);
                if (savedPet instanceof SavedPet) {
                    if ((canUntameOwn && savedPet.isOwner(playerUuid)) || canUntameOthers) {
                        if (entity instanceof Tameable tameable) tameable.setTamed(false);
                        unregisterSavedMount(savedPet);
                        player.sendMessage(config.UNTAME);
                    } else {
                        player.sendMessage(config.TAMED_NOT_YOURS);
                    }
                } else {
                    player.sendMessage(config.NOT_TAMED);
                }
            } else {
                player.sendMessage(config.NOT_TAMED);
            }
        }
    }

    /**
     * Removes the saddle of a mount on untame so that it can be tamed again.
     * Horses with saddles, while untamed, can't be retamed through vanilla methods.
     *
     * @param entity the entity being untamed and having its saddle taken away
     */
    private void removeSaddle(Entity entity) {
        if(entity instanceof AbstractHorse abstractHorse) {
            AbstractHorseInventory inventory = abstractHorse.getInventory();
            ItemStack saddle = inventory.getSaddle();
            if(saddle != null) {
                inventory.setSaddle(new ItemStack(Material.AIR));
                Location location = entity.getLocation();
                location.getWorld().dropItem(location.add(0.0, 2.0, 0.0), saddle);
            }
        }
    }

}
