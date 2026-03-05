package com.apsurt.blockmarket.engine

import com.apsurt.blockmarket.data.MarketState
import java.util.UUID

class WalletManager {
    private var state: MarketState? = null

    /**
     * Called when the server starts to inject the loaded NBT state.
     */
    fun loadState(marketState: MarketState) {
        this.state = marketState
    }

    /**
     * Gets the current liquid balance of a player.
     * Defaults to 0 if they don't have an account yet.
     */
    fun getBalance(uuid: UUID): Coins {
        return state?.balances?.getOrDefault(uuid, 0L) ?: 0L
    }

    /**
     * Mints new coins and adds them to a player's wallet.
     * Throws an error if someone tries to add negative money.
     */
    fun addCoins(uuid: UUID, amount: Coins) {
        require(amount >= 0) { "Cannot add a negative amount of coins: $amount" }

        val currentState = state ?: return

        val currentBalance = getBalance(uuid)
        currentState.balances[uuid] = currentBalance + amount
        currentState.markDirty()
    }

    /**
     * Attempts to remove coins from a player's wallet.
     * @return true if successful, false if the player doesn't have enough funds.
     */
    fun removeCoins(uuid: UUID, amount: Coins): Boolean {
        require(amount >= 0) { "Cannot remove a negative amount of coins: $amount" }

        val currentState = state ?: return false
        val currentBalance = getBalance(uuid)

        if (currentBalance >= amount) {
            currentState.balances[uuid] = currentBalance - amount
            currentState.markDirty()
            return true
        }

        return false
    }

    /**
     * Optional utility function: Force-sets a balance (useful for admin commands).
     */
    fun setBalance(uuid: UUID, amount: Coins) {
        require(amount >= 0) { "Cannot set a negative balance: $amount" }

        val currentState = state ?: return
        currentState.balances[uuid] = amount
        currentState.markDirty()
    }
}