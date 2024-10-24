package com.easytools.jsonscheme.source

import com.intellij.openapi.fileChooser.FileSaverDescriptor
import com.intellij.openapi.fileChooser.FileSaverDialog
import com.intellij.openapi.fileChooser.FileChooserFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VirtualFileWrapper
import com.intellij.openapi.vfs.VfsUtil

class SaveFileDialog {
    fun showSaveFileDialog(project: Project, content: String) {
        val dialogTitle = "Save File"
        val dialogDescription = "Choose the location and file name to save the file"

        val descriptor = FileSaverDescriptor(dialogTitle, dialogDescription, "json")

        val baseDir = VfsUtil.getUserHomeDir()

        val fileSaver: FileSaverDialog = FileChooserFactory.getInstance().createSaveFileDialog(descriptor, project)
        val wrapper: VirtualFileWrapper? = fileSaver.save(baseDir, "myfile.json") // Default file name

        if (wrapper != null) {
            val selectedFile = wrapper.file

            try {
                selectedFile.writeText(content)
                Messages.showInfoMessage("File saved successfully at: ${selectedFile.absolutePath}", "Success")
            } catch (e: Exception) {
                Messages.showErrorDialog("Error saving file: ${e.message}", "Error")
            }
        } else {
            Messages.showWarningDialog("No file selected.", "Warning")
        }
    }
}
