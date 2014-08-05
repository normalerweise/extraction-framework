package ch.weisenburger.dbpedia.extraction.mappings

import org.dbpedia.extraction.dataparser.DataParser
import org.dbpedia.extraction.wikiparser.Node
import org.dbpedia.extraction.wikiparser.TemplateNode

case class CompoundParser(
  val firstParser: DataParser,
  val secondParser: DataParser) extends DataParser {

   override def parse(node: Node, subjectURI: String): Option[Any] = {
     val firstResult = firstParser.parse(node, subjectURI)
     val secondResult = secondParser.parse(node, firstResult, subjectURI) 
    Some((firstResult, secondResult))
  }
  
  
  def parse(node: Node): Option[Any] = {
    assert(1==0); Some(0) // Some((firstParser.parse(node), secondParser.parse(node)))
  }

 }
