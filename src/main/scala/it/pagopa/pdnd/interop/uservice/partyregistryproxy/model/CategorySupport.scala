package it.pagopa.pdnd.interop.uservice.partyregistryproxy.model

import spray.json.{DefaultJsonProtocol, JsArray, JsValue}
import spray.json._
import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Paths}

object CategorySupport extends DefaultJsonProtocol {
  def load(): Categories = {
    val path          = Paths.get("src/main/resources/categorie_ipa.json")
    val content       = Files.readString(path, StandardCharsets.UTF_8)
    val json: JsValue = content.parseJson
    val cats: Set[Category] = json.asJsObject.fields("records") match {
      case JsArray(elements) =>
        elements.flatMap {
          case JsArray(elements) =>
            Some(Category(elements(1).convertTo[String], elements(2).convertTo[String], elements(3).convertTo[String]))
          case _ => None
        }.toSet
      case _ => Set.empty[Category]
    }
    Categories(cats.toSeq)
  }
}
