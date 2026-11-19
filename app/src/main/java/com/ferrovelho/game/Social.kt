package com.ferrovelho.game

import android.content.SharedPreferences
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlin.math.min
import kotlin.random.Random

/** Amizades: nome -> nível (0..5). Nível 3+ = amigo de verdade. Salvo nas preferências. */
const val FRIEND_LVL = 3

val NPC_NAMES = listOf("Rafa", "Camila", "Seu Jorge", "Dani", "Léo", "Bia", "Tião Jr", "Marcos", "Nanda", "Paulinho", "Vera", "Gabi", "Beto", "Lu", "Cadu", "Sônia")

fun loadFriends(p: SharedPreferences): Map<String, Int> =
    (p.getString("friends", "") ?: "").split(";").mapNotNull {
        val a = it.split(":"); if (a.size == 2) a[0] to (a[1].toIntOrNull() ?: 0) else null
    }.toMap()

fun saveFriends(p: SharedPreferences, m: Map<String, Int>) {
    p.edit().putString("friends", m.entries.joinToString(";") { "${it.key}:${it.value}" }).apply()
}

private val TALK = listOf(
    "Esse calor tá de matar, hein?", "Cê viu o preço dos carros usados? Tá uma loucura.",
    "Meu primo comprou um Gol e o motor fundiu no mesmo dia!", "Dizem que o ferro-velho tá chegando carro bom essa semana.",
    "Aqui é o melhor lugar da cidade pra espairecer.", "Se for comprar carro, sempre olha o chassi primeiro.",
)
private val TIPS = listOf(
    "Carro com pneu careca vende barato. Troca e revende que dá lucro.", "Aerofólio e turbo sobem bem o valor, mas olha o custo antes.",
    "Cliente apressado paga mais. Segura o preço!", "Pintura nova no carro velho engana muito freguês.",
    "Dia de chuva tem menos cliente na loja, aproveita pra mexer na oficina.",
)

@Composable
fun FriendChat(g: GameState, name: String, level: Int, me: String, onLevel: (Int) -> Unit, onClose: () -> Unit) {
    var line by remember { mutableStateOf("Oi, $me! Tudo bem?") }
    val friend = level >= FRIEND_LVL
    fun say(t: String) { line = t; Voice.speak(t, 1.0f) }
    Dialog(onDismissRequest = onClose) {
        Card {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text((if (friend) "" else "") + name, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Text("Amizade: " + "".repeat(level.coerceIn(0, 5)) + "".repeat(5 - level.coerceIn(0, 5)), fontSize = 12.sp)
                Text("“$line”", fontSize = 14.sp)
                if (g.message.isNotEmpty()) Text(g.message, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                OutlinedButton(onClick = {
                    g.spendTime(10); say(TALK.random())
                    val n = min(5, level + 1); onLevel(n)
                    g.message = if (n >= FRIEND_LVL && !friend) "Vocês agora são amigos!" else "Vocês conversaram um pouco."
                }, Modifier.fillMaxWidth()) { Text("Conversar (10 min)") }
                OutlinedButton(onClick = {
                    g.money -= 10; g.spendTime(15); g.thirst = min(100.0, g.thirst + 20.0)
                    val n = min(5, level + 2); onLevel(n); say("Valeu pelo refri, $me! Você é gente boa.")
                    g.message = if (n >= FRIEND_LVL && !friend) "Vocês agora são amigos!" else "Você pagou um refri"
                }, Modifier.fillMaxWidth(), enabled = g.money >= 10) { Text("Pagar um refri · ${brl(10)}") }
                if (friend) OutlinedButton(onClick = { say(TIPS.random()); g.message = "" }, Modifier.fillMaxWidth()) { Text("Pedir uma dica de negócio") }
                TextButton(onClick = onClose) { Text("Tchau") }
            }
        }
    }
}
