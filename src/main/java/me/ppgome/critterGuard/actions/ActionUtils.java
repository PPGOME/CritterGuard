package me.ppgome.critterGuard.actions;

import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

public class ActionUtils {

    public static void clickDing(Player player, Entity entity) {
        player.playSound(entity, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.MASTER, 0.5f, 1);
    }

    public static void clickFail(Player player, Entity entity) {
        player.playSound(entity, Sound.BLOCK_ANVIL_LAND, SoundCategory.MASTER, 0.5f, 1);
    }

}
