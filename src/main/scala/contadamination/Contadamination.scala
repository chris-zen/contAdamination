package contadamination

import java.io.File

import contadamination.bloom.BloomFilterBuilder
import contadamination.results.{ ContaminationFilterUtils, ContaminationFilter }
import org.apache.spark.{ SparkConf, SparkContext }
import org.bdgenomics.adam.rdd.ADAMContext
import org.bdgenomics.formats.avro.AlignmentRecord
import org.bdgenomics.utils.cli._
import org.kohsuke.args4j.{ Argument, Option => Args4jOption }

class ContadaminationArgs extends Args4jBase with Serializable {
  @Argument(required = false, metaVar = "INPUT", usage = "Reads path", index = 0)
  var readsPath = "2-5pM-3h_S3_L001_I2_001.fastq.bam.adam"

  @Args4jOption(required = false, name = "-reference_paths", usage = "Reference paths")
  var referencePaths = Array("src/test/resources/mt.fasta")

  @Args4jOption(required = false, name = "-prob_of_false_positive", usage = "Probability of false positive, default 0.0005")
  val probOfFalsePositive = 0.0005

  @Args4jOption(required = false, name = "-window_size", usage = "Window size, default 30")
  val windowSize = 30
}

object ContadaminationCompanion extends BDGCommandCompanion with Serializable {
  val commandName = "contadamination"
  val commandDescription = "Find contamination in NGS read data using a bloom filter implementation."

  def apply(cmdLine: Array[String]) = {
    new Contadamination(Args4j[ContadaminationArgs](cmdLine))
  }
}

class Contadamination(protected val args: ContadaminationArgs) extends BDGSparkCommand[ContadaminationArgs] with Serializable {
  val companion = ContadaminationCompanion

  def run(sc: SparkContext) {
    //println("arguments: " + args.windowSize + " " + args.probOfFalsePositive + " " + args.referencePaths + " " + args.readsPath);

    val adamContext = new ADAMContext(sc)

    val reads = adamContext.loadAlignments(args.readsPath)

    val bloomFilterBuilder = new BloomFilterBuilder(
      adamContext,
      args.probOfFalsePositive,
      args.windowSize)

    val contaminationFilters =
      ContaminationFilterUtils.
        createContaminationFilters(args.referencePaths, bloomFilterBuilder)

    val results = ContaminationFilterUtils.
      queryReadsAgainstFilters(args.windowSize, contaminationFilters, reads)

    results.foreach(println)
  }
}
