Key value CLI 
=================

A command line utility for reading and writing key value data.

Build
--------
* Run `sbt assembly`.

Put to KV
-----------
#### Usage
* `java io.iguaz.cli.kv.PutJson <target> <key field> [<partition field> [<partition regex> [<partition names>]]]`

#### Examples
* Run `echo '{"x":"abc", "date":"2018-06-01 10:01:55"}' | java -cp igz/bigdata/libs/scala-library-2.11.12.jar:igz/bigdata/libs/v3io-spark2-object-dataframe_2.11.jar:kv_cli-assembly-0.1.jar io.iguaz.cli.kv.PutJson /test x date '(\d\d\d\d)-(\d\d)-(\d\d) (\d\d):(\d\d):(\d\d)' year/month/day/hour/minute/second`.
  * This will write an entry to `/test/year=2018/month=06/day=01/hour=10/minute=01/second=55/abc`.
  * The date is partitioned by the parsed values of the date field, prefixed by the respective partition names.
* Run `echo '{"x":"abc", "date":"2018-06-01 10:01:55"}' | java -cp igz/bigdata/libs/scala-library-2.11.12.jar:igz/bigdata/libs/v3io-spark2-object-dataframe_2.11.jar:kv_cli-assembly-0.1.jar io.iguaz.cli.kv.PutJson /test x date '(\d\d\d\d)-(\d\d)-(\d\d) (\d\d):(\d\d):(\d\d)' year/month/day/hour minute,second`.
  * This will write an entry to `/test/year=2018/month=06/day=01/hour=10/minute=01_second=55_abc`.
  * The date is partitioned by the parsed values of the date field, excluding minute and hour, prefixed by the respective partition names. The key is prefixed by the minute and second.
* Run `echo '{"x":"abc", "date":"2018-06-01 10:01:55"}' | java -cp igz/bigdata/libs/scala-library-2.11.12.jar:igz/bigdata/libs/v3io-spark2-object-dataframe_2.11.jar:kv_cli-assembly-0.1.jar io.iguaz.cli.kv.PutJson /test x date '(\d\d\d\d)-(\d\d)-(\d\d) (\d\d):(\d\d):(\d\d)'`.
  * This will write an entry to `/test/test/2018/06/01/10/01/55/abc`.
  * The date is partitioned by the parsed values of the date field.
* Run `echo '{"x":"abc", "date":"2018-06-01_10_01_55"}' | java -cp igz/bigdata/libs/scala-library-2.11.12.jar:igz/bigdata/libs/v3io-spark2-object-dataframe_2.11.jar:kv_cli-assembly-0.1.jar io.iguaz.cli.kv.PutJson /test x date`.
  * This will write an entry to `/test/2018-06-01_10_01_55/abc`.
  * The data is partitioned by the whole date field (and so must conform to path rules).
* Run `echo '{"x":"abc", "date":"2018-06-01 10:01:55"}' | java -cp igz/bigdata/libs/scala-library-2.11.12.jar:igz/bigdata/libs/v3io-spark2-object-dataframe_2.11.jar:kv_cli-assembly-0.1.jar io.iguaz.cli.kv.PutJson /test x`.
  * This will write an entry to `/test/abc`.
  * The data is not partitioned.
  
Configure
----------
* Use `-Dmode` to set the overwrite mode (e.g. `-Dmode=replace`). Default is `overwrite`.
* Use `-Dcontainer` to choose a container (e.g. `-Dcontainer=bigdata`). Default is system default.
* Use `-Dkey-prefix-separator` to choose a container (e.g. `-Dkey-prefix-separator=.`). Default is `_`.
