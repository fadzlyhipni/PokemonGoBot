/**
 * Pokemon Go Bot  Copyright (C) 2016  PokemonGoBot-authors (see authors.md for more information)
 * This program comes with ABSOLUTELY NO WARRANTY;
 * This is free software, and you are welcome to redistribute it under certain conditions.
 *
 * For more information, refer to the LICENSE file in this repositories root directory
 */

package ink.abb.pogo.scraper.util.pokemon

import com.pokegoapi.api.pokemon.Pokemon
import ink.abb.pogo.scraper.Settings

fun Pokemon.getIv(): Int {
    val iv = individualAttack + individualDefense + individualStamina
    return iv
}

fun Pokemon.getIvPercentage(): Int {
    val iv = getIv()
    val ivPercentage = (iv * 100) / 45
    return ivPercentage
}

fun Pokemon.getCpPercentageToPlayer(): Int {
    return (cp.toDouble() / maxCpForPlayer.toDouble() * 100).toInt()
}

fun Pokemon.getStatsFormatted(): String {
    val details = "Stamina: $individualStamina | Attack: $individualAttack | Defense: $individualDefense"
    val maxCpDetails = "Max Trainer CP: $maxCpForPlayer (${getCpPercentageToPlayer()}%)"
    return details + " | IV: ${getIv()} (${(getIvPercentage())}%) | $maxCpDetails "
}

fun isTooMany(settings: Settings, pokemonCounts: MutableMap<String, Int>, pokemon: Pokemon): Boolean {
    val max = settings.maxPokemonAmount
    if (max == -1) {
        return false
    }
    val name = pokemon.pokemonId.name
    val count = pokemonCounts.getOrElse(name, { 0 }) + 1
    pokemonCounts.put(name, count)
    return (count > max)
}

fun Pokemon.shouldTransfer(settings: Settings, pokemonCounts: MutableMap<String, Int>): Pair<Boolean, String> {
    val obligatoryTransfer = settings.obligatoryTransfer
    val ignoredPokemon = settings.ignoredPokemon
    val ivPercentage = getIvPercentage()
    val minIVPercentage = settings.transferIvThreshold
    val minCP = settings.transferCpThreshold
    val minCpPercentage = settings.transferCpMinThreshold

    // add 1 to the map
    val isTooMany = isTooMany(settings, pokemonCounts, this)

    var shouldRelease = obligatoryTransfer.contains(this.pokemonId)
    var reason: String = "Obligatory transfer"
    if (!ignoredPokemon.contains(this.pokemonId)) {
        // shouldn't release? check for IV/CP
        if (!shouldRelease) {
            var ivTooLow = false
            var cpTooLow = false
            var maxCpInRange = false

            // never transfer > min IV percentage (unless set to -1)
            if (ivPercentage < minIVPercentage || minIVPercentage == -1) {
                ivTooLow = true
            }
            // never transfer > min CP  (unless set to -1)
            if (this.cp < minCP || minCP == -1) {
                cpTooLow = true
            }
            reason = "CP < $minCP and IV < $minIVPercentage%"

            if (minCpPercentage != -1 && minCpPercentage >= getCpPercentageToPlayer()) {
                maxCpInRange = true
                reason += " and CP max $maxCpForPlayer: achieved ${getCpPercentageToPlayer()}% <= $minCpPercentage%"
            }
            shouldRelease = ivTooLow && cpTooLow && (maxCpInRange || minCpPercentage == -1)
        }
        // still shouldn't release? Check if we have too many
        if (!shouldRelease && isTooMany) {
            shouldRelease = true
            reason = "Too many"
        }
        // Save pokemon for evolve stacking
        if (settings.evolveBeforeTransfer.contains(this.pokemonId) && settings.evolveStackLimit > 0){
            shouldRelease = false
        }
    }
    return Pair(shouldRelease, reason)
}
