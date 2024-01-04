package it.polimi.nsds.spark.lab.cities;

import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.sql.Column;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.streaming.StreamingQuery;
import org.apache.spark.sql.streaming.StreamingQueryException;
import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.StructField;
import org.apache.spark.sql.types.StructType;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;

import static org.apache.spark.sql.functions.*;

public class Cities {
    public static void main(String[] args) throws TimeoutException {
        final String master = args.length > 0 ? args[0] : "local[4]";
        final String filePath = args.length > 1 ? args[1] : "./";

        final SparkSession spark = SparkSession
                .builder()
                .master(master)
                .appName("SparkEval")
                .getOrCreate();
        spark.sparkContext().setLogLevel("ERROR");

        final List<StructField> citiesRegionsFields = new ArrayList<>();
        citiesRegionsFields.add(DataTypes.createStructField("city", DataTypes.StringType, false));
        citiesRegionsFields.add(DataTypes.createStructField("region", DataTypes.StringType, false));
        final StructType citiesRegionsSchema = DataTypes.createStructType(citiesRegionsFields);

        final List<StructField> citiesPopulationFields = new ArrayList<>();
        citiesPopulationFields.add(DataTypes.createStructField("id", DataTypes.IntegerType, false));
        citiesPopulationFields.add(DataTypes.createStructField("city", DataTypes.StringType, false));
        citiesPopulationFields.add(DataTypes.createStructField("population", DataTypes.IntegerType, false));
        final StructType citiesPopulationSchema = DataTypes.createStructType(citiesPopulationFields);

        final Dataset<Row> citiesPopulation = spark
                .read()
                .option("header", "true")
                .option("delimiter", ";")
                .schema(citiesPopulationSchema)
                .csv(filePath + "files/cities/cities_population.csv");

        final Dataset<Row> citiesRegions = spark
                .read()
                .option("header", "true")
                .option("delimiter", ";")
                .schema(citiesRegionsSchema)
                .csv(filePath + "files/cities/cities_regions.csv");

        System.out.println("query 1:");

        final Dataset<Row> regionCitiesPopulation = citiesPopulation
                .join(citiesRegions, "city");

        final Dataset<Row> q1 = regionCitiesPopulation
                .groupBy("region")
                .sum("population")
                .withColumnRenamed("sum(population)", "population");

        q1.show();

        System.out.println("query 2:");

        final Dataset<Row> countCityMaxPopulation = regionCitiesPopulation
                .groupBy("region")
                .agg(count("city"), max("population"))
                .withColumnRenamed("count(city)", "numCities")
                .withColumnRenamed("max(population)", "maxPopulation");

        final Dataset<Row> q2 = countCityMaxPopulation
                .join(regionCitiesPopulation, "region")
                .filter("population = maxPopulation")
                .select("region", "numCities", "city", "population")
                .withColumnRenamed("city", "mostPopulatedCity")
                .withColumnRenamed("population", "populationOfMostPopulatedCity");

        q2.show();

        System.out.println("query 3:");

        // JavaRDD where each element is an integer and represents the population of a city
        JavaRDD<Integer> population = citiesPopulation.toJavaRDD().map(r -> r.getInt(2));
        
        int italyPopulation = population.reduce((a, b) -> a + b);
        int currentYear = 0;

        while(italyPopulation < 100000000){
            currentYear++;
            JavaRDD<Integer> newPopulation = population.map(x -> x > 1000 ? x+x/100 : x-x/100);
            italyPopulation = newPopulation.reduce((a, b) -> a + b);

            System.out.println("Year: " + currentYear + ", total population: " + italyPopulation);
            
            newPopulation.cache();
            population.unpersist();
            population = newPopulation;
        }

        // Bookings: the value represents the city of the booking
        final Dataset<Row> bookings = spark
                .readStream()
                .format("rate")
                .option("rowsPerSecond", 100)
                .load();

        System.out.println("query 4:");

        long numCities = citiesPopulation.count();

        final StreamingQuery q4 = bookings
                .join(regionCitiesPopulation, regionCitiesPopulation.col("id").equalTo(bookings.col("value").mod(numCities).plus(1)))
                .groupBy(col("region"), window(col("timestamp"), "30 seconds", "5 seconds"))
                .count()
                .writeStream()
                .outputMode("update")
                .format("console")
                .start();

        try {
            q4.awaitTermination();
        } catch (final StreamingQueryException e) {
            e.printStackTrace();
        }

        spark.close();
    }
}