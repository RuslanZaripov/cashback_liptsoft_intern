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
    private val bank = BankDTO("Сбербанк", 1000.0)
    private val bank2 = BankDTO("Альфа-Банк", 2000.0)

    private val card = CardDTO("Сбербанк Black", "Сбербанк")
    private val card2 = CardDTO("Сбербанк Platinum", "Сбербанк")

    private val category = CashbackCategoryDTO("Еда", 5, false, "Сбербанк Black")

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
