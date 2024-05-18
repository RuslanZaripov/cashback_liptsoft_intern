package org.example.domain

import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable


object CashbackCategories : IntIdTable() {
    val name = varchar("category", 50).uniqueIndex()
    val percent = integer("percent")
    val permanent = bool("permanent")
    val card = reference("card", Cards)
}

class CashbackCategory(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<CashbackCategory>(CashbackCategories)

    var name by CashbackCategories.name
    var percent by CashbackCategories.percent
    var permanent by CashbackCategories.permanent

    var cards by Card referencedOn CashbackCategories.card
}

data class CashbackCategoryDTO(
    val name: String,
    val percent: Int,
    val permanent: Boolean,
    val cardName: String,
)

fun addCashback(category: CashbackCategoryDTO): CashbackCategory {
    val card = findCard(category.cardName)

    val categories = card.getCashbackCategories()

    if (categories.any { it.name == category.name }) {
        throw IllegalArgumentException("Category already exists")
    }

    return CashbackCategory.new {
        name = category.name
        percent = category.percent
        permanent = category.permanent
        cards = card
    }
}
