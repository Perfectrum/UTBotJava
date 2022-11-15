package org.utbot.python.framework.codegen.model.constructor.tree

import fj.data.Either
import org.utbot.framework.codegen.domain.context.CgContext
import org.utbot.framework.codegen.domain.context.CgContextOwner
import org.utbot.framework.codegen.domain.models.CgAnnotation
import org.utbot.framework.codegen.domain.models.CgAnonymousFunction
import org.utbot.framework.codegen.domain.models.CgComment
import org.utbot.framework.codegen.domain.models.CgDeclaration
import org.utbot.framework.codegen.domain.models.CgEmptyLine
import org.utbot.framework.codegen.domain.models.CgExpression
import org.utbot.framework.codegen.domain.models.CgIfStatement
import org.utbot.framework.codegen.domain.models.CgInnerBlock
import org.utbot.framework.codegen.domain.models.CgIsInstance
import org.utbot.framework.codegen.domain.models.CgLogicalAnd
import org.utbot.framework.codegen.domain.models.CgLogicalOr
import org.utbot.framework.codegen.domain.models.CgMultilineComment
import org.utbot.framework.codegen.domain.models.CgMultipleArgsAnnotation
import org.utbot.framework.codegen.domain.models.CgNamedAnnotationArgument
import org.utbot.framework.codegen.domain.models.CgParameterDeclaration
import org.utbot.framework.codegen.domain.models.CgReturnStatement
import org.utbot.framework.codegen.domain.models.CgSingleArgAnnotation
import org.utbot.framework.codegen.domain.models.CgSingleLineComment
import org.utbot.framework.codegen.domain.models.CgThrowStatement
import org.utbot.framework.codegen.domain.models.CgTryCatch
import org.utbot.framework.codegen.domain.models.CgVariable
import org.utbot.framework.codegen.services.access.CgCallableAccessManager
import org.utbot.framework.codegen.tree.CgForEachLoopBuilder
import org.utbot.framework.codegen.tree.CgForLoopBuilder
import org.utbot.framework.codegen.tree.CgStatementConstructor
import org.utbot.framework.codegen.tree.CgTestClassConstructor.CgComponents.getCallableAccessManagerBy
import org.utbot.framework.codegen.tree.CgTestClassConstructor.CgComponents.getNameGeneratorBy
import org.utbot.framework.codegen.tree.ExpressionWithType
import org.utbot.framework.codegen.tree.buildAssignment
import org.utbot.framework.codegen.tree.buildCgForEachLoop
import org.utbot.framework.codegen.tree.buildDeclaration
import org.utbot.framework.codegen.tree.buildDoWhileLoop
import org.utbot.framework.codegen.tree.buildExceptionHandler
import org.utbot.framework.codegen.tree.buildForLoop
import org.utbot.framework.codegen.tree.buildTryCatch
import org.utbot.framework.codegen.tree.buildWhileLoop
import org.utbot.framework.codegen.util.isAccessibleFrom
import org.utbot.framework.codegen.util.resolve
import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.ExecutableId
import org.utbot.framework.plugin.api.FieldId
import org.utbot.framework.plugin.api.UtModel
import org.utbot.framework.plugin.api.util.objectClassId
import org.utbot.python.framework.codegen.model.constructor.util.plus
import java.util.*

class PythonCgStatementConstructorImpl(context: CgContext) :
    CgStatementConstructor,
    CgContextOwner by context,
    CgCallableAccessManager by getCallableAccessManagerBy(context) {

    private val nameGenerator = getNameGeneratorBy(context)

    override fun newVar(
        baseType: ClassId,
        model: UtModel?,
        baseName: String?,
        isMock: Boolean,
        isMutable: Boolean,
        init: () -> CgExpression
    ): CgVariable {
        val declarationOrVar: Either<CgDeclaration, CgVariable> =
            createDeclarationForNewVarAndUpdateVariableScopeOrGetExistingVariable(
                baseType,
                model,
                baseName,
                isMock,
                isMutable,
                init
            )

        return declarationOrVar.either(
            { declaration ->
                currentBlock += declaration

                declaration.variable
            },
            { variable -> variable }
        )
    }

    override fun createDeclarationForNewVarAndUpdateVariableScopeOrGetExistingVariable(
        baseType: ClassId,
        model: UtModel?,
        baseName: String?,
        isMock: Boolean,
        isMutableVar: Boolean,
        init: () -> CgExpression
    ): Either<CgDeclaration, CgVariable> {
        val baseExpr = init()

        val name = nameGenerator.variableName(baseType, baseName)
        val (type, expr) = guardExpression(baseType, baseExpr)

        val declaration = buildDeclaration {
            variableType = type
            variableName = name
            initializer = expr
        }
        updateVariableScope(declaration.variable, model)
        return Either.left(declaration)
    }

    override fun CgExpression.`=`(value: Any?) {
        currentBlock += buildAssignment {
            lValue = this@`=`
            rValue = value.resolve()
        }
    }

    override fun CgExpression.and(other: CgExpression): CgLogicalAnd =
        CgLogicalAnd(this, other)

    override fun CgExpression.or(other: CgExpression): CgLogicalOr =
        CgLogicalOr(this, other)

    override fun ifStatement(
        condition: CgExpression,
        trueBranch: () -> Unit,
        falseBranch: (() -> Unit)?
    ): CgIfStatement {
        val trueBranchBlock = block(trueBranch)
        val falseBranchBlock = falseBranch?.let { block(it) }
        return CgIfStatement(condition, trueBranchBlock, falseBranchBlock).also {
            currentBlock += it
        }
    }

    override fun forLoop(init: CgForLoopBuilder.() -> Unit) {
        currentBlock += buildForLoop(init)
    }

    override fun whileLoop(condition: CgExpression, statements: () -> Unit) {
        currentBlock += buildWhileLoop {
            this.condition = condition
            this.statements += block(statements)
        }
    }

    override fun doWhileLoop(condition: CgExpression, statements: () -> Unit) {
        currentBlock += buildDoWhileLoop {
            this.condition = condition
            this.statements += block(statements)
        }
    }

    override fun forEachLoop(init: CgForEachLoopBuilder.() -> Unit) = withNameScope {
        currentBlock += buildCgForEachLoop(init)
    }

    override fun getClassOf(classId: ClassId): CgExpression {
        TODO("Not yet implemented")
    }

    override fun createFieldVariable(fieldId: FieldId): CgVariable {
        TODO("Not yet implemented")
    }

    override fun createExecutableVariable(executableId: ExecutableId, arguments: List<CgExpression>): CgVariable {
        TODO("Not yet implemented")
    }

    override fun tryBlock(init: () -> Unit): CgTryCatch = tryBlock(init, null)

    override fun tryBlock(init: () -> Unit, resources: List<CgDeclaration>?): CgTryCatch =
        buildTryCatch {
            statements = block(init)
            this.resources = resources
        }

    override fun CgTryCatch.catch(exception: ClassId, init: (CgVariable) -> Unit): CgTryCatch {
        val newHandler = buildExceptionHandler {
            val e = declareVariable(exception, nameGenerator.variableName(exception.simpleName.replaceFirstChar {
                it.lowercase(
                    Locale.getDefault()
                )
            }))
            this.exception = e
            this.statements = block { init(e) }
        }
        return this.copy(handlers = handlers + newHandler)
    }

    override fun CgTryCatch.finally(init: () -> Unit): CgTryCatch {
        val finallyBlock = block(init)
        return this.copy(finally = finallyBlock)
    }

    override fun CgExpression.isInstance(value: CgExpression): CgIsInstance = TODO("Not yet implemented")

    override fun innerBlock(init: () -> Unit): CgInnerBlock =
        CgInnerBlock(block(init)).also {
            currentBlock += it
        }

    override fun comment(text: String): CgComment =
        CgSingleLineComment(text).also {
            currentBlock += it
        }

    override fun comment(): CgComment =
        CgSingleLineComment("").also {
            currentBlock += it
        }

    override fun multilineComment(lines: List<String>): CgComment =
        CgMultilineComment(lines).also {
            currentBlock += it
        }

    override fun lambda(type: ClassId, vararg parameters: CgVariable, body: () -> Unit): CgAnonymousFunction {
        return withNameScope {
            for (parameter in parameters) {
                declareParameter(parameter.type, parameter.name)
            }
            val paramDeclarations = parameters.map { CgParameterDeclaration(it) }
            CgAnonymousFunction(type, paramDeclarations, block(body))
        }
    }

    override fun annotation(classId: ClassId, argument: Any?): CgAnnotation {
        val annotation = CgSingleArgAnnotation(classId, argument.resolve())
        addAnnotation(annotation)
        return annotation
    }

    override fun annotation(classId: ClassId, namedArguments: List<Pair<String, CgExpression>>): CgAnnotation {
        val annotation = CgMultipleArgsAnnotation(
            classId,
            namedArguments.mapTo(mutableListOf()) { (name, value) -> CgNamedAnnotationArgument(name, value) }
        )
        addAnnotation(annotation)
        return annotation
    }

    override fun annotation(
        classId: ClassId,
        buildArguments: MutableList<Pair<String, CgExpression>>.() -> Unit
    ): CgAnnotation {
        val arguments = mutableListOf<Pair<String, CgExpression>>()
            .apply(buildArguments)
            .map { (name, value) -> CgNamedAnnotationArgument(name, value) }
        val annotation = CgMultipleArgsAnnotation(classId, arguments.toMutableList())
        addAnnotation(annotation)
        return annotation
    }

    override fun returnStatement(expression: () -> CgExpression) {
        currentBlock += CgReturnStatement(expression())
    }

    override fun throwStatement(exception: () -> CgExpression): CgThrowStatement =
        CgThrowStatement(exception()).also { currentBlock += it }

    override fun emptyLine() {
        currentBlock += CgEmptyLine()
    }

    override fun emptyLineIfNeeded() {
        val lastStatement = currentBlock.lastOrNull() ?: return
        if (lastStatement is CgEmptyLine) return
        emptyLine()
    }

    override fun declareVariable(type: ClassId, name: String): CgVariable =
        CgVariable(name, type).also {
            updateVariableScope(it)
        }

    override fun guardExpression(baseType: ClassId, expression: CgExpression): ExpressionWithType {
        return ExpressionWithType(baseType, expression)
    }

    override fun wrapTypeIfRequired(baseType: ClassId): ClassId =
        if (baseType.isAccessibleFrom(testClassPackageName)) baseType else objectClassId
}