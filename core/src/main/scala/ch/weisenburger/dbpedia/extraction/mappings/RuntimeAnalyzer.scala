//package ch.weisenburger.dbpedia.extraction.mappings
//
//class RuntimeAnalyzer(val page: String) {
//  var startTime: Long = _
//  var stopTime: Long = _
//
//  var startWikiParserTime: Long = _
//  var stopWikiParserTime: Long = _
//  val aggWikiParserTime = collection.mutable.ListBuffer.empty[Long]
//
//  var startHeidelTimeTime: Long = _
//  var stopHeidelTimeTime: Long = _
//  val aggHeidelTime = collection.mutable.ListBuffer.empty[Long]
//
//  var startPosTaggerTime: Long = _
//  var stopPosTaggerTime: Long = _
//  val aggPosTaggerlTime = collection.mutable.ListBuffer.empty[Long]
//
//  val dbPediaExtractorTime = collection.mutable.ListBuffer.empty[Long]
//  val formatTimexTime = collection.mutable.ListBuffer.empty[Long]
//  val jcasTime = collection.mutable.ListBuffer.empty[Long]
//
//  def start =
//    startTime = System.currentTimeMillis
//
//  def stop =
//    stopTime = System.currentTimeMillis
//
//  def startHeideltime =
//    startHeidelTimeTime = System.currentTimeMillis
//
//  def stopHeidelTime = {
//    stopHeidelTimeTime = System.currentTimeMillis
//    aggHeidelTime += (stopHeidelTimeTime - startHeidelTimeTime)
//  }
//
//  def startPosTagger =
//    startPosTaggerTime = System.currentTimeMillis
//
//  def stopPosTagger = {
//    stopPosTaggerTime = System.currentTimeMillis
//    aggPosTaggerlTime += (stopPosTaggerTime - startPosTaggerTime)
//  }
//
//  def startWikiParser =
//    startWikiParserTime = System.currentTimeMillis
//
//  def stopWikiParser = {
//    stopWikiParserTime = System.currentTimeMillis
//    aggWikiParserTime += (stopWikiParserTime - startWikiParserTime)
//  }
//
//  lazy val getTotalRuntime = stopTime - startTime
//
//  def getHeidelTimeRuntime = {
//    val heidelTimeRuntime = aggHeidelTime.foldLeft(0L)((acc, run) => acc + run)
//    (heidelTimeRuntime, heidelTimeRuntime.toDouble / getTotalRuntime)
//  }
//
//  def getPosTaggeRuntime = {
//    val posTagerRuntime = aggPosTaggerlTime.foldLeft(0L)((acc, run) => acc + run)
//    (posTagerRuntime, posTagerRuntime.toDouble / getTotalRuntime)
//  }
//
//  def getWikiParserRuntime = {
//    val wikiParserRuntime = aggWikiParserTime.foldLeft(0L)((acc, run) => acc + run)
//    (wikiParserRuntime, wikiParserRuntime.toDouble / getTotalRuntime)
//  }
//
//  def getExtractorRuntime = {
//    val extractorRuntime = dbPediaExtractorTime.foldLeft(0L)((acc, run) => acc + run)
//    (extractorRuntime, extractorRuntime.toDouble / getTotalRuntime)
//  }
//
//  def getTimexFormaterRuntime = {
//    val formaterRuntime = formatTimexTime.foldLeft(0L)((acc, run) => acc + run)
//    (formaterRuntime, formaterRuntime.toDouble / getTotalRuntime)
//  }
//
//  def getJcasRuntime = {
//    val formaterRuntime = jcasTime.foldLeft(0L)((acc, run) => acc + run)
//    (formaterRuntime, formaterRuntime.toDouble / getTotalRuntime)
//  }
//
//  def dbdPediaExtractor[T](f: => T) = {
//    val (result, runtime) = measure(f)
//    dbPediaExtractorTime += runtime
//    result
//  }
//
//  def formatTIMEX[T](f: => T) = {
//    val (result, runtime) = measure(f)
//    formatTimexTime += runtime
//    result
//  }
//
//  def jcas[T](f: => T) = {
//    val (result, runtime) = measure(f)
//    jcasTime += runtime
//    result
//  }
//
//  private def measure[T](f: => T) = {
//    val start = System.currentTimeMillis
//    val result = f
//    val end = System.currentTimeMillis
//    (result, end - start)
//  }
//
//}
//
//object RuntimeAnalyzer {
//
//  var instances = collection.mutable.Map.empty[String, RuntimeAnalyzer]
//
//  def apply(page: String): RuntimeAnalyzer = {
//    instances.getOrElse(page, {
//      val a = new RuntimeAnalyzer(page)
//      instances(page) = a
//      a
//    })
//  }
//
//  def all = instances.toList.map(_._2)
//
//  def clear = { instances = instances.empty }
//}
