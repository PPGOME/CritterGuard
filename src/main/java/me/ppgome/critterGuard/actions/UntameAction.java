package me.ppgome.critterGuard.actions;

import me.ppgome.critterGuard.CritterCache;
import me.ppgome.critterGuard.CritterGuard;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.UUID;

public class UntameAction implements ActionHandler {

    private Player player;
    private UUID playerUuid;
    private CritterGuard plugin;
    private CritterCache cache;

    //------------------------------------------------------------------------------------------------------------------

    public UntameAction(Player player, CritterGuard plugin) {
        this.player = player;
        this.playerUuid = player.getUniqueId();
        this.plugin = plugin;
        this.cache = plugin.getCritterCache();
    }

    //------------------------------------------------------------------------------------------------------------------

    @Override
    public void execute(Entity clickedEntity) {
        UUID entityUuid = clickedEntity.getUniqueId();
        plugin.getCritterTamingHandler().untame(playerUuid, player, cache.getSavedMount(entityUuid), entityUuid, clickedEntity);
        ActionUtils.clickDing(player, clickedEntity);
    }
}
