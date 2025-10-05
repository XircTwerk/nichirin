package com.xirc.nichirin.common.entity.npc;

import lombok.Getter;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.damagesource.DamageSource;
import org.jetbrains.annotations.NotNull;

/**
 * Complete Player wrapper that delegates all operations to the wrapped NPC entity.
 * This allows NPCs to use the existing Player-based attack system without modification.
 */
public class NPCPlayerWrapper extends Player {

    /**
     * -- GETTER --
     *  Get the wrapped NPC entity
     */
    @Getter
    private final LivingEntity wrappedNPC;
    private final Inventory dummyInventory;

    public NPCPlayerWrapper(LivingEntity npc) {
        // Use a proper name instead of "NPC_" + id
        super(npc.level(), npc.blockPosition(), 0.0f,
                new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "Temple Demon"));

        this.wrappedNPC = npc;
        this.dummyInventory = new Inventory(this);
        this.syncWithNPC();
    }

    // === POSITION AND MOVEMENT DELEGATION ===

    @Override
    public void absMoveTo(double x, double y, double z, float yRot, float xRot) {
        super.absMoveTo(x, y, z, yRot, xRot);
        // Keep wrapper synced with NPC position
    }

    @Override
    public void setDeltaMovement(@NotNull Vec3 deltaMovement) {
        // Delegate movement to the wrapped NPC
        if (wrappedNPC != null) {
            wrappedNPC.setDeltaMovement(deltaMovement);
        }
        super.setDeltaMovement(deltaMovement);
    }

    @Override
    public @NotNull Vec3 getDeltaMovement() {
        return wrappedNPC != null ? wrappedNPC.getDeltaMovement() : super.getDeltaMovement();
    }

    @Override
    public void setOnGround(boolean onGround) {
        if (wrappedNPC != null) {
            wrappedNPC.setOnGround(onGround);
        }
        super.setOnGround(onGround);
    }

    @Override
    public boolean onGround() {
        return wrappedNPC != null ? wrappedNPC.onGround() : super.onGround();
    }

    @Override
    public @NotNull Vec3 getLookAngle() {
        return wrappedNPC != null ? wrappedNPC.getLookAngle() : super.getLookAngle();
    }

    @Override
    public float getYRot() {
        return wrappedNPC != null ? wrappedNPC.getYRot() : super.getYRot();
    }

    @Override
    public float getXRot() {
        return wrappedNPC != null ? wrappedNPC.getXRot() : super.getXRot();
    }

    @Override
    public @NotNull Level level() {
        return wrappedNPC != null ? wrappedNPC.level() : super.level();
    }

    // === EFFECTS AND STATUS DELEGATION ===

    @Override
    public boolean addEffect(@NotNull MobEffectInstance effectInstance, Entity entity) {
        return wrappedNPC != null ? wrappedNPC.addEffect(effectInstance, entity) : super.addEffect(effectInstance, entity);
    }

    @Override
    public boolean removeEffect(net.minecraft.world.effect.@NotNull MobEffect effect) {
        return wrappedNPC != null ? wrappedNPC.removeEffect(effect) : super.removeEffect(effect);
    }

    @Override
    public boolean hasEffect(net.minecraft.world.effect.@NotNull MobEffect effect) {
        return wrappedNPC != null ? wrappedNPC.hasEffect(effect) : super.hasEffect(effect);
    }

    @Override
    public MobEffectInstance getEffect(net.minecraft.world.effect.@NotNull MobEffect effect) {
        return wrappedNPC != null ? wrappedNPC.getEffect(effect) : super.getEffect(effect);
    }

    // === DAMAGE AND HEALTH DELEGATION ===

    @Override
    public boolean hurt(@NotNull DamageSource damageSource, float damage) {
        return wrappedNPC != null ? wrappedNPC.hurt(damageSource, damage) : super.hurt(damageSource, damage);
    }

    @Override
    public void heal(float healAmount) {
        if (wrappedNPC != null) {
            wrappedNPC.heal(healAmount);
        } else {
            super.heal(healAmount);
        }
    }

    // getHealth(), getMaxHealth(), setHealth() are final - can't override
    // The attack system will use the wrapper's health, but damage/healing goes to NPC

    // === INVENTORY AND ITEMS (Dummy implementations) ===

    @Override
    public @NotNull Inventory getInventory() {
        return dummyInventory;
    }

    @Override
    public @NotNull ItemStack getMainHandItem() {
        // NPCs can return empty or a default item
        return ItemStack.EMPTY;
    }

    @Override
    public @NotNull ItemStack getOffhandItem() {
        return ItemStack.EMPTY;
    }

    @Override
    public void setItemInHand(@NotNull InteractionHand hand, @NotNull ItemStack item) {
        // NPCs don't need to actually hold items for attacks
    }

    // === PLAYER INTERFACE REQUIREMENTS ===

    @Override
    public boolean isSpectator() {
        return false;
    }

    @Override
    public boolean isCreative() {
        return false;
    }

    @Override
    public void displayClientMessage(@NotNull Component message, boolean actionBar) {
        // NPCs don't display messages
    }

    @Override
    public void sendSystemMessage(@NotNull Component message) {
        // NPCs don't receive system messages
    }

    // === CONTAINER AND UI METHODS (No-op implementations) ===

    @Override
    public void openTextEdit(@NotNull SignBlockEntity signBlockEntity, boolean front) {
        // NPCs don't edit signs
    }

    @Override
    public void openCommandBlock(@NotNull CommandBlockEntity commandBlock) {
        // NPCs don't use command blocks
    }

    @Override
    public void openStructureBlock(@NotNull StructureBlockEntity structureBlock) {
        // NPCs don't use structure blocks
    }

    @Override
    public void openJigsawBlock(@NotNull JigsawBlockEntity jigsawBlock) {
        // NPCs don't use jigsaw blocks
    }

    @Override
    public void openHorseInventory(@NotNull AbstractHorse horse, @NotNull Container inventory) {
        // NPCs don't ride horses
    }

    // openMenu() return type issue - use proper return type
    @Override
    public java.util.@NotNull OptionalInt openMenu(MenuProvider provider) {
        return java.util.OptionalInt.empty();
    }

    @Override
    public void closeContainer() {
        // NPCs don't have containers
    }

    // === FINAL METHODS - CAN'T OVERRIDE, PROVIDE ACCESS METHODS ===
    // getBoundingBox(), setBoundingBox(), isRemoved(), getMaxHealth(), etc. are final
    // The wrapper will just use its own versions, while damage/effects go to NPC

    /**
     * Get the NPC's bounding box (can't override the final method)
     */
    public net.minecraft.world.phys.AABB getNPCBoundingBox() {
        return wrappedNPC.getBoundingBox();
    }

    /**
     * Check if the wrapped NPC is removed (can't override final method)
     */
    public boolean isNPCRemoved() {
        return wrappedNPC.isRemoved();
    }

    /**
     * Get NPC's health (can't override final getHealth())
     */
    public float getNPCHealth() {
        return wrappedNPC.getHealth();
    }

    /**
     * Get NPC's max health (can't override final getMaxHealth())
     */
    public float getNPCMaxHealth() {
        return wrappedNPC.getMaxHealth();
    }

    // === ADDITIONAL DELEGATIONS FOR ATTACK SYSTEM ===

    @Override
    public boolean isAlive() {
        return wrappedNPC != null ? wrappedNPC.isAlive() : super.isAlive();
    }

    @Override
    public int getId() {
        return wrappedNPC != null ? wrappedNPC.getId() : super.getId();
    }

    @Override
    public java.util.@NotNull UUID getUUID() {
        return wrappedNPC != null ? wrappedNPC.getUUID() : super.getUUID();
    }

    @Override
    public boolean isShiftKeyDown() {
        return false; // NPCs don't crouch in the same way
    }

    // === UTILITY METHODS ===

    /**
     * Update wrapper position to match NPC
     */
    public void syncWithNPC() {
        this.absMoveTo(wrappedNPC.getX(), wrappedNPC.getY(), wrappedNPC.getZ(),
                wrappedNPC.getYRot(), wrappedNPC.getXRot());
    }

    /**
     * Check if this wrapper is still valid (NPC hasn't been removed)
     */
    public boolean isValid() {
        return wrappedNPC != null && !wrappedNPC.isRemoved() && wrappedNPC.isAlive();
    }
}