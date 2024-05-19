package org.example.domain

import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import java.time.LocalDate


object CashbackCategories : IntIdTable() {
    val name = varchar("category", 50)
    val period = integer("period")
    val percent = double("percent")
    val permanent = bool("permanent")
    val card = reference("card", Cards)

    init {
        uniqueIndex(name, period)
    }
}

class CashbackCategory(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<CashbackCategory>(CashbackCategories)

    var name by CashbackCategories.name
    var percent by CashbackCategories.percent
    var permanent by CashbackCategories.permanent
    var period by CashbackCategories.period

    var cards by Card referencedOn CashbackCategories.card
}

data class CashbackCategoryDTO(
    val name: String,
    val percent: Double,
    val permanent: Boolean,
    val cardName: String,
    val period: String = "current",
)

fun convertPeriod(period: Int): String =
    if (period == LocalDate.now().monthValue) "current" else "future"

fun CashbackCategory.toDTO(): CashbackCategoryDTO =
    CashbackCategoryDTO(name, percent, permanent, cards.name, convertPeriod(period))
