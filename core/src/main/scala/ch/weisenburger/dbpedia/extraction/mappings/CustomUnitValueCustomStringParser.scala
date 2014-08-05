package ch.weisenburger.dbpedia.extraction.mappings

import org.dbpedia.extraction.util.WikiUtil
import org.dbpedia.extraction.wikiparser._
import scala.util.matching.Regex.Match
import org.dbpedia.extraction.dataparser.DataParser

/**
 * Parses a human-readable character string from a node.
 * Special implementation for Unit Values, which ignores Template Nodes, but continues
 * to parse children 
 */
object CustomUnitValueCustomStringParser extends DataParser
{
    private val smallTagRegex = """<small[^>]*>\(?(.*?)\)?<\/small>""".r
    private val tagRegex = """\<.*?\>""".r

    override def parse(node : Node) : Option[String] =
    {
        //Build text from node
        val sb = new StringBuilder()
        nodeToString(node, sb)

        //Clean text
        var text = sb.toString()
        // Replace text in <small></small> tags with an "equivalent" string representation
        // Simply extracting the content puts this data at the same level as other text appearing
        // in the node, which might not be the editor's semantics
        text = smallTagRegex.replaceAllIn(text, (m: Match) => if (m.group(1).nonEmpty) "($1)" else "")
        text = tagRegex.replaceAllIn(text, "") //strip tags
        text = WikiUtil.removeWikiEmphasis(text)
        text = text.replace("&nbsp;", " ")//TODO decode all html entities here
        text = text.trim
        
        if(text.isEmpty)
        {
            None
        }
        else
        {
            Some(text)
        }
    }
  
    private def nodeToString(node : Node, sb : StringBuilder)
    {
        node match
        {
            case TextNode(text, _) => {
              if(!isNonsenseText(node.asInstanceOf[TextNode])){
              // avoid concatenation without space -> confuses unit parser e.g. "GBP(2008)" is not recognized,
              // whereas "GBP (2008)" is fine
              if(!sb.isEmpty){
                if(sb.last != ' ' && text.length >= 1 && text.charAt(0) != ' ') {
                 sb.append(' ')
                }
              }
               sb.append(text)
              }
            }
            case _ : TableNode => //ignore
            case  n: InternalLinkNode if n.destination.encoded == "Green_Arrow_Up.svg" => // ignore
            case _ => node.children.foreach(child => nodeToString(child, sb))
        }
    }
    
    // E.g HSBC rev 509142287 -> Has Property node with strange 0 value 
    private def isNonsenseText(node: TextNode) = {
      if(node.parent.isInstanceOf[org.dbpedia.extraction.wikiparser.PropertyNode] 
         && node.parent.asInstanceOf[org.dbpedia.extraction.wikiparser.PropertyNode].key == "1"
         && (node.text == "0" || node.text == "00")){
         true  
      }else {
         false
      }
    }
}
