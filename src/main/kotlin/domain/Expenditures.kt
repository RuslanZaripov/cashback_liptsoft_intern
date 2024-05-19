package org.example.domain

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.upsert

// create table where will be store bak id, date and amount of expenditures
// make primary key from bank id and date
// don't use int table
object Expenditures : Table() {
    val bank = reference("bank", Banks)
    val time = varchar("time", 50)
    val amount = double("amount")

    override val primaryKey = PrimaryKey(bank, time)
}

fun spendMoney(bankName: String, time: String, amount: Double) {
    Expenditures.upsert {
        it[this.bank] = findBank(bankName).id
        it[this.time] = time
        it[this.amount] = it.getOrNull(Expenditures.amount) ?: (0.0 + amount)
    }
}

fun getRemainingLimit(bankName: String, time: String): Double? {
    val bank = findBank(bankName)
    if (bank.limit == null) return null
    val expenditures = Expenditures
        .selectAll().where { (Expenditures.bank eq bank.id) and (Expenditures.time eq time) }
        .sumOf { it[Expenditures.amount] }
    return bank.limit!! - expenditures
}
