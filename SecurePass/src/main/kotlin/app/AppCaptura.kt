package app

import Captura.Captura
import java.net.InetAddress
import SecurePass.SecurePass

open class Main {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val networkData = SecurePass()
            networkData.configurar()

            val nomeDispositivo = InetAddress.getLocalHost().hostName
            println("Hostname: $nomeDispositivo")

            val dispositivoData = networkData.buscarFkNRIdDispositivoELinha(nomeDispositivo)

            if (dispositivoData == null) {
                println("Dispositivo não encontrado ou inativo: $nomeDispositivo")
                return
            }

            val (fkDispositivo, idDispositivo) = dispositivoData

            val fkComponenteRecebidos = networkData.buscarFkComponente("RedeRecebida")
            val fkComponenteEnviados = networkData.buscarFkComponente("RedeEnviada")

            if (fkComponenteRecebidos == null || fkComponenteEnviados == null) {
                println("Não foi possível encontrar os componentes de RedeRecebida ou RedeEnviada.")
                return
            }

            networkData.python()

            while (true) {
                val (recebidos, enviados) = networkData.getFormattedNetworkData()

                println("Mega Bytes Recebidos: %.2f MB".format(recebidos))
                println("Mega Bytes Enviados: %.2f MB".format(enviados))

                // Inserção e análise para "RedeRecebida"
                val novoRegistroRecebidos = Captura()
                novoRegistroRecebidos.setRegistro(recebidos.toFloat())

                val idCapturaRecebidos = networkData.inserir(novoRegistroRecebidos, fkDispositivo, idDispositivo, "RedeRecebida")

                if (idCapturaRecebidos != null) {
                    println("Registro de recebidos inserido com sucesso.")

                    val limitesRecebidos = networkData.buscarLimites(fkComponenteRecebidos, fkDispositivo, idDispositivo)
                    for ((tipo, limite) in limitesRecebidos) {
                        if ((tipo == "acima" && recebidos > limite) || (tipo == "abaixo" && recebidos < limite)) {
                            val descricaoAlerta = "Alerta: RedeRecebida $tipo do limite ($limite): Valor registrado: %.2f MB".format(recebidos)
                            if (networkData.inserirAlerta(descricaoAlerta, idCapturaRecebidos, idDispositivo, fkDispositivo, fkComponenteRecebidos)) {
                                println("Alerta inserido: $descricaoAlerta")
                            } else {
                                println("Falha ao inserir alerta para recebidos.")
                            }
                        }
                    }
                } else {
                    println("Falha ao inserir registro de recebidos.")
                }

                // Inserção e análise para "RedeEnviada"
                val novoRegistroEnviados = Captura()
                novoRegistroEnviados.setRegistro(enviados.toFloat())

                val idCapturaEnviados = networkData.inserir(novoRegistroEnviados, fkDispositivo, idDispositivo, "RedeEnviada")

                if (idCapturaEnviados != null) {
                    println("Registro de enviados inserido com sucesso.")

                    val limitesEnviados = networkData.buscarLimites(fkComponenteEnviados, fkDispositivo, idDispositivo)
                    for ((tipo, limite) in limitesEnviados) {
                        if ((tipo == "acima" && enviados > limite) || (tipo == "abaixo" && enviados < limite)) {
                            val descricaoAlerta = "Alerta: RedeEnviada $tipo do limite ($limite): Valor registrado: %.2f MB".format(enviados)
                            if (networkData.inserirAlerta(descricaoAlerta, idCapturaEnviados, idDispositivo, fkDispositivo, fkComponenteRecebidos)) {
                                println("Alerta inserido: $descricaoAlerta")
                            } else {
                                println("Falha ao inserir alerta para enviados.")
                            }
                        }
                    }
                } else {
                    println("Falha ao inserir registro de enviados.")
                }

                // Listar todos os registros na tabela captura
                println("\n--- Listagem de Registros da Tabela Captura ---")
                println("-------------------------------------------------")
                println("| ID       | Data                | Registro      |")
                println("-------------------------------------------------")
                val capturas = networkData.listarCapturas()
                capturas.forEach { captura ->
                    println("| ${captura.idCaptura.toString().padEnd(8)} | ${captura.dataRegistro.toString().padEnd(18)} | ${captura.registro.toString().padEnd(12)} MB |")
                }
                println("-------------------------------------------------\n")

                // Listar todos os alertas na tabela alerta
                println("--- Listagem de Alertas da Tabela Alerta ---")
                println("-------------------------------------------------------------------------------------------------------------------")
                println("| ID Alerta | Captura ID | Data                | Descricao                                                        |")
                println("-------------------------------------------------------------------------------------------------------------------")
                val alertas = networkData.listarAlertas()
                alertas.forEach { alerta ->
                    println("| ${alerta.idAlerta.toString().padEnd(10)} | ${alerta.fkCaptura.toString().padEnd(10)} | ${alerta.dataAlerta.toString().padEnd(18)} | ${alerta.descricao.toString().padEnd(30)} |")
                }
                println("-------------------------------------------------------------------------------------------------------------------\n")

                Thread.sleep(60000)
            }

        }
    }
}