package com.idrnd.idvoice.utils

import java.io.File

/**
 * Voice template file creator.
 *
 * @param folderWithTemplates folder with templates.
 */
class TemplateFileCreator(private val folderWithTemplates: File) {

    /**
     * Create empty template file in storage.
     *
     * @param filename name of saved file.
     * @param override if template file exists and override param is true then override it else return an existed file.
     */
    fun createTemplateFile(filename: String, override: Boolean): File {
        val templateFile = File(folderWithTemplates, "$filename.bin")

        if (templateFile.exists() && override) {
            templateFile.delete()
        }

        if (!templateFile.exists()) {
            templateFile.createNewFile()
        }

        return templateFile
    }
}
