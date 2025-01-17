package io.wunderschild.country_codes

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import CountryHelpers._

import scala.io.Source

object ISOCountryCodes {
  private def using[A, B <: {def close(): Unit}](closeable: B)(f: B => A): A = {
    try {
      f(closeable)
    } finally {
      closeable.close()
    }
  }

  /** Create lookup table that can be used for fast search of country by name.
   *
   * @param localization language to be used for the country names
   */
  def apply(
    localization: String = "en",
    indexedFields: Seq[String] = Seq("officialName", "otherNames", "nationality")
  ): LookupTable = {
    type mT = Map[String, Map[String, Any]]

    val mapper = new ObjectMapper(new YAMLFactory())
    mapper.registerModule(DefaultScalaModule)

    val countriesPath = "/countries"

    val countryDataPaths = using(getClass.getResourceAsStream("/countries/hint")) { stream =>
      Source.fromInputStream(stream).getLines.map(
        country => s"${countriesPath}/${country}.yaml"
      ).toList
    }

    def readMap(path: String): mT = using(getClass.getResourceAsStream(path)) {
      stream => mapper.readValue(stream, classOf[mT])
    }

    val localizationPath = s"/localization/${localization.toLowerCase}.yaml"
    val localizationMap = readMap(localizationPath)
    val countryDataMap = countryDataPaths.map { countryDataPath =>
      readMap(countryDataPath).mapValues(data => {
        (data ++ localizationMap(data("alpha2").asInstanceOf[String])).toCaseClass[Country]
      })
    }

    new LookupTable(countryDataMap.flatMap(_.values), indexedFields)
  }
}
