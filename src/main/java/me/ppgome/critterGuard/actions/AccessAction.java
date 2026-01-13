package me.ppgome.critterGuard.actions;

import me.ppgome.critterGuard.CGConfig;
import me.ppgome.critterGuard.CritterCache;
import me.ppgome.critterGuard.CritterGuard;
import me.ppgome.critterGuard.database.MountAccess;
import me.ppgome.critterGuard.database.SavedMount;
import me.ppgome.critterGuard.utility.CritterAccessHandler;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.UUID;

public class AccessAction implements ActionHandler {

    private Player player;
    private OfflinePlayer playerBeingAdded;
    private CGConfig config;
    private CritterCache cache;
    private CritterAccessHandler accessHandler;

    private boolean beingAdded;
    private boolean isFullAccess;

    //------------------------------------------------------------------------------------------------------------------

    public AccessAction(Player player, OfflinePlayer playerBeingAdded, boolean beingAdded, boolean isFullAccess,
                        CritterGuard plugin) {
        this.player = player;
        this.playerBeingAdded = playerBeingAdded;
        this.beingAdded = beingAdded;
        this.isFullAccess = isFullAccess;
        this.config = plugin.getCGConfig();
        this.cache = plugin.getCritterCache();
        this.accessHandler = plugin.getCritterAccessHandler();
    }

    //------------------------------------------------------------------------------------------------------------------

    @Override
    public void execute(Entity clickedEntity) {
        UUID entityUuid = clickedEntity.getUniqueId();
        UUID playerBeingAddedUuid = playerBeingAdded.getUniqueId();
        SavedMount savedMount = cache.getSavedMount(entityUuid);

        if(savedMount != null) {

            boolean hasAccess = savedMount.hasAccess(playerBeingAddedUuid);

            MountAccess mountAccess = new MountAccess(entityUuid.toString(), playerBeingAddedUuid.toString());

            if(!savedMount.isOwner(player.getUniqueId())) return;

            if(beingAdded) {
                if(!savedMount.isOwner(playerBeingAddedUuid)) addAccess(mountAccess, savedMount,
                        playerBeingAddedUuid, clickedEntity, entityUuid);
            } else if(hasAccess) {
                accessHandler.removeAccess(savedMount, mountAccess, playerBeingAddedUuid);
                accessHandler.sendRevocationMessage(playerBeingAddedUuid, player);
            }

            ActionUtils.clickDing(player, clickedEntity);

        } else {
            player.sendMessage(config.NOT_TAMED);
            ActionUtils.clickFail(player, clickedEntity);
        }
    }

    private void addAccess(MountAccess mountAccess, SavedMount savedMount, UUID playerBeingAddedUuid, Entity clickedEntity, UUID entityUuid) {
        MountAccess existingAccess = savedMount.getMountAccess(playerBeingAddedUuid);
        boolean isExisting = existingAccess != null;

        if(isFullAccess) {
            if(isExisting) {
                if (!existingAccess.isFullAccess()) {
                    accessHandler.removeAccess(savedMount, mountAccess, playerBeingAddedUuid);
                } else {
                    player.sendMessage(config.ALREADY_HAS_ACCESS);
                    return;
                }
            }
            mountAccess.setFullAccess(true);
            accessHandler.handleFullAccess(player, savedMount, mountAccess, beingAdded,
                    playerBeingAddedUuid, entityUuid);
        } else {
            if(isExisting) {
                if (existingAccess.isFullAccess()) {
                    accessHandler.removeAccess(savedMount, mountAccess, playerBeingAddedUuid);
                } else {
                    player.sendMessage(config.ALREADY_HAS_ACCESS);
                    return;
                }
            }
            mountAccess.setFullAccess(false);
            accessHandler.handlePassengerAccess(player, clickedEntity, savedMount, mountAccess, beingAdded,
                    playerBeingAddedUuid, entityUuid);
        }
    }

}
