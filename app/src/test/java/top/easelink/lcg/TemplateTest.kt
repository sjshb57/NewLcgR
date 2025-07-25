package top.easelink.lcg

class Box<out T>(private val value: T) {
    fun get(): T = value
}

class TemplateTest {

    val box: Box<Int> = Box(1)

    @org.junit.Test
    fun aTest() {
        val a = mutableListOf<Any>("1", 1, 4.0)
        val b = mutableListOf<B>(B(), B())

        fill(a, b)
    }
}

open class A
class B : A()

fun fill(dest: MutableList<in A>, value: MutableList<out A>) {
    dest.addAll(value)
}