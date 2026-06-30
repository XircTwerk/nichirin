package com.xirc.nichirin.common.util;

import com.xirc.nichirin.BreathOfNichirin;
import com.xirc.nichirin.common.data.MovesetHelper;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

public final class NichirinDamageSources {

    public static final ResourceKey<DamageType> BLADE = key("blade");
    public static final ResourceKey<DamageType> BREATHING = key("breathing");
    public static final ResourceKey<DamageType> CQC = key("cqc");
    public static final ResourceKey<DamageType> DEMON = key("demon");
    public static final ResourceKey<DamageType> SUNLIGHT = key("sunlight");
    public static final ResourceKey<DamageType> BLOOD_LOSS = key("blood_loss");
    public static final ResourceKey<DamageType> WISTERIA = key("wisteria");
    public static final ResourceKey<DamageType> THROWN_KATANA = key("thrown_katana");
    public static final ResourceKey<DamageType> EXPLOSION = key("explosion");
    public static final ResourceKey<DamageType> BOAR = key("boar");
    public static final ResourceKey<DamageType> TEMPLE_DEMON = key("temple_demon");
    public static final ResourceKey<DamageType> TRAINER = key("trainer");
    public static final ResourceKey<DamageType> PERCENTAGE = key("percentage");

    private NichirinDamageSources() {}

    public static DamageSource blade(LivingEntity attacker) {
        return source(attacker, BLADE, attacker);
    }

    public static DamageSource breathing(LivingEntity attacker) {
        Holder<DamageType> holder = holder(attacker, BREATHING);
        return new BreathingDamageSource(holder, attacker, breathingStyleName(attacker));
    }

    public static DamageSource cqc(LivingEntity attacker) {
        return source(attacker, CQC, attacker);
    }

    public static DamageSource demon(LivingEntity attacker) {
        return source(attacker, DEMON, attacker);
    }

    public static DamageSource sunlight(LivingEntity victim) {
        return source(victim, SUNLIGHT, null);
    }

    public static DamageSource bloodLoss(LivingEntity victim) {
        return source(victim, BLOOD_LOSS, null);
    }

    public static DamageSource wisteria(LivingEntity victim) {
        return source(victim, WISTERIA, null);
    }

    public static DamageSource thrownKatana(LivingEntity victim, Entity direct, @Nullable Entity owner) {
        Holder<DamageType> holder = holder(victim, THROWN_KATANA);
        return owner != null ? new DamageSource(holder, direct, owner) : new DamageSource(holder, direct);
    }

    public static DamageSource explosion(LivingEntity victim, @Nullable Entity direct, @Nullable Entity owner) {
        Holder<DamageType> holder = holder(victim, EXPLOSION);
        if (direct != null && owner != null) return new DamageSource(holder, direct, owner);
        if (direct != null) return new DamageSource(holder, direct);
        if (owner != null) return new DamageSource(holder, owner);
        return new DamageSource(holder);
    }

    public static DamageSource boar(LivingEntity boar) {
        return source(boar, BOAR, boar);
    }

    public static DamageSource templeDemon(LivingEntity demon) {
        return source(demon, TEMPLE_DEMON, demon);
    }

    public static DamageSource trainer(LivingEntity trainer) {
        return source(trainer, TRAINER, trainer);
    }

    public static DamageSource percentage(LivingEntity target, DamageSource original) {
        Holder<DamageType> holder = holder(target, PERCENTAGE);
        Entity direct = original.getDirectEntity();
        Entity attacker = original.getEntity();
        return new PercentageDamageSource(holder, direct, attacker, original);
    }

    /**
     * Type check that sees through the percentage wrapper. When {@code percentageDamage} is enabled
     * the original move source (CQC, blade, etc.) is wrapped in a {@link PercentageDamageSource} before
     * the entity is hurt, so a plain {@code source.is(CQC)} would miss it. Use this when reacting to a
     * kill/hit by move type (e.g. unlock conditions).
     */
    public static boolean is(DamageSource source, ResourceKey<DamageType> key) {
        if (source.is(key)) return true;
        return source instanceof PercentageDamageSource wrapped && wrapped.original.is(key);
    }

    private static DamageSource source(LivingEntity context, ResourceKey<DamageType> key, @Nullable Entity attacker) {
        Holder<DamageType> holder = holder(context, key);
        return attacker != null ? new DamageSource(holder, attacker) : new DamageSource(holder);
    }

    private static Holder<DamageType> holder(LivingEntity context, ResourceKey<DamageType> key) {
        return context.level().registryAccess()
                .registryOrThrow(Registries.DAMAGE_TYPE)
                .getHolderOrThrow(key);
    }

    private static ResourceKey<DamageType> key(String id) {
        return ResourceKey.create(Registries.DAMAGE_TYPE, BreathOfNichirin.id(id));
    }

    private static Component breathingStyleName(LivingEntity attacker) {
        if (attacker instanceof Player player) {
            String id = MovesetHelper.getBreathingMovesetId(player);
            if (id != null && !id.isBlank()) {
                return Component.translatable("breathing_style." + id);
            }
        }
        return Component.translatable("breathing_style.breathing");
    }

    private static final class BreathingDamageSource extends DamageSource {
        private final Component breathingStyle;

        private BreathingDamageSource(Holder<DamageType> type, Entity attacker, Component breathingStyle) {
            super(type, attacker);
            this.breathingStyle = breathingStyle;
        }

        @Override
        public Component getLocalizedDeathMessage(LivingEntity victim) {
            Entity attacker = getEntity();
            if (attacker == null) {
                return Component.translatable("death.attack." + getMsgId(), victim.getDisplayName(), breathingStyle);
            }

            String key = switch (Math.floorMod(victim.tickCount + attacker.getId(), 3)) {
                case 0 -> "death.attack.nichirin_breathing";
                case 1 -> "death.attack.nichirin_breathing_keepup";
                default -> "death.attack.nichirin_breathing_overwhelmed";
            };
            return Component.translatable(key, victim.getDisplayName(), attacker.getDisplayName(), breathingStyle);
        }
    }

    private static final class PercentageDamageSource extends DamageSource {
        private final DamageSource original;

        private PercentageDamageSource(Holder<DamageType> type, Entity direct, Entity attacker,
                                       DamageSource original) {
            super(type, direct, attacker);
            this.original = original;
        }

        @Override
        public Component getLocalizedDeathMessage(LivingEntity victim) {
            return original.getLocalizedDeathMessage(victim);
        }
    }
}
