package org.example

import org.example.domain.*

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

fun removeCashback(cardName: String, categoryName: String) {
    TODO("Not yet implemented")
}


fun transaction(cardName: String, categoryName: String, value: Double) {
    val card = findCard(cardName)

    val limit = card.bank.limit ?: return

    val category = card.getCashbackCategory(categoryName)
        ?: throw IllegalArgumentException("Category not found")

    val cashback = category.percent * value

    val newLimit = limit - cashback

    modifyLimit(card.bank.name, newLimit)
}

fun estimateCashback() {
    getAllBanks().forEach { println(it) }
}

fun choose(categoryName: String, value: Double): Card? {
    return getAllBanks()
        .filter { (it.limit != null) and (it.limit!! > value) }
        .map { bank ->
            bank.getCards()
                .filter { it.getCashbackCategories().any { category -> category.name == categoryName } }
        }
        .flatten()
        .maxByOrNull { it.getCashbackCategory(categoryName)!!.percent }
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
