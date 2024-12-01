package SecurePass

import Captura.Alerta
import Captura.Captura
import com.github.britooo.looca.api.core.Looca
import com.google.common.graph.Network
import org.apache.commons.dbcp2.BasicDataSource
import org.springframework.jdbc.core.BeanPropertyRowMapper
import org.springframework.jdbc.core.JdbcTemplate
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

data class DispositivoData(val fkLinha: Int, val idDispositivo: Int)
data class NetworkData(
    val megabytesRecebidos: Double,
    val megabytesEnviados: Double,
    val pacotesRecebidos: Long,
    val pacotesEnviados: Long
)

class SecurePass {

    private val looca = Looca()
    lateinit var jdbcTemplate: JdbcTemplate

    // Configuração do banco de dados
    fun configurar() {
        val datasource = BasicDataSource()
        datasource.driverClassName = "com.mysql.cj.jdbc.Driver"
        datasource.url = "jdbc:mysql://3.81.255.141:3306/securepass?serverTimezone=America/Sao_Paulo"
        datasource.username = "root"
        datasource.password = "urubu100"

        jdbcTemplate = JdbcTemplate(datasource)
    }

    // Busca ID de linha e dispositivo com base no nome do dispositivo
    fun buscarFkNRAndIdDispositivo(nomeDispositivo: String): DispositivoData? {
        val sql = """
            SELECT l.idLinha, d.idDispositivo 
            FROM linha l 
            JOIN dispositivo d ON d.fkLinha = l.idLinha 
            WHERE d.nome = ? 
              AND d.status = 1;
        """
        return jdbcTemplate.query(
            sql,
            arrayOf(nomeDispositivo)
        ) { rs, _ -> DispositivoData(rs.getInt("idLinha"), rs.getInt("idDispositivo")) }
            .firstOrNull()
    }

    // Método para executar um script Python
    fun python() {
        try {
            Runtime.getRuntime().exec("python3 CapturaPythonSP/main.py")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Busca o ID do componente pelo nome
    fun buscarFkComponente(nome: String): Int? {
        val sql = "SELECT idComponente FROM componente WHERE nome = ?"
        return jdbcTemplate.query(
            sql,
            arrayOf(nome)
        ) { rs, _ -> rs.getInt("idComponente") }
            .firstOrNull()
    }

    // Retorna os dados de rede formatados em megabytes
    fun getFormattedNetworkData():  NetworkData {
        val bytesRecebidos = looca.rede.grupoDeInterfaces.interfaces[0].bytesRecebidos
        val bytesEnviados = looca.rede.grupoDeInterfaces.interfaces[0].bytesEnviados
        val pacotesEnviados = looca.rede.grupoDeInterfaces.interfaces[0].pacotesEnviados
        val pacotesRecebidos = looca.rede.grupoDeInterfaces.interfaces[0].pacotesRecebidos

        val megabytesRecebidos = bytesRecebidos / (1024.0 * 1024.0)
        val megabytesEnviados = bytesEnviados / (1024.0 * 1024.0)


        return NetworkData(megabytesRecebidos, megabytesEnviados, pacotesRecebidos, pacotesEnviados)
    }

    // Inserção de registro na tabela `captura`
    fun inserir(novoRegistro: Captura, idDispositivo: Int, fkLinha: Int, fkComponente: String): Int? {
        val fkComponenteId = buscarFkComponente(fkComponente) ?: run {
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

    // Busca limites associados a um componente, dispositivo e linha
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

    // Inserção de alerta na tabela `alerta`
    fun inserirAlerta(descricao: String, idCaptura: Int, fkLinha: Int, fkComponente: Int, fkDispositivo: Int): Boolean {
        val formattedDataAlerta = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))

        val qtdLinhasAfetadas = jdbcTemplate.update(
            """
            INSERT INTO alerta (descricao, dataAlerta, visualizacao, fkComponente, fkDispositivo, fkLinha, fkCaptura)
            VALUES (?, ?, 0, ?, ?, ?, ?)
            """,
            descricao, formattedDataAlerta, fkComponente, fkDispositivo, fkLinha, idCaptura
        )
        return qtdLinhasAfetadas > 0
    }

    fun listarCapturas(): List<Captura> {
        val sql = "SELECT * \n" +
                "FROM captura \n" +
                "WHERE dataRegistro >= DATE_SUB(NOW(), INTERVAL 1 MINUTE) \n" +
                "ORDER BY dataRegistro DESC;\n"
        return jdbcTemplate.query(sql, BeanPropertyRowMapper(Captura::class.java))
    }

    fun listarAlertas(idDispositivo: Int): List<Alerta> {
        val sql = """
       SELECT 
    idAlerta, 
    fkCaptura, 
    dataAlerta, 
    descricao, 
    fkLinha, 
    fkComponente, 
    fkDispositivo, 
    visualizacao 
FROM 
    alerta 
WHERE 
    fkDispositivo = ? 
    AND visualizacao = 0
    AND dataAlerta >= DATE_SUB(NOW(), INTERVAL 1 MINUTE)
ORDER BY 
    dataAlerta DESC;

    """
        return jdbcTemplate.query(
            sql,
            arrayOf(idDispositivo)
        ) { rs, _ ->
            Alerta().apply {
                idAlerta = rs.getInt("idAlerta")
                fkCaptura = rs.getInt("fkCaptura")
                fkLinha = rs.getInt("fkLinha")
                fkComponente = rs.getInt("fkComponente")
                fkDispositivo = rs.getInt("fkDispositivo")
                dataAlerta = rs.getTimestamp("dataAlerta").toLocalDateTime()
                descricao = rs.getString("descricao")
                visualizacao = rs.getBoolean("visualizacao")
            }
        }
    }

}
