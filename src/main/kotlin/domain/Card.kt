package org.example.domain

import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable

object Cards : IntIdTable() {
    val name = varchar("name", 50).uniqueIndex()
    val bank = reference("bank", Banks)
}

class Card(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Card>(Cards)

    var name by Cards.name
    var bank by Bank referencedOn Cards.bank

    override fun toString(): String = "$name (${bank.name})"
}

data class CardDTO(val name: String, val bankName: String)

fun Card.toDTO() = CardDTO(name, bank.name)

fun findCard(cardName: String) =
    Card.find { Cards.name eq cardName }.firstOrNull()
        ?: throw IllegalArgumentException("Card not found")

fun Card.getCashbackCategories() =
    CashbackCategory
        .find { CashbackCategories.card eq id }
        .toList()

fun Card.getCashbackCategory(categoryName: String): CashbackCategory? =
    getCashbackCategories()
        .firstOrNull { it.name == categoryName }
