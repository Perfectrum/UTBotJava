package org.utbot.python.fuzzing.provider.utils

import org.utbot.fuzzing.Seed
import org.utbot.python.framework.api.python.PythonTree
import org.utbot.python.fuzzing.PythonFuzzedValue
import org.utbot.python.fuzzing.PythonMethodDescription
import org.utbot.python.newtyping.PythonAnyTypeDescription
import org.utbot.python.newtyping.PythonConcreteCompositeTypeDescription
import org.utbot.python.newtyping.PythonDefinition
import org.utbot.python.newtyping.PythonSubtypeChecker
import org.utbot.python.newtyping.PythonTypeStorage
import org.utbot.python.newtyping.PythonVariableDescription
import org.utbot.python.newtyping.general.Type
import org.utbot.python.newtyping.getPythonAttributeByName

fun Type.isAny(): Boolean {
    return meta is PythonAnyTypeDescription
}

fun getSuitableConstantsFromCode(description: PythonMethodDescription, type: Type): List<Seed<Type, PythonFuzzedValue>> {
    return description.concreteValues.filter {
        PythonSubtypeChecker.checkIfRightIsSubtypeOfLeft(type, it.type, description.pythonTypeStorage)
    }.mapNotNull { value ->
        PythonTree.fromParsedConstant(Pair(value.type, value.value))?.let {
            Seed.Simple(PythonFuzzedValue(it))
        }
    }
}

fun isConcreteType(type: Type): Boolean {
    return (type.meta as? PythonConcreteCompositeTypeDescription)?.isAbstract == false
}

fun PythonDefinition.isProtected(): Boolean {
    val name = this.meta.name
    return name.startsWith("_") && !this.isMagic()
}

fun PythonDefinition.isPrivate(): Boolean {
    val name = this.meta.name
    return name.startsWith("__") && !this.isMagic()
}

fun PythonDefinition.isMagic(): Boolean {
    return this.meta.name.startsWith("__") && this.meta.name.endsWith("__") && this.meta.name.length >= 4
}

fun PythonDefinition.isProperty(): Boolean {
    return (this.meta as? PythonVariableDescription)?.isProperty == true
}

fun PythonDefinition.isCallable(typeStorage: PythonTypeStorage): Boolean {
    return this.type.getPythonAttributeByName(typeStorage, "__call__") != null
}