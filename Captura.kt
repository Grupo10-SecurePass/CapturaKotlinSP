import java.time.LocalDateTime

class Captura {
    var idCaptura: Int = 0
    var dataRegistro: LocalDateTime = LocalDateTime.now()
    var registro: Float = 0.0f
        private set

    fun setRegistro(novoValor: Float) {
        if (novoValor > 0) {
            registro = novoValor
        } else {
            println("Registro inválido! Deve ser registrado um valor maior que 0")
        }
    }
}

class Alerta {
    var idAlerta: Int = 0
    var fkCaptura: Int = 0
    var fkNR: Int = 0
    var dataAlerta: LocalDateTime = LocalDateTime.now()
    var descricao: String = ""
}