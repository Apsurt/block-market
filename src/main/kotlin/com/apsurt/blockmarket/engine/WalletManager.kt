package com.apsurt.blockmarket.engine

import java.util.UUID

class WalletManager {
    // In-memory storage for the MVP.
    // Later, we will tie this to Minecraft's server-side saving system (NBT/JSON).
    private val balances = mutableMapOf<UUID, Coins>()

    /**
     * Gets the current liquid balance of a player.
     * Defaults to 0 if they don't have an account yet.
     */
    fun getBalance(uuid: UUID): Coins {
        return balances.getOrDefault(uuid, 0L)
    }

    /**
     * Mints new coins and adds them to a player's wallet.
     * Throws an error if someone tries to add negative money.
     */
    fun addCoins(uuid: UUID, amount: Coins) {
        require(amount >= 0) { "Cannot add a negative amount of coins: $amount" }

        val currentBalance = getBalance(uuid)
        balances[uuid] = currentBalance + amount
    }

    /**
     * Attempts to remove coins from a player's wallet.
     * @return true if successful, false if the player doesn't have enough funds.
     */
    fun removeCoins(uuid: UUID, amount: Coins): Boolean {
        require(amount >= 0) { "Cannot remove a negative amount of coins: $amount" }

        val currentBalance = getBalance(uuid)

        if (currentBalance >= amount) {
            balances[uuid] = currentBalance - amount
            return true
        }

        return false // Insufficient funds!
    }

    /**
     * Optional utility function: Force-sets a balance (useful for admin commands).
     */
    fun setBalance(uuid: UUID, amount: Coins) {
        require(amount >= 0) { "Cannot set a negative balance: $amount" }
        balances[uuid] = amount
    }
}