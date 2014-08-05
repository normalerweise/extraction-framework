package org.dbpedia.extraction.dataparser

import java.util.logging.Logger
import org.dbpedia.extraction.config.dataparser.ParserUtilsConfig
import java.text.{NumberFormat,DecimalFormatSymbols}
import org.dbpedia.extraction.util.Language
import java.util.Locale
import java.util.regex.Pattern
import scala.language.reflectiveCalls

/**
 * Utility functions used by the data parsers.
 */
//TODO test after re-factor
class ParserUtils( val context : { def language : Language } )
{

  private val logger = Logger.getLogger(classOf[ParserUtils].getName)

    private val scales = ParserUtilsConfig.scalesMap.getOrElse(context.language.wikiCode, ParserUtilsConfig.scalesMap("en"))

    // NumberFormat is not thread-safe
    private val numberFormat = new ThreadLocal[NumberFormat] {
      override def initialValue = NumberFormat.getNumberInstance(context.language.locale)
    } 
    
    private val groupingSeparator = DecimalFormatSymbols.getInstance(context.language.locale).getGroupingSeparator
    
    private val defaultDecimalSeparator = DecimalFormatSymbols.getInstance(context.language.locale).getDecimalSeparator
    private val decimalSeparatorsRegex = ParserUtilsConfig.decimalSeparators.get(context.language.wikiCode) match
      {
        case Some(sep) => ("["+sep+"]").r
        case None => ("""\"""+defaultDecimalSeparator).r 
      }



    
    def parse(str: String): Number = {
      // space is sometimes used as grouping separator
      val cleanedString = decimalSeparatorsRegex
         .replaceAllIn(str, ""+defaultDecimalSeparator)
         .replace(' ', groupingSeparator)
      numberFormat.get.parse(cleanedString)
    }

    /**
     * Converts large numbers like '100.5 million' to '100500000'
     */
    def convertLargeNumbers(input : String) : String = convertLargeNumbersWithSurfaceStringInfo(input)._1

  // TODO: use "\s+" instead of "\s?" between number and scale?
  // TODO: in some Asian languages, digits are not separated by thousands but by ten thousands or so...

  // escape and sort scales descending -> longer values should be matched first
  private val scalesRegexPart = scales.keySet.toList.sortBy(_.length * -1).map(Pattern.quote).mkString("|")
  private val englishGroupingSeparatorRegexStr = "[,.]"
  private val englishGroupingSeparatorRegex = englishGroupingSeparatorRegexStr.r
  private val englishDecimalSeparatorRegex = "[,.]".r
  private val regex = ("""(?i)([\D]*)([0-9]+(?:[,.][0-9]{3}(?![0-9]+))*)([,.][0-9]+)?(\s?(?:""" + scalesRegexPart + """))?(.*)""").r


  // TODO rename -> normalize number with surface string info
    def convertLargeNumbersWithSurfaceStringInfo(input : String) : (String, String) =
     {
      val _match = regex.findFirstMatchIn(input)
       if(_match.isDefined) {
        val m = _match.get
        val begin = m.group(1)
        var integer = m.group(2)
        var fract = m.group(3)
        val scale = m.group(4)
        val end = m.group(5)

        // Some values are hard to decide e.g. 5.000 = 5.0 or 5000
        // default decimal/grouping seaprators don't help as not all values follow the current language
        // 5.000 -> in most cases integer
        // 5.000 billion -> in most cases double
         if(scale != null) {
           val integerComponents = integer.split(englishGroupingSeparatorRegexStr)
           // numer like 5.000 and not 5.000.000  and the last thouthands separator is .
           if(integerComponents.length == 2 && integer.charAt(integer.length - 4) == defaultDecimalSeparator){
             //swap thousands group to fraction
             val oldInteger = integer
             integer = oldInteger.substring(0,oldInteger.length - 4)
             fract = oldInteger.substring(oldInteger.length - 4)
           }
         }

        var fractLength = if(fract != null) fract.substring(1).length else 0
        val fractStr = if(fract != null) fract.substring(1) else ""
        val scaleMagnitude = if(scale != null) scales(scale.trim.toLowerCase) else -1

        val fractionAndTrailingZerosPart =
          if(fractLength == 0 && scaleMagnitude == -1) {
            ""
          } else if(fractLength <= scaleMagnitude) {
            fractStr + "0" * ( scaleMagnitude - fractLength)
          } else {
            val (before, after) = fractStr.splitAt(scaleMagnitude)
            before + defaultDecimalSeparator + after
          }

        val normalizedInteger = englishGroupingSeparatorRegex.replaceAllIn(integer,"")

        val inputWithNewValue = begin + normalizedInteger + fractionAndTrailingZerosPart + end

        val scaleStr = if(scale != null) scale else ""
        val fracWithThouthandsSep = if(fract != null) fract else ""
        val surfaceString = integer + fracWithThouthandsSep + scaleStr

      (inputWithNewValue, surfaceString)
       }else{
         logger.info("Did not match: " + input)
         (input, null)
       }
     }
      
//        input match
//        {
//            case regex(begin, integer, fract, scale, end) =>
//            {
//                val fraction = if(fract != null) fract.substring(1) else ""
//                val trailingZeros = "0" * (scales(scale.toLowerCase) - fraction.length)
//                begin + integer.replace(groupingSeparator.toString	, "") + fraction + trailingZeros + end
//            }
//            case _ => input
//        }

}
