fun main() {
    val networkData = SecurePass()

    networkData.configurar()

    val fkDispositivo = 1
    val fkNR = 4826
    val fkComponenteRecebidos = "RedeRecebida"
    val fkComponenteEnviados = "RedeEnviada"

    while (true) {
        val (recebidos, enviados) = networkData.getFormattedNetworkData()

        println("Mega Bytes Recebidos: %.2f MB".format(recebidos))
        println("Mega Bytes Enviados: %.2f MB".format(enviados))

        // Registro de recebidos
        val novoRegistroRecebidos = Captura()
        novoRegistroRecebidos.setRegistro(recebidos.toFloat())

        val idCapturaRecebidos = networkData.inserir(novoRegistroRecebidos, fkNR, fkComponenteRecebidos)

        if (idCapturaRecebidos != null) {
            println("Registro de recebidos inserido com sucesso.")

            // Inserção de alerta se necessário
            if (recebidos < 1000.0) {
                val descricaoAlerta = "Alerta: Registro de Download de rede abaixo do esperado: ${recebidos} MB"
                if (networkData.inserirAlerta(descricaoAlerta, idCapturaRecebidos, fkNR)) {
                    println("Alerta inserido: $descricaoAlerta")
                } else {
                    println("Falha ao inserir alerta para recebidos.")
                }
            }
        } else {
            println("Falha ao inserir registro de recebidos.")
        }

        // Registro de enviados
        val novoRegistroEnviados = Captura()
        novoRegistroEnviados.setRegistro(enviados.toFloat())

        val idCapturaEnviados = networkData.inserir(novoRegistroEnviados, fkNR, fkComponenteEnviados)

        if (idCapturaEnviados != null) {
            println("Registro de enviados inserido com sucesso.")

            // Inserção de alerta se necessário
            if (enviados < 1000.0) {
                val descricaoAlerta = "Alerta: Registro de Upload de rede abaixo do esperado: ${enviados} MB"
                if (networkData.inserirAlerta(descricaoAlerta, idCapturaEnviados, fkNR)) {
                    println("Alerta inserido: $descricaoAlerta")
                } else {
                    println("Falha ao inserir alerta para enviados.")
                }
            }
        } else {
            println("Falha ao inserir registro de enviados.")
        }

        // Listar todos os registros na tabela captura
        println("\n--- Listagem de Registros da Tabela Captura ---")
        val capturas = networkData.listarCapturas()
        capturas.forEach { captura ->
            println("ID: ${captura.idCaptura}, Data: ${captura.dataRegistro}, Registro: ${captura.registro}")
        }

        // Listar todos os alertas na tabela alerta
        println("\n--- Listagem de Alertas da Tabela Alerta ---")
        val alertas = networkData.listarAlertas()
        alertas.forEach { alerta ->
            println("ID: ${alerta.idAlerta}, Captura ID: ${alerta.fkCaptura}, Data: ${alerta.dataAlerta}, Descrição: ${alerta.descricao}")
        }

        Thread.sleep(60000)
    }
}