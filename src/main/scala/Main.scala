import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.SparkConf
import com.github.nscala_time.time.Imports._
import org.joda.time.format.DateTimeFormatter
import org.joda.time.format.ISODateTimeFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.SimpleTimeZone
import java.util.TimeZone
import spray.json._
import java.nio.file.{Paths, Files}
import java.nio.charset.StandardCharsets

object nyc {
  //lat
  val south = 40.701434
  val north = 40.877151
  //long
  val west = -73.909750
  val east = -74.017210
}

case class FeatureCollection(`type`:String = "FeatureCollection", features: Seq[Feature])
case class Feature(`type`:String, properties:Properties, geometry:Geometry)
case class Properties(waitTime:Double)
case class Geometry(coordinates:Seq[Seq[Seq[Double]]], `type`:String = "Polygon")

object MyJsonProtocol extends DefaultJsonProtocol {
  implicit val propertiesFormat = jsonFormat1(Properties)
  implicit val geometryFormat = jsonFormat2(Geometry)
  implicit val featureFormat = jsonFormat3(Feature)
  implicit val featureCollectionFormat = jsonFormat2(FeatureCollection)
  
}

import MyJsonProtocol._


case class Location(long:Double, lat:Double) {
  import nyc._
  def box:(Int,Int) = {
    ((long - west)/(east - west)*100).toInt -> ((lat - south)/(north - south)*100).toInt  
  }
  def valid = {
    lat  < north + 1 &&
    lat  > south - 1 &&
    long < west  + 1 &&
    long > east  - 1 
  }
}
case class TaF(car:String, driver:String, vendor:String, pickupTime: Date, dropoffTime: Date, pickupLocation: Location, dropoffLocation: Location)



object Main {
  
  val longOffset = 74.124979 - 74.019064
  
  def main(args:Array[String]) {
    val conf = new SparkConf().setAppName("Datascience Challenge").setMaster("local[8]")
    val sc = new SparkContext(conf)
    val input = sc.textFile("taf.csv", 2).cache().map(line => line.split(",").toSeq)
    val sdf = new SimpleDateFormat("\"yyyy-MM-dd HH:mm:ss\"")
    sdf.setTimeZone(TimeZone.getTimeZone("EST"))
    val parsed = input.map(line => TaF(
        line(1).slice(1, line(1).size - 1),
        line(2).slice(1, line(2).size - 1),
        line(3).slice(1, line(3).size - 1),
        sdf.parse(line(6)),
        sdf.parse(line(7)),
        Location(line(11).toDouble,line(12).toDouble),
        Location(line(13).toDouble,line(14).toDouble)
    )).filter(taf => taf.pickupLocation.valid && taf.dropoffLocation.valid)
    def waitTime(x:TaF, y:TaF) = (x.pickupTime.getTime - y.dropoffTime.getTime).toDouble/60000
    val grouped = parsed.map(taf => taf.car -> taf).groupBy(_._1).map{case (k,v)=>k->v.map(_._2).toSeq.sortBy { taf => taf.pickupTime }}
    val locationWaitTimes = grouped.map { case (k,v) => k -> (v.zip(v.tail)
        .map { case (f,s) => f.pickupLocation.box -> waitTime(s,f) }
        .filter{case ((long,lat),_) => long >= 0 && long <= 100 && lat <= 100 && lat >= 0 } ) }
    val waitTimesByLoc = locationWaitTimes.flatMap{ case (car,times) => times}.sortBy(_._1)
    val avgWaitByBox = waitTimesByLoc.groupBy(_._1).map{case (box, pairs) => box -> pairs.map(_._2).sum / pairs.size}
    def boxToPoly(x:Int,y:Int):Seq[Seq[Double]] = {
      import nyc._
      val nsd = north - south
      val ewd = east - west
      val s = south + (y.toDouble / 100) * nsd
      val n = south + ((y.toDouble+1) / 100) * nsd
      val e = east + (x.toDouble / 100) * ewd + longOffset
      val w = east + ((x.toDouble+1) / 100) * ewd + longOffset
      Seq(Seq(e,n),Seq(w,n),Seq(w,s),Seq(e,s),Seq(e,n))
    }
    val res = FeatureCollection("FeatureCollection", avgWaitByBox.collect().toSeq.map { case ((x,y),wait) => Feature("Feature", Properties(wait), Geometry(Seq(boxToPoly(x,y))))})
    Files.write(Paths.get("waittimes.js"), ("var waitData = " + res.toJson.toString + ";").getBytes(StandardCharsets.UTF_8))
    
    
    
  }
}