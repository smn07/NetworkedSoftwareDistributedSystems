package it.polimi.nsds.spark;

import org.apache.spark.sql.*;
import org.apache.spark.sql.streaming.StreamingQuery;
import org.apache.spark.sql.streaming.StreamingQueryException;
import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.StructField;
import org.apache.spark.sql.types.StructType;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;

import static org.apache.spark.sql.functions.*;

/*
 * Group number: 24
 *
 * Group members
 *  - Marco Barbieri
 *  - Lucrezia Sorrentino
 *  - Simone Di Ienno
 */

public class SparkGroup24 {
    private static final int numCourses = 3000;

    public static void main(String[] args) throws TimeoutException {
        final String master = args.length > 0 ? args[0] : "local[4]";
        final String filePath = args.length > 1 ? args[1] : "./";

        final SparkSession spark = SparkSession
                .builder()
                .master(master)
                .appName("SparkEval")
                .getOrCreate();
        spark.sparkContext().setLogLevel("ERROR");

        final List<StructField> profsFields = new ArrayList<>();
        profsFields.add(DataTypes.createStructField("prof_name", DataTypes.StringType, false));
        profsFields.add(DataTypes.createStructField("course_name", DataTypes.StringType, false));
        final StructType profsSchema = DataTypes.createStructType(profsFields);

        final List<StructField> coursesFields = new ArrayList<>();
        coursesFields.add(DataTypes.createStructField("course_name", DataTypes.StringType, false));
        coursesFields.add(DataTypes.createStructField("course_hours", DataTypes.IntegerType, false));
        coursesFields.add(DataTypes.createStructField("course_students", DataTypes.IntegerType, false));
        final StructType coursesSchema = DataTypes.createStructType(coursesFields);

        final List<StructField> videosFields = new ArrayList<>();
        videosFields.add(DataTypes.createStructField("video_id", DataTypes.IntegerType, false));
        videosFields.add(DataTypes.createStructField("video_duration", DataTypes.IntegerType, false));
        videosFields.add(DataTypes.createStructField("course_name", DataTypes.StringType, false));
        final StructType videosSchema = DataTypes.createStructType(videosFields);

        // Professors: prof_name, course_name
        final Dataset<Row> profs = spark
                .read()
                .option("header", "false")
                .option("delimiter", ",")
                .schema(profsSchema)
                .csv(filePath + "files/profs.csv");

        // Courses: course_name, course_hours, course_students
        final Dataset<Row> courses = spark
                .read()
                .option("header", "false")
                .option("delimiter", ",")
                .schema(coursesSchema)
                .csv(filePath + "files/courses.csv");

        // Videos: video_id, video_duration, course_name
        final Dataset<Row> videos = spark
                .read()
                .option("header", "false")
                .option("delimiter", ",")
                .schema(videosSchema)
                .csv(filePath + "files/videos.csv");

        // Visualizations: value, timestamp
        // value represents the video id
        final Dataset<Row> visualizations = spark
                .readStream()
                .format("rate")
                .option("rowsPerSecond", 10)
                .load()
                .withColumn("value", col("value").mod(numCourses));

        /*
         * Query Q1. Compute the total number of lecture hours per prof
         */

        System.out.println("Query Q1");

        // we join the profs table with the courses table based on the course name
        // we group by the professor name and we sum the course hours

        final Dataset<Row> q1 = profs
                .join(courses, "course_name")
                .groupBy("prof_name")
                .sum("course_hours")
                .withColumnRenamed("sum(course_hours)", "total_hours");

        q1.show();

        /*
         * Query Q2. For each course, compute the total duration of all the visualizations of videos of that course,
         * computed over a minute, updated every 10 seconds
         */

        System.out.println("Query Q2");

        // we join the visualizations table with the videos table based on the video id
        // we group by the course name and we sum the video duration

        final StreamingQuery q2 = visualizations
                .join(videos, col("value").equalTo(col("video_id")))
                .groupBy(col("course_name"), window(col("timestamp"), "1 minute", "10 seconds"))
                .agg(sum("video_duration").as("total_duration"))
                .writeStream()
                .outputMode("update")
                .format("console")
                .start();

        /*
         * Query Q3. For each video, compute the total number of visualizations of that video
         * with respect to the number of students in the course in which the video is used.
         */

        System.out.println("Query Q3");

        // we compute a table with the number of students per course for each video associated to that course
        // we do this because this table is static and we can cache it in order to avoid recomputations

        Dataset<Row> students_per_video = videos
                .join(courses, "course_name")
                .select("video_id", "course_students");

        students_per_video.cache();

        // we compute a table with the total number of visualizations for each video

        Dataset<Row> total_visualizations = visualizations
                .join(videos, col("value").equalTo(col("video_id")))
                .groupBy("video_id")
                .agg(count("video_id").as("total_visualizations"));

        // we join the total_visualizations table with the students_per_video table based on the video id
        // we compute the number of visualizations per student for each video
        // by dividing the total visualizations by the number of students of the course related to that video

        final StreamingQuery q3 = total_visualizations
                .join(students_per_video, "video_id")
                .withColumn("visualizations_per_student", 
                                col("total_visualizations").divide(col("course_students")))
                .select("video_id", "visualizations_per_student")
                .writeStream()
                .outputMode("update")
                .format("console")
                .start();

        try {
            q2.awaitTermination();
            q3.awaitTermination();
        } catch (final StreamingQueryException e) {
            e.printStackTrace();
        }

        spark.close();
    }
}