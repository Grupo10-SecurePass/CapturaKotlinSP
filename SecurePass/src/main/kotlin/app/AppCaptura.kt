package app

import Captura.Captura
import java.net.InetAddress
import SecurePass.SecurePass
import java.time.LocalDateTime

open class Main {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val networkData = SecurePass()
            networkData.configurar()

            val nomeDispositivo = InetAddress.getLocalHost().hostName
            println("Hostname: $nomeDispositivo")

            val dispositivoData = networkData.buscarFkNRAndIdDispositivo(nomeDispositivo)

            if (dispositivoData == null) {
                println("Dispositivo não encontrado ou inativo: $nomeDispositivo")
                return
            }

            val (fkLinha, fkDispositivo) = dispositivoData

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

                // Inserindo dados de Rede Recebida
                val novoRegistroRecebidos = Captura()
                novoRegistroRecebidos.setRegistro(recebidos.toFloat())

                val idCapturaRecebidos = networkData.inserir(novoRegistroRecebidos, fkDispositivo, fkLinha, "RedeRecebida")

                if (idCapturaRecebidos != null) {
                    println("Registro de recebidos inserido com sucesso.")

                    val limitesRecebidos = networkData.buscarLimites(fkComponenteRecebidos, fkDispositivo, fkLinha)
                    for ((tipo, limite) in limitesRecebidos) {
                        if ((tipo == "acima" && recebidos > limite) || (tipo == "abaixo" && recebidos < limite)) {
                            val descricaoAlerta = "Alerta: RedeRecebida $tipo do limite ($limite): Valor registrado: %.2f MB".format(recebidos)
                            if (networkData.inserirAlerta(descricaoAlerta, idCapturaRecebidos, fkLinha, fkComponenteRecebidos, fkDispositivo)) {
                                println("Alerta inserido: $descricaoAlerta")
                            } else {
                                println("Falha ao inserir alerta para recebidos.")
                            }
                        }
                    }
                } else {
                    println("Falha ao inserir registro de recebidos.")
                }

                // Inserindo dados de Rede Enviada
                val novoRegistroEnviados = Captura()
                novoRegistroEnviados.setRegistro(enviados.toFloat())

                val idCapturaEnviados = networkData.inserir(novoRegistroEnviados, fkDispositivo, fkLinha, "RedeEnviada")

                if (idCapturaEnviados != null) {
                    println("Registro de enviados inserido com sucesso.")

                    val limitesEnviados = networkData.buscarLimites(fkComponenteEnviados, fkDispositivo, fkLinha)
                    for ((tipo, limite) in limitesEnviados) {
                        if ((tipo == "acima" && enviados > limite) || (tipo == "abaixo" && enviados < limite)) {
                            val descricaoAlerta = "Alerta: RedeEnviada $tipo do limite ($limite): Valor registrado: %.2f MB".format(enviados)
                            if (networkData.inserirAlerta(descricaoAlerta, idCapturaEnviados, fkLinha, fkComponenteEnviados, fkDispositivo)) {
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
                    println("| ${captura.idCaptura.toString().padEnd(8)} | ${captura.dataRegistro.toString().padEnd(18)} | ${captura.registro.toString().padEnd(12)} MB | Linha: ${captura.fkLinha}")
                }
                println("-------------------------------------------------\n")

                // Listar todos os alertas na tabela alerta
                println("--- Listagem de Alertas da Tabela Alerta ---")
                println("-------------------------------------------------------------------------------------------------------------------")
                println("| ID Alerta | Captura ID | Data                | Descrição                                                        |")
                println("-------------------------------------------------------------------------------------------------------------------")
                val alertas = networkData.listarAlertas()
                alertas.forEach { alerta ->
                    println("| ${alerta.idAlerta.toString().padEnd(10)} | ${alerta.fkCaptura.toString().padEnd(10)} | ${alerta.dataAlerta.toString().padEnd(18)} | ${alerta.descricao.toString().padEnd(30)} | Linha: ${alerta.fkLinha}")
                }
                println("-------------------------------------------------------------------------------------------------------------------\n")


                Thread.sleep(30000)
            }
        }
    }
}
