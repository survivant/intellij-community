// "Create parameter 'foo'" "false"
// ACTION: Add 'f =' to argument
// ACTION: Create function 'foo'
// ACTION: Enable 'Types' inlay hints
// ACTION: Rename reference
// ERROR: Unresolved reference: foo
fun test(f: (Int) -> Int) {}

fun refer() {
    val v = test(::<caret>foo)
}
