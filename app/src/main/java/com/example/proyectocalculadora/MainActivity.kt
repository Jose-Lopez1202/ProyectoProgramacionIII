package com.example.proyectocalculadora
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.proyectocalculadora.ui.theme.ProyectoCalculadoraTheme
import java.util.*
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import kotlin.math.pow


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ProyectoCalculadoraTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    ExpressionCalculatorApp(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

data class TreeNode(
    val value: String,
    val left: TreeNode? = null,
    val right: TreeNode? = null,
    val isOperator: Boolean = false
)

@Composable
fun ExpressionCalculatorApp(modifier: Modifier = Modifier) {
    var expression by remember { mutableStateOf("") }
    var result by remember { mutableStateOf("") }
    var showTree by remember { mutableStateOf(false) }
    var tree by remember { mutableStateOf<TreeNode?>(null) }
    var errorMessage by remember { mutableStateOf("") }
    var isMaximized by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Calculadora de Expresiones",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        OutlinedTextField(
            value = expression,
            onValueChange = { expression = it; errorMessage = ""; result = "" },
            label = { Text("Expresión matemática") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            trailingIcon = {
                IconButton(onClick = { expression = "" }) {
                    Text("×")
                }
            }
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    try {
                        val parsedTree = buildExpressionTree(expression)
                        tree = parsedTree
                        result = evaluateTree(parsedTree).toString()
                        showTree = true
                        errorMessage = ""
                    } catch (e: Exception) {
                        errorMessage = "Error: ${e.message ?: "Expresión inválida"}"
                        result = ""
                        showTree = false
                    }
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("Calcular")
            }

            OutlinedButton(
                onClick = { showTree = !showTree },
                enabled = tree != null,
                modifier = Modifier.weight(1f)
            ) {
                Text(if (showTree) "Ocultar árbol" else "Mostrar árbol")
            }
        }

        CalculatorButtons(
            onButtonClick = { symbol ->
                expression += symbol
            },
            onDeleteClick = {
                if (expression.isNotEmpty()) {
                    expression = expression.dropLast(1)
                }
            },
            onClearClick = {
                expression = ""
                result = ""
                errorMessage = ""
                showTree = false
            }
        )

        if (errorMessage.isNotEmpty()) {
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }
        if (result.isNotEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
                    .shadow(4.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Text(
                    text = "Resultado: $result",
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
        if (showTree && tree != null) {
            Box(modifier = Modifier.fillMaxWidth()) {
                ZoomableTree(tree = tree!!, isMaximized = isMaximized)
               /* IconButton(
                    onClick = { isMaximized = !isMaximized },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .background(Color.White, shape = CircleShape)
                        .zIndex(2f)
                ) {
                    Icon(
                        imageVector = Icons.Filled.ArrowDropDown,
                        contentDescription = "Maximizar"
                    )
                */}
            }
        }
    }
//}

@Composable
fun ZoomableTree(tree: TreeNode, isMaximized: Boolean) {
    var scale by remember { mutableStateOf(1f) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }

    val heightModifier = if (isMaximized) Modifier.fillMaxHeight() else Modifier.height(600.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .then(heightModifier)
            .clip(shape = RectangleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .pointerInput(Unit) {
                detectTransformGestures { _, pan: Offset, zoom: Float, _ ->
                    scale = (scale * zoom).coerceIn(0.5f, 3f)
                    offsetX = (offsetX + pan.x).coerceIn(-500f, 500f)
                    offsetY = (offsetY + pan.y).coerceIn(-500f, 500f)
                }
            }
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offsetX,
                    translationY = offsetY
                )
        ) {
            val center = Offset(size.width / 2, 60f)
            drawTree(tree, center, size.width / 2)
        }
    }
}

@Composable
fun CalculatorButtons(
    onButtonClick: (String) -> Unit,
    onDeleteClick: () -> Unit,
    onClearClick: () -> Unit
) {
    val buttons = listOf(
        listOf("(", ")", "^", "DEL"),
        listOf("7", "8", "9", "/"),
        listOf("4", "5", "6", "*"),
        listOf("1", "2", "3", "-"),
        listOf("0", ".", "C", "+")
    )

    Column(
        modifier = Modifier
            .padding(vertical = 8.dp)
            .fillMaxWidth()
    ) {
        buttons.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { button ->
                    Button(
                        onClick = {
                            when (button) {
                                "DEL" -> onDeleteClick()
                                "C" -> onClearClick()
                                else -> onButtonClick(button)
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .padding(vertical = 4.dp),
                        colors = when (button) {
                            "DEL" -> ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                            "C" -> ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                            in listOf("+", "-", "*", "/", "^", "(", ")") ->
                                ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                            else -> ButtonDefaults.buttonColors()
                        }
                    ) {
                        Text(button)
                    }
                }
            }
        }
    }
}

@Composable
fun TreeDrawer(tree: TreeNode) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val startX = size.width / 2
        val startY = 60f
        val horizontalSpacing = size.width / 3

        drawTree(tree, Offset(startX, startY), horizontalSpacing)
    }
}

fun androidx.compose.ui.graphics.drawscope.DrawScope.drawTree(
    node: TreeNode,
    position: Offset,
    horizontalOffset: Float,
    depth: Int = 0
) {
    val nodeRadius = 30f
    val textColor = if (node.isOperator) Color.White else Color.Black
    val nodeColor = if (node.isOperator) Color(0xFF2196F3) else Color(0xFFE3F2FD)
    val borderColor = Color(0xFF1976D2)

    drawCircle(
        color = nodeColor,
        radius = nodeRadius,
        center = position
    )

    drawCircle(
        color = borderColor,
        radius = nodeRadius,
        center = position,
        style = Stroke(width = 2f)
    )
    drawIntoCanvas { canvas ->
        val paint = android.graphics.Paint().apply {
            color = android.graphics.Color.BLACK
            if (node.isOperator) {
                color = android.graphics.Color.WHITE
            }
            textAlign = android.graphics.Paint.Align.CENTER
            textSize = 36f
            isFakeBoldText = true
        }
        canvas.nativeCanvas.drawText(node.value, position.x, position.y + 12f, paint)
    }

    val verticalSpacing = 80f
    node.left?.let {
        val nextHorizontalOffset = horizontalOffset * 0.6f
        val childPos = Offset(
            position.x - horizontalOffset / (depth * 0.5f + 1),
            position.y + verticalSpacing
        )
        drawLine(
            color = Color(0xFF1976D2),
            start = position,
            end = childPos,
            strokeWidth = 2f
        )
        drawTree(it, childPos, nextHorizontalOffset, depth + 1)
    }

    node.right?.let {
        val nextHorizontalOffset = horizontalOffset * 0.6f
        val childPos = Offset(
            position.x + horizontalOffset / (depth * 0.5f + 1),
            position.y + verticalSpacing
        )
        drawLine(
            color = Color(0xFF1976D2),
            start = position,
            end = childPos,
            strokeWidth = 2f
        )
        drawTree(it, childPos, nextHorizontalOffset, depth + 1)
    }
}
fun buildExpressionTree(expression: String): TreeNode {
    if (expression.isEmpty()) {
        throw Exception("La expresión está vacía")
    }
    val tokens = tokenize(expression)
    val postfix = infixToPostfix(tokens)
    val stack = Stack<TreeNode>()

    for (token in postfix) {
        if (isOperator(token)) {

            if (stack.size < 2) throw Exception("Expresión inválida: insuficientes operandos")

            val right = stack.pop()
            val left = stack.pop()
            stack.push(TreeNode(token, left, right, isOperator = true))
        } else {

            stack.push(TreeNode(token))
        }
    }

    if (stack.size != 1) throw Exception("Expresión inválida")
    return stack.pop()
}

fun tokenize(expression: String): List<String> {
    val result = mutableListOf<String>()
    var i = 0

    while (i < expression.length) {
        val c = expression[i]

        if (c.isWhitespace()) {
            i++
            continue
        }

        if (c.isDigit() || c == '.') {

            val startIndex = i
            var hasDecimal = c == '.'

            i++
            while (i < expression.length && (expression[i].isDigit() || expression[i] == '.' && !hasDecimal)) {
                if (expression[i] == '.') hasDecimal = true
                i++
            }

            result.add(expression.substring(startIndex, i))
        } else if (c == '(' || c == ')' || isOperatorChar(c)) {
            result.add(c.toString())
            i++
        } else {
            throw Exception("Carácter inválido: $c")
        }
    }

    return result
}

fun isOperator(token: String): Boolean {
    return token.length == 1 && isOperatorChar(token[0])
}

fun isOperatorChar(c: Char): Boolean {
    return c in "+-*/^"
}

fun getPrecedence(operator: String): Int {
    return when (operator) {
        "+", "-" -> 1
        "*", "/" -> 2
        "^" -> 3
        else -> 0
    }
}

fun infixToPostfix(tokens: List<String>): List<String> {
    val result = mutableListOf<String>()
    val stack = Stack<String>()

    for (token in tokens) {
        when {
            token == "(" -> stack.push(token)
            token == ")" -> {
                while (stack.isNotEmpty() && stack.peek() != "(") {
                    result.add(stack.pop())
                }
                if (stack.isNotEmpty() && stack.peek() == "(") {
                    stack.pop()
                } else {
                    throw Exception("Paréntesis desbalanceados")
                }
            }
            isOperator(token) -> {
                while (stack.isNotEmpty() && stack.peek() != "(" &&
                    (getPrecedence(stack.peek()) > getPrecedence(token) ||
                            (getPrecedence(stack.peek()) == getPrecedence(token) && token != "^"))) {
                    result.add(stack.pop())
                }
                stack.push(token)
            }
            else -> result.add(token)
        }
    }

    while (stack.isNotEmpty()) {
        if (stack.peek() == "(") throw Exception("Paréntesis desbalanceados")
        result.add(stack.pop())
    }

    return result
}

fun evaluateTree(node: TreeNode): Double {
    if (node.left == null && node.right == null) {
        return node.value.toDoubleOrNull() ?: throw Exception("Valor inválido: ${node.value}")
    }

    val leftValue = evaluateTree(node.left!!)
    val rightValue = evaluateTree(node.right!!)

    return when (node.value) {
        "+" -> leftValue + rightValue
        "-" -> leftValue - rightValue
        "*" -> leftValue * rightValue
        "/" -> {
            if (rightValue == 0.0) throw Exception("División por cero")
            leftValue / rightValue
        }
        "^" -> leftValue.pow(rightValue)
        else -> throw Exception("Operador desconocido: ${node.value}")
    }
}

@Preview(showBackground = true)
@Composable
fun CalculatorPreview() {
    ProyectoCalculadoraTheme {
        ExpressionCalculatorApp()
    }
}