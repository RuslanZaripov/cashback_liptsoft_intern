@file:OptIn(ExperimentalCli::class)

package org.example

import kotlinx.cli.*
import kotlinx.coroutines.runBlocking
import org.example.DatabaseSingleton.dbQuery
import org.example.domain.BankDTO
import org.example.domain.CardDTO
import org.example.domain.CashbackCategoryDTO

// add bank <name> [limit]
class AddBank(private val service: CashbackService) : Subcommand("add-bank", "Add bank to database") {
    private val bankName by option(ArgType.String, "bank", "b", "bank name").required()
    private val limit by option(ArgType.Double, "limit", "l", "bank cashback limit").default(0.0)

    override fun execute(): Unit = runBlocking {
        println("add bank $bankName with limit $limit")

        dbQuery { service.addBank(BankDTO(bankName, limit)) }
    }
}


// add card <bank name> <card name>
class AddCard(private val service: CashbackService) : Subcommand("add-card", "Add card to database") {
    private val bankName by option(ArgType.String, "bank", "b", "bank name").required()
    private val cardName by option(ArgType.String, "card", "c", "card name").required()

    override fun execute(): Unit = runBlocking {
        println("add card $cardName to $bankName")

        dbQuery { service.addCard(CardDTO(cardName, bankName)) }
    }
}

// add current-cashback <card name> <category> <percent> [permanent]
// add future-cashback <card name> <category> <percent> [permanent]
class AddCashback(private val service: CashbackService) : Subcommand("add-cashback", "Add cashback category for card") {
    private val period by option(ArgType.Choice(listOf("current", "future"), { it }), "period", "period").required()
    private val cardName by option(ArgType.String, "card", "c", "card name").required()
    private val category by option(ArgType.String, "category", "category name").required()
    private val percent by option(ArgType.Double, "percent", "percent").required()
    private val permanent by option(ArgType.Boolean, "permanent", "percent").default(false)


    override fun execute(): Unit = runBlocking {
        println("add category $category for $cardName with $percent [$permanent] $period")

        // TODO: add period
        dbQuery { service.addCashback(CashbackCategoryDTO(category, percent, permanent, cardName, period)) }
    }
}

// add transaction <card name> <category> <value>
class AddTransaction(val service: CashbackService) : Subcommand("add-transaction", "Add transaction") {
    private val cardName by option(ArgType.String, "card", "c", "card name").required()
    private val category by option(ArgType.String, "category", "category name").required()
    private val value by option(ArgType.Double, "value", "v", "transaction value").default(0.0)

    override fun execute(): Unit = runBlocking {
        println("add transaction $cardName $category $value")

        dbQuery { service.transaction(cardName, category, value) }
    }
}

// card list
class CardList(private val service: CashbackService) : Subcommand("card-list", "List all cards") {
    override fun execute(): Unit = runBlocking {
        println("list cards")

        dbQuery { service.listCards().forEach { println(it) } }
    }
}

// estimate cashback
class EstimateCashback(val service: CashbackService) : Subcommand("estimate-cashback", "Estimate cashback") {
    override fun execute(): Unit = runBlocking {
        println("estimate cashback")

        dbQuery { service.estimateCashback().forEach { println(it) } }
    }
}

// choose card <category> [value]
class ChooseCard(private val service: CashbackService) : Subcommand("choose-card", "Choose card") {
    private val category by option(ArgType.String, "category", "c", "category name").required()
    private val value by option(ArgType.Double, "value", "v", "transaction value").default(0.0)

    override fun execute(): Unit = runBlocking {
        println("choose card $category $value")

        dbQuery {
            val card = service.choose(category, value)
            if (card != null) {
                println(card)
            } else {
                println("No card found")
            }
        }
    }
}

// remove current cashback <card name> <category>
// remove future cashback <card name> <category>
class RemoveCashback(private val service: CashbackService) : Subcommand("remove-cashback", "Remove cashback") {
    private val period by option(ArgType.Choice(listOf("current", "future"), { it }), "period", "period").required()
    private val cardName by option(ArgType.String, "card", "c", "card name").required()
    private val category by option(ArgType.String, "category", "category name").required()

    override fun execute(): Unit = runBlocking {
        println("remove cashback from $period $cardName $category")

        dbQuery { service.removeCashback(cardName, period, category) }
    }
}
