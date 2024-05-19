package org.example

import kotlinx.cli.ArgParser
import kotlinx.cli.ExperimentalCli
import kotlinx.coroutines.runBlocking

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
