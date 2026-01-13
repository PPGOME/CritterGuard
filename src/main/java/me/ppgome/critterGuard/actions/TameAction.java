package me.ppgome.critterGuard.actions;

import me.ppgome.critterGuard.CGConfig;
import me.ppgome.critterGuard.CritterCache;
import me.ppgome.critterGuard.CritterGuard;
import me.ppgome.critterGuard.database.SavedAnimal;
import me.ppgome.critterGuard.utility.PlaceholderParser;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Tameable;

import java.util.UUID;

public class TameAction implements ActionHandler {

    private Player player;
    private OfflinePlayer playerTaming;
    private CritterGuard plugin;
    private CGConfig config;
    private CritterCache cache;

    //------------------------------------------------------------------------------------------------------------------

    public TameAction(Player player, OfflinePlayer playerTaming, CritterGuard plugin) {
        this.player = player;
        this.playerTaming = playerTaming;
        this.plugin = plugin;
        this.config = plugin.getCGConfig();
        this.cache = plugin.getCritterCache();
    }

    //------------------------------------------------------------------------------------------------------------------

    @Override
    public void execute(Entity clickedEntity) {
        if(clickedEntity instanceof Tameable tameable && !tameable.isTamed()) {
            UUID entityUuid = clickedEntity.getUniqueId();

            if(cache.isSavedPet(entityUuid) || cache.getSavedMount(entityUuid) != null) return;

            plugin.getCritterTamingHandler().handleTaming(playerTaming, clickedEntity);
            ActionUtils.clickDing(player, clickedEntity);
            player.sendMessage(PlaceholderParser
                    .of(config.TAMING_TO_OTHERS)
                    .player(playerTaming.getName())
                    .parse());
        }

    }

}
