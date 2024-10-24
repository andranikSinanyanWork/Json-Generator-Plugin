package com.easytools.jsonscheme.source

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.SelectionModel
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiField
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.PsiUtilBase

class MainPlugin : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val selectionModel: SelectionModel = editor.selectionModel
        val selectedText = selectionModel.selectedText

        val finalFilePath = getPackageName(project, editor).run {
            "$this.$selectedText".replace("package", "").replace("\\s+".toRegex(), "")
        }

        val clazz = getPsiClassFromQualifiedName(project = project, qualifiedName = finalFilePath)
        if(clazz == null){
            onError()
        }
        val json = getJsonFromClass(clazz = clazz ?: return).first

        SaveFileDialog().showSaveFileDialog(project, makeSchemeStructured(json).toString())

    }

    private fun getPsiClassFromQualifiedName(project: Project, qualifiedName: String): PsiClass? {
        return JavaPsiFacade.getInstance(project).findClass(qualifiedName, GlobalSearchScope.allScope(project))
    }

    private fun getPackageName(project: Project, editor: Editor): String? {
        val psiFile = PsiUtilBase.getPsiFileInEditor(editor, project) ?: return null

        val virtualFile = psiFile.virtualFile ?: return null
        return readFirstLine(virtualFile)
    }

    private fun readFirstLine(virtualFile: VirtualFile): String? {
        return try {
            virtualFile.inputStream.bufferedReader().use { reader ->
                reader.lineSequence()
                    .firstOrNull { it.trimStart().startsWith("package") }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }



    private fun getJsonFromClass(
        clazz: PsiClass,
        visitedClasses: MutableSet<PsiClass> = mutableSetOf()
    ): Pair<JsonObject, MutableSet<String>> {
        if (!visitedClasses.add(clazz)) return Pair(JsonObject(), mutableSetOf())

        val jsonObject = JsonObject()
        val requiredFieldsObject = mutableSetOf<String>()


        clazz.allFields.forEach { field ->
            val key = getSerializedNameAnnotation(field) ?: return@forEach
            val fieldType = field.type

            val newJsonObject = JsonObject()

            when {
                fieldType.isEnumType() -> {
                    newJsonObject.addProperty("type", "string")
                    val enumValues = JsonArray().apply {
                        getEnumValues((fieldType as PsiClassType).resolve() ?: return@forEach)
                    }
                    newJsonObject.add("enum", enumValues)
                }
                isPsiTypeList(fieldType) -> {
                    val listsItemType = getListElementType(fieldType)
                    val elementTypeJson = if (listsItemType.isPrimitiveOrWrapperType() || listsItemType?.presentableText == "String") {
                        JsonObject().apply { addProperty("type", listsItemType?.presentableText?.toLowerCase()) }

                    } else {
                        createTypeJson((listsItemType as PsiClassType).resolve() ?: return@forEach, visitedClasses)
                    }
                    newJsonObject.addProperty("type", "array")
                    newJsonObject.add("items", elementTypeJson)
                }
                fieldType.isPrimitiveOrWrapperType() || fieldType.presentableText == "String"  -> {
                    newJsonObject.addProperty("type", fieldType.presentableText.toLowerCase())
                }
                else -> {
                    val nestedJson = getJsonFromClass((fieldType as PsiClassType).resolve()!!, visitedClasses)
                    newJsonObject.addProperty("type", "object")
                    newJsonObject.add("properties", nestedJson.first)
                    val tmpJsonArray = JsonArray()
                    nestedJson.second.forEach {
                        tmpJsonArray.add(it)
                    }
                    newJsonObject.addProperty("additionalProperties" , false)
                }
            }
            newJsonObject.addProperty("description", "")
            jsonObject.add(key, newJsonObject)
        }
        visitedClasses.remove(clazz)
        return Pair(jsonObject, requiredFieldsObject)
    }

    private fun makeSchemeStructured(generatedObject: JsonObject, ): JsonObject {
        val newJson = JsonObject()
        newJson.addProperty("\$schema", "http://json-schema.org/draft-07/schema")
        newJson.addProperty("type" , "object")
        newJson.add("properties", generatedObject)
        newJson.addProperty("additionalProperties", false)
        return newJson
    }

    private fun createTypeJson(clazz: PsiClass, visitedClasses: MutableSet<PsiClass>): JsonObject {
        val data = getJsonFromClass(clazz, visitedClasses)
        val newJson = JsonObject()
        newJson.addProperty("type", "object")
        newJson.add("properties", data.first)
        val tmpJsonArray = JsonArray()
        data.second.forEach {
            tmpJsonArray.add(it)
        }
        return newJson
    }

    private fun getSerializedNameAnnotation(psiField: PsiField): String? {
        // Iterate over all annotations on the field
        for (annotation in psiField.annotations) {
            // Check if the annotation is `SerializedName`
            if (annotation.qualifiedName == "com.google.gson.annotations.SerializedName") {
                // Get the value of the annotation
                val annotationValue = annotation.findAttributeValue("value")
                if (annotationValue != null) {
                    return annotationValue.text.trim('"')
                }
            }
        }
        return null
    }

    private fun onError() {
        Messages.showErrorDialog("Error occurred: Something went wrong please try again", "Error")
    }
}