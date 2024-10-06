import com.github.britooo.looca.api.core.Looca
import org.apache.commons.dbcp2.BasicDataSource
import org.springframework.jdbc.core.BeanPropertyRowMapper
import org.springframework.jdbc.core.JdbcTemplate
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import org.springframework.dao.EmptyResultDataAccessException

class SecurePass {

    private val looca = Looca()
    lateinit var jdbcTemplate: JdbcTemplate

    fun configurar() {
        val datasource = BasicDataSource()
        datasource.url = "jdbc:mysql://localhost:3306/securepass?serverTimezone=America/Sao_Paulo"
        datasource.username = "root"
        datasource.password = "#Gf48500284897"

        jdbcTemplate = JdbcTemplate(datasource)
    }

    private fun buscarFkDispositivo(fkNR: Int): Int? {
        val sql = "SELECT idDispositivo FROM dispositivo WHERE fkNR = ? AND stats = 'ativo'"
        return jdbcTemplate.queryForObject(sql, Int::class.java, fkNR)
    }

    private fun buscarFkComponente(nome: String): Int? {
        val sql = "SELECT idComponente FROM componente WHERE nome = ?"
        return try {
            jdbcTemplate.queryForObject(sql, Int::class.java, nome)
        } catch (e: EmptyResultDataAccessException) {
            null
        }
    }

    fun getFormattedNetworkData(): Pair<Double, Double> {
        val bytesRecebidos = looca.rede.grupoDeInterfaces.interfaces[0].bytesRecebidos
        val bytesEnviados = looca.rede.grupoDeInterfaces.interfaces[0].bytesEnviados

        val megabytesRecebidos = bytesRecebidos / (1024.0 * 1024.0)
        val megabytesEnviados = bytesEnviados / (1024.0 * 1024.0)

        return Pair(megabytesRecebidos, megabytesEnviados)
    }

    fun inserir(novoRegistro: Captura, fkNR: Int, fkComponente: String): Int? {
        val fkDispositivo = buscarFkDispositivo(fkNR)
        val fkComponenteId = buscarFkComponente(fkComponente)

        if (fkDispositivo == null) {
            println("Dispositivo não encontrado para o fkNR: $fkNR.")
            return null
        }

        if (fkComponenteId == null) {
            println("Componente não encontrado para o nome: $fkComponente.")
            return null
        }

        val formattedRegistro = BigDecimal(novoRegistro.registro.toDouble())
            .setScale(4, RoundingMode.HALF_UP)

        val formattedDataRegistro = novoRegistro.dataRegistro.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))

        val qtdLinhasAfetadas = jdbcTemplate.update(
            """
        INSERT INTO captura (fkDispositivo, fkNR, fkComponente, registro, dataRegistro)
        VALUES (?, ?, ?, ?, ?)
        """,
            fkDispositivo, fkNR, fkComponenteId, formattedRegistro, formattedDataRegistro
        )

        return if (qtdLinhasAfetadas > 0) {
            jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Int::class.java)
        } else {
            null
        }
    }

    fun inserirAlerta(descricao: String, idCaptura: Int, fkNR: Int): Boolean {
        val formattedDataAlerta = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))

        val fkCaptura = buscarFkCaptura(idCaptura)

        if (fkCaptura == null) {
            println("Não foi possível encontrar a FK para o alerta.")
            return false
        }

        val qtdLinhasAfetadas = jdbcTemplate.update(
            """
            INSERT INTO alerta (fkCaptura, fkNR, dataAlerta, descricao)
            VALUES (?, ?, ?, ?)
            """,
            fkCaptura, fkNR, formattedDataAlerta, descricao
        )
        return qtdLinhasAfetadas > 0
    }

    private fun buscarFkCaptura(idCaptura: Int): Int? {
        val sql = "SELECT idCaptura FROM captura WHERE idCaptura = ?"
        return jdbcTemplate.queryForObject(sql, Int::class.java, idCaptura)
    }

    fun listarCapturas(): List<Captura> {
        return jdbcTemplate.query(
            "SELECT * FROM captura",
            BeanPropertyRowMapper(Captura::class.java)
        )
    }

    fun listarAlertas(): List<Alerta> {
        return jdbcTemplate.query(
            "SELECT * FROM alerta",
            BeanPropertyRowMapper(Alerta::class.java)
        )
    }
}