package com.mylist

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.model.UpdateAvailability
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Composable
fun MyListTheme(content: @Composable () -> Unit) {
    val colorScheme = lightColorScheme(
        primary = Color(0xFF4CAF50),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFE8F5E9),
        onPrimaryContainer = Color(0xFF1B5E20),
        secondary = Color(0xFFFF6B35),
        onSecondary = Color.White,
        secondaryContainer = Color(0xFFFFE5D9),
        onSecondaryContainer = Color(0xFFD84315),
        tertiary = Color(0xFFFFC107),
        onTertiary = Color(0xFF3E2723),
        tertiaryContainer = Color(0xFFFFF9C4),
        onTertiaryContainer = Color(0xFFFF6F00),
        background = Color(0xFFFAFAFA),
        onBackground = Color(0xFF1C1B1F),
        surface = Color.White,
        onSurface = Color(0xFF1C1B1F),
        error = Color(0xFFD32F2F),
        onError = Color.White,
        errorContainer = Color(0xFFFFCDD2),
        onErrorContainer = Color(0xFFB71C1C)
    )

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyListTheme {
                MyListApp()
            }
        }
    }
}

@Serializable
data class Produto(
    val id: Int,
    val nome: String,
    val preco: String,
    val observacao: String = ""
)

@Serializable
data class ItemLista(
    val produto: Produto,
    val quantidade: Int,
    var comprado: Boolean = false
)

@Serializable
data class DadosExportacao(
    val produtos: List<Produto>,
    val lista: List<ItemLista>,
    val proximoId: Int
)

class AppState {
    var produtos = mutableStateListOf<Produto>()
    var minhaLista = mutableStateListOf<ItemLista>()
    var proximoIdProduto = 1
}

fun calcularTotal(lista: List<ItemLista>): Double {
    return lista.sumOf { item ->
        val precoLimpo = item.produto.preco.replace(",", ".").trim()
        val preco = precoLimpo.toDoubleOrNull() ?: 0.0
        preco * item.quantidade
    }
}

fun formatarPreco(texto: String): String {
    val limpo = texto.replace(",", ".").trim()
    val numero = limpo.toDoubleOrNull() ?: return texto
    return "%.2f".format(numero).replace(".", ",")
}

fun carregarProdutosPadrao(): List<Produto> {
    return listOf(
        // Mercearia Básica
        Produto(1, "Arroz branco (5kg)", "25,00", ""),
        Produto(2, "Feijão preto (1kg)", "8,50", ""),
        Produto(3, "Feijão carioca (1kg)", "7,90", ""),
        Produto(4, "Macarrão espaguete", "4,20", ""),
        Produto(5, "Macarrão penne", "4,50", ""),
        Produto(6, "Molho de tomate", "3,50", ""),
        Produto(7, "Óleo de soja (900ml)", "7,80", ""),
        Produto(8, "Azeite de oliva", "18,00", ""),
        Produto(9, "Sal (1kg)", "2,50", ""),
        Produto(10, "Açúcar (1kg)", "4,90", ""),
        Produto(11, "Açúcar mascavo", "6,50", ""),
        Produto(12, "Farinha de trigo (1kg)", "5,20", ""),
        Produto(13, "Farinha de milho", "4,80", ""),
        Produto(14, "Café em pó (500g)", "12,00", ""),
        Produto(15, "Café solúvel", "15,00", ""),
        Produto(16, "Biscoito cream cracker", "4,50", ""),
        Produto(17, "Biscoito maria", "3,90", ""),
        Produto(18, "Biscoito recheado", "5,20", ""),
        Produto(19, "Bolacha água e sal", "4,00", ""),
        Produto(20, "Achocolatado em pó", "8,50", ""),

        // Laticínios
        Produto(21, "Leite integral (1L)", "5,80", ""),
        Produto(22, "Leite desnatado (1L)", "6,00", ""),
        Produto(23, "Iogurte natural", "3,90", ""),
        Produto(24, "Iogurte grego", "7,50", ""),
        Produto(25, "Queijo mussarela", "28,00", "por kg"),
        Produto(26, "Queijo prato", "32,00", "por kg"),
        Produto(27, "Queijo parmesão", "45,00", "por kg"),
        Produto(28, "Manteiga (200g)", "9,50", ""),
        Produto(29, "Margarina (500g)", "6,80", ""),
        Produto(30, "Requeijão (200g)", "7,80", ""),
        Produto(31, "Creme de leite", "3,50", ""),
        Produto(32, "Leite condensado", "6,00", ""),

        // Carnes e Ovos
        Produto(33, "Peito de frango", "16,00", "por kg"),
        Produto(34, "Coxa de frango", "14,00", "por kg"),
        Produto(35, "Sobrecoxa de frango", "13,50", "por kg"),
        Produto(36, "Carne moída", "25,00", "por kg"),
        Produto(37, "Alcatra", "42,00", "por kg"),
        Produto(38, "Picanha", "68,00", "por kg"),
        Produto(39, "Costela bovina", "28,00", "por kg"),
        Produto(40, "Linguiça toscana", "18,00", "por kg"),
        Produto(41, "Linguiça calabresa", "17,00", "por kg"),
        Produto(42, "Ovos (dúzia)", "12,00", ""),
        Produto(43, "Salsicha", "8,50", "pacote"),
        Produto(44, "Presunto", "22,00", "por kg"),
        Produto(45, "Mortadela", "15,00", "por kg"),
        Produto(46, "Bacon", "28,00", "por kg"),

        // Frutas
        Produto(47, "Banana prata", "6,00", "por kg"),
        Produto(48, "Banana nanica", "5,50", "por kg"),
        Produto(49, "Maçã argentina", "8,50", "por kg"),
        Produto(50, "Maçã gala", "7,90", "por kg"),
        Produto(51, "Laranja pera", "4,50", "por kg"),
        Produto(52, "Laranja lima", "5,00", "por kg"),
        Produto(53, "Limão taiti", "6,50", "por kg"),
        Produto(54, "Melancia", "3,00", "por kg"),
        Produto(55, "Melão", "4,50", "por kg"),
        Produto(56, "Mamão papaia", "5,50", "por kg"),
        Produto(57, "Mamão formosa", "4,80", "por kg"),
        Produto(58, "Abacaxi", "5,00", "unidade"),
        Produto(59, "Uva", "12,00", "por kg"),
        Produto(60, "Morango", "15,00", "por kg"),
        Produto(61, "Manga", "7,00", "por kg"),
        Produto(62, "Pera", "9,50", "por kg"),

        // Verduras e Legumes
        Produto(63, "Tomate", "7,00", "por kg"),
        Produto(64, "Tomate cereja", "12,00", "por kg"),
        Produto(65, "Cebola", "4,50", "por kg"),
        Produto(66, "Batata inglesa", "5,00", "por kg"),
        Produto(67, "Batata doce", "4,50", "por kg"),
        Produto(68, "Cenoura", "4,00", "por kg"),
        Produto(69, "Alface americana", "3,50", "unidade"),
        Produto(70, "Alface crespa", "3,00", "unidade"),
        Produto(71, "Alho", "25,00", "por kg"),
        Produto(72, "Pimentão verde", "8,00", "por kg"),
        Produto(73, "Pimentão vermelho", "9,50", "por kg"),
        Produto(74, "Abobrinha", "5,50", "por kg"),
        Produto(75, "Berinjela", "6,00", "por kg"),
        Produto(76, "Brócolis", "7,50", "por kg"),
        Produto(77, "Couve-flor", "8,00", "por kg"),
        Produto(78, "Repolho", "3,50", "por kg"),
        Produto(79, "Pepino", "4,50", "por kg"),
        Produto(80, "Beterraba", "4,00", "por kg"),

        // Bebidas
        Produto(81, "Refrigerante cola 2L", "8,00", ""),
        Produto(82, "Refrigerante guaraná 2L", "7,50", ""),
        Produto(83, "Suco de laranja (1L)", "6,50", ""),
        Produto(84, "Suco de uva (1L)", "7,00", ""),
        Produto(85, "Água mineral (1,5L)", "2,50", ""),
        Produto(86, "Água com gás (1,5L)", "3,00", ""),
        Produto(87, "Cerveja lata", "3,50", "unidade"),
        Produto(88, "Vinho tinto", "25,00", "garrafa"),

        // Higiene e Limpeza
        Produto(89, "Sabão em pó (1kg)", "12,00", ""),
        Produto(90, "Detergente líquido", "2,50", ""),
        Produto(91, "Papel higiênico (12 rolos)", "18,00", ""),
        Produto(92, "Sabonete", "2,80", "unidade"),
        Produto(93, "Shampoo (400ml)", "12,00", ""),
        Produto(94, "Condicionador (400ml)", "11,50", ""),
        Produto(95, "Pasta de dente", "6,50", ""),
        Produto(96, "Água sanitária (1L)", "4,00", ""),
        Produto(97, "Desinfetante (2L)", "8,50", ""),
        Produto(98, "Esponja de lavar louça", "3,00", "pacote"),
        Produto(99, "Sabão em barra", "6,00", "5 unidades"),
        Produto(100, "Amaciante de roupas (2L)", "10,50", "")
    )
}

@Composable
fun DialogConfirmarExclusao(
    titulo: String,
    mensagem: String,
    onConfirmar: () -> Unit,
    onCancelar: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCancelar,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Warning,
                    null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(titulo, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        },
        text = {
            Text(mensagem, fontSize = 16.sp)
        },
        confirmButton = {
            Button(
                onClick = onConfirmar,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("SIM, DELETAR", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onCancelar) {
                Text("CANCELAR")
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
fun DialogNovidades(
    versaoAtual: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Star,
                    null,
                    tint = Color(0xFFFFC107),
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text("🎉 Novidades v$versaoAtual", fontWeight = FontWeight.Bold, fontSize = 20.sp)
            }
        },
        text = {
            Column {
                Text(
                    "Confirmação ao Deletar!",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(verticalAlignment = Alignment.Top) {
                    Text("✅", fontSize = 16.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Agora você confirma antes de deletar produtos e itens", fontSize = 14.sp)
                }
                Spacer(modifier = Modifier.height(6.dp))

                Row(verticalAlignment = Alignment.Top) {
                    Text("✅", fontSize = 16.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Verificação automática de atualizações disponíveis", fontSize = 14.sp)
                }
                Spacer(modifier = Modifier.height(6.dp))

                Row(verticalAlignment = Alignment.Top) {
                    Text("✅", fontSize = 16.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Melhorias de desempenho e estabilidade", fontSize = 14.sp)
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Obrigado por usar My List! 🛒",
                    fontSize = 14.sp,
                    color = Color.Gray,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("ENTENDI", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
fun VerificarAtualizacao(context: Context) {
    val appUpdateManager = remember { AppUpdateManagerFactory.create(context) }
    var mostrarDialogAtualizacao by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val appUpdateInfoTask = appUpdateManager.appUpdateInfo

        appUpdateInfoTask.addOnSuccessListener { appUpdateInfo ->
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE) {
                mostrarDialogAtualizacao = true
            }
        }
    }

    if (mostrarDialogAtualizacao) {
        AlertDialog(
            onDismissRequest = { mostrarDialogAtualizacao = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Info,
                        null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("🎉 Atualização Disponível!", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                }
            },
            text = {
                Column {
                    Text(
                        "Uma nova versão do My List está disponível!",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "Atualize agora para ter acesso às últimas funcionalidades e melhorias.",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW).apply {
                            data = Uri.parse("https://play.google.com/store/apps/details?id=${context.packageName}")
                            setPackage("com.android.vending")
                        }
                        try {
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=${context.packageName}"))
                            context.startActivity(browserIntent)
                        }
                        mostrarDialogAtualizacao = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("ATUALIZAR AGORA", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarDialogAtualizacao = false }) {
                    Text("DEPOIS")
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

fun exportarDados(context: Context, appState: AppState) {
    try {
        val dados = DadosExportacao(
            produtos = appState.produtos.toList(),
            lista = appState.minhaLista.toList(),
            proximoId = appState.proximoIdProduto
        )

        val json = Json.encodeToString(dados)
        val fileName = "lista_compras_${System.currentTimeMillis()}.mylist"
        val file = File(context.filesDir, fileName)
        file.writeText(json)

        val uri = try {
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } catch (e: Exception) {
            val cacheFile = File(context.cacheDir, fileName)
            cacheFile.writeText(json)
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", cacheFile)
        }

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/octet-stream"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Lista de Compras - My List")
            putExtra(Intent.EXTRA_TEXT, "📝 Aqui está minha lista de compras!")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        context.startActivity(Intent.createChooser(shareIntent, "Compartilhar lista"))
        Toast.makeText(context, context.getString(R.string.success_export), Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        Toast.makeText(context, context.getString(R.string.error_export, e.message), Toast.LENGTH_LONG).show()
    }
}

fun importarDados(context: Context, appState: AppState, uri: Uri, scope: kotlinx.coroutines.CoroutineScope) {
    try {
        val inputStream = context.contentResolver.openInputStream(uri)
        val json = inputStream?.bufferedReader()?.use { it.readText() } ?: ""

        if (json.isEmpty()) {
            Toast.makeText(context, "❌ Arquivo vazio", Toast.LENGTH_SHORT).show()
            return
        }

        val jsonParser = Json {
            ignoreUnknownKeys = true
            isLenient = true
        }

        val dados = jsonParser.decodeFromString<DadosExportacao>(json)

        appState.produtos.clear()
        appState.produtos.addAll(dados.produtos)
        appState.minhaLista.clear()
        appState.minhaLista.addAll(dados.lista)
        appState.proximoIdProduto = dados.proximoId

        scope.launch {
            context.dataStore.edit { preferences ->
                preferences[stringPreferencesKey("produtos_cadastrados")] = Json.encodeToString(dados.produtos)
                preferences[stringPreferencesKey("minha_lista")] = Json.encodeToString(dados.lista)
            }
        }

        Toast.makeText(context, context.getString(R.string.success_import, dados.lista.size), Toast.LENGTH_LONG).show()
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "❌ Erro ao importar: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
    }
}

@Composable
fun MyListApp() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val appState = remember { AppState() }
    val scope = rememberCoroutineScope()

    val LISTA_KEY = stringPreferencesKey("minha_lista")
    val PRODUTOS_KEY = stringPreferencesKey("produtos_cadastrados")
    val VERSION_KEY = stringPreferencesKey("app_version")
    val NOVIDADES_MOSTRADAS_KEY = stringPreferencesKey("novidades_mostradas")

    var mostrarDialogNovidades by remember { mutableStateOf(false) }
    val versaoAtual = "1.8"

    LaunchedEffect(Unit) {
        try {
            val preferences = context.dataStore.data.first()
            val savedVersion = preferences[VERSION_KEY] ?: ""
            val currentVersion = versaoAtual

            val jsonProdutos = preferences[PRODUTOS_KEY] ?: ""
            if (jsonProdutos.isNotEmpty() && savedVersion == currentVersion) {
                val produtosSalvos = Json.decodeFromString<List<Produto>>(jsonProdutos)
                appState.produtos.clear()
                appState.produtos.addAll(produtosSalvos)
                val maxId = produtosSalvos.maxOfOrNull { it.id } ?: 0
                appState.proximoIdProduto = maxId + 1
            } else {
                val produtosPadrao = carregarProdutosPadrao()
                appState.produtos.addAll(produtosPadrao)
                appState.proximoIdProduto = produtosPadrao.size + 1

                context.dataStore.edit { prefs ->
                    prefs[PRODUTOS_KEY] = Json.encodeToString(produtosPadrao)
                    prefs[VERSION_KEY] = currentVersion
                }
            }

            val jsonLista = preferences[LISTA_KEY] ?: ""
            if (jsonLista.isNotEmpty()) {
                val lista = Json.decodeFromString<List<ItemLista>>(jsonLista)
                appState.minhaLista.clear()
                appState.minhaLista.addAll(lista)
            }

            val novidadesMostradas = preferences[NOVIDADES_MOSTRADAS_KEY] ?: ""
            if (novidadesMostradas != versaoAtual) {
                mostrarDialogNovidades = true
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    if (mostrarDialogNovidades) {
        DialogNovidades(
            versaoAtual = versaoAtual,
            onDismiss = {
                mostrarDialogNovidades = false
                scope.launch {
                    context.dataStore.edit { prefs ->
                        prefs[NOVIDADES_MOSTRADAS_KEY] = versaoAtual
                    }
                }
            }
        )
    }

    VerificarAtualizacao(context)

    LaunchedEffect(appState.minhaLista.size, appState.minhaLista.toList()) {
        scope.launch {
            val json = Json.encodeToString(appState.minhaLista.toList())
            context.dataStore.edit { preferences ->
                preferences[LISTA_KEY] = json
            }
        }
    }

    Scaffold(
        bottomBar = { BottomNavigationBar(navController) }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = "lista",
            modifier = Modifier.padding(padding)
        ) {
            composable("cadastro") { TelaCadastroProdutos(appState, scope, context) }
            composable("lista") { TelaFazerLista(appState, scope, context) }
            composable("compras") { TelaIrAsCompras(appState, scope, context) }
        }
    }
}

@Composable
fun BottomNavigationBar(navController: NavController) {
    val items = listOf(
        NavigationItem("cadastro", stringResource(R.string.nav_products), Icons.Default.Add),
        NavigationItem("lista", stringResource(R.string.nav_make_list), Icons.Default.Edit),
        NavigationItem("compras", stringResource(R.string.nav_shopping), Icons.Default.ShoppingCart)
    )

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        tonalElevation = 8.dp
    ) {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route

        items.forEach { item ->
            NavigationBarItem(
                icon = { Icon(item.icon, contentDescription = item.title) },
                label = { Text(item.title) },
                selected = currentRoute == item.route,
                onClick = {
                    navController.navigate(item.route) {
                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    }
}

data class NavigationItem(val route: String, val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TelaCadastroProdutos(
    appState: AppState,
    scope: kotlinx.coroutines.CoroutineScope,
    context: Context
) {
    var mostrarDialogo by remember { mutableStateOf(false) }
    var mostrarMenu by remember { mutableStateOf(false) }
    var nome by remember { mutableStateOf("") }
    var preco by remember { mutableStateOf("") }
    var observacao by remember { mutableStateOf("") }
    var editandoProduto by remember { mutableStateOf<Produto?>(null) }
    var produtoParaDeletar by remember { mutableStateOf<Produto?>(null) }

    val PRODUTOS_KEY = stringPreferencesKey("produtos_cadastrados")

    val produtosOrdenados = remember(appState.produtos.toList()) {
        appState.produtos.sortedBy { it.nome }
    }

    val importarLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { importarDados(context, appState, it, scope) }
    }

    fun salvarProdutos() {
        scope.launch {
            val json = Json.encodeToString(appState.produtos.toList())
            context.dataStore.edit { preferences ->
                preferences[PRODUTOS_KEY] = json
            }
        }
    }

    if (produtoParaDeletar != null) {
        DialogConfirmarExclusao(
            titulo = "Deletar Produto?",
            mensagem = "Tem certeza que deseja deletar \"${produtoParaDeletar!!.nome}\"? Esta ação não pode ser desfeita.",
            onConfirmar = {
                appState.produtos.remove(produtoParaDeletar)
                salvarProdutos()
                produtoParaDeletar = null
            },
            onCancelar = {
                produtoParaDeletar = null
            }
        )
    }

    if (mostrarDialogo) {
        AlertDialog(
            onDismissRequest = {
                mostrarDialogo = false
                nome = ""
                preco = ""
                observacao = ""
                editandoProduto = null
            },
            title = {
                Text(
                    stringResource(if (editandoProduto != null) R.string.edit_product else R.string.new_product),
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            },
            text = {
                Column {
                    OutlinedTextField(
                        value = nome,
                        onValueChange = { nome = it },
                        label = { Text(stringResource(R.string.product_name)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = preco,
                        onValueChange = { preco = it },
                        label = { Text(stringResource(R.string.price)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        placeholder = { Text("0,00") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = observacao,
                        onValueChange = { observacao = it },
                        label = { Text(stringResource(R.string.observation)) },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        maxLines = 3
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (nome.isNotBlank()) {
                            val precoFormatado = if (preco.isNotBlank()) formatarPreco(preco) else ""

                            if (editandoProduto != null) {
                                val index = appState.produtos.indexOf(editandoProduto)
                                appState.produtos[index] = Produto(editandoProduto!!.id, nome, precoFormatado, observacao)
                            } else {
                                appState.produtos.add(Produto(appState.proximoIdProduto++, nome, precoFormatado, observacao))
                            }
                            salvarProdutos()
                            mostrarDialogo = false
                            nome = ""
                            preco = ""
                            observacao = ""
                            editandoProduto = null
                        }
                    },
                    enabled = nome.isNotBlank()
                ) {
                    Text(stringResource(if (editandoProduto != null) R.string.update else R.string.save))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    mostrarDialogo = false
                    nome = ""
                    preco = ""
                    observacao = ""
                    editandoProduto = null
                }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { mostrarDialogo = true },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, stringResource(R.string.new_product), tint = Color.White)
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                            colors = listOf(Color(0xFF4CAF50), Color(0xFF81C784))
                        )
                    )
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            stringResource(R.string.products_title),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
                        Text(
                            "${appState.produtos.size}",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            color = Color(0xFF4CAF50),
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }

                    Box {
                        IconButton(onClick = { mostrarMenu = true }) {
                            Icon(Icons.Default.MoreVert, "Menu", tint = Color.White)
                        }

                        DropdownMenu(
                            expanded = mostrarMenu,
                            onDismissRequest = { mostrarMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.export_data)) },
                                onClick = {
                                    exportarDados(context, appState)
                                    mostrarMenu = false
                                },
                                leadingIcon = { Icon(Icons.Default.Share, null) }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.import_data)) },
                                onClick = {
                                    importarLauncher.launch("*/*")
                                    mostrarMenu = false
                                },
                                leadingIcon = { Icon(Icons.Default.Download, null) }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.privacy_policy)) },
                                onClick = {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://rooxteam.com.br/mylist-privacy-policy.html"))
                                    context.startActivity(intent)
                                    mostrarMenu = false
                                },
                                leadingIcon = { Icon(Icons.Default.Info, null) }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.clear_all), color = MaterialTheme.colorScheme.error) },
                                onClick = {
                                    appState.produtos.clear()
                                    appState.minhaLista.clear()
                                    scope.launch {
                                        context.dataStore.edit { it.clear() }
                                    }
                                    Toast.makeText(context, context.getString(R.string.cleared_all), Toast.LENGTH_SHORT).show()
                                    mostrarMenu = false
                                },
                                leadingIcon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) }
                            )
                        }
                    }
                }
            }

            if (appState.produtos.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Add, null, modifier = Modifier.size(100.dp), tint = Color.Gray.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(stringResource(R.string.no_products), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(stringResource(R.string.click_add), fontSize = 14.sp, color = Color.Gray)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(produtosOrdenados, key = { it.id }) { produto ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                            elevation = CardDefaults.cardElevation(3.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(produto.nome, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                                    if (produto.preco.isNotBlank()) {
                                        Text(produto.preco, fontSize = 14.sp, color = Color.Gray)
                                    }
                                    if (produto.observacao.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            produto.observacao,
                                            fontSize = 12.sp,
                                            color = Color.DarkGray,
                                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                        )
                                    }
                                }

                                IconButton(onClick = {
                                    nome = produto.nome
                                    preco = produto.preco
                                    observacao = produto.observacao
                                    editandoProduto = produto
                                    mostrarDialogo = true
                                }) {
                                    Icon(Icons.Default.Edit, stringResource(R.string.edit), tint = MaterialTheme.colorScheme.primary)
                                }

                                IconButton(onClick = {
                                    produtoParaDeletar = produto
                                }) {
                                    Icon(Icons.Default.Delete, stringResource(R.string.delete), tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// CONTINUAÇÃO DO MainActivity.kt v1.7 - PARTE 2
// Cole este código APÓS a função TelaCadastroProdutos

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TelaFazerLista(
    appState: AppState,
    scope: kotlinx.coroutines.CoroutineScope,
    context: Context
) {
    var textoBusca by remember { mutableStateOf("") }
    var produtoSelecionado by remember { mutableStateOf<Produto?>(null) }
    var quantidade by remember { mutableStateOf(1) }
    var mostrarDialogo by remember { mutableStateOf(false) }
    var mostrarMenu by remember { mutableStateOf(false) }
    var itemParaDeletar by remember { mutableStateOf<ItemLista?>(null) }

    val produtosFiltrados = appState.produtos.filter {
        it.nome.contains(textoBusca, ignoreCase = true)
    }

    val valorTotal = calcularTotal(appState.minhaLista.toList())

    val importarLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { importarDados(context, appState, it, scope) }
    }

    fun salvarLista() {
        scope.launch {
            val json = Json.encodeToString(appState.minhaLista.toList())
            context.dataStore.edit { preferences ->
                preferences[stringPreferencesKey("minha_lista")] = json
            }
        }
    }

    if (itemParaDeletar != null) {
        DialogConfirmarExclusao(
            titulo = "Remover Item?",
            mensagem = "Tem certeza que deseja remover \"${itemParaDeletar!!.produto.nome}\" da lista?",
            onConfirmar = {
                appState.minhaLista.remove(itemParaDeletar)
                salvarLista()
                itemParaDeletar = null
            },
            onCancelar = {
                itemParaDeletar = null
            }
        )
    }

    if (mostrarDialogo && produtoSelecionado != null) {
        AlertDialog(
            onDismissRequest = {
                mostrarDialogo = false
                produtoSelecionado = null
                quantidade = 1
            },
            title = { Text(stringResource(R.string.add_to_list), fontWeight = FontWeight.Bold, fontSize = 20.sp) },
            text = {
                Column {
                    Text(produtoSelecionado!!.nome, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                    if (produtoSelecionado!!.preco.isNotBlank()) {
                        Text(produtoSelecionado!!.preco, fontSize = 14.sp, color = Color.Gray)
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    Text(stringResource(R.string.quantity), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        IconButton(
                            onClick = { if (quantidade > 1) quantidade-- },
                            modifier = Modifier.size(56.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.Delete, "Diminuir", tint = Color.White, modifier = Modifier.size(28.dp))
                        }

                        Text(
                            text = quantidade.toString(),
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.width(80.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )

                        IconButton(
                            onClick = { quantidade++ },
                            modifier = Modifier.size(56.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.Add, "Aumentar", tint = Color.White, modifier = Modifier.size(28.dp))
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        appState.minhaLista.add(ItemLista(produtoSelecionado!!, quantidade))
                        salvarLista()
                        mostrarDialogo = false
                        produtoSelecionado = null
                        quantidade = 1
                        textoBusca = ""
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.Check, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.add_button), fontSize = 16.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    mostrarDialogo = false
                    produtoSelecionado = null
                    quantidade = 1
                }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Box(
            modifier = Modifier.fillMaxWidth().background(
                brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                    colors = listOf(Color(0xFFFF6B35), Color(0xFFFF8A65))
                )
            )
        ) {
            Row(
                modifier = Modifier.padding(20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.make_list_title), fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(stringResource(R.string.total_estimated, valorTotal), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }

                Box {
                    IconButton(onClick = { mostrarMenu = true }) {
                        Icon(Icons.Default.MoreVert, "Menu", tint = Color.White)
                    }

                    DropdownMenu(expanded = mostrarMenu, onDismissRequest = { mostrarMenu = false }) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.export_list)) },
                            onClick = {
                                exportarDados(context, appState)
                                mostrarMenu = false
                            },
                            leadingIcon = { Icon(Icons.Default.Share, null) }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.import_list)) },
                            onClick = {
                                importarLauncher.launch("*/*")
                                mostrarMenu = false
                            },
                            leadingIcon = { Icon(Icons.Default.Download, null) }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.privacy_policy)) },
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://rooxteam.com.br/mylist-privacy-policy.html"))
                                context.startActivity(intent)
                                mostrarMenu = false
                            },
                            leadingIcon = { Icon(Icons.Default.Info, null) }
                        )
                    }
                }
            }
        }

        Column(modifier = Modifier.padding(16.dp)) {
            OutlinedTextField(
                value = textoBusca,
                onValueChange = { textoBusca = it },
                label = { Text(stringResource(R.string.search_product)) },
                placeholder = { Text(stringResource(R.string.search_example)) },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.Search, null) },
                trailingIcon = {
                    if (textoBusca.isNotBlank()) {
                        IconButton(onClick = { textoBusca = "" }) {
                            Icon(Icons.Default.Close, "Limpar")
                        }
                    }
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (textoBusca.isNotBlank() && produtosFiltrados.isNotEmpty()) {
                Text(
                    stringResource(R.string.found, produtosFiltrados.size),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                LazyColumn(
                    modifier = Modifier.weight(1f).clip(RoundedCornerShape(8.dp)),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(produtosFiltrados) { produto ->
                        Card(
                            modifier = Modifier.fillMaxWidth().clickable {
                                produtoSelecionado = produto
                                quantidade = 1
                                mostrarDialogo = true
                            },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                            elevation = CardDefaults.cardElevation(3.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Add, null, modifier = Modifier.size(32.dp), tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(produto.nome, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                                    if (produto.preco.isNotBlank()) {
                                        Text(produto.preco, fontSize = 14.sp, color = Color.Gray)
                                    }
                                    if (produto.observacao.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            produto.observacao,
                                            fontSize = 12.sp,
                                            color = Color.DarkGray,
                                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                        )
                                    }
                                }
                                Icon(Icons.Default.ShoppingCart, null, tint = Color.Gray, modifier = Modifier.size(24.dp))
                            }
                        }
                    }
                }
            } else if (textoBusca.isNotBlank() && produtosFiltrados.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Search, null, modifier = Modifier.size(32.dp), tint = MaterialTheme.colorScheme.onErrorContainer)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(stringResource(R.string.not_found, textoBusca), fontSize = 16.sp, color = MaterialTheme.colorScheme.onErrorContainer)
                    }
                }
                Spacer(modifier = Modifier.weight(1f))
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(R.string.my_list), fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)) {
                        Text(
                            stringResource(R.string.items, appState.minhaLista.size),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (appState.minhaLista.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.ShoppingCart, null, modifier = Modifier.size(80.dp), tint = Color.Gray.copy(alpha = 0.5f))
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(stringResource(R.string.empty_list), fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Color.Gray)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(stringResource(R.string.search_start), fontSize = 14.sp, color = Color.Gray)
                        }
                    }
                } else {
                    LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(appState.minhaLista.toList(), key = { "${it.produto.id}_${it.hashCode()}" }) { item ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                                elevation = CardDefaults.cardElevation(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
                                        modifier = Modifier.size(48.dp)
                                    ) {
                                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                            Text(
                                                text = item.quantidade.toString(),
                                                fontSize = 20.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(16.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(item.produto.nome, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                                        if (item.produto.preco.isNotBlank()) {
                                            Text(item.produto.preco, fontSize = 14.sp, color = Color.Gray)
                                        }
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        IconButton(onClick = {
                                            produtoSelecionado = item.produto
                                            quantidade = item.quantidade
                                            mostrarDialogo = true
                                            appState.minhaLista.remove(item)
                                            salvarLista()
                                        }) {
                                            Icon(Icons.Default.Edit, "Editar quantidade", tint = MaterialTheme.colorScheme.primary)
                                        }

                                        IconButton(onClick = {
                                            itemParaDeletar = item
                                        }) {
                                            Icon(Icons.Default.Delete, "Remover", tint = MaterialTheme.colorScheme.error)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TelaIrAsCompras(
    appState: AppState,
    scope: kotlinx.coroutines.CoroutineScope,
    context: Context
) {
    val totalItens = appState.minhaLista.size
    val itensComprados = appState.minhaLista.count { it.comprado }

    val valorTotalGeral = calcularTotal(appState.minhaLista.toList())
    val valorComprado = calcularTotal(appState.minhaLista.filter { it.comprado })
    val valorRestante = valorTotalGeral - valorComprado

    var mostrarMenu by remember { mutableStateOf(false) }
    var itemParaDeletar by remember { mutableStateOf<ItemLista?>(null) }

    val importarLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { importarDados(context, appState, it, scope) }
    }

    fun salvarLista() {
        scope.launch {
            val json = Json.encodeToString(appState.minhaLista.toList())
            context.dataStore.edit { preferences ->
                preferences[stringPreferencesKey("minha_lista")] = json
            }
        }
    }

    if (itemParaDeletar != null) {
        DialogConfirmarExclusao(
            titulo = "Remover Item?",
            mensagem = "Tem certeza que deseja remover \"${itemParaDeletar!!.produto.nome}\" da lista de compras?",
            onConfirmar = {
                val index = appState.minhaLista.indexOfFirst {
                    it.produto.id == itemParaDeletar!!.produto.id &&
                            it.quantidade == itemParaDeletar!!.quantidade &&
                            it.comprado == itemParaDeletar!!.comprado
                }
                if (index != -1) {
                    appState.minhaLista.removeAt(index)
                    salvarLista()
                }
                itemParaDeletar = null
            },
            onCancelar = {
                itemParaDeletar = null
            }
        )
    }

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Box(
            modifier = Modifier.fillMaxWidth().background(
                brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                    colors = listOf(Color(0xFF66BB6A), Color(0xFF81C784))
                )
            )
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(R.string.shopping_title), fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)

                    Box {
                        IconButton(onClick = { mostrarMenu = true }) {
                            Icon(Icons.Default.MoreVert, "Menu", tint = Color.White)
                        }

                        DropdownMenu(expanded = mostrarMenu, onDismissRequest = { mostrarMenu = false }) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.export_list)) },
                                onClick = {
                                    exportarDados(context, appState)
                                    mostrarMenu = false
                                },
                                leadingIcon = { Icon(Icons.Default.Share, null) }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.import_list)) },
                                onClick = {
                                    importarLauncher.launch("*/*")
                                    mostrarMenu = false
                                },
                                leadingIcon = { Icon(Icons.Default.Download, null) }
                            )
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.privacy_policy)) },
                                onClick = {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://rooxteam.com.br/mylist-privacy-policy.html"))
                                    context.startActivity(intent)
                                    mostrarMenu = false
                                },
                                leadingIcon = { Icon(Icons.Default.Info, null) }
                            )
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.clear_list), color = MaterialTheme.colorScheme.error) },
                                onClick = {
                                    appState.minhaLista.clear()
                                    salvarLista()
                                    Toast.makeText(context, context.getString(R.string.cleared_list), Toast.LENGTH_SHORT).show()
                                    mostrarMenu = false
                                },
                                leadingIcon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(stringResource(R.string.purchased, itensComprados, totalItens), fontSize = 14.sp, color = Color.White.copy(alpha = 0.8f))

                Spacer(modifier = Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text(stringResource(R.string.total_general), fontSize = 12.sp, color = Color.White.copy(alpha = 0.8f))
                        Text("%.2f".format(valorTotalGeral), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(stringResource(R.string.remaining), fontSize = 12.sp, color = Color.White.copy(alpha = 0.8f))
                        Text("%.2f".format(valorRestante), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                LinearProgressIndicator(
                    progress = { if (totalItens > 0) itensComprados.toFloat() / totalItens else 0f },
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                    color = Color.White,
                    trackColor = Color.White.copy(alpha = 0.3f)
                )
            }
        }

        if (appState.minhaLista.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.ShoppingCart, null, modifier = Modifier.size(100.dp), tint = Color.Gray)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(stringResource(R.string.no_items_list), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(stringResource(R.string.go_make_list), fontSize = 14.sp, color = Color.Gray)
                }
            }
        } else {
            val listaSnapshot = remember(appState.minhaLista.toList()) { appState.minhaLista.toList() }

            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(
                    items = listaSnapshot
                ) { item ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (item.comprado) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.primaryContainer
                        ),
                        elevation = CardDefaults.cardElevation(if (item.comprado) 2.dp else 6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(20.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = {
                                    val index = appState.minhaLista.indexOfFirst {
                                        it.produto.id == item.produto.id &&
                                                it.quantidade == item.quantidade &&
                                                it.comprado == item.comprado
                                    }
                                    if (index != -1) {
                                        appState.minhaLista[index] = item.copy(comprado = !item.comprado)
                                        salvarLista()
                                    }
                                },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    if (item.comprado) Icons.Default.CheckCircle else Icons.Default.ShoppingCart,
                                    null,
                                    tint = if (item.comprado) MaterialTheme.colorScheme.primary else Color.Gray,
                                    modifier = Modifier.size(40.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "${item.quantidade}x ${item.produto.nome}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    textDecoration = if (item.comprado) androidx.compose.ui.text.style.TextDecoration.LineThrough else androidx.compose.ui.text.style.TextDecoration.None,
                                    color = if (item.comprado) Color.Gray else Color.Unspecified
                                )
                                if (item.produto.preco.isNotBlank()) {
                                    Text(
                                        item.produto.preco,
                                        fontSize = 14.sp,
                                        color = Color.Gray,
                                        textDecoration = if (item.comprado) androidx.compose.ui.text.style.TextDecoration.LineThrough else androidx.compose.ui.text.style.TextDecoration.None
                                    )
                                }
                                if (item.produto.observacao.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        item.produto.observacao,
                                        fontSize = 12.sp,
                                        color = Color.DarkGray,
                                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                        textDecoration = if (item.comprado) androidx.compose.ui.text.style.TextDecoration.LineThrough else androidx.compose.ui.text.style.TextDecoration.None
                                    )
                                }
                            }

                            IconButton(
                                onClick = {
                                    itemParaDeletar = item
                                },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(Icons.Default.Delete, "Deletar", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(32.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}