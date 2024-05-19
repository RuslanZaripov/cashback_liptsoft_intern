import org.example.CashbackService
import org.example.FutureMonthProvider
import org.example.domain.*
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.postgresql.ds.PGSimpleDataSource
import java.time.YearMonth
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
    private val service = CashbackService()

    private val monthProvider = FutureMonthProvider(YearMonth.of(2024, 5))
    private val future_service = CashbackService(monthProvider)

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
            val bankEntity = service.addBank(bank)
            expect(bank) { bankEntity.toDTO() }
        }
    }

    @Test
    fun `test bank uniqueness`() {
        transaction(database) { service.addBank(bank) }
        assertFails { transaction(database) { service.addBank(bank) } }
    }

    @Test
    fun `test card addition`() {
        transaction(database) {
            service.addBank(bank)
            val cardEntity = service.addCard(card)
            expect(card) { cardEntity.toDTO() }
        }
    }

    @Test
    fun `test card uniqueness`() {
        transaction(database) {
            service.addBank(bank)
            service.addCard(card)
        }
        assertFails { transaction(database) { service.addCard(card) } }
    }

    @Test
    fun `test card addition with non-existent bank`() {
        assertFails { transaction(database) { service.addCard(card) } }
    }

    @Test
    fun `test current cashback category addition and remove`() {
        transaction(database) {
            service.addBank(bank)

            val card = service.addCard(card)
            service.addCashback(category)

            var cardCategories = service.getCashbackCategories(card)

            assertTrue { cardCategories.isNotEmpty() and (cardCategories.size == 1) }
            expect(category) { cardCategories[0].toDTO(service) }

            assertTrue { service.getCurrentCashbackCategory(card, category.name) != null }
            expect(category) { service.getCurrentCashbackCategory(card, category.name)!!.toDTO(service) }

            service.removeCashback(card.name, category.period, category.name)

            cardCategories = service.getCashbackCategories(card)

            assertTrue { cardCategories.isEmpty() }
            assertTrue { service.getCurrentCashbackCategory(card, category.name) == null }
        }
    }

    @Test
    fun `test future cashback category addition and remove`() {
        transaction(database) {
            service.addBank(bank)

            val card = service.addCard(card)
            service.addCashback(categoryFuture)

            var cardCategories = service.getCashbackCategories(card)

            assertTrue { cardCategories.isNotEmpty() and (cardCategories.size == 1) }
            expect(categoryFuture) { cardCategories[0].toDTO(service) }

            assertTrue { service.getCurrentCashbackCategory(card, categoryFuture.name) == null }

            service.removeCashback(card.name, categoryFuture.period, categoryFuture.name)

            cardCategories = service.getCashbackCategories(card)

            assertTrue { cardCategories.isEmpty() }
            assertTrue { service.getCurrentCashbackCategory(card, categoryFuture.name) == null }
        }
    }

    @Test
    fun `remove unknown cashback category`() {
        transaction(database) {
            service.addBank(bank)
            service.addCard(card)
            service.addCashback(category)
            assertFails { service.removeCashback(card.name, category.period, "unknown") }
        }
    }

    @Test
    fun `test current cashback category uniqueness`() {
        transaction(database) {
            service.addBank(bank)
            service.addCard(card)
            service.addCashback(category)
            assertFails { service.addCashback(category) }
        }
    }

    private fun setup() {
        transaction(database) {
            service.addBank(bank)
            service.addBank(bank2)
            service.addBank(bank3)
            service.addBank(bank4)

            service.addCard(card)
            service.addCard(card2)
            service.addCard(card3)
            service.addCard(card4)
            service.addCard(card5)

            service.addCashback(category)
            service.addCashback(category2)
            service.addCashback(category3)
            service.addCashback(category4)
            service.addCashback(category5)
            service.addCashback(category6)
            service.addCashback(category7)
            service.addCashback(category11)
        }
    }

    @Test
    fun `get card list`() {
        setup()

        transaction(database) {
            val a = service.listCards()
            assertTrue { a.size == 3 }
        }
    }

    @Test
    fun `test transaction`() {
        setup()

        transaction(database) {
            val value = 1000.0

            service.transaction(card.name, category.name, value)

            val remainingLimit = service.getCurrentRemainingLimit(bank.name)

            val calculated = bank.limit!! - value * (category.percent / 100)

            assertTrue { remainingLimit == calculated }
        }
    }

    @Test
    fun `test transaction with unknown card`() {
        setup()

        transaction(database) {
            assertFails { service.transaction("unknown", category.name, 1000.0) }
        }
    }

    @Test
    fun `test transaction with unknown category`() {
        setup()

        transaction(database) {
            assertFails { service.transaction(card.name, "unknown", 1000.0) }
        }
    }

    @Test
    fun `test choose card`() {
        setup()

        transaction(database) {
            val actualCard = service.choose(category.name, 1000.0)
            expect(card) { actualCard!!.toDTO() }

            val actualCard2 = service.choose(category4.name, 1000.0)
            expect(card2) { actualCard2!!.toDTO() }
        }
    }

    @Test
    fun `test choose card unknown category`() {
        setup()

        transaction(database) {
            assertNull(service.choose("unknown", 1000.0))
        }
    }

    @Test
    fun `test estimate cashback`() {
        setup()

        transaction(database) {
            val a = service.estimateCashback()

            assertTrue { a.size == 3 }

            assertTrue { a[0].first.toDTO() == card }
            assertTrue { a[0].second == bank.limit }

            assertTrue { a[1].first.toDTO() == card2 }
            assertTrue { a[1].second == bank2.limit }

            assertTrue { a[2].first.toDTO() == card3 }
            assertTrue { a[2].second == bank3.limit }

            service.transaction(card.name, category.name, 1000.0)
            service.transaction(card2.name, category4.name, 1000.0)
            service.transaction(card3.name, category6.name, 1000.0)

            val b = service.estimateCashback()

            assertTrue { b.size == 3 }

            assertTrue { b[0].first.toDTO() == card }
            assertTrue { b[0].second == bank.limit!! - 1000.0 * (category.percent / 100) }

            assertTrue { b[1].first.toDTO() == card2 }
            assertTrue { b[1].second == bank2.limit!! - 1000.0 * (category4.percent / 100) }

            assertTrue { b[2].first.toDTO() == card3 }
            assertTrue { b[2].second == bank3.limit!! - 1000.0 * (category6.percent / 100) }
        }
    }

    @Test
    fun `test future cashback category in next month`() {
        transaction(database) {
            future_service.addBank(bank)
            future_service.addCard(card)
            future_service.addCashback(categoryFuture)

            monthProvider.advanceMonth()

            val card = findCard(card.name)

            val cardCategories = future_service.getCurrentCashbackCategory(card, categoryFuture.name)

            assertTrue { cardCategories != null }
        }
    }
}
