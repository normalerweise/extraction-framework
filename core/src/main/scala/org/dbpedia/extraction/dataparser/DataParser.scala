package org.dbpedia.extraction.dataparser

import org.dbpedia.extraction.wikiparser.{NodeUtil, PropertyNode, Node}
import org.dbpedia.extraction.config.dataparser.DataParserConfig
import org.dbpedia.extraction.wikiparser.TemplateNode

/**
 * Extracts data from a node in the abstract syntax tree.
 * The type of the data which is extracted depends on the specific parser e.g. The IntegerParser extracts integers.
 */
abstract class DataParser
{
  
  def parse( node : Node) : Option[Any]

  def parse( node : Node, subjectUri: String) : Option[Any] = parse(node, null, subjectUri)
  def parse( node : Node, dependentParserResult: Option[Any], subjectUri: String) : Option[Any] = parse(node)

    /**
     * Parser dependent splitting of nodes. Default is overridden by some parsers.
     */
    val splitPropertyNodeRegex = DataParserConfig.splitPropertyNodeRegex.get("en").get

    /**
     * (Split node and) return parse result.
     */
    def parsePropertyNode( propertyNode : PropertyNode, split : Boolean, subjectUri: String) : List[Any] =
    {
        if(split)
        {
            NodeUtil.splitPropertyNode(propertyNode, splitPropertyNodeRegex).flatMap( node => parse(node, subjectUri).toList )
        }
        else
        {
            parse(propertyNode, subjectUri).toList
        }
    }
    
      def parsePropertyNode( propertyNode : PropertyNode, split : Boolean) : List[Any] =
    {
        if(split)
        {
            NodeUtil.splitPropertyNode(propertyNode, splitPropertyNodeRegex).flatMap( node => parse(node).toList )
        }
        else
        {
            parse(propertyNode).toList
        }
    }

}
