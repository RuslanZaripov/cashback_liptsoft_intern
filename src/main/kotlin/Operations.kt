package org.example

import org.example.domain.*
import java.time.LocalDate

fun addBank(bank: BankDTO): Bank =
    Bank.new {
        name = bank.name
        limit = bank.limit
    }


fun addCard(card: CardDTO): Card =
    Card.new {
        name = card.name
        bank = findBank(card.bankName)
    }


fun addCashback(category: CashbackCategoryDTO): CashbackCategory {
    // take current month value as period

    val currentMonth = LocalDate.now()
    val period = when (category.period) {
        "current" -> currentMonth
        "future" -> currentMonth.plusMonths(1)
        else -> throw IllegalArgumentException("Invalid period")
    }.monthValue

    val card = findCard(category.cardName)

    val categories = card.getCashbackCategories()

    if (categories.any { it.name == category.name }) {
        throw IllegalArgumentException("Category already exists")
    }

    return CashbackCategory.new {
        this.name = category.name
        this.period = period
        this.percent = category.percent
        this.permanent = category.permanent
        this.cards = card
    }
}

fun removeCashback(cardName: String, period: String, categoryName: String) {
    val card = findCard(cardName)

    val currentMonth = LocalDate.now()
    val periodValue = when (period) {
        "current" -> currentMonth
        "future" -> currentMonth.plusMonths(1)
        else -> throw IllegalArgumentException("Invalid period")
    }.monthValue

    val category = card.getCashbackCategories()
        .firstOrNull { it.name == categoryName && it.period == periodValue }
        ?: throw IllegalArgumentException("Category not found")

    category.delete()
}


fun transaction(cardName: String, categoryName: String, value: Double) {
    val card = findCard(cardName)

    val limit = card.bank.limit ?: return

    val category = card.getCurrentCashbackCategory(categoryName)
        ?: throw IllegalArgumentException("Category not found")

    val cashback = category.percent * value

    // format time in yyyy-MM
    val period = LocalDate.now().toString().substring(0, 7)

    spendMoney(card.bank.name, period, cashback)
}

fun estimateCashback(): List<Pair<Card, Double?>> {
    val period = LocalDate.now().toString().substring(0, 7)
    return getAllBanks()
        .filter {
            val a = getRemainingLimit(it.name, period)
            (a == null) || (a > 0.0)
        }
        .map { bank ->
            bank.getCards()
                .filter { it.getCashbackCategories().isNotEmpty() } // maybe card has no cashback categories
                .toList()
        }
        .flatten()
        .map { card ->
            val remaining = getRemainingLimit(card.bank.name, period)
            Pair(card, remaining)
        }
}

fun choose(categoryName: String, value: Double): Card? {
    val period = LocalDate.now().toString().substring(0, 7)
    return getAllBanks()
        .filter {
            val a = getRemainingLimit(it.name, period)
            (a == null) || (a >= value)
        }
        .map { bank ->
            bank.getCards()
                .filter { it.getCashbackCategories().any { category -> category.name == categoryName } }
        }
        .flatten()
        .maxByOrNull { it.getCurrentCashbackCategory(categoryName)!!.percent }
}

fun listCards() {
    getAllBanks()
        .filter { it.limit != null }
        .forEach { bank ->
            bank.getCards()
                .filter { it.getCashbackCategories().isNotEmpty() }
                .forEach { println(it) }
        }
}
