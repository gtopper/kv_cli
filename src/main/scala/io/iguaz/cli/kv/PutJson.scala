package io.iguaz.cli.kv

import java.net.URI

import scala.io.Source

import org.json4s.DefaultFormats
import org.json4s.jackson.JsonMethods

import io.iguaz.v3io.kv.{KeyValueOperations, OverwriteMode, Row, UpdateEntry}

object PutJson {

  private implicit val defaultFormats: DefaultFormats = org.json4s.DefaultFormats

  def main(args: Array[String]): Unit = {
    TerminateOnSigPipe.registerHandler()
    require(
      args.length >= 1 && args.length <= 2,
      s"Usage: java ${getClass.getName.dropRight(1)} <target> <key field> [<partition field> [<partition regex>]]"
    )
    val targetBase = args(0)
    val keyField = args(1)
    val partitionFieldOption = args.lift.apply(2)
    val partitionRegexOption = partitionFieldOption.flatMap(_ => args.lift.apply(3).map(_.r))

    val container = sys.props.getOrElse("container", "")
    val overwriteMode = sys.props.get("mode")
      .map(str => OverwriteMode.valueOf(str.toUpperCase))
      .getOrElse(OverwriteMode.OVERWRITE)

    val targetBaseUri = new URI("v3io", container, targetBase, null, null)
    val inputLineIterator = Source.fromInputStream(System.in).getLines()
    val updateEntryIterator = inputLineIterator.map { line =>
      val json = JsonMethods.parse(line)
      val key = (json \ keyField).extract[Any].toString
      val targetUri = partitionFieldOption match {
        case None => targetBaseUri
        case Some(partitionField) =>
          val partitionFieldValueStr = (json \ partitionField).extract[Any].toString
          partitionRegexOption match {
            case None => targetBaseUri.resolve(partitionFieldValueStr)
            case Some(partitionRegex) =>
              val partitionRegexMatch = partitionRegex.findFirstMatchIn(partitionFieldValueStr).getOrElse(
                throw new Exception(s"Partition regex '$partitionRegex' did not match '$partitionFieldValueStr'.")
              )
              partitionRegexMatch.subgroups.foldLeft(targetBaseUri)(_ resolve _)
          }
      }
      val row = Row(key, json.extract[Map[String, Any]])
      UpdateEntry(targetUri, row, overwriteMode)
    }
    KeyValueOperations().updateItems(updateEntryIterator)
  }
}
