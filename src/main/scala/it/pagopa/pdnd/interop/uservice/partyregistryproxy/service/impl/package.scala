package it.pagopa.pdnd.interop.uservice.partyregistryproxy.service

import it.pagopa.pdnd.interop.uservice.partyregistryproxy.model.Institution
import org.apache.lucene.document.{Document, Field, StoredField, StringField, TextField}

import javax.naming.directory.SearchResult

package object impl {
  @SuppressWarnings(Array("org.wartremover.warts.AsInstanceOf"))
  implicit class SearchResultOps(val result: SearchResult) extends AnyVal {
    def extract(attributeName: String): Option[String] = {
      Option(result.getAttributes.get(attributeName)).map(_.get().asInstanceOf[String])

    }
  }

  object InstitutionFields {
    final val ID                 = "id"
    final val O                  = "o"
    final val OU                 = "ou"
    final val AOO                = "aoo"
    final val TAXCODE            = "taxCode"
    final val ADMINISTRATIONCODE = "administrationCode"
    final val CATEGORY           = "category"
    final val MANAGERNAME        = "managerName"
    final val MANAGERSURNAME     = "managerSurname"
    final val DESCRIPTION        = "description"
    final val DIGITALADDRESS     = "digitalAddress"
  }

  implicit class DocumentOps(val document: Document) extends AnyVal {
    def addOptional(fieldName: String, opt: Option[String]): Unit =
      opt.foreach(value => document.add(new StoredField(fieldName, value)))

    def toInstitution: Institution = {
      Institution(
        id = document.get(InstitutionFields.ID),
        o = Option(document.get(InstitutionFields.O)),
        ou = Option(document.get(InstitutionFields.OU)),
        aoo = Option(document.get(InstitutionFields.AOO)),
        taxCode = Option(document.get(InstitutionFields.TAXCODE)),
        administrationCode = Option(document.get(InstitutionFields.ADMINISTRATIONCODE)),
        category = Option(document.get(InstitutionFields.CATEGORY)),
        managerName = Option(document.get(InstitutionFields.MANAGERNAME)),
        managerSurname = Option(document.get(InstitutionFields.MANAGERSURNAME)),
        description = document.get(InstitutionFields.DESCRIPTION),
        digitalAddress = Option(document.get(InstitutionFields.DIGITALADDRESS))
      )
    }
  }

  implicit class InstitutionOps(val institution: Institution) extends AnyVal {
    def toDocument: Document = {
      val doc = new Document
      doc.add(new StringField(InstitutionFields.ID, institution.id, Field.Store.YES))
      doc.add(new TextField(InstitutionFields.DESCRIPTION, institution.description, Field.Store.YES))

      doc.addOptional(InstitutionFields.O, institution.o)
      doc.addOptional(InstitutionFields.OU, institution.ou)
      doc.addOptional(InstitutionFields.AOO, institution.aoo)
      doc.addOptional(InstitutionFields.TAXCODE, institution.taxCode)
      doc.addOptional(InstitutionFields.ADMINISTRATIONCODE, institution.administrationCode)
      doc.addOptional(InstitutionFields.CATEGORY, institution.category)
      doc.addOptional(InstitutionFields.MANAGERNAME, institution.managerName)
      doc.addOptional(InstitutionFields.MANAGERSURNAME, institution.managerSurname)
      doc.addOptional(InstitutionFields.DIGITALADDRESS, institution.digitalAddress)

      doc
    }
  }

}
