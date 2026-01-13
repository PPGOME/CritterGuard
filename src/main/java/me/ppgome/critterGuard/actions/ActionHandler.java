package me.ppgome.critterGuard.actions;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

public interface ActionHandler {

    /**
     * Executes an action.
     */
    void execute(Entity clickedEntity);

}
