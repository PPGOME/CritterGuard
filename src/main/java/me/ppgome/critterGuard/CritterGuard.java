package me.ppgome.critterGuard;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.jdbc.JdbcConnectionSource;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;
import me.ppgome.critterGuard.commands.CritterCommand;
import me.ppgome.critterGuard.database.*;
import me.ppgome.critterGuard.disguisesaddles.DisguiseSaddleHandler;
import me.ppgome.critterGuard.disguisesaddles.LibsDisguiseProvider;
import me.ppgome.critterGuard.utility.CritterAccessHandler;
import me.ppgome.critterGuard.utility.CritterTamingHandler;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * CritterGuard is a plugin for managing mount access permissions and pet protections in Minecraft.
 * It allows players to control who can access their mounts and provides a database
 * to store mount-related information.
 */
public final class CritterGuard extends JavaPlugin {

    /**
     * The configuration for the CritterGuard plugin.
     * This object holds various settings and options for the plugin.
     */
    CGConfig config;

    /**
     * The URL for the SQLite database used by the CritterGuard plugin.
     * This database stores information about mount access permissions and saved mounts.
     */
    private static final String DATABASE_URL = "jdbc:sqlite:plugins/CritterGuard/CritterGuard.db";

    /**
     * The connection source for the SQLite database.
     * This is used to establish connections to the database for performing CRUD operations.
     */
    private ConnectionSource connectionSource;

    /**
     * The Data Access Object (DAO) for managing MountAccess records in the database.
     * This DAO provides methods to perform CRUD operations on MountAccess records.
     */
    private Dao<MountAccess, Integer> mountAccessDao;

    /**
     * The Data Access Object (DAO) for managing SavedMount records in the database.
     * This DAO provides methods to perform CRUD operations on SavedMount records.
     */
    private Dao<SavedMount, String> savedMountDao;

    /**
     * The Data Access Object (DAO) for managing SavedPet records in the database.
     * This DAO provides methods to perform CRUD operations on SavedPet records.
     */
    private Dao<SavedPet, String> savedPetDao;

    /**
     * Table handler for MountAccess records.
     * This object provides higher-level methods for managing MountAccess records in the database.
     */
    private MountAccessTable mountAccessTable;

    /**
     * Table handler for SavedMount records.
     * This object provides higher-level methods for managing SavedMount records in the database.
     */
    private SavedMountTable savedMountTable;

    /**
     * Table handler for SavedPet records.
     * This object provides higher-level methods for managing SavedPet records in the database.
     */
    private SavedPetTable savedPetTable;

    /**
     * In-memory cache for storing critter and player metadata.
     * This cache is used to quickly access critter and player information without querying the database.
     */
    private CritterCache critterCache;

    /**
     * Command handler for the /critter command.
     * This object handles the execution of the critter command and its subcommands.
     */
    private CritterCommand critterCommand;

    /**
     * Provides methods for taming critters and checking if they can be tamed.
     */
    private CritterTamingHandler critterTamingHandler;

    /**
     * Provides methods for granting/revoking access to critters.
     */
    private CritterAccessHandler critterAccessHandler;

    /**
     * Provides methods for disguising and undisguising mounts when using the disguise saddles feature.
     */
    private LibsDisguiseProvider disguiseProvider;

    /**
     * Handles the checking required for the disguise saddles feature.
     */
    private DisguiseSaddleHandler disguiseSaddleHandler;

    //------------------------------------------------------------------------------------------------------------------

    @Override
    public void onEnable() {
        // Plugin startup logic
        config = new CGConfig(this);
        setupDatabase();
        loadDatabaseData();
        critterCache = new CritterCache(this);
        critterTamingHandler = new CritterTamingHandler(this);
        critterAccessHandler = new CritterAccessHandler(this);


        if(config.ENABLE_DISGUISE_SADDLES && getServer().getPluginManager().getPlugin("LibsDisguises") != null) {
            disguiseProvider = new LibsDisguiseProvider(this);
            disguiseSaddleHandler = new DisguiseSaddleHandler(this);
        }

        getServer().getPluginManager().registerEvents(new CGEventHandler(this), this);
        critterCommand = new CritterCommand(this);
        this.getCommand("critter").setExecutor(critterCommand);

    }

    @Override
    public void onDisable() {
        for(Player player : Bukkit.getOnlinePlayers()) {
            processPlayerLogout(player);
        }
    }

    //------------------------------------------------------------------------------------------------------------------

    /**
     * Sets up the SQLite database for the MountGuard plugin.
     * This method initializes the connection source, creates DAOs for MountAccess and SavedMount,
     * and creates the necessary tables if they do not already exist.
     */
    public void setupDatabase() {
        try {
            // Initialize the connection source
            connectionSource = new JdbcConnectionSource(DATABASE_URL);

            // Create DAOs for MountAccess and SavedMount
            mountAccessDao = DaoManager.createDao(connectionSource, MountAccess.class);
            savedMountDao = DaoManager.createDao(connectionSource, SavedMount.class);
            savedPetDao = DaoManager.createDao(connectionSource, SavedPet.class);

            // Initialize table handlers for MountAccess and SavedMount
            mountAccessTable = new MountAccessTable(this);
            savedMountTable = new SavedMountTable(this);
            savedPetTable = new SavedPetTable(this);

            // Create tables if they do not exist
            TableUtils.createTableIfNotExists(connectionSource, MountAccess.class);
            TableUtils.createTableIfNotExists(connectionSource, SavedMount.class);
            TableUtils.createTableIfNotExists(connectionSource, SavedPet.class);

        } catch (SQLException e) {
            logError("Failed to set up database" + e.getMessage());
        }
    }

    /**
     * Loads existing data from the database into the in-memory cache.
     * This method retrieves all SavedMount and MountAccess records from the database
     * and populates the CritterCache with this data for quick access during runtime.
     */
    public void loadDatabaseData() {

        CompletableFuture<List<SavedMount>> savedMountsFuture = savedMountTable.getAllSavedMounts();
        CompletableFuture<List<MountAccess>> mountAccessFuture = mountAccessTable.getAllMountAccess();
        CompletableFuture<List<SavedPet>> savedPetsFuture = savedPetTable.getAllSavedPets();

        CompletableFuture.allOf(savedMountsFuture, mountAccessFuture, savedPetsFuture).thenRun(() -> {

            PlayerMeta playerMeta;

            try {
                List<SavedMount> savedMounts = savedMountsFuture.get();

                // Initialize the in-memory cache for saved mounts
                if(savedMounts != null) {
                    for(SavedMount savedMount : savedMounts) {
                        critterCache.addSavedMount(savedMount);
                        playerMeta = registerNewPlayer(savedMount.getEntityOwnerUuid());
                        savedMount.setIndex(playerMeta.getOwnedList().size() + 1);
                        playerMeta.addOwnedAnimal(savedMount);
                    }
                    logInfo("Loaded " + savedMounts.size() + " saved mounts from the database.");
                }
            } catch(Exception e) {
                logError("Failed to load saved mount data: " + e.getMessage());
            }

            try {
                List<MountAccess> mountAccesses = mountAccessFuture.get();

                // Initialize the in-memory cache for mount accesses
                if(mountAccesses != null) {
                    for(MountAccess mountAccess : mountAccesses) {
                        registerNewPlayer(UUID.fromString(mountAccess.getPlayerUuid())).addMountAccess(mountAccess);
                        SavedMount savedMount = critterCache.getSavedMount(UUID.fromString(mountAccess.getMountUuid()));
                        if(savedMount != null) {
                            savedMount.addAccess(UUID.fromString(mountAccess.getPlayerUuid()), mountAccess);
                        } else {
                            mountAccessTable.delete(mountAccess);
                        }
                    }
                    logInfo("Loaded " + mountAccesses.size() + " saved mount accesses from the database.");
                }
            } catch(Exception e) {
                logError("Failed to load mount access data: " + e.getMessage());
            }

            try {
                List<SavedPet> savedPets = savedPetsFuture.get();

                // Initialize the in-memory cache for saved pets
                if(savedPets != null) {
                    for(SavedPet savedPet : savedPets) {
                        playerMeta = registerNewPlayer(savedPet.getEntityOwnerUuid());
                        savedPet.setIndex(playerMeta.getOwnedList().size() + 1);
                        playerMeta.addOwnedAnimal(savedPet);
                        critterCache.addSavedPet(savedPet);
                    }
                    logInfo("Loaded " + savedPets.size() + " saved pets from the database.");
                }
            } catch(Exception e) {
                logError("Failed to load saved pet data: " + e.getMessage());
            }
        });
    }

    /**
     * Registers a new player by adding their UUID to the in-memory cache.
     * If the player already exists in the cache, this method does nothing.
     * @param playerUuid the UUID of the player to register
     */
    public PlayerMeta registerNewPlayer(UUID playerUuid) {
        PlayerMeta playerMeta = critterCache.getPlayerMeta(playerUuid);
        if(playerMeta == null) {
            playerMeta = new PlayerMeta(playerUuid, this);
            critterCache.addPlayerMeta(playerMeta);
        }
        return playerMeta;
    }

    /**
     * Processes a player's logout event.
     * If the player is riding a mount that they do not own, they will be ejected from the mount.
     * @param player the player who is logging out
     */
    public void processPlayerLogout(Player player) {
        Entity vehicle = player.getVehicle();
        if(vehicle != null) {
            SavedMount savedMount = critterCache.getSavedMount(vehicle.getUniqueId());
            if(savedMount != null && !savedMount.isOwner(player.getUniqueId())) {
                vehicle.eject();
            }
        }
    }

    /**
     * Logs an informational message to the server console with a CritterGuard prefix.
     * @param message the message to log
     */
    public void logInfo(String message) {
        getComponentLogger().info(Component.text(message));
    }

    /**
     * Logs an error message to the server console with a CritterGuard prefix in red color, if supported by the console.
     * @param message the error message to log
     */
    public void logError(String message) {
        getComponentLogger().error(Component.text(message), NamedTextColor.RED);
    }

    //------------------------------------------------------------------------------------------------------------------

    /**
     * Returns the configuration object for the CritterGuard plugin.
     * This object contains various settings and options for the plugin.
     *
     * @return the configuration object
     */
    public CGConfig getCGConfig() {
        return config;
    }

    /**
     * Returns the root command for the CritterGuard plugin. This contains all the subcommands.
     *
     * @return the root command object
     */
    public CritterCommand getCritterCommand() {
        return critterCommand;
    }

    /**
     * Returns the Data Access Object (DAO) for managing MountAccess records.
     * This DAO provides methods to perform CRUD operations on MountAccess records.
     *
     * @return the DAO for MountAccess records
     */
    public Dao<MountAccess, Integer> getMountAccessDao() {
        return mountAccessDao;
    }

    /**
     * Returns the Data Access Object (DAO) for managing SavedMount records.
     *
     * @return the DAO for SavedMount records
     */
    public Dao<SavedMount, String> getSavedMountDao() {
        return savedMountDao;
    }

    /**
     * Returns the Data Access Object (DAO) for managing SavedPet records.
     *
     * @return the DAO for SavedPet records
     */
    public Dao<SavedPet, String> getSavedPetDao() {
        return savedPetDao;
    }

    /**
     * Returns the MountAccessTable instance
     * @return the MountAccessTable instance
     */
    public MountAccessTable getMountAccessTable() {
        return mountAccessTable;
    }

    /**
     * Returns the SavedMountTable instance
     * @return the SavedMountTable instance
     */
    public SavedMountTable getSavedMountTable() {
        return savedMountTable;
    }

    /**
     * Returns the SavedPetTable instance
     * @return the SavedPetTable instance
     */
    public SavedPetTable getSavedPetTable() {
        return savedPetTable;
    }

    /**
     * Returns the CritterCache instance.
     * @return the CritterCache instance
     */
    public CritterCache getCritterCache() {
        return critterCache;
    }

    /**
     * Returns the CritterTamingHandler instance.
     * @return the CritterTamingHandler instance.
     */
    public CritterTamingHandler getCritterTamingHandler() {
        return critterTamingHandler;
    }

    /**
     * Returns the CritterAccessHandler instance.
     * @return the CritterAccessHandler instance.
     */
    public CritterAccessHandler getCritterAccessHandler() {
        return critterAccessHandler;
    }

    /**
     * Returns the LibsDisguiseProvider instance
     * @return the LibsDisguiseProvider instance
     */
    public LibsDisguiseProvider getDisguiseProvider() {
        return disguiseProvider;
    }

    /**
     * Returns the DisguiseSaddleHandler instance
     * @return the DisguiseSaddleHandler instance
     */
    public DisguiseSaddleHandler getDisguiseSaddleHandler() {
        return disguiseSaddleHandler;
    }

}
