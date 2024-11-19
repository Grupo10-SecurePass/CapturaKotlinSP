package SecurePass

import Captura.Alerta
import Captura.Captura
import com.github.britooo.looca.api.core.Looca
import org.apache.commons.dbcp2.BasicDataSource
import org.springframework.jdbc.core.BeanPropertyRowMapper
import org.springframework.jdbc.core.JdbcTemplate
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import org.springframework.dao.EmptyResultDataAccessException

data class DispositivoData(val fkLinha: Int, val idDispositivo: Int)

data class Alerta(
    val idAlerta: Int,
    val fkCaptura: Int,
    val dataAlerta: LocalDateTime,
    val descricao: String,
    val fkLinha: Int
)

class SecurePass {

    private val looca = Looca()
    lateinit var jdbcTemplate: JdbcTemplate

    fun configurar() {
        val datasource = BasicDataSource()
        datasource.driverClassName = "com.mysql.cj.jdbc.Driver"
        datasource.url = "jdbc:mysql://localhost:3306/securepass?serverTimezone=America/Sao_Paulo"
        datasource.username = "root"
        datasource.password = "#Gf48500284897"

        jdbcTemplate = JdbcTemplate(datasource)
    }

    fun buscarFkNRAndIdDispositivo(nomeDispositivo: String): DispositivoData? {
        val sql = """
        SELECT 
        l.idLinha, d.idDispositivo 
        FROM linha l 
        JOIN dispositivo d ON d.fkLinha = l.idLinha 
        WHERE d.nome = ? 
        AND d.status = 1;
    """
        val resultados = jdbcTemplate.query(
            sql,
            arrayOf(nomeDispositivo)
        ) { rs, _ -> DispositivoData(rs.getInt("idLinha"), rs.getInt("idDispositivo")) }

        return resultados.firstOrNull()
    }

    fun python(){
        try {
            val process = Runtime.getRuntime().exec("python3 CapturaPythonSP/main.py")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun buscarFkComponente(nome: String): Int? {
        val sql = "SELECT idComponente FROM componente WHERE nome = ?"
        val resultados = jdbcTemplate.query(
            sql,
            arrayOf(nome)
        ) { rs, _ -> rs.getInt("idComponente") }

        return resultados.firstOrNull()
    }

    fun getFormattedNetworkData(): Pair<Double, Double> {
        val bytesRecebidos = looca.rede.grupoDeInterfaces.interfaces[0].bytesRecebidos
        val bytesEnviados = looca.rede.grupoDeInterfaces.interfaces[0].bytesEnviados

        val megabytesRecebidos = bytesRecebidos / (1024.0 * 1024.0)
        val megabytesEnviados = bytesEnviados / (1024.0 * 1024.0)

        return Pair(megabytesRecebidos, megabytesEnviados)
    }


    fun inserir(novoRegistro: Captura, idDispositivo: Int, fkLinha: Int, fkComponente: String): Int? {
        val fkComponenteId = buscarFkComponente(fkComponente)

        if (fkComponenteId == null) {
            println("Componente não encontrado para o nome: $fkComponente.")
            return null
        }

        val formattedRegistro = BigDecimal(novoRegistro.registro.toDouble())
            .setScale(2, RoundingMode.HALF_UP)

        val formattedDataRegistro = novoRegistro.dataRegistro.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))

        val qtdLinhasAfetadas = jdbcTemplate.update(
            """
        INSERT INTO captura (fkDispositivo, fkLinha, fkComponente, registro, dataRegistro)
        VALUES (?, ?, ?, ?, ?)
        """,
            idDispositivo, fkLinha, fkComponenteId, formattedRegistro, formattedDataRegistro
        )

        return if (qtdLinhasAfetadas > 0) {
            jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Int::class.java)
        } else {
            null
        }
    }

    fun buscarLimites(fkComponente: Int, fkDispositivo: Int, fkLinha: Int): List<Pair<String, Float>> {
        val sql = """
        SELECT tipo, valor 
        FROM limite 
        WHERE fkComponente = ? AND fkDispositivo = ? AND fkLinha = ?
    """
        return jdbcTemplate.query(sql, arrayOf(fkComponente, fkDispositivo, fkLinha)) { rs, _ ->
            Pair(rs.getString("tipo"), rs.getFloat("valor"))
        }
    }

    fun inserirAlerta(descricao: String, idCaptura: Int, fkLinha: Int, fkComponente: Int, fkDispositivo: Int): Boolean {
        val formattedDataAlerta = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))

        val fkCaptura = buscarFkCaptura(idCaptura)

        if (fkCaptura == null) {
            println("Não foi possível encontrar a FK para o alerta.")
            return false
        }

        val qtdLinhasAfetadas = jdbcTemplate.update(
            """
         INSERT INTO alerta (descricao, dataAlerta, visualizacao, fkComponente, fkDispositivo, fkLinha, fkCaptura)
            VALUES (?, ?, 0, ?, ?, ?, ?)
        """,
            descricao, formattedDataAlerta, fkComponente, fkDispositivo, fkLinha, fkCaptura
        )
        return qtdLinhasAfetadas > 0
    }


    fun buscarFkCaptura(idCaptura: Int): Int? {
        val sql = "SELECT idCaptura FROM captura WHERE idCaptura = ?"
        return jdbcTemplate.queryForObject(sql, Int::class.java, idCaptura)
    }

    fun listarCapturas(): List<Captura> {
        return jdbcTemplate.query(
            "SELECT * FROM captura ORDER BY dataRegistro DESC LIMIT 15",
            BeanPropertyRowMapper(Captura::class.java)
        )
    }
    fun listarAlertas(): List<Alerta> {
        return jdbcTemplate.query(
            "SELECT idAlerta, fkCaptura, dataAlerta, descricao, fkLinha FROM alerta ORDER BY dataAlerta DESC LIMIT 15;",
            BeanPropertyRowMapper(Alerta::class.java)
        )
    }

}