package org.example.domain

import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable

object Banks : IntIdTable() {
    val name = varchar("name", 50).uniqueIndex()
    val limit = double("limit")
}

data class BankDTO(val name: String, val limit: Double)

class Bank(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Bank>(Banks)

    var name by Banks.name
    var limit by Banks.limit

    override fun toString(): String =
        "Bank(id=$id, name=$name, limit=$limit)"
}

fun Bank.toDTO() = BankDTO(name, limit)

fun addBank(bank: BankDTO) =
    Bank.new {
        name = bank.name
        limit = bank.limit
    }

fun findBank(bankName: String): Bank =
    Bank.find { Banks.name eq bankName }.firstOrNull()
        ?: throw IllegalArgumentException("Bank not found")

fun modifyLimit(bankName: String, newLimit: Double) {
    val bank = findBank(bankName)
    bank.limit = newLimit
}

fun getAllBanks() = Bank.all().toList()
