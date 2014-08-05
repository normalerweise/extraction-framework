package ch.weisenburger.dbpedia.extraction.mappings

import org.dbpedia.extraction.destinations.Quad

class ExtendedQuad(
  override val language: String,
  override val dataset: String,
  override val subject: String,
  override val predicate: String,
  override val value: String,
  override val context: String,
  override val datatype: String,
  val fromDate: Option[String],
  val toDate: Option[String]) extends Quad(
  language, dataset, subject, predicate, value, context, datatype)