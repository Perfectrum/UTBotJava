package org.utbot.fuzzing

import org.utbot.fuzzing.seeds.KnownValue
import org.utbot.fuzzing.utils.MissedSeed
import org.utbot.fuzzing.utils.chooseOne
import org.utbot.fuzzing.utils.flipCoin
import kotlin.random.Random

/**
 * User class that holds data about current fuzzing running.
 */
interface Statistic<TYPE, RESULT> {
    val startTime: Long
    val totalRuns: Long
    val elapsedTime: Long
    val missedTypes: MissedSeed<TYPE, RESULT>
    val random: Random
    val configuration: Configuration
//    val minsetSize: Long
}

///region Statistic Implementations

interface SeedsMaintainingStatistic<TYPE, RESULT, FEEDBACK : Feedback<TYPE, RESULT>>: Statistic<TYPE, RESULT> {
    override var totalRuns: Long
    fun put(random: Random, configuration: Configuration, feedback: FEEDBACK, seed: Node<TYPE, RESULT>) : MinsetEvent
    fun getRandomSeed(random: Random, configuration: Configuration): Node<TYPE, RESULT>
    fun getMutationsEfficiencies(): Map<Mutation<*>, Float> { return emptyMap() }
    fun isNotEmpty() : Boolean
    fun size() : Int
}

open class BaseStatisticImpl<TYPE, RESULT, FEEDBACK : Feedback<TYPE, RESULT>>(
    override var totalRuns: Long = 0,
    override val startTime: Long = System.nanoTime(),
    override var missedTypes: MissedSeed<TYPE, RESULT> = MissedSeed(),
    override val random: Random,
    override val configuration: Configuration
) : SeedsMaintainingStatistic<TYPE, RESULT, FEEDBACK> {
    constructor(source: Statistic<TYPE, RESULT>) : this(
        totalRuns = source.totalRuns,
        startTime = source.startTime,
        missedTypes = source.missedTypes,
        random = source.random,
        configuration = source.configuration.copy(),
    )

    override val elapsedTime: Long
        get() = System.nanoTime() - startTime
    private val seeds = linkedMapOf<FEEDBACK, Node<TYPE, RESULT>>()
    private val count = linkedMapOf<FEEDBACK, Long>()

    override fun put(random: Random, configuration: Configuration, feedback: FEEDBACK, seed: Node<TYPE, RESULT>) : MinsetEvent {
        var result = MinsetEvent.NOTHING_NEW

        if (!seeds.containsKey(feedback)) {
            result = MinsetEvent.NEW_FEEDBACK
        }

        if (random.flipCoin(configuration.probUpdateSeedInsteadOfKeepOld)) {
            if (seeds[feedback] != seed) {
                result = MinsetEvent.NEW_VALUE
            }
            seeds[feedback] = seed
        } else {
            seeds.putIfAbsent(feedback, seed)
        }
        count[feedback] = count.getOrDefault(feedback, 0L) + 1L

        return result
    }

    override fun getRandomSeed(random: Random, configuration: Configuration): Node<TYPE, RESULT> {
        if (seeds.isEmpty()) error("Call `isNotEmpty` before getting the seed")
        val entries = seeds.entries.toList()
        val frequencies = DoubleArray(seeds.size).also { f ->
            entries.forEachIndexed { index, (key, _) ->
                f[index] = configuration.energyFunction(count.getOrDefault(key, 0L))
            }
        }
        val index = random.chooseOne(frequencies)
        return entries[index].value
    }

    override fun isNotEmpty() = seeds.isNotEmpty()

    override fun size(): Int {
        return seeds.size
    }
}

open class SingleValueMinsetStatistic<TYPE, RESULT, FEEDBACK : Feedback<TYPE, RESULT>, STORAGE : SingleValueStorage<TYPE, RESULT>> (
    override var totalRuns: Long = 0,
    override val startTime: Long = System.nanoTime(),
    override var missedTypes: MissedSeed<TYPE, RESULT> = MissedSeed(),
    override val random: Random,
    override val configuration: Configuration,
    private val generateStorage: () -> STORAGE
) : SeedsMaintainingStatistic<TYPE, RESULT, FEEDBACK> {
    override val elapsedTime: Long
        get() = System.nanoTime() - startTime

    private val minset: Minset<TYPE, RESULT, FEEDBACK, STORAGE> = SingleValueMinset(
        valueStorageGenerator = generateStorage
    )

    constructor(source: Statistic<TYPE, RESULT>, generateStorage: () -> STORAGE) : this(
        totalRuns = source.totalRuns,
        startTime = source.startTime,
        missedTypes = source.missedTypes,
        random = source.random,
        configuration = source.configuration.copy(),
        generateStorage = generateStorage
    )

    override fun put(random: Random, configuration: Configuration, feedback: FEEDBACK, seed: Node<TYPE, RESULT>) : MinsetEvent {
        return minset.put(seed, feedback)
    }

    override fun getRandomSeed(random: Random, configuration: Configuration): Node<TYPE, RESULT> {
        if (minset.isEmpty()) error("Call `isNotEmpty` before getting the seed")
        val entries = minset.seeds.entries.toList()
        val frequencies = DoubleArray(minset.getSize()).also { f ->
            entries.forEachIndexed { index, (key, _) ->
                f[index] = configuration.energyFunction( minset.count.getOrDefault(key, 0L))
            }
        }
        val index = random.chooseOne(frequencies)
        return entries[index].value.next()
    }

    override fun isNotEmpty() = minset.isNotEmpty()
    override fun size(): Int {
        return minset.getSize()
    }
}

open class BasicSingleValueMinsetStatistic<TYPE, RESULT, FEEDBACK : Feedback<TYPE, RESULT>>(
    override var totalRuns: Long = 0,
    override val startTime: Long = System.nanoTime(),
    override var missedTypes: MissedSeed<TYPE, RESULT> = MissedSeed(),
    override val random: Random,
    override val configuration: Configuration,
    private val seedSelectionStrategy: SingleValueSelectionStrategy
) : SingleValueMinsetStatistic<TYPE, RESULT, FEEDBACK, SingleValueStorage<TYPE, RESULT>>(
    totalRuns, startTime, missedTypes, random, configuration, { SingleValueStorage(seedSelectionStrategy) }
) {
    constructor(source: Statistic<TYPE, RESULT>, seedSelectionStrategy: SingleValueSelectionStrategy) : this (
        totalRuns = source.totalRuns,
        startTime = source.startTime,
        missedTypes = source.missedTypes,
        random = source.random,
        configuration = source.configuration.copy(),
        seedSelectionStrategy = seedSelectionStrategy
    )
}

open class MutationsCountingSingleValueMinsetStatistic<TYPE, RESULT, FEEDBACK : Feedback<TYPE, RESULT>>(
    override var totalRuns: Long = 0,
    override val startTime: Long = System.nanoTime(),
    override var missedTypes: MissedSeed<TYPE, RESULT> = MissedSeed(),
    override val random: Random,
    override val configuration: Configuration,
    private val seedSelectionStrategy: SingleValueSelectionStrategy
) : SingleValueMinsetStatistic<TYPE, RESULT, FEEDBACK, SingleValueStorage<TYPE, RESULT>>(
    totalRuns, startTime, missedTypes, random, configuration, { MutationsCountingSingleValueStorage(seedSelectionStrategy) }
) {
    constructor(source: Statistic<TYPE, RESULT>, seedSelectionStrategy: SingleValueSelectionStrategy) : this (
        totalRuns = source.totalRuns,
        startTime = source.startTime,
        missedTypes = source.missedTypes,
        random = source.random,
        configuration = source.configuration.copy(),
        seedSelectionStrategy = seedSelectionStrategy
    )
}


open class SingleSeedKeepingStatistics<TYPE, RESULT, FEEDBACK : Feedback<TYPE, RESULT>> (
    override var totalRuns: Long = 0,
    override val startTime: Long = System.nanoTime(),
    override var missedTypes: MissedSeed<TYPE, RESULT> = MissedSeed(),
    override val random: Random,
    override val configuration: Configuration
): SeedsMaintainingStatistic<TYPE, RESULT, FEEDBACK> {
    constructor(source: Statistic<TYPE, RESULT>) : this(
        totalRuns = source.totalRuns,
        startTime = source.startTime,
        missedTypes = source.missedTypes,
        random = source.random,
        configuration = source.configuration.copy()
    )

    override val elapsedTime: Long
        get() = System.nanoTime() - startTime

    private val storedSeed = SingleValueStorage<TYPE, RESULT>(configuration.singleValueSelectionStrategy)
    private val feedbacksCount: LinkedHashMap<FEEDBACK, Long> = linkedMapOf()
    private val successCumSums: HashMap<Mutation<*>, Int> = hashMapOf()
    private val overallCumSums: HashMap<Mutation<*>, Int> = hashMapOf()
    private val mutationsEfficiencies: HashMap<Mutation<*>, Float> = hashMapOf()


    override fun put(random: Random, configuration: Configuration, feedback: FEEDBACK, seed: Node<TYPE, RESULT>): MinsetEvent {
        storedSeed.put(seed, feedback)
        feedbacksCount.merge(feedback, 1L, Long::plus)
        val event = if (feedbacksCount[feedback] == 1L) MinsetEvent.NEW_FEEDBACK else MinsetEvent.NOTHING_NEW

        seed.result.forEach { result ->
            when(result) {
                is Result.Known<TYPE, RESULT, *> -> {
                    result.lastMutation?.let {
                        overallCumSums[it] = overallCumSums.getOrDefault(it, 0) + 1
                    }
                }
                else -> {}
            }
        }

        if (event == MinsetEvent.NEW_FEEDBACK) {
            seed.result.forEach { result ->
                when(result) {
                    is Result.Known<TYPE, RESULT, *> -> {
                        result.lastMutation.let {
                            it?.let {
                                successCumSums[it] = successCumSums.getOrDefault(it, 0) + 1
                                mutationsEfficiencies[it] =
                                    successCumSums[it]!!.toFloat() / overallCumSums[it]!!.toFloat()
                            }
                        }
                    }
                    else -> {}
                }
            }
        }

        return event
    }

    override fun getRandomSeed(random: Random, configuration: Configuration): Node<TYPE, RESULT> {
        return storedSeed.next()
    }

    override fun getMutationsEfficiencies(): Map<Mutation<*>, Float> {
        return mutationsEfficiencies
    }

    override fun isNotEmpty(): Boolean {
        return feedbacksCount.isNotEmpty()
    }

    override fun size(): Int {
        return feedbacksCount.size
    }
}

class FirstSeedKeepingStatistics<TYPE, RESULT, FEEDBACK : Feedback<TYPE, RESULT>>(
    override var totalRuns: Long = 0,
    override val startTime: Long = System.nanoTime(),
    override var missedTypes: MissedSeed<TYPE, RESULT> = MissedSeed(),
    override val random: Random,
    override val configuration: Configuration,
) : SingleSeedKeepingStatistics<TYPE, RESULT, FEEDBACK>(totalRuns, startTime, missedTypes, random, configuration) {
    constructor(source: Statistic<TYPE, RESULT>) : this(
        totalRuns = source.totalRuns,
        startTime = source.startTime,
        missedTypes = source.missedTypes,
        random = source.random,
        configuration = source.configuration.copy()
    )
}

class LastSeedKeepingStatistics<TYPE, RESULT, FEEDBACK : Feedback<TYPE, RESULT>>(
    override var totalRuns: Long = 0,
    override val startTime: Long = System.nanoTime(),
    override var missedTypes: MissedSeed<TYPE, RESULT> = MissedSeed(),
    override val random: Random,
    override val configuration: Configuration,
) : SingleSeedKeepingStatistics<TYPE, RESULT, FEEDBACK>(totalRuns, startTime, missedTypes, random, configuration) {
    constructor(source: Statistic<TYPE, RESULT>) : this(
        totalRuns = source.totalRuns,
        startTime = source.startTime,
        missedTypes = source.missedTypes,
        random = source.random,
        configuration = source.configuration.copy()
    )
}
///endregion

///region Minset

enum class MinsetEvent { NEW_FEEDBACK, NEW_VALUE, NOTHING_NEW }

abstract class Minset<TYPE, RESULT, FEEDBACK : Feedback<TYPE, RESULT>, STORAGE : ValueStorage<TYPE, RESULT>> (
    open val valueStorageGenerator: () -> STORAGE,
    val seeds: LinkedHashMap<FEEDBACK, STORAGE> = linkedMapOf(),
    val count: LinkedHashMap<FEEDBACK, Long> = linkedMapOf()
) {
    operator fun get(feedback: FEEDBACK): STORAGE? {
        return seeds[feedback]
    }

    open fun put(value: Node<TYPE, RESULT>, feedback: FEEDBACK) : MinsetEvent {
        val result: MinsetEvent

        if (seeds.containsKey(feedback)) {
            result = if( seeds[feedback]!!.put(value, feedback) ) { MinsetEvent.NEW_VALUE } else { MinsetEvent.NOTHING_NEW }
        } else {
            result = MinsetEvent.NEW_FEEDBACK
            seeds[feedback] = valueStorageGenerator.invoke()
            seeds[feedback]!!.put(value, feedback)
        }

        count[feedback] = count.getOrDefault(feedback, 0L) + 1L

        return result
    }

    fun isNotEmpty(): Boolean {
        return seeds.isNotEmpty()
    }

    fun isEmpty(): Boolean {
        return seeds.isEmpty()
    }

    fun getSize() : Int {
        return seeds.size
    }
}

open class SingleValueMinset<TYPE, RESULT, FEEDBACK : Feedback<TYPE, RESULT>, STORAGE : SingleValueStorage<TYPE, RESULT>> (
    override val valueStorageGenerator: () -> STORAGE,
) : Minset<TYPE, RESULT, FEEDBACK, STORAGE>(valueStorageGenerator)
///endregion

///region Value storages
interface InfiniteIterator<T> : Iterator<T> {
    override operator fun next(): T
    override operator fun hasNext(): Boolean
}

interface ValueStorage<TYPE, RESULT> : InfiniteIterator<Node<TYPE, RESULT>> {
    fun put(value: Node<TYPE, RESULT>, feedback: Feedback<TYPE, RESULT>) : Boolean
}

open class SingleValueStorage<TYPE, RESULT> (
    private val strategy : SingleValueSelectionStrategy
) : ValueStorage<TYPE, RESULT> {

    private var storedValue: Node<TYPE, RESULT>? = null
    override fun put(value: Node<TYPE, RESULT>, feedback: Feedback<TYPE, RESULT>) : Boolean {
        val result = storedValue == null

        storedValue = when (strategy) {
            SingleValueSelectionStrategy.FIRST -> storedValue ?: value
            SingleValueSelectionStrategy.LAST -> value
        }

        return result || (strategy == SingleValueSelectionStrategy.LAST)
    }

    override fun next(): Node<TYPE, RESULT> {
        if (storedValue == null) {
            error("Next value requested but no value stored")
        } else {
            return storedValue as Node<TYPE, RESULT>
        }
    }

    override fun hasNext(): Boolean {
        return storedValue != null
    }
}

class MutationsCountingSingleValueStorage<TYPE, RESULT>(strategy: SingleValueSelectionStrategy) :
    SingleValueStorage<TYPE, RESULT>(strategy) {
    private val knownValueMutationsCount: HashMap<Mutation<*>, Int> = hashMapOf()

    override fun put(value: Node<TYPE, RESULT>, feedback: Feedback<TYPE, RESULT>): Boolean {
        value.result.forEach { result ->
            when(result) {
                is Result.Known<*, *, *> -> {
                    result.lastMutation.let {
                        it?.let {
                            knownValueMutationsCount[it] = knownValueMutationsCount.getOrDefault(it, 0) + 1
                        }
                    }
                }
                else -> {}
            }
        }

        return super.put(value, feedback)
    }
}

///endregion