package demy.twitter

import demy.util.log
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions._

object GeoNames {

def geoNames2Parquet(spark:SparkSession) {
import spark.implicits._
//Countries
val countries = (spark.read.parquet("hdfs:///data/geo/all-cities.parquet")
    .where($"feature_code".isin(Array("PCL", "PCLD", "PCLF", "PCLI"):_*))
    .select($"id", $"name", concat_ws(",", regexp_replace($"name", lit(","), lit(" ")), coalesce($"alternatenames", lit(""))).as("alias")
                , $"country_code".as("code"), typedLit[String](null).as("parent_code"), typedLit[String](null).as("city"), typedLit[String](null).as("adm4")
                , typedLit[String](null).as("adm3"), typedLit[String](null).as("adm2"), typedLit[String](null).as("adm1"), typedLit[String](null).as("country")
                , typedLit[String](null).as("adm4_code"), typedLit[String](null).as("adm3_code"), typedLit[String](null).as("adm2_code"), typedLit[String](null).as("adm1_code")
                , $"country_code", $"population", $"feature_code".as("geo_type"), $"timezone".as("time_zone")).as[GeoElement]
    .map(current => GeoElement(current.id, current.name, current.alias, current.code, current.parent_code, current.city, current.adm4, current.adm3, current.adm2, current.adm1, current.alias
                                , current.adm4_code, current.adm3_code, current.adm2_code, current.adm1_code, current.country_code, current.population, current.geo_type, current.time_zone)
        ).persist)

log.msg(s"${countries.count} countries")
//Level 1
val adm1_0 = (spark.read.parquet("hdfs:///data/geo/all-cities.parquet")
    .where($"feature_code"==="ADM1")
    .select($"id",$"name", concat_ws(",", regexp_replace($"name", lit(","), lit(" ")), coalesce($"alternatenames", lit(""))).as("alias")
                , concat($"admin1_code", lit(";"),$"country_code").as("code"), $"country_code".as("parent_code"), typedLit[String](null).as("city"), typedLit[String](null).as("adm4")
                , typedLit[String](null).as("adm3"), typedLit[String](null).as("adm2"), typedLit[String](null).as("adm1"), typedLit[String](null).as("country")
                , typedLit[String](null).as("adm4_code"), typedLit[String](null).as("adm3_code"), typedLit[String](null).as("adm2_code"), $"admin1_code".as("adm1_code")
                , $"country_code", $"population", $"feature_code".as("geo_type"), $"timezone".as("time_zone")).as[GeoElement])
val adm1 = (adm1_0
    .joinWith(countries, adm1_0("parent_code")===countries("code"), "inner")
    .map(p => p match { case (current, parent)
            => GeoElement(current.id, current.name, current.alias, current.code, current.parent_code, current.city, current.adm4, current.adm3, current.adm2, current.alias, parent.country
                                , current.adm4_code, current.adm3_code, current.adm2_code, current.adm1_code, current.country_code, current.population, current.geo_type, current.time_zone)
        }).persist)

log.msg(s"${adm1.count} adm 1")
//Level 2
val adm2_0 = (spark.read.parquet("hdfs:///data/geo/all-cities.parquet")
    .where($"feature_code"==="ADM2")
    .select($"id",$"name", concat_ws(",", regexp_replace($"name", lit(","), lit(" ")), coalesce($"alternatenames", lit(""))).as("alias")
                , concat($"admin2_code", lit(";"),$"admin1_code", lit(";"),$"country_code").as("code"), concat($"admin1_code", lit(";"),$"country_code").as("parent_code")
                , typedLit[String](null).as("city"), typedLit[String](null).as("adm4")
                , typedLit[String](null).as("adm3"), typedLit[String](null).as("adm2"), typedLit[String](null).as("adm1"), typedLit[String](null).as("country")
                , typedLit[String](null).as("adm4_code"), typedLit[String](null).as("adm3_code"), $"admin2_code".as("adm2_code"), $"admin1_code".as("adm1_code")
                , $"country_code", $"population", $"feature_code".as("geo_type"), $"timezone".as("time_zone")).as[GeoElement])
val adm2 = (adm2_0
    .joinWith(adm1, adm2_0("parent_code")===adm1("code") , "inner")
    .map(p => p match { case (current, parent)
            => GeoElement(current.id, current.name, current.alias, current.code, current.parent_code, current.city, current.adm4, current.adm3, current.alias, parent.adm1, parent.country
                                , current.adm4_code, current.adm3_code, current.adm2_code, current.adm1_code, current.country_code, current.population, current.geo_type, current.time_zone)
        }).persist)

log.msg(s"${adm2.count} adm 2")

//Level 3
val adm3_0 = (spark.read.parquet("hdfs:///data/geo/all-cities.parquet")
    .where($"feature_code"==="ADM3")
    .select($"id",$"name", concat_ws(",", regexp_replace($"name", lit(","), lit(" ")), coalesce($"alternatenames", lit(""))).as("alias")
                , concat($"admin3_code", lit(";"),$"admin2_code", lit(";"),$"admin1_code", lit(";"),$"country_code").as("code"),concat($"admin2_code", lit(";"),$"admin1_code", lit(";"),$"country_code").as("parent_code")
                , typedLit[String](null).as("city"), typedLit[String](null).as("adm4")
                , typedLit[String](null).as("adm3"), typedLit[String](null).as("adm2"), typedLit[String](null).as("adm1"), typedLit[String](null).as("country")
                , typedLit[String](null).as("adm4_code"), $"admin3_code".as("adm3_code"), $"admin2_code".as("adm2_code"), $"admin1_code".as("adm1_code")
                , $"country_code", $"population", $"feature_code".as("geo_type"), $"timezone".as("time_zone")).as[GeoElement])
val adm3 = (adm3_0
    .joinWith(adm2, adm3_0("parent_code")===adm2("code") , "inner")
    .map(p => p match { case (current, parent)
            => GeoElement(current.id, current.name, current.alias, current.code, current.parent_code, current.city, current.adm4, current.alias, parent.adm2, parent.adm1, parent.country
                                , current.adm4_code, current.adm3_code, current.adm2_code, current.adm1_code, current.country_code, current.population, current.geo_type, current.time_zone)
        }).persist)

log.msg(s"${adm3.count} adm 3")

//Level 4
val adm4_0 = (spark.read.parquet("hdfs:///data/geo/all-cities.parquet")
    .where($"feature_code"==="ADM4")
    .select($"id",$"name", concat_ws(",", regexp_replace($"name", lit(","), lit(" ")), coalesce($"alternatenames", lit(""))).as("alias")
                , concat($"admin4_code", lit(";"), $"admin3_code", lit(";"),$"admin2_code", lit(";"),$"admin1_code", lit(";"),$"country_code").as("code")
                , concat($"admin3_code", lit(";"),$"admin2_code", lit(";"),$"admin1_code", lit(";"),$"country_code").as("parent_code"), typedLit[String](null).as("city"), typedLit[String](null).as("adm4")
                , typedLit[String](null).as("adm3"), typedLit[String](null).as("adm2"), typedLit[String](null).as("adm1"), typedLit[String](null).as("country")
                , $"admin4_code".as("adm4_code"), $"admin3_code".as("adm3_code"), $"admin2_code".as("adm2_code"), $"admin1_code".as("adm1_code")
                , $"country_code", $"population", $"feature_code".as("geo_type"), $"timezone".as("time_zone")).as[GeoElement])
val adm4 = (adm4_0
    .joinWith(adm3, adm4_0("parent_code")===adm3("code") , "inner")
    .map(p => p match { case (current, parent)
            => GeoElement(current.id, current.name, current.alias, current.code, current.parent_code, current.city, current.alias,  parent.adm3,  parent.adm2, parent.adm1, parent.country
                                , current.adm4_code, current.adm3_code, current.adm2_code, current.adm1_code, current.country_code, current.population, current.geo_type, current.time_zone)
        }).persist)

log.msg(s"${adm4.count} adm 4")

//Villes
val all_regions = adm1.union(adm2).union(adm3).union(adm4)

val cities_0 = (spark.read.parquet("hdfs:///data/geo/all-cities.parquet")
    .where($"feature_class"==="P")
    .select($"id",$"name", concat_ws(",", regexp_replace($"name", lit(","), lit(" ")), coalesce($"alternatenames", lit(""))).as("alias")
                , $"id".as("code")
                , when($"admin4_code".isNotNull, concat($"admin4_code", lit(";"), $"admin3_code", lit(";"),$"admin2_code", lit(";"),$"admin1_code", lit(";"),$"country_code"))
                    .when($"admin3_code".isNotNull, concat($"admin3_code", lit(";"),$"admin2_code", lit(";"),$"admin1_code", lit(";"),$"country_code"))
                    .when($"admin2_code".isNotNull, concat($"admin2_code", lit(";"),$"admin1_code", lit(";"),$"country_code"))
                    .when($"admin1_code".isNotNull, concat($"admin1_code", lit(";"),$"country_code"))
                    .otherwise($"country_code").as("parent_code"), typedLit[String](null).as("city"), typedLit[String](null).as("adm4")
                , typedLit[String](null).as("adm3"), typedLit[String](null).as("adm2"), typedLit[String](null).as("adm1"), typedLit[String](null).as("country")
                , $"admin4_code".as("adm4_code"), $"admin3_code".as("adm3_code"), $"admin2_code".as("adm2_code"), $"admin1_code".as("adm1_code"), $"country_code", $"population"
                , $"feature_code".as("geo_type"), $"timezone".as("time_zone")).as[GeoElement])
val cities = (cities_0
    .joinWith(all_regions,cities_0("parent_code")===all_regions("code"), "inner")
    .map(p => p match { case (current, parent)
            => GeoElement(current.id, current.name, current.alias, current.code, current.parent_code, current.alias, current.adm4,  parent.adm3,  parent.adm2, parent.adm1, parent.country
                                , current.adm4_code, current.adm3_code, current.adm2_code, current.adm1_code, current.country_code, current.population, current.geo_type, current.time_zone)
        }).persist)
log.msg(s"${cities.count} cities")

//Writing all to disk
cities.union(adm1).union(adm2).union(adm3).union(adm4).union(countries).repartition(32, expr("cast(id/100000 as int)")).write.mode("overwrite").parquet("hdfs:///data/geo/all-cities-expanded.parquet")
cities.unpersist
adm1.unpersist
adm2.unpersist
adm3.unpersist
adm4.unpersist
countries.unpersist
log.msg("all done!")

}
}
