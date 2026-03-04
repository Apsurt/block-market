package com.apsurt.blockmarket.engine

import java.util.UUID

class EscrowManager {

    /**
     * The Inbox holds what a player has earned from trades but hasn't physically claimed yet.
     */
    data class Inbox(
        var claimableCoins: Coins = 0L,
        val claimableItems: MutableMap<AssetId, Int> = mutableMapOf()
    )

    // Player UUID -> Their Inbox
    private val inboxes = mutableMapOf<UUID, Inbox>()

    // Vaults for active Limit Orders (Locked until filled or canceled)
    private val lockedCoins = mutableMapOf<UUID, Coins>()
    private val lockedItems = mutableMapOf<UUID, MutableMap<AssetId, Int>>()

    // --- INBOX LOGIC ---

    fun getInbox(uuid: UUID): Inbox = inboxes.getOrPut(uuid) { Inbox() }

    /**
     * Deposits trade results into a player's inbox.
     * This is how offline players "get paid."
     */
    fun depositToInbox(uuid: UUID, assetId: AssetId? = null, coinAmount: Coins = 0L, itemAmount: Int = 0) {
        val inbox = getInbox(uuid)
        inbox.claimableCoins += coinAmount

        if (assetId != null && itemAmount > 0) {
            val current = inbox.claimableItems.getOrDefault(assetId, 0)
            inbox.claimableItems[assetId] = current + itemAmount
        }
    }

    // --- LOCKING LOGIC (Vaults) ---

    fun lockCoins(uuid: UUID, amount: Coins) {
        lockedCoins[uuid] = lockedCoins.getOrDefault(uuid, 0L) + amount
    }

    fun unlockCoins(uuid: UUID, amount: Coins) {
        val current = lockedCoins.getOrDefault(uuid, 0L)
        lockedCoins[uuid] = (current - amount).coerceAtLeast(0L)
    }

    fun lockItems(uuid: UUID, assetId: AssetId, amount: Int) {
        val playerVault = lockedItems.getOrPut(uuid) { mutableMapOf() }
        playerVault[assetId] = playerVault.getOrDefault(assetId, 0) + amount
    }

    fun unlockItems(uuid: UUID, assetId: AssetId, amount: Int) {
        val playerVault = lockedItems[uuid] ?: return
        val current = playerVault.getOrDefault(assetId, 0)
        playerVault[assetId] = (current - amount).coerceAtLeast(0)
    }

    /**
     * Helper to see total value currently tied up in market orders.
     */
    fun getLockedCoins(uuid: UUID): Coins = lockedCoins.getOrDefault(uuid, 0L)
}