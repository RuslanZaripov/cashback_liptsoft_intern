package org.example

import kotlinx.cli.ArgParser
import kotlinx.cli.ExperimentalCli
import kotlinx.coroutines.runBlocking
import org.example.domain.findCard
import org.example.domain.getAllBanks
import org.example.domain.getCashbackCategory
import org.example.domain.modifyLimit

@OptIn(ExperimentalCli::class)
fun main(args: Array<String>): Unit = runBlocking {
    DatabaseSingleton.init()

    val parser = ArgParser("cashback")

    parser.subcommands(
        AddBank(),
        AddCard(),
        AddCashback(),
        AddTransaction(),
        CardList(),
        EstimateCashback(),
        ChooseCard(),
        RemoveCashback()
    )

    parser.parse(args)
}


fun transaction(cardName: String, categoryName: String, value: Double) {
    val card = findCard(cardName)

    val category = card.getCashbackCategory(categoryName)
        ?: throw IllegalArgumentException("Category not found")

    val cashback = category.percent * value

    val newLimit = card.bank.limit - cashback

    modifyLimit(card.bank.name, newLimit)
}

fun estimateCashback() {
    getAllBanks().forEach { bank ->
        println("${bank.name} ${bank.limit}")
    }
}

fun choose(categoryName: String, value: Double) {
}

fun getCards() {
}