import scala.util.matching.Regex
import sys.process._
import java.net.URL
import java.io._
import java.net.HttpURLConnection
import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.SparkConf
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.Row
import org.apache.spark.sql.DataFrame
import org.apache.spark.sql.functions._
import org.apache.spark.sql._

object LolDataFinder {
    val conf = new SparkConf().setAppName("LolDataFinder").setMaster("local[2]").set("spark.executor.memory","1g")
        .set("spark.ui.showConsoleProgress", "false");
    val sc = new SparkContext(conf)
    val sqlContext = new SQLContext(sc)
    val spark = SparkSession.builder().getOrCreate()
    import sqlContext.implicits._

    def main(args: Array[String]) {
        sc.setLogLevel("OFF")
        val inputFileName = args(0)
        val adversaryId = teamIdFromInputFile(inputFileName)

        getResults(adversaryId)
        sc.stop()
    }

    def teamIdFromInputFile(fileName: String): Long = {
        var championsString = sc.textFile(fileName).first()

        championsString = championsString.drop(2) // remove ["
        championsString = championsString.dropRight(2) // remove "]

        val championsList = championsString.split("\",\"")

        val teamId = idFromTeamSeq(championsList.map(_.toLong).toSeq)

        return teamId
    }

    def getResults(adversaryId: Long) {

        // (advId, champId, percentage, n, total)
        val dataFileName = "/var/www/html/projeto-bigdata/project/data.txt"
        val dataFile = sc.textFile(dataFileName)
        val dataRdd = dataFile.map { line =>
            val a = line.split(",")
            (a(0).toLong, a(1).toLong, a(2).toFloat, a(3).toLong, a(4).toLong)
        }

        println("[")

        print("\t[")
        val adversaryArray = teamArrayFromId(adversaryId)
        var commaCount = 1
        adversaryArray.foreach { adv =>
            if (commaCount > 1) {
                print(", ")
            }
            print(adv)
            commaCount += 1
        }
        println("],")

        commaCount = 1
        adversaryArray.foreach { advChampId =>
            if (commaCount > 1) {
                println(",")
            }
            println("\t[")

            // println(s"Adversario: ${advChampId}")
            // println("Sugestoes:")

            var commaCount2 = 1

            val orderedSuggestionsRdd = dataRdd
                .filter { case (advId, champId, percentage, n, total) => advId ==  advChampId && total >= 30 }
                .map { case (advId, champId, percentage, n, total) => (percentage, (champId, n, total)) }
                .sortByKey(false)

            var champCount = 0
            orderedSuggestionsRdd.take(5).foreach { case (percentage, info) =>
                //println(s"    ${info._1} ganhou ${"%1.2f".format(percentage)}% das vezes (${info._2}/${info._3})")
                if (commaCount2 > 1) {
                    println(",")
                }
                print(s"\t\t[${info._1}, ${percentage}, ${info._2}, ${info._3}]")
                commaCount2 += 1
                champCount +=1
            }
            if (champCount == 0) {
                print(s"\t\t[0]")
            }

            //println()
            print("\n\t]")
            commaCount += 1
        }

        println("\n]")

    }

    // gera o DataFrame dos campeoes a partir do json
    def championsDataFrameFrom(fileName: String): DataFrame = {
        var championsString = sc.textFile(fileName).first()
        val prefixIndex = championsString.indexOfSlice("\"data\":{")

        championsString = championsString.drop(prefixIndex + 8) // remove ..."data"...
        championsString = championsString.dropRight(2) // remove }}

        val pattern = ",?\"(\\w)+\":[{]".r // ,"string":{
        championsString = pattern.replaceAllIn(championsString, "\n{")
        championsString = championsString.drop(1) // remove primeiro \n

        val championsList = championsString.split("\n")
        val championsRdd = sc.parallelize(championsList)

        return spark.read.json(championsRdd)
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

    // gera uma lista de partidas a partir de um json de featuredGames
    def featuredGamesDataFrameFrom(fileName: String): DataFrame = {
        var gamesDf = spark.read.json(fileName)

        gamesDf = gamesDf.select(explode(gamesDf("gameList"))).toDF("gameList")
        gamesDf = gamesDf.select("gameList.gameId")

        return gamesDf
    }

    // baixa um json de uma url pra um arquivo
    def downloadJsonToFile(url: String, fileName: String) = {
        // http://alvinalexander.com/scala/scala-how-to-download-url-contents-to-string-file
        // new URL(url) #> new File(fileName) !! // nao funcionou quando a API retorna 404

        val file = new java.io.File(fileName)

        if (!file.exists()) {
            try {
                val fileUrl = new URL(url)
                val connection = fileUrl.openConnection().asInstanceOf[HttpURLConnection]
                connection.setRequestMethod("GET")

                val in: InputStream = connection.getInputStream

                val out: OutputStream = new BufferedOutputStream(new FileOutputStream(file))
                val byteArray = Stream.continually(in.read).takeWhile(-1 !=).map(_.toByte).toArray

                out.write(byteArray)
                in.close()
                out.close()
            } catch {
                case ex: java.io.FileNotFoundException => {
                    file.createNewFile(); // cria um arquivo vazio
                    println("\nArquivo nao encontrado na URL " + url + "\n")
                }
            }
        }

    }

    // gera um DataFrame com o nome de cada campeao de um jogo e seu status de vitoria
    // (nao ta sendo usada)
    def championResultsDataFrameFrom(matchDf: DataFrame, championsDf: DataFrame): DataFrame = {
        val joinedDf = matchDf.as('match).join(championsDf.as('champions), $"match.championId" === $"champions.id")
        return joinedDf.select($"name", $"win")
    }

    // gera um id unico para cada combinacao de 5 campeoes
    // ex: (19, 51, 91, 161, 267) -> 19051091161267
    def idFromTeamSeq(team: Seq[Long]): Long = {
        return team.sorted.reduceLeft((x, y) => (x * 1000) + y)
    }

    // desconverte a lista de 5 campeoes a partir do id unico gerado acima
    // ex: 19051091161267 -> (19, 51, 91, 161, 267)
    // (deve ter uma maneira menos feia de implementar)
    def teamArrayFromId(id: Long): Array[Long] = {
        val team = new Array[Long](5)

        team(0) = id/1000000000000L
        team(1) = id/1000000000L    - team(0)*1000L
        team(2) = id/1000000L       - team(0)*1000000L       - team(1)*1000L
        team(3) = id/1000L          - team(0)*1000000000L    - team(1)*1000000L    - team(2)*1000L
        team(4) = id/1L             - team(0)*1000000000000L - team(1)*1000000000L - team(2)*1000000L - team(3)*1000L

        return team
    }

    // gera uma array de nomes de campeoes baseado num id de time
    def championNamesFromTeamId(id: Long): Array[String] = {
        val championsJson = "/var/www/html/projeto-bigdata/project/champions.json"
        val championsDf = championsDataFrameFrom(championsJson)

        val championIdsArray = teamArrayFromId(id)
        val nameArray = championIdsArray.map(championId => {
            val nameDf = championsDf.select("name").where("id == " + championId.toString)
            val name = nameDf.first()(0).toString
            name
        })

        return nameArray
    }

}

/*

o que eu fiz:

chegar num rdd final que seja tipo (teamId1, teamId2, N) (nao necessariamente nessa ordem)
o que significa que o time teamId1 perdeu pro time teamId2 N vezes (é parecido com o word count do professor)

assim, a gente vai receber da web 5 campeoes, vai gerar o id unico dessa combinacao, e vai olhar as linhas que tem o teamId1 igual a esse id

Ai, basta escolher o teamId2 da linha que tiver o maior N (esse vai ser o melhor time contra o teamId1)

exemplo de como vai ta no rdd no final de tudo:

(time1, time2, 670)
(time1, time3, 332)
(time1, time4, 54)
(time2, time5, 652)
(time2, time6, 353)
(time2, time7, 111)
...

ou seja,
se o time adversario for o time1, a gente escolhe o time2, pq ele ganhou 670 vezes
se o time adversario for o time2, a gente escolhe o time5, pq ele ganhou 652 vezes
etc

onde eu parei:

fiz praticamente tudo isso, o que resultou num rdd (teamSuggestionsRdd) assim: (N, (adversaryInputExample, teamSuggestion))

exemplo que tá rodando:
(3,(19051091161267,22044105120420))
(2,(19051091161267,6043079157236))

ou seja,
o time 22044105120420 ganhou 3 vezes do time 19051091161267
o time 6043079157236 ganhou 2 vezes do time 19051091161267

depois eu converto esses IDs em arrays de nomes de campeoes e imprimo as sugestoes

agora, faltam 3 coisas:

1- mudar a lista de partidas que é lida. Por enquanto eu so pego as partidas do arquivo featuredGames, e adiciono algumas
    manualmente so pra repetir alguns times e aparecer no exemplo (depois tem q tirar o bloco q faz isso).
    Provalmente vai ter q olhar na API, ver aquilo que o diguin falou, pra pegar uma lista gigante de milhoes de partidas jogadas.
    Todas funcoes tao prontas, so tem q mudar mesmo a lista inicial de jogos, ou seja, basta mudar o gamesDf pra lista nova

2- ajustar alguma coisa pra considerar um numero minimo de 100 partidas por time por ex, porcentagem, algo assim (aquilo q a gente tinha conversado)

3- gerar o arquivo json final com as estatisticas

alem disso provavelmente da pra mudar alguns nomes de variaveis etc pra ficar mais claro se quiser,
e da pra refatorar algumas coisas tbm (deixei alguns comentarios de coisas q podiam ser diferentes), talvez passar pra orientacao a objeto.
Fiz meio de qualquer jeito as funcoes e essa "main", mas deve ter umas partes da main que podem
virar outras funcoes tbm, alem de tirar alguns prints, etc

obs: se vc for rodar em outra pasta, acho q vc vai ter q criar a paste games/ dentro, nao sei se ele cria sozinho

*/

















