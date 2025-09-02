package apollo.kotlin.compiler.plugin

import org.jetbrains.kotlin.name.Name

object Identifiers {
  val Data = Name.identifier("Data")
  val value = Name.identifier("value")
  val element = Name.identifier("element")
  val compiledDocumentName = Name.identifier("compiledDocument")
  val dataAdapterName = Name.identifier("dataAdapter")
  val variablesName = Name.identifier("variables")
  val Adapter= Name.identifier("Adapter")
  val fromJson = Name.identifier("fromJson")
  val toJson = Name.identifier("toJson")

}