package me.ppgome.critterGuard;

import io.papermc.paper.event.player.PlayerNameEntityEvent;
import me.ppgome.critterGuard.commands.actions.*;
import me.ppgome.critterGuard.database.*;
import me.ppgome.critterGuard.disguisesaddles.DisguiseSaddleHandler;
import me.ppgome.critterGuard.disguisesaddles.LibsDisguiseProvider;
import me.ppgome.critterGuard.utility.CritterAccessHandler;
import me.ppgome.critterGuard.utility.CritterTamingHandler;
import me.ppgome.critterGuard.utility.MountSeatHandler;
import me.ppgome.critterGuard.utility.PlaceholderParser;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.event.world.EntitiesUnloadEvent;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.List;
import java.util.UUID;

/**
 * Handles events related to critter management.
 * This class will contain methods to handle various events such as mount spawning,
 * player interactions with mounts, and any other relevant events.
 */
public class CGEventHandler implements Listener {

    /**
     * The instance of the plugin.
     */
    private CritterGuard plugin;
    /**
     * The instance of the configuration class.
     */
    private CGConfig config;
    /**
     * The instance of the SavedMountTable for interacting with the database.
     */
    private SavedMountTable savedMountTable;
    /**
     * The instance of the SavedPetTable for interacting with the database.
     */
    private SavedPetTable savedPetTable;
    /**
     * The instance of the CritterCache for interacting with the data stored in-memory.
     */
    private CritterCache critterCache;
    /**
     * The instance of the CritterTamingHandler for handling all taming-related tasks.
     */
    private CritterTamingHandler tamingHandler;
    /**
     * The instance of the CritterAccessHandler for handling all access-related tasks.
     */
    private CritterAccessHandler accessHandler;
    /**
     * The instance of the DisguiseSaddleHandler for the logic behind disguised saddles.
     */
    private DisguiseSaddleHandler disguiseSaddleHandler;
    /**
     * The instance of the LibsDisguiseProvider for interacting directly with the LibsDisguises API.
     */
    private LibsDisguiseProvider disguiseProvider;
    /**
     * The instance of the MountSeatHandler for swapping player seats on multi-seat mounts.
     */
    private MountSeatHandler mountSeatHandler;

    //------------------------------------------------------------------------------------------------------------------

    public CGEventHandler(CritterGuard plugin) {
        this.plugin = plugin;
        this.config = plugin.getCGConfig();
        this.savedMountTable = plugin.getSavedMountTable();
        this.savedPetTable = plugin.getSavedPetTable();
        this.critterCache = plugin.getCritterCache();
        this.tamingHandler = new CritterTamingHandler(plugin);
        this.accessHandler = new CritterAccessHandler(plugin);
        this.disguiseSaddleHandler = plugin.getDisguiseSaddleHandler();
        this.disguiseProvider = plugin.getDisguiseProvider();
        this.mountSeatHandler = new MountSeatHandler(plugin);
    }

    //------------------------------------------------------------------------------------------------------------------

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        UUID playerUuid = event.getPlayer().getUniqueId();
        PlayerMeta playerMeta = critterCache.getPlayerMeta(playerUuid);
        if(playerMeta == null) {
            critterCache.addPlayerMeta(new PlayerMeta(playerUuid, plugin));
        }
        if(disguiseSaddleHandler != null) {
            disguiseSaddleHandler.refreshSaddleDisguises();
        }
    }

    @EventHandler
    public void onCritterTame(EntityTameEvent event) {
        if(event.getOwner() instanceof OfflinePlayer player) {
            tamingHandler.handleTaming(player, event.getEntity());
        }
    }

    @EventHandler
    public void onCritterDeath(EntityDeathEvent event) {
        Entity entity = event.getEntity();
        if(tamingHandler.isMountableEntity(entity)) {
            SavedMount savedMount = critterCache.getSavedMount(entity.getUniqueId());
            if(savedMount != null && entity.getPassengers().getFirst() instanceof Player player) {
                UUID playerUuid = player.getUniqueId();
                if(savedMount.hasFullAccess(playerUuid) && !savedMount.isOwner(playerUuid)) {
                    notifyPlayer(player, savedMount, config.NOTIFICATION_DIED);
                    plugin.logInfo(player.getName() + " was riding " +
                            savedMount.getEntityOwnerUuid() + "'s mount when it died: " +
                            savedMount.getEntityUuid());
                }
            }
        }
        if(!tamingHandler.canHandleTaming(entity)) return; // Only handle tameable entities
        tamingHandler.processAnimalDeath(entity.getUniqueId());
    }

    @EventHandler
    public void onCritterInteract(PlayerInteractEntityEvent event) {
        Entity entity = event.getRightClicked();

        if (!tamingHandler.canHandleTaming(entity)) return;

        Player player = event.getPlayer();
        UUID playerUuid = player.getUniqueId();
        UUID entityUuid = entity.getUniqueId();
        SavedMount savedMount = critterCache.getSavedMount(entityUuid);

        if(critterCache.isAwaitingClick(playerUuid) && critterCache.isAwaitingAction(playerUuid)) {
            CommandAction commandAction = critterCache.getAwaitingAction(playerUuid);
            switch(commandAction) {
                case AccessAction accessAction:
                    break;
                case InfoAction infoAction:
                    break;
                case TameAction tameAction:
                    break;
                case UntameAction untameAction:
                    break;
                default:
                    throw new IllegalStateException("Unexpected value: " + commandAction);
            }
        }

        if(critterCache.isAwaitingInfo(playerUuid)) {
            interactInfo(entity, player);
            event.setCancelled(true);
            return;
        }

        if (savedMount == null || savedMount.isOwner(playerUuid)) {
            // Not a saved mount, player is the owner, pending access request
            if(critterCache.isAwaitingAccess(playerUuid)) {
                interactAccess(playerUuid, player, savedMount, entityUuid, entity);
                event.setCancelled(true);
            }
            // Not a saved mount, player is the owner, pending tame request
            else if(critterCache.isAwaitingTame(playerUuid)) {
                interactTame(playerUuid, player, entityUuid, entity);
                event.setCancelled(true);
            } else if(critterCache.isAwaitingUntame(playerUuid)) {
                tamingHandler.untame(playerUuid, player, savedMount, entityUuid, entity);
                clickDing(player, entity);
                event.setCancelled(true);
            }
        } else {
            // Player has mount access. Allow passthrough
            if(savedMount.hasAccess(playerUuid)) return;
            // Saved mount, player is not the owner
            else if (!savedMount.hasAccess(playerUuid)) {
                if(config.CAN_BREED_LOCKED_ANIMALS && entity instanceof Animals animal && animal.isAdult()
                        && animal.isBreedItem(player.getActiveItem())) {
                    return; // Allow breeding
                }
                // Player does not have access, prevent interaction
                event.setCancelled(true);
                // Let the player know who the owner is. Get name asynchronously as it's thread-blocking
                accessHandler.getOwnerName(savedMount.getEntityOwnerUuid()).thenAccept(ownerName -> {
                    Bukkit.getScheduler().runTask(plugin, () -> player.sendMessage(PlaceholderParser
                            .of(config.PERMISSION_INTERACT)
                            .player(ownerName)
                            .parse()));
                });
            }
            // Player clicking is trying to untame the entity
            if(critterCache.isAwaitingUntame(playerUuid)) {
                tamingHandler.untame(playerUuid, player, savedMount, entityUuid, entity);
            }
            event.setCancelled(true);
        }
    }

    /**
     * Performs the ownership checks on the entity that's being queried by "/cg info".
     *
     * @param entity the entity being checked
     * @param player the player checking the entity
     */
    private void interactInfo(Entity entity, Player player) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String ownerName;
            if(entity instanceof Tameable tameable && tameable.isTamed()) {
                ownerName = tameable.getOwner().getName();
                sendInfo(entity, player, ownerName);
            } else {
                SavedMount mount = critterCache.getSavedMount(entity.getUniqueId());
                if(mount != null) {
                    ownerName = Bukkit.getOfflinePlayer(mount.getEntityOwnerUuid()).getName();
                    sendInfo(entity, player, ownerName);
                } else {
                    sendInfo(entity, player, "Nobody");
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
    private void sendInfo(Entity entity, Player player, String ownerName) {
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
                                NamedTextColor.YELLOW)).appendNewline();

                if(entity instanceof Llama llama) {
                    message = message.appendNewline().append(Component.text("Strength: ",
                                    NamedTextColor.LIGHT_PURPLE))
                            .append(Component.text(llama.getStrength(),
                                    NamedTextColor.YELLOW));
                }
            }
            player.sendMessage(message);
            critterCache.removeAwaitingInfo(player.getUniqueId());
            clickDing(player, entity);
        });
    }

    /**
     * Handles the logic required for players who are clicking on the mount to modify the access of another player.
     *
     * @param playerUuid The UUID of the player who is clicking the mount
     * @param player The player who is clicking the mount
     * @param savedMount The SavedMount instance of the mount that is being clicked
     * @param entityUuid The UUID of the mount that is being clicked
     * @param entity The mount that is being clicked
     */
    private void interactAccess(UUID playerUuid, Player player, SavedMount savedMount, UUID entityUuid, Entity entity) {
        MountAccess mountAccess = critterCache.getAwaitingAccess(playerUuid);
        UUID beingAddedUuid = UUID.fromString(mountAccess.getPlayerUuid());

        if (mountAccess.isFullAccess()) {
            accessHandler.handleFullAccess(player, savedMount, mountAccess, beingAddedUuid, entityUuid);
        } else {
            accessHandler.handlePassengerAccess(player, entity, savedMount, mountAccess, beingAddedUuid, entityUuid);
        }
        clickDing(player, entity);
    }

    /**
     * Handles the logic required for players who are clicking on the mount to tame it to another player.
     *
     * @param playerUuid The UUID of the player who is clicking the mount
     * @param player The player who is clicking the mount
     * @param entityUuid The UUID of the mount that is being clicked
     * @param entity The mount that is being clicked
     */
    private void interactTame(UUID playerUuid, Player player, UUID entityUuid, Entity entity) {
        savedPetTable.getSavedPet(entityUuid.toString()).thenAccept(savedPet -> Bukkit.getScheduler().runTask(plugin, () -> {
            if(savedPet == null) {
                OfflinePlayer playerTaming = critterCache.getAwaitingTame(playerUuid);
                tamingHandler.handleTaming(playerTaming, entity);
                critterCache.removeAwaitingTame(playerUuid);
                player.sendMessage(PlaceholderParser
                        .of(config.TAMING_TO_OTHERS)
                        .player(playerTaming.getName())
                        .parse());
            }
        }));
        clickDing(player, entity);
    }

    private void clickDing(Player player, Entity entity) {
        player.playSound(entity, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.MASTER, 0.5f, 1);
    }

    @EventHandler
    public void onPlayerNameCritter(PlayerNameEntityEvent event) {
        UUID entityUuid = event.getEntity().getUniqueId(); // The entity being named
        Player player = event.getPlayer(); // The player who is naming the entity
        PlayerMeta playerMeta = critterCache.getPlayerMeta(player.getUniqueId());
        if(event.getName() == null) return; // No name provided

        if(playerMeta != null) {
            SavedAnimal savedAnimal = playerMeta.getOwnedAnimalByUuid(entityUuid);
            if(savedAnimal != null) {
                String newName = PlainTextComponentSerializer.plainText().serialize(event.getName());
                SavedMount savedMount = critterCache.getSavedMount(entityUuid);
                if(savedMount != null) {
                    savedMount.setEntityName(newName);
                    savedMountTable.save(savedMount);
                } else {
                    savedAnimal.setEntityName(newName);
                    savedPetTable.getSavedPet(entityUuid.toString()).thenAccept(savedPet -> {
                        if(savedPet != null) {
                            savedPet.setEntityName(newName);
                            savedPetTable.save(savedPet);
                        }
                    });
                }
            }
        }
    }

    @EventHandler
    public void onPlayerLeashCritter(PlayerLeashEntityEvent event) {
        Entity entity = event.getEntity();
        UUID entityUuid = entity.getUniqueId();
        UUID playerUuid = event.getPlayer().getUniqueId();
        SavedMount savedMount = critterCache.getSavedMount(entityUuid);

        if(savedMount != null) {
            if(savedMount.getEntityOwnerUuid().equals(playerUuid) || savedMount.hasFullAccess(playerUuid)) return;
            event.setCancelled(true);
            return;
        } else if(critterCache.isSavedPet(entityUuid)) {
            if(critterCache.getPlayerMeta(playerUuid).getOwnedAnimalByUuid(entityUuid) != null) return;
            event.setCancelled(true);
            return;
        }

        if(!(entity instanceof Llama)) return; // Only handle llamas
        Player player = event.getPlayer();
        tamingHandler.handleTaming(player, entity);
    }

    @EventHandler
    public void onCritterMount(EntityMountEvent event) {
        Entity passenger = event.getEntity();
        Entity mount = event.getMount();
        UUID mountUuid = mount.getUniqueId();

        if(mountSeatHandler.isPlayer(mount)) return; // For disguised saddle mounting
        if(!(passenger instanceof Player player)) return; // Only handle player mounts

        // Handle a saved mount
        SavedMount savedMount = critterCache.getSavedMount(mountUuid);
        if(savedMount != null) {
            boolean hasAccess = savedMount.hasAccess(passenger.getUniqueId());
            boolean isFullAccess = savedMount.hasFullAccess(passenger.getUniqueId());
            if(!mount.getPassengers().isEmpty()) {
                // Player has passenger access or is the owner, allow mounting
                if(hasAccess || savedMount.isOwner(passenger.getUniqueId())) {
                    if(!disguiseProvider.isDisguised(mount)) return;
                    event.setCancelled(true);

                    List<Entity> playerStack = mountSeatHandler.getPlayerStack(mount);
                    if(playerStack.size() >= mountSeatHandler.getMaxSeats(mount)) {
                        event.setCancelled(true);
                        return;
                    }
                    Entity stackLast = playerStack.getLast();
                    if(stackLast.getUniqueId().equals(player.getUniqueId())) return;
                    playerStack.getLast().addPassenger(player);
                }
                // Player has full access, allow mounting
            } else if(isFullAccess || savedMount.isOwner(passenger.getUniqueId())) {

                // Notify owner that another player is controlling their mount
                if(isFullAccess) {
                    // Check passenger list AFTER the event to see if the passenger of this event is now the controller
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        if(mount.getPassengers().getFirst().getUniqueId().equals(passenger.getUniqueId())) {
                            notifyPlayer(player, savedMount, config.NOTIFICATION_MOUNTED);
                            plugin.logInfo(player.getName() + " started riding " +
                                    savedMount.getEntityOwnerUuid() + "'s mount: " + savedMount.getEntityUuid());
                        }
                    }, 5L);
                }
                String disguiseType = disguiseSaddleHandler.getDisguiseFromSaddle(mount);
                if(disguiseType != null) disguiseSaddleHandler.applySaddleDisguise(mount, player, disguiseType);
                return;
            }
            // Player does not have access, prevent mounting
            event.setCancelled(true);
            if(!disguiseProvider.isDisguised(mount)) {
                // Let the player know who the owner is. Get name asynchronously as it's thread-blocking
                accessHandler.getOwnerName(savedMount.getEntityOwnerUuid()).thenAccept(ownerName -> {
                    Bukkit.getScheduler().runTask(plugin, () -> player.sendMessage(PlaceholderParser
                            .of(config.PERMISSION_MOUNT)
                            .player(ownerName)
                            .parse()));
                });
            }
            return;
        }

        if (mountSeatHandler.isCamel(mount) || mountSeatHandler.isHappyGhast(mount) || mount instanceof Strider) {
            tamingHandler.handleTaming(player, mount);
        }

    }

    /**
     * Send a notification to the owner of the mount regarding other users riding them.
     * Used for mounting, dismounting, and deaths.
     *
     * @param player The player triggering the notification
     * @param savedMount The mount the notification is about
     * @param notificationType The type of notification
     */
    public void notifyPlayer(Player player, SavedMount savedMount, String notificationType) {
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

    @EventHandler
    public void onSneakToggle(PlayerToggleSneakEvent event) {
        if(!event.isSneaking()) return;
        Player player = event.getPlayer();
        Entity mount = player.getVehicle();

        if(mountSeatHandler.isPlayer(mount)) mountSeatHandler.teleportDown(mount, player);

        if((!(mountSeatHandler.isCamel(mount)) && !(mountSeatHandler.isHappyGhast(mount)))) return;

        if(disguiseProvider.isDisguised(mount)) {
            disguisedSeatSwap(player, mount);
        } else {
            normalSeatSwap(player, mount);
        }
    }

    /**
     * Handles seat swapping for non-disguised, multi-seated mobs.
     *
     * @param player The player dismounting from the mount
     * @param mount The mount being dismounted
     */
    private void normalSeatSwap(Player player, Entity mount) {
        List<Entity> passengers = mount.getPassengers();
        if(!mountSeatHandler.isDriver(player, passengers)) return;
        passengers.remove(player);

        SavedMount savedMount = critterCache.getSavedMount(mount.getUniqueId());
        if(savedMount == null) return;
        Entity newDriver = mountSeatHandler.findNewDriver(passengers, savedMount);

        if(newDriver != null) {
            mountSeatHandler.transferControl(mount, passengers, newDriver);
        } else if(!passengers.isEmpty()) {
            mountSeatHandler.dismountAllPassengers(mount, passengers);
        }
    }

    /**
     * Handles seat swapping for disguised, multi-seated mobs.
     *
     * @param player The player dismounting from the mount
     * @param mount The mount being dismounted
     */
    private void disguisedSeatSwap(Player player, Entity mount) {
        if(!mountSeatHandler.isDriverDisguised(player)) {
            mountSeatHandler.fixPlayerStack(mount, player);
            return;
        }
        SavedMount savedMount = critterCache.getSavedMount(mount.getUniqueId());
        if(savedMount == null) return;

        List<Entity> passengers = mountSeatHandler.getPlayerStack(mount);
        passengers.remove(player);
        if(mountSeatHandler.isHappyGhast(mount) && mount.getPassengers().size() > 1) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> mountSeatHandler.teleportDown(mount, player), 5L);
        }

        Entity newDriver = mountSeatHandler.findNewDriver(passengers, savedMount);
        if(newDriver != null) {
            mountSeatHandler.transferControlDisguised(mount, passengers, newDriver);
        } else if(!passengers.isEmpty()) {
            mountSeatHandler.disbandPlayerStack(mount);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        plugin.processPlayerLogout(event.getPlayer());
    }

    @EventHandler
    public void onCritterDamage(EntityDamageEvent event) {
        Entity entity = event.getEntity();
        if(tamingHandler.isMountableEntity(entity)) {
            Vehicle mount = (Vehicle) entity;
            if(critterCache.getSavedMount(entity.getUniqueId()) != null) {
                if(mount.getPassengers().isEmpty()) {
                    event.setCancelled(true);
                } else {
                    Entity causingEntity = event.getDamageSource().getCausingEntity();
                    if(causingEntity != null && causingEntity instanceof Player) {
                        event.setCancelled(true);
                    }
                }
            }

        } else if(tamingHandler.isPetEntity(entity)) {
            Tameable pet = (Tameable) entity;
            Entity damager = event.getDamageSource().getCausingEntity();
            UUID damagerUuid = null;
            if(damager != null) damagerUuid = damager.getUniqueId();

            if(damagerUuid != null) {
                if(pet.isTamed() && !damagerUuid.equals(pet.getOwnerUniqueId())) {
                    event.setCancelled(true);
                }
            } else {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onCritterDismount(EntityDismountEvent event) {
        Entity entity = event.getDismounted();
        Entity dismounting = event.getEntity();

        if (tamingHandler.isMountableEntity(entity)) {
            SavedMount savedMount = critterCache.getSavedMount(entity.getUniqueId());
            if (savedMount != null && dismounting instanceof Player player) {

                // Check to handle disguised happy ghast dismounts
                if(mountSeatHandler.isHappyGhast(entity) && disguiseProvider.isDisguised(entity)
                        && entity.getPassengers().size() > 1) {
                    mountSeatHandler.teleportDown(entity, player);
                }

                // Handles notifying the owner that someone is on their mount, if applicable
                UUID playerUUID = player.getUniqueId();
                if (savedMount.hasFullAccess(playerUUID) && !savedMount.isOwner(playerUUID) &&
                        entity.getPassengers().getFirst().getUniqueId().equals(playerUUID)) {
                    notifyPlayer(player, savedMount, config.NOTIFICATION_DISMOUNTED);
                    plugin.logInfo(player.getName() + " stopped riding " +
                            savedMount.getEntityOwnerUuid() + "'s mount: " + savedMount.getEntityUuid());
                }

                // If nobody else will be on this mount after this dismount, undisguise it
                if(entity.getPassengers().size() <= 1) {
                    disguiseProvider.removeDisguiseForAll(entity);
                }
            }
        }
    }

    @EventHandler
    public void onEntitiesUnload(EntitiesUnloadEvent event) {
        World world = event.getWorld();
        for(Entity entity : event.getEntities()) {
            if(tamingHandler.canHandleTaming(entity)) {
                UUID entityUuid = entity.getUniqueId();
                SavedMount savedMount = critterCache.getSavedMount(entityUuid);
                if(savedMount != null) {
                    Location location = entity.getLocation();
                    savedMount.setLastLocation(new Location(world, location.x(), location.y(), location.z()));
                    savedMountTable.save(savedMount);
                } else if(critterCache.isSavedPet(entityUuid) && entity instanceof Tameable tameable && tameable.isTamed()) {
                    for(SavedAnimal cachedAnimal : critterCache.getPlayerMeta(tameable.getOwnerUniqueId()).getOwnedList()) {
                        if (cachedAnimal.getEntityUuid().equals(entityUuid)) {
                            Location location = entity.getLocation();
                            cachedAnimal.setLastLocation(new Location(world, location.x(), location.y(), location.z()));
                            savedPetTable.save((SavedPet) cachedAnimal);
                        }
                    }
                }
            }
        }
    }
}
