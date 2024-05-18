package org.example

import kotlinx.coroutines.Dispatchers
import org.example.domain.Banks
import org.example.domain.Cards
import org.example.domain.CashbackCategories
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import org.postgresql.ds.PGSimpleDataSource

object DatabaseSingleton {
    fun init() {
        val dataSource = PGSimpleDataSource().apply {
            user = "liptsoft"
            password = "cashback"
            databaseName = "liptsoft-cashback"
            portNumbers = intArrayOf(5431)
        }
        val database = Database.connect(dataSource)
        transaction(database) {
            SchemaUtils.createMissingTablesAndColumns(Banks)
            SchemaUtils.createMissingTablesAndColumns(Cards)
            SchemaUtils.createMissingTablesAndColumns(CashbackCategories)
        }
    }

    suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }
}
