package com.xirc.nichirin.common.data;

import com.xirc.nichirin.common.attack.moveset.AbstractMoveset;
import com.xirc.nichirin.common.attack.moveset.CqcMoveset;

import java.util.Collection;
import java.util.Comparator;

/**
 * Read-only catalog view for CQC GUI and preset validation.
 * CqcMoveset owns the actual move data; this class must not duplicate combat stats.
 */
public final class CqcMoveCatalog {

    private CqcMoveCatalog() {}

    public static Collection<Definition> all() {
        return CqcMoveset.configurations().values().stream()
                .filter(config -> !"demon_slash_2".equals(config.getMoveId()))
                .map(CqcMoveCatalog::fromConfig)
                .sorted(Comparator.comparing(Definition::displayName))
                .toList();
    }

    public static Definition get(String id) {
        AbstractMoveset.MoveConfiguration config = CqcMoveset.configurationFor(normalize(id));
        return config != null && !"demon_slash_2".equals(config.getMoveId()) ? fromConfig(config) : null;
    }

    public static boolean contains(String id) {
        return get(id) != null;
    }

    public static String normalize(String id) {
        return id == null ? "" : id.trim().toLowerCase().replace(' ', '_');
    }

    private static Definition fromConfig(AbstractMoveset.MoveConfiguration config) {
        String id = normalize(config.getMoveId());
        return new Definition(
                id,
                config.getDisplayName(),
                CqcMoveset.animationNameFor(config),
                Math.max(1, config.getWindupOrDefault(0) + config.getDurationOrDefault(0)),
                config.getDamageOrDefault(0f),
                config.getRangeOrDefault(0f),
                config.getKnockbackOrDefault(0f),
                config.getHitStunOrDefault(0),
                config.getCooldownOrDefault(0),
                config.getDashSpeedOrDefault(0f),
                CqcMoveset.isDemonOnlyMove(id),
                CqcMoveset.isDestructiveDeathMove(id),
                config.getDescription()
        );
    }

    public record Definition(String id, String displayName, String animationName, int durationTicks,
                             float damage, float range, float knockback, int hitStun, int cooldown,
                             float dashDistance, boolean demonOnly, boolean destructiveDeath,
                             String description) {}
}
