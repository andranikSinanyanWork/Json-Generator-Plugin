package com.easytools.jsonscheme.source

import com.intellij.psi.*
import com.intellij.psi.util.PsiUtil

fun isPsiTypeList(type: PsiType): Boolean {
    if (type is PsiClassType) {
        val resolvedClass = type.resolve()
        if (resolvedClass != null && resolvedClass.qualifiedName == "java.util.List") {
            return true
        }
    }
    return false
}

fun PsiType.isEnumType(): Boolean {
    val psiClass = PsiUtil.resolveClassInType(this)
    return psiClass?.isEnum ?: false
}

fun getEnumValues(psiClass: PsiClass): List<String> {
    val enumConstants = mutableListOf<String>()

    if (psiClass.isEnum) {
        for (field in psiClass.fields) {
            if (field is PsiField && field.hasModifierProperty("static") && field.hasModifierProperty("final")) {
                enumConstants.add(field.name)
            }
        }
    }

    return enumConstants
}


fun getListElementType(listType: PsiType): PsiType? {
    if (listType is PsiClassType) {
        val resolvedClass = listType.resolve()
        if (resolvedClass != null && (resolvedClass.qualifiedName == "java.util.List" ||
                    resolvedClass.qualifiedName == "java.util.Collection")
        ) {

            val parameters = listType.parameters
            if (parameters.isNotEmpty()) {
                return parameters[0]
            }
        }
    }
    return null
}

val primitiveWrapperTypes = setOf(
    "java.lang.Boolean",
    "java.lang.Byte",
    "java.lang.Short",
    "java.lang.Integer",
    "java.lang.Long",
    "java.lang.Float",
    "java.lang.Double",
    "java.lang.Character",
    "java.lang.String"
)

fun PsiType?.isPrimitiveOrWrapperType(): Boolean {
    return this is PsiPrimitiveType || primitiveWrapperTypes.contains(this?.canonicalText ?: return false)
}
