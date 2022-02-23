package it.pagopa.interop.partyregistryproxy.service.impl

import shapeless.{:+:, CNil}

package object util {
  type RecordValue = Int :+: String :+: CNil
}
