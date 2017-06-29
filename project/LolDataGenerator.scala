import scala.util.matching.Regex
// import sys.process._
import java.net.{URL, HttpURLConnection}
import java.io._
// import org.apache.spark.SparkContext._
import org.apache.spark.SparkContext
import org.apache.spark.SparkConf
import org.apache.spark.sql._
import org.apache.spark.sql.functions._
import org.apache.spark.rdd.RDD

object LolDataGenerator {
    val conf = new SparkConf().setAppName("LolDataGenerator").setMaster("local[2]").set("spark.executor.memory","1g");
    val sc = new SparkContext(conf)
    val sqlContext = new SQLContext(sc)
    val spark = SparkSession.builder().getOrCreate()
    import sqlContext.implicits._

    val apiUrlPrefix = "https://br1.api.riotgames.com"
    val apiKeySuffix = "?api_key=RGAPI-41a4c27c-2d0c-4424-b98a-59e464659a9b" //"?api_key=RGAPI-f08f49ff-27fb-4389-b413-fe17dc9d8370"
    val jsonExtension = ".json"

    val supportingFilesDirectory = "/var/www/html/projeto-bigdata/project/supportingFiles/"
    val playersDirectory = supportingFilesDirectory + "players/"
    val accountIdsDirectory = supportingFilesDirectory + "accountIds/"
    val gamesDirectory = supportingFilesDirectory + "games/"

    def main(args: Array[String]) {
        sc.setLogLevel("ERROR")

        val matchApiUrl = "/lol/match/v3/matches/"
        val gameIds = getGameIdsFromChallengerLeague()

        gameIds.foreach { gameId =>
            val fileName = gamesDirectory + gameId + jsonExtension
            downloadApiFileFrom(matchApiUrl + gameId, fileName)
        }

        sc.stop()
    }

    def generateResultsFrom(gamesDfArray: Array[DataFrame]) {
        val gamesDf2 = gamesDfArray.reduce((df1, df2) => df1.unionAll(df2))

        // (key: (gameId, win), value: championId).groupByKey() = (key: (gameId, win), value: championIds array)
        val gamesRdd = gamesDf2.rdd.map(row => ((row(3), row(2)), row(1))).groupByKey()

        // (key: (gameId, win), value: calculated team id)
        val gamesRdd2 = gamesRdd.map(kv => (kv._1, idFromTeamSeq(kv._2.asInstanceOf[Seq[Long]])))

        // (key: gameId, value: (calculated team id, win))
        val gamesRdd3 = gamesRdd2.map(kv => (kv._1._1, (kv._2, kv._1._2)))

        val gamesRdd4 = gamesRdd3.groupByKey()

        // (key: (teamId1, teamId2), value: 1)
        // teamId1 perdeu pro teamId2 uma vez
        val gamesRdd5 = gamesRdd4.map(kv => {
                val tupleSeq = kv._2.asInstanceOf[Seq[(Long, String)]]
                val teamIdAndWin1 = tupleSeq(0)
                val teamIdAndWin2 = tupleSeq(1)

                if (teamIdAndWin1._2 == "Win") {
                    ((teamIdAndWin2._1, teamIdAndWin1._1), 1)
                } else {
                    ((teamIdAndWin1._1, teamIdAndWin2._1), 1)
                }
            })

        // (key: (teamId1, teamId2), value: N)
        // teamId1 perdeu pro teamId2 N vezes
        val gamesRdd6 = gamesRdd5.reduceByKey((v1, v2) => v1 + v2)

        val writer = new PrintWriter(new File(supportingFilesDirectory + "results.txt"))

        gamesRdd6.collect.foreach { row =>
            writer.write(row._1._1 + "," + row._1._2 + "," + row._2)
            writer.write("\n")
        }
        writer.close()
    }

    def getGameIdsFromChallengerLeague(): RDD[String] = {
      // val accountIds = sc.parallelize(Array("10", "20", "30"))
      // def test(x: String): RDD[String] = {
      //   val rdd1 = sc.parallelize(Array("1", "2"))
      //   val rdd2 = sc.parallelize(Array("4", "3"))
      //   val rdd3 = sc.parallelize(Array("6", "2"))
      //
      //   if (x == "10") rdd1 else if (x == "20") rdd2 else rdd3
      // }
      // return accountIds.collect.toList.foldLeft(sc.emptyRDD[String]){(z,i) => z.union(test(i))}.distinct

      val accountIds = getAccountIdsFromChallengerLeague()

      return accountIds.collect.toList.foldLeft(sc.emptyRDD[String]){(z,i) => z.union(getGameIdsFrom(i))}.distinct
    }

    def getGameIdsFrom(accountId: String): RDD[String] = {
      val accountUrl = s"/lol/match/v3/matchlists/by-account/${accountId}"
      val accountFileName = accountIdsDirectory + accountId + jsonExtension

      downloadApiFileFrom(accountUrl, accountFileName)
      val df = spark.read.json(accountFileName)

      return df.select(explode(df("matches"))).toDF("games").select("games.gameId").map(_.getAs[Long]("gameId").toString).rdd.distinct
    }

    // gera um RDD com os accountIds de todos jogadores da challenger league
    def getAccountIdsFromChallengerLeague(): RDD[String] = {
        val challengerLeagueDF = getChallengerLeagueDF()
        val playerNamesDF = challengerLeagueDF.select(explode(challengerLeagueDF("entries"))).toDF("players").select("players.playerOrTeamName")

        val pattern = " ".r
        return playerNamesDF.map(row => getAccountIdFrom(pattern.replaceAllIn(row.getAs[String]("playerOrTeamName"), "%20"))).rdd
    }

    // pega o accountId a partir do nome do jogador
    def getAccountIdFrom(playerName: String): String = {
        val playerUrl = "/lol/summoner/v3/summoners/by-name/" + playerName
        val playerFileName = playersDirectory + playerName + jsonExtension

        downloadApiFileFrom(playerUrl, playerFileName)

        return spark.read.json(playerFileName).first().getAs[Long]("accountId").toString
    }

    // baixa e gera o DataFrame de challenger league
    def getChallengerLeagueDF(): DataFrame = {
        val challengerLeagueUrl = "/lol/league/v3/challengerleagues/by-queue/RANKED_SOLO_5x5"
        val challengerleagueFileName = supportingFilesDirectory + "challengerLeague" + jsonExtension

        downloadApiFileFrom(challengerLeagueUrl, challengerleagueFileName)

        return spark.read.json(challengerleagueFileName)
    }

    // baixa um arquivo da API atraves da URL especifica desse request
    def downloadApiFileFrom(apiUrl: String, fileName: String) = {
        val file = new File(fileName)

        if (!file.exists()) {
            print(s"Baixando da API arquivo ${fileName} ... ")
            Thread.sleep(1250)
            downloadUrlToFile(apiUrlPrefix + apiUrl + apiKeySuffix, fileName)
            println("Feito!")
        }
    }

    // baixa o arquivo de uma URL pra um arquivo local
    def downloadUrlToFile(url: String, fileName: String) = {
        // http://alvinalexander.com/scala/scala-how-to-download-url-contents-to-string-file
        // new URL(url) #> new File(fileName) !! // nao funcionou quando a API retorna 404

        val file = new File(fileName)

        if (!file.exists()) {
            val fileUrl = new URL(url)
            val connection = fileUrl.openConnection().asInstanceOf[HttpURLConnection]
            connection.setRequestMethod("GET")

            var in: InputStream = null
            var error = false

            try {
                in = connection.getInputStream
            } catch {
                case ex: FileNotFoundException => {
                    error = true
                    println("\nArquivo nao encontrado na URL " + url + "\n")
                }
                case ex: IOException => {
                    error = true
                    println("\n" + ex.toString + "\n")
                }
            }

            if (!error) {
                val out: OutputStream = new BufferedOutputStream(new FileOutputStream(file))
                val byteArray = Stream.continually(in.read).takeWhile(-1 !=).map(_.toByte).toArray
                out.write(byteArray)
                out.close()
                in.close()
            } else {
                file.createNewFile(); // cria um arquivo vazio
            }
        }
    }

    // gera o DataFrame de uma partida a partir de seu json
    def matchDataFrameFrom(fileName: String): DataFrame = {
        // http://bigdatums.net/2016/02/12/how-to-extract-nested-json-data-in-spark/

        val matchDf = spark.read.json(fileName)
        var teamsDf = matchDf.select(explode(matchDf("teams"))).toDF("teams")
        var participantsDf = matchDf.select(explode(matchDf("participants"))).toDF("participants")

        teamsDf = teamsDf.select("teams.teamId", "teams.win")
        participantsDf = participantsDf.select("participants.teamId", "participants.championId")

        val gameId = matchDf.select("gameId").take(1)(0).getLong(0)
        return participantsDf.join(teamsDf, "teamId").withColumn("gameId", lit(gameId))
    }

    // gera um id unico para cada combinacao de 5 campeoes
    // ex: (19, 51, 91, 161, 267) -> 19051091161267
    def idFromTeamSeq(team: Seq[Long]): Long = {
        return team.sorted.reduceLeft((x, y) => (x * 1000) + y)
    }

}
