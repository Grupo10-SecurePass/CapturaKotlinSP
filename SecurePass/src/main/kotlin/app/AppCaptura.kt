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

            val (fkNR, fkDispositivo) = dispositivoData

            val fkComponenteRecebidos = networkData.buscarFkComponente("RedeRecebida")
            val fkComponenteEnviados = networkData.buscarFkComponente("RedeEnviada")

            if (fkComponenteRecebidos == null || fkComponenteEnviados == null) {
                println("Não foi possível encontrar os componentes de RedeRecebida ou RedeEnviada.")
                return
            }

            while (true) {
                val (recebidos, enviados) = networkData.getFormattedNetworkData()

                println("Mega Bytes Recebidos: %.2f MB".format(recebidos))
                println("Mega Bytes Enviados: %.2f MB".format(enviados))


                val novoRegistroRecebidos = Captura()
                novoRegistroRecebidos.setRegistro(recebidos.toFloat())

                val idCapturaRecebidos = networkData.inserir(novoRegistroRecebidos, fkDispositivo, fkNR, "RedeRecebida")

                if (idCapturaRecebidos != null) {
                    println("Registro de recebidos inserido com sucesso.")

                    if (recebidos < 1000.0) {
                        val descricaoAlerta = "Alerta: Registro de Download de rede abaixo do esperado: %.2f MB".format(recebidos)
                        if (networkData.inserirAlerta(descricaoAlerta, idCapturaRecebidos, fkNR)) {
                            println("Alerta inserido: $descricaoAlerta")
                        } else {
                            println("Falha ao inserir alerta para recebidos.")
                        }
                    }
                } else {
                    println("Falha ao inserir registro de recebidos.")
                }


                val novoRegistroEnviados = Captura()
                novoRegistroEnviados.setRegistro(enviados.toFloat())

                val idCapturaEnviados = networkData.inserir(novoRegistroEnviados, fkDispositivo, fkNR, "RedeEnviada")

                if (idCapturaEnviados != null) {
                    println("Registro de enviados inserido com sucesso.")

                    if (enviados < 1000.0) {
                        val descricaoAlerta = "Alerta: Registro de Upload de rede abaixo do esperado: %.2f MB".format(enviados)
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
                println("| ID Alerta | Captura ID | Data                | Descrição                                                        |")
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