import org.example.addBank
import org.example.addCard
import org.example.addCashback
import org.example.domain.*
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.postgresql.ds.PGSimpleDataSource
import kotlin.test.assertFails
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

    private val card = CardDTO("МИР", "Тинькофф")
    private val card2 = CardDTO("Банк Санкт-Петербург", "Банк Санкт-Петербург")
    private val card3 = CardDTO("Альфа Кредитка", "Альфа")
    private val card4 = CardDTO("Альфа Мир", "Альфа")

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

    @BeforeEach
    fun resetDB() {
        transaction(database) {
            SchemaUtils.drop(CashbackCategories)
            SchemaUtils.drop(Cards)
            SchemaUtils.drop(Banks)

            SchemaUtils.create(Banks)
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
        assertFails {
            transaction(database) { addBank(bank) }
        }
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
        assertFails {
            transaction(database) {
                addCard(card)
            }
        }
    }

    @Test
    fun `test card addition with non-existent bank`() {
        assertFails {
            transaction(database) {
                addCard(card)
            }
        }
    }

    @Test
    fun `test cashback category addition`() {
        transaction(database) {
            addBank(bank)

            val card = addCard(card)
            val category = addCashback(category)

            val cardCategories = card.getCashbackCategories()

            assertTrue { cardCategories.size == 1 }
            assertTrue { cardCategories[0].name == category.name }
            assertTrue { cardCategories[0].percent == category.percent }
            assertTrue { cardCategories[0].permanent == category.permanent }
        }
    }
}
