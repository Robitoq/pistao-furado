package com.ferrovelho.game

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private data class BusDestination(val name: String, val detail: String, val x: Float)

@Composable
fun BusStopDialog(g: GameState, onClose: () -> Unit, onTravel: (Float) -> Unit) {
    val destinations = listOf(
        BusDestination("Centro da cidade", "Comércio, lojas e serviços", 1700f),
        BusDestination("Casa", "Voltar para casa", 260f),
        BusDestination("Oficina", "Oficina do Tião", GARAGE_X),
        BusDestination("Banco Pistão", "Resolver assuntos bancários", BANK_X),
        BusDestination("Parque Central", "Fonte, lago e área verde", PARK_X),
        BusDestination("Praia", "Quiosques e faixa de areia", BEACH_X),
        BusDestination("Bairro novo", "Lojas, empresas e imóveis", 10800f),
    )
    val open = g.minutes in (5 * 60) until (23 * 60)
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text("🚌 Terminal de ônibus") },
        text = {
            Column(
                Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    if (open) "Linha municipal • Passagem R$ 5,00 • Viagem: 15 minutos"
                    else "O transporte funciona das 5h às 23h.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                destinations.forEach { destination ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(Modifier.weight(1f).padding(end = 8.dp)) {
                            Text(destination.name, fontSize = 14.sp)
                            Text(destination.detail, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Button(
                            onClick = { onTravel(destination.x) },
                            enabled = open && g.money >= 5
                        ) { Text("R$ 5") }
                    }
                }
                if (!open) Text("Volte entre 5h e 23h para pegar o ônibus.", fontSize = 12.sp)
                else if (g.money < 5) Text("Você precisa de R$ 5,00 para embarcar.", fontSize = 12.sp)
            }
        },
        confirmButton = { TextButton(onClick = onClose) { Text("Fechar") } }
    )
}
