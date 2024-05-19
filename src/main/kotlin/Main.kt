package org.example

import kotlinx.cli.ArgParser
import kotlinx.cli.ExperimentalCli
import kotlinx.coroutines.runBlocking

@OptIn(ExperimentalCli::class)
fun main(args: Array<String>): Unit = runBlocking {
    DatabaseSingleton.init()

    val parser = ArgParser("cashback")

    val service = CashbackService()

    parser.subcommands(
        AddBank(service),
        AddCard(service),
        AddCashback(service),
        AddTransaction(service),
        CardList(service),
        EstimateCashback(service),
        ChooseCard(service),
        RemoveCashback(service)
    )

    parser.parse(args)
}
