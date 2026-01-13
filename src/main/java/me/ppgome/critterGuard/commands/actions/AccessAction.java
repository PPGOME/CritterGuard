package me.ppgome.critterGuard.commands.actions;

import me.ppgome.critterGuard.CritterCache;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.UUID;

public class AccessAction implements CommandAction {

    private CritterCache critterCache;
    private UUID playerUuid;
    private Player player;
    private UUID entityUuid;
    private Entity entity;
    private boolean isBeingAdded;
    private boolean isFullAccess;

    //------------------------------------------------------------------------------------------------------------------

    public AccessAction(CritterCache critterCache, Player player, UUID playerUuid,
                        boolean isBeingAdded, boolean isFullAccess) {
        this.critterCache = critterCache;
        this.player = player;
        this.playerUuid = playerUuid;
    }

    //------------------------------------------------------------------------------------------------------------------

    @Override
    public void execute() {
    }

    //------------------------------------------------------------------------------------------------------------------

    public void addEntity(UUID entityUuid, Entity entity) {
        this.entityUuid = entityUuid;
        this.entity = entity;
    }

}
