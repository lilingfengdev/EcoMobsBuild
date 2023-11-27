package com.willfp.ecomobs.nms.v1_20_R1

import com.willfp.eco.core.packet.Packet
import com.willfp.eco.core.packet.sendPacket
import com.willfp.ecomobs.name.DisplayNameProxy
import io.papermc.paper.adventure.PaperAdventure
import net.kyori.adventure.text.Component
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket
import net.minecraft.network.syncher.EntityDataAccessor
import net.minecraft.network.syncher.SynchedEntityData
import net.minecraft.world.entity.Entity
import org.bukkit.craftbukkit.v1_20_R1.entity.CraftMob
import org.bukkit.entity.Mob
import org.bukkit.entity.Player
import java.util.Optional

@Suppress("UNCHECKED_CAST")
class DisplayName : DisplayNameProxy {
    private val displayNameAccessor = Entity::class.java
        .declaredFields
        .filter { it.type == EntityDataAccessor::class.java }
        .toList()[2]
        .apply { isAccessible = true }
        .get(null) as EntityDataAccessor<Optional<net.minecraft.network.chat.Component>>

    private val customNameVisibleAccessor = Entity::class.java
        .declaredFields
        .filter { it.type == EntityDataAccessor::class.java }
        .toList()[3]
        .apply { isAccessible = true }
        .get(null) as EntityDataAccessor<Boolean>

    override fun setDisplayName(mob: Mob, player: Player, displayName: Component, visible: Boolean) {
        if (mob !is CraftMob) {
            return
        }

        val nmsComponent = PaperAdventure.asVanilla(displayName)
            ?: throw IllegalStateException("Display name component is null!")

        val nmsEntity = mob.handle
        nmsEntity.isCustomNameVisible
        val entityData = SynchedEntityData(nmsEntity)

        entityData.forceSet(displayNameAccessor, Optional.of(nmsComponent))
        entityData.forceSet(customNameVisibleAccessor, visible)

        val packet = ClientboundSetEntityDataPacket(
            nmsEntity.id,
            entityData.packDirty() ?: throw IllegalStateException("No packed entity data")
        )

        player.sendPacket(Packet(packet))
    }

    private fun <T> SynchedEntityData.forceSet(
        accessor: EntityDataAccessor<T>,
        value: T
    ) {
        if (!this.hasItem(accessor)) {
            this.define(accessor, value)
        }
        this[accessor] = value
        this.markDirty(accessor)
    }
}