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
      args.length >= 2 && args.length <= 6,
      s"Usage: java ${getClass.getName.dropRight(1)} <target> <key field> [<partition field> [<partition regex> " +
        s"[<partition name>/... [<partition prefix>,...]]]]"
    )
    val targetBase = if (args(0).endsWith("/")) args(0) else s"${args(0)}/"
    val keyField = args(1)
    val partitionFieldOption = args.lift.apply(2)
    val partitionRegexOption = partitionFieldOption.flatMap(_ => args.lift.apply(3).map(_.r))
    val partitionNamesOption = partitionFieldOption.flatMap(_ => args.lift.apply(4).map(_.split('/').toVector))
    val keyPrefixes = partitionNamesOption.flatMap(_ => args.lift.apply(5).map(_.split(',').toVector))
      .getOrElse(Vector.empty)

    val container = sys.props.getOrElse("container", "")
    val overwriteMode = sys.props.get("mode")
      .map(str => OverwriteMode.valueOf(str.toUpperCase))
      .getOrElse(OverwriteMode.OVERWRITE)
    val keyPrefixSeparator = sys.props.getOrElse("key-prefix-separator", "_")

    val targetBaseUri = new URI("v3io", container, targetBase, null, null)
    val inputLineIterator = Source.fromInputStream(System.in).getLines()
    val updateEntryIterator = inputLineIterator.map { line =>
      val json = JsonMethods.parse(line)
      val key = (json \ keyField).extract[Any].toString
      val (targetUri, keyPrefix) = partitionFieldOption match {
        case None => targetBaseUri -> ""
        case Some(partitionField) =>
          val partitionFieldValueStr = (json \ partitionField).extract[Any].toString
          partitionRegexOption match {
            case None => targetBaseUri.resolve(partitionFieldValueStr) -> ""
            case Some(partitionRegex) =>
              val partitionValues = partitionFieldValueStr match {
                case partitionRegex(subgroups@_*) => subgroups
                case _ => throw new Exception(
                  s"Partition regex '$partitionRegex' did not match '$partitionFieldValueStr'."
                )
              }
              val (partitions, prefix) = partitionNamesOption match {
                case None => partitionValues -> ""
                case Some(partitionNames) =>
                  require(
                    partitionNames.size + keyPrefixes.size == partitionValues.size,
                    s"Captured ${partitionValues.size} parititons in '$partitionFieldValueStr', " +
                      s"but ${partitionNames.size} were named."
                  )
                  partitionNames.zip(partitionValues).map { case (k, v) => s"$k=$v" } ->
                    keyPrefixes.zip(partitionValues.drop(partitionNames.size))
                      .map { case (k, v) => s"$k=$v" }.mkString(keyPrefixSeparator)
              }
              partitions.foldLeft(targetBaseUri) {
                case (acc, nextPartition) => acc.resolve(s"$nextPartition/")
              } -> prefix
          }
      }
      val data = json.extract[Map[String, Any]].mapValues {
        case bigInt: BigInt => bigInt.toLong
        case other => other
      }
      val row = Row(if (keyPrefix.isEmpty) key else s"${keyPrefix}_$keyField=$key", data)
      UpdateEntry(targetUri, row, overwriteMode)
    }
    KeyValueOperations().updateItems(updateEntryIterator)
  }
}
