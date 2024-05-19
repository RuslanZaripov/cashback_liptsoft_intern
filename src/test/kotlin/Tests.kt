import org.example.*
import org.example.domain.*
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.postgresql.ds.PGSimpleDataSource
import kotlin.test.assertFails
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.expect

val dataSource = PGSimpleDataSource().apply {
    user = "liptsoft"
    password = "cashback"
    databaseName = "liptsoft-cashback"
    portNumbers = intArrayOf(5433)
}
val database = Database.connect(dataSource)

class BanksTests {
    private val bank = BankDTO("Тинькофф", 5000.0)
    private val bank2 = BankDTO("Банк Санкт-Петербург", 3000.0)
    private val bank3 = BankDTO("Альфа", 2000.0)
    private val bank4 = BankDTO("Сбербанк", 0.0)

    private val card = CardDTO("МИР", "Тинькофф")
    private val card2 = CardDTO("Банк Санкт-Петербург", "Банк Санкт-Петербург")
    private val card3 = CardDTO("Альфа Кредитка", "Альфа")
    private val card4 = CardDTO("Альфа МИР", "Альфа")
    private val card5 = CardDTO("СберКарта", "Сбербанк")

    private val category = CashbackCategoryDTO("Рестораны", 5.0, false, "МИР")
    private val category2 = CashbackCategoryDTO("Дом и Ремонт", 5.0, false, "МИР")
    private val category3 = CashbackCategoryDTO("Остальное", 1.0, true, "МИР")

    private val category4 = CashbackCategoryDTO("ЖД билеты", 7.0, true, "Банк Санкт-Петербург")
    private val category5 = CashbackCategoryDTO("Остальное", 1.5, true, "Банк Санкт-Петербург")

    private val category6 = CashbackCategoryDTO("Рестораны", 3.0, false, "Альфа Кредитка")
    private val category7 = CashbackCategoryDTO("Заправки", 5.0, true, "Альфа Кредитка")

    private val category8 = CashbackCategoryDTO("Заправки", 5.0, false, "Альфа МИР")
    private val category9 = CashbackCategoryDTO("ЖД билеты", 3.0, false, "Альфа МИР")
    private val category10 = CashbackCategoryDTO("Остальное", 1.0, true, "Альфа МИР")

    private val category11 = CashbackCategoryDTO("Остальное", 1.0, true, "СберКарта")

    private val categoryFuture = CashbackCategoryDTO("Рестораны", 5.0, false, "МИР", "future")

    @BeforeEach
    fun resetDB() {
        transaction(database) {
            SchemaUtils.drop(CashbackCategories)
            SchemaUtils.drop(Cards)
            SchemaUtils.drop(Expenditures)
            SchemaUtils.drop(Banks)

            SchemaUtils.create(Banks)
            SchemaUtils.create(Expenditures)
            SchemaUtils.create(Cards)
            SchemaUtils.create(CashbackCategories)
        }
    }

    @Test
    fun `test bank addition`() {
        transaction(database) {
            val bankEntity = addBank(bank)
            expect(bank) { bankEntity.toDTO() }
        }
    }

    @Test
    fun `test bank uniqueness`() {
        transaction(database) { addBank(bank) }
        assertFails { transaction(database) { addBank(bank) } }
    }

    @Test
    fun `test card addition`() {
        transaction(database) {
            addBank(bank)
            val cardEntity = addCard(card)
            expect(card) { cardEntity.toDTO() }
        }
    }

    @Test
    fun `test card uniqueness`() {
        transaction(database) {
            addBank(bank)
            addCard(card)
        }
        assertFails { transaction(database) { addCard(card) } }
    }

    @Test
    fun `test card addition with non-existent bank`() {
        assertFails { transaction(database) { addCard(card) } }
    }

    @Test
    fun `test current cashback category addition and remove`() {
        transaction(database) {
            addBank(bank)

            val card = addCard(card)
            addCashback(category)

            var cardCategories = card.getCashbackCategories()

            assertTrue { cardCategories.isNotEmpty() and (cardCategories.size == 1) }
            expect(category) { cardCategories[0].toDTO() }

            assertTrue { card.getCurrentCashbackCategory(category.name) != null }
            expect(category) { card.getCurrentCashbackCategory(category.name)!!.toDTO() }

            removeCashback(card.name, category.period, category.name)

            cardCategories = card.getCashbackCategories()

            assertTrue { cardCategories.isEmpty() }
            assertTrue { card.getCurrentCashbackCategory(category.name) == null }
        }
    }

    @Test
    fun `test future cashback category addition and remove`() {
        transaction(database) {
            addBank(bank)

            val card = addCard(card)
            addCashback(categoryFuture)

            var cardCategories = card.getCashbackCategories()

            assertTrue { cardCategories.isNotEmpty() and (cardCategories.size == 1) }
            expect(categoryFuture) { cardCategories[0].toDTO() }

            assertTrue { card.getCurrentCashbackCategory(categoryFuture.name) == null }

            removeCashback(card.name, categoryFuture.period, categoryFuture.name)

            cardCategories = card.getCashbackCategories()

            assertTrue { cardCategories.isEmpty() }
            assertTrue { card.getCurrentCashbackCategory(categoryFuture.name) == null }
        }
    }

    @Test
    fun `remove unknown cashback category`() {
        transaction(database) {
            addBank(bank)
            addCard(card)
            addCashback(category)
            assertFails { removeCashback(card.name, category.period, "unknown") }
        }
    }

    @Test
    fun `test current cashback category uniqueness`() {
        transaction(database) {
            addBank(bank)
            addCard(card)
            addCashback(category)
            assertFails { addCashback(category) }
        }
    }

    private fun setup() {
        transaction(database) {
            addBank(bank)
            addBank(bank2)
            addBank(bank3)
            addBank(bank4)

            addCard(card)
            addCard(card2)
            addCard(card3)
            addCard(card4)
            addCard(card5)

            addCashback(category)
            addCashback(category2)
            addCashback(category3)
            addCashback(category4)
            addCashback(category5)
            addCashback(category6)
            addCashback(category7)
            addCashback(category11)
        }
    }

    @Test
    fun `get card list`() {
        setup()

        transaction(database) {
            val a = listCards()
            assertTrue { a.size == 3 }
        }
    }

    @Test
    fun `test transaction`() {
        setup()

        transaction(database) {
            val value = 1000.0

            transaction(card.name, category.name, value)

            val remainingLimit = getCurrentRemainingLimit(bank.name)

            val calculated = bank.limit!! - value * (category.percent / 100)

            assertTrue { remainingLimit == calculated }
        }
    }

    @Test
    fun `test transaction with unknown card`() {
        setup()

        transaction(database) {
            assertFails { transaction("unknown", category.name, 1000.0) }
        }
    }

    @Test
    fun `test transaction with unknown category`() {
        setup()

        transaction(database) {
            assertFails { transaction(card.name, "unknown", 1000.0) }
        }
    }

    @Test
    fun `test choose card`() {
        setup()

        transaction(database) {
            val actualCard = choose(category.name, 1000.0)
            expect(card) { actualCard!!.toDTO() }

            val actualCard2 = choose(category4.name, 1000.0)
            expect(card2) { actualCard2!!.toDTO() }
        }
    }

    @Test
    fun `test choose card unknown category`() {
        setup()

        transaction(database) {
            assertNull(choose("unknown", 1000.0))
        }
    }

    @Test
    fun `test estimate cashback`() {
        setup()

        transaction(database) {
            val a = estimateCashback()

            assertTrue { a.size == 3 }

            assertTrue { a[0].first.toDTO() == card }
            assertTrue { a[0].second == bank.limit }

            assertTrue { a[1].first.toDTO() == card2 }
            assertTrue { a[1].second == bank2.limit }

            assertTrue { a[2].first.toDTO() == card3 }
            assertTrue { a[2].second == bank3.limit }

            transaction(card.name, category.name, 1000.0)
            transaction(card2.name, category4.name, 1000.0)
            transaction(card3.name, category6.name, 1000.0)

            val b = estimateCashback()

            assertTrue { b.size == 3 }

            assertTrue { b[0].first.toDTO() == card }
            assertTrue { b[0].second == bank.limit!! - 1000.0 * (category.percent / 100) }

            assertTrue { b[1].first.toDTO() == card2 }
            assertTrue { b[1].second == bank2.limit!! - 1000.0 * (category4.percent / 100) }

            assertTrue { b[2].first.toDTO() == card3 }
            assertTrue { b[2].second == bank3.limit!! - 1000.0 * (category6.percent / 100) }
        }
    }
}
