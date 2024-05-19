package org.example

import org.example.domain.*
import org.jetbrains.exposed.sql.insertAndGetId
import java.time.YearMonth

interface MonthProvider {
    fun getYearMonth(): YearMonth
    fun getMonth(): Int = getYearMonth().monthValue
}

object DefaultMonthProvider : MonthProvider {
    override fun getYearMonth(): YearMonth = YearMonth.now()
}

class FutureMonthProvider(private var yearMonth: YearMonth) : MonthProvider {
    override fun getYearMonth(): YearMonth = yearMonth

    fun advanceMonth() {
        yearMonth = yearMonth.plusMonths(1)
    }
}


class CashbackService(
    private val monthProvider: MonthProvider = DefaultMonthProvider,
) {
    private fun getIntPeriod(period: String): Int {
        val currentMonth = monthProvider.getYearMonth()
        return when (period) {
            "current" -> currentMonth
            "future" -> currentMonth.plusMonths(1)
            else -> throw IllegalArgumentException("Invalid period")
        }.monthValue
    }

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
        val period = getIntPeriod(category.period)

        val card = findCard(category.cardName)

        val insertedId = CashbackCategories.insertAndGetId {
            it[this.name] = category.name
            it[this.percent] = category.percent
            it[this.permanent] = category.permanent
            it[this.period] = period
            it[this.card] = card.id
        }

        return CashbackCategory.findById(insertedId)!!
    }

    fun removeCashback(cardName: String, period: String, categoryName: String) {
        val card = findCard(cardName)

        val periodValue = getIntPeriod(period)

        val category = getCashbackCategories(card)
            .firstOrNull { it.name == categoryName && it.period == periodValue }
            ?: throw IllegalArgumentException("Category not found")

        category.delete()
    }

    fun transaction(cardName: String, categoryName: String, value: Double) {
        val card = findCard(cardName)

        val limit = card.bank.limit ?: return

        val category = getCurrentCashbackCategory(card, categoryName)
            ?: throw IllegalArgumentException("Category not found")

        val cashback = (category.percent / 100) * value

        // format time in yyyy-MM
        val period = monthProvider.getYearMonth().toString()

        spendMoney(card.bank.name, period, cashback)
    }

    fun estimateCashback(): List<Pair<Card, Double?>> {
        val period = monthProvider.getYearMonth().toString()
        return getAllBanks()
            .filter {
                val a = getRemainingLimit(it.name, period)
                (a == null) || (a > 0.0)
            }
            .map { bank ->
                bank.getCards()
                    .filter { getCashbackCategories(it).isNotEmpty() } // maybe card has no cashback categories
                    .toList()
            }
            .flatten()
            .map { card ->
                val remaining = getRemainingLimit(card.bank.name, period)
                Pair(card, remaining)
            }
    }

    fun choose(categoryName: String, value: Double): Card? {
        val period = monthProvider.getYearMonth().toString()
        return getAllBanks()
            .filter {
                val a = getRemainingLimit(it.name, period)
                (a == null) || (a >= value)
            }
            .map { bank ->
                bank.getCards()
                    .filter { getCashbackCategories(it).any { category -> category.name == categoryName } }
            }
            .flatten()
            .maxByOrNull { getCurrentCashbackCategory(it, categoryName)!!.percent }
    }

    fun listCards(): List<Card> {
        val period = monthProvider.getYearMonth().toString()
        return getAllBanks()
            .filter {
                val a = getRemainingLimit(it.name, period)
                (a == null) || (a > 0.0)
            }
            .map { bank ->
                bank.getCards()
                    .filter { getCashbackCategories(it).isNotEmpty() }
            }
            .flatten()
    }

    fun getCurrentRemainingLimit(bankName: String): Double? {
        return getRemainingLimit(bankName, monthProvider.getYearMonth().toString())
    }

    fun convertPeriod(period: Int): String =
        if (period == monthProvider.getMonth()) "current" else "future"

    fun getCashbackCategories(card: Card) =
        CashbackCategory.find { CashbackCategories.card eq card.id }.toList()

    fun getCurrentCashbackCategory(card: Card, categoryName: String): CashbackCategory? =
        getCashbackCategories(card)
            .find { it.name == categoryName && it.period == monthProvider.getMonth() }
}
