package net.corda.contracts.universal

import net.corda.core.contracts.BusinessCalendar
import net.corda.core.contracts.Frequency
import net.corda.core.crypto.Party
import java.math.BigDecimal
import java.time.LocalDate
import java.util.*

val Int.M: BigDecimal get() = BigDecimal(this) * BigDecimal(1000000)
val Int.K: BigDecimal get() = BigDecimal(this) * BigDecimal(1000)

val Double.bd: BigDecimal get() = BigDecimal(this)

val zero = Zero()

class ActionsBuilder {
    private var actions = mutableSetOf<Action>()

    fun final() =
            if (actions.isEmpty())
                zero
            else
                Actions(actions.toSet())

    infix fun Party.may(init: ActionBuilder.() -> Action): Action {
        val builder = ActionBuilder(setOf(this))
        builder.init()
        actions.addAll(builder.actions)
        return builder.actions.first()
    }

    infix fun Set<Party>.may(init: ActionBuilder.() -> Action): Action {
        val builder = ActionBuilder(this)
        builder.init()
        actions.addAll(builder.actions)

        return builder.actions.first()
    }

    infix fun Party.or(party: Party) = setOf(this, party)
    infix fun Set<Party>.or(party: Party) = this.plus(party)
}

open class ContractBuilder {
    private val contracts = mutableListOf<Arrangement>()

    fun actions(init: ActionsBuilder.() -> Action): Arrangement {
        val b = ActionsBuilder()
        b.init()
        val c = b.final()
        contracts.add(c)
        return c
    }

    fun Party.owes(beneficiary: Party, amount: BigDecimal, currency: Currency): Obligation {
        val c = Obligation(const(amount), currency, this, beneficiary)
        contracts.add(c)
        return c
    }

    fun Party.owes(beneficiary: Party, amount: Perceivable<BigDecimal>, currency: Currency): Obligation {
        val c = Obligation(amount, currency, this, beneficiary)
        contracts.add(c)
        return c
    }

    @Deprecated(level = DeprecationLevel.ERROR, message = "Not allowed")
    fun Action(@Suppress("UNUSED_PARAMETER") name: String, @Suppress("UNUSED_PARAMETER") condition: Perceivable<Boolean>,
               @Suppress("UNUSED_PARAMETER") actors: Set<Party>, @Suppress("UNUSED_PARAMETER") arrangement: Arrangement) {
    }

    @Deprecated(level = DeprecationLevel.ERROR, message = "Not available")
    fun <T> String.anytime(@Suppress("UNUSED_PARAMETER") ignore: T) {
    }

    @Deprecated(level = DeprecationLevel.ERROR, message = "Not available")
    fun <T> String.givenThat(@Suppress("UNUSED_PARAMETER") ignore: T) {
    }

    @Deprecated(level = DeprecationLevel.ERROR, message = "Not available")
    fun <T> String.givenThat(@Suppress("UNUSED_PARAMETER") ignore1: T, @Suppress("UNUSED_PARAMETER") ignore2: T) {
    }

    @Deprecated(level = DeprecationLevel.ERROR, message = "Not available")
    fun Party.may(init: ActionBuilder.() -> Action) {
    }

    @Deprecated(level = DeprecationLevel.ERROR, message = "Not available")
    fun Set<Party>.may(init: ActionBuilder.() -> Action) {
    }

    val start = StartDate()
    val end = EndDate()

    fun next(): Continuation {
        val c = Continuation()
        contracts.add(c)
        return c
    }

    fun <T1> next(@Suppress("UNUSED_PARAMETER") p1: kotlin.Pair<Parameter<T1>, Perceivable<T1>>) = Continuation()
    fun <T1, T2> next(@Suppress("UNUSED_PARAMETER") p1: kotlin.Pair<Parameter<T1>, Perceivable<T1>>,
                      @Suppress("UNUSED_PARAMETER") p2: kotlin.Pair<Parameter<T2>, Perceivable<T2>>) = Continuation()

    fun <T1, T2, T3> next(@Suppress("UNUSED_PARAMETER") p1: kotlin.Pair<Parameter<T1>, Perceivable<T1>>,
                          @Suppress("UNUSED_PARAMETER") p2: kotlin.Pair<Parameter<T2>, Perceivable<T2>>,
                          @Suppress("UNUSED_PARAMETER") p3: kotlin.Pair<Parameter<T3>, Perceivable<T3>>) = Continuation()

    fun rollOut(startDate: LocalDate, endDate: LocalDate, frequency: Frequency, init: RollOutBuilder<Dummy>.() -> Unit): RollOut {
        val b = RollOutBuilder(startDate, endDate, frequency, Dummy())
        b.init()
        val c = b.final()
        contracts.add(c)
        return c
    }

    fun <T> rollOut(startDate: LocalDate, endDate: LocalDate, frequency: Frequency, vars: T, init: RollOutBuilder<T>.() -> Unit): RollOut {
        val b = RollOutBuilder(startDate, endDate, frequency, vars)
        b.init()
        val c = b.final()
        contracts.add(c)
        return c
    }

    val String.ld: LocalDate get() = BusinessCalendar.parseDateFromString(this)

    open fun final() =
            when (contracts.size) {
                0 -> zero
                1 -> contracts[0]
                else -> And(contracts.toSet())
            }
}


interface GivenThatResolve {
    fun resolve(contract: Arrangement)
}

class ActionBuilder(val actors: Set<Party>) {
    val actions = mutableListOf<Action>()

    fun String.givenThat(condition: Perceivable<Boolean>, init: ContractBuilder.() -> Arrangement): Action {
        val b = ContractBuilder()
        b.init()
        val a = Action(this, condition, actors, b.final())
        actions.add(a)
        return a
    }

    fun String.givenThat(condition: Perceivable<Boolean>): GivenThatResolve {
        val This = this
        return object : GivenThatResolve {
            override fun resolve(contract: Arrangement) {
                actions.add(Action(This, condition, actors, contract))
            }
        }
    }

    infix fun String.anytime(init: ContractBuilder.() -> Unit): Action {
        val b = ContractBuilder()
        b.init()
        val a = Action(this, const(true), actors, b.final())
        actions.add(a)
        return a
    }
}

fun arrange(init: ContractBuilder.() -> Unit): Arrangement {
    val b = ContractBuilder()
    b.init()
    return b.final()
}

data class Parameter<T>(val initialValue: T) : Perceivable<T>

fun <T> variable(v: T) = Parameter<T>(v)

class RollOutBuilder<T>(val startDate: LocalDate, val endDate: LocalDate, val frequency: Frequency, val vars: T) : ContractBuilder() {
    override fun final() =
            RollOut(startDate, endDate, frequency, super.final())
}

class Dummy {}
