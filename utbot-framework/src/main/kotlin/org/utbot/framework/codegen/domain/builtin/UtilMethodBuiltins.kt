package org.utbot.framework.codegen.domain.builtin

import org.mockito.MockitoAnnotations
import org.utbot.framework.codegen.domain.MockitoStaticMocking
import org.utbot.framework.codegen.domain.models.CgClassId
import org.utbot.framework.codegen.renderer.utilMethodTextById
import org.utbot.framework.codegen.tree.arrayTypeOf
import org.utbot.framework.codegen.tree.utilMethodId
import org.utbot.framework.codegen.tree.ututils.UtilClassKind.Companion.PACKAGE_DELIMITER
import org.utbot.framework.codegen.tree.ututils.UtilClassKind.Companion.UT_UTILS_BASE_PACKAGE_NAME
import org.utbot.framework.plugin.api.BuiltinClassId
import org.utbot.framework.plugin.api.BuiltinConstructorId
import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.CodegenLanguage
import org.utbot.framework.plugin.api.MethodId
import org.utbot.framework.plugin.api.util.baseStreamClassId
import org.utbot.framework.plugin.api.util.booleanClassId
import org.utbot.framework.plugin.api.util.builtinConstructorId
import org.utbot.framework.plugin.api.util.classClassId
import org.utbot.framework.plugin.api.util.id
import org.utbot.framework.plugin.api.util.intClassId
import org.utbot.framework.plugin.api.util.jClass
import org.utbot.framework.plugin.api.util.objectArrayClassId
import org.utbot.framework.plugin.api.util.objectClassId
import org.utbot.framework.plugin.api.util.stringClassId
import org.utbot.framework.plugin.api.util.voidClassId
import sun.misc.Unsafe
import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType
import java.lang.reflect.Method

/**
 * Set of ids of all possible util methods for a given class.
 *
 * The class may actually not have some of these methods if they
 * are not required in the process of code generation (this is the case for [TestClassUtilMethodProvider]).
 */
abstract class UtilMethodProvider(val utilClassId: ClassId) {
    val utilMethodIds: Set<MethodId>
        get() = setOf(
            getUnsafeInstanceMethodId,
            createInstanceMethodId,
            createArrayMethodId,
            setFieldMethodId,
            setStaticFieldMethodId,
            getFieldValueMethodId,
            getStaticFieldValueMethodId,
            getEnumConstantByNameMethodId,
            deepEqualsMethodId,
            arraysDeepEqualsMethodId,
            iterablesDeepEqualsMethodId,
            streamsDeepEqualsMethodId,
            mapsDeepEqualsMethodId,
            hasCustomEqualsMethodId,
            getArrayLengthMethodId,
            consumeBaseStreamMethodId,
            buildStaticLambdaMethodId,
            buildLambdaMethodId,
            getLookupInMethodId,
            getLambdaCapturedArgumentTypesMethodId,
            getLambdaCapturedArgumentValuesMethodId,
            getInstantiatedMethodTypeMethodId,
            getLambdaMethodMethodId,
            getSingleAbstractMethodMethodId
        )

    val getUnsafeInstanceMethodId: MethodId
        get() = utilClassId.utilMethodId(
            name = "getUnsafeInstance",
            returnType = Unsafe::class.id,
        )

    /**
     * Method that creates instance using Unsafe
     */
    val createInstanceMethodId: MethodId
        get() = utilClassId.utilMethodId(
            name = "createInstance",
            returnType = CgClassId(objectClassId, isNullable = true),
            arguments = arrayOf(stringClassId)
        )

    val createArrayMethodId: MethodId
        get() = utilClassId.utilMethodId(
            name = "createArray",
            returnType = Array<Any>::class.id,
            arguments = arrayOf(stringClassId, intClassId, Array<Any>::class.id)
        )

    val setFieldMethodId: MethodId
        get() = utilClassId.utilMethodId(
            name = "setField",
            returnType = voidClassId,
            arguments = arrayOf(objectClassId, stringClassId, stringClassId, objectClassId)
        )

    val setStaticFieldMethodId: MethodId
        get() = utilClassId.utilMethodId(
            name = "setStaticField",
            returnType = voidClassId,
            arguments = arrayOf(Class::class.id, stringClassId, objectClassId)
        )

    val getFieldValueMethodId: MethodId
        get() = utilClassId.utilMethodId(
            name = "getFieldValue",
            returnType = objectClassId,
            arguments = arrayOf(objectClassId, stringClassId, stringClassId)
        )

    val getStaticFieldValueMethodId: MethodId
        get() = utilClassId.utilMethodId(
            name = "getStaticFieldValue",
            returnType = objectClassId,
            arguments = arrayOf(Class::class.id, stringClassId)
        )

    val getEnumConstantByNameMethodId: MethodId
        get() = utilClassId.utilMethodId(
            name = "getEnumConstantByName",
            returnType = objectClassId,
            arguments = arrayOf(Class::class.id, stringClassId)
        )

    val deepEqualsMethodId: MethodId
        get() = utilClassId.utilMethodId(
            name = "deepEquals",
            returnType = booleanClassId,
            arguments = arrayOf(objectClassId, objectClassId)
        )

    val arraysDeepEqualsMethodId: MethodId
        get() = utilClassId.utilMethodId(
            name = "arraysDeepEquals",
            returnType = booleanClassId,
            arguments = arrayOf(objectClassId, objectClassId)
        )

    val iterablesDeepEqualsMethodId: MethodId
        get() = utilClassId.utilMethodId(
            name = "iterablesDeepEquals",
            returnType = booleanClassId,
            arguments = arrayOf(java.lang.Iterable::class.id, java.lang.Iterable::class.id)
        )

    val streamsDeepEqualsMethodId: MethodId
        get() = utilClassId.utilMethodId(
            name = "streamsDeepEquals",
            returnType = booleanClassId,
            arguments = arrayOf(java.util.stream.BaseStream::class.id, java.util.stream.BaseStream::class.id)
        )

    val mapsDeepEqualsMethodId: MethodId
        get() = utilClassId.utilMethodId(
            name = "mapsDeepEquals",
            returnType = booleanClassId,
            arguments = arrayOf(java.util.Map::class.id, java.util.Map::class.id)
        )

    val hasCustomEqualsMethodId: MethodId
        get() = utilClassId.utilMethodId(
            name = "hasCustomEquals",
            returnType = booleanClassId,
            arguments = arrayOf(Class::class.id)
        )

    val getArrayLengthMethodId: MethodId
        get() = utilClassId.utilMethodId(
            name = "getArrayLength",
            returnType = intClassId,
            arguments = arrayOf(objectClassId)
        )

    val consumeBaseStreamMethodId: MethodId
        get() = utilClassId.utilMethodId(
            name = "consumeBaseStream",
            returnType = voidClassId,
            arguments = arrayOf(baseStreamClassId)
        )

    val buildStaticLambdaMethodId: MethodId
        get() = utilClassId.utilMethodId(
            name = "buildStaticLambda",
            returnType = objectClassId,
            arguments = arrayOf(
                classClassId,
                classClassId,
                stringClassId,
                arrayTypeOf(capturedArgumentClassId)
            )
        )

    val buildLambdaMethodId: MethodId
        get() = utilClassId.utilMethodId(
            name = "buildLambda",
            returnType = objectClassId,
            arguments = arrayOf(
                classClassId,
                classClassId,
                stringClassId,
                objectClassId,
                arrayTypeOf(capturedArgumentClassId)
            )
        )

    val getLookupInMethodId: MethodId
        get() = utilClassId.utilMethodId(
            name = "getLookupIn",
            returnType = MethodHandles.Lookup::class.id,
            arguments = arrayOf(classClassId)
        )

    val getLambdaCapturedArgumentTypesMethodId: MethodId
        get() = utilClassId.utilMethodId(
            name = "getLambdaCapturedArgumentTypes",
            returnType = arrayTypeOf(classClassId),
            arguments = arrayOf(arrayTypeOf(capturedArgumentClassId))
        )

    val getLambdaCapturedArgumentValuesMethodId: MethodId
        get() = utilClassId.utilMethodId(
            name = "getLambdaCapturedArgumentValues",
            returnType = objectArrayClassId,
            arguments = arrayOf(arrayTypeOf(capturedArgumentClassId))
        )

    val getInstantiatedMethodTypeMethodId: MethodId
        get() = utilClassId.utilMethodId(
            name = "getInstantiatedMethodType",
            returnType = MethodType::class.id,
            arguments = arrayOf(Method::class.id, arrayTypeOf(classClassId))
        )

    val getLambdaMethodMethodId: MethodId
        get() = utilClassId.utilMethodId(
            name = "getLambdaMethod",
            returnType = Method::class.id,
            arguments = arrayOf(classClassId, stringClassId)
        )

    val getSingleAbstractMethodMethodId: MethodId
        get() = utilClassId.utilMethodId(
            name = "getSingleAbstractMethod",
            returnType = java.lang.reflect.Method::class.id,
            arguments = arrayOf(classClassId)
        )

    val capturedArgumentClassId: BuiltinClassId
        get() = BuiltinClassId(
            canonicalName = "${utilClassId.name}.CapturedArgument",
            simpleName = "CapturedArgument"
        )

    val capturedArgumentConstructorId: BuiltinConstructorId
        get() = builtinConstructorId(capturedArgumentClassId, classClassId, objectClassId)
}

/**
 * This provider represents an util class file that is generated and put into the user's test module.
 * The generated class is UtUtils (its id is defined at [utJavaUtilsClassId] or [utKotlinUtilsClassId]).
 *
 * Content of this util class may be different (due to mocks in deepEquals), but the methods (and their ids) are the same.
 */
internal class UtilClassFileMethodProvider(language: CodegenLanguage)
    : UtilMethodProvider(selectUtilClassId(language)) {
    /**
     * This property contains the current version of util class.
     * This version will be written to the util class file inside a comment.
     *
     * Whenever we want to create an util class, we first check if there is an already existing one.
     * If there is, then we decide whether we need to overwrite it or not. One of the factors here
     * is the version of this existing class. If the version of existing class is older than the one
     * that is currently stored in [UtilClassFileMethodProvider.UTIL_CLASS_VERSION], then we need to
     * overwrite an util class, because it might have been changed in the new version.
     *
     * **IMPORTANT** if you make any changes to util methods (see [utilMethodTextById]), do not forget to update this version.
     */
    val UTIL_CLASS_VERSION = "2.1"
}

internal class TestClassUtilMethodProvider(testClassId: ClassId) : UtilMethodProvider(testClassId)

internal fun selectUtilClassId(codegenLanguage: CodegenLanguage): ClassId =
    when (codegenLanguage) {
        CodegenLanguage.JAVA -> utJavaUtilsClassId
        CodegenLanguage.KOTLIN -> utKotlinUtilsClassId
    }

internal val utJavaUtilsClassId: ClassId
    get() = BuiltinClassId(
        canonicalName = UT_UTILS_BASE_PACKAGE_NAME + PACKAGE_DELIMITER + "java" + PACKAGE_DELIMITER + "UtUtils",
        simpleName = "UtUtils",
        isFinal = true,
    )

internal val utKotlinUtilsClassId: ClassId
    get() = BuiltinClassId(
        canonicalName = UT_UTILS_BASE_PACKAGE_NAME + PACKAGE_DELIMITER + "kotlin" + PACKAGE_DELIMITER + "UtUtils",
        simpleName = "UtUtils",
        isFinal = true,
        isKotlinObject = true
    )

/**
 * [MethodId] for [AutoCloseable.close].
 */
val openMocksMethodId = MethodId(
    classId = MockitoAnnotations::class.id,
    name = "openMocks",
    returnType = AutoCloseable::class.java.id,
    parameters = listOf(objectClassId),
)

val closeMethodId = MethodId(
    classId = AutoCloseable::class.java.id,
    name = "close",
    returnType = voidClassId,
    parameters = emptyList(),
)

private val clearCollectionMethodId = MethodId(
    classId = Collection::class.java.id,
    name = "clear",
    returnType = voidClassId,
    parameters = emptyList()
)

private val clearMapMethodId = MethodId(
    classId = Map::class.java.id,
    name = "clear",
    returnType = voidClassId,
    parameters = emptyList()
)

fun clearMethodId(javaClass: Class<*>): MethodId = when {
    Collection::class.java.isAssignableFrom(javaClass) -> clearCollectionMethodId
    Map::class.java.isAssignableFrom(javaClass) -> clearMapMethodId
    else -> error("Clear method is not implemented for $javaClass")
}

val mocksAutoCloseable: Set<ClassId> = setOf(
    MockitoStaticMocking.mockedStaticClassId,
    MockitoStaticMocking.mockedConstructionClassId
)

val predefinedAutoCloseable: Set<ClassId> = mocksAutoCloseable

/**
 * Checks if this class is marked as auto closeable
 * (useful for classes that could not be loaded by class loader like mocks for mocking statics from Mockito Inline).
 */
internal val ClassId.isPredefinedAutoCloseable: Boolean
    get() = this in predefinedAutoCloseable

/**
 * Returns [AutoCloseable.close] method id for all auto closeable.
 * and predefined as auto closeable via [isPredefinedAutoCloseable], and null otherwise.
 * Null always for [BuiltinClassId].
 */
internal val ClassId.closeMethodIdOrNull: MethodId?
    get() = when {
        isPredefinedAutoCloseable -> closeMethodId
        this is BuiltinClassId -> null
        else -> (jClass as? AutoCloseable)?.let { closeMethodId }
    }
