package org.dbpedia.extraction.mappings

import java.util.logging.Logger
import org.dbpedia.extraction.ontology.datatypes._
import org.dbpedia.extraction.dataparser._
import org.dbpedia.extraction.destinations.{ DBpediaDatasets, Quad }
import org.dbpedia.extraction.util.Language
import org.dbpedia.extraction.ontology._
import java.lang.IllegalArgumentException
import org.dbpedia.extraction.wikiparser.TemplateNode
import org.dbpedia.extraction.ontology.{ OntologyDatatypeProperty, OntologyClass, OntologyProperty, DBpediaNamespace }
import scala.collection.mutable.ArrayBuffer
import scala.language.reflectiveCalls
import ch.weisenburger.dbpedia.extraction.mappings._
import scala.Some

class SimplePropertyMapping(
  val templateProperty: String, // IntermediateNodeMapping and CreateMappingStats requires this to be public
  ontologyProperty: OntologyProperty,
  select: String,
  unit: Datatype,
  private var language: Language,
  factor: Double,
  context: {
    def ontology: Ontology
    def redirects: Redirects // redirects required by DateTimeParser and UnitValueParser
    def language: Language
    def timexParser: DataParser
  })
  extends PropertyMapping {
  
  private val logger = Logger.getLogger(classOf[SimplePropertyMapping].getName)
  
  val selector: List[_] => List[_] =
    select match {
      case "first" => _.take(1)
      case "last" => _.reverse.take(1)
      case null => identity
      case _ => throw new IllegalArgumentException("Only 'first' or 'last' are allowed in property 'select'")
    }

  if (language == null) language = context.language

  ontologyProperty match {
    case datatypeProperty: OntologyDatatypeProperty =>
      {
        //Check if unit is compatible to the range of the ontology property
        (unit, datatypeProperty.range) match {
          case (dt1: UnitDatatype, dt2: UnitDatatype) => require(dt1.dimension == dt2.dimension,
            "Unit must conform to the dimension of the range of the ontology property")

          case (dt1: UnitDatatype, dt2: DimensionDatatype) => require(dt1.dimension == dt2,
            "Unit must conform to the dimension of the range of the ontology property")

          case (dt1: DimensionDatatype, dt2: UnitDatatype) => require(dt1 == dt2.dimension,
            "The dimension of unit must match the range of the ontology property")

          case (dt1: DimensionDatatype, dt2: DimensionDatatype) => require(dt1 == dt2,
            "Unit must match the range of the ontology property")

          case _ if unit != null => require(unit == ontologyProperty.range, "Unit must be compatible to the range of the ontology property")
          case _ =>
        }
      }
    case _ =>
  }

  if (language != context.language) {
    require(ontologyProperty.isInstanceOf[OntologyDatatypeProperty],
      "Language can only be specified for datatype properties")

    // TODO: don't use string constant, use RdfNamespace
    require(ontologyProperty.range.uri == "http://www.w3.org/2001/XMLSchema#string",
      "Language can only be specified for string datatype properties")
  }

  private val parser: DataParser = CompoundParser(
    ontologyProperty.range match {
      //TODO
      case c: OntologyClass =>
        if (ontologyProperty.name == "foaf:homepage") {
          checkMultiplicationFactor("foaf:homepage")
          new LinkParser()
        } else {
          new ObjectParser(context)
        }
      case dt: UnitDatatype =>
        new CustomUnitValueParser(context, if (unit != null) unit else dt, multiplicationFactor = factor)
      case dt: DimensionDatatype =>
        new CustomUnitValueParser(context, if (unit != null) unit else dt, multiplicationFactor = factor)
      case dt: EnumerationDatatype =>
        {
          checkMultiplicationFactor("EnumerationDatatype")
          new EnumerationParser(dt)
        }
      case dt: Datatype => dt.name match {
        case "xsd:integer" => new CustomIntegerParser(context, multiplicationFactor = factor)
        case "xsd:positiveInteger" => new CustomIntegerParser(context, multiplicationFactor = factor, validRange = (i => i > 0))
        case "xsd:nonNegativeInteger" => new CustomIntegerParser(context, multiplicationFactor = factor, validRange = (i => i >= 0))
        case "xsd:nonPositiveInteger" => new CustomIntegerParser(context, multiplicationFactor = factor, validRange = (i => i <= 0))
        case "xsd:negativeInteger" => new CustomIntegerParser(context, multiplicationFactor = factor, validRange = (i => i < 0))
        case "xsd:double" => new DoubleParser(context, multiplicationFactor = factor)
        case "xsd:float" => new DoubleParser(context, multiplicationFactor = factor)
        case "xsd:string" =>
          {
            checkMultiplicationFactor("xsd:string")
            StringParser
          }
        case "xsd:anyURI" =>
          {
            checkMultiplicationFactor("xsd:anyURI")
            new LinkParser(false)
          }
        case "xsd:date" =>
          {
            checkMultiplicationFactor("xsd:date")
            new DateTimeParser(context, dt)
          }
        case "xsd:gYear" =>
          {
            checkMultiplicationFactor("xsd:gYear")
            new DateTimeParser(context, dt)
          }
        case "xsd:gYearMonth" =>
          {
            checkMultiplicationFactor("xsd:gYearMonth")
            new DateTimeParser(context, dt)
          }
        case "xsd:gMonthDay" =>
          {
            checkMultiplicationFactor("xsd:gMonthDay")
            new DateTimeParser(context, dt)
          }
        case "xsd:boolean" =>
          {
            checkMultiplicationFactor("xsd:boolean")
            BooleanParser
          }
        case name => throw new IllegalArgumentException("Not implemented range " + name + " of property " + ontologyProperty)
      }
      case other => throw new IllegalArgumentException("Property " + ontologyProperty + " does have invalid range " + other)
    },context.timexParser)

  private def checkMultiplicationFactor(datatypeName: String) {
    if (factor != 1) {
      throw new IllegalArgumentException("multiplication factor cannot be specified for " + datatypeName)
    }
  }

  override val datasets = Set(DBpediaDatasets.OntologyProperties, DBpediaDatasets.SpecificProperties)

  override def extract(node: TemplateNode, subjectUri: String, pageContext: PageContext): Seq[Quad] =
    {
      //val a = RuntimeAnalyzer(subjectUri)
      val graph = new ArrayBuffer[Quad]

  
      for (propertyNode <- node.property(templateProperty) if propertyNode.children.size > 0) {
     //   val parseResults =   a.dbPediaExtractor{
         val parseResults = parser.parsePropertyNode(propertyNode, !ontologyProperty.isFunctional, subjectUri)
     //    }
         val propertyNodeGraph = new ArrayBuffer[Quad]
        for (parseResult <- selector(parseResults)) {
          val g = parseResult match {
            case (Some((value: Double, unit: UnitDatatype, _, _)), None) =>
              writeUnitValue(node, value, unit, subjectUri, propertyNode.sourceUri)
            case (Some((value: Double, unit: UnitDatatype)), None) =>
              writeUnitValue(node, value, unit, subjectUri, propertyNode.sourceUri)

            case (Some((value, _)), None) =>
              writeValue(value, subjectUri, propertyNode.sourceUri)
            case (Some(value), None) =>
              writeValue(value, subjectUri, propertyNode.sourceUri)

            case (Some((value: Double, unit: UnitDatatype, _, _)), Some(timexes: (Option[String],Option[String]))) =>
              writeTemporalUnitValue(node, value, unit, subjectUri, propertyNode.sourceUri, timexes)
            case (Some((value: Double, unit: UnitDatatype)), Some(timexes: (Option[String],Option[String]))) =>
              writeTemporalUnitValue(node, value, unit, subjectUri, propertyNode.sourceUri, timexes)

            case (Some((value, _)), Some(timexes: (Option[String],Option[String]))) =>
              writeTemporalValue(value, subjectUri, propertyNode.sourceUri, timexes)
            case (Some(value), Some(timexes: (Option[String],Option[String]))) =>
              writeTemporalValue(value, subjectUri, propertyNode.sourceUri, timexes)

            case _ => Seq.empty[Quad]
          }
           propertyNodeGraph ++= g
        }
         if(propertyNodeGraph.isEmpty) {
            logger.fine( "Unable to parse value from Property Node \n\t" + propertyNode + 
                ";\n\t Results: " + parseResults.mkString("; ") +
                ";\n\t Node Value: " + CustomStringParser.parse(propertyNode).toString); 
          }
        graph ++= propertyNodeGraph
      }

      graph
    }
  private def writeTemporalUnitValue(node: TemplateNode, value: Double, unit: UnitDatatype, subjectUri: String, sourceUri: String, timexes: (Option[String],Option[String])): Seq[Quad] = {
    //  println(subjectUri + "-" + ontologyProperty + " - " + value + unit + "; " + date);
    //  writeUnitValue(node, value, unit, subjectUri, sourceUri)
    val fromDate = timexes._1
    val toDate = timexes._2

    //TODO better handling of inconvertible units
    if (unit.isInstanceOf[InconvertibleUnitDatatype]) {
      val quad = new ExtendedQuad(language.toString, DBpediaDatasets.OntologyProperties.toString, subjectUri, ontologyProperty.toString, value.toString, sourceUri, unit.toString, fromDate, toDate)
      return Seq(quad)
    }

    //Write generic property
    val stdValue = unit.toStandardUnit(value)

    val graph = new ArrayBuffer[Quad]

    graph += new ExtendedQuad(language.toString, DBpediaDatasets.OntologyProperties.toString, subjectUri, ontologyProperty.toString, stdValue.toString, sourceUri, (new Datatype("xsd:double")).toString, fromDate, toDate)

    // Write specific properties
    // FIXME: copy-and-paste in CalculateMapping
    for (
      templateClass <- node.getAnnotation(TemplateMapping.CLASS_ANNOTATION);
      currentClass <- templateClass.relatedClasses
    ) {
      for (specificPropertyUnit <- context.ontology.specializations.get((currentClass, ontologyProperty))) {
        val outputValue = specificPropertyUnit.fromStandardUnit(stdValue)
        val propertyUri = DBpediaNamespace.ONTOLOGY.append(currentClass.name + '/' + ontologyProperty.name)
        val quad = new ExtendedQuad(language.toString, DBpediaDatasets.SpecificProperties.toString, subjectUri,
          propertyUri, outputValue.toString, sourceUri, specificPropertyUnit.toString, fromDate, toDate)
        graph += quad
      }
    }

    graph

  }

  private def writeUnitValue(node: TemplateNode, value: Double, unit: UnitDatatype, subjectUri: String, sourceUri: String): Seq[Quad] =
    {
      //TODO better handling of inconvertible units
      if (unit.isInstanceOf[InconvertibleUnitDatatype]) {
        val quad = new Quad(language, DBpediaDatasets.OntologyProperties, subjectUri, ontologyProperty, value.toString, sourceUri, unit)
        return Seq(quad)
      }

      //Write generic property
      val stdValue = unit.toStandardUnit(value)

      val graph = new ArrayBuffer[Quad]

      graph += new Quad(language, DBpediaDatasets.OntologyProperties, subjectUri, ontologyProperty, stdValue.toString, sourceUri, new Datatype("xsd:double"))

      // Write specific properties
      // FIXME: copy-and-paste in CalculateMapping
      for (
        templateClass <- node.getAnnotation(TemplateMapping.CLASS_ANNOTATION);
        currentClass <- templateClass.relatedClasses
      ) {
        for (specificPropertyUnit <- context.ontology.specializations.get((currentClass, ontologyProperty))) {
          val outputValue = specificPropertyUnit.fromStandardUnit(stdValue)
          val propertyUri = DBpediaNamespace.ONTOLOGY.append(currentClass.name + '/' + ontologyProperty.name)
          val quad = new Quad(language, DBpediaDatasets.SpecificProperties, subjectUri,
            propertyUri, outputValue.toString, sourceUri, specificPropertyUnit)
          graph += quad
        }
      }

      graph
    }
  private def writeTemporalValue(value: Any, subjectUri: String, sourceUri: String, timexes: (Option[String],Option[String])): Seq[Quad] = {
    //println(subjectUri + " - " + ontologyProperty + " - " + value + ";" + date);
    //writeValue(value, subjectUri, sourceUri)
    val datatype = if (ontologyProperty.range.isInstanceOf[Datatype]) ontologyProperty.range.asInstanceOf[Datatype].toString else ""

    val fromDate = timexes._1
    val toDate = timexes._2


    Seq(new ExtendedQuad(language.toString, DBpediaDatasets.OntologyProperties.toString, subjectUri, ontologyProperty.toString, value.toString, sourceUri, datatype, fromDate, toDate))
  }

  private def writeValue(value: Any, subjectUri: String, sourceUri: String): Seq[Quad] =
    {
      val datatype = if (ontologyProperty.range.isInstanceOf[Datatype]) ontologyProperty.range.asInstanceOf[Datatype] else null

      Seq(new Quad(language, DBpediaDatasets.OntologyProperties, subjectUri, ontologyProperty, value.toString, sourceUri, datatype))
    }
}
