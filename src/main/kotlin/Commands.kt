@file:OptIn(ExperimentalCli::class)

package org.example

import kotlinx.cli.*
import kotlinx.coroutines.runBlocking
import org.example.DatabaseSingleton.dbQuery
import org.example.domain.*

// add bank <name> [limit]
class AddBank : Subcommand("add-bank", "Add bank to database") {
    private val bankName by option(ArgType.String, "bank", "b", "bank name").required()
    private val limit by option(ArgType.Double, "limit", "l", "bank cashback limit").default(0.0)

    override fun execute(): Unit = runBlocking {
        println("add bank $bankName with limit $limit")
        dbQuery { addBank(BankDTO(bankName, limit)) }
    }
}


// add card <bank name> <card name>
class AddCard : Subcommand("add-card", "Add card to database") {
    private val bankName by option(ArgType.String, "bank", "b", "bank name").required()
    private val cardName by option(ArgType.String, "card", "c", "card name").required()

    override fun execute(): Unit = runBlocking {
        println("add card $cardName to $bankName")
        dbQuery { addCard(CardDTO(cardName, bankName)) }
    }
}

// add current-cashback <card name> <category> <percent> [permanent]
// add future-cashback <card name> <category> <percent> [permanent]
class AddCashback : Subcommand("add-cashback", "Add cashback category for card") {
    private val period by option(ArgType.String, "period", "period").required()
    private val cardName by option(ArgType.String, "card", "c", "card name").required()
    private val category by option(ArgType.String, "category", "category name").required()
    private val percent by option(ArgType.Int, "percent", "percent").required()
    private val permanent by option(ArgType.Boolean, "permanent", "percent").default(false)


    override fun execute(): Unit = runBlocking {
        println("add category $category for $cardName with $percent [$permanent] $period")

        // TODO: add period
        dbQuery { addCashback(CashbackCategoryDTO(category, percent, permanent, cardName)) }
    }
}

// add transaction <card name> <category> <value>
class AddTransaction : Subcommand("add-transaction", "Add transaction") {
    private val cardName by option(ArgType.String, "card", "c", "card name").required()
    private val category by option(ArgType.String, "category", "category name").required()
    private val value by option(ArgType.Double, "value", "v", "transaction value").default(0.0)

    override fun execute(): Unit = runBlocking {
        println("add transaction $cardName $category $value")

        dbQuery { transaction(cardName, category, value) }
    }
}

// card list
class CardList : Subcommand("card-list", "List all cards") {
    override fun execute() {
        println("card list")
    }
}

// estimate cashback
class EstimateCashback : Subcommand("estimate-cashback", "Estimate cashback") {
    override fun execute(): Unit = runBlocking {
        println("estimate cashback")
        dbQuery { estimateCashback() }
    }
}

// choose card <category> [value]
class ChooseCard : Subcommand("choose-card", "Choose card") {
    private val category by option(ArgType.String, "category", "c", "category name").required()
    private val value by option(ArgType.Double, "value", "v", "transaction value").default(0.0)

    override fun execute() {
        println("choose card $category $value")
    }
}

// remove current cashback <card name> <category>
// remove future cashback <card name> <category>
class RemoveCashback : Subcommand("remove-cashback", "Remove cashback") {
    private val period by option(ArgType.String, "period", "period").required()
    private val cardName by option(ArgType.String, "card", "c", "card name").required()
    private val category by option(ArgType.String, "category", "category name").required()

    override fun execute() {
        println("remove cashback from $period $cardName $category")
    }
}
